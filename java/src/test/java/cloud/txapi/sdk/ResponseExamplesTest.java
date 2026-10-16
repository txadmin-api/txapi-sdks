package cloud.txapi.sdk;

import cloud.txapi.sdk.internal.Json;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.io.InputStream;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.RecordComponent;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.regex.Pattern;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Feeds the documented response example of every endpoint ({@code src/test/resources/response-examples.json},
 * extracted from the website's API catalog) through the typed methods and checks that each
 * documented top-level field lands in the model — a renamed field would otherwise silently be
 * {@code null}.
 *
 * <p>The fixture {@code response-examples.json} holds the documented example response of
 * every endpoint, keyed by {@code "METHOD /path"}. It is generated from the API catalog in
 * the txApi main repository together with {@code spec/endpoints.json} — do not edit it by hand.
 */
class ResponseExamplesTest {
    private static MockApi mock;
    private static TxApi client;
    private static final Map<String, JsonNode> EXAMPLES = new TreeMap<>();
    private static final Map<Pattern, String> TEMPLATES = new java.util.LinkedHashMap<>();

    @BeforeAll
    static void setUp() throws Exception {
        try (InputStream in = ResponseExamplesTest.class.getResourceAsStream("/response-examples.json")) {
            JsonNode root = Json.MAPPER.readTree(in);
            root.fields().forEachRemaining(e -> EXAMPLES.put(e.getKey(), e.getValue()));
        }
        List<String> keys = new ArrayList<>(EXAMPLES.keySet());
        keys.sort(Comparator.comparingInt(k -> (int) k.chars().filter(c -> c == '{').count()));
        for (String k : keys) {
            TEMPLATES.put(Pattern.compile("^" + Pattern.quote(k).replaceAll("\\{[^}]+}", "\\\\E[^/]+\\\\Q") + "$"), k);
        }
        mock = new MockApi().handler(r -> {
            String key = example(r.method() + " " + r.rawPath());
            if (key != null) {
                return MockApi.Resp.ok(EXAMPLES.get(key).toString());
            }
            boolean list = r.method().equals("GET") && r.rawPath().endsWith("/ban-templates");
            return MockApi.Resp.ok(list ? "[]" : "{}");
        });
        client = mock.client(b -> b.serverId(1L).maxRetries(0));
    }

    @AfterAll
    static void tearDown() {
        client.close();
        mock.close();
    }

    private static String example(String request) {
        for (Map.Entry<Pattern, String> e : TEMPLATES.entrySet()) {
            if (e.getKey().matcher(request).matches()) {
                return e.getValue();
            }
        }
        return null;
    }

    @Test
    void everyDocumentedFieldIsMapped() throws Exception {
        List<String> problems = new ArrayList<>();
        int checked = 0;
        List<Object> groups = new ArrayList<>();
        for (String g : List.of("account", "players", "history", "server", "console", "serverLog", "events",
                "whitelist", "admins")) {
            groups.add(TxApi.class.getMethod(g).invoke(client));
        }
        for (int i = 0; i < groups.size(); i++) {
            Object group = groups.get(i);
            for (Method m : group.getClass().getDeclaredMethods()) {
                if (!Modifier.isPublic(m.getModifiers()) || m.getParameterCount() > 0 && hasOptions(m)) {
                    continue;
                }
                Class<?> rt = m.getReturnType();
                if (rt.getPackageName().equals("cloud.txapi.sdk.resources") && m.getParameterCount() == 0) {
                    groups.add(m.invoke(group));
                    continue;
                }
                if (PageIterable.class.isAssignableFrom(rt) || m.getName().equals("connect")
                        || m.getName().equals("toString")) {
                    continue;
                }
                Object[] args = new Object[m.getParameterCount()];
                for (int a = 0; a < args.length; a++) {
                    args[a] = EndpointCoverageTestValues.value(m.getParameterTypes()[a]);
                }
                Object result = m.invoke(group, args);
                String key = example(mock.last().method() + " " + mock.last().rawPath());
                if (key == null) {
                    continue;
                }
                checked++;
                check(m.getDeclaringClass().getSimpleName() + "." + m.getName(), EXAMPLES.get(key), result, problems);
            }
        }
        assertThat(checked).isGreaterThanOrEqualTo(55);
        assertThat(problems).isEmpty();
    }

    private static boolean hasOptions(Method m) {
        return m.getParameterTypes()[m.getParameterCount() - 1] == RequestOptions.class;
    }

    /** Every top-level key of the example must be present in the record as a non-null value. */
    private static void check(String where, JsonNode example, Object result, List<String> problems) {
        if (result == null) {
            problems.add(where + ": null result");
            return;
        }
        if (result instanceof JsonNode node) {
            if (!node.equals(example)) {
                problems.add(where + ": JsonNode passthrough differs");
            }
            return;
        }
        if (result instanceof Collection<?> c) {
            if (example.isArray() && c.size() != example.size()) {
                problems.add(where + ": list size " + c.size() + " != " + example.size());
            }
            Iterator<?> it = c.iterator();
            for (JsonNode item : example) {
                if (it.hasNext()) {
                    check(where + "[]", item, it.next(), problems);
                }
            }
            return;
        }
        if (!result.getClass().isRecord() || !example.isObject()) {
            return;
        }
        Map<String, RecordComponent> byJsonName = new TreeMap<>();
        for (RecordComponent rc : result.getClass().getRecordComponents()) {
            JsonProperty p = rc.getAccessor().getAnnotation(JsonProperty.class);
            if (p == null) {
                p = fieldAnnotation(result.getClass(), rc.getName());
            }
            byJsonName.put(p != null && !p.value().isEmpty() ? p.value() : rc.getName(), rc);
        }
        example.fields().forEachRemaining(f -> {
            RecordComponent rc = byJsonName.get(f.getKey());
            if (rc == null) {
                if (!f.getKey().equals("success")) {
                    problems.add(where + ": field '" + f.getKey() + "' has no component in "
                            + result.getClass().getSimpleName());
                }
                return;
            }
            try {
                Object v = rc.getAccessor().invoke(result);
                if (v == null && !f.getValue().isNull()) {
                    problems.add(where + ": '" + f.getKey() + "' parsed as null");
                } else if (v != null && !f.getValue().isNull()) {
                    check(where + "." + f.getKey(), f.getValue(), v, problems);
                }
            } catch (ReflectiveOperationException e) {
                problems.add(where + ": " + e);
            }
        });
    }

    private static JsonProperty fieldAnnotation(Class<?> type, String name) {
        try {
            return type.getDeclaredField(name).getAnnotation(JsonProperty.class);
        } catch (NoSuchFieldException e) {
            return null;
        }
    }
}
