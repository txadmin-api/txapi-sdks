package cloud.txapi.sdk.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.time.Instant;
import java.util.List;

/**
 * {@code GET /me}: the calling account, its plan, quota and the key in use.
 *
 * @param success      always {@code true}
 * @param accountId    account id
 * @param plan         plan code and effective features (plan ∩ key scopes)
 * @param quota        monthly request quota
 * @param rpmLimit     requests per minute; {@code null} when unlimited
 * @param rpmUnlimited {@code true} when there is no per-minute limit
 * @param auth         {@code apiKey} or {@code discordBot}
 * @param key          the API key used ({@code null} when called through the Discord bot)
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record AccountInfo(boolean success, Long accountId, Plan plan, Quota quota, Integer rpmLimit,
                          boolean rpmUnlimited, String auth, Key key) {

    /**
     * @param code             plan code, e.g. {@code professional}
     * @param features         effective permission keys
     * @param serverLimit      number of servers allowed; {@code null} = unlimited
     * @param logRetentionDays how long the event log is kept
     */
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Plan(String code, List<String> features, Integer serverLimit, Integer logRetentionDays) {
        public Plan {
            features = Lists.of(features);
        }

        /** Whether the plan (and key) grants a permission, e.g. {@code players.write}. */
        public boolean has(String permission) {
            return features.contains(permission);
        }
    }

    /**
     * @param unlimited {@code true} when the quota is unlimited ({@code limit}/{@code remaining} are then null)
     * @param used      requests used this month
     * @param limit     monthly limit
     * @param remaining requests left
     * @param resetsAt  1st of next month, 00:00 UTC
     */
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Quota(boolean unlimited, Long used, Long limit, Long remaining, Instant resetsAt) {
    }

    /**
     * @param id         key id
     * @param name       key name
     * @param prefix     visible key prefix
     * @param serverId   server the key is bound to, {@code null} = all servers
     * @param expiresAt  expiry, {@code null} = never
     * @param scopes     key scopes (empty = the whole plan)
     * @param allowedIps IP allow-list (empty = any IP)
     */
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Key(Long id, String name, String prefix, Long serverId, Instant expiresAt,
                      List<String> scopes, List<String> allowedIps) {
        public Key {
            scopes = Lists.of(scopes);
            allowedIps = Lists.of(allowedIps);
        }
    }
}
