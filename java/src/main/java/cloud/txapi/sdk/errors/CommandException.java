package cloud.txapi.sdk.errors;

/**
 * The live console answered a command with an {@code error} frame (empty or too long command,
 * too many commands, monthly quota used up, txAdmin unreachable …).
 */
public class CommandException extends TxApiException {
    private static final long serialVersionUID = 1L;

    public CommandException(String message) {
        super(message);
    }
}
