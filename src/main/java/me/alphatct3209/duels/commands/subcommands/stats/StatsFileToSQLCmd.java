package me.alphatct3209.duels.commands.subcommands.stats;

import me.alphatct3209.duels.Duels;
import me.alphatct3209.duels.commands.subcommands.DuelsSubCommand;
import me.alphatct3209.duels.configuration.PluginFiles;
import me.alphatct3209.duels.stats.db.StatisticsDatabase;
import me.alphatct3209.duels.stats.db.impl.StatisticsDatabaseSQL;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.UUID;

public class StatsFileToSQLCmd extends DuelsSubCommand
{
    public StatsFileToSQLCmd(Duels plugin)
    {
        super(plugin, "filetosql");
    }

    @Override
    public boolean execute(CommandSender sender, String[] args)
    {
        if (!sender.hasPermission("duels.admin"))
        {
            noPerm(sender);
            return true;
        }
        if (!(plugin.getStatisticsManager().getStatsDB() instanceof StatisticsDatabaseSQL))
        {
            sender.sendMessage(ChatColor.RED + "You have not configured Duels to use a SQL database.");
            return true;
        }

        File source = PluginFiles.data(plugin, "statistics.yml", true);
        if (!source.isFile())
        {
            sender.sendMessage(ChatColor.RED + "statistics.yml was not found.");
            return true;
        }

        FileConfiguration config = new YamlConfiguration();
        try
        {
            config.load(source);
        }
        catch (IOException | InvalidConfigurationException exception)
        {
            throw new IllegalStateException("Could not load statistics.yml", exception);
        }

        ConfigurationSection players = config.getConfigurationSection("Statistics");
        if (players == null)
        {
            sender.sendMessage(ChatColor.GREEN + "No file statistics were found to transfer.");
            return true;
        }

        sender.sendMessage(ChatColor.GREEN + "Loading statistics.yml into the SQL database...");
        StatisticsDatabase database = plugin.getStatisticsManager().getStatsDB();
        for (String uuidString : players.getKeys(false))
        {
            UUID uuid = UUID.fromString(uuidString);
            String path = "Statistics." + uuid + ".";
            if (!database.isRegistered(uuid))
            {
                database.registerNewPlayer(uuid, config.getString(path + "Last-Known-Name", "Unknown"));
            }
            database.setWins(uuid, config.getInt(path + "Wins"));
            database.setLosses(uuid, config.getInt(path + "Losses"));
            database.setKills(uuid, config.getInt(path + "Kills"));
            database.setDeaths(uuid, config.getInt(path + "Deaths"));
            database.setWinStreak(uuid, config.getInt(path + "WinStreak"));
            database.setHighestWinStreak(uuid, config.getInt(path + "HighestWinStreak"));

            ConfigurationSection gamemodes = config.getConfigurationSection(path + "Gamemodes");
            if (gamemodes != null)
            {
                for (String gamemode : gamemodes.getKeys(false))
                {
                    database.setGamemodeWins(uuid, gamemode,
                            gamemodes.getInt(gamemode + ".Wins", 0));
                }
            }
            // Aggregate wins remain aggregate; retired fields are intentionally ignored.
        }
        plugin.requestLeaderboardRefresh();
        sender.sendMessage(ChatColor.GREEN + "Statistics loading complete!");

        return true;
    }
}
