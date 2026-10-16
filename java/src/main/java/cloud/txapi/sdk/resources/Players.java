package cloud.txapi.sdk.resources;

import cloud.txapi.sdk.AsyncPageIterable;
import cloud.txapi.sdk.PageIterable;
import cloud.txapi.sdk.RequestOptions;
import cloud.txapi.sdk.internal.Call;
import cloud.txapi.sdk.internal.Endpoint;
import cloud.txapi.sdk.internal.Invoker;
import cloud.txapi.sdk.internal.Pager;
import cloud.txapi.sdk.internal.Tokens;
import cloud.txapi.sdk.live.*;
import cloud.txapi.sdk.model.*;
import cloud.txapi.sdk.params.*;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;

import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * Player database, moderation and whitelist flags ({@code /players/…}).
 *
 * <p>Obtain it from {@code client.players()}; do not construct it yourself.
 * Every method has an overload with a trailing {@link RequestOptions} for per-call settings
 * such as another {@code serverId}.
 *
 * @see AsyncPlayers
 */
@SuppressWarnings("unused")
public final class Players {
    private final Invoker inv;

    /**
     * Internal: created by the client.
     *
     * @param invoker internal call runner
     */
    public Players(Invoker invoker) {
        this.inv = invoker;

    }

    /**
     * Searches txAdmin's player database (one page of at most 100 rows). Use {@link #searchAll()} to iterate over every page.
     *
     * <p>{@code GET /api/v1/players/search} · permission {@code players.read}
     *
     * @return one page of players
     * @throws cloud.txapi.sdk.errors.TxApiException on API errors (see {@link cloud.txapi.sdk.errors}) and transport problems
     */
    public PlayerPage search() {
        return inv.call(Calls.players_search0(), null);
    }

    /**
     * Searches txAdmin's player database (one page of at most 100 rows). Use {@link #searchAll()} to iterate over every page.
     *
     * <p>{@code GET /api/v1/players/search} · permission {@code players.read}
     *
     * @param options per-call options (server, timeout, retries); {@code null} = defaults of the client/view
     * @return one page of players
     * @throws cloud.txapi.sdk.errors.TxApiException on API errors (see {@link cloud.txapi.sdk.errors}) and transport problems
     */
    public PlayerPage search(RequestOptions options) {
        return inv.call(Calls.players_search0(), options);
    }

    /**
     * Searches txAdmin's player database (one page of at most 100 rows). Use {@link #searchAll(PlayerSearchParams)} to iterate over every page.
     *
     * <p>{@code GET /api/v1/players/search} · permission {@code players.read}
     *
     * @param params search parameters; {@code null} = all players by play time
     * @return one page of players
     * @throws cloud.txapi.sdk.errors.TxApiException on API errors (see {@link cloud.txapi.sdk.errors}) and transport problems
     */
    public PlayerPage search(PlayerSearchParams params) {
        return inv.call(Calls.players_search1(params), null);
    }

    /**
     * Searches txAdmin's player database (one page of at most 100 rows). Use {@link #searchAll(PlayerSearchParams)} to iterate over every page.
     *
     * <p>{@code GET /api/v1/players/search} · permission {@code players.read}
     *
     * @param params search parameters; {@code null} = all players by play time
     * @param options per-call options (server, timeout, retries); {@code null} = defaults of the client/view
     * @return one page of players
     * @throws cloud.txapi.sdk.errors.TxApiException on API errors (see {@link cloud.txapi.sdk.errors}) and transport problems
     */
    public PlayerPage search(PlayerSearchParams params, RequestOptions options) {
        return inv.call(Calls.players_search1(params), options);
    }

    /**
     * Players connected right now, sorted by play time (one page). Use {@link #onlineAll()} for all pages.
     *
     * <p>{@code GET /api/v1/players/online} · permission {@code players.read}
     *
     * @return one page of online players
     * @throws cloud.txapi.sdk.errors.TxApiException on API errors (see {@link cloud.txapi.sdk.errors}) and transport problems
     */
    public PlayerPage online() {
        return inv.call(Calls.players_online0(), null);
    }

