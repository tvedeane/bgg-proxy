package dev.tvedeane;

import java.util.List;

public record PlayersCountDto(String id, List<Integer> bestWith, List<Integer> recommendedWith) {
}
