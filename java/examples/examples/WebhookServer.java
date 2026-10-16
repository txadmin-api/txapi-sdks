package examples;

import cloud.txapi.sdk.errors.WebhookVerificationException;
import cloud.txapi.sdk.webhooks.WebhookEvent;
import cloud.txapi.sdk.webhooks.WebhookEvents;
import cloud.txapi.sdk.webhooks.Webhooks;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;

import java.io.IOException;
import java.io.InputStream;
import java.net.InetSocketAddress;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Minimal webhook receiver on the JDK's built-in HTTP server (no framework).
 *
 * <pre>
 * TXAPI_WEBHOOK_SECRET=whsec_… java … examples.WebhookServer 8081
 * </pre>
 *
 * Register {@code https://your-host/txapi-webhook} under Dashboard → Webhooks.
 */
public final class WebhookServer {
    /** Deliveries can arrive more than once — remember what was handled (use a DB in production). */
    private static final Set<String> SEEN = ConcurrentHashMap.newKeySet();

    private WebhookServer() {
    }

    public static void main(String[] args) throws IOException {
        int port = args.length > 0 ? Integer.parseInt(args[0]) : 8081;
        String secret = System.getenv("TXAPI_WEBHOOK_SECRET");
        if (secret == null || secret.isBlank()) {
            System.err.println("Set TXAPI_WEBHOOK_SECRET to the endpoint's signing secret (whsec_…)");
            System.exit(2);
        }

        HttpServer server = HttpServer.create(new InetSocketAddress(port), 0);
        server.createContext("/txapi-webhook", exchange -> handle(exchange, secret));
        server.start();
        System.out.println("Listening on http://localhost:" + port + "/txapi-webhook");
    }

    private static void handle(HttpExchange exchange, String secret) throws IOException {
        try (exchange) {
            if (!"POST".equals(exchange.getRequestMethod())) {
                exchange.sendResponseHeaders(405, -1);
                return;
            }
            // The RAW bytes — never parse and re-serialise before verifying.
            byte[] rawBody;
            try (InputStream in = exchange.getRequestBody()) {
                rawBody = in.readAllBytes();
            }
            String signature = exchange.getRequestHeaders().getFirst(Webhooks.SIGNATURE_HEADER);

            WebhookEvent event;
            try {
                event = Webhooks.verify(rawBody, signature, secret);
            } catch (WebhookVerificationException e) {
                System.err.println("rejected delivery: " + e.getMessage());
                exchange.sendResponseHeaders(400, -1);
                return;
            }

            // Answer quickly (2xx); txApi retries anything else.
            exchange.sendResponseHeaders(204, -1);

            if (event.id() != null && !SEEN.add(event.id())) {
                return; // duplicate delivery
            }
            switch (event.event()) {
                case WebhookEvents.PLAYER_BANNED -> System.out.println("ban: " + event.data());
                case WebhookEvents.SERVER_OFFLINE ->
                        System.out.println("server " + event.serverId() + " went offline");
                case WebhookEvents.PING -> System.out.println("test delivery received");
                default -> System.out.println(event.event() + ": " + event.data());
            }
        }
    }
}