    /**
     * Players connected right now, sorted by play time (one page). Use {@link #onlineAll()} for all pages.
     *
     * <p>{@code GET /api/v1/players/online} · permission {@code players.read}
     *
     * @param options per-call options (server, timeout, retries); {@code null} = defaults of the client/view
     * @return one page of online players
     * @throws cloud.txapi.sdk.errors.TxApiException on API errors (see {@link cloud.txapi.sdk.errors}) and transport problems
     */
    public PlayerPage online(RequestOptions options) {
        return inv.call(Calls.players_online0(), options);
    }

    /**
     * Players connected right now, sorted by play time, starting after {@code page}.
     *
     * <p>{@code GET /api/v1/players/online} · permission {@code players.read}
     *
     * @param page position after the last row of the previous page; {@code null} = first page
     * @return one page of online players
     * @throws cloud.txapi.sdk.errors.TxApiException on API errors (see {@link cloud.txapi.sdk.errors}) and transport problems
     */
    public PlayerPage online(PageCursor page) {
        return inv.call(Calls.players_online1(page), null);
    }

    /**
     * Players connected right now, sorted by play time, starting after {@code page}.
     *
     * <p>{@code GET /api/v1/players/online} · permission {@code players.read}
     *
     * @param page position after the last row of the previous page; {@code null} = first page
     * @param options per-call options (server, timeout, retries); {@code null} = defaults of the client/view
     * @return one page of online players
     * @throws cloud.txapi.sdk.errors.TxApiException on API errors (see {@link cloud.txapi.sdk.errors}) and transport problems
     */
    public PlayerPage online(PageCursor page, RequestOptions options) {
        return inv.call(Calls.players_online1(page), options);
    }

    /**
     * Finds every player record that carries the identifier (one page). Use {@link #lookupAll(String)} for all pages.
     *
     * <p>{@code GET /api/v1/players/lookup} · permission {@code players.read}
     *
     * @param identifier player identifier: {@code license:…}, {@code discord:…}, {@code fivem:…}, {@code steam:…} or a bare license hash
     * @return one page of players
     * @throws cloud.txapi.sdk.errors.ValidationException if a required value is missing or blank, or too long (no request is sent)
     * @throws cloud.txapi.sdk.errors.TxApiException on API errors (see {@link cloud.txapi.sdk.errors}) and transport problems
     */
    public PlayerPage lookup(String identifier) {
        return inv.call(Calls.players_lookup0(identifier), null);
    }

    /**
     * Finds every player record that carries the identifier (one page). Use {@link #lookupAll(String)} for all pages.
     *
     * <p>{@code GET /api/v1/players/lookup} · permission {@code players.read}
     *
     * @param identifier player identifier: {@code license:…}, {@code discord:…}, {@code fivem:…}, {@code steam:…} or a bare license hash
     * @param options per-call options (server, timeout, retries); {@code null} = defaults of the client/view
     * @return one page of players
     * @throws cloud.txapi.sdk.errors.ValidationException if a required value is missing or blank, or too long (no request is sent)
     * @throws cloud.txapi.sdk.errors.TxApiException on API errors (see {@link cloud.txapi.sdk.errors}) and transport problems
     */
    public PlayerPage lookup(String identifier, RequestOptions options) {
        return inv.call(Calls.players_lookup0(identifier), options);
    }

    /**
     * Finds every player record that carries the identifier, starting after {@code page}.
     *
     * <p>{@code GET /api/v1/players/lookup} · permission {@code players.read}
     *
     * @param identifier player identifier: {@code license:…}, {@code discord:…}, {@code fivem:…}, {@code steam:…} or a bare license hash
     * @param page position after the last row of the previous page; {@code null} = first page
     * @return one page of players
     * @throws cloud.txapi.sdk.errors.ValidationException if a required value is missing or blank, or too long (no request is sent)
     * @throws cloud.txapi.sdk.errors.TxApiException on API errors (see {@link cloud.txapi.sdk.errors}) and transport problems
     */
    public PlayerPage lookup(String identifier, PageCursor page) {
        return inv.call(Calls.players_lookup1(identifier, page), null);
    }

