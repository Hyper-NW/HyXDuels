package me.alphatct3209.duels.stats.db.impl;

import me.alphatct3209.duels.game.modes.ModeKey;
import me.alphatct3209.duels.stats.StatisticsManager;
import me.alphatct3209.duels.stats.db.LeaderboardMetric;
import me.alphatct3209.duels.stats.db.StatisticsDatabase;
import org.bukkit.configuration.ConfigurationSection;

import java.util.HashMap;
import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.ToIntFunction;

public class StatisticsDatabaseYAML implements StatisticsDatabase
{
    private final StatisticsManager manager;

    public StatisticsDatabaseYAML(StatisticsManager manager)
    {
        this.manager = manager;
    }

    private String path(UUID uuid, String statistic)
    {
        return "Statistics." + uuid + "." + statistic;
    }

    private String gamemodeWinsPath(UUID uuid, String gamemode)
    {
        return path(uuid, "Gamemodes." + ModeKey.parse(gamemode).value() + ".Wins");
    }

    @Override
    public int getWins(UUID uuid)
    {
        return manager.getStatsConfig().getInt(path(uuid, "Wins"));
    }

    @Override
    public int getLosses(UUID uuid)
    {
        return manager.getStatsConfig().getInt(path(uuid, "Losses"));
    }

    @Override
    public int getKills(UUID uuid)
    {
        return manager.getStatsConfig().getInt(path(uuid, "Kills"));
    }

    @Override
    public int getDeaths(UUID uuid)
    {
        return manager.getStatsConfig().getInt(path(uuid, "Deaths"));
    }

    @Override
    public int getWinStreak(UUID uuid)
    {
        return manager.getStatsConfig().getInt(path(uuid, "WinStreak"));
    }

    @Override
    public int getHighestWinStreak(UUID uuid)
    {
        return manager.getStatsConfig().getInt(path(uuid, "HighestWinStreak"));
    }

    @Override
    public int getGamemodeWins(UUID uuid, String gamemode)
    {
        return manager.getStatsConfig().getInt(gamemodeWinsPath(uuid, gamemode), 0);
    }

    @Override
    public void setWins(UUID uuid, int wins)
    {
        set(uuid, "Wins", wins);
    }

    @Override
    public void setLosses(UUID uuid, int losses)
    {
        set(uuid, "Losses", losses);
    }

    @Override
    public void setKills(UUID uuid, int kills)
    {
        set(uuid, "Kills", kills);
    }

    @Override
    public void setDeaths(UUID uuid, int deaths)
    {
        set(uuid, "Deaths", deaths);
    }

    @Override
    public void setWinStreak(UUID uuid, int winStreak)
    {
        set(uuid, "WinStreak", Math.max(0, winStreak));
    }

    @Override
    public void setHighestWinStreak(UUID uuid, int highestWinStreak)
    {
        set(uuid, "HighestWinStreak", Math.max(0, highestWinStreak));
    }

    @Override
    public void setGamemodeWins(UUID uuid, String gamemode, int wins)
    {
        manager.getStatsConfig().set(gamemodeWinsPath(uuid, gamemode), Math.max(0, wins));
        manager.saveStatsConfig();
    }

    @Override
    public void incrementPeriodic(UUID player, ModeKey mode, LeaderboardMetric metric, LocalDate date)
    {
        String valuePath = path(player, "Periods." + date + "." + mode.value()
                + "." + metric.column());
        int current = manager.getStatsConfig().getInt(valuePath, 0);
        manager.getStatsConfig().set(valuePath,
                current == Integer.MAX_VALUE ? current : Math.max(0, current) + 1);
        manager.saveStatsConfig();
    }

    @Override
    public HashMap<UUID, Integer> getFilteredLeaderboard(LeaderboardMetric metric, String mode,
                                                         LocalDate since, Set<UUID> players)
    {
        String modeKey = mode == null ? null : ModeKey.parse(mode).value();
        ConfigurationSection section = manager.getStatsConfig().getConfigurationSection("Statistics");
        HashMap<UUID, Integer> values = new HashMap<>();
        if (section == null)
        {
            return values;
        }
        for (String rawUuid : section.getKeys(false))
        {
            UUID uuid;
            try
            {
                uuid = UUID.fromString(rawUuid);
            }
            catch (IllegalArgumentException ignored)
            {
                continue;
            }
            if (players != null && !players.contains(uuid))
            {
                continue;
            }
            int value;
            if (since == null && modeKey == null)
            {
                value = metric == LeaderboardMetric.WINS ? getWins(uuid) : getKills(uuid);
            }
            else if (since == null && metric == LeaderboardMetric.WINS)
            {
                value = getGamemodeWins(uuid, modeKey);
            }
            else
            {
                value = periodicTotal(uuid, metric, modeKey, since);
            }
            values.put(uuid, value);
        }
        return topTen(values);
    }

