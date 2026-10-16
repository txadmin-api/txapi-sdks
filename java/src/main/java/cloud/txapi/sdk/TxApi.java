package cloud.txapi.sdk;

import cloud.txapi.sdk.internal.ClientConfig;
import cloud.txapi.sdk.internal.ClientCore;
import cloud.txapi.sdk.internal.Invoker;
import cloud.txapi.sdk.internal.ResponseSink;
import cloud.txapi.sdk.resources.Account;
import cloud.txapi.sdk.resources.Admins;
import cloud.txapi.sdk.resources.Console;
import cloud.txapi.sdk.resources.Events;
import cloud.txapi.sdk.resources.History;
import cloud.txapi.sdk.resources.Players;
import cloud.txapi.sdk.resources.Server;
import cloud.txapi.sdk.resources.ServerLog;
import cloud.txapi.sdk.resources.Whitelist;

import java.net.URI;
import java.net.http.HttpClient;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.OptionalLong;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;
import java.util.function.Function;

/**
 * The txApi client — entry point of the SDK.
 *
 * <pre>{@code
 * try (TxApi client = TxApi.builder()
 *         .apiKey(System.getenv("TXAPI_KEY"))
 *         .serverId(1)                         // default server for every call
 *         .build()) {
 *     ActionResult r = client.players().ban(BanRequest.of("license:abc123", "Cheating").withDurationHours(72));
 *     System.out.println(r.message());
 * }
 * }</pre>
 *
 * <p>Calls are grouped like the API: {@link #players()}, {@link #history()}, {@link #server()},
 * {@link #console()}, {@link #serverLog()}, {@link #events()}, {@link #whitelist()},
 * {@link #admins()} and {@link #account()}. Every call blocks; {@link #async()} offers the same calls
 * returning {@link java.util.concurrent.CompletableFuture}s.
 *
 * <p>The client is thread-safe and meant to be shared. {@link #forServer(long)} and
 * {@link #withOptions(RequestOptions)} return lightweight <em>views</em> with other defaults that
 * share the connection pool; closing a view does nothing, closing the client closes everything.
 *
 * <p>Errors are unchecked: {@link cloud.txapi.sdk.errors.TxApiException} and its subclasses.
 */
public final class TxApi implements AutoCloseable {
    /** Version of this SDK. */
    public static final String VERSION = "1.0.0";
    /** API version this SDK was built against ({@code spec/endpoints.json → apiVersion}). */
    public static final String API_VERSION = "1.8.0";
    /** Default base URL. */
    public static final String DEFAULT_BASE_URL = "https://txapi.cloud";
    /** Default timeout per attempt. */
    public static final Duration DEFAULT_TIMEOUT = Duration.ofSeconds(30);
    /** Default number of retries (SPEC §5). */
    public static final int DEFAULT_MAX_RETRIES = 2;

    private final ClientCore core;
    private final Invoker inv;
    private final boolean root;

    private final Account account;
    private final Players players;
    private final History history;
    private final Server server;
    private final Console console;
    private final ServerLog serverLog;
    private final Events events;
    private final Whitelist whitelist;
    private final Admins admins;
    private volatile TxApiAsync async;

    private TxApi(ClientCore core, Invoker inv, boolean root) {
        this.core = core;
        this.inv = inv;
        this.root = root;
        this.account = new Account(inv);
        this.players = new Players(inv);
        this.history = new History(inv);
        this.server = new Server(inv);
        this.console = new Console(inv);
        this.serverLog = new ServerLog(inv);
        this.events = new Events(inv);
        this.whitelist = new Whitelist(inv);
        this.admins = new Admins(inv);
    }

    /** @return a new builder */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Client configured from the environment: {@code TXAPI_KEY} (required), {@code TXAPI_BASE_URL}
     * and {@code TXAPI_SERVER_ID} (optional).
     *
     * @return the client
     * @throws IllegalArgumentException if {@code TXAPI_KEY} is missing or a value is invalid
     */
    public static TxApi fromEnv() {
        return builder().build();
    }

    // ── groups ────────────────────────────────────────────────────────────────

    /** @return account-level calls ({@code /me}, {@code /servers}) */
    public Account account() {
        return account;
    }

    /** @return player database and moderation */
    public Players players() {
        return players;
    }

    /** @return bans and warnings */
    public History history() {
        return history;
    }

    /** @return server state and control, resource commands via {@code server().resources()} */
    public Server server() {
        return server;
    }

    /** @return console buffer, commands and the live console */
    public Console console() {
        return console;
    }

    /** @return server log and the live server log */
    public ServerLog serverLog() {
        return serverLog;
    }

    /** @return the stored event log (deaths, kills, sessions, admin actions …) */
    public Events events() {
        return events;
    }

    /** @return whitelist requests and pre-approvals */
    public Whitelist whitelist() {
        return whitelist;
    }

    /** @return txAdmin panel admins */
    public Admins admins() {
        return admins;
    }

