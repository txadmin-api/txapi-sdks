package cloud.txapi.sdk;

import cloud.txapi.sdk.internal.Invoker;
import cloud.txapi.sdk.resources.AsyncAccount;
import cloud.txapi.sdk.resources.AsyncAdmins;
import cloud.txapi.sdk.resources.AsyncConsole;
import cloud.txapi.sdk.resources.AsyncEvents;
import cloud.txapi.sdk.resources.AsyncHistory;
import cloud.txapi.sdk.resources.AsyncPlayers;
import cloud.txapi.sdk.resources.AsyncServer;
import cloud.txapi.sdk.resources.AsyncServerLog;
import cloud.txapi.sdk.resources.AsyncWhitelist;

import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.function.Function;

/**
 * Asynchronous face of {@link TxApi}: the same groups and methods, each returning a
 * {@link CompletableFuture}. Nothing blocks the calling thread — retries wait on a scheduler,
 * not with {@code Thread.sleep}.
 *
 * <pre>{@code
 * client.async().players().ban(BanRequest.of("license:abc123", "Cheating"))
 *       .thenAccept(r -> System.out.println(r.message()))
 *       .exceptionally(e -> { e.printStackTrace(); return null; });
 * }</pre>
 *
 * <p>Errors complete the future exceptionally with the same {@link cloud.txapi.sdk.errors.TxApiException}
 * types the blocking API throws — also invalid input, which never reaches the network.
 * Cancelling a future aborts the request.
 *
 * <p>Obtained from {@link TxApi#async()}; it shares the client's configuration, view defaults and
 * connection pool and needs no closing of its own.
 */
public final class TxApiAsync {
    private final TxApi sync;
    private final Invoker inv;

    private final AsyncAccount account;
    private final AsyncPlayers players;
    private final AsyncHistory history;
    private final AsyncServer server;
    private final AsyncConsole console;
    private final AsyncServerLog serverLog;
    private final AsyncEvents events;
    private final AsyncWhitelist whitelist;
    private final AsyncAdmins admins;

    TxApiAsync(TxApi sync, Invoker inv) {
        this.sync = sync;
        this.inv = inv;
        this.account = new AsyncAccount(inv);
        this.players = new AsyncPlayers(inv);
        this.history = new AsyncHistory(inv);
        this.server = new AsyncServer(inv);
        this.console = new AsyncConsole(inv);
        this.serverLog = new AsyncServerLog(inv);
        this.events = new AsyncEvents(inv);
        this.whitelist = new AsyncWhitelist(inv);
        this.admins = new AsyncAdmins(inv);
    }

    /** @return account-level calls */
    public AsyncAccount account() {
        return account;
    }

    /** @return player database and moderation */
    public AsyncPlayers players() {
        return players;
    }

    /** @return bans and warnings */
    public AsyncHistory history() {
        return history;
    }

    /** @return server state and control */
    public AsyncServer server() {
        return server;
    }

    /** @return console buffer, commands and the live console */
    public AsyncConsole console() {
        return console;
    }

    /** @return server log and the live server log */
    public AsyncServerLog serverLog() {
        return serverLog;
    }

    /** @return the stored event log */
    public AsyncEvents events() {
        return events;
    }

    /** @return whitelist requests and pre-approvals */
    public AsyncWhitelist whitelist() {
        return whitelist;
    }

    /** @return txAdmin panel admins */
    public AsyncAdmins admins() {
        return admins;
    }

    /** @return the blocking client this belongs to */
    public TxApi sync() {
        return sync;
    }

    /**
     * Asynchronous view addressing another server by default.
     *
     * @param serverId server id
     * @return the asynchronous API of {@code sync().forServer(serverId)}
     */
    public TxApiAsync forServer(long serverId) {
        return sync.forServer(serverId).async();
    }

    /**
     * Asynchronous view with other defaults.
     *
     * @param options defaults of the view
     * @return the asynchronous API of {@code sync().withOptions(options)}
     */
    public TxApiAsync withOptions(RequestOptions options) {
        return sync.withOptions(options).async();
    }

    /**
     * Runs one asynchronous call and completes with its data plus status, headers and rate limit.
     * If the function starts several calls, the response that arrives last wins.
     *
     * <pre>{@code
     * client.async().withRawResponse(a -> a.server().status())
     *       .thenAccept(r -> System.out.println(r.status() + " " + r.rateLimit()));
     * }</pre>
     *
     * @param call the call, made on the passed asynchronous client
     * @param <T>  data type
     * @return a future of data plus response details
     */
    public <T> CompletableFuture<ApiResponse<T>> withRawResponse(Function<TxApiAsync, CompletableFuture<T>> call) {
        Objects.requireNonNull(call, "call");
        TxApi.Capture capture = new TxApi.Capture();
        TxApiAsync captured = new TxApiAsync(sync, inv.withSink(capture));
        CompletableFuture<T> future;
        try {
            future = Objects.requireNonNull(call.apply(captured), "withRawResponse: the function returned null");
        } catch (RuntimeException e) {
            return CompletableFuture.failedFuture(e);
        }
        return future.thenApply(capture::toResponse);
    }

    /** @return rate-limit information of the most recent response of the client */
    public Optional<RateLimit> lastRateLimit() {
        return sync.lastRateLimit();
    }

    @Override
    public String toString() {
        return "TxApiAsync{" + sync + "}";
    }
}
