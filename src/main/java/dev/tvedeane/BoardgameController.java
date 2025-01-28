package dev.tvedeane;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.google.common.collect.Lists;
import io.micronaut.http.MediaType;
import io.micronaut.http.annotation.Body;
import io.micronaut.http.annotation.Controller;
import io.micronaut.http.annotation.Get;
import io.micronaut.http.annotation.Post;
import io.reactivex.rxjava3.core.Flowable;
import io.reactivex.rxjava3.schedulers.Schedulers;
import org.reactivestreams.Publisher;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;

@Controller("boardgames")
public class BoardgameController {
    private final BoardgamegeekClient boardgamegeekClient;
    private final Cache<String, PlayersCountDto> cache = CacheBuilder.newBuilder()
        .maximumSize(10_000)
//            .weigher() // TODO use weight based on the game position
        .build();
    private final Flowable<PlayersCountDto> oneSecondDelayFlowable =
        Flowable.timer(1_000, TimeUnit.MILLISECONDS, Schedulers.single()).flatMap(tick -> Flowable.empty());
    private final Pattern IDS_REGEX = Pattern.compile("^\\d+(,\\d+)*$");

    public BoardgameController(BoardgamegeekClient boardgamegeekClient) {
        this.boardgamegeekClient = boardgamegeekClient;
    }

    @Post(value = "/stream", produces = MediaType.APPLICATION_JSON_STREAM)
    public Publisher<String> playersCountsStreamPost(@Body GamesIdsDto ids) {
        return streamGames(ids.ids());
    }

    @Get(value = "/stream/{ids}", produces = MediaType.APPLICATION_JSON_STREAM)
    public Publisher<String> playersCountsStream(String ids) {
        if (!IDS_REGEX.matcher(ids).matches()) {
            throw new InvalidIdsStringException();
        }

        var separatedIds = Arrays.stream(ids.split(",")).toList();
        return streamGames(separatedIds);
    }

    private Publisher<String> streamGames(List<String> separatedIds) {
        var cached = new ArrayList<PlayersCountDto>();
        var missingKeys = new ArrayList<String>();

        for (String key : separatedIds) {
            var value = cache.getIfPresent(key);
            if (value != null) {
                cached.add(value);
            } else {
                missingKeys.add(key);
            }
        }

        Flowable<PlayersCountDto> cachedFlow = Flowable.fromIterable(cached);
        if (missingKeys.isEmpty()) {
            return cachedFlow.map(BoardgameController::addNewline);
        }

        List<List<String>> partitions = Lists.partition(missingKeys, BoardgamegeekClient.MAX_READING_THINGS);
        Flowable<PlayersCountDto> uncachedFlow = Flowable.fromIterable(partitions)
            .concatMap(partition -> {
                List<PlayersCountDto> fetchedItems = boardgamegeekClient.fetchGame(partition);

                for (var entry : fetchedItems) {
                    cache.put(entry.id(), entry);
                }

                return Flowable.fromIterable(fetchedItems).concatWith(oneSecondDelayFlowable);
            }).subscribeOn(Schedulers.single());

        return Flowable.merge(cachedFlow, uncachedFlow).map(BoardgameController::addNewline);
    }

    private static String addNewline(PlayersCountDto item) {
        return item.toJson() + "\n";
    }
}
