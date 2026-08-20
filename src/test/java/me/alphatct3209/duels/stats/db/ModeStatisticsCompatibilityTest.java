package me.alphatct3209.duels.stats.db;

import me.alphatct3209.duels.game.modes.ModeKey;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.time.LocalDate;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ModeStatisticsCompatibilityTest
{
    @Test
    void typedApisPreserveThePhysicalStableModeKey()
    {
        CapturingDatabase database = new CapturingDatabase();
        UUID player = UUID.randomUUID();
        ModeKey mode = ModeKey.parse("bed_wars");
        database.setModeWins(player, mode, 7);
        assertEquals("bed_wars", database.lastKey);
        assertEquals(7, database.getModeWins(player, mode));
        assertEquals("bed_wars", database.lastKey);
    }

    @Test
    void completedDuelTracksLifetimeWinsAndBothWinStreakValues()
    {
        CapturingDatabase database = new CapturingDatabase();
        UUID winner = UUID.randomUUID();
        UUID loser = UUID.randomUUID();
        database.wins.put(winner, 9);
        database.losses.put(loser, 4);
        database.winStreaks.put(winner, 2);
        database.winStreaks.put(loser, 7);
        database.highestWinStreaks.put(winner, 5);

        StatisticsDatabase.DuelWinUpdate update = database.recordDuelWin(
                winner, loser, ModeKey.parse("classic"));

        assertEquals(10, database.getWins(winner));
        assertEquals(5, database.getLosses(loser));
        assertEquals(3, database.getWinStreak(winner));
        assertEquals(5, database.getHighestWinStreak(winner));
        assertEquals(0, database.getWinStreak(loser));
        assertEquals(0, update.previousModeWins());
        assertEquals(1, update.currentModeWins());

        database.winStreaks.put(winner, 5);
        database.recordDuelWin(winner, loser, ModeKey.parse("classic"));
        assertEquals(6, database.getHighestWinStreak(winner));
    }

    private static final class CapturingDatabase implements StatisticsDatabase
    {
        String lastKey; int value;
        final Map<UUID, Integer> wins = new HashMap<>();
        final Map<UUID, Integer> losses = new HashMap<>();
        final Map<UUID, Integer> winStreaks = new HashMap<>();
        final Map<UUID, Integer> highestWinStreaks = new HashMap<>();
        @Override public int getGamemodeWins(UUID uuid, String gamemode) { lastKey = gamemode; return value; }
        @Override public void setGamemodeWins(UUID uuid, String gamemode, int wins) { lastKey = gamemode; value = wins; }
        @Override public int getWins(UUID uuid) { return wins.getOrDefault(uuid, 0); }
        @Override public int getLosses(UUID uuid) { return losses.getOrDefault(uuid, 0); }
        @Override public int getKills(UUID uuid) { return 0; }
        @Override public int getDeaths(UUID uuid) { return 0; }
        @Override public int getWinStreak(UUID uuid) { return winStreaks.getOrDefault(uuid, 0); }
        @Override public int getHighestWinStreak(UUID uuid) { return highestWinStreaks.getOrDefault(uuid, 0); }
        @Override public void setWins(UUID uuid, int value) { wins.put(uuid, value); }
        @Override public void setLosses(UUID uuid, int value) { losses.put(uuid, value); }
        @Override public void setKills(UUID uuid, int kills) {}
        @Override public void setDeaths(UUID uuid, int deaths) {}
        @Override public void setWinStreak(UUID uuid, int value) { winStreaks.put(uuid, value); }
        @Override public void setHighestWinStreak(UUID uuid, int value) { highestWinStreaks.put(uuid, value); }
        @Override public void registerNewPlayer(UUID uuid, String name) {}
        @Override public void updateLastKnownName(UUID uuid, String name) {}
        @Override public boolean isRegistered(UUID uuid) { return false; }
        @Override public UUID getUUID(String playerName) { return null; }
        @Override public String getLastKnownName(UUID uuid) { return null; }
        @Override public HashMap<UUID, Integer> getTopTenWins() { return new HashMap<>(); }
        @Override public HashMap<UUID, Integer> getTopTenKills() { return new HashMap<>(); }
        @Override public HashMap<UUID, Integer> getTopTenGamemodeWins(String gamemode) { return new HashMap<>(); }
        @Override public void incrementPeriodic(UUID player, ModeKey mode, LeaderboardMetric metric, LocalDate date) {}
        @Override public HashMap<UUID, Integer> getFilteredLeaderboard(
                LeaderboardMetric metric, String mode, LocalDate since, Set<UUID> players) {
            return new HashMap<>();
        }
    }
}
