package cloud.txapi.sdk.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * A name with a count (top victims, killers, weapons, causes).
 *
 * @param name  the name
 * @param count how often
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record NameCount(String name, long count) {
}
