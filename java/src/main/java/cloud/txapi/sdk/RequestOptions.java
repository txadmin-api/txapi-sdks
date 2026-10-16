package cloud.txapi.sdk;

import java.time.Duration;
import java.util.Objects;
import java.util.Optional;
import java.util.OptionalInt;
import java.util.OptionalLong;

/**
 * Options for a single call (or for a view created with {@link TxApi#withOptions(RequestOptions)}).
 * Every value left unset falls back to the client configuration.
 *
 * <pre>{@code
 * client.players().ban(request, RequestOptions.serverId(3));
 * client.server().status(RequestOptions.builder().serverId(3).timeout(Duration.ofSeconds(5)).build());
 * }</pre>
 */
public final class RequestOptions {
    private static final RequestOptions NONE = new RequestOptions(null, null, null);

    private final Long serverId;
    private final Duration timeout;
    private final Integer maxRetries;

    private RequestOptions(Long serverId, Duration timeout, Integer maxRetries) {
        this.serverId = serverId;
        this.timeout = timeout;
        this.maxRetries = maxRetries;
    }

    /** Options that change nothing. */
    public static RequestOptions none() {
        return NONE;
    }

    /** Address a different txAdmin server than the client default. */
    public static RequestOptions serverId(long serverId) {
        return builder().serverId(serverId).build();
    }

    public static Builder builder() {
        return new Builder();
    }

    /** Server to address ({@code ?serverId=}); ignored by account-level endpoints. */
    public OptionalLong serverId() {
        return serverId == null ? OptionalLong.empty() : OptionalLong.of(serverId);
    }

    /** Timeout per attempt. */
    public Optional<Duration> timeout() {
        return Optional.ofNullable(timeout);
    }

    /** Maximum number of automatic retries. */
    public OptionalInt maxRetries() {
        return maxRetries == null ? OptionalInt.empty() : OptionalInt.of(maxRetries);
    }

    /** Values set here win over values set in {@code base}. */
    public RequestOptions over(RequestOptions base) {
        if (base == null || base == NONE) {
            return this;
        }
        return new RequestOptions(
                serverId != null ? serverId : base.serverId,
                timeout != null ? timeout : base.timeout,
                maxRetries != null ? maxRetries : base.maxRetries);
    }

    public Builder toBuilder() {
        Builder b = new Builder();
        b.serverId = serverId;
        b.timeout = timeout;
        b.maxRetries = maxRetries;
        return b;
    }

    @Override
    public boolean equals(Object o) {
        return o instanceof RequestOptions r && Objects.equals(serverId, r.serverId)
                && Objects.equals(timeout, r.timeout) && Objects.equals(maxRetries, r.maxRetries);
    }

    @Override
    public int hashCode() {
        return Objects.hash(serverId, timeout, maxRetries);
    }

    @Override
    public String toString() {
        return "RequestOptions{serverId=" + serverId + ", timeout=" + timeout + ", maxRetries=" + maxRetries + "}";
    }

    /** Builder for {@link RequestOptions}. */
    public static final class Builder {
        private Long serverId;
        private Duration timeout;
        private Integer maxRetries;

        private Builder() {
        }

        public Builder serverId(long serverId) {
            this.serverId = serverId;
            return this;
        }

        public Builder timeout(Duration timeout) {
            if (timeout != null && (timeout.isNegative() || timeout.isZero())) {
                throw new IllegalArgumentException("timeout must be positive");
            }
            this.timeout = timeout;
            return this;
        }

        public Builder maxRetries(int maxRetries) {
            if (maxRetries < 0) {
                throw new IllegalArgumentException("maxRetries must be >= 0");
            }
            this.maxRetries = maxRetries;
            return this;
        }

        public RequestOptions build() {
            return new RequestOptions(serverId, timeout, maxRetries);
        }
    }
}
