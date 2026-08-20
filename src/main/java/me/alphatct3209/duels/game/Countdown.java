package me.alphatct3209.duels.game;

import me.alphatct3209.duels.Duels;
import me.alphatct3209.duels.game.arenas.Arena;
import org.bukkit.ChatColor;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.Objects;

public class Countdown extends BukkitRunnable
{

    private final Duels plugin;
    private final Arena arena;
    private int seconds;

    public Countdown(Duels plugin, Arena arena, int seconds)
    {
        this.plugin = plugin;
        this.arena = arena;
        if(seconds > 0)
        {
            this.seconds = seconds;
        }
        else
        {
            this.seconds = 15;
        }
    }

    private void sendSeconds()
    {
        arena.sendConfiguredToPlayers("Messages.Countdown",
                java.util.Map.of("<seconds>", seconds),
                "&aThe Duel begins in <seconds> second(s)!");
    }

    public void start()
    {
        if(seconds % 10 != 0)
            sendSeconds();
        runTaskTimer(plugin, 0L, 20L);
    }

    @Override
    public void run()
    {
        if(seconds == 0)
        {
            cancel();
            arena.getGame().start();
        }
        else
        {
            if(seconds <= 5 || seconds % 10 == 0)
            {
                sendSeconds();
            }
            seconds--;
        }
    }

    /** Current remaining whole seconds, exposed read-only for displays. */
    public int getSecondsRemaining()
    {
        return Math.max(0, seconds);
    }

}
