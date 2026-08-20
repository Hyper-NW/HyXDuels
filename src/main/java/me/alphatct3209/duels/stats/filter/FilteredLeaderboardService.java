package me.alphatct3209.duels.stats.filter;

import me.alphatct3209.duels.Duels;
import me.alphatct3209.duels.hologram.HologramDefinition;
import me.alphatct3209.duels.stats.db.LeaderboardMetric;
import me.alphatct3209.duels.stats.leaderboard.LeaderboardEntry;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/** Resolves viewer-specific hologram rows without mutating the global cached leaderboard. */
public final class FilteredLeaderboardService
{
    private final Duels plugin;
    private final LeaderboardFilterManager filters;
    private final Map<CacheKey, CachedRows> cache = new HashMap<>();

    public FilteredLeaderboardService(Duels plugin, LeaderboardFilterManager filters)
    {
        this.plugin = plugin;
        this.filters = filters;
    }

    public LeaderboardEntry entry(UUID viewer, HologramDefinition.Type type,
                                  String fallbackMode, int rank)
    {
        List<LeaderboardEntry> rows = rows(viewer, type, fallbackMode);
        return rank < 1 || rank > rows.size() ? null : rows.get(rank - 1);
    }

    public String summary(UUID viewer, HologramDefinition.Type type, String fallbackMode)
    {
        LeaderboardFilter filter = filters.get(viewer);
        String mode = effectiveMode(filter, type, fallbackMode);
        String modeName = mode == null ? "All Modes" : plugin.getModeManager().resolve(mode)
                .map(value -> value.displayName()).orElse(mode);
        return modeName + " / " + filter.time().displayName() + " / " + filter.scope().displayName();
    }

    public void invalidate(UUID viewer)
    {
        cache.keySet().removeIf(key -> key.viewer().equals(viewer));
    }

    private List<LeaderboardEntry> rows(UUID viewer, HologramDefinition.Type type, String fallbackMode)
    {
        LeaderboardFilter filter = filters.get(viewer);
        String mode = effectiveMode(filter, type, fallbackMode);
        CacheKey key = new CacheKey(viewer, type, mode, filter.time(), filter.scope(), LocalDate.now());
        CachedRows cached = cache.get(key);
        long now = System.currentTimeMillis();
        if (cached != null && now - cached.createdAt() < 30_000L) return cached.rows();

        Set<UUID> candidates = candidates(viewer, filter.scope());
        LeaderboardMetric metric = type == HologramDefinition.Type.KILLS
                ? LeaderboardMetric.KILLS : LeaderboardMetric.WINS;
        Map<UUID, Integer> values = plugin.getStatisticsManager().getStatsDB()
                .getFilteredLeaderboard(metric, mode, filter.time().since(LocalDate.now()), candidates);
        List<LeaderboardEntry> rows = new ArrayList<>();
        for (Map.Entry<UUID, Integer> value : values.entrySet())
        {
            String name = plugin.getStatisticsManager().getStatsDB().getLastKnownName(value.getKey());
            String division = plugin.getDivisionManager().getCurrentDivision(value.getValue())
                    .map(result -> result.displayName()).orElse("Unranked");
            rows.add(new LeaderboardEntry(value.getKey(), name, value.getValue(), division));
        }
        List<LeaderboardEntry> immutable = List.copyOf(rows);
        cache.put(key, new CachedRows(immutable, now));
        return immutable;
    }

    private String effectiveMode(LeaderboardFilter filter, HologramDefinition.Type type, String fallback)
    {
        if (filter.mode() != null) return filter.mode();
        return type == HologramDefinition.Type.DIVISIONS ? fallback : null;
    }

    private Set<UUID> candidates(UUID viewer, LeaderboardScope scope)
    {
        if (scope == LeaderboardScope.ALL) return null;
        Set<UUID> result = new LinkedHashSet<>();
        result.add(viewer);
        switch (scope)
        {
            case FRIENDS -> result.addAll(plugin.getSocialManager().friends(viewer));
            case BEST_FRIENDS -> result.addAll(plugin.getSocialManager().bestFriends(viewer));
            case GUILD -> result.addAll(plugin.getSocialManager().guildMembers(viewer));
            case ALL -> { }
        }
        return Set.copyOf(result);
    }

    private record CacheKey(UUID viewer, HologramDefinition.Type type, String mode,
                            LeaderboardTime time, LeaderboardScope scope, LocalDate date) {}
    private record CachedRows(List<LeaderboardEntry> rows, long createdAt) {}
}
