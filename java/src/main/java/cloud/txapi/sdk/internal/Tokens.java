package cloud.txapi.sdk.internal;

import cloud.txapi.sdk.model.EventPage;
import cloud.txapi.sdk.model.HistoryEntry;
import cloud.txapi.sdk.model.HistoryPage;
import cloud.txapi.sdk.model.Player;
import cloud.txapi.sdk.model.PlayerPage;
import cloud.txapi.sdk.model.ServerEvent;
import cloud.txapi.sdk.params.PageCursor;
import cloud.txapi.sdk.params.PlayerSort;

/** Next-page tokens (SPEC §7). Internal API. */
public final class Tokens {
    private Tokens() {
    }

    /** Players: offset = sort-key value of the last row, cursor = its license. */
    public static Pager.Page<Player> players(PlayerPage page, PlayerSort sortKey) {
        if (page == null) {
            return new Pager.Page<>(null, null);
        }
        if (page.hasReachedEnd() || page.players().isEmpty()) {
            return new Pager.Page<>(page.players(), null);
        }
        Player last = page.players().get(page.players().size() - 1);
        Long offset = switch (sortKey == null ? PlayerSort.PLAY_TIME : sortKey) {
            case PLAY_TIME -> last.playTime();
            case JOINED -> last.tsJoined();
            case LAST_CONNECTION -> last.tsLastConnection();
        };
        if (offset == null || last.license() == null || last.license().isBlank()) {
            return new Pager.Page<>(page.players(), null);
        }
        return new Pager.Page<>(page.players(), new PageCursor(offset, last.license()));
    }

    /** History: offset = timestamp of the last row, cursor = its action id. */
    public static Pager.Page<HistoryEntry> history(HistoryPage page) {
        if (page == null) {
            return new Pager.Page<>(null, null);
        }
        if (page.hasReachedEnd() || page.history().isEmpty()) {
            return new Pager.Page<>(page.history(), null);
        }
        HistoryEntry last = page.history().get(page.history().size() - 1);
        if (last.timestamp() == null || last.id() == null || last.id().isBlank()) {
            return new Pager.Page<>(page.history(), null);
        }
        return new Pager.Page<>(page.history(), new PageCursor(last.timestamp(), last.id()));
    }

    /** Events: cursor = {@code nextCursor}; stop when it is absent. */
    public static Pager.Page<ServerEvent> events(EventPage page) {
        if (page == null) {
            return new Pager.Page<>(null, null);
        }
        String next = page.nextCursor();
        return new Pager.Page<>(page.events(), next == null || next.isBlank() ? null : next);
    }
}
