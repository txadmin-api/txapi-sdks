package cloud.txapi.sdk.internal;

import cloud.txapi.sdk.RateLimit;
import cloud.txapi.sdk.RequestEvent;
import cloud.txapi.sdk.RequestOptions;
import cloud.txapi.sdk.ResponseEvent;
import cloud.txapi.sdk.errors.ApiConnectionException;
import cloud.txapi.sdk.errors.ApiException;
import cloud.txapi.sdk.errors.ApiTimeoutException;
import cloud.txapi.sdk.errors.RateLimitException;
import cloud.txapi.sdk.errors.RequestCancelledException;
import cloud.txapi.sdk.errors.TxApiException;
import cloud.txapi.sdk.errors.ValidationException;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;

import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpTimeoutException;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CancellationException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Request building, transport, retries and error mapping shared by every view of a client.
 * Internal API — not exported from the module, may change without notice.
 */
public final class ClientCore implements AutoCloseable {
    private static final System.Logger LOG = System.getLogger("cloud.txapi.sdk");
    /** Longest {@code Retry-After} the SDK waits for on its own. */
    static final Duration MAX_RETRY_AFTER = Duration.ofSeconds(60);
    static final Set<Integer> RETRYABLE_GET_STATUS = Set.of(502, 503, 504);

    private final ClientConfig config;
    private final String userAgentHeader;
    private final AtomicReference<RateLimit> lastRateLimit = new AtomicReference<>();
    private final Set<AutoCloseable> liveStreams = ConcurrentHashMap.newKeySet();
    private volatile boolean closed;

    public ClientCore(ClientConfig config, String sdkVersion) {
        this.config = config;
        String ua = "txapi-java/" + sdkVersion;
        if (config.userAgent() != null && !config.userAgent().isBlank()) {
            ua = ua + " " + config.userAgent().trim();
        }
        this.userAgentHeader = ua;
    }

    public ClientConfig config() {
        return config;
    }

    public HttpClient http() {
        return config.httpClient();
    }

    public String userAgent() {
        return userAgentHeader;
    }

    public RateLimit lastRateLimit() {
        return lastRateLimit.get();
    }

    // ── request building ──────────────────────────────────────────────────────

    /** A request ready to be sent (possibly several times). */
    record Prepared(Endpoint endpoint, String method, URI uri, byte[] body, Duration timeout, int maxRetries) {
        HttpRequest toRequest(String apiKey, String userAgent) {
            HttpRequest.Builder b = HttpRequest.newBuilder(uri)
                    .timeout(timeout)
                    .header("Authorization", "Bearer " + apiKey)
                    .header("Accept", "application/json")
                    .header("User-Agent", userAgent);
            if (body != null) {
                b.header("Content-Type", "application/json");
                b.method(method, HttpRequest.BodyPublishers.ofByteArray(body));
            } else {
                b.method(method, HttpRequest.BodyPublishers.noBody());
            }
            return b.build();
        }

        boolean isGet() {
            return "GET".equals(method);
        }
    }

    Prepared prepare(Call<?> call, RequestOptions options) {
        Endpoint ep = call.endpoint;
        RequestOptions opts = options == null ? RequestOptions.none() : options;

        StringBuilder path = new StringBuilder();
        String template = ep.path();
        int i = 0;
        while (i < template.length()) {
            char c = template.charAt(i);
            if (c == '{') {
                int end = template.indexOf('}', i);
                String name = template.substring(i + 1, end);
                String value = call.path.get(name);
                if (value == null || value.isBlank()) {
                    throw new ValidationException(name, ep.id() + ": path parameter '" + name + "' is required");
                }
                path.append(encodePathSegment(value));
                i = end + 1;
            } else {
                path.append(c);
                i++;
            }
        }

        Map<String, String> query = new LinkedHashMap<>();
        if (!ep.accountLevel()) {
            Long serverId = opts.serverId().isPresent() ? Long.valueOf(opts.serverId().getAsLong()) : config.serverId();
            if (serverId != null) {
                query.put("serverId", String.valueOf(serverId));
            }
        }
        for (String required : ep.requiredQuery()) {
            String v = call.query.get(required);
            if (v == null || v.isBlank()) {
                throw new ValidationException(required, ep.id() + ": parameter '" + required + "' is required");
            }
        }
        query.putAll(call.query);

        byte[] body = null;
        if (ep.hasBody()) {
            validateBody(ep, call.body);
            try {
                body = Json.MAPPER.writeValueAsBytes(call.body);
            } catch (JsonProcessingException e) {
                throw new ValidationException(null, ep.id() + ": could not serialise the request body: " + e.getOriginalMessage());
            }
        }

        StringBuilder uri = new StringBuilder(config.baseUrl()).append(path);
        if (!query.isEmpty()) {
            char sep = '?';
            for (Map.Entry<String, String> e : query.entrySet()) {
                uri.append(sep).append(encodeQuery(e.getKey())).append('=').append(encodeQuery(e.getValue()));
                sep = '&';
            }
        }
        Duration timeout = opts.timeout().orElse(config.timeout());
        int maxRetries = opts.maxRetries().isPresent() ? opts.maxRetries().getAsInt() : config.maxRetries();
        return new Prepared(ep, ep.method(), URI.create(uri.toString()), body, timeout, maxRetries);
    }

