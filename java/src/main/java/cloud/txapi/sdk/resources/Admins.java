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
 * txAdmin panel admins ({@code /txadmin/admins/…}). The master admin cannot be read or changed through the API.
 *
 * <p>Obtain it from {@code client.admins()}; do not construct it yourself.
 * Every method has an overload with a trailing {@link RequestOptions} for per-call settings
 * such as another {@code serverId}.
 *
 * @see AsyncAdmins
 */
@SuppressWarnings("unused")
public final class Admins {
    private final Invoker inv;

    /**
     * Internal: created by the client.
     *
     * @param invoker internal call runner
     */
    public Admins(Invoker invoker) {
        this.inv = invoker;

    }

    /**
     * Lists the panel admins with a permission summary.
     *
     * <p>{@code GET /api/v1/txadmin/admins} · permission {@code admin.manage}
     *
     * @return the admins
     * @throws cloud.txapi.sdk.errors.TxApiException on API errors (see {@link cloud.txapi.sdk.errors}) and transport problems
     */
    public AdminList list() {
        return inv.call(Calls.admins_list(), null);
    }

    /**
     * Lists the panel admins with a permission summary.
     *
     * <p>{@code GET /api/v1/txadmin/admins} · permission {@code admin.manage}
     *
     * @param options per-call options (server, timeout, retries); {@code null} = defaults of the client/view
     * @return the admins
     * @throws cloud.txapi.sdk.errors.TxApiException on API errors (see {@link cloud.txapi.sdk.errors}) and transport problems
     */
    public AdminList list(RequestOptions options) {
        return inv.call(Calls.admins_list(), options);
    }

    /**
     * One admin with Cfx.re id, Discord id and permissions. The master admin answers 403.
     *
     * <p>{@code GET /api/v1/txadmin/admins/{name}} · permission {@code admin.manage}
     *
     * @param name admin name (may contain spaces)
     * @return the admin
     * @throws cloud.txapi.sdk.errors.ValidationException if a required value is missing or blank, or too long (no request is sent)
     * @throws cloud.txapi.sdk.errors.TxApiException on API errors (see {@link cloud.txapi.sdk.errors}) and transport problems
     */
    public AdminDetails get(String name) {
        return inv.call(Calls.admins_get(name), null);
    }

    /**
     * One admin with Cfx.re id, Discord id and permissions. The master admin answers 403.
     *
     * <p>{@code GET /api/v1/txadmin/admins/{name}} · permission {@code admin.manage}
     *
     * @param name admin name (may contain spaces)
     * @param options per-call options (server, timeout, retries); {@code null} = defaults of the client/view
     * @return the admin
     * @throws cloud.txapi.sdk.errors.ValidationException if a required value is missing or blank, or too long (no request is sent)
     * @throws cloud.txapi.sdk.errors.TxApiException on API errors (see {@link cloud.txapi.sdk.errors}) and transport problems
     */
    public AdminDetails get(String name, RequestOptions options) {
        return inv.call(Calls.admins_get(name), options);
    }

    /**
     * Every permission id txAdmin knows, with a label.
     *
     * <p>{@code GET /api/v1/txadmin/admins/available-permissions} · permission {@code admin.manage}
     *
     * @return the permissions
     * @throws cloud.txapi.sdk.errors.TxApiException on API errors (see {@link cloud.txapi.sdk.errors}) and transport problems
     */
    public AdminPermissions permissions() {
        return inv.call(Calls.admins_permissions(), null);
    }

    /**
     * Every permission id txAdmin knows, with a label.
     *
     * <p>{@code GET /api/v1/txadmin/admins/available-permissions} · permission {@code admin.manage}
     *
     * @param options per-call options (server, timeout, retries); {@code null} = defaults of the client/view
     * @return the permissions
     * @throws cloud.txapi.sdk.errors.TxApiException on API errors (see {@link cloud.txapi.sdk.errors}) and transport problems
     */
    public AdminPermissions permissions(RequestOptions options) {
        return inv.call(Calls.admins_permissions(), options);
    }

    /**
     * Creates a panel admin. txAdmin generates a password and returns it <strong>once</strong> ({@link AdminCreated#generatedPassword()}).
     *
     * <p>{@code POST /api/v1/txadmin/admins} · permission {@code admin.manage}
     *
     * @param request name, permissions and optional Cfx.re / Discord id
     * @return the result with the generated password
     * @throws cloud.txapi.sdk.errors.ValidationException if a required value is missing or blank, or too long (no request is sent)
     * @throws cloud.txapi.sdk.errors.TxApiException on API errors (see {@link cloud.txapi.sdk.errors}) and transport problems
     */
    public AdminCreated add(AdminCreateRequest request) {
        return inv.call(Calls.admins_add1(request), null);
    }

