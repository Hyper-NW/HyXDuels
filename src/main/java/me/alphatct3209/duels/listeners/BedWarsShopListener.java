package me.alphatct3209.duels.listeners;

import me.alphatct3209.duels.Duels;
import me.alphatct3209.duels.game.GameState;
import me.alphatct3209.duels.game.arenas.Arena;
import me.alphatct3209.duels.game.modes.ModeHandlerType;
import me.alphatct3209.duels.game.modes.bedwars.BedWarsUpgrade;
import me.alphatct3209.duels.gui.config.MenuConfiguration;
import me.alphatct3209.duels.gui.item.MenuItemFactory;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/** Minimal server-owned quick shop that supplies the defining Bed Wars resource economy. */
public final class BedWarsShopListener implements Listener
{
    private static final Map<Integer, Offer> OFFERS = offers();
    private final Duels plugin;
    private final MenuItemFactory items;

    public BedWarsShopListener(Duels plugin)
    {
        this.plugin = plugin;
        this.items = new MenuItemFactory(plugin, new MenuConfiguration(plugin));
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onBlockInteract(PlayerInteractEvent event)
    {
        if (!event.getAction().isRightClick() || event.getClickedBlock() == null) return;
        Arena arena = bedWarsArena(event.getPlayer());
        if (arena != null && atOwnShop(arena, event.getPlayer(), event.getClickedBlock().getLocation()))
        {
            event.setCancelled(true);
            open(event.getPlayer(), arena);
        }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onEntityInteract(PlayerInteractEntityEvent event)
    {
        Arena arena = bedWarsArena(event.getPlayer());
        if (arena != null && atOwnShop(arena, event.getPlayer(), event.getRightClicked().getLocation()))
        {
            event.setCancelled(true);
            open(event.getPlayer(), arena);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onShopClick(InventoryClickEvent event)
    {
        if (!(event.getView().getTopInventory().getHolder() instanceof ShopHolder holder)) return;
        event.setCancelled(true);
        if (!(event.getWhoClicked() instanceof Player player)
                || !player.getUniqueId().equals(holder.player)) return;
        Arena arena = plugin.getArenaManager().getArena(player);
        if (arena == null || arena.getId() != holder.arenaId
                || !arena.getGame().isMode(ModeHandlerType.BED_WARS))
        {
            player.closeInventory();
            return;
        }
        Offer offer = OFFERS.get(event.getRawSlot());
        if (offer == null) return;
        if (offer.upgrade != null && !arena.getGame().canPurchaseBedWarsUpgrade(player, offer.upgrade))
        {
            me.alphatct3209.duels.utils.MessageService.send(player, plugin.getConfig(),
                    "Messages.Shop-Maxed", Map.of("<item>", offer.display),
                    "&cYou already own this upgrade or a better one.");
            return;
        }
        if (!take(player, offer.currency, offer.cost))
        {
            me.alphatct3209.duels.utils.MessageService.send(player, plugin.getConfig(),
                    "Messages.Shop-Insufficient", Map.of("<cost>", offer.cost,
                            "<currency>", readable(offer.currency), "<item>", offer.display),
                    "&cYou need <cost> <currency>!");
            return;
        }
        if (offer.upgrade != null)
        {
            arena.getGame().purchaseBedWarsUpgrade(player, offer.upgrade);
        }
        else
        {
            Material result = offer.result;
            if (result == Material.WHITE_WOOL)
                result = player.getUniqueId().equals(arena.getPlayerOne()) ? Material.RED_WOOL : Material.BLUE_WOOL;
            Map<Integer, ItemStack> overflow = player.getInventory().addItem(new ItemStack(result, offer.amount));
            overflow.values().forEach(item -> player.getWorld().dropItemNaturally(player.getLocation(), item));
        }
        me.alphatct3209.duels.utils.MessageService.send(player, plugin.getConfig(),
                "Messages.Shop-Purchased", Map.of("<item>", offer.display,
                        "<amount>", offer.amount, "<cost>", offer.cost,
                        "<currency>", readable(offer.currency)),
                "&aPurchased <item>.");
    }

    private Arena bedWarsArena(Player player)
    {
        Arena arena = plugin.getArenaManager().getArena(player);
        return arena != null && arena.getGameState() == GameState.PLAYING
                && arena.getGame().isMode(ModeHandlerType.BED_WARS) ? arena : null;
    }

    private boolean atOwnShop(Arena arena, Player player, Location location)
    {
        String point = player.getUniqueId().equals(arena.getPlayerOne()) ? "shop_1" : "shop_2";
        Location shop = arena.getPoint(point).orElse(null);
        return shop != null && shop.getWorld() == location.getWorld()
                && shop.distanceSquared(location) <= 9D;
    }

    private void open(Player player, Arena arena)
    {
        ShopHolder holder = new ShopHolder(arena.getId(), player.getUniqueId());
        Inventory inventory = Bukkit.createInventory(holder, 27, ChatColor.DARK_GRAY + "Bed Wars Quick Shop");
        holder.inventory = inventory;
        OFFERS.forEach((slot, offer) -> inventory.setItem(slot, icon(offer)));
        items.fillEmpty(inventory);
        player.openInventory(inventory);
    }

    private ItemStack icon(Offer offer)
    {
        ItemStack item = new ItemStack(offer.result, offer.amount);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(ChatColor.YELLOW + offer.display);
        meta.setLore(offer.upgrade == null
                ? List.of(ChatColor.GRAY + "Cost: " + ChatColor.WHITE + offer.cost + " "
                        + readable(offer.currency), ChatColor.GREEN + "Click to purchase")
                : List.of(ChatColor.GRAY + "Cost: " + ChatColor.WHITE + offer.cost + " "
                        + readable(offer.currency), ChatColor.AQUA + "Permanent upgrade",
                        ChatColor.GREEN + "Click to purchase"));
        item.setItemMeta(meta);
        return item;
    }

    static boolean take(Player player, Material currency, int cost)
    {
        int available = 0;
        for (ItemStack item : player.getInventory().getStorageContents())
            if (item != null && item.getType() == currency) available += item.getAmount();
        if (available < cost) return false;
        int remaining = cost;
        ItemStack[] storage = player.getInventory().getStorageContents();
        for (int slot = 0; slot < storage.length && remaining > 0; slot++)
        {
            ItemStack item = storage[slot];
            if (item == null || item.getType() != currency) continue;
            int used = Math.min(remaining, item.getAmount());
            remaining -= used;
            if (used == item.getAmount()) storage[slot] = null;
            else item.setAmount(item.getAmount() - used);
        }
        player.getInventory().setStorageContents(storage);
        return true;
    }

    private static Map<Integer, Offer> offers()
    {
        Map<Integer, Offer> offers = new LinkedHashMap<>();
        offers.put(0, new Offer("16 Wool", Material.WHITE_WOOL, 16, Material.IRON_INGOT, 4));
        offers.put(1, new Offer("Stone Sword", Material.STONE_SWORD, 1, Material.IRON_INGOT, 10));
        offers.put(2, new Offer("Iron Sword", Material.IRON_SWORD, 1, Material.GOLD_INGOT, 7));
        offers.put(3, upgrade("Wooden Pickaxe", Material.WOODEN_PICKAXE,
                Material.IRON_INGOT, 10, BedWarsUpgrade.WOODEN_PICKAXE));
        offers.put(4, upgrade("Iron Pickaxe", Material.IRON_PICKAXE,
                Material.GOLD_INGOT, 3, BedWarsUpgrade.IRON_PICKAXE));
        offers.put(5, upgrade("Diamond Pickaxe", Material.DIAMOND_PICKAXE,
                Material.GOLD_INGOT, 6, BedWarsUpgrade.DIAMOND_PICKAXE));
        offers.put(6, upgrade("Permanent Shears", Material.SHEARS,
                Material.IRON_INGOT, 20, BedWarsUpgrade.SHEARS));
        offers.put(7, new Offer("Diamond Sword", Material.DIAMOND_SWORD, 1, Material.EMERALD, 4));
        offers.put(9, upgrade("Permanent Chainmail Armor", Material.CHAINMAIL_LEGGINGS,
                Material.IRON_INGOT, 40, BedWarsUpgrade.CHAINMAIL_ARMOR));
        offers.put(10, upgrade("Permanent Iron Armor", Material.IRON_LEGGINGS,
                Material.GOLD_INGOT, 12, BedWarsUpgrade.IRON_ARMOR));
        offers.put(11, upgrade("Permanent Diamond Armor", Material.DIAMOND_LEGGINGS,
                Material.EMERALD, 6, BedWarsUpgrade.DIAMOND_ARMOR));
        offers.put(12, upgrade("Wooden Axe", Material.WOODEN_AXE,
                Material.IRON_INGOT, 10, BedWarsUpgrade.WOODEN_AXE));
        offers.put(13, upgrade("Iron Axe", Material.IRON_AXE,
                Material.GOLD_INGOT, 3, BedWarsUpgrade.IRON_AXE));
        offers.put(14, upgrade("Diamond Axe", Material.DIAMOND_AXE,
                Material.GOLD_INGOT, 6, BedWarsUpgrade.DIAMOND_AXE));
        offers.put(15, new Offer("Bow", Material.BOW, 1, Material.GOLD_INGOT, 12));
        offers.put(16, new Offer("8 Arrows", Material.ARROW, 8, Material.GOLD_INGOT, 2));
        offers.put(17, new Offer("Golden Apple", Material.GOLDEN_APPLE, 1, Material.GOLD_INGOT, 3));
        offers.put(18, new Offer("TNT", Material.TNT, 1, Material.GOLD_INGOT, 4));
        offers.put(19, new Offer("Water Bucket", Material.WATER_BUCKET, 1, Material.GOLD_INGOT, 3));
        offers.put(20, new Offer("Ender Pearl", Material.ENDER_PEARL, 1, Material.EMERALD, 4));
        offers.put(21, new Offer("4 Obsidian", Material.OBSIDIAN, 4, Material.EMERALD, 4));
        return Map.copyOf(offers);
    }

    private static Offer upgrade(String display, Material icon, Material currency,
                                 int cost, BedWarsUpgrade upgrade)
    {
        return new Offer(display, icon, 1, currency, cost, upgrade);
    }

    private static String readable(Material material)
    { return material.name().toLowerCase().replace('_', ' '); }

    private record Offer(String display, Material result, int amount, Material currency,
                         int cost, BedWarsUpgrade upgrade)
    {
        private Offer(String display, Material result, int amount, Material currency, int cost)
        { this(display, result, amount, currency, cost, null); }
    }

    private static final class ShopHolder implements InventoryHolder
    {
        private final int arenaId;
        private final UUID player;
        private Inventory inventory;
        private ShopHolder(int arenaId, UUID player) { this.arenaId = arenaId; this.player = player; }
        @Override public Inventory getInventory() { return inventory; }
    }
}
