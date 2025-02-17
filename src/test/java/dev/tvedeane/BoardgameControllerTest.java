package dev.tvedeane;

import io.reactivex.rxjava3.core.Flowable;
import org.junit.jupiter.api.Test;
import org.reactivestreams.Publisher;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

class BoardgameControllerTest {
    @Test
    void streamUsesCacheCallingClientOnce() {
        var mockClient = mock(BoardgamegeekClient.class);
        var controller = new BoardgameController(mockClient);
        var e1 = new PlayersCountCacheEntry("4", List.of(2), List.of(2, 3), System.currentTimeMillis(), 30);
        var e2 = new PlayersCountCacheEntry("10", List.of(2), List.of(2, 3, 4), System.currentTimeMillis(), 30);
        when(mockClient.fetchGame(List.of(4L, 10L))).thenReturn(List.of(e1, e2));

        var result1 = getBlockingFrom(controller.playersCountsStream("4,10"));
        var result2 = getBlockingFrom(controller.playersCountsStream("4,10"));

        String[] results = {
            "{\"id\":\"4\",\"bestWith\":[2],\"recommendedWith\":[2,3]}\n",
            "{\"id\":\"10\",\"bestWith\":[2],\"recommendedWith\":[2,3,4]}\n"
        };
        assertThat(result1).containsOnly(results);
        assertThat(result2).containsOnly(results);
        verify(mockClient, times(1)).fetchGame(List.of(4L, 10L));
    }

    private static List<String> getBlockingFrom(Publisher<String> publisher) {
        return Flowable.fromPublisher(publisher)
            .toList()
            .blockingGet();
    }

    @Test
    void partitionsCallsToTheClient() {
        var mockClient = mock(BoardgamegeekClient.class);
        var controller = new BoardgameController(mockClient);

        getBlockingFrom(controller.playersCountsStream("1,2,3,4,5,6,7,8,9,10,11,12,13,14,15,16,17,18,19,20,21,22"));

        verify(mockClient).fetchGame(List.of(
            1L, 2L, 3L, 4L, 5L, 6L, 7L, 8L, 9L, 10L,
            11L, 12L, 13L, 14L, 15L, 16L, 17L, 18L, 19L, 20L));
        verify(mockClient).fetchGame(List.of(21L, 22L));
    }

    @Test
    void playerCountsStreamThrowsExceptionOnWrongInput() {
        var mockClient = mock(BoardgamegeekClient.class);
        var controller = new BoardgameController(mockClient);

        assertThatThrownBy(() -> getBlockingFrom(controller.playersCountsStream("1,a")))
            .isInstanceOf(InvalidIdsStringException.class);
    }

    @Test
    void callsClientWhenEntryExpired() {
        var mockClient = mock(BoardgamegeekClient.class);
        var controller = new BoardgameController(mockClient);
        var e1 = new PlayersCountCacheEntry("9", List.of(2), List.of(2, 3), 1, 30);
        when(mockClient.fetchGame(List.of(9L))).thenReturn(List.of(e1));

        getBlockingFrom(controller.playersCountsStream("9"));
        getBlockingFrom(controller.playersCountsStream("9"));

        verify(mockClient, times(2)).fetchGame(List.of(9L));
    }
}