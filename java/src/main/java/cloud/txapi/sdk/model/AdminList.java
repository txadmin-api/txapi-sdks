package cloud.txapi.sdk.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.List;

/**
 * The txAdmin panel admins.
 *
 * @param admins the admins
 * @param total  number reported by the API
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record AdminList(List<PanelAdmin> admins, int total) {
    public AdminList {
        admins = Lists.of(admins);
    }
}
