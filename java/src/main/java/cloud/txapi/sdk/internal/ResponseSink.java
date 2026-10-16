package cloud.txapi.sdk.internal;

import cloud.txapi.sdk.RateLimit;

import java.util.List;
import java.util.Map;

/** Receives the HTTP metadata of every successful response (used by {@code withRawResponse}). */
@FunctionalInterface
public interface ResponseSink {
    ResponseSink NONE = (status, headers, rateLimit) -> { };

    void accept(int status, Map<String, List<String>> headers, RateLimit rateLimit);
}
