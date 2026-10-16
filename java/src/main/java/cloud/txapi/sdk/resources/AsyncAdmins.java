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
 * txAdmin panel admins ({@code /txadmin/admins/…}). The master admin cannot be read or changed through the API. Every method returns a {@link CompletableFuture} and never blocks the caller.
 *
 * <p>Obtain it from {@code client.async().admins()}; do not construct it yourself.
 * Every method has an overload with a trailing {@link RequestOptions} for per-call settings
 * such as another {@code serverId}.
 *
 * @see Admins
 */
@SuppressWarnings("unused")
public final class AsyncAdmins {
    private final Invoker inv;

    /**
     * Internal: created by the client.
     *
     * @param invoker internal call runner
     */
    public AsyncAdmins(Invoker invoker) {
        this.inv = invoker;

    }

    /**
     * Lists the panel admins with a permission summary.
     *
     * <p>{@code GET /api/v1/txadmin/admins} · permission {@code admin.manage}
     *
     * <p>Asynchronous variant of {@link Admins#list}; errors complete the future exceptionally.
     *
     * @return a future completing with the admins
     */
    public CompletableFuture<AdminList> list() {
        return inv.callAsync(Calls.admins_list(), null);
    }

    /**
     * Lists the panel admins with a permission summary.
     *
     * <p>{@code GET /api/v1/txadmin/admins} · permission {@code admin.manage}
     *
     * <p>Asynchronous variant of {@link Admins#list}; errors complete the future exceptionally.
     *
     * @param options per-call options (server, timeout, retries); {@code null} = defaults of the client/view
     * @return a future completing with the admins
     */
    public CompletableFuture<AdminList> list(RequestOptions options) {
        return inv.callAsync(Calls.admins_list(), options);
    }

    /**
     * One admin with Cfx.re id, Discord id and permissions. The master admin answers 403.
     *
     * <p>{@code GET /api/v1/txadmin/admins/{name}} · permission {@code admin.manage}
     *
     * <p>Asynchronous variant of {@link Admins#get}; errors complete the future exceptionally.
     *
     * @param name admin name (may contain spaces)
     * @return a future completing with the admin
     */
    public CompletableFuture<AdminDetails> get(String name) {
        return inv.callAsync(Calls.admins_get(name), null);
    }

    /**
     * One admin with Cfx.re id, Discord id and permissions. The master admin answers 403.
     *
     * <p>{@code GET /api/v1/txadmin/admins/{name}} · permission {@code admin.manage}
     *
     * <p>Asynchronous variant of {@link Admins#get}; errors complete the future exceptionally.
     *
     * @param name admin name (may contain spaces)
     * @param options per-call options (server, timeout, retries); {@code null} = defaults of the client/view
     * @return a future completing with the admin
     */
    public CompletableFuture<AdminDetails> get(String name, RequestOptions options) {
        return inv.callAsync(Calls.admins_get(name), options);
    }

    /**
     * Every permission id txAdmin knows, with a label.
     *
     * <p>{@code GET /api/v1/txadmin/admins/available-permissions} · permission {@code admin.manage}
     *
     * <p>Asynchronous variant of {@link Admins#permissions}; errors complete the future exceptionally.
     *
     * @return a future completing with the permissions
     */
    public CompletableFuture<AdminPermissions> permissions() {
        return inv.callAsync(Calls.admins_permissions(), null);
    }

    /**
     * Every permission id txAdmin knows, with a label.
     *
     * <p>{@code GET /api/v1/txadmin/admins/available-permissions} · permission {@code admin.manage}
     *
     * <p>Asynchronous variant of {@link Admins#permissions}; errors complete the future exceptionally.
     *
     * @param options per-call options (server, timeout, retries); {@code null} = defaults of the client/view
     * @return a future completing with the permissions
     */
    public CompletableFuture<AdminPermissions> permissions(RequestOptions options) {
        return inv.callAsync(Calls.admins_permissions(), options);
    }

    /**
     * Creates a panel admin. txAdmin generates a password and returns it <strong>once</strong> ({@link AdminCreated#generatedPassword()}).
     *
     * <p>{@code POST /api/v1/txadmin/admins} · permission {@code admin.manage}
     *
     * <p>Asynchronous variant of {@link Admins#add}; errors complete the future exceptionally.
     *
     * @param request name, permissions and optional Cfx.re / Discord id
     * @return a future completing with the result with the generated password
     */
    public CompletableFuture<AdminCreated> add(AdminCreateRequest request) {
        return inv.callAsync(Calls.admins_add1(request), null);
    }

