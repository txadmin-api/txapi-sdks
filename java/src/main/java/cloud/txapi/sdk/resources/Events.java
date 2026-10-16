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
 * The stored event log ({@code /events/…}): what happened on the server, whoever did it — deaths and kills, joins and leaves, admin actions, bans and warnings, commands and (opt-in) chat. Needs the event log switched on for the server in the dashboard. Lists are newest first.
 *
 * <p>Obtain it from {@code client.events()}; do not construct it yourself.
 * Every method has an overload with a trailing {@link RequestOptions} for per-call settings
 * such as another {@code serverId}.
 *
 * @see AsyncEvents
 */
@SuppressWarnings("unused")
public final class Events {
    private final Invoker inv;

    /**
     * Internal: created by the client.
     *
     * @param invoker internal call runner
     */
    public Events(Invoker invoker) {
        this.inv = invoker;

    }

    /**
     * Every stored event, filtered. One page; use {@link #listAll(EventListQuery)} for all pages.
     *
     * <p>{@code GET /api/v1/events} · permission {@code logs.view}
     *
     * @return one page of events
     * @throws cloud.txapi.sdk.errors.TxApiException on API errors (see {@link cloud.txapi.sdk.errors}) and transport problems
     */
    public EventPage list() {
        return inv.call(Calls.events_list0(), null);
    }

    /**
     * Every stored event, filtered. One page; use {@link #listAll(EventListQuery)} for all pages.
     *
     * <p>{@code GET /api/v1/events} · permission {@code logs.view}
     *
     * @param options per-call options (server, timeout, retries); {@code null} = defaults of the client/view
     * @return one page of events
     * @throws cloud.txapi.sdk.errors.TxApiException on API errors (see {@link cloud.txapi.sdk.errors}) and transport problems
     */
    public EventPage list(RequestOptions options) {
        return inv.call(Calls.events_list0(), options);
    }

    /**
     * Every stored event, filtered. One page.
     *
     * <p>{@code GET /api/v1/events} · permission {@code logs.view}
     *
     * @param query filters, time window, cursor and page size; {@code null} = defaults
     * @return one page of events
     * @throws cloud.txapi.sdk.errors.TxApiException on API errors (see {@link cloud.txapi.sdk.errors}) and transport problems
     */
    public EventPage list(EventListQuery query) {
        return inv.call(Calls.events_list1(query), null);
    }

    /**
     * Every stored event, filtered. One page.
     *
     * <p>{@code GET /api/v1/events} · permission {@code logs.view}
     *
     * @param query filters, time window, cursor and page size; {@code null} = defaults
     * @param options per-call options (server, timeout, retries); {@code null} = defaults of the client/view
     * @return one page of events
     * @throws cloud.txapi.sdk.errors.TxApiException on API errors (see {@link cloud.txapi.sdk.errors}) and transport problems
     */
    public EventPage list(EventListQuery query, RequestOptions options) {
        return inv.call(Calls.events_list1(query), options);
    }

    /**
     * Iterates over every page of {@link #list(EventListQuery)} (lazy), following {@code nextCursor}.
     *
     * <p>{@code GET /api/v1/events} · permission {@code logs.view}
     *
     * @param query filters, time window and page size; {@code null} = defaults
     * @return a lazy iterable over every row
     * @throws cloud.txapi.sdk.errors.TxApiException on API errors (see {@link cloud.txapi.sdk.errors}) and transport problems
     */
    public PageIterable<ServerEvent> listAll(EventListQuery query) {
        return listAll(query, (RequestOptions) null);
    }

    /**
     * Iterates over every page of {@link #list(EventListQuery)} (lazy), following {@code nextCursor}.
     *
     * <p>{@code GET /api/v1/events} · permission {@code logs.view}
     *
     * @param query filters, time window and page size; {@code null} = defaults
     * @param options per-call options (server, timeout, retries); {@code null} = defaults of the client/view
     * @return a lazy iterable over every row
     * @throws cloud.txapi.sdk.errors.TxApiException on API errors (see {@link cloud.txapi.sdk.errors}) and transport problems
     */
    public PageIterable<ServerEvent> listAll(EventListQuery query, RequestOptions options) {
        EventListQuery base = query == null ? EventListQuery.create() : query;
        return Pager.sync(token -> Tokens.events(inv.call(
                Calls.events_list1(token == null ? base : base.cursor((String) token)), options)));
    }

    /**
     * Deaths (with killer and cause where known). One page; use {@link #deathsAll(DeathQuery)} for all pages.
     *
     * <p>{@code GET /api/v1/events/deaths} · permission {@code logs.view}
     *
     * @return one page of events
     * @throws cloud.txapi.sdk.errors.TxApiException on API errors (see {@link cloud.txapi.sdk.errors}) and transport problems
     */
    public EventPage deaths() {
        return inv.call(Calls.events_deaths0(), null);
    }

    /**
     * Deaths (with killer and cause where known). One page; use {@link #deathsAll(DeathQuery)} for all pages.
     *
     * <p>{@code GET /api/v1/events/deaths} · permission {@code logs.view}
     *
     * @param options per-call options (server, timeout, retries); {@code null} = defaults of the client/view
     * @return one page of events
     * @throws cloud.txapi.sdk.errors.TxApiException on API errors (see {@link cloud.txapi.sdk.errors}) and transport problems
     */
    public EventPage deaths(RequestOptions options) {
        return inv.call(Calls.events_deaths0(), options);
    }

