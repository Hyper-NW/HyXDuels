package me.alphatct3209.duels.stats.filter;

public record LeaderboardFilter(String mode, LeaderboardTime time, LeaderboardScope scope)
{
    public LeaderboardFilter
    {
        if (mode != null && mode.isBlank()) mode = null;
        if (time == null) time = LeaderboardTime.LIFETIME;
        if (scope == null) scope = LeaderboardScope.ALL;
    }

    public static LeaderboardFilter defaults()
    {
        return new LeaderboardFilter(null, LeaderboardTime.LIFETIME, LeaderboardScope.ALL);
    }
}
