package cloud.txapi.sdk.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.JsonNode;

/**
 * A ban template from txAdmin's ban dialog.
 *
 * @param id       template id
 * @param reason   reason text
 * @param duration either the string {@code "permanent"} or an object {@code {value, unit}}
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record BanTemplate(String id, String reason, JsonNode duration) {

    /** {@code true} when the template bans permanently. */
    public boolean isPermanent() {
        return duration != null && duration.isTextual() && "permanent".equals(duration.asText());
    }
}
