package cloud.txapi.sdk.live;

import java.io.ByteArrayOutputStream;
import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import java.util.function.Function;

/**
 * A minimal RFC 6455 server for tests: text frames, close handshake, ping/pong, abrupt drops and
 * rejected handshakes. Not for production use.
 */
final class TestWsServer implements AutoCloseable {
    private static final String GUID = "258EAFA5-E914-47DA-95CA-C5AB0DC85B11";

    /** Result of the HTTP handshake: {@code status == 101} accepts, anything else rejects. */
    record Handshake(int status, String body) {
        static Handshake accept() {
            return new Handshake(101, null);
        }
    }

    final class Conn {
        final String target;
        final Map<String, String> headers;
        final BlockingQueue<String> received = new LinkedBlockingQueue<>();
        final BlockingQueue<Integer> closeCodes = new LinkedBlockingQueue<>();
        private final Socket socket;
        private final OutputStream out;

        Conn(Socket socket, String target, Map<String, String> headers) throws IOException {
            this.socket = socket;
            this.target = target;
            this.headers = headers;
            this.out = socket.getOutputStream();
        }

        synchronized void sendText(String text) {
            writeFrame(0x1, text.getBytes(StandardCharsets.UTF_8));
        }

        /** Sends a close frame with code and reason, then closes the TCP connection. */
        synchronized void close(int code, String reason) {
            byte[] r = reason.getBytes(StandardCharsets.UTF_8);
            byte[] payload = new byte[2 + r.length];
            payload[0] = (byte) (code >> 8);
            payload[1] = (byte) code;
            System.arraycopy(r, 0, payload, 2, r.length);
            writeFrame(0x8, payload);
            try {
                Thread.sleep(50);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            drop();
        }

        /** Closes the TCP connection without a close frame (client sees 1006). */
        void drop() {
            try {
                socket.close();
            } catch (IOException ignored) {
                // closed
            }
        }

        String nextText() throws InterruptedException {
            return received.poll(5, TimeUnit.SECONDS);
        }

        private void writeFrame(int opcode, byte[] payload) {
            try {
                ByteArrayOutputStream f = new ByteArrayOutputStream();
                f.write(0x80 | opcode);
                if (payload.length < 126) {
                    f.write(payload.length);
                } else if (payload.length < 65536) {
                    f.write(126);
                    f.write(payload.length >> 8);
                    f.write(payload.length);
                } else {
                    f.write(127);
                    for (int i = 7; i >= 0; i--) {
                        f.write((int) ((long) payload.length >> (8 * i)));
                    }
                }
                f.write(payload);
                out.write(f.toByteArray());
                out.flush();
            } catch (IOException ignored) {
                // peer gone
            }
        }

        private void readLoop() {
            try (InputStream in = socket.getInputStream()) {
                ByteArrayOutputStream message = new ByteArrayOutputStream();
                while (true) {
                    int b0 = readByte(in);
                    int b1 = readByte(in);
                    boolean fin = (b0 & 0x80) != 0;
                    int opcode = b0 & 0x0f;
                    long len = b1 & 0x7f;
                    if (len == 126) {
                        len = (readByte(in) << 8) | readByte(in);
                    } else if (len == 127) {
                        len = 0;
                        for (int i = 0; i < 8; i++) {
                            len = (len << 8) | readByte(in);
                        }
                    }
                    byte[] mask = new byte[4];
                    if ((b1 & 0x80) != 0) {
                        readFully(in, mask);
                    }
                    byte[] payload = new byte[(int) len];
                    readFully(in, payload);
                    for (int i = 0; i < payload.length; i++) {
                        payload[i] ^= mask[i % 4];
                    }
                    switch (opcode) {
                        case 0x0, 0x1 -> {
                            message.write(payload);
                            if (fin) {
                                received.add(message.toString(StandardCharsets.UTF_8));
                                message.reset();
                            }
                        }
                        case 0x8 -> {
                            int code = payload.length >= 2 ? ((payload[0] & 0xff) << 8) | (payload[1] & 0xff) : 1005;
                            closeCodes.add(code);
                            synchronized (this) {
                                writeFrame(0x8, payload.length >= 2 ? new byte[]{payload[0], payload[1]} : new byte[0]);
                            }
                            drop();
                            return;
                        }
                        case 0x9 -> {
                            synchronized (this) {
                                writeFrame(0xA, payload);
                            }
                        }
                        default -> {
                            // pong / unknown: ignore
                        }
                    }
                }
            } catch (IOException e) {
                // connection ended
            }
        }
    }

