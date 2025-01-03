package dev.tvedeane;

import io.reactivex.rxjava3.core.Flowable;
import org.junit.jupiter.api.Test;
import org.reactivestreams.Publisher;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

class BoardgameControllerTest {
    @Test
    void streamUsesCacheCallingClientOnce() {
        var mockClient = mock(BoardgamegeekClient.class);
        var controller = new BoardgameController(mockClient);
        var e1 = new PlayersCountDto("4", List.of(2), List.of(2));
        var e2 = new PlayersCountDto("10", List.of(2), List.of(2));
        when(mockClient.fetchGame(List.of("4", "10"))).thenReturn(List.of(e1, e2));

        var result1 = getBlockingFrom(controller.playersCountsStream("4,10"));
        var result2 = getBlockingFrom(controller.playersCountsStream("4,10"));

        assertThat(result1).containsOnly(e1, e2);
        assertThat(result2).containsOnly(e1, e2);
        verify(mockClient, times(1)).fetchGame(List.of("4", "10"));
    }

    private static List<PlayersCountDto> getBlockingFrom(Publisher<PlayersCountDto> publisher) {
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
            "1", "2", "3", "4", "5", "6", "7", "8", "9", "10",
            "11", "12", "13", "14", "15", "16", "17", "18", "19", "20"));
        verify(mockClient).fetchGame(List.of("21", "22"));
    }

    @Test
    void usesCacheCallingClientOnce() {
        var mock = mock(BoardgamegeekClient.class);
        var controller = new BoardgameController(mock);
        var e1 = new PlayersCountDto("4", List.of(2), List.of(2));
        var e2 = new PlayersCountDto("10", List.of(2), List.of(2));
        when(mock.fetchGame(List.of("4", "10"))).thenReturn(List.of(e1, e2));

        var result1 = controller.playersCounts("4,10");
        var result2 = controller.playersCounts("4,10");

        assertThat(result1).containsOnly(e1, e2);
        assertThat(result2).containsOnly(e1, e2);
        verify(mock, times(1)).fetchGame(List.of("4", "10"));
    }
}