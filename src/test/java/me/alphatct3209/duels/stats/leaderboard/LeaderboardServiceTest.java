package me.alphatct3209.duels.stats.leaderboard;

import me.alphatct3209.duels.stats.db.StatisticsDatabase;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.time.LocalDate;
import java.util.Set;
import me.alphatct3209.duels.game.modes.ModeKey;
import me.alphatct3209.duels.stats.db.LeaderboardMetric;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LeaderboardServiceTest
{
    private static final UUID FIRST = UUID.fromString("00000000-0000-0000-0000-000000000001");
    private static final UUID SECOND = UUID.fromString("00000000-0000-0000-0000-000000000002");
    private static final UUID THIRD = UUID.fromString("00000000-0000-0000-0000-000000000003");

    @Test
    void publishesImmutableCompleteSnapshotsWithStableValueThenUuidOrdering()
    {
        FakeDatabase database = new FakeDatabase();
        database.wins.put(SECOND, 9);
        database.wins.put(FIRST, 9);
        database.wins.put(THIRD, 12);
        database.kills.put(FIRST, 4);
        database.modes.put("nodebuff_ranked", map(SECOND, 5, FIRST, 5));

        LeaderboardService service = service(database, List.of("nodebuff_ranked"), new ArrayList<>(), new long[]{0});
        assertTrue(service.refresh());
        LeaderboardSnapshot snapshot = service.snapshot();

        assertEquals(List.of(THIRD, FIRST, SECOND),
                snapshot.overallWins().stream().map(LeaderboardEntry::uuid).toList());
        assertEquals(List.of(FIRST, SECOND), snapshot.modes().get("nodebuff_ranked").stream()
                .map(LeaderboardEntry::uuid).toList());
        assertEquals("Division 5", snapshot.mode("nodebuff_ranked", 1).orElseThrow().division());
        assertEquals(FIRST.toString(),
                new LeaderboardEntry(FIRST, null, 1, null).name(), "null names fall back to UUID");
        assertThrows(UnsupportedOperationException.class,
                () -> snapshot.overallWins().add(new LeaderboardEntry(FIRST, "x", 1, "x")));
        assertThrows(UnsupportedOperationException.class,
                () -> snapshot.modes().put("other", List.of()));
    }

    @Test
    void backendFailureRetainsSnapshotAndWarningsAreRateLimited()
    {
        FakeDatabase database = new FakeDatabase();
        database.wins.put(FIRST, 3);
        List<String> warnings = new ArrayList<>();
        long[] clock = {0L};
        LeaderboardService service = service(database, List.of("default"), warnings, clock);
        assertTrue(service.refresh());
        LeaderboardSnapshot successful = service.snapshot();

        database.fail = true;
        assertFalse(service.refresh());
        assertSame(successful, service.snapshot());
        assertEquals(1, warnings.size());

        clock[0] = 999L;
        assertFalse(service.refresh());
        assertEquals(1, warnings.size());
        clock[0] = 1000L;
        assertFalse(service.refresh());
        assertEquals(2, warnings.size());
        assertTrue(warnings.getFirst().contains("retaining the last snapshot"));
    }

    private LeaderboardService service(FakeDatabase database, List<String> modes,
                                       List<String> warnings, long[] clock)
    {
        return new LeaderboardService(database, () -> modes,
                wins -> "Division " + wins, warnings::add, () -> clock[0], Duration.ofSeconds(1));
    }

    private static HashMap<UUID, Integer> map(Object... pairs)
    {
        HashMap<UUID, Integer> result = new HashMap<>();
        for (int index = 0; index < pairs.length; index += 2)
        {
            result.put((UUID) pairs[index], (Integer) pairs[index + 1]);
        }
        return result;
    }

    private static final class FakeDatabase implements StatisticsDatabase
    {
        private final HashMap<UUID, Integer> wins = new HashMap<>();
        private final HashMap<UUID, Integer> kills = new HashMap<>();
        private final Map<String, HashMap<UUID, Integer>> modes = new HashMap<>();
        private boolean fail;

        @Override public int getWins(UUID uuid) { return wins.getOrDefault(uuid, 0); }
        @Override public int getLosses(UUID uuid) { return 0; }
        @Override public int getKills(UUID uuid) { return kills.getOrDefault(uuid, 0); }
        @Override public int getDeaths(UUID uuid) { return 0; }
        @Override public int getWinStreak(UUID uuid) { return 0; }
        @Override public int getHighestWinStreak(UUID uuid) { return 0; }
        @Override public int getGamemodeWins(UUID uuid, String gamemode) {
            return modes.getOrDefault(gamemode, new HashMap<>()).getOrDefault(uuid, 0);
        }
        @Override public void setWins(UUID uuid, int value) { wins.put(uuid, value); }
        @Override public void setLosses(UUID uuid, int value) {}
        @Override public void setKills(UUID uuid, int value) { kills.put(uuid, value); }
        @Override public void setDeaths(UUID uuid, int value) {}
        @Override public void setWinStreak(UUID uuid, int value) {}
        @Override public void setHighestWinStreak(UUID uuid, int value) {}
        @Override public void setGamemodeWins(UUID uuid, String mode, int value) {
            modes.computeIfAbsent(mode, ignored -> new HashMap<>()).put(uuid, value);
        }
        @Override public void registerNewPlayer(UUID uuid, String name) {}
        @Override public void updateLastKnownName(UUID uuid, String name) {}
        @Override public boolean isRegistered(UUID uuid) { return true; }
        @Override public UUID getUUID(String playerName) { return null; }
        @Override public String getLastKnownName(UUID uuid) {
            return uuid.equals(FIRST) ? null : "Player-" + uuid.toString().charAt(35);
        }
        @Override public HashMap<UUID, Integer> getTopTenWins() {
            if (fail) throw new IllegalStateException("backend offline");
            return new HashMap<>(wins);
        }
        @Override public HashMap<UUID, Integer> getTopTenKills() { return new HashMap<>(kills); }
        @Override public HashMap<UUID, Integer> getTopTenGamemodeWins(String mode) {
            return new HashMap<>(modes.getOrDefault(mode, new HashMap<>()));
        }
        @Override public void incrementPeriodic(UUID player, ModeKey mode, LeaderboardMetric metric, LocalDate date) {}
        @Override public HashMap<UUID, Integer> getFilteredLeaderboard(
                LeaderboardMetric metric, String mode, LocalDate since, Set<UUID> players) {
            return new HashMap<>();
        }
    }
}
