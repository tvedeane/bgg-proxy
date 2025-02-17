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

import java.time.Duration;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;

@Controller("boardgames")
public class BoardgameController {
    private final BoardgamegeekClient boardgamegeekClient;
    private final Cache<Long, PlayersCountCacheEntry> cache = CacheBuilder.newBuilder()
        .maximumSize(10_000)
        .expireAfterWrite(30, TimeUnit.DAYS)
        .build();
    private final Flowable<PlayersCountCacheEntry> oneSecondDelayFlowable =
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

        var separatedIds = Arrays.stream(ids.split(",")).map(Long::parseLong).toList();
        return streamGames(separatedIds);
    }

    private Publisher<String> streamGames(List<Long> separatedIds) {
        var cached = new ArrayList<PlayersCountCacheEntry>();
        var missingKeys = new ArrayList<Long>();

        for (Long key : separatedIds) {
            var value = cache.getIfPresent(key);
            if (value != null) {
                var isExpired =
                    System.currentTimeMillis() - value.creationTime() >
                    Duration.ofDays(value.expirationInDays()).toMillis();
                if (isExpired) {
                    cache.invalidate(key);
                    missingKeys.add(key);
                } else {
                    cached.add(value);
                }
            } else {
                missingKeys.add(key);
            }
        }

        Flowable<PlayersCountCacheEntry> cachedFlow = Flowable.fromIterable(cached);
        if (missingKeys.isEmpty()) {
            return cachedFlow.map(BoardgameController::addNewline);
        }

        List<List<Long>> partitions = Lists.partition(missingKeys, BoardgamegeekClient.MAX_READING_THINGS);
        Flowable<PlayersCountCacheEntry> uncachedFlow = Flowable.fromIterable(partitions)
            .concatMap(partition -> {
                List<PlayersCountCacheEntry> fetchedItems = boardgamegeekClient.fetchGame(partition);

                for (var entry : fetchedItems) {
                    cache.put(entry.id(), entry);
                }

                return Flowable.fromIterable(fetchedItems).concatWith(oneSecondDelayFlowable);
            }).subscribeOn(Schedulers.single());

        return Flowable.merge(cachedFlow, uncachedFlow).map(BoardgameController::addNewline);
    }

    private static String addNewline(PlayersCountCacheEntry item) {
        return item.toJson() + "\n";
    }
}
