package dev.tvedeane;

import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.List;

public record PlayersCountDto(String id, List<Integer> bestWith, List<Integer> recommendedWith) {
    public String toJson() {
        try {
            return new ObjectMapper().writeValueAsString(this);
        } catch (Exception e) {
            return "{}";
        }
    }
}
