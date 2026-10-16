package cloud.txapi.sdk.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.List;

/**
 * One page (at most 100 rows) of bans and/or warnings.
 *
 * @param history       the rows
 * @param hasReachedEnd {@code true} on the last page
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record HistoryPage(List<HistoryEntry> history, boolean hasReachedEnd) {
    public HistoryPage {
        history = Lists.of(history);
    }
}