    /**
     * Deaths (with killer and cause where known). One page.
     *
     * <p>{@code GET /api/v1/events/deaths} · permission {@code logs.view}
     *
     * @param query filters, time window, cursor and page size; {@code null} = defaults
     * @return one page of events
     * @throws cloud.txapi.sdk.errors.TxApiException on API errors (see {@link cloud.txapi.sdk.errors}) and transport problems
     */
    public EventPage deaths(DeathQuery query) {
        return inv.call(Calls.events_deaths1(query), null);
    }

    /**
     * Deaths (with killer and cause where known). One page.
     *
     * <p>{@code GET /api/v1/events/deaths} · permission {@code logs.view}
     *
     * @param query filters, time window, cursor and page size; {@code null} = defaults
     * @param options per-call options (server, timeout, retries); {@code null} = defaults of the client/view
     * @return one page of events
     * @throws cloud.txapi.sdk.errors.TxApiException on API errors (see {@link cloud.txapi.sdk.errors}) and transport problems
     */
    public EventPage deaths(DeathQuery query, RequestOptions options) {
        return inv.call(Calls.events_deaths1(query), options);
    }

    /**
     * Iterates over every page of {@link #deaths(DeathQuery)} (lazy), following {@code nextCursor}.
     *
     * <p>{@code GET /api/v1/events/deaths} · permission {@code logs.view}
     *
     * @param query filters, time window and page size; {@code null} = defaults
     * @return a lazy iterable over every row
     * @throws cloud.txapi.sdk.errors.TxApiException on API errors (see {@link cloud.txapi.sdk.errors}) and transport problems
     */
    public PageIterable<ServerEvent> deathsAll(DeathQuery query) {
        return deathsAll(query, (RequestOptions) null);
    }

    /**
     * Iterates over every page of {@link #deaths(DeathQuery)} (lazy), following {@code nextCursor}.
     *
     * <p>{@code GET /api/v1/events/deaths} · permission {@code logs.view}
     *
     * @param query filters, time window and page size; {@code null} = defaults
     * @param options per-call options (server, timeout, retries); {@code null} = defaults of the client/view
     * @return a lazy iterable over every row
     * @throws cloud.txapi.sdk.errors.TxApiException on API errors (see {@link cloud.txapi.sdk.errors}) and transport problems
     */
    public PageIterable<ServerEvent> deathsAll(DeathQuery query, RequestOptions options) {
        DeathQuery base = query == null ? DeathQuery.create() : query;
        return Pager.sync(token -> Tokens.events(inv.call(
                Calls.events_deaths1(token == null ? base : base.cursor((String) token)), options)));
    }

    /**
     * Player-vs-player kills. One page; use {@link #killsAll(KillQuery)} for all pages.
     *
     * <p>{@code GET /api/v1/events/kills} · permission {@code logs.view}
     *
     * @return one page of events
     * @throws cloud.txapi.sdk.errors.TxApiException on API errors (see {@link cloud.txapi.sdk.errors}) and transport problems
     */
    public EventPage kills() {
        return inv.call(Calls.events_kills0(), null);
    }

    /**
     * Player-vs-player kills. One page; use {@link #killsAll(KillQuery)} for all pages.
     *
     * <p>{@code GET /api/v1/events/kills} · permission {@code logs.view}
     *
     * @param options per-call options (server, timeout, retries); {@code null} = defaults of the client/view
     * @return one page of events
     * @throws cloud.txapi.sdk.errors.TxApiException on API errors (see {@link cloud.txapi.sdk.errors}) and transport problems
     */
    public EventPage kills(RequestOptions options) {
        return inv.call(Calls.events_kills0(), options);
    }

    /**
     * Player-vs-player kills. One page.
     *
     * <p>{@code GET /api/v1/events/kills} · permission {@code logs.view}
     *
     * @param query filters, time window, cursor and page size; {@code null} = defaults
     * @return one page of events
     * @throws cloud.txapi.sdk.errors.TxApiException on API errors (see {@link cloud.txapi.sdk.errors}) and transport problems
     */
    public EventPage kills(KillQuery query) {
        return inv.call(Calls.events_kills1(query), null);
    }

    /**
     * Player-vs-player kills. One page.
     *
     * <p>{@code GET /api/v1/events/kills} · permission {@code logs.view}
     *
     * @param query filters, time window, cursor and page size; {@code null} = defaults
     * @param options per-call options (server, timeout, retries); {@code null} = defaults of the client/view
     * @return one page of events
     * @throws cloud.txapi.sdk.errors.TxApiException on API errors (see {@link cloud.txapi.sdk.errors}) and transport problems
     */
    public EventPage kills(KillQuery query, RequestOptions options) {
        return inv.call(Calls.events_kills1(query), options);
    }

    /**
     * Iterates over every page of {@link #kills(KillQuery)} (lazy), following {@code nextCursor}.
     *
     * <p>{@code GET /api/v1/events/kills} · permission {@code logs.view}
     *
     * @param query filters, time window and page size; {@code null} = defaults
     * @return a lazy iterable over every row
     * @throws cloud.txapi.sdk.errors.TxApiException on API errors (see {@link cloud.txapi.sdk.errors}) and transport problems
     */
    public PageIterable<ServerEvent> killsAll(KillQuery query) {
        return killsAll(query, (RequestOptions) null);
    }

