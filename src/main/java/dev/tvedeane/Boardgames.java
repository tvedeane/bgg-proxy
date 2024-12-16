package dev.tvedeane;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlElementWrapper;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlRootElement;
import io.micronaut.core.annotation.Introspected;

import java.util.List;

@Introspected
@JacksonXmlRootElement(localName = "items")
@JsonIgnoreProperties(ignoreUnknown = true)
public record Boardgames(
        @JacksonXmlElementWrapper(useWrapping = false)
        @JacksonXmlProperty(localName = "item")
        List<Item> items) {
}

@JsonIgnoreProperties(ignoreUnknown = true)
record Item(String id, @JacksonXmlProperty(localName = "poll-summary") PollSummary pollSummary) {
}

@JsonIgnoreProperties(ignoreUnknown = true)
record PollSummary(@JacksonXmlElementWrapper(useWrapping = false) List<PollResult> result) {
}

record PollResult(String name, String value) {
}
