package dev.tvedeane;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.dataformat.xml.XmlMapper;
import io.micronaut.http.HttpRequest;
import io.micronaut.http.client.HttpClient;
import io.micronaut.http.client.annotation.Client;
import io.micronaut.http.uri.UriBuilder;
import jakarta.inject.Singleton;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

import static io.micronaut.http.HttpHeaders.USER_AGENT;

/**
 * Read more at <a href="https://boardgamegeek.com/wiki/page/BGG_XML_API2">BGG_XML_API2</a>.
 */
@Singleton
public class BoardgamegeekClient {
    public static final int MAX_READING_THINGS = 20;

    private static final Logger LOG = LoggerFactory.getLogger(BoardgamegeekClient.class);
    private final HttpClient httpClient;
    private final XmlMapper xmlMapper = XmlMapper.builder().build();

    public BoardgamegeekClient(@Client(id = "bgg") HttpClient httpClient) {
        this.httpClient = httpClient;
    }

    List<PlayersCountDto> fetchGame(List<String> ids) {
        var BGG_API_THING_URL = "https://boardgamegeek.com/xmlapi2/thing";
        var uri = UriBuilder.of(BGG_API_THING_URL).queryParam("id", String.join(",", ids)).build();
        HttpRequest<?> req = HttpRequest.GET(uri)
                .header(USER_AGENT, "Micronaut HTTP Client");

        var response = httpClient.toBlocking().exchange(req, byte[].class);
        var responseBody = new String(response.body(), StandardCharsets.UTF_8);
        Boardgames result;
        try {
            result = xmlMapper.readValue(responseBody, Boardgames.class);
        } catch (JsonProcessingException e) {
            LOG.error("Cannot parse XML: {}", e.getMessage());
            return List.of();
        }
        return result.items().stream().map(i -> toPlayersCountDto(i.id(), i.pollSummary().result())).toList();
    }

    private static PlayersCountDto toPlayersCountDto(String id, List<PollResult> pollResults) {
        final var bestWith = new ArrayList<Integer>();
        final var recommendedWith = new ArrayList<Integer>();
        pollResults.forEach(p -> {
            var playerNumbers = extractPlayerNumbers(p.value());
            switch (p.name()) {
                case "bestwith":
                    bestWith.addAll(playerNumbers);
                    break;
                case "recommmendedwith": // handling typo in the returned XML
                    recommendedWith.addAll(playerNumbers);
                    break;
            }
        });
        return new PlayersCountDto(id, bestWith, recommendedWith);
    }

    private  static List<Integer> extractPlayerNumbers(String input) {
        var result = new ArrayList<Integer>();

        var individualNumbersOrRanges = Pattern.compile("(\\d+)(?:[–-](\\d+))?");
        var matcher = individualNumbersOrRanges.matcher(input);

        while (matcher.find()) {
            int start = Integer.parseInt(matcher.group(1)); // First number
            if (matcher.group(2) != null) { // Range detected
                int end = Integer.parseInt(matcher.group(2)); // Second number
                for (int i = start; i <= end; i++) {
                    result.add(i);
                }
            } else { // Single number
                result.add(start);
            }
        }
        return result;
    }
}
