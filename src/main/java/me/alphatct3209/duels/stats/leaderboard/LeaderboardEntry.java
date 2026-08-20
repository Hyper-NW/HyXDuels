package me.alphatct3209.duels.stats.leaderboard;

import java.util.Objects;
import java.util.UUID;

/** One immutable, display-ready leaderboard row. */
public record LeaderboardEntry(UUID uuid, String name, int value, String division)
{
    public LeaderboardEntry
    {
        Objects.requireNonNull(uuid, "uuid");
        name = name == null || name.isBlank() ? uuid.toString() : name;
        division = division == null || division.isBlank() ? "Unranked" : division;
    }
}
