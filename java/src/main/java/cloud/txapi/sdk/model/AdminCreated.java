package cloud.txapi.sdk.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.Optional;

/**
 * Result of creating a panel admin. {@code password} is only returned once — store it now.
 *
 * @param success  whether txAdmin created the admin
 * @param password generated one-time password (only for a new password login)
 * @param note     reminder to store the password
 * @param refresh  {@code true} when the admin list should be reloaded
 * @param message  txAdmin's message, if any
 * @param error    error text when {@code success} is {@code false}
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record AdminCreated(boolean success, String password, String note, Boolean refresh, String message,
                           String error) {

    /** The generated password, if one was created. */
    public Optional<String> generatedPassword() {
        return Optional.ofNullable(password).filter(p -> !p.isEmpty());
    }

    @Override
    public String toString() {
        return "AdminCreated[success=" + success + ", password=" + (password == null ? null : "***")
                + ", note=" + note + ", refresh=" + refresh + ", message=" + message + ", error=" + error + "]";
    }
}
