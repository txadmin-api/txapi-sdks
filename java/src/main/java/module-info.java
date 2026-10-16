/**
 * Official Java SDK for the txApi REST and WebSocket API.
 *
 * <p>Start with {@link cloud.txapi.sdk.TxApi#builder()}.
 */
module cloud.txapi.sdk {
    requires transitive com.fasterxml.jackson.databind;
    requires transitive java.net.http;

    exports cloud.txapi.sdk;
    exports cloud.txapi.sdk.errors;
    exports cloud.txapi.sdk.live;
    exports cloud.txapi.sdk.model;
    exports cloud.txapi.sdk.params;
    exports cloud.txapi.sdk.resources;
    exports cloud.txapi.sdk.webhooks;

    // Jackson builds the response records reflectively.
    opens cloud.txapi.sdk.model to com.fasterxml.jackson.databind;
}
