package cloud.txapi.sdk.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.List;

/**
 * {@code GET /servers}: the servers this key can address with {@code serverId}.
 *
 * @param success       always {@code true}
 * @param count         number of servers
 * @param boundServerId server the key is bound to, {@code null} for an unbound key
 * @param servers       the servers
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record ServerList(boolean success, int count, Long boundServerId, List<ServerSummary> servers) {
    public ServerList {
        servers = Lists.of(servers);
    }
}
