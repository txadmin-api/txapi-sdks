package cloud.txapi.sdk.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.JsonNode;

import java.util.List;

/**
 * An older page of the server log, live from txAdmin.
 *
 * @param boundary {@code true} when the start/end of txAdmin's buffer was reached
 *                 (JSON field {@code boundry}, spelled as txAdmin spells it)
 * @param log      the entries
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record ServerLogHistory(@JsonProperty("boundry") boolean boundary, List<JsonNode> log) {
    public ServerLogHistory {
        log = Lists.of(log);
    }
}