    /**
     * Finds every player record that carries the identifier, starting after {@code page}.
     *
     * <p>{@code GET /api/v1/players/lookup} · permission {@code players.read}
     *
     * @param identifier player identifier: {@code license:…}, {@code discord:…}, {@code fivem:…}, {@code steam:…} or a bare license hash
     * @param page position after the last row of the previous page; {@code null} = first page
     * @param options per-call options (server, timeout, retries); {@code null} = defaults of the client/view
     * @return one page of players
     * @throws cloud.txapi.sdk.errors.ValidationException if a required value is missing or blank, or too long (no request is sent)
     * @throws cloud.txapi.sdk.errors.TxApiException on API errors (see {@link cloud.txapi.sdk.errors}) and transport problems
     */
    public PlayerPage lookup(String identifier, PageCursor page, RequestOptions options) {
        return inv.call(Calls.players_lookup1(identifier, page), options);
    }

    /**
     * Full player record as txAdmin returns it (identifiers, history, notes, play time …), passed through unchanged.
     *
     * <p>{@code GET /api/v1/players/info} · permission {@code players.read}
     *
     * @param identifier player identifier: {@code license:…}, {@code discord:…}, {@code fivem:…}, {@code steam:…} or a bare license hash
     * @return txAdmin's player record (raw JSON)
     * @throws cloud.txapi.sdk.errors.ValidationException if a required value is missing or blank, or too long (no request is sent)
     * @throws cloud.txapi.sdk.errors.TxApiException on API errors (see {@link cloud.txapi.sdk.errors}) and transport problems
     */
    public JsonNode info(String identifier) {
        return inv.call(Calls.players_info(identifier), null);
    }

    /**
     * Full player record as txAdmin returns it (identifiers, history, notes, play time …), passed through unchanged.
     *
     * <p>{@code GET /api/v1/players/info} · permission {@code players.read}
     *
     * @param identifier player identifier: {@code license:…}, {@code discord:…}, {@code fivem:…}, {@code steam:…} or a bare license hash
     * @param options per-call options (server, timeout, retries); {@code null} = defaults of the client/view
     * @return txAdmin's player record (raw JSON)
     * @throws cloud.txapi.sdk.errors.ValidationException if a required value is missing or blank, or too long (no request is sent)
     * @throws cloud.txapi.sdk.errors.TxApiException on API errors (see {@link cloud.txapi.sdk.errors}) and transport problems
     */
    public JsonNode info(String identifier, RequestOptions options) {
        return inv.call(Calls.players_info(identifier), options);
    }

    /**
     * Player statistics of the server as txAdmin returns them, passed through unchanged.
     *
     * <p>{@code GET /api/v1/players/stats} · permission {@code players.read}
     *
     * @return the statistics (raw JSON)
     * @throws cloud.txapi.sdk.errors.TxApiException on API errors (see {@link cloud.txapi.sdk.errors}) and transport problems
     */
    public JsonNode stats() {
        return inv.call(Calls.players_stats(), null);
    }

    /**
     * Player statistics of the server as txAdmin returns them, passed through unchanged.
     *
     * <p>{@code GET /api/v1/players/stats} · permission {@code players.read}
     *
     * @param options per-call options (server, timeout, retries); {@code null} = defaults of the client/view
     * @return the statistics (raw JSON)
     * @throws cloud.txapi.sdk.errors.TxApiException on API errors (see {@link cloud.txapi.sdk.errors}) and transport problems
     */
    public JsonNode stats(RequestOptions options) {
        return inv.call(Calls.players_stats(), options);
    }

