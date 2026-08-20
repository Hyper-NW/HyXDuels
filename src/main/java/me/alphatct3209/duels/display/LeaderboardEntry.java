package me.alphatct3209.duels.display;

import java.util.Objects;

/** A display-safe immutable leaderboard row. */
public record LeaderboardEntry(String player, int score)
{
    public LeaderboardEntry
    {
        player = Objects.requireNonNullElse(player, "-");
    }
}