    /**
     * The same calls, asynchronous: every method returns a {@link java.util.concurrent.CompletableFuture}
     * and never blocks. Shares this client's configuration, view defaults and connection pool.
     *
     * @return the asynchronous API
     */
    public TxApiAsync async() {
        TxApiAsync a = async;
        if (a == null) {
            a = new TxApiAsync(this, inv);
            async = a;
        }
        return a;
    }

    // ── views ─────────────────────────────────────────────────────────────────

    /**
     * A view whose calls address another txAdmin server by default.
     *
     * @param serverId server id
     * @return a view sharing this client
     */
    public TxApi forServer(long serverId) {
        return withOptions(RequestOptions.serverId(serverId));
    }

    /**
     * A view with other defaults (server, timeout, retries). Per-call options still win.
     *
     * @param options defaults of the view
     * @return a view sharing this client
     */
    public TxApi withOptions(RequestOptions options) {
        return new TxApi(core, inv.withDefaults(options), false);
    }

    /** @return the defaults of this view (empty for the client itself) */
    public RequestOptions options() {
        return inv.defaults();
    }

    /**
     * Runs one call and returns its parsed data together with HTTP status, headers and rate limit
     * (SPEC §6). If the function makes several calls, the last response wins.
     *
     * <pre>{@code
     * ApiResponse<ServerStatus> r = client.withRawResponse(c -> c.server().status());
     * r.rateLimit().ifPresent(rl -> System.out.println(rl.remaining() + " requests left"));
     * }</pre>
     *
     * @param call the call, made on the passed client
     * @param <T>  data type
     * @return data plus response details
     * @throws IllegalStateException if the function made no request
     */
    public <T> ApiResponse<T> withRawResponse(Function<TxApi, T> call) {
        Objects.requireNonNull(call, "call");
        Capture capture = new Capture();
        T data = call.apply(new TxApi(core, inv.withSink(capture), false));
        return capture.toResponse(data);
    }

    /**
     * Rate-limit information of the most recent response of this client (any view, any thread).
     *
     * @return the last rate limit, empty before the first response or if the API sent none
     */
    public Optional<RateLimit> lastRateLimit() {
        return Optional.ofNullable(core.lastRateLimit());
    }

    /** @return the base URL (without {@code /api/v1}) */
    public String baseUrl() {
        return core.config().baseUrl();
    }

    /** @return the default server of this view or client, if any */
    public OptionalLong serverId() {
        if (inv.defaults().serverId().isPresent()) {
            return inv.defaults().serverId();
        }
        Long id = core.config().serverId();
        return id == null ? OptionalLong.empty() : OptionalLong.of(id);
    }

    /**
     * Closes open live streams and, if the client created it, the HTTP client. Views ignore this;
     * close the client itself. Idempotent.
     */
    @Override
    public void close() {
        if (root) {
            core.close();
        }
    }

    @Override
    public String toString() {
        return "TxApi{baseUrl=" + baseUrl() + ", serverId=" + (serverId().isPresent() ? serverId().getAsLong() : "none")
                + (root ? "" : ", view") + ", apiKey=***}";
    }

    ClientCore core() {
        return core;
    }

    /** Collects the response details for {@link #withRawResponse}. */
    static final class Capture implements ResponseSink {
        private final AtomicReference<Object[]> last = new AtomicReference<>();

        @Override
        public void accept(int status, Map<String, List<String>> headers, RateLimit rateLimit) {
            last.set(new Object[]{status, headers, rateLimit});
        }

        @SuppressWarnings("unchecked")
        <T> ApiResponse<T> toResponse(T data) {
            Object[] l = last.get();
            if (l == null) {
                throw new IllegalStateException("withRawResponse: the function made no API request");
            }
            return new ApiResponse<>((Integer) l[0], (Map<String, List<String>>) l[1], (RateLimit) l[2], data);
        }
    }

    // ── builder ───────────────────────────────────────────────────────────────

    /**
     * Builder of {@link TxApi}. Unset values fall back to the environment ({@code TXAPI_KEY},
     * {@code TXAPI_BASE_URL}, {@code TXAPI_SERVER_ID}) and then to the defaults.
     */
    public static final class Builder {
        private String apiKey;
        private String baseUrl;
        private Long serverId;
        private Duration timeout;
        private Integer maxRetries;
        private String userAgent;
        private HttpClient httpClient;
        private Consumer<RequestEvent> onRequest;
        private Consumer<ResponseEvent> onResponse;
        private Function<String, String> env = System::getenv;

        private Builder() {
        }

        /** @param apiKey the API key ({@code txp_…}); default env {@code TXAPI_KEY} @return this */
        public Builder apiKey(String apiKey) {
            this.apiKey = apiKey;
            return this;
        }

        /**
         * @param baseUrl e.g. {@code https://txapi.cloud}; a trailing slash or {@code /api/v1} is
         *                stripped. Default env {@code TXAPI_BASE_URL}, else {@value TxApi#DEFAULT_BASE_URL}.
         * @return this
         */
        public Builder baseUrl(String baseUrl) {
            this.baseUrl = baseUrl;
            return this;
        }

        /** @param serverId default server for every server-scoped call @return this */
        public Builder serverId(long serverId) {
            this.serverId = serverId;
            return this;
        }

