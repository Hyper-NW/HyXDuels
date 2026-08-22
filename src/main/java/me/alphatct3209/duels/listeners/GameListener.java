package me.alphatct3209.duels.listeners;

import me.alphatct3209.duels.Duels;
import me.alphatct3209.duels.game.GameState;
import me.alphatct3209.duels.game.arenas.Arena;
import me.alphatct3209.duels.game.arenas.ArenaSettings;
import me.alphatct3209.duels.game.modes.DuelMode;
import me.alphatct3209.duels.game.modes.ModeAction;
import me.alphatct3209.duels.stats.db.StatisticsDatabase;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityRegainHealthEvent;
import org.bukkit.event.entity.FoodLevelChangeEvent;

public class GameListener implements Listener
{
    private final Duels plugin;
    public GameListener(Duels plugin) { this.plugin = plugin; }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onDamage(EntityDamageEvent event)
    {
        if (!(event.getEntity() instanceof Player victim)) return;
        Arena arena = plugin.getArenaManager().getArena(victim);
        if (arena == null) return;
        if (arena.getGameState() != GameState.PLAYING) { event.setCancelled(true); return; }
        DuelMode mode = arena.getGame().getMode().orElse(null);
        if (mode == null) { event.setCancelled(true); return; }
        if (event.getCause() == EntityDamageEvent.DamageCause.FALL && !mode.combat().fallDamage())
        { event.setCancelled(true); return; }

        Player attacker = null;
        boolean ranged = false;
        if (event instanceof EntityDamageByEntityEvent byEntity)
        {
            if (event.getCause() == EntityDamageEvent.DamageCause.ENTITY_SWEEP_ATTACK
                    && arena.usesLegacyPvp())
            {
                event.setCancelled(true);
                return;
            }
            if (byEntity.getDamager() instanceof Player player) attacker = player;
            else if (byEntity.getDamager() instanceof Projectile projectile && projectile.getShooter() instanceof Player shooter)
            { attacker = shooter; ranged = true; }
            // Entity-caused damage has no authority unless it resolves to the exact opponent.
            if (attacker == null || !arena.areOpponents(victim.getUniqueId(), attacker.getUniqueId()))
            { event.setCancelled(true); return; }
            if (!mode.combat().pvp() || (ranged ? !mode.combat().projectiles() : !mode.combat().melee()))
            { event.setCancelled(true); return; }

            ModeAction action = ranged ? arena.getGame().rangedHit(attacker, victim)
                    : arena.getGame().meleeHit(attacker, victim);
            boolean healthDamage = mode.combat().healthDamage()
                    && arena.getGame().getController().map(controller -> controller.healthDamage()).orElse(false);
            if (!healthDamage)
            {
                event.setDamage(0D); // do not cancel: Bukkit still applies authorized knockback
                return;
            }
            if (action.terminal()) { event.setCancelled(true); return; }
        }

        if (victim.getHealth() - event.getFinalDamage() <= 0D)
        {
            if (attacker != null && arena.areOpponents(victim.getUniqueId(), attacker.getUniqueId()))
                arena.getGame().announceKill(attacker, victim,
                        victim.getHealth() - event.getFinalDamage());
            ModeAction action = arena.getGame().death(victim);
            event.setCancelled(true);
            StatisticsDatabase database = plugin.getStatisticsManager().getStatsDB();
            database.setDeaths(victim.getUniqueId(), increment(database.getDeaths(victim.getUniqueId())));
            if (attacker != null && arena.areOpponents(victim.getUniqueId(), attacker.getUniqueId()))
                database.recordKill(attacker.getUniqueId(), mode.key());
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onDuelHealing(EntityRegainHealthEvent event)
    {
        if (!(event.getEntity() instanceof Player player)) return;
        Arena arena = plugin.getArenaManager().getArena(player);
        if (arena != null && arena.getGameState() == GameState.PLAYING
                && !isAllowedDuelHealing(event.getRegainReason()))
            event.setCancelled(true);
    }

    static boolean isAllowedDuelHealing(EntityRegainHealthEvent.RegainReason reason)
    {
        return reason == EntityRegainHealthEvent.RegainReason.MAGIC
                || reason == EntityRegainHealthEvent.RegainReason.MAGIC_REGEN;
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onHunger(FoodLevelChangeEvent event)
    {
        if (!(event.getEntity() instanceof Player player)) return;
        Arena arena = plugin.getArenaManager().getArena(player);
        if (arena != null && arena.getGameState() == GameState.PLAYING
                && arena.getGame().getMode().map(mode -> !mode.combat().hunger()).orElse(false))
            event.setCancelled(true);
    }
    private int increment(int value) { return value == Integer.MAX_VALUE ? value : value + 1; }
}
