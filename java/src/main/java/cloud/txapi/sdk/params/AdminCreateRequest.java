package cloud.txapi.sdk.params;

import java.util.List;

/**
 * Body of {@code POST /txadmin/admins}: creates a txAdmin panel admin. Absent fields are not sent.
 * Valid permission ids: {@code client.admins().permissions()}.
 *
 * @param name        admin name; required
 * @param citizenfxId Cfx.re account id (optional)
 * @param discordId   Discord user id (optional)
 * @param permissions permission ids, at least one; required
 */
public record AdminCreateRequest(String name, String citizenfxId, String discordId, List<String> permissions) {

    /** Defensive copy of {@code permissions}. */
    public AdminCreateRequest {
        permissions = permissions == null ? null : List.copyOf(permissions);
    }

    /**
     * @param name        admin name
     * @param permissions permission ids
     * @return a request without Cfx.re or Discord id
     */
    public static AdminCreateRequest of(String name, List<String> permissions) {
        return new AdminCreateRequest(name, null, null, permissions);
    }

    /** @param id Cfx.re account id @return a copy */
    public AdminCreateRequest withCitizenfxId(String id) {
        return new AdminCreateRequest(name, id, discordId, permissions);
    }

    /** @param id Discord user id @return a copy */
    public AdminCreateRequest withDiscordId(String id) {
        return new AdminCreateRequest(name, citizenfxId, id, permissions);
    }
}
