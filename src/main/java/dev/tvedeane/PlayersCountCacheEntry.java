package dev.tvedeane;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.BitSet;
import java.util.List;

public record PlayersCountCacheEntry(Long id, BitSet bestWith, BitSet recommendedWith) {
    private static final Logger LOG = LoggerFactory.getLogger(PlayersCountCacheEntry.class);

    PlayersCountCacheEntry(String id, List<Integer> bestWith, List<Integer> recommendedWith) {
        this(Long.parseLong(id), toBitSet(bestWith), toBitSet(recommendedWith));
    }

    private static BitSet toBitSet(List<Integer> integers) {
        var bitSet = new BitSet();
        for (Integer i : integers) {
            bitSet.set(i);
        }
        return bitSet;
    }

    private static List<Integer> toList(BitSet bitSet) {
        var list = new ArrayList<Integer>();
        for (int i = 1; i <= 9; i++) {
            if (bitSet.get(i)) {
                list.add(i);
            }
        }
        return list;
    }

    public String toJson() {
        try {
            var objectMapper = new ObjectMapper();
            return "{\"id\":\"%s\",\"bestWith\":%s,\"recommendedWith\":%s}"
                .formatted(
                    this.id,
                    objectMapper.writeValueAsString(toList(this.bestWith)),
                    objectMapper.writeValueAsString(toList(this.recommendedWith))
                );
        } catch (Exception e) {
            LOG.error("Cannot serialize entry: {}", e.getMessage());
            return "{}";
        }
    }
}
