package cloud.txapi.sdk.resources;

import cloud.txapi.sdk.AsyncPageIterable;
import cloud.txapi.sdk.PageIterable;
import cloud.txapi.sdk.RequestOptions;
import cloud.txapi.sdk.internal.Call;
import cloud.txapi.sdk.internal.Endpoint;
import cloud.txapi.sdk.internal.Invoker;
import cloud.txapi.sdk.internal.Pager;
import cloud.txapi.sdk.internal.Tokens;
import cloud.txapi.sdk.live.*;
import cloud.txapi.sdk.model.*;
import cloud.txapi.sdk.params.*;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;

import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * Resource commands ({@code /server/resource/…}). Resource names are validated by the API ({@code [A-Za-z0-9_.-]}, at most 128 characters). Every method returns a {@link CompletableFuture} and never blocks the caller.
 *
 * <p>Obtain it from {@code client.async().serverResources()}; do not construct it yourself.
 * Every method has an overload with a trailing {@link RequestOptions} for per-call settings
 * such as another {@code serverId}.
 *
 * @see ServerResources
 */
@SuppressWarnings("unused")
public final class AsyncServerResources {
    private final Invoker inv;

    /**
     * Internal: created by the client.
     *
     * @param invoker internal call runner
     */
    public AsyncServerResources(Invoker invoker) {
        this.inv = invoker;

    }

    /**
     * Starts a resource. txAdmin only forwards the command; FXServer does not confirm it.
     *
     * <p>{@code POST /api/v1/server/resource/start} · permission {@code server.control}
     *
     * <p>Asynchronous variant of {@link ServerResources#start}; errors complete the future exceptionally.
     *
     * @param name resource name, at most 128 characters
     * @return a future completing with {@code success} and txAdmin's message
     */
    public CompletableFuture<ActionResult> start(String name) {
        return inv.callAsync(Calls.serverResources_start(name), null);
    }

    /**
     * Starts a resource. txAdmin only forwards the command; FXServer does not confirm it.
     *
     * <p>{@code POST /api/v1/server/resource/start} · permission {@code server.control}
     *
     * <p>Asynchronous variant of {@link ServerResources#start}; errors complete the future exceptionally.
     *
     * @param name resource name, at most 128 characters
     * @param options per-call options (server, timeout, retries); {@code null} = defaults of the client/view
     * @return a future completing with {@code success} and txAdmin's message
     */
    public CompletableFuture<ActionResult> start(String name, RequestOptions options) {
        return inv.callAsync(Calls.serverResources_start(name), options);
    }

    /**
     * Stops a resource. txAdmin only forwards the command; FXServer does not confirm it.
     *
     * <p>{@code POST /api/v1/server/resource/stop} · permission {@code server.control}
     *
     * <p>Asynchronous variant of {@link ServerResources#stop}; errors complete the future exceptionally.
     *
     * @param name resource name, at most 128 characters
     * @return a future completing with {@code success} and txAdmin's message
     */
    public CompletableFuture<ActionResult> stop(String name) {
        return inv.callAsync(Calls.serverResources_stop(name), null);
    }

    /**
     * Stops a resource. txAdmin only forwards the command; FXServer does not confirm it.
     *
     * <p>{@code POST /api/v1/server/resource/stop} · permission {@code server.control}
     *
     * <p>Asynchronous variant of {@link ServerResources#stop}; errors complete the future exceptionally.
     *
     * @param name resource name, at most 128 characters
     * @param options per-call options (server, timeout, retries); {@code null} = defaults of the client/view
     * @return a future completing with {@code success} and txAdmin's message
     */
    public CompletableFuture<ActionResult> stop(String name, RequestOptions options) {
        return inv.callAsync(Calls.serverResources_stop(name), options);
    }

    /**
     * Restarts a resource. txAdmin only forwards the command; FXServer does not confirm it.
     *
     * <p>{@code POST /api/v1/server/resource/restart} · permission {@code server.control}
     *
     * <p>Asynchronous variant of {@link ServerResources#restart}; errors complete the future exceptionally.
     *
     * @param name resource name, at most 128 characters
     * @return a future completing with {@code success} and txAdmin's message
     */
    public CompletableFuture<ActionResult> restart(String name) {
        return inv.callAsync(Calls.serverResources_restart(name), null);
    }

    /**
     * Restarts a resource. txAdmin only forwards the command; FXServer does not confirm it.
     *
     * <p>{@code POST /api/v1/server/resource/restart} · permission {@code server.control}
     *
     * <p>Asynchronous variant of {@link ServerResources#restart}; errors complete the future exceptionally.
     *
     * @param name resource name, at most 128 characters
     * @param options per-call options (server, timeout, retries); {@code null} = defaults of the client/view
     * @return a future completing with {@code success} and txAdmin's message
     */
    public CompletableFuture<ActionResult> restart(String name, RequestOptions options) {
        return inv.callAsync(Calls.serverResources_restart(name), options);
    }

    /**
     * Starts a resource, or restarts it if it runs. txAdmin only forwards the command; FXServer does not confirm it.
     *
     * <p>{@code POST /api/v1/server/resource/ensure} · permission {@code server.control}
     *
     * <p>Asynchronous variant of {@link ServerResources#ensure}; errors complete the future exceptionally.
     *
     * @param name resource name, at most 128 characters
     * @return a future completing with {@code success} and txAdmin's message
     */
    public CompletableFuture<ActionResult> ensure(String name) {
        return inv.callAsync(Calls.serverResources_ensure(name), null);
    }

    /**
     * Starts a resource, or restarts it if it runs. txAdmin only forwards the command; FXServer does not confirm it.
     *
     * <p>{@code POST /api/v1/server/resource/ensure} · permission {@code server.control}
     *
     * <p>Asynchronous variant of {@link ServerResources#ensure}; errors complete the future exceptionally.
     *
     * @param name resource name, at most 128 characters
     * @param options per-call options (server, timeout, retries); {@code null} = defaults of the client/view
     * @return a future completing with {@code success} and txAdmin's message
     */
    public CompletableFuture<ActionResult> ensure(String name, RequestOptions options) {
        return inv.callAsync(Calls.serverResources_ensure(name), options);
    }

    /**
     * Rescans the resource folders ({@code refresh}).
     *
     * <p>{@code POST /api/v1/server/resource/refresh} · permission {@code server.control}
     *
     * <p>Asynchronous variant of {@link ServerResources#refresh}; errors complete the future exceptionally.
     *
     * @return a future completing with {@code success} and txAdmin's message
     */
    public CompletableFuture<ActionResult> refresh() {
        return inv.callAsync(Calls.serverResources_refresh(), null);
    }

    /**
     * Rescans the resource folders ({@code refresh}).
     *
     * <p>{@code POST /api/v1/server/resource/refresh} · permission {@code server.control}
     *
     * <p>Asynchronous variant of {@link ServerResources#refresh}; errors complete the future exceptionally.
     *
     * @param options per-call options (server, timeout, retries); {@code null} = defaults of the client/view
     * @return a future completing with {@code success} and txAdmin's message
     */
    public CompletableFuture<ActionResult> refresh(RequestOptions options) {
        return inv.callAsync(Calls.serverResources_refresh(), options);
    }
}
