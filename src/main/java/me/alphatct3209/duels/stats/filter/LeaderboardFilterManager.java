package me.alphatct3209.duels.stats.filter;

import me.alphatct3209.duels.Duels;
import me.alphatct3209.duels.configuration.PluginFiles;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

/** Persistent per-player leaderboard view preferences. */
public final class LeaderboardFilterManager
{
    private final File file;
    private final YamlConfiguration yaml;
    private final Map<UUID, LeaderboardFilter> filters = new HashMap<>();

    public LeaderboardFilterManager(Duels plugin)
    {
        file = PluginFiles.data(plugin, "leaderboard-filters.yml", false);
        yaml = YamlConfiguration.loadConfiguration(file);
        load();
    }

    public LeaderboardFilter get(UUID player)
    {
        return filters.getOrDefault(player, LeaderboardFilter.defaults());
    }

    public void set(UUID player, LeaderboardFilter filter)
    {
        filters.put(player, filter);
        String path = "Players." + player;
        yaml.set(path + ".Mode", filter.mode() == null ? "ALL" : filter.mode());
        yaml.set(path + ".Time", filter.time().name());
        yaml.set(path + ".Players", filter.scope().name());
        save();
    }

    public void shutdown() { save(); }

    private void load()
    {
        ConfigurationSection players = yaml.getConfigurationSection("Players");
        if (players == null) return;
        for (String rawUuid : players.getKeys(false))
        {
            try
            {
                UUID uuid = UUID.fromString(rawUuid);
                String base = "Players." + rawUuid;
                String rawMode = yaml.getString(base + ".Mode", "ALL");
                String mode = rawMode.equalsIgnoreCase("ALL") ? null : rawMode;
                LeaderboardTime time = enumValue(LeaderboardTime.class,
                        yaml.getString(base + ".Time"), LeaderboardTime.LIFETIME);
                LeaderboardScope scope = enumValue(LeaderboardScope.class,
                        yaml.getString(base + ".Players"), LeaderboardScope.ALL);
                filters.put(uuid, new LeaderboardFilter(mode, time, scope));
            }
            catch (IllegalArgumentException ignored) { }
        }
    }

    private static <E extends Enum<E>> E enumValue(Class<E> type, String raw, E fallback)
    {
        if (raw == null) return fallback;
        try { return Enum.valueOf(type, raw.toUpperCase(Locale.ROOT).replace('-', '_')); }
        catch (IllegalArgumentException ignored) { return fallback; }
    }

    private void save()
    {
        File temporary = new File(file.getParentFile(), file.getName() + ".tmp");
        try
        {
            file.getParentFile().mkdirs();
            yaml.save(temporary);
            try
            {
                Files.move(temporary.toPath(), file.toPath(), StandardCopyOption.REPLACE_EXISTING,
                        StandardCopyOption.ATOMIC_MOVE);
            }
            catch (AtomicMoveNotSupportedException exception)
            {
                Files.move(temporary.toPath(), file.toPath(), StandardCopyOption.REPLACE_EXISTING);
            }
        }
        catch (IOException exception)
        {
            temporary.delete();
            throw new IllegalStateException("Could not save leaderboard-filters.yml", exception);
        }
    }
}
