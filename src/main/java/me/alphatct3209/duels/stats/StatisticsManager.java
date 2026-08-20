package me.alphatct3209.duels.stats;

import me.alphatct3209.duels.Duels;
import me.alphatct3209.duels.configuration.PluginFiles;
import me.alphatct3209.duels.stats.db.DatabaseType;
import me.alphatct3209.duels.stats.db.StatisticsDatabase;
import me.alphatct3209.duels.stats.db.impl.StatisticsDatabaseSQL;
import me.alphatct3209.duels.stats.db.impl.StatisticsDatabaseYAML;
import org.bukkit.Bukkit;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;

public class StatisticsManager
{
    private final Duels plugin;
    private final FileConfiguration statsFile = new YamlConfiguration();
    private File statsF;
    private final StatisticsDatabase statsDB;

    public StatisticsManager(Duels plugin, DatabaseType dbType)
    {
        this.plugin = plugin;
        this.statsDB = connectToDatabase(dbType);
    }

    private StatisticsDatabase connectToDatabase(DatabaseType dbType)
    {
        if (dbType == DatabaseType.YAML)
        {
            Bukkit.getLogger().info("[Duels] Using YAML File storage for Statistics");
            createStatsConfig();
            saveStatsConfig();
            return new StatisticsDatabaseYAML(this);
        }
        if (dbType == DatabaseType.SQL)
        {
            Bukkit.getLogger().info("[Duels] Using SQL Database for Statistics");
            return new StatisticsDatabaseSQL(plugin, this);
        }
        throw new IllegalArgumentException("Unsupported statistics database type: " + dbType);
    }

    public StatisticsDatabase getStatsDB()
    {
        return statsDB;
    }

    public FileConfiguration getStatsConfig()
    {
        return statsFile;
    }

    public void saveStatsConfig()
    {
        try
        {
            statsFile.save(statsF);
        }
        catch (IOException exception)
        {
            exception.printStackTrace();
            Bukkit.getLogger().warning("[Duels] Failed to save statistics.yml");
        }
    }

    private void createStatsConfig()
    {
        statsF = PluginFiles.data(plugin, "statistics.yml", true);
        try
        {
            statsFile.load(statsF);
        }
        catch (IOException | InvalidConfigurationException exception)
        {
            exception.printStackTrace();
            Bukkit.getLogger().warning("[Duels] Failed to create statistics.yml");
        }
    }

}
