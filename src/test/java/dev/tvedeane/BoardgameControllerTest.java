package dev.tvedeane;

import com.fasterxml.jackson.core.JsonProcessingException;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

class BoardgameControllerTest {
    @Test
    void usesCacheCallingClientOnce() throws JsonProcessingException {
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