    private final ServerSocket server;
    final BlockingQueue<Conn> connections = new LinkedBlockingQueue<>();
    final CopyOnWriteArrayList<String> handshakeTargets = new CopyOnWriteArrayList<>();
    private volatile Function<String, Handshake> handshake = t -> Handshake.accept();
    private volatile Consumer<Conn> onConnect = c -> { };

    TestWsServer() throws IOException {
        server = new ServerSocket(0, 50, InetAddress.getLoopbackAddress());
        Thread t = new Thread(this::acceptLoop, "test-ws-accept");
        t.setDaemon(true);
        t.start();
    }

    String baseUrl() {
        return "http://127.0.0.1:" + server.getLocalPort();
    }

    TestWsServer handshake(Function<String, Handshake> h) {
        this.handshake = h;
        return this;
    }

    TestWsServer onConnect(Consumer<Conn> c) {
        this.onConnect = c;
        return this;
    }

    Conn nextConnection() throws InterruptedException {
        return connections.poll(5, TimeUnit.SECONDS);
    }

    private void acceptLoop() {
        while (!server.isClosed()) {
            try {
                Socket s = server.accept();
                Thread t = new Thread(() -> serve(s), "test-ws-conn");
                t.setDaemon(true);
                t.start();
            } catch (IOException e) {
                return;
            }
        }
    }

    private void serve(Socket s) {
        try {
            InputStream in = s.getInputStream();
            String requestLine = readLine(in);
            Map<String, String> headers = new LinkedHashMap<>();
            String line;
            while (!(line = readLine(in)).isEmpty()) {
                int colon = line.indexOf(':');
                if (colon > 0) {
                    headers.put(line.substring(0, colon).trim().toLowerCase(Locale.ROOT), line.substring(colon + 1).trim());
                }
            }
            String target = requestLine.split(" ")[1];
            handshakeTargets.add(target);
            Handshake h = handshake.apply(target);
            OutputStream out = s.getOutputStream();
            if (h.status() != 101) {
                byte[] body = (h.body() == null ? "" : h.body()).getBytes(StandardCharsets.UTF_8);
                out.write(("HTTP/1.1 " + h.status() + " Rejected\r\nContent-Type: application/json\r\nContent-Length: "
                        + body.length + "\r\nConnection: close\r\n\r\n").getBytes(StandardCharsets.US_ASCII));
                out.write(body);
                out.flush();
                s.close();
                return;
            }
            String accept = Base64.getEncoder().encodeToString(MessageDigest.getInstance("SHA-1")
                    .digest((headers.get("sec-websocket-key") + GUID).getBytes(StandardCharsets.US_ASCII)));
            out.write(("HTTP/1.1 101 Switching Protocols\r\nUpgrade: websocket\r\nConnection: Upgrade\r\n"
                    + "Sec-WebSocket-Accept: " + accept + "\r\n\r\n").getBytes(StandardCharsets.US_ASCII));
            out.flush();
            Conn c = new Conn(s, target, headers);
            Thread reader = new Thread(c::readLoop, "test-ws-read");
            reader.setDaemon(true);
            reader.start();
            onConnect.accept(c);
            connections.add(c);
        } catch (Exception e) {
            try {
                s.close();
            } catch (IOException ignored) {
                // closed
            }
        }
    }

    private static String readLine(InputStream in) throws IOException {
        StringBuilder sb = new StringBuilder();
        int c;
        while ((c = in.read()) != -1) {
            if (c == '\n') {
                break;
            }
            if (c != '\r') {
                sb.append((char) c);
            }
        }
        if (c == -1 && sb.length() == 0) {
            throw new EOFException();
        }
        return sb.toString();
    }

    private static int readByte(InputStream in) throws IOException {
        int b = in.read();
        if (b < 0) {
            throw new EOFException();
        }
        return b;
    }

    private static void readFully(InputStream in, byte[] buf) throws IOException {
        int off = 0;
        while (off < buf.length) {
            int n = in.read(buf, off, buf.length - off);
            if (n < 0) {
                throw new EOFException();
            }
            off += n;
        }
    }

    @Override
    public void close() throws IOException {
        server.close();
        Conn c;
        while ((c = connections.poll()) != null) {
            c.drop();
        }
    }
}
