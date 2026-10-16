package cloud.txapi.sdk.live;

import cloud.txapi.sdk.TxApi;
import cloud.txapi.sdk.errors.AuthenticationException;
import cloud.txapi.sdk.errors.CommandException;
import cloud.txapi.sdk.errors.TxApiException;
import cloud.txapi.sdk.errors.ValidationException;
import cloud.txapi.sdk.internal.Json;
import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class LiveStreamTest {
    private static final String KEY = "txp_live_test_key";

    private TestWsServer ws;
    private TxApi client;

    /** Records every callback as a short string. */
    static final class Recorder implements ConsoleListener {
        final BlockingQueue<String> events = new LinkedBlockingQueue<>();

        String next() throws InterruptedException {
            String e = events.poll(5, TimeUnit.SECONDS);
            assertThat(e).as("expected another listener event").isNotNull();
            return e;
        }

        String nextMatching(String prefix) throws InterruptedException {
            while (true) {
                String e = next();
                if (e.startsWith(prefix)) {
                    return e;
                }
            }
        }

        @Override
        public void onOpen(LiveConsole console) {
            events.add("open");
        }

        @Override
        public void onInit(List<String> lines) {
            events.add("init:" + lines);
        }

        @Override
        public void onLine(String line) {
            events.add("line:" + line);
        }

        @Override
        public void onError(String message) {
            events.add("error:" + message);
        }

        @Override
        public void onReconnecting(int attempt, Duration delay) {
            events.add("reconnecting:" + attempt);
        }

        @Override
        public void onClose(int code, String reason) {
            events.add("close:" + code + ":" + reason);
        }
    }

    /** Server-log variant (onInit has another parameter type). */
    static final class LogRecorder implements ServerLogListener {
        final BlockingQueue<String> events = new LinkedBlockingQueue<>();

        String next() throws InterruptedException {
            String e = events.poll(5, TimeUnit.SECONDS);
            assertThat(e).isNotNull();
            return e;
        }

        @Override
        public void onInit(List<JsonNode> entries) {
            events.add("init:" + entries.size());
        }

        @Override
        public void onEntry(JsonNode entry) {
            events.add("entry:" + entry.path("msg").asText());
        }

        @Override
        public void onError(String message) {
            events.add("error:" + message);
        }

        @Override
        public void onClose(int code, String reason) {
            events.add("close:" + code);
        }
    }

    @BeforeEach
    void setUp() throws Exception {
        ws = new TestWsServer();
        client = TxApi.builder().apiKey(KEY).baseUrl(ws.baseUrl()).serverId(7L).timeout(Duration.ofSeconds(5)).build();
    }

    @AfterEach
    void tearDown() throws Exception {
        client.close();
        ws.close();
    }

    private static LiveOptions fast() {
        return LiveOptions.defaults().initialReconnectDelay(Duration.ofMillis(50)).maxReconnectDelay(Duration.ofMillis(200));
    }

    private static String frame(Object... kv) {
        var node = Json.MAPPER.createObjectNode();
        for (int i = 0; i < kv.length; i += 2) {
            node.set((String) kv[i], Json.MAPPER.valueToTree(kv[i + 1]));
        }
        return node.toString();
    }

    @Test
    void consoleReceivesInitAndLines() throws Exception {
        ws.onConnect(c -> {
            c.sendText(frame("type", "init", "lines", List.of("a", "b")));
            c.sendText(frame("type", "log", "lines", List.of("c", "d")));
        });
        Recorder rec = new Recorder();
        try (LiveConsole console = client.console().connect(rec)) {
            assertThat(rec.next()).isEqualTo("open");
            assertThat(rec.next()).isEqualTo("init:[a, b]");
            assertThat(rec.next()).isEqualTo("line:c");
            assertThat(rec.next()).isEqualTo("line:d");
            assertThat(console.isOpen()).isTrue();
            assertThat(console.toString()).doesNotContain(KEY);
        }
        TestWsServer.Conn c = ws.nextConnection();
        assertThat(c.target).startsWith("/ws/console?").contains("apiKey=" + KEY).contains("serverId=7");
        assertThat(c.headers.get("user-agent")).startsWith("txapi-java/");
    }

    @Test
    void sendCommandIsAcknowledgedInOrder() throws Exception {
        try (LiveConsole console = client.console().connect(new Recorder())) {
            TestWsServer.Conn c = ws.nextConnection();
            CompletableFuture<String> first = console.sendCommand("say one");
            CompletableFuture<String> second = console.sendCommand("say two");

            JsonNode sent1 = Json.MAPPER.readTree(c.nextText());
            JsonNode sent2 = Json.MAPPER.readTree(c.nextText());
            assertThat(sent1.path("type").asText()).isEqualTo("command");
            assertThat(sent1.path("command").asText()).isEqualTo("say one");
            assertThat(sent2.path("command").asText()).isEqualTo("say two");
            assertThat(console.pendingCommands()).isEqualTo(2);

            c.sendText(frame("type", "ack", "message", "Command sent"));
            c.sendText(frame("type", "error", "message", "Unknown command"));

            assertThat(first.get(5, TimeUnit.SECONDS)).isEqualTo("Command sent");
            assertThatThrownBy(() -> second.get(5, TimeUnit.SECONDS)).isInstanceOf(ExecutionException.class)
                    .hasCauseInstanceOf(CommandException.class).hasMessageContaining("Unknown command");
            assertThat(console.pendingCommands()).isZero();
        }
    }

    @Test
    void errorWithoutPendingCommandGoesToListener() throws Exception {
        ws.onConnect(c -> c.sendText(frame("type", "error", "message", "Command limit reached")));
        Recorder rec = new Recorder();
        try (LiveConsole ignored = client.console().connect(rec)) {
            assertThat(rec.nextMatching("error:")).isEqualTo("error:Command limit reached");
        }
    }

    @Test
    void commandTimesOutAndALateAckDoesNotResolveTheNext() throws Exception {
        try (LiveConsole console = client.console().connect(new Recorder(), LiveOptions.defaults().commandTimeout(Duration.ofMillis(200)))) {
            TestWsServer.Conn c = ws.nextConnection();
            CompletableFuture<String> slow = console.sendCommand("slow");
            assertThatThrownBy(() -> slow.get(5, TimeUnit.SECONDS)).hasCauseInstanceOf(CommandException.class)
                    .hasMessageContaining("no acknowledgement");
            CompletableFuture<String> next = console.sendCommand("next");
            c.nextText();
            c.nextText();
            c.sendText(frame("type", "ack", "message", "late ack for slow"));
            c.sendText(frame("type", "ack", "message", "ack for next"));
            assertThat(next.get(5, TimeUnit.SECONDS)).isEqualTo("ack for next");
        }
    }

    @Test
    void invalidCommandsFailWithoutSending() throws Exception {
        try (LiveConsole console = client.console().connect(new Recorder())) {
            TestWsServer.Conn c = ws.nextConnection();
            assertThatThrownBy(() -> console.sendCommand(" ").join()).hasCauseInstanceOf(ValidationException.class);
            assertThatThrownBy(() -> console.sendCommand("x".repeat(1025)).join())
                    .hasCauseInstanceOf(ValidationException.class);
            assertThat(c.received.poll(300, TimeUnit.MILLISECONDS)).isNull();
        }
    }

    @Test
    void reconnectsAfter1011AndFailsPendingCommands() throws Exception {
        ws.onConnect(c -> c.sendText(frame("type", "init", "lines", List.of("hello"))));
        Recorder rec = new Recorder();
        try (LiveConsole console = client.console().connect(rec, fast())) {
            assertThat(rec.nextMatching("init:")).isEqualTo("init:[hello]");
            TestWsServer.Conn first = ws.nextConnection();
            CompletableFuture<String> pending = console.sendCommand("restart");
            first.nextText();
            first.close(1011, "txAdmin connection failed");

            assertThatThrownBy(() -> pending.get(5, TimeUnit.SECONDS)).hasCauseInstanceOf(CommandException.class)
                    .hasMessageContaining("connection lost");
            assertThat(rec.nextMatching("reconnecting:")).isEqualTo("reconnecting:1");
            assertThat(rec.nextMatching("open")).isEqualTo("open");
            assertThat(rec.nextMatching("init:")).isEqualTo("init:[hello]");
            assertThat(ws.nextConnection()).isNotNull();
            assertThat(console.isOpen()).isTrue();
        }
        assertThat(ws.handshakeTargets).hasSize(2);
    }

    @Test
    void reconnectsAfterAnAbruptDrop() throws Exception {
        Recorder rec = new Recorder();
        try (LiveConsole ignored = client.console().connect(rec, fast())) {
            rec.nextMatching("open");
            ws.nextConnection().drop();
            assertThat(rec.nextMatching("reconnecting:")).isEqualTo("reconnecting:1");
            assertThat(rec.nextMatching("open")).isEqualTo("open");
        }
    }

    @Test
    void doesNotReconnectAfter1008AndReportsTheReason() throws Exception {
        Recorder rec = new Recorder();
        LiveConsole console = client.console().connect(rec, fast());
        rec.nextMatching("open");
        ws.nextConnection().close(1008, "Monthly request quota exceeded");
        assertThat(rec.nextMatching("close:")).isEqualTo("close:1008:Monthly request quota exceeded");
        LiveClose how = console.awaitClose(Duration.ofSeconds(5));
        assertThat(how).isEqualTo(new LiveClose(1008, "Monthly request quota exceeded", false));
        assertThat(console.isClosed()).isTrue();
        Thread.sleep(400);
        assertThat(ws.handshakeTargets).hasSize(1);
        assertThatThrownBy(() -> console.sendCommand("say hi").join()).hasCauseInstanceOf(CommandException.class);
    }

    @Test
    void doesNotReconnectAfter1009() throws Exception {
        Recorder rec = new Recorder();
        LiveConsole console = client.console().connect(rec, fast());
        rec.nextMatching("open");
        ws.nextConnection().close(1009, "Message too big");
        assertThat(rec.nextMatching("close:")).isEqualTo("close:1009:Message too big");
        Thread.sleep(300);
        assertThat(ws.handshakeTargets).hasSize(1);
        assertThat(console.isClosed()).isTrue();
    }

    @Test
    void closeStopsTheStream() throws Exception {
        Recorder rec = new Recorder();
        LiveConsole console = client.console().connect(rec, fast());
        rec.nextMatching("open");
        TestWsServer.Conn c = ws.nextConnection();
        console.close();
        console.close(); // idempotent
        assertThat(c.closeCodes.poll(5, TimeUnit.SECONDS)).isEqualTo(1000);
        LiveClose how = console.awaitClose(Duration.ofSeconds(5));
        assertThat(how).isNotNull();
        assertThat(how.byClient()).isTrue();
        assertThat(rec.nextMatching("close:")).startsWith("close:1000");
        Thread.sleep(300);
        assertThat(ws.handshakeTargets).hasSize(1);
    }

    @Test
    void closingTheClientClosesItsStreams() throws Exception {
        Recorder rec = new Recorder();
        LiveConsole console = client.console().connect(rec, fast());
        rec.nextMatching("open");
        client.close();
        assertThat(console.awaitClose(Duration.ofSeconds(5))).isNotNull();
        assertThat(console.isOpen()).isFalse();
    }

    @Test
    void rejectedHandshakeFailsTheConnect() {
        ws.handshake(t -> new TestWsServer.Handshake(401, "{\"success\":false,\"error\":\"Invalid API key\"}"));
        assertThatThrownBy(() -> client.console().connect(new Recorder()))
                .isInstanceOf(TxApiException.class)
                .satisfies(e -> {
                    if (e instanceof cloud.txapi.sdk.errors.ApiException api) {
                        assertThat(api).isInstanceOf(AuthenticationException.class);
                    }
                });
    }

    @Test
    void perStreamServerIdAndViews() throws Exception {
        client.forServer(9).console().connect(new Recorder()).close();
        assertThat(ws.nextConnection().target).contains("serverId=9");
        client.console().connect(new Recorder(), LiveOptions.defaults().serverId(12)).close();
        assertThat(ws.nextConnection().target).contains("serverId=12");
    }

    @Test
    void asyncConnect() throws Exception {
        ws.onConnect(c -> c.sendText(frame("type", "init", "lines", List.of("x"))));
        Recorder rec = new Recorder();
        LiveConsole console = client.async().console().connect(rec).get(5, TimeUnit.SECONDS);
        try (console) {
            assertThat(rec.nextMatching("init:")).isEqualTo("init:[x]");
            TestWsServer.Conn c = ws.nextConnection();
            CompletableFuture<String> ack = console.sendCommand("status");
            c.nextText();
            c.sendText(frame("type", "ack", "message", "Command sent"));
            assertThat(ack.get(5, TimeUnit.SECONDS)).isEqualTo("Command sent");
        }
    }

    @Test
    void serverLogReceivesEntries() throws Exception {
        ws.onConnect(c -> {
            c.sendText(frame("type", "init", "entries", List.of(java.util.Map.of("msg", "boot"), java.util.Map.of("msg", "x"))));
            c.sendText(frame("type", "log", "entries", List.of(java.util.Map.of("msg", "joined"))));
            c.sendText(frame("type", "error", "message", "txAdmin unreachable"));
        });
        LogRecorder rec = new LogRecorder();
        try (LiveServerLog log = client.serverLog().connect(rec)) {
            assertThat(rec.next()).isEqualTo("init:2");
            assertThat(rec.next()).isEqualTo("entry:joined");
            assertThat(rec.next()).isEqualTo("error:txAdmin unreachable");
            assertThat(log.isOpen()).isTrue();
        }
        assertThat(ws.nextConnection().target).startsWith("/ws/serverlog?");
    }
}