    /**
     * Warns a player. The warning is shown to the player in game and stored in the history.
     *
     * <p>{@code POST /api/v1/players/warn} · permission {@code players.write}
     *
     * @param identifier player identifier: {@code license:…}, {@code discord:…}, {@code fivem:…}, {@code steam:…} or a bare license hash
     * @param reason warning text, at most 512 characters
     * @return {@code success} and txAdmin's message
     * @throws cloud.txapi.sdk.errors.ValidationException if a required value is missing or blank, or too long (no request is sent)
     * @throws cloud.txapi.sdk.errors.TxApiException on API errors (see {@link cloud.txapi.sdk.errors}) and transport problems
     */
    public ActionResult warn(String identifier, String reason) {
        return inv.call(Calls.players_warn(identifier, reason), null);
    }

    /**
     * Warns a player. The warning is shown to the player in game and stored in the history.
     *
     * <p>{@code POST /api/v1/players/warn} · permission {@code players.write}
     *
     * @param identifier player identifier: {@code license:…}, {@code discord:…}, {@code fivem:…}, {@code steam:…} or a bare license hash
     * @param reason warning text, at most 512 characters
     * @param options per-call options (server, timeout, retries); {@code null} = defaults of the client/view
     * @return {@code success} and txAdmin's message
     * @throws cloud.txapi.sdk.errors.ValidationException if a required value is missing or blank, or too long (no request is sent)
     * @throws cloud.txapi.sdk.errors.TxApiException on API errors (see {@link cloud.txapi.sdk.errors}) and transport problems
     */
    public ActionResult warn(String identifier, String reason, RequestOptions options) {
        return inv.call(Calls.players_warn(identifier, reason), options);
    }

    /**
     * Bans a player — permanently, or for {@link BanRequest#durationHours()} hours. With {@link BanRequest#discordBan()} the player is also banned on every Discord server whose txBot is linked to this txAdmin server; the outcome is in {@link ActionResult#discord()}.
     *
     * <p>{@code POST /api/v1/players/ban} · permission {@code players.write}
     *
     * @param request ban parameters
     * @return {@code success}, txAdmin's message and (with {@code discordBan}) the Discord result
     * @throws cloud.txapi.sdk.errors.ValidationException if a required value is missing or blank, or too long (no request is sent)
     * @throws cloud.txapi.sdk.errors.TxApiException on API errors (see {@link cloud.txapi.sdk.errors}) and transport problems
     */
    public ActionResult ban(BanRequest request) {
        return inv.call(Calls.players_ban1(request), null);
    }

    /**
     * Bans a player — permanently, or for {@link BanRequest#durationHours()} hours. With {@link BanRequest#discordBan()} the player is also banned on every Discord server whose txBot is linked to this txAdmin server; the outcome is in {@link ActionResult#discord()}.
     *
     * <p>{@code POST /api/v1/players/ban} · permission {@code players.write}
     *
     * @param request ban parameters
     * @param options per-call options (server, timeout, retries); {@code null} = defaults of the client/view
     * @return {@code success}, txAdmin's message and (with {@code discordBan}) the Discord result
     * @throws cloud.txapi.sdk.errors.ValidationException if a required value is missing or blank, or too long (no request is sent)
     * @throws cloud.txapi.sdk.errors.TxApiException on API errors (see {@link cloud.txapi.sdk.errors}) and transport problems
     */
    public ActionResult ban(BanRequest request, RequestOptions options) {
        return inv.call(Calls.players_ban1(request), options);
    }

    /**
     * Bans a player permanently. For a temporary ban or a Discord ban use {@link #ban(BanRequest)}.
     *
     * <p>{@code POST /api/v1/players/ban} · permission {@code players.write}
     *
     * @param identifier player identifier: {@code license:…}, {@code discord:…}, {@code fivem:…}, {@code steam:…} or a bare license hash
     * @param reason ban reason, at most 512 characters
     * @return {@code success} and txAdmin's message
     * @throws cloud.txapi.sdk.errors.ValidationException if a required value is missing or blank, or too long (no request is sent)
     * @throws cloud.txapi.sdk.errors.TxApiException on API errors (see {@link cloud.txapi.sdk.errors}) and transport problems
     */
    public ActionResult ban(String identifier, String reason) {
        return inv.call(Calls.players_ban2(identifier, reason), null);
    }

