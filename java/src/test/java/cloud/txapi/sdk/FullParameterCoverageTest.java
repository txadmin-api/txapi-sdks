package cloud.txapi.sdk;

import cloud.txapi.sdk.internal.Json;
import cloud.txapi.sdk.params.*;
import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeSet;
import java.util.function.Consumer;
import java.util.regex.Pattern;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Every endpoint called with EVERY parameter and body field the API accepts. Each query
 * parameter and body field of {@code spec/endpoints.json} must arrive under exactly its wire
 * name — nothing missing, nothing extra.
 */
class FullParameterCoverageTest {
    private static MockApi mock;
    private static TxApi client;
    private static final Map<String, JsonNode> SPEC = new LinkedHashMap<>();
    private static final Map<String, Pattern> REGEX = new LinkedHashMap<>();
    private static final PageCursor PAGE = PageCursor.of(5, "lic");
    private static final List<String> PERMS = List.of("players.read", "players.ban");

    @BeforeAll
    static void setUp() throws Exception {
        JsonNode root = Json.MAPPER.readTree(Files.readAllBytes(Path.of("..", "spec", "endpoints.json")));
        for (JsonNode e : root.path("endpoints")) {
            String path = e.path("path").asText();
            SPEC.put(e.path("id").asText(), e);
            REGEX.put(e.path("id").asText(), Pattern.compile(
                    "^" + Pattern.quote(path).replaceAll("\\{[^}]+}", "\\\\E[^/]+\\\\Q") + "$"));
        }
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

    private static Map<String, Consumer<TxApi>> calls() {
        Map<String, Consumer<TxApi>> c = new LinkedHashMap<>();
        c.put("account.me", a -> a.account().me());
        c.put("account.servers", a -> a.account().servers());
        c.put("players.search", a -> a.players().search(PlayerSearchParams.create().query("john")
                .sortBy(PlayerSort.JOINED).order(SortOrder.ASC).filters(PlayerFilter.ONLINE, PlayerFilter.HAS_NOTE).page(PAGE)));
        c.put("players.online", a -> a.players().online(PAGE));
        c.put("players.lookup", a -> a.players().lookup("discord:1", PAGE));
        c.put("players.info", a -> a.players().info("license:abc"));
        c.put("players.stats", a -> a.players().stats());
        c.put("players.warn", a -> a.players().warn("license:abc", "spam"));
        c.put("players.ban", a -> a.players().ban(BanRequest.of("license:abc", "cheating").withDurationHours(24).withDiscordBan(true)));
        c.put("players.kick", a -> a.players().kick("license:abc", "afk"));
        c.put("players.dm", a -> a.players().dm("license:abc", "hi"));
        c.put("players.notes", a -> a.players().setNote("license:abc", "watch"));
        c.put("players.whitelist", a -> a.players().setWhitelisted("license:abc", false));
        c.put("history.bans", a -> a.history().bans(HistoryPageParams.create().order(SortOrder.ASC).page(PAGE)));
        c.put("history.warns", a -> a.history().warns(HistoryPageParams.create().order(SortOrder.ASC).page(PAGE)));
        c.put("history.search", a -> a.history().search(HistorySearchParams.create().type(HistoryType.BAN).admin("x")
                .searchType(HistorySearchType.REASON).search("y").order(SortOrder.ASC).page(PAGE)));
        c.put("history.ban-templates", a -> a.history().banTemplates());
        c.put("history.action", a -> a.history().action("BKL5-3SH4"));
        c.put("history.stats", a -> a.history().stats());
        c.put("history.revoke", a -> a.history().revoke("BKL5-3SH4", RevokeRequest.reason("oops").withDiscordUnban(true)));
        c.put("server.status", a -> a.server().status());
        c.put("server.diagnostics", a -> a.server().diagnostics());
        c.put("server.drops", a -> a.server().drops());
        c.put("server.performance", a -> a.server().performance("svMain"));
        c.put("server.restart", a -> a.server().restart());
        c.put("server.start", a -> a.server().start());
        c.put("server.stop", a -> a.server().stop());
        c.put("server.schedule-restart", a -> a.server().scheduleRestart("+30"));
        c.put("server.skip-restart", a -> a.server().skipRestart(false));
        c.put("server.kickall", a -> a.server().kickAll("maintenance"));
        c.put("server.announcement", a -> a.server().announce("Restart in 5"));
        c.put("server.resource.start", a -> a.server().resources().start("chat"));
        c.put("server.resource.stop", a -> a.server().resources().stop("chat"));
        c.put("server.resource.restart", a -> a.server().resources().restart("chat"));
        c.put("server.resource.ensure", a -> a.server().resources().ensure("chat"));
        c.put("server.resource.refresh", a -> a.server().resources().refresh());
        c.put("console.buffer", a -> a.console().buffer());
        c.put("console.command", a -> a.console().command("status"));
        c.put("serverlog.buffer", a -> a.serverLog().buffer());
        c.put("serverlog.history", a -> a.serverLog().history(LogDirection.NEWER, 1_700_000_000_000L));
        c.put("events.list", a -> a.events().list(EventListQuery.create().type("death", "ban").player("p").search("s")
                .since("7d").until("1h").cursor("1_2").limit(10)));
        c.put("events.deaths", a -> a.events().deaths(DeathQuery.create().player("p").victim("v").killer("k").cause("c")
                .pvp(false).since("7d").until("1h").cursor("1_2").limit(10)));
        c.put("events.kills", a -> a.events().kills(KillQuery.create().player("p").killer("k").victim("v").cause("c")
                .since("7d").until("1h").cursor("1_2").limit(10)));
        c.put("events.leaderboard", a -> a.events().leaderboard(LeaderboardQuery.create().since("7d").sort(LeaderboardSort.KD).limit(10)));
        c.put("events.player", a -> a.events().player("Jane", "7d"));
        c.put("events.summary", a -> a.events().summary("7d"));
        c.put("events.chat", a -> a.events().chat(ChatQuery.create().player("p").search("s")
                .since("7d").until("1h").cursor("1_2").limit(10)));
        c.put("events.commands", a -> a.events().commands(CommandQuery.create().player("p").search("s")
                .since("7d").until("1h").cursor("1_2").limit(10)));
        c.put("events.admin-actions", a -> a.events().adminActions(AdminActionQuery.create().admin("a").kind("ban").search("s")
                .since("7d").until("1h").cursor("1_2").limit(10)));
        c.put("events.actions", a -> a.events().actions(ActionQuery.create().type("warn").player("p").admin("a").revoked(false)
                .since("7d").until("1h").cursor("1_2").limit(10)));
        c.put("events.sessions", a -> a.events().sessions(SessionQuery.create().player("p")
                .since("7d").until("1h").cursor("1_2").limit(10)));
        c.put("events.playtime", a -> a.events().playtime(PlaytimeQuery.create().since("7d").player("p").limit(10)));
        c.put("events.activity", a -> a.events().activity(ActivityQuery.create().since("7d").bucket(ActivityBucket.DAY)
                .timeZone(ZoneId.of("Europe/Vienna"))));
        c.put("events.action-log", a -> a.events().actionLog(10));
        c.put("whitelist.requests", a -> a.whitelist().requests(2));
        c.put("whitelist.approve", a -> a.whitelist().approve("R1234"));
        c.put("whitelist.deny", a -> a.whitelist().deny("R1234"));
        c.put("whitelist.deny-all", a -> a.whitelist().denyAll(1_700_000_000L));
        c.put("whitelist.approvals", a -> a.whitelist().approvals().list());
        c.put("whitelist.approvals.add", a -> a.whitelist().approvals().add("discord:1"));
        c.put("whitelist.approvals.remove", a -> a.whitelist().approvals().remove("discord:1"));
        c.put("admins.list", a -> a.admins().list());
        c.put("admins.get", a -> a.admins().get("moderator 1"));
        c.put("admins.permissions", a -> a.admins().permissions());
        c.put("admins.add", a -> a.admins().add(AdminCreateRequest.of("mod", PERMS).withCitizenfxId("fivem:1").withDiscordId("discord:2")));
        c.put("admins.edit", a -> a.admins().edit("mod", AdminUpdateRequest.create().withPermissions(PERMS)
                .withCitizenfxId("fivem:1").withDiscordId("discord:2")));
        c.put("admins.delete", a -> a.admins().delete("mod"));
        return c;
    }

    @Test
    void callTableCoversEveryEndpoint() {
        assertThat(new TreeSet<>(calls().keySet())).isEqualTo(new TreeSet<>(SPEC.keySet()));
    }

    @Test
    void everyParameterAndBodyFieldArrivesUnderItsWireName() {
        for (Map.Entry<String, Consumer<TxApi>> call : calls().entrySet()) {
            String id = call.getKey();
            JsonNode ep = SPEC.get(id);
            int before = mock.requests.size();
            call.getValue().accept(client);
            assertThat(mock.requests.size() - before).as(id).isEqualTo(1);
            MockApi.Req req = mock.last();
            assertThat(req.method()).as(id).isEqualTo(ep.path("method").asText());
            assertThat(REGEX.get(id).matcher(req.rawPath()).matches()).as(id + " " + req.rawPath()).isTrue();

            TreeSet<String> query = new TreeSet<>(req.query().keySet());
            query.remove("serverId");
            TreeSet<String> wantQuery = new TreeSet<>();
            for (JsonNode p : ep.path("params")) {
                if (p.path("in").asText().equals("query")) {
                    wantQuery.add(p.path("name").asText());
                }
            }
            assertThat(query).as(id + " query").isEqualTo(wantQuery);

            TreeSet<String> body = new TreeSet<>();
            JsonNode json = req.body() == null || req.body().isEmpty() ? null : req.json();
            if (json != null) {
                for (Iterator<String> it = json.fieldNames(); it.hasNext(); ) {
                    body.add(it.next());
                }
            }
            TreeSet<String> wantBody = new TreeSet<>();
            ep.path("body").forEach(b -> wantBody.add(b.path("name").asText()));
            assertThat(body).as(id + " body").isEqualTo(wantBody);
        }
    }
}
