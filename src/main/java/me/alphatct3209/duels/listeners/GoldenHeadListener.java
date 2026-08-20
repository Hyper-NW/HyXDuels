package me.alphatct3209.duels.listeners;

import me.alphatct3209.duels.Duels;
import me.alphatct3209.duels.game.GameState;
import me.alphatct3209.duels.game.arenas.Arena;
import me.alphatct3209.duels.game.items.GoldenHead;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public final class GoldenHeadListener implements Listener
{
    private static final long USE_DEBOUNCE_MILLIS = 250L;
    private final Duels plugin;
    private final Map<UUID, Long> lastUse = new HashMap<>();

    public GoldenHeadListener(Duels plugin) { this.plugin = plugin; }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onInteract(PlayerInteractEvent event)
    {
        if (!event.getAction().isRightClick() || !GoldenHead.isGoldenHead(event.getItem())) return;
        event.setCancelled(true);
        Player player = event.getPlayer();
        Arena arena = plugin.getArenaManager().getArena(player);
        boolean uhc = arena != null && arena.getGameState() == GameState.PLAYING
                && arena.getGame().getMode().map(mode -> mode.key().value().equals("uhc")).orElse(false);
        if (!uhc) return;

        long now = System.currentTimeMillis();
        long previous = lastUse.getOrDefault(player.getUniqueId(), Long.MIN_VALUE / 2);
        if (now - previous < USE_DEBOUNCE_MILLIS) return;
        lastUse.put(player.getUniqueId(), now);

        EquipmentSlot hand = event.getHand();
        if (hand == null) return;
        ItemStack held = hand == EquipmentSlot.HAND
                ? player.getInventory().getItemInMainHand()
                : player.getInventory().getItemInOffHand();
        if (!GoldenHead.isGoldenHead(held)) return;
        if (held.getAmount() <= 1) held = new ItemStack(Material.AIR);
        else held.setAmount(held.getAmount() - 1);
        if (hand == EquipmentSlot.HAND) player.getInventory().setItemInMainHand(held);
        else player.getInventory().setItemInOffHand(held);

        GoldenHead.applyEffects(player);
        player.getWorld().playSound(player.getLocation(), Sound.ENTITY_GENERIC_EAT, 1F, 1F);
    }
}
