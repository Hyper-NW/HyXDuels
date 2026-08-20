package me.alphatct3209.duels.listeners;

import me.alphatct3209.duels.Duels;
import me.alphatct3209.duels.game.GameState;
import me.alphatct3209.duels.game.arenas.Arena;
import me.alphatct3209.duels.game.arenas.ArenaSettings;
import me.alphatct3209.duels.game.modes.ModeController;
import me.alphatct3209.duels.game.modes.ModeHandlerType;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockBurnEvent;
import org.bukkit.event.block.BlockExplodeEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.block.BlockSpreadEvent;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.bukkit.event.entity.EntityPickupItemEvent;
import org.bukkit.event.entity.EntityPlaceEvent;
import org.bukkit.event.player.PlayerDropItemEvent;

import java.util.List;

public class ArenaGameplayListener implements Listener
{
    private final Duels plugin;

    public ArenaGameplayListener(Duels plugin)
    {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onBreak(BlockBreakEvent event)
    {
        if (pending(event.getBlock().getWorld()))
        {
            event.setCancelled(true);
            return;
        }
        Arena arena = plugin.getArenaManager().getArena(event.getPlayer());
        if (arena == null)
        {
            return;
        }
        if (!arena.containsObjectiveRegion(event.getBlock().getLocation()))
        {
            event.setCancelled(true);
            return;
        }
        boolean bedWars = arena.getGameState() == GameState.PLAYING
                && arena.getGame().isMode(ModeHandlerType.BED_WARS);
        if (bedWars && org.bukkit.Tag.BEDS.isTagged(event.getBlock().getType()))
        {
            if (arena.getGame().breakBed(event.getPlayer(), event.getBlock().getLocation()))
            {
                arena.recordOriginal(event.getBlock());
                return;
            }
            event.setCancelled(true);
            return;
        }
        if (bedWars && !arena.isRuntimePlacedBlock(event.getBlock()))
        {
            event.setCancelled(true);
            return;
        }
        boolean spleef = arena.getGame().getController()
                .map(controller -> controller.essentialItems().contains(
                        ModeController.EssentialItem.SPLEEF_SHOVEL))
                .orElse(false);
        if (spleef)
        {
            Material held = event.getPlayer().getInventory().getItemInMainHand().getType();
            Material floor = event.getBlock().getType();
            if ((held == Material.DIAMOND_SHOVEL || held == Material.IRON_SHOVEL
                    || held == Material.STONE_SHOVEL || held == Material.WOODEN_SHOVEL
                    || held == Material.GOLDEN_SHOVEL || held == Material.NETHERITE_SHOVEL)
                    && (floor == Material.SNOW_BLOCK || floor == Material.SNOW))
            {
                arena.recordOriginal(event.getBlock());
                event.setDropItems(false);
                int current = 0;
                for (org.bukkit.inventory.ItemStack stack : event.getPlayer().getInventory().getContents())
                    if (stack != null && stack.getType() == Material.SNOWBALL) current += stack.getAmount();
                if (current < 16) event.getPlayer().getInventory().addItem(
                        new org.bukkit.inventory.ItemStack(Material.SNOWBALL, 1));
                return;
            }
            event.setCancelled(true);
            return;
        }
        if (!playingAndAllowed(arena, ArenaSettings.Flag.BLOCK_BREAK) || !modeAllowsBlockDamage(arena))
        {
            event.setCancelled(true);
            return;
        }
        arena.recordOriginal(event.getBlock());
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onPlace(BlockPlaceEvent event)
    {
        if (pending(event.getBlock().getWorld()))
        {
            event.setCancelled(true);
            return;
        }
        Arena arena = plugin.getArenaManager().getArena(event.getPlayer());
        if (arena == null)
        {
            return;
        }
        if (!arena.containsObjectiveRegion(event.getBlock().getLocation()))
        {
            event.setCancelled(true);
            return;
        }
        if (!playingAndAllowed(arena, ArenaSettings.Flag.BLOCK_PLACE) || !modeAllowsBlockDamage(arena))
        {
            event.setCancelled(true);
            return;
        }
        arena.recordOriginal(event.getBlock(), event.getBlockReplacedState().getBlockData());
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onEntityPlace(EntityPlaceEvent event)
    {
        if (pending(event.getBlock().getWorld()))
        {
            event.setCancelled(true);
            return;
        }
        Player player = event.getPlayer();
        Arena arena;
        if (player == null)
        {
            List<Arena> active = plugin.getArenaManager().getActiveArenas(event.getBlock().getWorld());
            if (active.isEmpty())
            {
                return;
            }
            if (active.size() != 1)
            {
                event.setCancelled(true);
                return;
            }
            arena = active.getFirst();
        }
        else
        {
            arena = plugin.getArenaManager().getArena(player);
        }
        if (arena != null && (!arena.containsObjectiveRegion(event.getBlock().getLocation())
                || !playingAndAllowed(arena, ArenaSettings.Flag.ENTITY_PLACEMENT)))
        {
            event.setCancelled(true);
            return;
        }
        if (arena != null)
        {
            arena.trackTransient(event.getEntity());
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onEntityExplosion(EntityExplodeEvent event)
    {
        handleExplosion(event.getLocation().getWorld(), event.blockList(), event::setCancelled);
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onBlockExplosion(BlockExplodeEvent event)
    {
        handleExplosion(event.getBlock().getWorld(), event.blockList(), event::setCancelled);
    }

    private void handleExplosion(World world, List<Block> blocks, Cancel action)
    {
        if (pending(world))
        {
            blocks.clear();
            action.cancel(true);
            return;
        }
        List<Arena> matches = plugin.getArenaManager().getActiveArenas(world);
        if (matches.size() > 1)
        {
            // Never mutate shared terrain when the owning active arena is ambiguous.
            // Clearing blocks preserves entity damage for entity explosions.
            blocks.clear();
            return;
        }
        if (matches.isEmpty())
        {
            return;
        }
        Arena arena = matches.get(0);
        if (!arena.getSettings().get(ArenaSettings.Flag.EXPLOSIONS))
        {
            action.cancel(true);
            return;
        }
        boolean bedWars = arena.getGame().isMode(ModeHandlerType.BED_WARS);
        if (!arena.getSettings().get(ArenaSettings.Flag.EXPLOSION_BLOCK_DAMAGE) && !bedWars)
        {
            blocks.clear();
            return;
        }
        if (bedWars)
            blocks.removeIf(block -> org.bukkit.Tag.BEDS.isTagged(block.getType())
                    || !arena.isRuntimePlacedBlock(block));
        // Record every original before Bukkit damages any block.
        blocks.forEach(arena::recordOriginal);
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onFireSpread(BlockSpreadEvent event)
    {
        if (event.getSource().getType() != Material.FIRE && event.getNewState().getType() != Material.FIRE)
        {
            return;
        }
        if (pending(event.getBlock().getWorld()))
        {
            event.setCancelled(true);
            return;
        }
        Arena arena = fireArena(event.getBlock().getWorld());
        if (arena == null || !playingAndAllowed(arena, ArenaSettings.Flag.FIRE_SPREAD))
        {
            if (hasActiveArena(event.getBlock().getWorld()))
            {
                event.setCancelled(true);
            }
            return;
        }
        arena.recordOriginal(event.getBlock());
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onBurn(BlockBurnEvent event)
    {
        if (pending(event.getBlock().getWorld()))
        {
            event.setCancelled(true);
            return;
        }
        Arena arena = fireArena(event.getBlock().getWorld());
        if (arena == null || !playingAndAllowed(arena, ArenaSettings.Flag.FIRE_SPREAD))
        {
            if (hasActiveArena(event.getBlock().getWorld()))
            {
                event.setCancelled(true);
            }
            return;
        }
        arena.recordOriginal(event.getBlock());
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onDrop(PlayerDropItemEvent event)
    {
        Arena arena = plugin.getArenaManager().getArena(event.getPlayer());
        if (pending(event.getPlayer().getWorld())
                || (arena != null && !playingAndAllowed(arena, ArenaSettings.Flag.ITEM_DROP)))
        {
            event.setCancelled(true);
        }
        else if (arena != null)
        {
            event.getItemDrop().getPersistentDataContainer().set(
                    new org.bukkit.NamespacedKey("hyxduels", "arena_id"),
                    org.bukkit.persistence.PersistentDataType.INTEGER, arena.getId());
            arena.trackTransient(event.getItemDrop());
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onPickup(EntityPickupItemEvent event)
    {
        if (!(event.getEntity() instanceof Player player))
        {
            return;
        }
        Arena arena = plugin.getArenaManager().getArena(player);
        Integer ownerId = event.getItem().getPersistentDataContainer().get(
                new org.bukkit.NamespacedKey("hyxduels", "arena_id"),
                org.bukkit.persistence.PersistentDataType.INTEGER);
        if (pending(player.getWorld())
                || (ownerId != null && (arena == null || arena.getId() != ownerId))
                || (arena != null && !playingAndAllowed(arena, ArenaSettings.Flag.ITEM_PICKUP)))
        {
            event.setCancelled(true);
        }
    }

    private boolean modeAllowsBlockDamage(Arena arena)
    {
        return arena.getGame().getMode().map(mode -> mode.combat().blockDamage()).orElse(false);
    }

    private boolean playingAndAllowed(Arena arena, ArenaSettings.Flag flag)
    {
        if (arena.getGameState() != GameState.PLAYING
                || plugin.getSlimeWorldManager().isPending(arena.getMapWorldName())) return false;
        boolean constructionMode = arena.getGame().isMode(
                ModeHandlerType.BED_WARS)
                || arena.getGame().isMode(ModeHandlerType.SKY_WARS);
        boolean requiredByMode = constructionMode
                && (flag == ArenaSettings.Flag.BLOCK_BREAK || flag == ArenaSettings.Flag.BLOCK_PLACE
                || flag == ArenaSettings.Flag.ITEM_PICKUP);
        return requiredByMode || arena.getSettings().get(flag);
    }

    private Arena unique(World world)
    {
        return plugin.getArenaManager().getUniqueActiveArena(world).orElse(null);
    }

    private Arena fireArena(World world)
    {
        return unique(world);
    }

    private boolean hasActiveArena(World world)
    {
        return pending(world) || !plugin.getArenaManager().getActiveArenas(world).isEmpty();
    }

    private boolean pending(World world)
    {
        return world != null && plugin.getSlimeWorldManager().isPending(world.getName());
    }

    @FunctionalInterface
    private interface Cancel
    {
        void cancel(boolean cancelled);
    }
}
