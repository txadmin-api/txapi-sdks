package examples;

import cloud.txapi.sdk.TxApi;
import cloud.txapi.sdk.errors.CommandException;
import cloud.txapi.sdk.live.ConsoleListener;
import cloud.txapi.sdk.live.LiveClose;
import cloud.txapi.sdk.live.LiveOptions;

import java.time.Duration;
import java.util.List;
import java.util.concurrent.CompletionException;

/**
 * Live console: prints every console line, runs one command and waits for its acknowledgement.
 * Reconnects by itself after network drops and server restarts; stops on a permanent close
 * (revoked key, missing permission) or when you close it.
 *
 * <pre>
 * TXAPI_KEY=txp_… TXAPI_SERVER_ID=12 java … examples.LiveConsole "say Hello from Java"
 * </pre>
 */
public final class LiveConsole {
    private LiveConsole() {
    }

    public static void main(String[] args) {
        String command = args.length > 0 ? args[0] : "status";

        try (TxApi client = TxApi.fromEnv();
             cloud.txapi.sdk.live.LiveConsole console = client.console().connect(new ConsoleListener() {
                 @Override
                 public void onInit(List<String> lines) {
                     System.out.println("— " + lines.size() + " lines of scrollback —");
                 }

                 @Override
                 public void onLine(String line) {
                     System.out.println(line);
                 }

                 @Override
                 public void onReconnecting(int attempt, Duration delay) {
                     System.err.println("connection lost, reconnect #" + attempt + " in " + delay);
                 }

                 @Override
                 public void onClose(int code, String reason) {
                     System.err.println("closed: " + code + " " + reason);
                 }
             }, LiveOptions.defaults().commandTimeout(Duration.ofSeconds(10)))) {

            try {
                String ack = console.sendCommand(command).join();
                System.out.println(">> " + command + " → " + ack);
            } catch (CompletionException e) {
                if (e.getCause() instanceof CommandException ce) {
                    System.err.println("command rejected: " + ce.getMessage());
                } else {
                    throw e;
                }
            }

            // Keep printing lines for 30 seconds, or until the server closes the stream for good.
            LiveClose close = console.awaitClose(Duration.ofSeconds(30));
            if (close != null) {
                System.err.println("stream ended: " + close);
            }
        }
    }
}