    /**
     * Iterates over every page of {@link #kills(KillQuery)} (lazy), following {@code nextCursor}.
     *
     * <p>{@code GET /api/v1/events/kills} · permission {@code logs.view}
     *
     * @param query filters, time window and page size; {@code null} = defaults
     * @param options per-call options (server, timeout, retries); {@code null} = defaults of the client/view
     * @return a lazy iterable over every row
     * @throws cloud.txapi.sdk.errors.TxApiException on API errors (see {@link cloud.txapi.sdk.errors}) and transport problems
     */
    public PageIterable<ServerEvent> killsAll(KillQuery query, RequestOptions options) {
        KillQuery base = query == null ? KillQuery.create() : query;
        return Pager.sync(token -> Tokens.events(inv.call(
                Calls.events_kills1(token == null ? base : base.cursor((String) token)), options)));
    }

    /**
     * Chat messages (only if chat logging is on — see {@link EventPage#chatLog()}). One page; use {@link #chatAll(ChatQuery)} for all pages.
     *
     * <p>{@code GET /api/v1/events/chat} · permission {@code logs.view}
     *
     * @return one page of events
     * @throws cloud.txapi.sdk.errors.TxApiException on API errors (see {@link cloud.txapi.sdk.errors}) and transport problems
     */
    public EventPage chat() {
        return inv.call(Calls.events_chat0(), null);
    }

    /**
     * Chat messages (only if chat logging is on — see {@link EventPage#chatLog()}). One page; use {@link #chatAll(ChatQuery)} for all pages.
     *
     * <p>{@code GET /api/v1/events/chat} · permission {@code logs.view}
     *
     * @param options per-call options (server, timeout, retries); {@code null} = defaults of the client/view
     * @return one page of events
     * @throws cloud.txapi.sdk.errors.TxApiException on API errors (see {@link cloud.txapi.sdk.errors}) and transport problems
     */
    public EventPage chat(RequestOptions options) {
        return inv.call(Calls.events_chat0(), options);
    }

    /**
     * Chat messages (only if chat logging is on — see {@link EventPage#chatLog()}). One page.
     *
     * <p>{@code GET /api/v1/events/chat} · permission {@code logs.view}
     *
     * @param query filters, time window, cursor and page size; {@code null} = defaults
     * @return one page of events
     * @throws cloud.txapi.sdk.errors.TxApiException on API errors (see {@link cloud.txapi.sdk.errors}) and transport problems
     */
    public EventPage chat(ChatQuery query) {
        return inv.call(Calls.events_chat1(query), null);
    }

    /**
     * Chat messages (only if chat logging is on — see {@link EventPage#chatLog()}). One page.
     *
     * <p>{@code GET /api/v1/events/chat} · permission {@code logs.view}
     *
     * @param query filters, time window, cursor and page size; {@code null} = defaults
     * @param options per-call options (server, timeout, retries); {@code null} = defaults of the client/view
     * @return one page of events
     * @throws cloud.txapi.sdk.errors.TxApiException on API errors (see {@link cloud.txapi.sdk.errors}) and transport problems
     */
    public EventPage chat(ChatQuery query, RequestOptions options) {
        return inv.call(Calls.events_chat1(query), options);
    }

    /**
     * Iterates over every page of {@link #chat(ChatQuery)} (lazy), following {@code nextCursor}.
     *
     * <p>{@code GET /api/v1/events/chat} · permission {@code logs.view}
     *
     * @param query filters, time window and page size; {@code null} = defaults
     * @return a lazy iterable over every row
     * @throws cloud.txapi.sdk.errors.TxApiException on API errors (see {@link cloud.txapi.sdk.errors}) and transport problems
     */
    public PageIterable<ServerEvent> chatAll(ChatQuery query) {
        return chatAll(query, (RequestOptions) null);
    }

    /**
     * Iterates over every page of {@link #chat(ChatQuery)} (lazy), following {@code nextCursor}.
     *
     * <p>{@code GET /api/v1/events/chat} · permission {@code logs.view}
     *
     * @param query filters, time window and page size; {@code null} = defaults
     * @param options per-call options (server, timeout, retries); {@code null} = defaults of the client/view
     * @return a lazy iterable over every row
     * @throws cloud.txapi.sdk.errors.TxApiException on API errors (see {@link cloud.txapi.sdk.errors}) and transport problems
     */
    public PageIterable<ServerEvent> chatAll(ChatQuery query, RequestOptions options) {
        ChatQuery base = query == null ? ChatQuery.create() : query;
        return Pager.sync(token -> Tokens.events(inv.call(
                Calls.events_chat1(token == null ? base : base.cursor((String) token)), options)));
    }

    /**
     * Commands run by players. One page; use {@link #commandsAll(CommandQuery)} for all pages.
     *
     * <p>{@code GET /api/v1/events/commands} · permission {@code logs.view}
     *
     * @return one page of events
     * @throws cloud.txapi.sdk.errors.TxApiException on API errors (see {@link cloud.txapi.sdk.errors}) and transport problems
     */
    public EventPage commands() {
        return inv.call(Calls.events_commands0(), null);
    }

    /**
     * Commands run by players. One page; use {@link #commandsAll(CommandQuery)} for all pages.
     *
     * <p>{@code GET /api/v1/events/commands} · permission {@code logs.view}
     *
     * @param options per-call options (server, timeout, retries); {@code null} = defaults of the client/view
     * @return one page of events
     * @throws cloud.txapi.sdk.errors.TxApiException on API errors (see {@link cloud.txapi.sdk.errors}) and transport problems
     */
    public EventPage commands(RequestOptions options) {
        return inv.call(Calls.events_commands0(), options);
    }