    /**
     * Bans a player permanently. For a temporary ban or a Discord ban use {@link #ban(BanRequest)}.
     *
     * <p>{@code POST /api/v1/players/ban} · permission {@code players.write}
     *
     * @param identifier player identifier: {@code license:…}, {@code discord:…}, {@code fivem:…}, {@code steam:…} or a bare license hash
     * @param reason ban reason, at most 512 characters
     * @param options per-call options (server, timeout, retries); {@code null} = defaults of the client/view
     * @return {@code success} and txAdmin's message
     * @throws cloud.txapi.sdk.errors.ValidationException if a required value is missing or blank, or too long (no request is sent)
     * @throws cloud.txapi.sdk.errors.TxApiException on API errors (see {@link cloud.txapi.sdk.errors}) and transport problems
     */
    public ActionResult ban(String identifier, String reason, RequestOptions options) {
        return inv.call(Calls.players_ban2(identifier, reason), options);
    }

    /**
     * Kicks a connected player without a reason. Fails with {@link cloud.txapi.sdk.errors.TxAdminRejectedException} (422) when the player is offline.
     *
     * <p>{@code POST /api/v1/players/kick} · permission {@code players.write}
     *
     * @param identifier player identifier: {@code license:…}, {@code discord:…}, {@code fivem:…}, {@code steam:…} or a bare license hash
     * @return {@code success} and txAdmin's message
     * @throws cloud.txapi.sdk.errors.ValidationException if a required value is missing or blank, or too long (no request is sent)
     * @throws cloud.txapi.sdk.errors.TxApiException on API errors (see {@link cloud.txapi.sdk.errors}) and transport problems
     */
    public ActionResult kick(String identifier) {
        return inv.call(Calls.players_kick1(identifier), null);
    }

    /**
     * Kicks a connected player without a reason. Fails with {@link cloud.txapi.sdk.errors.TxAdminRejectedException} (422) when the player is offline.
     *
     * <p>{@code POST /api/v1/players/kick} · permission {@code players.write}
     *
     * @param identifier player identifier: {@code license:…}, {@code discord:…}, {@code fivem:…}, {@code steam:…} or a bare license hash
     * @param options per-call options (server, timeout, retries); {@code null} = defaults of the client/view
     * @return {@code success} and txAdmin's message
     * @throws cloud.txapi.sdk.errors.ValidationException if a required value is missing or blank, or too long (no request is sent)
     * @throws cloud.txapi.sdk.errors.TxApiException on API errors (see {@link cloud.txapi.sdk.errors}) and transport problems
     */
    public ActionResult kick(String identifier, RequestOptions options) {
        return inv.call(Calls.players_kick1(identifier), options);
    }

    /**
     * Kicks a connected player. Fails with {@link cloud.txapi.sdk.errors.TxAdminRejectedException} (422) when the player is offline.
     *
     * <p>{@code POST /api/v1/players/kick} · permission {@code players.write}
     *
     * @param identifier player identifier: {@code license:…}, {@code discord:…}, {@code fivem:…}, {@code steam:…} or a bare license hash
     * @param reason kick reason shown to the player, at most 512 characters; {@code null} = none
     * @return {@code success} and txAdmin's message
     * @throws cloud.txapi.sdk.errors.ValidationException if a required value is missing or blank, or too long (no request is sent)
     * @throws cloud.txapi.sdk.errors.TxApiException on API errors (see {@link cloud.txapi.sdk.errors}) and transport problems
     */
    public ActionResult kick(String identifier, String reason) {
        return inv.call(Calls.players_kick2(identifier, reason), null);
    }

