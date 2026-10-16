package cloud.txapi.sdk.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.List;

/**
 * txAdmin's admin log, live (newest last).
 *
 * @param lines the requested last lines
 * @param count lines returned
 * @param total lines in the log
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record ActionLog(List<Line> lines, int count, int total) {
    public ActionLog {
        lines = Lists.of(lines);
    }

    /**
     * @param time    time as logged by txAdmin ({@code HH:mm:ss}, host time)
     * @param admin   admin name
     * @param message log text
     * @param kind    {@code ban}, {@code warn}, {@code kick}, {@code revoke}, {@code command},
     *                {@code restart}, {@code whitelist}, {@code auth}, {@code other}
     */
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Line(String time, String admin, String message, String kind) {
    }
}