    /**
     * Commands run by players. One page.
     *
     * <p>{@code GET /api/v1/events/commands} · permission {@code logs.view}
     *
     * @param query filters, time window, cursor and page size; {@code null} = defaults
     * @return one page of events
     * @throws cloud.txapi.sdk.errors.TxApiException on API errors (see {@link cloud.txapi.sdk.errors}) and transport problems
     */
    public EventPage commands(CommandQuery query) {
        return inv.call(Calls.events_commands1(query), null);
    }

    /**
     * Commands run by players. One page.
     *
     * <p>{@code GET /api/v1/events/commands} · permission {@code logs.view}
     *
     * @param query filters, time window, cursor and page size; {@code null} = defaults
     * @param options per-call options (server, timeout, retries); {@code null} = defaults of the client/view
     * @return one page of events
     * @throws cloud.txapi.sdk.errors.TxApiException on API errors (see {@link cloud.txapi.sdk.errors}) and transport problems
     */
    public EventPage commands(CommandQuery query, RequestOptions options) {
        return inv.call(Calls.events_commands1(query), options);
    }

    /**
     * Iterates over every page of {@link #commands(CommandQuery)} (lazy), following {@code nextCursor}.
     *
     * <p>{@code GET /api/v1/events/commands} · permission {@code logs.view}
     *
     * @param query filters, time window and page size; {@code null} = defaults
     * @return a lazy iterable over every row
     * @throws cloud.txapi.sdk.errors.TxApiException on API errors (see {@link cloud.txapi.sdk.errors}) and transport problems
     */
    public PageIterable<ServerEvent> commandsAll(CommandQuery query) {
        return commandsAll(query, (RequestOptions) null);
    }

    /**
     * Iterates over every page of {@link #commands(CommandQuery)} (lazy), following {@code nextCursor}.
     *
     * <p>{@code GET /api/v1/events/commands} · permission {@code logs.view}
     *
     * @param query filters, time window and page size; {@code null} = defaults
     * @param options per-call options (server, timeout, retries); {@code null} = defaults of the client/view
     * @return a lazy iterable over every row
     * @throws cloud.txapi.sdk.errors.TxApiException on API errors (see {@link cloud.txapi.sdk.errors}) and transport problems
     */
    public PageIterable<ServerEvent> commandsAll(CommandQuery query, RequestOptions options) {
        CommandQuery base = query == null ? CommandQuery.create() : query;
        return Pager.sync(token -> Tokens.events(inv.call(
                Calls.events_commands1(token == null ? base : base.cursor((String) token)), options)));
    }

    /**
     * Actions of txAdmin admins in the panel and the in-game menu. One page; use {@link #adminActionsAll(AdminActionQuery)} for all pages.
     *
     * <p>{@code GET /api/v1/events/admin-actions} · permission {@code logs.view}
     *
     * @return one page of events
     * @throws cloud.txapi.sdk.errors.TxApiException on API errors (see {@link cloud.txapi.sdk.errors}) and transport problems
     */
    public EventPage adminActions() {
        return inv.call(Calls.events_adminActions0(), null);
    }

    /**
     * Actions of txAdmin admins in the panel and the in-game menu. One page; use {@link #adminActionsAll(AdminActionQuery)} for all pages.
     *
     * <p>{@code GET /api/v1/events/admin-actions} · permission {@code logs.view}
     *
     * @param options per-call options (server, timeout, retries); {@code null} = defaults of the client/view
     * @return one page of events
     * @throws cloud.txapi.sdk.errors.TxApiException on API errors (see {@link cloud.txapi.sdk.errors}) and transport problems
     */
    public EventPage adminActions(RequestOptions options) {
        return inv.call(Calls.events_adminActions0(), options);
    }

    /**
     * Actions of txAdmin admins in the panel and the in-game menu. One page.
     *
     * <p>{@code GET /api/v1/events/admin-actions} · permission {@code logs.view}
     *
     * @param query filters, time window, cursor and page size; {@code null} = defaults
     * @return one page of events
     * @throws cloud.txapi.sdk.errors.TxApiException on API errors (see {@link cloud.txapi.sdk.errors}) and transport problems
     */
    public EventPage adminActions(AdminActionQuery query) {
        return inv.call(Calls.events_adminActions1(query), null);
    }

    /**
     * Actions of txAdmin admins in the panel and the in-game menu. One page.
     *
     * <p>{@code GET /api/v1/events/admin-actions} · permission {@code logs.view}
     *
     * @param query filters, time window, cursor and page size; {@code null} = defaults
     * @param options per-call options (server, timeout, retries); {@code null} = defaults of the client/view
     * @return one page of events
     * @throws cloud.txapi.sdk.errors.TxApiException on API errors (see {@link cloud.txapi.sdk.errors}) and transport problems
     */
    public EventPage adminActions(AdminActionQuery query, RequestOptions options) {
        return inv.call(Calls.events_adminActions1(query), options);
    }

    /**
     * Iterates over every page of {@link #adminActions(AdminActionQuery)} (lazy), following {@code nextCursor}.
     *
     * <p>{@code GET /api/v1/events/admin-actions} · permission {@code logs.view}
     *
     * @param query filters, time window and page size; {@code null} = defaults
     * @return a lazy iterable over every row
     * @throws cloud.txapi.sdk.errors.TxApiException on API errors (see {@link cloud.txapi.sdk.errors}) and transport problems
     */
    public PageIterable<ServerEvent> adminActionsAll(AdminActionQuery query) {
        return adminActionsAll(query, (RequestOptions) null);
    }

