package me.alphatct3209.duels.stats.db;

public enum LeaderboardMetric
{
    WINS("Wins"),
    KILLS("Kills");

    private final String column;

    LeaderboardMetric(String column)
    {
        this.column = column;
    }

    public String column()
    {
        return column;
    }
}
