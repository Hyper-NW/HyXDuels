package me.alphatct3209.duels.listeners;

import me.alphatct3209.duels.Duels;
import me.alphatct3209.duels.game.Game;
import me.alphatct3209.duels.game.GameState;
import me.alphatct3209.duels.game.arenas.Arena;
import me.alphatct3209.duels.game.modes.ModeController;
import org.bukkit.FluidCollisionMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.block.Block;
import org.bukkit.entity.AreaEffectCloud;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.entity.Snowball;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.AreaEffectCloudApplyEvent;
import org.bukkit.event.entity.ProjectileHitEvent;
import org.bukkit.event.entity.ProjectileLaunchEvent;
import org.bukkit.event.entity.PotionSplashEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.util.RayTraceResult;
import org.bukkit.util.Vector;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public final class ModeMechanicsListener implements Listener
{
    private static final NamespacedKey ITEM_KEY = new NamespacedKey("hyxduels", Game.OBJECTIVE_KEY);
    private static final NamespacedKey ARENA_KEY = new NamespacedKey("hyxduels", "arena_id");
    private final Duels plugin;
    private final Map<UUID, Long> railgun = new HashMap<>();
    private final Map<UUID, Long> dash = new HashMap<>();
    private final Map<UUID, Long> spleef = new HashMap<>();
    public ModeMechanicsListener(Duels plugin) { this.plugin = plugin; }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onMove(PlayerMoveEvent event)
    {
        if (!event.hasChangedPosition()) return;
        Arena arena = plugin.getArenaManager().getArena(event.getPlayer());
        if (arena != null && arena.getGameState() == GameState.PLAYING)
            arena.getGame().movement(event.getPlayer(), event.getTo());
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onInteract(PlayerInteractEvent event)
    {
        if (!event.getAction().isRightClick()) return;
        Player player = event.getPlayer();
        Arena arena = plugin.getArenaManager().getArena(player);
        if (arena == null || arena.getGameState() != GameState.PLAYING) return;
        String role = role(event.getItem());
        if (role == null) return;
        long now = System.currentTimeMillis();
        switch (role)
        {
            case "RAILGUN" -> {
                event.setCancelled(true);
                if (!ready(railgun, player.getUniqueId(), now, arena.getObjectiveSettings().railgunCooldownMillis())) return;
                double range = arena.getObjectiveSettings().railgunRange();
                Location eye = player.getEyeLocation();
                Vector direction = eye.getDirection();
                RayTraceResult blocks = player.getWorld().rayTraceBlocks(eye, direction, range,
                        FluidCollisionMode.NEVER, true);
                double blockDistance = blocks == null || blocks.getHitPosition() == null ? range + 1D
                        : blocks.getHitPosition().distance(eye.toVector());
                RayTraceResult entities = player.getWorld().rayTraceEntities(eye, direction, range, 0.35D,
                        entity -> entity instanceof Player target
                                && arena.areOpponents(target.getUniqueId(), player.getUniqueId()));
                if (entities != null && entities.getHitEntity() instanceof Player target
                        && entities.getHitPosition().distance(eye.toVector()) < blockDistance)
                    arena.getGame().rangedHit(player, target);
            }
            case "DASH" -> {
                event.setCancelled(true);
                if (!ready(dash, player.getUniqueId(), now, arena.getObjectiveSettings().dashCooldownMillis())) return;
                player.setVelocity(player.getLocation().getDirection().normalize().multiply(1.25D).setY(0.35D));
            }
            case "COMPASS" -> {
                UUID other = arena.getPlayerOne().equals(player.getUniqueId()) ? arena.getPlayerTwo() : arena.getPlayerOne();
                Player target = plugin.getServer().getPlayer(other);
                if (target != null) player.setCompassTarget(target.getLocation());
            }
            default -> { }
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onLaunch(ProjectileLaunchEvent event)
    {
        if (!(event.getEntity().getShooter() instanceof Player shooter)) return;
        Arena arena = plugin.getArenaManager().getArena(shooter);
        if (arena == null || arena.getGameState() != GameState.PLAYING) return;
        event.getEntity().getPersistentDataContainer().set(ARENA_KEY, PersistentDataType.INTEGER, arena.getId());
        arena.trackTransient(event.getEntity());
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onProjectileHit(ProjectileHitEvent event)
    {
        if (!(event.getEntity() instanceof Snowball snowball)
                || !(snowball.getShooter() instanceof Player shooter) || event.getHitBlock() == null) return;
        Arena arena = plugin.getArenaManager().getArena(shooter);
        if (arena == null || arena.getGameState() != GameState.PLAYING) return;
        boolean isSpleef = arena.getGame().getController().map(controller -> controller.essentialItems()
                .contains(ModeController.EssentialItem.SPLEEF_SHOVEL)).orElse(false);
        Block block = event.getHitBlock();
        if (!isSpleef || !arena.containsObjectiveRegion(block.getLocation())
                || !ready(spleef, shooter.getUniqueId(), System.currentTimeMillis(),
                arena.getObjectiveSettings().spleefProjectileCooldownMillis())) return;
        if (block.getType() == Material.SNOW_BLOCK || block.getType() == Material.SNOW)
        {
            arena.recordOriginal(block);
            block.setType(Material.AIR, false);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onPotion(PotionSplashEvent event)
    {
        Arena owner = projectileArena(event.getPotion());
        for (org.bukkit.entity.LivingEntity affected : event.getAffectedEntities())
            if (affected instanceof Player player && (owner == null || !owner.isParticipant(player.getUniqueId())))
                event.setIntensity(affected, 0D);
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onCloud(AreaEffectCloudApplyEvent event)
    {
        AreaEffectCloud cloud = event.getEntity();
        Integer id = cloud.getPersistentDataContainer().get(ARENA_KEY, PersistentDataType.INTEGER);
        Arena owner = id == null ? null : plugin.getArenaManager().getArena(id);
        event.getAffectedEntities().removeIf(entity -> entity instanceof Player player
                && (owner == null || !owner.isParticipant(player.getUniqueId())));
    }

    private Arena projectileArena(Projectile projectile)
    {
        Integer id = projectile.getPersistentDataContainer().get(ARENA_KEY, PersistentDataType.INTEGER);
        return id == null ? null : plugin.getArenaManager().getArena(id);
    }
    private String role(ItemStack item)
    {
        if (item == null || !item.hasItemMeta()) return null;
        return item.getItemMeta().getPersistentDataContainer().get(ITEM_KEY, PersistentDataType.STRING);
    }
    private boolean ready(Map<UUID, Long> cooldowns, UUID player, long now, long delay)
    {
        long prior = cooldowns.getOrDefault(player, Long.MIN_VALUE / 2);
        if (now - prior < delay) return false;
        cooldowns.put(player, now); return true;
    }
}
