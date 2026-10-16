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
 * Server state and control ({@code /server/…}). Resource commands: {@link #resources()}.
 *
 * <p>Obtain it from {@code client.server()}; do not construct it yourself.
 * Every method has an overload with a trailing {@link RequestOptions} for per-call settings
 * such as another {@code serverId}.
 *
 * @see AsyncServer
 */
@SuppressWarnings("unused")
public final class Server {
    private final Invoker inv;
    private final ServerResources resources;

    /**
     * Internal: created by the client.
     *
     * @param invoker internal call runner
     */
    public Server(Invoker invoker) {
        this.inv = invoker;
        this.resources = new ServerResources(invoker);
    }

    /**
     * Server name, versions, whether the FXServer process runs ({@link ServerStatus#online()}), whether txAdmin answers, and player statistics.
     *
     * <p>{@code GET /api/v1/server/status} · permission {@code server.control}
     *
     * @return the status
     * @throws cloud.txapi.sdk.errors.TxApiException on API errors (see {@link cloud.txapi.sdk.errors}) and transport problems
     */
    public ServerStatus status() {
        return inv.call(Calls.server_status(), null);
    }

    /**
     * Server name, versions, whether the FXServer process runs ({@link ServerStatus#online()}), whether txAdmin answers, and player statistics.
     *
     * <p>{@code GET /api/v1/server/status} · permission {@code server.control}
     *
     * @param options per-call options (server, timeout, retries); {@code null} = defaults of the client/view
     * @return the status
     * @throws cloud.txapi.sdk.errors.TxApiException on API errors (see {@link cloud.txapi.sdk.errors}) and transport problems
     */
    public ServerStatus status(RequestOptions options) {
        return inv.call(Calls.server_status(), options);
    }

    /**
     * txAdmin's diagnostics report (host, processes, FXServer), passed through unchanged.
     *
     * <p>{@code GET /api/v1/server/diagnostics} · permission {@code server.control}
     *
     * @return the report (raw JSON)
     * @throws cloud.txapi.sdk.errors.TxApiException on API errors (see {@link cloud.txapi.sdk.errors}) and transport problems
     */
    public JsonNode diagnostics() {
        return inv.call(Calls.server_diagnostics(), null);
    }

    /**
     * txAdmin's diagnostics report (host, processes, FXServer), passed through unchanged.
     *
     * <p>{@code GET /api/v1/server/diagnostics} · permission {@code server.control}
     *
     * @param options per-call options (server, timeout, retries); {@code null} = defaults of the client/view
     * @return the report (raw JSON)
     * @throws cloud.txapi.sdk.errors.TxApiException on API errors (see {@link cloud.txapi.sdk.errors}) and transport problems
     */
    public JsonNode diagnostics(RequestOptions options) {
        return inv.call(Calls.server_diagnostics(), options);
    }

    /**
     * Player drop (disconnect reason) statistics, passed through unchanged.
     *
     * <p>{@code GET /api/v1/server/drops} · permission {@code server.control}
     *
     * @return the statistics (raw JSON)
     * @throws cloud.txapi.sdk.errors.TxApiException on API errors (see {@link cloud.txapi.sdk.errors}) and transport problems
     */
    public JsonNode drops() {
        return inv.call(Calls.server_drops(), null);
    }

    /**
     * Player drop (disconnect reason) statistics, passed through unchanged.
     *
     * <p>{@code GET /api/v1/server/drops} · permission {@code server.control}
     *
     * @param options per-call options (server, timeout, retries); {@code null} = defaults of the client/view
     * @return the statistics (raw JSON)
     * @throws cloud.txapi.sdk.errors.TxApiException on API errors (see {@link cloud.txapi.sdk.errors}) and transport problems
     */
    public JsonNode drops(RequestOptions options) {
        return inv.call(Calls.server_drops(), options);
    }

    /**
     * Performance chart data (tick times, player count) of the main thread, passed through unchanged.
     *
     * <p>{@code GET /api/v1/server/performance} · permission {@code server.control}
     *
     * @return the chart data (raw JSON)
     * @throws cloud.txapi.sdk.errors.TxApiException on API errors (see {@link cloud.txapi.sdk.errors}) and transport problems
     */
    public JsonNode performance() {
        return inv.call(Calls.server_perf0(), null);
    }

    /**
     * Performance chart data (tick times, player count) of the main thread, passed through unchanged.
     *
     * <p>{@code GET /api/v1/server/performance} · permission {@code server.control}
     *
     * @param options per-call options (server, timeout, retries); {@code null} = defaults of the client/view
     * @return the chart data (raw JSON)
     * @throws cloud.txapi.sdk.errors.TxApiException on API errors (see {@link cloud.txapi.sdk.errors}) and transport problems
     */
    public JsonNode performance(RequestOptions options) {
        return inv.call(Calls.server_perf0(), options);
    }

    /**
     * Performance chart data of one thread, passed through unchanged.
     *
     * <p>{@code GET /api/v1/server/performance} · permission {@code server.control}
     *
     * @param thread thread name, e.g. {@code svMain}, {@code svSync}, {@code svNetwork}; {@code null} = API default
     * @return the chart data (raw JSON)
     * @throws cloud.txapi.sdk.errors.TxApiException on API errors (see {@link cloud.txapi.sdk.errors}) and transport problems
     */
    public JsonNode performance(String thread) {
        return inv.call(Calls.server_perf1(thread), null);
    }

    /**
     * Performance chart data of one thread, passed through unchanged.
     *
     * <p>{@code GET /api/v1/server/performance} · permission {@code server.control}
     *
     * @param thread thread name, e.g. {@code svMain}, {@code svSync}, {@code svNetwork}; {@code null} = API default
     * @param options per-call options (server, timeout, retries); {@code null} = defaults of the client/view
     * @return the chart data (raw JSON)
     * @throws cloud.txapi.sdk.errors.TxApiException on API errors (see {@link cloud.txapi.sdk.errors}) and transport problems
     */
    public JsonNode performance(String thread, RequestOptions options) {
        return inv.call(Calls.server_perf1(thread), options);
    }

    /**
     * Restarts the FXServer. <strong>Not retried automatically</strong> (it is a POST).
     *
     * <p>{@code POST /api/v1/server/restart} · permission {@code server.control}
     *
     * @return {@code success} and txAdmin's message
     * @throws cloud.txapi.sdk.errors.TxApiException on API errors (see {@link cloud.txapi.sdk.errors}) and transport problems
     */
    public ActionResult restart() {
        return inv.call(Calls.server_restart(), null);
    }

    /**
     * Restarts the FXServer. <strong>Not retried automatically</strong> (it is a POST).
     *
     * <p>{@code POST /api/v1/server/restart} · permission {@code server.control}
     *
     * @param options per-call options (server, timeout, retries); {@code null} = defaults of the client/view
     * @return {@code success} and txAdmin's message
     * @throws cloud.txapi.sdk.errors.TxApiException on API errors (see {@link cloud.txapi.sdk.errors}) and transport problems
     */
    public ActionResult restart(RequestOptions options) {
        return inv.call(Calls.server_restart(), options);
    }

    /**
     * Starts the FXServer.
     *
     * <p>{@code POST /api/v1/server/start} · permission {@code server.control}
     *
     * @return {@code success} and txAdmin's message
     * @throws cloud.txapi.sdk.errors.TxApiException on API errors (see {@link cloud.txapi.sdk.errors}) and transport problems
     */
    public ActionResult start() {
        return inv.call(Calls.server_start(), null);
    }

    /**
     * Starts the FXServer.
     *
     * <p>{@code POST /api/v1/server/start} · permission {@code server.control}
     *
     * @param options per-call options (server, timeout, retries); {@code null} = defaults of the client/view
     * @return {@code success} and txAdmin's message
     * @throws cloud.txapi.sdk.errors.TxApiException on API errors (see {@link cloud.txapi.sdk.errors}) and transport problems
     */
    public ActionResult start(RequestOptions options) {
        return inv.call(Calls.server_start(), options);
    }

    /**
     * Stops the FXServer.
     *
     * <p>{@code POST /api/v1/server/stop} · permission {@code server.control}
     *
     * @return {@code success} and txAdmin's message
     * @throws cloud.txapi.sdk.errors.TxApiException on API errors (see {@link cloud.txapi.sdk.errors}) and transport problems
     */
    public ActionResult stop() {
        return inv.call(Calls.server_stop(), null);
    }

    /**
     * Stops the FXServer.
     *
     * <p>{@code POST /api/v1/server/stop} · permission {@code server.control}
     *
     * @param options per-call options (server, timeout, retries); {@code null} = defaults of the client/view
     * @return {@code success} and txAdmin's message
     * @throws cloud.txapi.sdk.errors.TxApiException on API errors (see {@link cloud.txapi.sdk.errors}) and transport problems
     */
    public ActionResult stop(RequestOptions options) {
        return inv.call(Calls.server_stop(), options);
    }

    /**
     * Schedules a one-off restart through txAdmin's own scheduler (not txApi's schedules).
     *
     * <p>{@code POST /api/v1/server/schedule-restart} · permission {@code server.control}
     *
     * @param time {@code HH:MM} in the host's clock, or {@code +N} for N minutes from now (1–1439)
     * @return {@code success} and txAdmin's message
     * @throws cloud.txapi.sdk.errors.ValidationException if a required value is missing or blank, or too long (no request is sent)
     * @throws cloud.txapi.sdk.errors.TxApiException on API errors (see {@link cloud.txapi.sdk.errors}) and transport problems
     */
    public ActionResult scheduleRestart(String time) {
        return inv.call(Calls.server_scheduleRestart(time), null);
    }

    /**
     * Schedules a one-off restart through txAdmin's own scheduler (not txApi's schedules).
     *
     * <p>{@code POST /api/v1/server/schedule-restart} · permission {@code server.control}
     *
     * @param time {@code HH:MM} in the host's clock, or {@code +N} for N minutes from now (1–1439)
     * @param options per-call options (server, timeout, retries); {@code null} = defaults of the client/view
     * @return {@code success} and txAdmin's message
     * @throws cloud.txapi.sdk.errors.ValidationException if a required value is missing or blank, or too long (no request is sent)
     * @throws cloud.txapi.sdk.errors.TxApiException on API errors (see {@link cloud.txapi.sdk.errors}) and transport problems
     */
    public ActionResult scheduleRestart(String time, RequestOptions options) {
        return inv.call(Calls.server_scheduleRestart(time), options);
    }

    /**
     * Skips (or un-skips) the next scheduled restart in txAdmin.
     *
     * <p>{@code POST /api/v1/server/skip-restart} · permission {@code server.control}
     *
     * @param skip {@code true} = skip the next scheduled restart, {@code false} = undo
     * @return {@code success} and txAdmin's message
     * @throws cloud.txapi.sdk.errors.ValidationException if a required value is missing or blank, or too long (no request is sent)
     * @throws cloud.txapi.sdk.errors.TxApiException on API errors (see {@link cloud.txapi.sdk.errors}) and transport problems
     */
    public ActionResult skipRestart(boolean skip) {
        return inv.call(Calls.server_skipRestart(skip), null);
    }

    /**
     * Skips (or un-skips) the next scheduled restart in txAdmin.
     *
     * <p>{@code POST /api/v1/server/skip-restart} · permission {@code server.control}
     *
     * @param skip {@code true} = skip the next scheduled restart, {@code false} = undo
     * @param options per-call options (server, timeout, retries); {@code null} = defaults of the client/view
     * @return {@code success} and txAdmin's message
     * @throws cloud.txapi.sdk.errors.ValidationException if a required value is missing or blank, or too long (no request is sent)
     * @throws cloud.txapi.sdk.errors.TxApiException on API errors (see {@link cloud.txapi.sdk.errors}) and transport problems
     */
    public ActionResult skipRestart(boolean skip, RequestOptions options) {
        return inv.call(Calls.server_skipRestart(skip), options);
    }

    /**
     * Kicks every connected player without a reason.
     *
     * <p>{@code POST /api/v1/server/kickall} · permission {@code server.control}
     *
     * @return {@code success} and txAdmin's message
     * @throws cloud.txapi.sdk.errors.ValidationException if a required value is missing or blank, or too long (no request is sent)
     * @throws cloud.txapi.sdk.errors.TxApiException on API errors (see {@link cloud.txapi.sdk.errors}) and transport problems
     */
    public ActionResult kickAll() {
        return inv.call(Calls.server_kickall0(), null);
    }

    /**
     * Kicks every connected player without a reason.
     *
     * <p>{@code POST /api/v1/server/kickall} · permission {@code server.control}
     *
     * @param options per-call options (server, timeout, retries); {@code null} = defaults of the client/view
     * @return {@code success} and txAdmin's message
     * @throws cloud.txapi.sdk.errors.ValidationException if a required value is missing or blank, or too long (no request is sent)
     * @throws cloud.txapi.sdk.errors.TxApiException on API errors (see {@link cloud.txapi.sdk.errors}) and transport problems
     */
    public ActionResult kickAll(RequestOptions options) {
        return inv.call(Calls.server_kickall0(), options);
    }

    /**
     * Kicks every connected player.
     *
     * <p>{@code POST /api/v1/server/kickall} · permission {@code server.control}
     *
     * @param reason reason shown to the players, at most 512 characters; {@code null} = none
     * @return {@code success} and txAdmin's message
     * @throws cloud.txapi.sdk.errors.ValidationException if a required value is missing or blank, or too long (no request is sent)
     * @throws cloud.txapi.sdk.errors.TxApiException on API errors (see {@link cloud.txapi.sdk.errors}) and transport problems
     */
    public ActionResult kickAll(String reason) {
        return inv.call(Calls.server_kickall1(reason), null);
    }

    /**
     * Kicks every connected player.
     *
     * <p>{@code POST /api/v1/server/kickall} · permission {@code server.control}
     *
     * @param reason reason shown to the players, at most 512 characters; {@code null} = none
     * @param options per-call options (server, timeout, retries); {@code null} = defaults of the client/view
     * @return {@code success} and txAdmin's message
     * @throws cloud.txapi.sdk.errors.ValidationException if a required value is missing or blank, or too long (no request is sent)
     * @throws cloud.txapi.sdk.errors.TxApiException on API errors (see {@link cloud.txapi.sdk.errors}) and transport problems
     */
    public ActionResult kickAll(String reason, RequestOptions options) {
        return inv.call(Calls.server_kickall1(reason), options);
    }

    /**
     * Shows an announcement to every player in game.
     *
     * <p>{@code POST /api/v1/server/announcement} · permission {@code server.control}
     *
     * @param message announcement text, at most 512 characters
     * @return {@code success} and txAdmin's message
     * @throws cloud.txapi.sdk.errors.ValidationException if a required value is missing or blank, or too long (no request is sent)
     * @throws cloud.txapi.sdk.errors.TxApiException on API errors (see {@link cloud.txapi.sdk.errors}) and transport problems
     */
    public ActionResult announce(String message) {
        return inv.call(Calls.server_announce(message), null);
    }

    /**
     * Shows an announcement to every player in game.
     *
     * <p>{@code POST /api/v1/server/announcement} · permission {@code server.control}
     *
     * @param message announcement text, at most 512 characters
     * @param options per-call options (server, timeout, retries); {@code null} = defaults of the client/view
     * @return {@code success} and txAdmin's message
     * @throws cloud.txapi.sdk.errors.ValidationException if a required value is missing or blank, or too long (no request is sent)
     * @throws cloud.txapi.sdk.errors.TxApiException on API errors (see {@link cloud.txapi.sdk.errors}) and transport problems
     */
    public ActionResult announce(String message, RequestOptions options) {
        return inv.call(Calls.server_announce(message), options);
    }

    /**
     * Resource commands ({@code /server/resource/…}).
     *
     * @return the resources group (same per-call defaults as this group)
     */
    public ServerResources resources() {
        return resources;
    }
}
