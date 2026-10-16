package cloud.txapi.sdk.live;

import java.time.Duration;
import java.util.Objects;

/**
 * Settings of a live stream. Immutable; every setter returns a copy.
 *
 * <pre>{@code
 * LiveOptions opts = LiveOptions.defaults()
 *         .serverId(2)                              // another server than the client default
 *         .maxReconnectDelay(Duration.ofSeconds(10));
 * }</pre>
 */
public final class LiveOptions {
    private static final LiveOptions DEFAULTS = new LiveOptions(null, true, Duration.ofSeconds(1),
            Duration.ofSeconds(30), -1, Duration.ofSeconds(10), null);

    private final Long serverId;
    private final boolean reconnect;
    private final Duration initialReconnectDelay;
    private final Duration maxReconnectDelay;
    private final int maxReconnectAttempts;
    private final Duration commandTimeout;
    private final Duration connectTimeout;

    private LiveOptions(Long serverId, boolean reconnect, Duration initialReconnectDelay, Duration maxReconnectDelay,
                        int maxReconnectAttempts, Duration commandTimeout, Duration connectTimeout) {
        this.serverId = serverId;
        this.reconnect = reconnect;
        this.initialReconnectDelay = initialReconnectDelay;
        this.maxReconnectDelay = maxReconnectDelay;
        this.maxReconnectAttempts = maxReconnectAttempts;
        this.commandTimeout = commandTimeout;
        this.connectTimeout = connectTimeout;
    }

    /**
     * Defaults: client's server, automatic reconnect (1 s → 30 s with jitter, unlimited attempts),
     * 10 s command acknowledgement timeout, client timeout for the handshake.
     *
     * @return the defaults
     */
    public static LiveOptions defaults() {
        return DEFAULTS;
    }

    private static Duration positive(Duration d, String name) {
        Objects.requireNonNull(d, name);
        if (d.isNegative() || d.isZero()) {
            throw new IllegalArgumentException(name + " must be positive");
        }
        return d;
    }

    /** @param id server to stream from (instead of the client default) @return a copy */
    public LiveOptions serverId(long id) {
        return new LiveOptions(id, reconnect, initialReconnectDelay, maxReconnectDelay, maxReconnectAttempts,
                commandTimeout, connectTimeout);
    }

    /** @param enabled reconnect automatically after a lost connection (default {@code true}) @return a copy */
    public LiveOptions reconnect(boolean enabled) {
        return new LiveOptions(serverId, enabled, initialReconnectDelay, maxReconnectDelay, maxReconnectAttempts,
                commandTimeout, connectTimeout);
    }

    /** @param delay first reconnect delay before jitter (default 1 s) @return a copy */
    public LiveOptions initialReconnectDelay(Duration delay) {
        return new LiveOptions(serverId, reconnect, positive(delay, "initialReconnectDelay"), maxReconnectDelay,
                maxReconnectAttempts, commandTimeout, connectTimeout);
    }

    /** @param delay upper bound of the reconnect delay (default 30 s) @return a copy */
    public LiveOptions maxReconnectDelay(Duration delay) {
        return new LiveOptions(serverId, reconnect, initialReconnectDelay, positive(delay, "maxReconnectDelay"),
                maxReconnectAttempts, commandTimeout, connectTimeout);
    }

    /** @param attempts give up after this many failed reconnects in a row; negative = never (default) @return a copy */
    public LiveOptions maxReconnectAttempts(int attempts) {
        return new LiveOptions(serverId, reconnect, initialReconnectDelay, maxReconnectDelay, attempts,
                commandTimeout, connectTimeout);
    }

    /** @param timeout how long {@code sendCommand} waits for the acknowledgement (default 10 s) @return a copy */
    public LiveOptions commandTimeout(Duration timeout) {
        return new LiveOptions(serverId, reconnect, initialReconnectDelay, maxReconnectDelay, maxReconnectAttempts,
                positive(timeout, "commandTimeout"), connectTimeout);
    }

    /** @param timeout handshake timeout (default: the client's request timeout) @return a copy */
    public LiveOptions connectTimeout(Duration timeout) {
        return new LiveOptions(serverId, reconnect, initialReconnectDelay, maxReconnectDelay, maxReconnectAttempts,
                commandTimeout, positive(timeout, "connectTimeout"));
    }

    /** @return the server id, or {@code null} for the client default */
    public Long serverId() {
        return serverId;
    }

    /** @return whether to reconnect automatically */
    public boolean reconnect() {
        return reconnect;
    }

    /** @return the first reconnect delay */
    public Duration initialReconnectDelay() {
        return initialReconnectDelay;
    }

    /** @return the maximum reconnect delay */
    public Duration maxReconnectDelay() {
        return maxReconnectDelay;
    }

    /** @return the maximum number of failed reconnects in a row, negative = unlimited */
    public int maxReconnectAttempts() {
        return maxReconnectAttempts;
    }

    /** @return the command acknowledgement timeout */
    public Duration commandTimeout() {
        return commandTimeout;
    }

    /** @return the handshake timeout, or {@code null} for the client's request timeout */
    public Duration connectTimeout() {
        return connectTimeout;
    }

    @Override
    public String toString() {
        return "LiveOptions{serverId=" + serverId + ", reconnect=" + reconnect + ", initialReconnectDelay="
                + initialReconnectDelay + ", maxReconnectDelay=" + maxReconnectDelay + ", maxReconnectAttempts="
                + maxReconnectAttempts + ", commandTimeout=" + commandTimeout + ", connectTimeout=" + connectTimeout + '}';
    }
}
