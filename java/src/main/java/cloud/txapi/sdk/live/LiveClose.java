package cloud.txapi.sdk.live;

/**
 * How a live stream ended for good.
 *
 * @param code     WebSocket close code: {@code 1000} closed by you, {@code 1008} rejected by the server
 *                 (invalid key, missing permission, quota used up, client too slow — see {@code reason}),
 *                 {@code 1009} message too big, {@code 1006} connection lost and not (or no longer)
 *                 reconnected
 * @param reason   close reason from the server, or a description of what happened
 * @param byClient {@code true} if {@link LiveStream#close()} ended it
 */
public record LiveClose(int code, String reason, boolean byClient) {
}
