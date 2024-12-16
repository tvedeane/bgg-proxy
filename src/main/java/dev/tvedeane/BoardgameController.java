package dev.tvedeane;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.google.common.collect.Lists;
import io.micronaut.http.MediaType;
import io.micronaut.http.annotation.Controller;
import io.micronaut.http.annotation.Get;
import io.micronaut.http.annotation.Produces;
import io.micronaut.scheduling.TaskExecutors;
import io.micronaut.scheduling.annotation.ExecuteOn;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@Controller("boardgames")
public class BoardgameController {
    private final BoardgamegeekClient boardgamegeekClient;
    private final Cache<String, PlayersCountDto> cache = CacheBuilder.newBuilder()
            .maximumSize(10_000)
//            .weigher() // TODO use weight based on the game position
            .build();

    public BoardgameController(BoardgamegeekClient boardgamegeekClient) {
        this.boardgamegeekClient = boardgamegeekClient;
    }

    @Get(value = "/{ids}")
    @Produces(MediaType.APPLICATION_JSON)
    @ExecuteOn(TaskExecutors.BLOCKING)
    public List<PlayersCountDto> playersCounts(String ids) throws JsonProcessingException {
        // TODO use regex to check if `ids` has only digits and commas

        var separatedIds = Arrays.stream(ids.split(",")).toList();
        var results = new ArrayList<PlayersCountDto>();
        var missingKeys = new ArrayList<String>();

        synchronized (cache) { // TODO improve the concurrency
            for (String key : separatedIds) {
                var value = cache.getIfPresent(key);
                if (value != null) {
                    results.add(value);
                } else {
                    missingKeys.add(key);
                }
            }

            if (!missingKeys.isEmpty()) {
                var partitions = Lists.partition(missingKeys, BoardgamegeekClient.MAX_READING_THINGS);
                partitions.forEach(partition -> {
                    List<PlayersCountDto> fetchedItems = boardgamegeekClient.fetchGame(partition);

                    for (var entry : fetchedItems) {
                        cache.put(entry.id(), entry);
                        results.add(entry);
                    }

                    try {
                        // to avoid rate limits
                        Thread.sleep(1_000); // TODO use better construct
                    } catch (InterruptedException e) {
                        throw new RuntimeException(e);
                    }
                });
            }
        }
        return results;
    }
}