    /**
     * Iterates over every page of {@link #adminActions(AdminActionQuery)} (lazy), following {@code nextCursor}.
     *
     * <p>{@code GET /api/v1/events/admin-actions} · permission {@code logs.view}
     *
     * @param query filters, time window and page size; {@code null} = defaults
     * @param options per-call options (server, timeout, retries); {@code null} = defaults of the client/view
     * @return a lazy iterable over every row
     * @throws cloud.txapi.sdk.errors.TxApiException on API errors (see {@link cloud.txapi.sdk.errors}) and transport problems
     */
    public PageIterable<ServerEvent> adminActionsAll(AdminActionQuery query, RequestOptions options) {
        AdminActionQuery base = query == null ? AdminActionQuery.create() : query;
        return Pager.sync(token -> Tokens.events(inv.call(
                Calls.events_adminActions1(token == null ? base : base.cursor((String) token)), options)));
    }

    /**
     * Bans and warnings from every source (panel, menu, API). One page; use {@link #actionsAll(ActionQuery)} for all pages.
     *
     * <p>{@code GET /api/v1/events/actions} · permission {@code logs.view}
     *
     * @return one page of events
     * @throws cloud.txapi.sdk.errors.TxApiException on API errors (see {@link cloud.txapi.sdk.errors}) and transport problems
     */
    public EventPage actions() {
        return inv.call(Calls.events_actions0(), null);
    }

    /**
     * Bans and warnings from every source (panel, menu, API). One page; use {@link #actionsAll(ActionQuery)} for all pages.
     *
     * <p>{@code GET /api/v1/events/actions} · permission {@code logs.view}
     *
     * @param options per-call options (server, timeout, retries); {@code null} = defaults of the client/view
     * @return one page of events
     * @throws cloud.txapi.sdk.errors.TxApiException on API errors (see {@link cloud.txapi.sdk.errors}) and transport problems
     */
    public EventPage actions(RequestOptions options) {
        return inv.call(Calls.events_actions0(), options);
    }

    /**
     * Bans and warnings from every source (panel, menu, API). One page.
     *
     * <p>{@code GET /api/v1/events/actions} · permission {@code logs.view}
     *
     * @param query filters, time window, cursor and page size; {@code null} = defaults
     * @return one page of events
     * @throws cloud.txapi.sdk.errors.TxApiException on API errors (see {@link cloud.txapi.sdk.errors}) and transport problems
     */
    public EventPage actions(ActionQuery query) {
        return inv.call(Calls.events_actions1(query), null);
    }

    /**
     * Bans and warnings from every source (panel, menu, API). One page.
     *
     * <p>{@code GET /api/v1/events/actions} · permission {@code logs.view}
     *
     * @param query filters, time window, cursor and page size; {@code null} = defaults
     * @param options per-call options (server, timeout, retries); {@code null} = defaults of the client/view
     * @return one page of events
     * @throws cloud.txapi.sdk.errors.TxApiException on API errors (see {@link cloud.txapi.sdk.errors}) and transport problems
     */
    public EventPage actions(ActionQuery query, RequestOptions options) {
        return inv.call(Calls.events_actions1(query), options);
    }

    /**
     * Iterates over every page of {@link #actions(ActionQuery)} (lazy), following {@code nextCursor}.
     *
     * <p>{@code GET /api/v1/events/actions} · permission {@code logs.view}
     *
     * @param query filters, time window and page size; {@code null} = defaults
     * @return a lazy iterable over every row
     * @throws cloud.txapi.sdk.errors.TxApiException on API errors (see {@link cloud.txapi.sdk.errors}) and transport problems
     */
    public PageIterable<ServerEvent> actionsAll(ActionQuery query) {
        return actionsAll(query, (RequestOptions) null);
    }

    /**
     * Iterates over every page of {@link #actions(ActionQuery)} (lazy), following {@code nextCursor}.
     *
     * <p>{@code GET /api/v1/events/actions} · permission {@code logs.view}
     *
     * @param query filters, time window and page size; {@code null} = defaults
     * @param options per-call options (server, timeout, retries); {@code null} = defaults of the client/view
     * @return a lazy iterable over every row
     * @throws cloud.txapi.sdk.errors.TxApiException on API errors (see {@link cloud.txapi.sdk.errors}) and transport problems
     */
    public PageIterable<ServerEvent> actionsAll(ActionQuery query, RequestOptions options) {
        ActionQuery base = query == null ? ActionQuery.create() : query;
        return Pager.sync(token -> Tokens.events(inv.call(
                Calls.events_actions1(token == null ? base : base.cursor((String) token)), options)));
    }

    /**
     * Joins and leaves. One page; use {@link #sessionsAll(SessionQuery)} for all pages.
     *
     * <p>{@code GET /api/v1/events/sessions} · permission {@code logs.view}
     *
     * @return one page of events
     * @throws cloud.txapi.sdk.errors.TxApiException on API errors (see {@link cloud.txapi.sdk.errors}) and transport problems
     */
    public EventPage sessions() {
        return inv.call(Calls.events_sessions0(), null);
    }

    /**
     * Joins and leaves. One page; use {@link #sessionsAll(SessionQuery)} for all pages.
     *
     * <p>{@code GET /api/v1/events/sessions} · permission {@code logs.view}
     *
     * @param options per-call options (server, timeout, retries); {@code null} = defaults of the client/view
     * @return one page of events
     * @throws cloud.txapi.sdk.errors.TxApiException on API errors (see {@link cloud.txapi.sdk.errors}) and transport problems
     */
    public EventPage sessions(RequestOptions options) {
        return inv.call(Calls.events_sessions0(), options);
    }

