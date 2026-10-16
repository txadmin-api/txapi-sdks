package cloud.txapi.sdk.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.JsonNode;

import java.util.List;

/**
 * The last server-log entries: txAdmin's structured entries as JSON objects
 * ({@code ts}, {@code type}, {@code src}, {@code msg}), anything else as JSON strings.
 *
 * @param entries the entries, oldest first
 * @param count   number of entries
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record ServerLogBuffer(List<JsonNode> entries, int count) {
    public ServerLogBuffer {
        entries = Lists.of(entries);
    }
}