    /**
     * Creates a panel admin. txAdmin generates a password and returns it <strong>once</strong> ({@link AdminCreated#generatedPassword()}).
     *
     * <p>{@code POST /api/v1/txadmin/admins} · permission {@code admin.manage}
     *
     * @param request name, permissions and optional Cfx.re / Discord id
     * @param options per-call options (server, timeout, retries); {@code null} = defaults of the client/view
     * @return the result with the generated password
     * @throws cloud.txapi.sdk.errors.ValidationException if a required value is missing or blank, or too long (no request is sent)
     * @throws cloud.txapi.sdk.errors.TxApiException on API errors (see {@link cloud.txapi.sdk.errors}) and transport problems
     */
    public AdminCreated add(AdminCreateRequest request, RequestOptions options) {
        return inv.call(Calls.admins_add1(request), options);
    }

    /**
     * Creates a panel admin with just a name and permissions. txAdmin generates a password and returns it <strong>once</strong>.
     *
     * <p>{@code POST /api/v1/txadmin/admins} · permission {@code admin.manage}
     *
     * @param name admin name (may contain spaces)
     * @param permissions permission ids (see {@link #permissions()}), at least one
     * @return the result with the generated password
     * @throws cloud.txapi.sdk.errors.ValidationException if a required value is missing or blank, or too long (no request is sent)
     * @throws cloud.txapi.sdk.errors.TxApiException on API errors (see {@link cloud.txapi.sdk.errors}) and transport problems
     */
    public AdminCreated add(String name, List<String> permissions) {
        return inv.call(Calls.admins_add2(name, permissions), null);
    }

    /**
     * Creates a panel admin with just a name and permissions. txAdmin generates a password and returns it <strong>once</strong>.
     *
     * <p>{@code POST /api/v1/txadmin/admins} · permission {@code admin.manage}
     *
     * @param name admin name (may contain spaces)
     * @param permissions permission ids (see {@link #permissions()}), at least one
     * @param options per-call options (server, timeout, retries); {@code null} = defaults of the client/view
     * @return the result with the generated password
     * @throws cloud.txapi.sdk.errors.ValidationException if a required value is missing or blank, or too long (no request is sent)
     * @throws cloud.txapi.sdk.errors.TxApiException on API errors (see {@link cloud.txapi.sdk.errors}) and transport problems
     */
    public AdminCreated add(String name, List<String> permissions, RequestOptions options) {
        return inv.call(Calls.admins_add2(name, permissions), options);
    }

    /**
     * Updates an admin. <strong>Only the fields set in {@code request} change</strong>; everything else keeps its value.
     *
     * <p>{@code PUT /api/v1/txadmin/admins/{name}} · permission {@code admin.manage}
     *
     * @param name admin name (may contain spaces)
     * @param request the changes
     * @return {@code success} and txAdmin's message
     * @throws cloud.txapi.sdk.errors.ValidationException if a required value is missing or blank, or too long (no request is sent)
     * @throws cloud.txapi.sdk.errors.TxApiException on API errors (see {@link cloud.txapi.sdk.errors}) and transport problems
     */
    public ActionResult edit(String name, AdminUpdateRequest request) {
        return inv.call(Calls.admins_edit(name, request), null);
    }

    /**
     * Updates an admin. <strong>Only the fields set in {@code request} change</strong>; everything else keeps its value.
     *
     * <p>{@code PUT /api/v1/txadmin/admins/{name}} · permission {@code admin.manage}
     *
     * @param name admin name (may contain spaces)
     * @param request the changes
     * @param options per-call options (server, timeout, retries); {@code null} = defaults of the client/view
     * @return {@code success} and txAdmin's message
     * @throws cloud.txapi.sdk.errors.ValidationException if a required value is missing or blank, or too long (no request is sent)
     * @throws cloud.txapi.sdk.errors.TxApiException on API errors (see {@link cloud.txapi.sdk.errors}) and transport problems
     */
    public ActionResult edit(String name, AdminUpdateRequest request, RequestOptions options) {
        return inv.call(Calls.admins_edit(name, request), options);
    }

    /**
     * Removes a panel admin.
     *
     * <p>{@code DELETE /api/v1/txadmin/admins/{name}} · permission {@code admin.manage}
     *
     * @param name admin name (may contain spaces)
     * @return {@code success} and txAdmin's message
     * @throws cloud.txapi.sdk.errors.ValidationException if a required value is missing or blank, or too long (no request is sent)
     * @throws cloud.txapi.sdk.errors.TxApiException on API errors (see {@link cloud.txapi.sdk.errors}) and transport problems
     */
    public ActionResult delete(String name) {
        return inv.call(Calls.admins_delete(name), null);
    }

    /**
     * Removes a panel admin.
     *
     * <p>{@code DELETE /api/v1/txadmin/admins/{name}} · permission {@code admin.manage}
     *
     * @param name admin name (may contain spaces)
     * @param options per-call options (server, timeout, retries); {@code null} = defaults of the client/view
     * @return {@code success} and txAdmin's message
     * @throws cloud.txapi.sdk.errors.ValidationException if a required value is missing or blank, or too long (no request is sent)
     * @throws cloud.txapi.sdk.errors.TxApiException on API errors (see {@link cloud.txapi.sdk.errors}) and transport problems
     */
    public ActionResult delete(String name, RequestOptions options) {
        return inv.call(Calls.admins_delete(name), options);
    }
}