    /**
     * Joins and leaves. One page.
     *
     * <p>{@code GET /api/v1/events/sessions} · permission {@code logs.view}
     *
     * @param query filters, time window, cursor and page size; {@code null} = defaults
     * @return one page of events
     * @throws cloud.txapi.sdk.errors.TxApiException on API errors (see {@link cloud.txapi.sdk.errors}) and transport problems
     */
    public EventPage sessions(SessionQuery query) {
        return inv.call(Calls.events_sessions1(query), null);
    }

    /**
     * Joins and leaves. One page.
     *
     * <p>{@code GET /api/v1/events/sessions} · permission {@code logs.view}
     *
     * @param query filters, time window, cursor and page size; {@code null} = defaults
     * @param options per-call options (server, timeout, retries); {@code null} = defaults of the client/view
     * @return one page of events
     * @throws cloud.txapi.sdk.errors.TxApiException on API errors (see {@link cloud.txapi.sdk.errors}) and transport problems
     */
    public EventPage sessions(SessionQuery query, RequestOptions options) {
        return inv.call(Calls.events_sessions1(query), options);
    }

    /**
     * Iterates over every page of {@link #sessions(SessionQuery)} (lazy), following {@code nextCursor}.
     *
     * <p>{@code GET /api/v1/events/sessions} · permission {@code logs.view}
     *
     * @param query filters, time window and page size; {@code null} = defaults
     * @return a lazy iterable over every row
     * @throws cloud.txapi.sdk.errors.TxApiException on API errors (see {@link cloud.txapi.sdk.errors}) and transport problems
     */
    public PageIterable<ServerEvent> sessionsAll(SessionQuery query) {
        return sessionsAll(query, (RequestOptions) null);
    }

    /**
     * Iterates over every page of {@link #sessions(SessionQuery)} (lazy), following {@code nextCursor}.
     *
     * <p>{@code GET /api/v1/events/sessions} · permission {@code logs.view}
     *
     * @param query filters, time window and page size; {@code null} = defaults
     * @param options per-call options (server, timeout, retries); {@code null} = defaults of the client/view
     * @return a lazy iterable over every row
     * @throws cloud.txapi.sdk.errors.TxApiException on API errors (see {@link cloud.txapi.sdk.errors}) and transport problems
     */
    public PageIterable<ServerEvent> sessionsAll(SessionQuery query, RequestOptions options) {
        SessionQuery base = query == null ? SessionQuery.create() : query;
        return Pager.sync(token -> Tokens.events(inv.call(
                Calls.events_sessions1(token == null ? base : base.cursor((String) token)), options)));
    }

    /**
     * Kill/death leaderboard (default: by kills).
     *
     * <p>{@code GET /api/v1/events/leaderboard} · permission {@code logs.view}
     *
     * @return the leaderboard
     * @throws cloud.txapi.sdk.errors.TxApiException on API errors (see {@link cloud.txapi.sdk.errors}) and transport problems
     */
    public Leaderboard leaderboard() {
        return inv.call(Calls.events_lb0(), null);
    }

    /**
     * Kill/death leaderboard (default: by kills).
     *
     * <p>{@code GET /api/v1/events/leaderboard} · permission {@code logs.view}
     *
     * @param options per-call options (server, timeout, retries); {@code null} = defaults of the client/view
     * @return the leaderboard
     * @throws cloud.txapi.sdk.errors.TxApiException on API errors (see {@link cloud.txapi.sdk.errors}) and transport problems
     */
    public Leaderboard leaderboard(RequestOptions options) {
        return inv.call(Calls.events_lb0(), options);
    }

    /**
     * Kill/death leaderboard.
     *
     * <p>{@code GET /api/v1/events/leaderboard} · permission {@code logs.view}
     *
     * @param query window, sort key and size; {@code null} = defaults
     * @return the leaderboard
     * @throws cloud.txapi.sdk.errors.TxApiException on API errors (see {@link cloud.txapi.sdk.errors}) and transport problems
     */
    public Leaderboard leaderboard(LeaderboardQuery query) {
        return inv.call(Calls.events_lb1(query), null);
    }

    /**
     * Kill/death leaderboard.
     *
     * <p>{@code GET /api/v1/events/leaderboard} · permission {@code logs.view}
     *
     * @param query window, sort key and size; {@code null} = defaults
     * @param options per-call options (server, timeout, retries); {@code null} = defaults of the client/view
     * @return the leaderboard
     * @throws cloud.txapi.sdk.errors.TxApiException on API errors (see {@link cloud.txapi.sdk.errors}) and transport problems
     */
    public Leaderboard leaderboard(LeaderboardQuery query, RequestOptions options) {
        return inv.call(Calls.events_lb1(query), options);
    }

    /**
     * Everything about one player: kills, deaths, K/D, top victims/killers/weapons, sessions, bans.
     *
     * <p>{@code GET /api/v1/events/player} · permission {@code logs.view}
     *
     * @param player player name (exact, case-insensitive) or identifier such as {@code license:…}
     * @return the player's statistics
     * @throws cloud.txapi.sdk.errors.ValidationException if a required value is missing or blank, or too long (no request is sent)
     * @throws cloud.txapi.sdk.errors.TxApiException on API errors (see {@link cloud.txapi.sdk.errors}) and transport problems
     */
    public PlayerEventStats player(String player) {
        return inv.call(Calls.events_pl0(player), null);
    }

