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
 * Resource commands ({@code /server/resource/…}). Resource names are validated by the API ({@code [A-Za-z0-9_.-]}, at most 128 characters).
 *
 * <p>Obtain it from {@code client.serverResources()}; do not construct it yourself.
 * Every method has an overload with a trailing {@link RequestOptions} for per-call settings
 * such as another {@code serverId}.
 *
 * @see AsyncServerResources
 */
@SuppressWarnings("unused")
public final class ServerResources {
    private final Invoker inv;

    /**
     * Internal: created by the client.
     *
     * @param invoker internal call runner
     */
    public ServerResources(Invoker invoker) {
        this.inv = invoker;

    }

    /**
     * Starts a resource. txAdmin only forwards the command; FXServer does not confirm it.
     *
     * <p>{@code POST /api/v1/server/resource/start} · permission {@code server.control}
     *
     * @param name resource name, at most 128 characters
     * @return {@code success} and txAdmin's message
     * @throws cloud.txapi.sdk.errors.ValidationException if a required value is missing or blank, or too long (no request is sent)
     * @throws cloud.txapi.sdk.errors.TxApiException on API errors (see {@link cloud.txapi.sdk.errors}) and transport problems
     */
    public ActionResult start(String name) {
        return inv.call(Calls.serverResources_start(name), null);
    }

    /**
     * Starts a resource. txAdmin only forwards the command; FXServer does not confirm it.
     *
     * <p>{@code POST /api/v1/server/resource/start} · permission {@code server.control}
     *
     * @param name resource name, at most 128 characters
     * @param options per-call options (server, timeout, retries); {@code null} = defaults of the client/view
     * @return {@code success} and txAdmin's message
     * @throws cloud.txapi.sdk.errors.ValidationException if a required value is missing or blank, or too long (no request is sent)
     * @throws cloud.txapi.sdk.errors.TxApiException on API errors (see {@link cloud.txapi.sdk.errors}) and transport problems
     */
    public ActionResult start(String name, RequestOptions options) {
        return inv.call(Calls.serverResources_start(name), options);
    }

    /**
     * Stops a resource. txAdmin only forwards the command; FXServer does not confirm it.
     *
     * <p>{@code POST /api/v1/server/resource/stop} · permission {@code server.control}
     *
     * @param name resource name, at most 128 characters
     * @return {@code success} and txAdmin's message
     * @throws cloud.txapi.sdk.errors.ValidationException if a required value is missing or blank, or too long (no request is sent)
     * @throws cloud.txapi.sdk.errors.TxApiException on API errors (see {@link cloud.txapi.sdk.errors}) and transport problems
     */
    public ActionResult stop(String name) {
        return inv.call(Calls.serverResources_stop(name), null);
    }

    /**
     * Stops a resource. txAdmin only forwards the command; FXServer does not confirm it.
     *
     * <p>{@code POST /api/v1/server/resource/stop} · permission {@code server.control}
     *
     * @param name resource name, at most 128 characters
     * @param options per-call options (server, timeout, retries); {@code null} = defaults of the client/view
     * @return {@code success} and txAdmin's message
     * @throws cloud.txapi.sdk.errors.ValidationException if a required value is missing or blank, or too long (no request is sent)
     * @throws cloud.txapi.sdk.errors.TxApiException on API errors (see {@link cloud.txapi.sdk.errors}) and transport problems
     */
    public ActionResult stop(String name, RequestOptions options) {
        return inv.call(Calls.serverResources_stop(name), options);
    }

    /**
     * Restarts a resource. txAdmin only forwards the command; FXServer does not confirm it.
     *
     * <p>{@code POST /api/v1/server/resource/restart} · permission {@code server.control}
     *
     * @param name resource name, at most 128 characters
     * @return {@code success} and txAdmin's message
     * @throws cloud.txapi.sdk.errors.ValidationException if a required value is missing or blank, or too long (no request is sent)
     * @throws cloud.txapi.sdk.errors.TxApiException on API errors (see {@link cloud.txapi.sdk.errors}) and transport problems
     */
    public ActionResult restart(String name) {
        return inv.call(Calls.serverResources_restart(name), null);
    }

    /**
     * Restarts a resource. txAdmin only forwards the command; FXServer does not confirm it.
     *
     * <p>{@code POST /api/v1/server/resource/restart} · permission {@code server.control}
     *
     * @param name resource name, at most 128 characters
     * @param options per-call options (server, timeout, retries); {@code null} = defaults of the client/view
     * @return {@code success} and txAdmin's message
     * @throws cloud.txapi.sdk.errors.ValidationException if a required value is missing or blank, or too long (no request is sent)
     * @throws cloud.txapi.sdk.errors.TxApiException on API errors (see {@link cloud.txapi.sdk.errors}) and transport problems
     */
    public ActionResult restart(String name, RequestOptions options) {
        return inv.call(Calls.serverResources_restart(name), options);
    }

    /**
     * Starts a resource, or restarts it if it runs. txAdmin only forwards the command; FXServer does not confirm it.
     *
     * <p>{@code POST /api/v1/server/resource/ensure} · permission {@code server.control}
     *
     * @param name resource name, at most 128 characters
     * @return {@code success} and txAdmin's message
     * @throws cloud.txapi.sdk.errors.ValidationException if a required value is missing or blank, or too long (no request is sent)
     * @throws cloud.txapi.sdk.errors.TxApiException on API errors (see {@link cloud.txapi.sdk.errors}) and transport problems
     */
    public ActionResult ensure(String name) {
        return inv.call(Calls.serverResources_ensure(name), null);
    }

    /**
     * Starts a resource, or restarts it if it runs. txAdmin only forwards the command; FXServer does not confirm it.
     *
     * <p>{@code POST /api/v1/server/resource/ensure} · permission {@code server.control}
     *
     * @param name resource name, at most 128 characters
     * @param options per-call options (server, timeout, retries); {@code null} = defaults of the client/view
     * @return {@code success} and txAdmin's message
     * @throws cloud.txapi.sdk.errors.ValidationException if a required value is missing or blank, or too long (no request is sent)
     * @throws cloud.txapi.sdk.errors.TxApiException on API errors (see {@link cloud.txapi.sdk.errors}) and transport problems
     */
    public ActionResult ensure(String name, RequestOptions options) {
        return inv.call(Calls.serverResources_ensure(name), options);
    }

    /**
     * Rescans the resource folders ({@code refresh}).
     *
     * <p>{@code POST /api/v1/server/resource/refresh} · permission {@code server.control}
     *
     * @return {@code success} and txAdmin's message
     * @throws cloud.txapi.sdk.errors.TxApiException on API errors (see {@link cloud.txapi.sdk.errors}) and transport problems
     */
    public ActionResult refresh() {
        return inv.call(Calls.serverResources_refresh(), null);
    }

    /**
     * Rescans the resource folders ({@code refresh}).
     *
     * <p>{@code POST /api/v1/server/resource/refresh} · permission {@code server.control}
     *
     * @param options per-call options (server, timeout, retries); {@code null} = defaults of the client/view
     * @return {@code success} and txAdmin's message
     * @throws cloud.txapi.sdk.errors.TxApiException on API errors (see {@link cloud.txapi.sdk.errors}) and transport problems
     */
    public ActionResult refresh(RequestOptions options) {
        return inv.call(Calls.serverResources_refresh(), options);
    }
}