        /** @param serverId default server, {@code null} = none @return this */
        public Builder serverId(Long serverId) {
            this.serverId = serverId;
            return this;
        }

        /** @param timeout timeout per attempt (default 30 s) @return this */
        public Builder timeout(Duration timeout) {
            this.timeout = timeout;
            return this;
        }

        /** @param maxRetries retries for retryable failures, 0 disables them (default 2) @return this */
        public Builder maxRetries(int maxRetries) {
            this.maxRetries = maxRetries;
            return this;
        }

        /** @param userAgent appended to {@code txapi-java/<version>} @return this */
        public Builder userAgent(String userAgent) {
            this.userAgent = userAgent;
            return this;
        }

        /**
         * Use your own {@link HttpClient} (proxy, executor, SSL context …). The SDK does not close it.
         * Make sure it does not follow redirects to other hosts (the API key travels in a header).
         *
         * @param httpClient the client
         * @return this
         */
        public Builder httpClient(HttpClient httpClient) {
            this.httpClient = httpClient;
            return this;
        }

        /**
         * @param hook called before every attempt (also retries); exceptions from it are logged and
         *             ignored, it can never break a request
         * @return this
         */
        public Builder onRequest(Consumer<RequestEvent> hook) {
            this.onRequest = hook;
            return this;
        }

        /**
         * @param hook called after every response (also errors and retried ones); exceptions from it
         *             are logged and ignored
         * @return this
         */
        public Builder onResponse(Consumer<ResponseEvent> hook) {
            this.onResponse = hook;
            return this;
        }

        /** Test hook: environment lookup. */
        Builder env(Function<String, String> env) {
            this.env = Objects.requireNonNull(env);
            return this;
        }

        /**
         * @return the client
         * @throws IllegalArgumentException if no API key is set (here or in {@code TXAPI_KEY}) or a
         *                                  value is invalid
         */
        public TxApi build() {
            String key = firstNonBlank(apiKey, env.apply("TXAPI_KEY"));
            if (key == null) {
                throw new IllegalArgumentException("txApi: an API key is required — set it with "
                        + "TxApi.builder().apiKey(...) or the TXAPI_KEY environment variable");
            }
            key = key.strip();
            String base = normaliseBaseUrl(firstNonBlank(baseUrl, env.apply("TXAPI_BASE_URL"), DEFAULT_BASE_URL));
            Long sid = serverId;
            if (sid == null) {
                String s = env.apply("TXAPI_SERVER_ID");
                if (s != null && !s.isBlank()) {
                    try {
                        sid = Long.parseLong(s.strip());
                    } catch (NumberFormatException e) {
                        throw new IllegalArgumentException("txApi: TXAPI_SERVER_ID is not a number: " + s);
                    }
                }
            }
            Duration t = timeout == null ? DEFAULT_TIMEOUT : timeout;
            if (t.isNegative() || t.isZero()) {
                throw new IllegalArgumentException("txApi: timeout must be positive");
            }
            int retries = maxRetries == null ? DEFAULT_MAX_RETRIES : maxRetries;
            if (retries < 0) {
                throw new IllegalArgumentException("txApi: maxRetries must be >= 0");
            }
            boolean owns = httpClient == null;
            HttpClient http = owns
                    ? HttpClient.newBuilder()
                    .connectTimeout(t.compareTo(Duration.ofSeconds(10)) < 0 ? t : Duration.ofSeconds(10))
                    .followRedirects(HttpClient.Redirect.NEVER)
                    .build()
                    : httpClient;
            ClientConfig config = new ClientConfig(key, base, sid, t, retries, userAgent, http, owns,
                    onRequest == null ? e -> { } : onRequest, onResponse == null ? e -> { } : onResponse);
            ClientCore core = new ClientCore(config, VERSION);
            return new TxApi(core, new Invoker(core, RequestOptions.none(), ResponseSink.NONE), true);
        }

        private static String firstNonBlank(String... values) {
            for (String v : values) {
                if (v != null && !v.isBlank()) {
                    return v;
                }
            }
            return null;
        }

        static String normaliseBaseUrl(String url) {
            String u = url.strip();
            while (u.endsWith("/")) {
                u = u.substring(0, u.length() - 1);
            }
            if (u.endsWith("/api/v1")) {
                u = u.substring(0, u.length() - "/api/v1".length());
            }
            while (u.endsWith("/")) {
                u = u.substring(0, u.length() - 1);
            }
            URI uri;
            try {
                uri = URI.create(u);
            } catch (IllegalArgumentException e) {
                throw new IllegalArgumentException("txApi: invalid base URL: " + url, e);
            }
            String scheme = uri.getScheme();
            if (scheme == null || !(scheme.equalsIgnoreCase("http") || scheme.equalsIgnoreCase("https"))
                    || uri.getHost() == null) {
                throw new IllegalArgumentException("txApi: base URL must be http(s)://host[:port]: " + url);
            }
            if (uri.getQuery() != null || uri.getFragment() != null) {
                throw new IllegalArgumentException("txApi: base URL must not contain a query or fragment: " + url);
            }
            return u;
        }
    }
}
