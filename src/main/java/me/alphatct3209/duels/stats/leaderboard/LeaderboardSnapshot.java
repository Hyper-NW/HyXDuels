package me.alphatct3209.duels.stats.leaderboard;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

/** A complete immutable point-in-time view of every cached leaderboard. */
public record LeaderboardSnapshot(List<LeaderboardEntry> overallWins,
                                  List<LeaderboardEntry> overallKills,
                                  Map<String, List<LeaderboardEntry>> modes)
{
    public LeaderboardSnapshot
    {
        overallWins = List.copyOf(overallWins);
        overallKills = List.copyOf(overallKills);
        Map<String, List<LeaderboardEntry>> copiedModes = new LinkedHashMap<>();
        modes.forEach((key, rows) -> copiedModes.put(
                key.toLowerCase(Locale.ROOT), List.copyOf(rows)));
        modes = Map.copyOf(copiedModes);
    }

    public static LeaderboardSnapshot empty()
    {
        return new LeaderboardSnapshot(List.of(), List.of(), Map.of());
    }

    public Optional<LeaderboardEntry> overallWins(int rank)
    {
        return ranked(overallWins, rank);
    }

    public Optional<LeaderboardEntry> overallKills(int rank)
    {
        return ranked(overallKills, rank);
    }

    public Optional<LeaderboardEntry> mode(String key, int rank)
    {
        if (key == null)
        {
            return Optional.empty();
        }
        return ranked(modes.getOrDefault(key.toLowerCase(Locale.ROOT), List.of()), rank);
    }

    private static Optional<LeaderboardEntry> ranked(List<LeaderboardEntry> rows, int rank)
    {
        return rank >= 1 && rank <= rows.size() ? Optional.of(rows.get(rank - 1)) : Optional.empty();
    }
}