    static void validateBody(Endpoint ep, Map<String, Object> body) {
        for (Endpoint.BodyField f : ep.bodyFields()) {
            Object v = body.get(f.name());
            if (f.required()) {
                boolean missing = v == null
                        || (v instanceof CharSequence cs && cs.toString().isBlank())
                        || (v instanceof Collection<?> col && col.isEmpty());
                if (missing) {
                    throw new ValidationException(f.name(), ep.id() + ": '" + f.name() + "' is required"
                            + (v instanceof Collection<?> ? " (at least one entry)" : " and must not be blank"));
                }
            }
            if (f.maxLength() > 0 && v instanceof CharSequence cs && cs.length() > f.maxLength()) {
                throw new ValidationException(f.name(), ep.id() + ": '" + f.name() + "' is " + cs.length()
                        + " characters long, the API accepts at most " + f.maxLength());
            }
        }
    }

    /** RFC 3986 path segment: everything but unreserved characters is percent-encoded (UTF-8). */
    public static String encodePathSegment(String s) {
        byte[] bytes = s.getBytes(StandardCharsets.UTF_8);
        StringBuilder out = new StringBuilder(bytes.length + 8);
        for (byte b : bytes) {
            int c = b & 0xff;
            if ((c >= 'a' && c <= 'z') || (c >= 'A' && c <= 'Z') || (c >= '0' && c <= '9')
                    || c == '-' || c == '.' || c == '_' || c == '~') {
                out.append((char) c);
            } else {
                out.append('%').append(Character.toUpperCase(Character.forDigit(c >> 4, 16)))
                        .append(Character.toUpperCase(Character.forDigit(c & 0xf, 16)));
            }
        }
        return out.toString();
    }

    static String encodeQuery(String s) {
        return URLEncoder.encode(s, StandardCharsets.UTF_8).replace("+", "%20");
    }

    // ── synchronous execution ─────────────────────────────────────────────────

    public <T> T execute(Call<T> call, RequestOptions options, ResponseSink sink) {
        ensureOpen();
        Prepared p = prepare(call, options);
        int attempt = 0;
        while (true) {
            if (Thread.currentThread().isInterrupted()) {
                throw new RequestCancelledException(p.endpoint().id() + ": interrupted", new InterruptedException());
            }
            RequestEvent event = new RequestEvent(p.endpoint().id(), p.method(), p.uri(), attempt);
            fireRequest(event);
            long start = System.nanoTime();
            HttpResponse<byte[]> response;
            try {
                response = http().send(p.toRequest(config.apiKey(), userAgentHeader), HttpResponse.BodyHandlers.ofByteArray());
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new RequestCancelledException(p.endpoint().id() + ": interrupted while waiting for the response", e);
            } catch (IOException e) {
                TxApiException error = transportError(p, e);
                Duration delay = transportRetryDelay(p, attempt);
                if (delay == null) {
                    throw error;
                }
                sleep(delay, p);
                attempt++;
                continue;
            }
            Outcome<T> outcome = handle(call, p, event, response, start, attempt, sink);
            if (outcome.error == null) {
                return outcome.value;
            }
            if (outcome.retryDelay == null) {
                throw outcome.error;
            }
            sleep(outcome.retryDelay, p);
            attempt++;
        }
    }

    private static void sleep(Duration d, Prepared p) {
        if (d.isZero() || d.isNegative()) {
            return;
        }
        try {
            Thread.sleep(d.toMillis());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RequestCancelledException(p.endpoint().id() + ": interrupted while waiting to retry", e);
        }
    }

    // ── asynchronous execution ────────────────────────────────────────────────

    public <T> CompletableFuture<T> executeAsync(Call<T> call, RequestOptions options, ResponseSink sink) {
        Prepared p;
        try {
            ensureOpen();
            p = prepare(call, options);
        } catch (RuntimeException e) {
            return CompletableFuture.failedFuture(e);
        }
        CompletableFuture<T> result = new CompletableFuture<>();
        attemptAsync(call, p, 0, result, sink);
        return result;
    }

