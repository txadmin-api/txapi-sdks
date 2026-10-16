package cloud.txapi.sdk;

import cloud.txapi.sdk.internal.Json;
import com.fasterxml.jackson.databind.JsonNode;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Consumer;
import java.util.function.Function;

/** A local HTTP server standing in for the txApi backend. */
final class MockApi implements AutoCloseable {
    static final String KEY = "txp_test_0123456789abcdef";

    static {
        // Without TCP_NODELAY every response waits ~40 ms for a delayed ACK.
        System.setProperty("sun.net.httpserver.nodelay", "true");
    }

    record Req(String method, String rawPath, String rawQuery, Map<String, List<String>> headers, String body) {
        String header(String name) {
            List<String> v = headers.get(name.toLowerCase(Locale.ROOT));
            return v == null || v.isEmpty() ? null : v.get(0);
        }

        Map<String, String> query() {
            Map<String, String> out = new LinkedHashMap<>();
            if (rawQuery == null || rawQuery.isEmpty()) {
                return out;
            }
            for (String part : rawQuery.split("&")) {
                int eq = part.indexOf('=');
                String k = URLDecoder.decode(eq < 0 ? part : part.substring(0, eq), StandardCharsets.UTF_8);
                String v = eq < 0 ? "" : URLDecoder.decode(part.substring(eq + 1), StandardCharsets.UTF_8);
                out.put(k, v);
            }
            return out;
        }

        JsonNode json() {
            return Json.tryParse(body);
        }
    }

    record Resp(int status, String body, Map<String, String> headers) {
        static Resp json(int status, String body) {
            return new Resp(status, body, Map.of("Content-Type", "application/json"));
        }

        static Resp ok(String body) {
            return json(200, body);
        }

        Resp header(String name, String value) {
            Map<String, String> h = new LinkedHashMap<>(headers);
            h.put(name, value);
            return new Resp(status, body, h);
        }
    }

    private final HttpServer server;
    private final ExecutorService executor = Executors.newCachedThreadPool();
    final List<Req> requests = new CopyOnWriteArrayList<>();
    private final Deque<Resp> queue = new ArrayDeque<>();
    private volatile Function<Req, Resp> handler = r -> Resp.ok("{}");

    MockApi() {
        try {
            server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
        server.createContext("/", this::handle);
        server.setExecutor(executor);
        server.start();
    }

    String baseUrl() {
        return "http://127.0.0.1:" + server.getAddress().getPort();
    }

    /** Responses served in order before falling back to the handler. */
    synchronized MockApi enqueue(Resp... responses) {
        queue.addAll(List.of(responses));
        return this;
    }

    MockApi handler(Function<Req, Resp> h) {
        this.handler = h;
        return this;
    }

    TxApi client() {
        return client(b -> { });
    }

    TxApi client(Consumer<TxApi.Builder> customise) {
        TxApi.Builder b = TxApi.builder().apiKey(KEY).baseUrl(baseUrl()).env(k -> null);
        customise.accept(b);
        return b.build();
    }

    Req last() {
        return requests.get(requests.size() - 1);
    }

    private void handle(HttpExchange ex) throws IOException {
        try (ex) {
            Map<String, List<String>> headers = new LinkedHashMap<>();
            ex.getRequestHeaders().forEach((k, v) -> headers.put(k.toLowerCase(Locale.ROOT), v));
            String body = new String(ex.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
            Req req = new Req(ex.getRequestMethod(), ex.getRequestURI().getRawPath(), ex.getRequestURI().getRawQuery(),
                    headers, body);
            requests.add(req);
            Resp resp;
            synchronized (this) {
                resp = queue.pollFirst();
            }
            if (resp == null) {
                resp = handler.apply(req);
            }
            resp.headers().forEach((k, v) -> ex.getResponseHeaders().add(k, v));
            byte[] bytes = resp.body() == null ? new byte[0] : resp.body().getBytes(StandardCharsets.UTF_8);
            ex.sendResponseHeaders(resp.status(), bytes.length == 0 ? -1 : bytes.length);
            if (bytes.length > 0) {
                try (OutputStream out = ex.getResponseBody()) {
                    out.write(bytes);
                }
            }
        }
    }

    @Override
    public void close() {
        server.stop(0);
        executor.shutdownNow();
    }
}
