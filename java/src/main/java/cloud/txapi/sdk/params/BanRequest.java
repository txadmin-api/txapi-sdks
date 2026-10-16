package cloud.txapi.sdk.params;

/**
 * Body of {@code POST /players/ban}. Absent ({@code null}) fields are not sent.
 *
 * <pre>{@code
 * BanRequest ban = BanRequest.of("license:abc123", "Cheating")
 *         .withDurationHours(72)      // omit for a permanent ban
 *         .withDiscordBan(true);      // also ban on linked Discord servers (API 1.8.0)
 * }</pre>
 *
 * @param identifier    player identifier ({@code license:…}, {@code discord:…}, {@code fivem:…},
 *                      {@code steam:…}, …); required
 * @param reason        ban reason, at most 512 characters; required
 * @param durationHours ban length in hours; {@code null} = permanent
 * @param discordBan    also ban the player on every Discord server whose txBot is linked to this
 *                      txAdmin server; {@code null} = API default (off)
 */
public record BanRequest(String identifier, String reason, Integer durationHours, Boolean discordBan) {

    /**
     * @param identifier player identifier
     * @param reason     ban reason
     * @return a permanent ban without Discord ban
     */
    public static BanRequest of(String identifier, String reason) {
        return new BanRequest(identifier, reason, null, null);
    }

    /** @param hours ban length; {@code null} = permanent @return a copy */
    public BanRequest withDurationHours(Integer hours) {
        return new BanRequest(identifier, reason, hours, discordBan);
    }

    /** @param ban also ban on linked Discord servers @return a copy */
    public BanRequest withDiscordBan(Boolean ban) {
        return new BanRequest(identifier, reason, durationHours, ban);
    }
}
