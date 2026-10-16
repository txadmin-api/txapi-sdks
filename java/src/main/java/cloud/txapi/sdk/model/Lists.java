package cloud.txapi.sdk.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Null-tolerant immutable copies for record components. */
final class Lists {
    private Lists() {
    }

    static <T> List<T> of(List<T> list) {
        return list == null ? List.of() : Collections.unmodifiableList(new ArrayList<>(list));
    }

    static <K, V> Map<K, V> of(Map<K, V> map) {
        return map == null ? Map.of() : Collections.unmodifiableMap(new LinkedHashMap<>(map));
    }
}
