package cloud.txapi.sdk.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

/**
 * One txAdmin panel admin with full permission keys.
 *
 * @param name        login name
 * @param citizenfxId CitizenFX id (empty when not set)
 * @param discordId   Discord id (empty when not set)
 * @param permissions txAdmin permission keys
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record AdminDetails(String name,
                           @JsonProperty("citizenfxID") String citizenfxId,
                           @JsonProperty("discordID") String discordId,
                           List<String> permissions) {
    public AdminDetails {
        permissions = Lists.of(permissions);
    }
}
