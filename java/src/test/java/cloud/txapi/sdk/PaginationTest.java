package cloud.txapi.sdk;

import cloud.txapi.sdk.model.HistoryEntry;
import cloud.txapi.sdk.model.Player;
import cloud.txapi.sdk.model.ServerEvent;
import cloud.txapi.sdk.params.EventListQuery;
import cloud.txapi.sdk.params.HistoryPageParams;
import cloud.txapi.sdk.params.PageCursor;
import cloud.txapi.sdk.params.PlayerSearchParams;
import cloud.txapi.sdk.params.SortOrder;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class PaginationTest {
    private MockApi mock;
    private TxApi client;

    @BeforeEach
    void setUp() {
        mock = new MockApi();
        client = mock.client(b -> b.serverId(1L));
    }

    @AfterEach
    void tearDown() {
        client.close();
        mock.close();
    }

    private static String player(String license, long playTime, long joined, long last) {
        return "{\"license\":\"" + license + "\",\"displayName\":\"" + license + "\",\"playTime\":" + playTime
                + ",\"tsJoined\":" + joined + ",\"tsLastConnection\":" + last + "}";
    }

    @Test
    void playersSearchAllFollowsTheCursor() {
        mock.enqueue(
                MockApi.Resp.ok("{\"players\":[" + player("license:a", 100, 11, 21) + "," + player("license:b", 50, 12, 22)
                        + "],\"hasReachedEnd\":false}"),
                MockApi.Resp.ok("{\"players\":[" + player("license:c", 10, 13, 23) + "],\"hasReachedEnd\":true}"));

        List<Player> all = client.players().searchAll(PlayerSearchParams.create().query("bob").order(SortOrder.DESC)).toList();

        assertThat(all).extracting(Player::license).containsExactly("license:a", "license:b", "license:c");
        assertThat(mock.requests).hasSize(2);
        assertThat(mock.requests.get(0).query()).containsEntry("query", "bob").doesNotContainKeys("offset", "cursor");
        assertThat(mock.requests.get(1).query()).containsEntry("query", "bob").containsEntry("order", "desc")
                .containsEntry("offset", "50").containsEntry("cursor", "license:b").containsEntry("serverId", "1");
    }

    @ParameterizedTest
    @CsvSource({"playTime,50", "tsJoined,12", "sessions,12", "tsLastConnection,22", "lastConnection,22"})
    void playerCursorUsesTheSortKey(String sortBy, String expectedOffset) {
        mock.enqueue(
                MockApi.Resp.ok("{\"players\":[" + player("license:a", 100, 11, 21) + "," + player("license:b", 50, 12, 22)
                        + "],\"hasReachedEnd\":false}"),
                MockApi.Resp.ok("{\"players\":[],\"hasReachedEnd\":true}"));
        client.players().searchAll(PlayerSearchParams.create().sortBy(sortBy)).toList();
        assertThat(mock.requests.get(1).query()).containsEntry("offset", expectedOffset).containsEntry("cursor", "license:b");
    }

    @Test
    void onlineAndLookupSortByPlayTime() {
        mock.handler(r -> r.query().containsKey("cursor")
                ? MockApi.Resp.ok("{\"players\":[],\"hasReachedEnd\":true}")
                : MockApi.Resp.ok("{\"players\":[" + player("license:z", 77, 1, 2) + "],\"hasReachedEnd\":false}"));
        assertThat(client.players().onlineAll().toList()).hasSize(1);
        assertThat(mock.last().rawPath()).isEqualTo("/api/v1/players/online");
        assertThat(mock.last().query()).containsEntry("offset", "77").containsEntry("cursor", "license:z");

        assertThat(client.players().lookupAll("discord:1").toList()).hasSize(1);
        assertThat(mock.last().query()).containsEntry("identifier", "discord:1").containsEntry("offset", "77");
    }

    @Test
    void streamIsLazy() {
        mock.handler(r -> MockApi.Resp.ok("{\"players\":[" + player("license:" + r.query().getOrDefault("offset", "x"), 5, 1, 1)
                + "," + player("license:y", 4, 1, 1) + "],\"hasReachedEnd\":false}"));
        List<Player> first = client.players().searchAll().stream().limit(2).toList();
        assertThat(first).hasSize(2);
        assertThat(mock.requests).hasSize(1);
    }

    @Test
    void firstPageHonoursACallerCursor() {
        mock.enqueue(MockApi.Resp.ok("{\"players\":[],\"hasReachedEnd\":true}"));
        client.players().searchAll(PlayerSearchParams.create().page(PageCursor.of(40, "license:q"))).toList();
        assertThat(mock.last().query()).containsEntry("offset", "40").containsEntry("cursor", "license:q");
    }

    @Test
    void stopsOnEmptyPageOrRepeatedToken() {
        // empty page although hasReachedEnd is false
        mock.enqueue(MockApi.Resp.ok("{\"players\":[],\"hasReachedEnd\":false}"));
        assertThat(client.players().searchAll().toList()).isEmpty();
        assertThat(mock.requests).hasSize(1);

        // the server keeps returning the same last row → stop instead of looping forever
        mock.requests.clear();
        mock.handler(r -> MockApi.Resp.ok("{\"players\":[" + player("license:same", 9, 1, 1) + "],\"hasReachedEnd\":false}"));
        assertThat(client.players().searchAll().toList()).hasSize(2);
        assertThat(mock.requests).hasSize(2);
    }

    @Test
    void historyFollowsTimestampAndActionId() {
        mock.enqueue(
                MockApi.Resp.ok("{\"history\":[{\"id\":\"A1\",\"type\":\"ban\",\"timestamp\":2000},"
                        + "{\"id\":\"A2\",\"type\":\"ban\",\"timestamp\":1000}],\"hasReachedEnd\":false}"),
                MockApi.Resp.ok("{\"history\":[{\"id\":\"A3\",\"type\":\"ban\",\"timestamp\":500}],\"hasReachedEnd\":true}"));
        List<HistoryEntry> bans = client.history().bansAll(HistoryPageParams.create().order(SortOrder.ASC)).toList();
        assertThat(bans).extracting(HistoryEntry::id).containsExactly("A1", "A2", "A3");
        assertThat(mock.requests.get(0).rawPath()).isEqualTo("/api/v1/history/bans");
        assertThat(mock.requests.get(1).query()).containsEntry("offset", "1000").containsEntry("cursor", "A2")
                .containsEntry("order", "asc");
    }

    @Test
    void eventsFollowNextCursor() {
        mock.enqueue(
                MockApi.Resp.ok("{\"success\":true,\"events\":[{\"id\":3,\"type\":\"death\"},{\"id\":2,\"type\":\"death\"}],"
                        + "\"nextCursor\":\"1760000000000_2\"}"),
                MockApi.Resp.ok("{\"success\":true,\"events\":[{\"id\":1,\"type\":\"join\"}]}"));
        List<ServerEvent> events = client.events().listAll(EventListQuery.create().type("death").limit(2)).toList();
        assertThat(events).extracting(ServerEvent::id).containsExactly(3L, 2L, 1L);
        assertThat(mock.requests.get(0).query()).containsEntry("type", "death").containsEntry("limit", "2")
                .doesNotContainKey("cursor");
        assertThat(mock.requests.get(1).query()).containsEntry("cursor", "1760000000000_2").containsEntry("type", "death");
    }

    @Test
    void pagesCanBeIteratedDirectly() {
        mock.enqueue(
                MockApi.Resp.ok("{\"events\":[{\"id\":2}],\"nextCursor\":\"c1\"}"),
                MockApi.Resp.ok("{\"events\":[{\"id\":1}]}"));
        int pages = 0;
        for (List<ServerEvent> page : client.events().deathsAll(null).pages()) {
            assertThat(page).hasSize(1);
            pages++;
        }
        assertThat(pages).isEqualTo(2);
    }

    @Test
    void asyncPaginationCollectsAllPages() {
        mock.enqueue(
                MockApi.Resp.ok("{\"players\":[" + player("license:a", 3, 1, 1) + "],\"hasReachedEnd\":false}"),
                MockApi.Resp.ok("{\"players\":[" + player("license:b", 2, 1, 1) + "],\"hasReachedEnd\":true}"));
        List<Player> all = client.async().players().searchAll().toList().join();
        assertThat(all).extracting(Player::license).containsExactly("license:a", "license:b");
        assertThat(mock.requests.get(1).query()).containsAllEntriesOf(Map.of("offset", "3", "cursor", "license:a"));
    }

    @Test
    void asyncToListWithMaxStopsEarly() {
        mock.handler(r -> MockApi.Resp.ok("{\"events\":[{\"id\":1},{\"id\":2}],\"nextCursor\":\"n" + mockSize() + "\"}"));
        List<ServerEvent> some = client.async().events().listAll(null).toList(3).join();
        assertThat(some).hasSize(3);
        assertThat(mock.requests).hasSize(2);
    }

    private int mockSize() {
        return mock.requests.size();
    }
}