    private <T> void attemptAsync(Call<T> call, Prepared p, int attempt, CompletableFuture<T> result, ResponseSink sink) {
        if (result.isDone()) {
            return;
        }
        RequestEvent event = new RequestEvent(p.endpoint().id(), p.method(), p.uri(), attempt);
        fireRequest(event);
        long start = System.nanoTime();
        CompletableFuture<HttpResponse<byte[]>> inFlight;
        try {
            inFlight = http().sendAsync(p.toRequest(config.apiKey(), userAgentHeader), HttpResponse.BodyHandlers.ofByteArray());
        } catch (RuntimeException e) {
            result.completeExceptionally(new ApiConnectionException(p.endpoint().id() + ": " + e.getMessage(), e));
            return;
        }
        result.whenComplete((v, t) -> {
            if (result.isCancelled()) {
                inFlight.cancel(true);
            }
        });
        inFlight.whenComplete((response, failure) -> {
            if (result.isDone()) {
                return;
            }
            if (failure != null) {
                Throwable cause = unwrap(failure);
                if (cause instanceof CancellationException) {
                    result.cancel(true);
                    return;
                }
                TxApiException error = cause instanceof IOException io ? transportError(p, io)
                        : new ApiConnectionException(p.endpoint().id() + ": " + cause, cause);
                Duration delay = transportRetryDelay(p, attempt);
                if (delay == null) {
                    result.completeExceptionally(error);
                } else {
                    later(delay, () -> attemptAsync(call, p, attempt + 1, result, sink));
                }
                return;
            }
            Outcome<T> outcome;
            try {
                outcome = handle(call, p, event, response, start, attempt, sink);
            } catch (RuntimeException e) {
                result.completeExceptionally(e);
                return;
            }
            if (outcome.error == null) {
                result.complete(outcome.value);
            } else if (outcome.retryDelay == null) {
                result.completeExceptionally(outcome.error);
            } else {
                later(outcome.retryDelay, () -> attemptAsync(call, p, attempt + 1, result, sink));
            }
        });
    }

    /** Runs a task after a delay without blocking a thread. */
    public static void later(Duration delay, Runnable task) {
        long ms = Math.max(0, delay.toMillis());
        CompletableFuture.delayedExecutor(ms, TimeUnit.MILLISECONDS).execute(task);
    }

    /** Unwraps {@link CompletionException}/{@link ExecutionException}. */
    public static Throwable unwrap(Throwable t) {
        Throwable c = t;
        while ((c instanceof CompletionException || c instanceof ExecutionException) && c.getCause() != null) {
            c = c.getCause();
        }
        return c;
    }

    // ── response handling and retry policy (SPEC §4–§6) ───────────────────────

    private record Outcome<T>(T value, TxApiException error, Duration retryDelay) {
    }

    private <T> Outcome<T> handle(Call<T> call, Prepared p, RequestEvent event, HttpResponse<byte[]> response,
                                  long startNanos, int attempt, ResponseSink sink) {
        int status = response.statusCode();
        Map<String, List<String>> headers = lowerCase(response.headers().map());
        RateLimit rateLimit = RateLimit.fromHeaders(headers);
        if (rateLimit != null) {
            lastRateLimit.set(rateLimit);
        }
        fireResponse(new ResponseEvent(event, status, headers, rateLimit,
                Duration.ofNanos(System.nanoTime() - startNanos)));
        byte[] bytes = response.body() == null ? new byte[0] : response.body();
        if (status >= 200 && status < 300) {
            T value = parse(call, p, bytes);
            if (sink != null) {
                sink.accept(status, headers, rateLimit);
            }
            return new Outcome<>(value, null, null);
        }
        ApiException error = Errors.fromResponse(status, new String(bytes, StandardCharsets.UTF_8), headers, rateLimit);
        return new Outcome<>(null, error, retryDelay(p, attempt, error));
    }

    @SuppressWarnings("unchecked")
    private <T> T parse(Call<T> call, Prepared p, byte[] bytes) {
        if (bytes.length == 0) {
            return call.type.getRawClass() == JsonNode.class
                    ? (T) com.fasterxml.jackson.databind.node.MissingNode.getInstance() : null;
        }
        try {
            return Json.MAPPER.readValue(bytes, call.type);
        } catch (IOException e) {
            throw new TxApiException(p.endpoint().id() + ": could not parse the response as "
                    + call.type.getRawClass().getSimpleName() + ": " + e.getMessage(), e);
        }
    }

