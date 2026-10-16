package cloud.txapi.sdk;

import cloud.txapi.sdk.params.AdminCreateRequest;
import cloud.txapi.sdk.params.AdminUpdateRequest;
import cloud.txapi.sdk.params.BanRequest;
import cloud.txapi.sdk.params.PageCursor;
import cloud.txapi.sdk.params.RevokeRequest;

import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.util.List;

/** Dummy arguments for reflective calls in tests. */
final class EndpointCoverageTestValues {
    private EndpointCoverageTestValues() {
    }

    static Object value(Class<?> t) {
        if (t == String.class) {
            return "1h";
        }
        if (t == long.class || t == Long.class) {
            return 1L;
        }
        if (t == int.class || t == Integer.class) {
            return 1;
        }
        if (t == boolean.class || t == Boolean.class) {
            return Boolean.TRUE;
        }
        if (t == RequestOptions.class) {
            return RequestOptions.none();
        }
        if (t == List.class) {
            return List.of("players.read");
        }
        if (t == Duration.class) {
            return Duration.ofHours(1);
        }
        if (t == Instant.class) {
            return Instant.parse("2026-10-01T00:00:00Z");
        }
        if (t == ZoneId.class) {
            return ZoneId.of("Europe/Vienna");
        }
        if (t == BanRequest.class) {
            return BanRequest.of("license:abc", "cheating");
        }
        if (t == RevokeRequest.class) {
            return RevokeRequest.reason("appeal accepted");
        }
        if (t == AdminCreateRequest.class) {
            return AdminCreateRequest.of("mod", List.of("players.read"));
        }
        if (t == AdminUpdateRequest.class) {
            return AdminUpdateRequest.create().withPermissions(List.of("players.read"));
        }
        if (t == PageCursor.class) {
            return PageCursor.of(25, "license:abc");
        }
        if (t.isEnum()) {
            return t.getEnumConstants()[0];
        }
        try {
            return t.getMethod("create").invoke(null);
        } catch (ReflectiveOperationException e) {
            throw new AssertionError("no test value for " + t, e);
        }
    }
}