    /**
     * Everything about one player: kills, deaths, K/D, top victims/killers/weapons, sessions, bans.
     *
     * <p>{@code GET /api/v1/events/player} · permission {@code logs.view}
     *
     * @param player player name (exact, case-insensitive) or identifier such as {@code license:…}
     * @param options per-call options (server, timeout, retries); {@code null} = defaults of the client/view
     * @return the player's statistics
     * @throws cloud.txapi.sdk.errors.ValidationException if a required value is missing or blank, or too long (no request is sent)
     * @throws cloud.txapi.sdk.errors.TxApiException on API errors (see {@link cloud.txapi.sdk.errors}) and transport problems
     */
    public PlayerEventStats player(String player, RequestOptions options) {
        return inv.call(Calls.events_pl0(player), options);
    }

    /**
     * Everything about one player within a time window.
     *
     * <p>{@code GET /api/v1/events/player} · permission {@code logs.view}
     *
     * @param player player name (exact, case-insensitive) or identifier such as {@code license:…}
     * @param since window start: {@code 24h}, {@code 7d}, ISO-8601 or epoch ms; {@code null} = everything stored
     * @return the player's statistics
     * @throws cloud.txapi.sdk.errors.ValidationException if a required value is missing or blank, or too long (no request is sent)
     * @throws cloud.txapi.sdk.errors.TxApiException on API errors (see {@link cloud.txapi.sdk.errors}) and transport problems
     */
    public PlayerEventStats player(String player, String since) {
        return inv.call(Calls.events_pl1(player, since), null);
    }

    /**
     * Everything about one player within a time window.
     *
     * <p>{@code GET /api/v1/events/player} · permission {@code logs.view}
     *
     * @param player player name (exact, case-insensitive) or identifier such as {@code license:…}
     * @param since window start: {@code 24h}, {@code 7d}, ISO-8601 or epoch ms; {@code null} = everything stored
     * @param options per-call options (server, timeout, retries); {@code null} = defaults of the client/view
     * @return the player's statistics
     * @throws cloud.txapi.sdk.errors.ValidationException if a required value is missing or blank, or too long (no request is sent)
     * @throws cloud.txapi.sdk.errors.TxApiException on API errors (see {@link cloud.txapi.sdk.errors}) and transport problems
     */
    public PlayerEventStats player(String player, String since, RequestOptions options) {
        return inv.call(Calls.events_pl1(player, since), options);
    }

    /**
     * Counts by type, kills, unique players, top killer and causes over everything stored.
     *
     * <p>{@code GET /api/v1/events/summary} · permission {@code logs.view}
     *
     * @return the summary
     * @throws cloud.txapi.sdk.errors.TxApiException on API errors (see {@link cloud.txapi.sdk.errors}) and transport problems
     */
    public EventSummary summary() {
        return inv.call(Calls.events_sum0(), null);
    }

    /**
     * Counts by type, kills, unique players, top killer and causes over everything stored.
     *
     * <p>{@code GET /api/v1/events/summary} · permission {@code logs.view}
     *
     * @param options per-call options (server, timeout, retries); {@code null} = defaults of the client/view
     * @return the summary
     * @throws cloud.txapi.sdk.errors.TxApiException on API errors (see {@link cloud.txapi.sdk.errors}) and transport problems
     */
    public EventSummary summary(RequestOptions options) {
        return inv.call(Calls.events_sum0(), options);
    }

    /**
     * Counts by type, kills, unique players, top killer and causes within a time window.
     *
     * <p>{@code GET /api/v1/events/summary} · permission {@code logs.view}
     *
     * @param since window start: {@code 24h}, {@code 7d}, ISO-8601 or epoch ms; {@code null} = everything stored
     * @return the summary
     * @throws cloud.txapi.sdk.errors.TxApiException on API errors (see {@link cloud.txapi.sdk.errors}) and transport problems
     */
    public EventSummary summary(String since) {
        return inv.call(Calls.events_sum1(since), null);
    }

    /**
     * Counts by type, kills, unique players, top killer and causes within a time window.
     *
     * <p>{@code GET /api/v1/events/summary} · permission {@code logs.view}
     *
     * @param since window start: {@code 24h}, {@code 7d}, ISO-8601 or epoch ms; {@code null} = everything stored
     * @param options per-call options (server, timeout, retries); {@code null} = defaults of the client/view
     * @return the summary
     * @throws cloud.txapi.sdk.errors.TxApiException on API errors (see {@link cloud.txapi.sdk.errors}) and transport problems
     */
    public EventSummary summary(String since, RequestOptions options) {
        return inv.call(Calls.events_sum1(since), options);
    }

    /**
     * Play time per player from join/leave pairs (default window).
     *
     * <p>{@code GET /api/v1/events/playtime} · permission {@code logs.view}
     *
     * @return play time ranking
     * @throws cloud.txapi.sdk.errors.TxApiException on API errors (see {@link cloud.txapi.sdk.errors}) and transport problems
     */
    public Playtime playtime() {
        return inv.call(Calls.events_pt0(), null);
    }

    /**
     * Play time per player from join/leave pairs (default window).
     *
     * <p>{@code GET /api/v1/events/playtime} · permission {@code logs.view}
     *
     * @param options per-call options (server, timeout, retries); {@code null} = defaults of the client/view
     * @return play time ranking
     * @throws cloud.txapi.sdk.errors.TxApiException on API errors (see {@link cloud.txapi.sdk.errors}) and transport problems
     */
    public Playtime playtime(RequestOptions options) {
        return inv.call(Calls.events_pt0(), options);
    }

