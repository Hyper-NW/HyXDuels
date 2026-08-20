package me.alphatct3209.duels.listeners;

import me.alphatct3209.duels.Duels;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;

import java.util.UUID;

public class StatsListener implements Listener
{

    private final Duels plugin;

    public StatsListener(Duels plugin)
    {
        this.plugin = plugin;
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event)
    {
        UUID uuid = event.getPlayer().getUniqueId();
        String name = event.getPlayer().getName();
        var database = plugin.getStatisticsManager().getStatsDB();
        if (!database.isRegistered(uuid))
        {
            database.registerNewPlayer(uuid, name);
        }
        else
        {
            database.updateLastKnownName(uuid, name);
        }
        plugin.requestLeaderboardRefresh();
    }

}
