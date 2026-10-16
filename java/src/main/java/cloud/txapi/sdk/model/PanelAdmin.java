package cloud.txapi.sdk.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * A txAdmin panel admin as shown in the admin list.
 *
 * @param name         login name
 * @param hasPassword  has a password login
 * @param hasCitizenfx has a CitizenFX login
 * @param hasDiscord   has a Discord login
 * @param permsSummary {@code Master Account}, {@code All Permissions} or a count
 * @param master       the master account
 * @param self         the account the bridge itself uses
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record PanelAdmin(String name, boolean hasPassword, boolean hasCitizenfx, boolean hasDiscord,
                         String permsSummary,
                         @com.fasterxml.jackson.annotation.JsonProperty("isMaster") boolean master,
                         @com.fasterxml.jackson.annotation.JsonProperty("isSelf") boolean self) {
}