    private int periodicTotal(UUID uuid, LeaderboardMetric metric, String mode, LocalDate since)
    {
        ConfigurationSection dates = manager.getStatsConfig().getConfigurationSection(
                path(uuid, "Periods"));
        if (dates == null)
        {
            return 0;
        }
        long total = 0L;
        for (String rawDate : dates.getKeys(false))
        {
            LocalDate date;
            try
            {
                date = LocalDate.parse(rawDate);
            }
            catch (java.time.format.DateTimeParseException ignored)
            {
                continue;
            }
            if (since != null && date.isBefore(since))
            {
                continue;
            }
            ConfigurationSection modes = dates.getConfigurationSection(rawDate);
            if (modes == null)
            {
                continue;
            }
            if (mode != null)
            {
                total += Math.max(0, modes.getInt(mode + "." + metric.column(), 0));
            }
            else
            {
                for (String modeKey : modes.getKeys(false))
                {
                    total += Math.max(0, modes.getInt(modeKey + "." + metric.column(), 0));
                }
            }
            if (total >= Integer.MAX_VALUE)
            {
                return Integer.MAX_VALUE;
            }
        }
        return (int) total;
    }

    private HashMap<UUID, Integer> topTen(Map<UUID, Integer> values)
    {
        HashMap<UUID, Integer> result = new LinkedHashMap<>();
        values.entrySet().stream()
                .sorted(Map.Entry.<UUID, Integer>comparingByValue().reversed()
                        .thenComparing(entry -> entry.getKey().toString()))
                .limit(10)
                .forEach(entry -> result.put(entry.getKey(), entry.getValue()));
        return result;
    }

    private void set(UUID uuid, String statistic, int value)
    {
        manager.getStatsConfig().set(path(uuid, statistic), value);
        manager.saveStatsConfig();
    }

    @Override
    public void registerNewPlayer(UUID uuid, String name)
    {
        manager.getStatsConfig().set(path(uuid, "Last-Known-Name"), name);
        manager.getStatsConfig().set(path(uuid, "Wins"), 0);
        manager.getStatsConfig().set(path(uuid, "Losses"), 0);
        manager.getStatsConfig().set(path(uuid, "Kills"), 0);
        manager.getStatsConfig().set(path(uuid, "Deaths"), 0);
        manager.getStatsConfig().set(path(uuid, "WinStreak"), 0);
        manager.getStatsConfig().set(path(uuid, "HighestWinStreak"), 0);
        manager.saveStatsConfig();
    }

    @Override
    public void updateLastKnownName(UUID uuid, String name)
    {
        String namePath = path(uuid, "Last-Known-Name");
        if (!name.equals(manager.getStatsConfig().getString(namePath)))
        {
            manager.getStatsConfig().set(namePath, name);
            manager.saveStatsConfig();
        }
    }

    @Override
    public boolean isRegistered(UUID uuid)
    {
        return manager.getStatsConfig().contains("Statistics." + uuid);
    }

    @Override
    public UUID getUUID(String playerName)
    {
        ConfigurationSection section = manager.getStatsConfig().getConfigurationSection("Statistics");
        if (section == null)
        {
            return null;
        }
        for (String uuidStr : section.getKeys(false))
        {
            String knownName = manager.getStatsConfig().getString(
                    "Statistics." + uuidStr + ".Last-Known-Name");
            if (knownName != null && knownName.equalsIgnoreCase(playerName))
            {
                return UUID.fromString(uuidStr);
            }
        }
        return null;
    }

    @Override
    public String getLastKnownName(UUID uuid)
    {
        return manager.getStatsConfig().getString(path(uuid, "Last-Known-Name"));
    }

    @Override
    public HashMap<UUID, Integer> getTopTenWins()
    {
        return getTopTen(this::getWins);
    }

    @Override
    public HashMap<UUID, Integer> getTopTenKills()
    {
        return getTopTen(this::getKills);
    }

    @Override
    public HashMap<UUID, Integer> getTopTenGamemodeWins(String gamemode)
    {
        String key = ModeKey.parse(gamemode).value();
        return getTopTen(uuid -> getGamemodeWins(uuid, key));
    }

    private HashMap<UUID, Integer> getTopTen(ToIntFunction<UUID> valueProvider)
    {
        ConfigurationSection section = manager.getStatsConfig().getConfigurationSection("Statistics");
        HashMap<UUID, Integer> values = new HashMap<>();
        HashMap<UUID, Integer> topTen = new HashMap<>();
        if (section == null)
        {
            return topTen;
        }

        for (String uuidStr : section.getKeys(false))
        {
            UUID uuid = UUID.fromString(uuidStr);
            values.put(uuid, valueProvider.applyAsInt(uuid));
        }
        values.entrySet().stream()
                .sorted(Map.Entry.<UUID, Integer>comparingByValue().reversed()
                        .thenComparing(entry -> entry.getKey().toString()))
                .limit(10)
                .forEach(entry -> topTen.put(entry.getKey(), entry.getValue()));
        return topTen;
    }
}
