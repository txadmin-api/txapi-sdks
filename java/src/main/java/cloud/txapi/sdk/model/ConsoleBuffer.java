package cloud.txapi.sdk.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.List;

/**
 * The last console lines (one element per line, with FXServer's ANSI colour codes).
 *
 * @param lines the lines, oldest first
 * @param count number of lines
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record ConsoleBuffer(List<String> lines, int count) {
    public ConsoleBuffer {
        lines = Lists.of(lines);
    }
}