    /**
     * Play time per player from join/leave pairs; with a player, also their recent sessions.
     *
     * <p>{@code GET /api/v1/events/playtime} · permission {@code logs.view}
     *
     * @param query window, player and size; {@code null} = defaults
     * @return play time ranking
     * @throws cloud.txapi.sdk.errors.TxApiException on API errors (see {@link cloud.txapi.sdk.errors}) and transport problems
     */
    public Playtime playtime(PlaytimeQuery query) {
        return inv.call(Calls.events_pt1(query), null);
    }

    /**
     * Play time per player from join/leave pairs; with a player, also their recent sessions.
     *
     * <p>{@code GET /api/v1/events/playtime} · permission {@code logs.view}
     *
     * @param query window, player and size; {@code null} = defaults
     * @param options per-call options (server, timeout, retries); {@code null} = defaults of the client/view
     * @return play time ranking
     * @throws cloud.txapi.sdk.errors.TxApiException on API errors (see {@link cloud.txapi.sdk.errors}) and transport problems
     */
    public Playtime playtime(PlaytimeQuery query, RequestOptions options) {
        return inv.call(Calls.events_pt1(query), options);
    }

    /**
     * Activity per hour (joins, players, deaths, kills, chat, commands, bans, warns), default window, UTC.
     *
     * <p>{@code GET /api/v1/events/activity} · permission {@code logs.view}
     *
     * @return the activity buckets
     * @throws cloud.txapi.sdk.errors.TxApiException on API errors (see {@link cloud.txapi.sdk.errors}) and transport problems
     */
    public Activity activity() {
        return inv.call(Calls.events_act0(), null);
    }

    /**
     * Activity per hour (joins, players, deaths, kills, chat, commands, bans, warns), default window, UTC.
     *
     * <p>{@code GET /api/v1/events/activity} · permission {@code logs.view}
     *
     * @param options per-call options (server, timeout, retries); {@code null} = defaults of the client/view
     * @return the activity buckets
     * @throws cloud.txapi.sdk.errors.TxApiException on API errors (see {@link cloud.txapi.sdk.errors}) and transport problems
     */
    public Activity activity(RequestOptions options) {
        return inv.call(Calls.events_act0(), options);
    }

    /**
     * Activity per hour or day, in a time zone of your choice.
     *
     * <p>{@code GET /api/v1/events/activity} · permission {@code logs.view}
     *
     * @param query window, bucket size and time zone; {@code null} = defaults
     * @return the activity buckets
     * @throws cloud.txapi.sdk.errors.TxApiException on API errors (see {@link cloud.txapi.sdk.errors}) and transport problems
     */
    public Activity activity(ActivityQuery query) {
        return inv.call(Calls.events_act1(query), null);
    }

    /**
     * Activity per hour or day, in a time zone of your choice.
     *
     * <p>{@code GET /api/v1/events/activity} · permission {@code logs.view}
     *
     * @param query window, bucket size and time zone; {@code null} = defaults
     * @param options per-call options (server, timeout, retries); {@code null} = defaults of the client/view
     * @return the activity buckets
     * @throws cloud.txapi.sdk.errors.TxApiException on API errors (see {@link cloud.txapi.sdk.errors}) and transport problems
     */
    public Activity activity(ActivityQuery query, RequestOptions options) {
        return inv.call(Calls.events_act1(query), options);
    }

    /**
     * txAdmin's own admin action log, read live from txAdmin (needs {@code txadmin.log.view} on the txAdmin account).
     *
     * <p>{@code GET /api/v1/txadmin/action-log} · permission {@code logs.view}
     *
     * @return the log lines
     * @throws cloud.txapi.sdk.errors.TxApiException on API errors (see {@link cloud.txapi.sdk.errors}) and transport problems
     */
    public ActionLog actionLog() {
        return inv.call(Calls.events_al0(), null);
    }

    /**
     * txAdmin's own admin action log, read live from txAdmin (needs {@code txadmin.log.view} on the txAdmin account).
     *
     * <p>{@code GET /api/v1/txadmin/action-log} · permission {@code logs.view}
     *
     * @param options per-call options (server, timeout, retries); {@code null} = defaults of the client/view
     * @return the log lines
     * @throws cloud.txapi.sdk.errors.TxApiException on API errors (see {@link cloud.txapi.sdk.errors}) and transport problems
     */
    public ActionLog actionLog(RequestOptions options) {
        return inv.call(Calls.events_al0(), options);
    }

    /**
     * txAdmin's own admin action log, the last {@code lines} lines.
     *
     * <p>{@code GET /api/v1/txadmin/action-log} · permission {@code logs.view}
     *
     * @param lines number of lines; {@code null} = API default
     * @return the log lines
     * @throws cloud.txapi.sdk.errors.TxApiException on API errors (see {@link cloud.txapi.sdk.errors}) and transport problems
     */
    public ActionLog actionLog(Integer lines) {
        return inv.call(Calls.events_al1(lines), null);
    }

    /**
     * txAdmin's own admin action log, the last {@code lines} lines.
     *
     * <p>{@code GET /api/v1/txadmin/action-log} · permission {@code logs.view}
     *
     * @param lines number of lines; {@code null} = API default
     * @param options per-call options (server, timeout, retries); {@code null} = defaults of the client/view
     * @return the log lines
     * @throws cloud.txapi.sdk.errors.TxApiException on API errors (see {@link cloud.txapi.sdk.errors}) and transport problems
     */
    public ActionLog actionLog(Integer lines, RequestOptions options) {
        return inv.call(Calls.events_al1(lines), options);
    }
}
