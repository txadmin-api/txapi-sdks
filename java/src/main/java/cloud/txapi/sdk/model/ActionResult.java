package cloud.txapi.sdk.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.Optional;

/**
 * Result of a mutation ({@code { success, message, discord? }}).
 *
 * @param success {@code true} when txAdmin accepted the action
 * @param message txAdmin's own confirmation text (or a short default)
 * @param discord Discord ban/unban result — only present when {@code discordBan} /
 *                {@code discordUnban} was requested
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record ActionResult(boolean success, String message, DiscordResult discord) {

    /** The Discord result, if one was requested. */
    public Optional<DiscordResult> discordResult() {
        return Optional.ofNullable(discord);
    }
}