    /**
     * Kicks a connected player. Fails with {@link cloud.txapi.sdk.errors.TxAdminRejectedException} (422) when the player is offline.
     *
     * <p>{@code POST /api/v1/players/kick} · permission {@code players.write}
     *
     * @param identifier player identifier: {@code license:…}, {@code discord:…}, {@code fivem:…}, {@code steam:…} or a bare license hash
     * @param reason kick reason shown to the player, at most 512 characters; {@code null} = none
     * @param options per-call options (server, timeout, retries); {@code null} = defaults of the client/view
     * @return {@code success} and txAdmin's message
     * @throws cloud.txapi.sdk.errors.ValidationException if a required value is missing or blank, or too long (no request is sent)
     * @throws cloud.txapi.sdk.errors.TxApiException on API errors (see {@link cloud.txapi.sdk.errors}) and transport problems
     */
    public ActionResult kick(String identifier, String reason, RequestOptions options) {
        return inv.call(Calls.players_kick2(identifier, reason), options);
    }

    /**
     * Sends a direct message to a connected player. Fails with {@link cloud.txapi.sdk.errors.TxAdminRejectedException} (422) when the player is offline.
     *
     * <p>{@code POST /api/v1/players/dm} · permission {@code players.write}
     *
     * @param identifier player identifier: {@code license:…}, {@code discord:…}, {@code fivem:…}, {@code steam:…} or a bare license hash
     * @param message message text, at most 512 characters
     * @return {@code success} and txAdmin's message
     * @throws cloud.txapi.sdk.errors.ValidationException if a required value is missing or blank, or too long (no request is sent)
     * @throws cloud.txapi.sdk.errors.TxApiException on API errors (see {@link cloud.txapi.sdk.errors}) and transport problems
     */
    public ActionResult dm(String identifier, String message) {
        return inv.call(Calls.players_dm(identifier, message), null);
    }

    /**
     * Sends a direct message to a connected player. Fails with {@link cloud.txapi.sdk.errors.TxAdminRejectedException} (422) when the player is offline.
     *
     * <p>{@code POST /api/v1/players/dm} · permission {@code players.write}
     *
     * @param identifier player identifier: {@code license:…}, {@code discord:…}, {@code fivem:…}, {@code steam:…} or a bare license hash
     * @param message message text, at most 512 characters
     * @param options per-call options (server, timeout, retries); {@code null} = defaults of the client/view
     * @return {@code success} and txAdmin's message
     * @throws cloud.txapi.sdk.errors.ValidationException if a required value is missing or blank, or too long (no request is sent)
     * @throws cloud.txapi.sdk.errors.TxApiException on API errors (see {@link cloud.txapi.sdk.errors}) and transport problems
     */
    public ActionResult dm(String identifier, String message, RequestOptions options) {
        return inv.call(Calls.players_dm(identifier, message), options);
    }

    /**
     * Sets (replaces) the note on a player. An empty string clears it.
     *
     * <p>{@code POST /api/v1/players/notes} · permission {@code players.write}
     *
     * @param identifier player identifier: {@code license:…}, {@code discord:…}, {@code fivem:…}, {@code steam:…} or a bare license hash
     * @param note the note, at most 2000 characters; {@code ""} clears it
     * @return {@code success} and txAdmin's message
     * @throws cloud.txapi.sdk.errors.ValidationException if a required value is missing or blank, or too long (no request is sent)
     * @throws cloud.txapi.sdk.errors.TxApiException on API errors (see {@link cloud.txapi.sdk.errors}) and transport problems
     */
    public ActionResult setNote(String identifier, String note) {
        return inv.call(Calls.players_setNote(identifier, note), null);
    }