    /**
     * Creates a panel admin. txAdmin generates a password and returns it <strong>once</strong> ({@link AdminCreated#generatedPassword()}).
     *
     * <p>{@code POST /api/v1/txadmin/admins} · permission {@code admin.manage}
     *
     * <p>Asynchronous variant of {@link Admins#add}; errors complete the future exceptionally.
     *
     * @param request name, permissions and optional Cfx.re / Discord id
     * @param options per-call options (server, timeout, retries); {@code null} = defaults of the client/view
     * @return a future completing with the result with the generated password
     */
    public CompletableFuture<AdminCreated> add(AdminCreateRequest request, RequestOptions options) {
        return inv.callAsync(Calls.admins_add1(request), options);
    }

    /**
     * Creates a panel admin with just a name and permissions. txAdmin generates a password and returns it <strong>once</strong>.
     *
     * <p>{@code POST /api/v1/txadmin/admins} · permission {@code admin.manage}
     *
     * <p>Asynchronous variant of {@link Admins#add}; errors complete the future exceptionally.
     *
     * @param name admin name (may contain spaces)
     * @param permissions permission ids (see {@link #permissions()}), at least one
     * @return a future completing with the result with the generated password
     */
    public CompletableFuture<AdminCreated> add(String name, List<String> permissions) {
        return inv.callAsync(Calls.admins_add2(name, permissions), null);
    }

    /**
     * Creates a panel admin with just a name and permissions. txAdmin generates a password and returns it <strong>once</strong>.
     *
     * <p>{@code POST /api/v1/txadmin/admins} · permission {@code admin.manage}
     *
     * <p>Asynchronous variant of {@link Admins#add}; errors complete the future exceptionally.
     *
     * @param name admin name (may contain spaces)
     * @param permissions permission ids (see {@link #permissions()}), at least one
     * @param options per-call options (server, timeout, retries); {@code null} = defaults of the client/view
     * @return a future completing with the result with the generated password
     */
    public CompletableFuture<AdminCreated> add(String name, List<String> permissions, RequestOptions options) {
        return inv.callAsync(Calls.admins_add2(name, permissions), options);
    }

    /**
     * Updates an admin. <strong>Only the fields set in {@code request} change</strong>; everything else keeps its value.
     *
     * <p>{@code PUT /api/v1/txadmin/admins/{name}} · permission {@code admin.manage}
     *
     * <p>Asynchronous variant of {@link Admins#edit}; errors complete the future exceptionally.
     *
     * @param name admin name (may contain spaces)
     * @param request the changes
     * @return a future completing with {@code success} and txAdmin's message
     */
    public CompletableFuture<ActionResult> edit(String name, AdminUpdateRequest request) {
        return inv.callAsync(Calls.admins_edit(name, request), null);
    }

    /**
     * Updates an admin. <strong>Only the fields set in {@code request} change</strong>; everything else keeps its value.
     *
     * <p>{@code PUT /api/v1/txadmin/admins/{name}} · permission {@code admin.manage}
     *
     * <p>Asynchronous variant of {@link Admins#edit}; errors complete the future exceptionally.
     *
     * @param name admin name (may contain spaces)
     * @param request the changes
     * @param options per-call options (server, timeout, retries); {@code null} = defaults of the client/view
     * @return a future completing with {@code success} and txAdmin's message
     */
    public CompletableFuture<ActionResult> edit(String name, AdminUpdateRequest request, RequestOptions options) {
        return inv.callAsync(Calls.admins_edit(name, request), options);
    }

    /**
     * Removes a panel admin.
     *
     * <p>{@code DELETE /api/v1/txadmin/admins/{name}} · permission {@code admin.manage}
     *
     * <p>Asynchronous variant of {@link Admins#delete}; errors complete the future exceptionally.
     *
     * @param name admin name (may contain spaces)
     * @return a future completing with {@code success} and txAdmin's message
     */
    public CompletableFuture<ActionResult> delete(String name) {
        return inv.callAsync(Calls.admins_delete(name), null);
    }

    /**
     * Removes a panel admin.
     *
     * <p>{@code DELETE /api/v1/txadmin/admins/{name}} · permission {@code admin.manage}
     *
     * <p>Asynchronous variant of {@link Admins#delete}; errors complete the future exceptionally.
     *
     * @param name admin name (may contain spaces)
     * @param options per-call options (server, timeout, retries); {@code null} = defaults of the client/view
     * @return a future completing with {@code success} and txAdmin's message
     */
    public CompletableFuture<ActionResult> delete(String name, RequestOptions options) {
        return inv.callAsync(Calls.admins_delete(name), options);
    }
}
