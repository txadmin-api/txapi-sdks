package cloud.txapi.sdk.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.time.Instant;

/**
 * One server of the account (no URL, no credentials).
 *
 * @param id              the id to pass as {@code serverId}
 * @param name            name in the dashboard
 * @param txServerName    server name reported by txAdmin
 * @param active          whether the server is enabled
 * @param health          last offline check: {@code ONLINE}, {@code STOPPED}, {@code UNREACHABLE}
 *                        (only with offline alerts enabled)
 * @param healthCheckedAt time of that check
 * @param offlineAlerts   offline alerts enabled
 * @param eventLog        event log enabled
 * @param banSync         ban sync enabled
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record ServerSummary(long id, String name, String txServerName, boolean active, String health,
                            Instant healthCheckedAt, boolean offlineAlerts, boolean eventLog, boolean banSync) {
}