    /**
     * Sets (replaces) the note on a player. An empty string clears it.
     *
     * <p>{@code POST /api/v1/players/notes} · permission {@code players.write}
     *
     * @param identifier player identifier: {@code license:…}, {@code discord:…}, {@code fivem:…}, {@code steam:…} or a bare license hash
     * @param note the note, at most 2000 characters; {@code ""} clears it
     * @param options per-call options (server, timeout, retries); {@code null} = defaults of the client/view
     * @return {@code success} and txAdmin's message
     * @throws cloud.txapi.sdk.errors.ValidationException if a required value is missing or blank, or too long (no request is sent)
     * @throws cloud.txapi.sdk.errors.TxApiException on API errors (see {@link cloud.txapi.sdk.errors}) and transport problems
     */
    public ActionResult setNote(String identifier, String note, RequestOptions options) {
        return inv.call(Calls.players_setNote(identifier, note), options);
    }

    /**
     * Adds a player to or removes them from the whitelist.
     *
     * <p>{@code POST /api/v1/players/whitelist} · permission {@code players.write}
     *
     * @param identifier player identifier: {@code license:…}, {@code discord:…}, {@code fivem:…}, {@code steam:…} or a bare license hash
     * @param whitelisted {@code true} = whitelist, {@code false} = remove
     * @return {@code success} and txAdmin's message
     * @throws cloud.txapi.sdk.errors.ValidationException if a required value is missing or blank, or too long (no request is sent)
     * @throws cloud.txapi.sdk.errors.TxApiException on API errors (see {@link cloud.txapi.sdk.errors}) and transport problems
     */
    public ActionResult setWhitelisted(String identifier, boolean whitelisted) {
        return inv.call(Calls.players_setWhitelisted(identifier, whitelisted), null);
    }

    /**
     * Adds a player to or removes them from the whitelist.
     *
     * <p>{@code POST /api/v1/players/whitelist} · permission {@code players.write}
     *
     * @param identifier player identifier: {@code license:…}, {@code discord:…}, {@code fivem:…}, {@code steam:…} or a bare license hash
     * @param whitelisted {@code true} = whitelist, {@code false} = remove
     * @param options per-call options (server, timeout, retries); {@code null} = defaults of the client/view
     * @return {@code success} and txAdmin's message
     * @throws cloud.txapi.sdk.errors.ValidationException if a required value is missing or blank, or too long (no request is sent)
     * @throws cloud.txapi.sdk.errors.TxApiException on API errors (see {@link cloud.txapi.sdk.errors}) and transport problems
     */
    public ActionResult setWhitelisted(String identifier, boolean whitelisted, RequestOptions options) {
        return inv.call(Calls.players_setWhitelisted(identifier, whitelisted), options);
    }

    /**
     * Iterates over every page of {@link #search(PlayerSearchParams)} (lazy). The next page starts after the last row's sort-key value and license.
     *
     * <p>{@code GET /api/v1/players/search} · permission {@code players.read}
     *
     * @param params search parameters; {@code null} = all players by play time
     * @return a lazy iterable over every row
     * @throws cloud.txapi.sdk.errors.TxApiException on API errors (see {@link cloud.txapi.sdk.errors}) and transport problems
     */
    public PageIterable<Player> searchAll(PlayerSearchParams params) {
        return searchAll(params, (RequestOptions) null);
    }

    /**
     * Iterates over every page of {@link #search(PlayerSearchParams)} (lazy). The next page starts after the last row's sort-key value and license.
     *
     * <p>{@code GET /api/v1/players/search} · permission {@code players.read}
     *
     * @param params search parameters; {@code null} = all players by play time
     * @param options per-call options (server, timeout, retries); {@code null} = defaults of the client/view
     * @return a lazy iterable over every row
     * @throws cloud.txapi.sdk.errors.TxApiException on API errors (see {@link cloud.txapi.sdk.errors}) and transport problems
     */
    public PageIterable<Player> searchAll(PlayerSearchParams params, RequestOptions options) {
        PlayerSearchParams base = params == null ? PlayerSearchParams.create() : params;
        return Pager.sync(token -> {
            PlayerSearchParams p = token == null ? base : base.page((PageCursor) token);
            return Tokens.players(inv.call(Calls.players_search1(p), options), p.sortKey());
        });
    }

