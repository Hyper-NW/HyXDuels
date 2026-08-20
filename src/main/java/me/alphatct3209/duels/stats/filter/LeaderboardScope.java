package me.alphatct3209.duels.stats.filter;

public enum LeaderboardScope
{
    ALL("All Players"), FRIENDS("Friends"), BEST_FRIENDS("Best Friends"), GUILD("Guild Members");

    private final String displayName;

    LeaderboardScope(String displayName) { this.displayName = displayName; }

    public String displayName() { return displayName; }
}
