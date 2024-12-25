package dev.tvedeane;

import io.micronaut.http.HttpRequest;
import io.micronaut.http.HttpResponse;
import io.micronaut.http.client.BlockingHttpClient;
import io.micronaut.http.client.HttpClient;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentMatchers;

import java.nio.charset.StandardCharsets;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class BoardgamegeekClientTest {
    @Test
    void fetchesPlayerCounts() {
        var httpClient = mock(HttpClient.class);
        var blockingClient = mock(BlockingHttpClient.class);
        var client = new BoardgamegeekClient(httpClient);
        when(httpClient.toBlocking()).thenReturn(blockingClient);
        var response = HttpResponse.ok("""
                <?xml version="1.0" encoding="utf-8"?>
                  <items>
                    <item type="boardgame" id="1">
                      <poll-summary name="suggested_numplayers"  title="User Suggested Number of Players">
                        <result name="bestwith" value="Best with 2 players" />
                        <result name="recommmendedwith" value="Recommended with 1–3 players" />
                      </poll-summary>
                    </item>
                    <item type="boardgame" id="2">
                      <poll-summary name="suggested_numplayers"  title="User Suggested Number of Players">
                        <result name="bestwith" value="Best with 2-4 players" />
                        <result name="recommmendedwith" value="Recommended with 1–5 players" />
                      </poll-summary>
                    </item>
                  </items>
                """.getBytes(StandardCharsets.UTF_8));
        when(blockingClient.exchange(ArgumentMatchers.<HttpRequest<?>>any(), ArgumentMatchers.<Class<byte[]>>any()))
                .thenReturn(response);

        var result = client.fetchGame(List.of("1", "2"));

        assertThat(result).containsOnly(
                new PlayersCountDto("1", List.of(2), List.of(1, 2, 3)),
                new PlayersCountDto("2", List.of(2, 3, 4), List.of(1, 2, 3, 4, 5)));
    }
}