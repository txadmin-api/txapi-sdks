package cloud.txapi.sdk.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * Result on one Discord server.
 *
 * @param guildId   Discord server id
 * @param guildName Discord server name
 * @param ok        whether the ban/unban worked there
 * @param code      {@code banned}, {@code unbanned}, {@code not_banned}, {@code disabled},
 *                  {@code missing_permission}, {@code hierarchy}, {@code owner}, {@code unknown_user},
 *                  {@code invalid_user}, {@code not_in_guild}, {@code bot_unavailable}, {@code failed}
 * @param message   explanation when not ok
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record DiscordGuildResult(String guildId, String guildName, boolean ok, String code, String message) {
}
