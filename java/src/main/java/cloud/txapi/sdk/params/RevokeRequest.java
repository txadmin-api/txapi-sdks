package cloud.txapi.sdk.params;

/**
 * Body of {@code POST /history/revoke/{actionId}}. Both fields are optional; absent fields are
 * not sent.
 *
 * @param reason       why the action is revoked
 * @param discordUnban also lift the Discord ban on linked Discord servers (API 1.8.0)
 */
public record RevokeRequest(String reason, Boolean discordUnban) {

    /** @return an empty request (no reason, no Discord unban) */
    public static RevokeRequest create() {
        return new RevokeRequest(null, null);
    }

    /** @param reason revoke reason @return a request with just a reason */
    public static RevokeRequest reason(String reason) {
        return new RevokeRequest(reason, null);
    }

    /** @param reason revoke reason @return a copy */
    public RevokeRequest withReason(String reason) {
        return new RevokeRequest(reason, discordUnban);
    }

    /** @param unban also lift the Discord ban @return a copy */
    public RevokeRequest withDiscordUnban(Boolean unban) {
        return new RevokeRequest(reason, unban);
    }
}
