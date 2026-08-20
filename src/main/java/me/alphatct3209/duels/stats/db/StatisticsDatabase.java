package me.alphatct3209.duels.stats.db;

import me.alphatct3209.duels.game.modes.ModeKey;

import java.util.HashMap;
import java.time.LocalDate;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

public interface StatisticsDatabase
{
    int getWins(UUID uuid);

    int getLosses(UUID uuid);

    int getKills(UUID uuid);

    int getDeaths(UUID uuid);

    int getWinStreak(UUID uuid);

    int getHighestWinStreak(UUID uuid);

    int getGamemodeWins(UUID uuid, String gamemode);

    default int getModeWins(UUID uuid, me.alphatct3209.duels.game.modes.ModeKey mode)
    {
        return getGamemodeWins(uuid, mode.value());
    }

    void setWins(UUID uuid, int wins);

    void setLosses(UUID uuid, int losses);

    void setKills(UUID uuid, int kills);

    void setDeaths(UUID uuid, int deaths);

    void setWinStreak(UUID uuid, int winStreak);

    void setHighestWinStreak(UUID uuid, int highestWinStreak);

    void setGamemodeWins(UUID uuid, String gamemode, int wins);

    default void setModeWins(UUID uuid, me.alphatct3209.duels.game.modes.ModeKey mode, int wins)
    {
        setGamemodeWins(uuid, mode.value(), wins);
    }

    /** Records one completed duel and returns the values needed by division/result displays. */
    default DuelWinUpdate recordDuelWin(UUID winner, UUID loser, ModeKey mode)
    {
        Objects.requireNonNull(winner, "winner");
        Objects.requireNonNull(loser, "loser");
        Objects.requireNonNull(mode, "mode");
        if (winner.equals(loser))
        {
            throw new IllegalArgumentException("A duel winner and loser must be different players");
        }

        int previousModeWins = Math.max(0, getModeWins(winner, mode));
        int currentModeWins = increment(previousModeWins);
        int currentWinStreak = increment(Math.max(0, getWinStreak(winner)));
        int highestWinStreak = Math.max(Math.max(0, getHighestWinStreak(winner)), currentWinStreak);

        setWins(winner, increment(Math.max(0, getWins(winner))));
        setLosses(loser, increment(Math.max(0, getLosses(loser))));
        setModeWins(winner, mode, currentModeWins);
        setWinStreak(winner, currentWinStreak);
        setHighestWinStreak(winner, highestWinStreak);
        setWinStreak(loser, 0);
        incrementPeriodic(winner, mode, LeaderboardMetric.WINS, LocalDate.now());
        return new DuelWinUpdate(previousModeWins, currentModeWins,
                currentWinStreak, highestWinStreak);
    }

    default int recordKill(UUID player, ModeKey mode)
    {
        Objects.requireNonNull(player, "player");
        Objects.requireNonNull(mode, "mode");
        int kills = increment(Math.max(0, getKills(player)));
        setKills(player, kills);
        incrementPeriodic(player, mode, LeaderboardMetric.KILLS, LocalDate.now());
        return kills;
    }

    void incrementPeriodic(UUID player, ModeKey mode, LeaderboardMetric metric, LocalDate date);

    /** Null mode means all modes; null since means lifetime. Null players means everybody. */
    HashMap<UUID, Integer> getFilteredLeaderboard(LeaderboardMetric metric, String mode,
                                                  LocalDate since, Set<UUID> players);

    private static int increment(int value)
    {
        return value == Integer.MAX_VALUE ? value : value + 1;
    }

    void registerNewPlayer(UUID uuid, String name);

    /** Updates only the persisted display name; aggregate and gamemode statistics are unchanged. */
    void updateLastKnownName(UUID uuid, String name);

    boolean isRegistered(UUID uuid);

    UUID getUUID(String playerName);

    String getLastKnownName(UUID uuid);

    /** May or may not be sorted depending on the database implementation. */
    HashMap<UUID, Integer> getTopTenWins();

    /** May or may not be sorted depending on the database implementation. */
    HashMap<UUID, Integer> getTopTenKills();

    /** May or may not be sorted depending on the database implementation. */
    HashMap<UUID, Integer> getTopTenGamemodeWins(String gamemode);

    record DuelWinUpdate(int previousModeWins, int currentModeWins,
                         int currentWinStreak, int highestWinStreak) {}
}
