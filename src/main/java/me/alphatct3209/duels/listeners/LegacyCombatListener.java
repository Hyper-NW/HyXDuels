package me.alphatct3209.duels.listeners;

import me.alphatct3209.duels.Duels;
import me.alphatct3209.duels.game.GameState;
import me.alphatct3209.duels.game.arenas.Arena;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;

import java.util.Objects;

public final class LegacyCombatListener implements Listener
{
    private final Duels plugin;

    public LegacyCombatListener(Duels plugin)
    {
        this.plugin = Objects.requireNonNull(plugin, "plugin");
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onSweepAttack(EntityDamageByEntityEvent event)
    {
        if (event.getCause() != org.bukkit.event.entity.EntityDamageEvent.DamageCause.ENTITY_SWEEP_ATTACK
                || !(event.getDamager() instanceof Player attacker))
        {
            return;
        }
        Arena arena = plugin.getArenaManager().getArena(attacker);
        if (arena != null && arena.getGameState() == GameState.PLAYING
                && arena.usesLegacyPvp())
        {
            event.setCancelled(true);
        }
    }
}