    /**
     * Iterates over all players, sorted by play time (lazy).
     *
     * <p>{@code GET /api/v1/players/search} · permission {@code players.read}
     *
     * @return a lazy iterable over every row
     * @throws cloud.txapi.sdk.errors.TxApiException on API errors (see {@link cloud.txapi.sdk.errors}) and transport problems
     */
    public PageIterable<Player> searchAll() {
        return searchAll((RequestOptions) null);
    }

    /**
     * Iterates over all players, sorted by play time (lazy).
     *
     * <p>{@code GET /api/v1/players/search} · permission {@code players.read}
     *
     * @param options per-call options (server, timeout, retries); {@code null} = defaults of the client/view
     * @return a lazy iterable over every row
     * @throws cloud.txapi.sdk.errors.TxApiException on API errors (see {@link cloud.txapi.sdk.errors}) and transport problems
     */
    public PageIterable<Player> searchAll(RequestOptions options) {
        return searchAll((PlayerSearchParams) null, options);
    }

    /**
     * Iterates over every connected player (lazy).
     *
     * <p>{@code GET /api/v1/players/online} · permission {@code players.read}
     *
     * @return a lazy iterable over every row
     * @throws cloud.txapi.sdk.errors.TxApiException on API errors (see {@link cloud.txapi.sdk.errors}) and transport problems
     */
    public PageIterable<Player> onlineAll() {
        return onlineAll((RequestOptions) null);
    }

    /**
     * Iterates over every connected player (lazy).
     *
     * <p>{@code GET /api/v1/players/online} · permission {@code players.read}
     *
     * @param options per-call options (server, timeout, retries); {@code null} = defaults of the client/view
     * @return a lazy iterable over every row
     * @throws cloud.txapi.sdk.errors.TxApiException on API errors (see {@link cloud.txapi.sdk.errors}) and transport problems
     */
    public PageIterable<Player> onlineAll(RequestOptions options) {
        return Pager.sync(token -> Tokens.players(inv.call(Calls.players_online1((PageCursor) token), options), PlayerSort.PLAY_TIME));
    }

    /**
     * Iterates over every player record that carries the identifier (lazy).
     *
     * <p>{@code GET /api/v1/players/lookup} · permission {@code players.read}
     *
     * @param identifier player identifier: {@code license:…}, {@code discord:…}, {@code fivem:…}, {@code steam:…} or a bare license hash
     * @return a lazy iterable over every row
     * @throws cloud.txapi.sdk.errors.ValidationException if a required value is missing or blank, or too long (no request is sent)
     * @throws cloud.txapi.sdk.errors.TxApiException on API errors (see {@link cloud.txapi.sdk.errors}) and transport problems
     */
    public PageIterable<Player> lookupAll(String identifier) {
        return lookupAll(identifier, (RequestOptions) null);
    }

    /**
     * Iterates over every player record that carries the identifier (lazy).
     *
     * <p>{@code GET /api/v1/players/lookup} · permission {@code players.read}
     *
     * @param identifier player identifier: {@code license:…}, {@code discord:…}, {@code fivem:…}, {@code steam:…} or a bare license hash
     * @param options per-call options (server, timeout, retries); {@code null} = defaults of the client/view
     * @return a lazy iterable over every row
     * @throws cloud.txapi.sdk.errors.ValidationException if a required value is missing or blank, or too long (no request is sent)
     * @throws cloud.txapi.sdk.errors.TxApiException on API errors (see {@link cloud.txapi.sdk.errors}) and transport problems
     */
    public PageIterable<Player> lookupAll(String identifier, RequestOptions options) {
        return Pager.sync(token -> Tokens.players(inv.call(Calls.players_lookup1(identifier, (PageCursor) token), options), PlayerSort.PLAY_TIME));
    }
}
