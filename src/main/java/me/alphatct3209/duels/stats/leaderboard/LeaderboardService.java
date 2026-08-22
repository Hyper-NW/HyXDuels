package me.alphatct3209.duels.stats.leaderboard;

import me.alphatct3209.duels.stats.db.StatisticsDatabase;
import me.alphatct3209.duels.stats.db.LeaderboardMetric;

import java.time.Duration;
import java.time.LocalDate;
import java.util.Collection;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;
import java.util.function.IntFunction;
import java.util.function.LongSupplier;
import java.util.function.Supplier;

/**
 * Loads leaderboard data only during refreshes and atomically publishes immutable snapshots.
 * Readers never touch the statistics backend.
 */
public final class LeaderboardService
{
    private static final Comparator<Map.Entry<UUID, Integer>> ORDER =
            Map.Entry.<UUID, Integer>comparingByValue().reversed()
                    .thenComparing(entry -> entry.getKey().toString());

    private final StatisticsDatabase database;
    private final Supplier<? extends Collection<String>> modeKeys;
    private final IntFunction<String> divisionName;
    private final Consumer<String> warningLogger;
    private final LongSupplier clockMillis;
    private final long warningIntervalMillis;
    private final AtomicReference<LeaderboardSnapshot> snapshot =
            new AtomicReference<>(LeaderboardSnapshot.empty());
    private long lastWarningMillis = -1L;

    public LeaderboardService(StatisticsDatabase database,
                              Supplier<? extends Collection<String>> modeKeys,
                              IntFunction<String> divisionName,
                              Consumer<String> warningLogger,
                              LongSupplier clockMillis,
                              Duration warningInterval)
    {
        this.database = Objects.requireNonNull(database, "database");
        this.modeKeys = Objects.requireNonNull(modeKeys, "modeKeys");
        this.divisionName = Objects.requireNonNull(divisionName, "divisionName");
        this.warningLogger = Objects.requireNonNull(warningLogger, "warningLogger");
        this.clockMillis = Objects.requireNonNull(clockMillis, "clockMillis");
        this.warningIntervalMillis = Math.max(0L,
                Objects.requireNonNull(warningInterval, "warningInterval").toMillis());
    }

    public LeaderboardSnapshot snapshot()
    {
        return snapshot.get();
    }

    /**
     * Builds and publishes one complete snapshot. Any backend/runtime failure preserves the
     * previously published snapshot rather than exposing partial or empty data.
     */
    public synchronized boolean refresh()
    {
        try
        {
            Set<String> modes = normalizedModes(modeKeys.get());
            List<LeaderboardEntry> wins = rows(database.getTopTenWins(), false);
            List<LeaderboardEntry> kills = rows(database.getTopTenKills(), false);
            Map<String, List<LeaderboardEntry>> modeRows = new LinkedHashMap<>();
            Map<String, List<LeaderboardEntry>> dailyModeRows = new LinkedHashMap<>();
            for (String mode : modes)
            {
                modeRows.put(mode, rows(database.getTopTenGamemodeWins(mode), true));
                dailyModeRows.put(mode, rows(database.getFilteredLeaderboard(
                        LeaderboardMetric.WINS, mode, LocalDate.now(), null), false));
            }
            snapshot.set(new LeaderboardSnapshot(wins, kills, modeRows, dailyModeRows));
            return true;
        }
        catch (RuntimeException exception)
        {
            warn(exception);
            return false;
        }
    }

    private List<LeaderboardEntry> rows(Map<UUID, Integer> values, boolean withDivision)
    {
        return Objects.requireNonNull(values, "Leaderboard backend returned null").entrySet().stream()
                .sorted(ORDER)
                .limit(10)
                .map(entry -> new LeaderboardEntry(entry.getKey(),
                        database.getLastKnownName(entry.getKey()), entry.getValue(),
                        withDivision ? divisionName.apply(entry.getValue()) : "Unranked"))
                .toList();
    }

    private Set<String> normalizedModes(Collection<String> configured)
    {
        Objects.requireNonNull(configured, "Configured mode keys cannot be null");
        Set<String> normalized = new LinkedHashSet<>();
        for (String mode : configured)
        {
            if (mode == null || !mode.matches("[a-z0-9]+(?:_[a-z0-9]+)*"))
            {
                throw new IllegalArgumentException("Invalid configured leaderboard mode key: " + mode);
            }
            normalized.add(mode.toLowerCase(Locale.ROOT));
        }
        return Set.copyOf(normalized);
    }

    private void warn(RuntimeException exception)
    {
        long now = clockMillis.getAsLong();
        if (lastWarningMillis < 0L || now < lastWarningMillis
                || now - lastWarningMillis >= warningIntervalMillis)
        {
            lastWarningMillis = now;
            String detail = exception.getMessage();
            warningLogger.accept("Could not refresh leaderboard cache; retaining the last snapshot"
                    + (detail == null || detail.isBlank() ? "." : ": " + detail));
        }
    }
}
