package cloud.txapi.sdk.internal;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;

import java.io.IOException;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;

/**
 * The SDK's single {@link ObjectMapper}.
 *
 * <ul>
 *   <li>Unknown properties are ignored — the API adds fields in minor versions.</li>
 *   <li>{@code null} values are never written ({@code NON_NULL}).</li>
 *   <li>Timestamps ({@link Instant}, {@link OffsetDateTime}) are parsed leniently: ISO-8601 with
 *       or without offset, or epoch seconds/milliseconds. A value that cannot be parsed becomes
 *       {@code null} instead of failing the whole response.</li>
 * </ul>
 * No {@code jackson-datatype-jsr310} needed.
 */
public final class Json {
    public static final ObjectMapper MAPPER = create();

    private Json() {
    }

    private static ObjectMapper create() {
        SimpleModule time = new SimpleModule("txapi-time");
        time.addDeserializer(Instant.class, new InstantDeserializer());
        time.addDeserializer(OffsetDateTime.class, new OffsetDateTimeDeserializer());
        time.addSerializer(Instant.class, ToStringSerializer.instance);
        time.addSerializer(OffsetDateTime.class, ToStringSerializer.instance);
        return JsonMapper.builder()
                .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
                .disable(DeserializationFeature.FAIL_ON_NULL_FOR_PRIMITIVES)
                .enable(DeserializationFeature.ACCEPT_EMPTY_STRING_AS_NULL_OBJECT)
                .disable(SerializationFeature.FAIL_ON_EMPTY_BEANS)
                .serializationInclusion(JsonInclude.Include.NON_NULL)
                .addModule(time)
                .build();
    }

    /** Parses text as JSON, or returns {@code null} when it is not JSON. */
    public static JsonNode tryParse(String text) {
        if (text == null || text.isBlank()) {
            return null;
        }
        try {
            return MAPPER.readTree(text);
        } catch (IOException e) {
            return null;
        }
    }

    /** Lenient parse of a timestamp string; {@code null} when it is not one. */
    public static Instant parseInstant(String s) {
        if (s == null) {
            return null;
        }
        String t = s.trim();
        if (t.isEmpty()) {
            return null;
        }
        if (t.chars().allMatch(Character::isDigit) && t.length() <= 16) {
            return epoch(Long.parseLong(t));
        }
        try {
            return Instant.parse(t);
        } catch (RuntimeException ignored) {
            // try the next format
        }
        try {
            return OffsetDateTime.parse(t).toInstant();
        } catch (RuntimeException ignored) {
            // try the next format
        }
        try {
            return ZonedDateTime.parse(t).toInstant();
        } catch (RuntimeException ignored) {
            // try the next format
        }
        try {
            return LocalDateTime.parse(t).toInstant(ZoneOffset.UTC);
        } catch (RuntimeException ignored) {
            return null;
        }
    }

    /** Epoch value: seconds below 10^11, otherwise milliseconds. */
    public static Instant epoch(long value) {
        return Math.abs(value) < 100_000_000_000L ? Instant.ofEpochSecond(value) : Instant.ofEpochMilli(value);
    }

    static final class InstantDeserializer extends StdDeserializer<Instant> {
        private static final long serialVersionUID = 1L;

        InstantDeserializer() {
            super(Instant.class);
        }

        @Override
        public Instant deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {
            JsonToken t = p.currentToken();
            if (t == JsonToken.VALUE_NUMBER_INT) {
                return epoch(p.getLongValue());
            }
            if (t == JsonToken.VALUE_STRING) {
                return parseInstant(p.getText());
            }
            p.skipChildren();
            return null;
        }
    }

    static final class OffsetDateTimeDeserializer extends StdDeserializer<OffsetDateTime> {
        private static final long serialVersionUID = 1L;

        OffsetDateTimeDeserializer() {
            super(OffsetDateTime.class);
        }

        @Override
        public OffsetDateTime deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {
            JsonToken t = p.currentToken();
            if (t == JsonToken.VALUE_STRING) {
                String s = p.getText().trim();
                try {
                    return OffsetDateTime.parse(s);
                } catch (RuntimeException e) {
                    Instant i = parseInstant(s);
                    return i == null ? null : i.atOffset(ZoneOffset.UTC);
                }
            }
            if (t == JsonToken.VALUE_NUMBER_INT) {
                return epoch(p.getLongValue()).atOffset(ZoneOffset.UTC);
            }
            p.skipChildren();
            return null;
        }
    }
}
