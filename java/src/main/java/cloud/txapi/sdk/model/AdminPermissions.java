package cloud.txapi.sdk.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.List;

/**
 * The txAdmin permission keys that can be granted (they change per txAdmin version).
 *
 * @param permissions available permissions
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record AdminPermissions(List<Permission> permissions) {
    public AdminPermissions {
        permissions = Lists.of(permissions);
    }

    /**
     * @param id    permission key, e.g. {@code players.ban}
     * @param label display label
     */
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Permission(String id, String label) {
    }
}