    /** Retry delay for an error response, or {@code null} when it must not be retried. */
    static Duration retryDelay(Prepared p, int attempt, ApiException error) {
        if (attempt >= p.maxRetries()) {
            return null;
        }
        if (error instanceof RateLimitException rl) {
            // Not executed by the server → safe for every method. No Retry-After = monthly quota.
            Duration ra = rl.retryAfter().orElse(null);
            if (rl.isQuotaExhausted() || ra == null || ra.compareTo(MAX_RETRY_AFTER) > 0) {
                return null;
            }
            return ra;
        }
        if (p.isGet() && RETRYABLE_GET_STATUS.contains(error.status())) {
            Duration ra = Errors.parseRetryAfter(Errors.first(error.headers(), "retry-after"));
            if (ra != null && ra.compareTo(MAX_RETRY_AFTER) <= 0) {
                return ra;
            }
            return backoff(attempt);
        }
        return null;
    }

    static Duration transportRetryDelay(Prepared p, int attempt) {
        return p.isGet() && attempt < p.maxRetries() ? backoff(attempt) : null;
    }

    /** {@code min(8 s, 0.5 s · 2^attempt)} with full jitter. */
    public static Duration backoff(int attempt) {
        long cap = Math.min(8_000L, 500L << Math.min(attempt, 10));
        return Duration.ofMillis(ThreadLocalRandom.current().nextLong(cap + 1));
    }

    private static TxApiException transportError(Prepared p, IOException e) {
        if (e instanceof HttpTimeoutException) {
            return new ApiTimeoutException(p.endpoint().id() + ": no response within " + p.timeout().toMillis() + " ms", e);
        }
        String msg = e.getMessage() == null ? e.getClass().getSimpleName() : e.getMessage();
        return new ApiConnectionException(p.endpoint().id() + ": " + msg, e);
    }

    public static Map<String, List<String>> lowerCase(Map<String, List<String>> headers) {
        Map<String, List<String>> out = new LinkedHashMap<>();
        headers.forEach((k, v) -> {
            if (k != null) {
                out.merge(k.toLowerCase(Locale.ROOT), List.copyOf(v), (a, b) -> {
                    java.util.ArrayList<String> merged = new java.util.ArrayList<>(a);
                    merged.addAll(b);
                    return List.copyOf(merged);
                });
            }
        });
        return java.util.Collections.unmodifiableMap(out);
    }

    // ── hooks ─────────────────────────────────────────────────────────────────

    private void fireRequest(RequestEvent event) {
        if (config.onRequest() == null) {
            return;
        }
        try {
            config.onRequest().accept(event);
        } catch (RuntimeException e) {
            LOG.log(System.Logger.Level.WARNING, "txApi onRequest hook threw; ignored", e);
        }
    }

    private void fireResponse(ResponseEvent event) {
        if (config.onResponse() == null) {
            return;
        }
        try {
            config.onResponse().accept(event);
        } catch (RuntimeException e) {
            LOG.log(System.Logger.Level.WARNING, "txApi onResponse hook threw; ignored", e);
        }
    }

    // ── live streams + lifecycle ──────────────────────────────────────────────

    /** WebSocket URI for a live stream: {@code ws(s)://host/ws/<path>?apiKey=…&serverId=…}. */
    public URI webSocketUri(String wsPath, Long serverId) {
        String base = config.baseUrl();
        if (base.startsWith("https://")) {
            base = "wss://" + base.substring("https://".length());
        } else if (base.startsWith("http://")) {
            base = "ws://" + base.substring("http://".length());
        }
        Long sid = serverId != null ? serverId : config.serverId();
        StringBuilder uri = new StringBuilder(base).append(wsPath)
                .append("?apiKey=").append(encodeQuery(config.apiKey()));
        if (sid != null) {
            uri.append("&serverId=").append(sid);
        }
        return URI.create(uri.toString());
    }

    public void register(AutoCloseable stream) {
        liveStreams.add(stream);
        if (closed) {
            closeQuietly(stream);
        }
    }

    public void unregister(AutoCloseable stream) {
        liveStreams.remove(stream);
    }

    private void ensureOpen() {
        if (closed) {
            throw new IllegalStateException("This TxApi client has been closed");
        }
    }

    public boolean isClosed() {
        return closed;
    }

    @Override
    public void close() {
        if (closed) {
            return;
        }
        closed = true;
        for (AutoCloseable s : List.copyOf(liveStreams)) {
            closeQuietly(s);
        }
        liveStreams.clear();
        if (config.ownsHttpClient() && config.httpClient() instanceof AutoCloseable c) {
            // HttpClient is AutoCloseable since Java 21; on 17–20 its threads simply idle out.
            closeQuietly(c);
        }
    }

    private static void closeQuietly(AutoCloseable c) {
        try {
            c.close();
        } catch (Exception e) {
            LOG.log(System.Logger.Level.DEBUG, "txApi: error while closing", e);
        }
    }
}
