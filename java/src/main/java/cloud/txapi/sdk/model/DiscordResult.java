package cloud.txapi.sdk.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.List;

/**
 * Outcome of {@code discordBan} / {@code discordUnban} (API 1.8.0).
 *
 * @param discordId the Discord user the ban applied to, if known
 * @param skipped   set when nothing was attempted: {@code no_discord_id}, {@code no_linked_guild},
 *                  {@code not_a_ban}
 * @param message   human readable note, if any
 * @param guilds    one result per linked Discord server (empty when skipped)
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record DiscordResult(String discordId, String skipped, String message, List<DiscordGuildResult> guilds) {
    public DiscordResult {
        guilds = Lists.of(guilds);
    }

    /** {@code true} when the Discord part was not attempted at all. */
    public boolean isSkipped() {
        return skipped != null;
    }

    /** {@code true} when at least one guild was processed and every guild reported {@code ok}. */
    public boolean allOk() {
        return !guilds.isEmpty() && guilds.stream().allMatch(DiscordGuildResult::ok);
    }
}
