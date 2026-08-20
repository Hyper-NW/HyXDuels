package me.alphatct3209.duels.stats.filter;

import java.time.LocalDate;

public enum LeaderboardTime
{
    DAILY("Daily"), WEEKLY("Weekly"), MONTHLY("Monthly"), LIFETIME("Lifetime");

    private final String displayName;

    LeaderboardTime(String displayName) { this.displayName = displayName; }

    public String displayName() { return displayName; }

    public LocalDate since(LocalDate today)
    {
        return switch (this)
        {
            case DAILY -> today;
            case WEEKLY -> today.minusDays(6);
            case MONTHLY -> today.withDayOfMonth(1);
            case LIFETIME -> null;
        };
    }
}
