package examples;

import cloud.txapi.sdk.TxApi;
import cloud.txapi.sdk.errors.NotFoundException;
import cloud.txapi.sdk.errors.PermissionDeniedException;
import cloud.txapi.sdk.errors.RateLimitException;
import cloud.txapi.sdk.errors.TxApiException;
import cloud.txapi.sdk.model.AccountInfo;
import cloud.txapi.sdk.model.ActionResult;
import cloud.txapi.sdk.model.HistoryEntry;
import cloud.txapi.sdk.model.Player;
import cloud.txapi.sdk.model.PlayerPage;
import cloud.txapi.sdk.params.BanRequest;
import cloud.txapi.sdk.params.PlayerSearchParams;

/**
 * Quickstart: who am I, find a player by name, ban them for a week, list the latest bans.
 *
 * <pre>
 * TXAPI_KEY=txp_… [TXAPI_SERVER_ID=12] java … examples.QuickstartBan "Player name"
 * </pre>
 */
public final class QuickstartBan {
    private QuickstartBan() {
    }

    public static void main(String[] args) {
        String name = args.length > 0 ? args[0] : "john";

        // Reads TXAPI_KEY, TXAPI_BASE_URL and TXAPI_SERVER_ID from the environment.
        try (TxApi client = TxApi.fromEnv()) {
            AccountInfo me = client.account().me();
            System.out.printf("Plan %s, %s requests left this month%n", me.plan().code(),
                    me.quota().unlimited() ? "unlimited" : me.quota().remaining());

            PlayerPage page = client.players().search(PlayerSearchParams.create().query(name));
            if (page.players().isEmpty()) {
                System.out.println("No player called " + name);
                return;
            }
            Player player = page.players().get(0);
            System.out.println("Found " + player.displayName() + " (" + player.license() + ")");

            ActionResult result = client.players().ban(
                    BanRequest.of("license:" + player.license(), "Cheating (SDK quickstart)")
                            .withDurationHours(168));
            System.out.println("Ban: " + result.message());

            // Iterates across pages and stops fetching after ten rows.
            client.history().bansAll().stream()
                    .limit(10)
                    .map(HistoryEntry::playerName)
                    .forEach(n -> System.out.println("  banned: " + n));

        } catch (NotFoundException e) {
            System.err.println("Not found: " + e.getMessage());
        } catch (PermissionDeniedException e) {
            System.err.println("Not allowed (plan or key scope): " + e.getMessage());
        } catch (RateLimitException e) {
            System.err.println(e.isQuotaExhausted()
                    ? "Monthly quota exhausted"
                    : "Too many requests, retry after " + e.retryAfter().orElse(null));
        } catch (TxApiException e) {
            System.err.println("txApi error: " + e);
        }
    }
}
