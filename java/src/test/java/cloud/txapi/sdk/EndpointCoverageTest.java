package cloud.txapi.sdk;

import cloud.txapi.sdk.internal.Json;
import cloud.txapi.sdk.params.AdminCreateRequest;
import cloud.txapi.sdk.params.AdminUpdateRequest;
import cloud.txapi.sdk.params.BanRequest;
import cloud.txapi.sdk.params.PageCursor;
import cloud.txapi.sdk.params.RevokeRequest;
import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.CompletableFuture;
import java.util.regex.Pattern;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Calls every public method of every resource group (blocking and asynchronous) against a mock
 * server and checks that together they hit exactly the endpoints of {@code spec/endpoints.json}.
 */
class EndpointCoverageTest {
    private static MockApi mock;
    private static TxApi client;
    private static List<SpecEndpoint> spec;

    record SpecEndpoint(String method, String path, Pattern regex, int params, boolean accountLevel) {
        String key() {
            return method + " " + path;
        }
    }

    @BeforeAll
    static void setUp() throws Exception {
        JsonNode root = Json.MAPPER.readTree(Files.readAllBytes(Path.of("..", "spec", "endpoints.json")));
        assertThat(root.path("apiVersion").asText()).isEqualTo(TxApi.API_VERSION);
        spec = new ArrayList<>();
        for (JsonNode e : root.path("endpoints")) {
            String path = e.path("path").asText();
            String regex = "^" + Pattern.quote(path).replaceAll("\\{[^}]+}", "\\\\E[^/]+\\\\Q") + "$";
            spec.add(new SpecEndpoint(e.path("method").asText(), path, Pattern.compile(regex),
                    (int) path.chars().filter(c -> c == '{').count(), e.path("accountLevel").asBoolean()));
        }
        assertThat(spec).hasSize(67);
        mock = new MockApi().handler(r -> {
            boolean list = r.method().equals("GET")
                    && (r.rawPath().endsWith("/ban-templates") || r.rawPath().endsWith("/whitelist/approvals"));
            return MockApi.Resp.ok(list ? "[]" : "{}");
        });
        client = mock.client(b -> b.serverId(7L).maxRetries(0));
    }

    @AfterAll
    static void tearDown() {
        client.close();
        mock.close();
    }

    @Test
    void blockingApiCoversEverySpecEndpoint() throws Exception {
        Set<String> hit = exercise(client, false);
        assertThat(hit).containsExactlyInAnyOrderElementsOf(allSpecKeys());
    }

    @Test
    void asyncApiCoversEverySpecEndpoint() throws Exception {
        Set<String> hit = exercise(client.async(), true);
        assertThat(hit).containsExactlyInAnyOrderElementsOf(allSpecKeys());
    }

    @Test
    void serverIdIsSentExactlyForServerScopedEndpoints() throws Exception {
        mock.requests.clear();
        exercise(client, false);
        for (MockApi.Req r : mock.requests) {
            SpecEndpoint ep = match(r);
            if (ep.accountLevel()) {
                assertThat(r.query()).as(ep.key()).doesNotContainKey("serverId");
            } else {
                assertThat(r.query()).as(ep.key()).containsEntry("serverId", "7");
            }
        }
    }

    private static Set<String> allSpecKeys() {
        Set<String> keys = new TreeSet<>();
        spec.forEach(e -> keys.add(e.key()));
        return keys;
    }

    private static Set<String> exercise(Object root, boolean async) throws Exception {
        Set<String> hit = new TreeSet<>();
        List<Object> groups = new ArrayList<>();
        for (String g : List.of("account", "players", "history", "server", "console", "serverLog", "events",
                "whitelist", "admins")) {
            groups.add(root.getClass().getMethod(g).invoke(root));
        }
        for (int i = 0; i < groups.size(); i++) {
            Object group = groups.get(i);
            Method[] methods = group.getClass().getDeclaredMethods();
            java.util.Arrays.sort(methods, Comparator.comparing(Method::toGenericString));
            for (Method m : methods) {
                if (!Modifier.isPublic(m.getModifiers()) || Modifier.isStatic(m.getModifiers())) {
                    continue;
                }
                Class<?> rt = m.getReturnType();
                if (rt.getPackageName().equals("cloud.txapi.sdk.resources") && m.getParameterCount() == 0) {
                    groups.add(m.invoke(group)); // child group (server().resources(), whitelist().approvals())
                    continue;
                }
                if (PageIterable.class.isAssignableFrom(rt) || AsyncPageIterable.class.isAssignableFrom(rt)
                        || m.getName().equals("connect") || m.getName().equals("toString")) {
                    continue; // pagers and live streams have their own tests
                }
                int before = mock.requests.size();
                Object result;
                try {
                    result = m.invoke(group, args(m));
                } catch (InvocationTargetException e) {
                    throw new AssertionError(m.toGenericString() + " threw " + e.getCause(), e.getCause());
                }
                if (async) {
                    assertThat(result).as(m.toGenericString()).isInstanceOf(CompletableFuture.class);
                    ((CompletableFuture<?>) result).join();
                }
                assertThat(mock.requests.size() - before).as(m.toGenericString() + " makes one request").isEqualTo(1);
                hit.add(match(mock.last()).key());
            }
        }
        return hit;
    }

    private static SpecEndpoint match(MockApi.Req r) {
        return spec.stream()
                .filter(e -> e.method().equals(r.method()) && e.regex().matcher(r.rawPath()).matches())
                .min(Comparator.comparingInt(SpecEndpoint::params))
                .orElseThrow(() -> new AssertionError("request matches no spec endpoint: " + r.method() + " " + r.rawPath()));
    }

    private static Object[] args(Method m) {
        Class<?>[] types = m.getParameterTypes();
        Object[] out = new Object[types.length];
        for (int i = 0; i < types.length; i++) {
            out[i] = value(types[i]);
        }
        return out;
    }

    private static Object value(Class<?> t) {
        return EndpointCoverageTestValues.value(t);
    }
}
