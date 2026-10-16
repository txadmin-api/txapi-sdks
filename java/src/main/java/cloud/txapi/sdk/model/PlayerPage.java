package cloud.txapi.sdk.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.List;

/**
 * One page (at most 100 rows) of a player list.
 *
 * @param players       the rows
 * @param hasReachedEnd {@code true} on the last page
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record PlayerPage(List<Player> players, boolean hasReachedEnd) {
    public PlayerPage {
        players = Lists.of(players);
    }
}
