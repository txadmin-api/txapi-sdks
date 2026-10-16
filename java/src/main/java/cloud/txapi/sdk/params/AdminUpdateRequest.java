package cloud.txapi.sdk.params;

import java.util.List;

/**
 * Body of {@code PUT /txadmin/admins/{name}}. <strong>Only the fields you set are changed</strong> —
 * every absent ({@code null}) field keeps its current value, so you can change one field without
 * touching the others.
 *
 * @param citizenfxId new Cfx.re account id, {@code null} = unchanged
 * @param discordId   new Discord user id, {@code null} = unchanged
 * @param permissions new permission list (replaces the old one), {@code null} = unchanged
 */
public record AdminUpdateRequest(String citizenfxId, String discordId, List<String> permissions) {

    /** Defensive copy of {@code permissions}. */
    public AdminUpdateRequest {
        permissions = permissions == null ? null : List.copyOf(permissions);
    }

    /** @return a request that changes nothing (add fields with the {@code with…} methods) */
    public static AdminUpdateRequest create() {
        return new AdminUpdateRequest(null, null, null);
    }

    /** @param id new Cfx.re account id @return a copy */
    public AdminUpdateRequest withCitizenfxId(String id) {
        return new AdminUpdateRequest(id, discordId, permissions);
    }

    /** @param id new Discord user id @return a copy */
    public AdminUpdateRequest withDiscordId(String id) {
        return new AdminUpdateRequest(citizenfxId, id, permissions);
    }

    /** @param list new permission list @return a copy */
    public AdminUpdateRequest withPermissions(List<String> list) {
        return new AdminUpdateRequest(citizenfxId, discordId, list);
    }
}
