package me.alphatct3209.duels.social.gui;

import me.alphatct3209.duels.Duels;
import me.alphatct3209.duels.gui.config.MenuConfiguration;
import me.alphatct3209.duels.gui.item.MenuItemFactory;
import me.alphatct3209.duels.social.PlayerPreferences;
import me.alphatct3209.duels.social.PlayerSetting;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import java.util.EnumMap;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

public final class SettingsGui implements Listener
{
    private static final int SIZE = 27;
    private static final Map<PlayerSetting, Integer> DEFAULT_SLOTS = defaults();
    private final Duels plugin;
    private final MenuConfiguration configuration;
    private final MenuItemFactory items;
    private final Map<UUID, UUID> active = new HashMap<>();

    public SettingsGui(Duels plugin)
    {
        this.plugin = Objects.requireNonNull(plugin);
        configuration = new MenuConfiguration(plugin);
        items = new MenuItemFactory(plugin, configuration);
    }

    public void open(Player player)
    {
        if (!player.hasPermission("duels.settings")) return;
        UUID token = UUID.randomUUID();
        Map<Integer, PlayerSetting> choices = new LinkedHashMap<>();
        SettingsInventoryHolder holder = new SettingsInventoryHolder(token, player.getUniqueId(), choices);
        Inventory inventory = Bukkit.createInventory(holder, SIZE, items.color(configuration.text(
                "Menus.Settings.Title", "&8Player Settings")));
        holder.attach(inventory);
        PlayerPreferences preferences = plugin.getSocialManager().preferences(player.getUniqueId());
        for (PlayerSetting setting : PlayerSetting.values())
        {
            String path = "Menus.Settings.Items." + setting.key();
            int slot = configuration.slot(path + ".Slot", DEFAULT_SLOTS.get(setting), SIZE);
            choices.put(slot, setting);
            inventory.setItem(slot, icon(path, setting, preferences));
        }
        items.fillEmpty(inventory);
        active.put(player.getUniqueId(), token);
        player.openInventory(inventory);
    }

    public void shutdown()
    {
        for (Player player : Bukkit.getOnlinePlayers())
            if (player.getOpenInventory().getTopInventory().getHolder() instanceof SettingsInventoryHolder)
                player.closeInventory();
        active.clear();
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onClick(InventoryClickEvent event)
    {
        if (!(event.getView().getTopInventory().getHolder() instanceof SettingsInventoryHolder holder)) return;
        event.setCancelled(true);
        if (!(event.getWhoClicked() instanceof Player player) || !isActive(player, holder)) return;
        PlayerSetting setting = holder.choices().get(event.getRawSlot());
        if (setting != null)
        {
            plugin.getSocialManager().cycle(player.getUniqueId(), setting);
            open(player);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onDrag(InventoryDragEvent event)
    {
        if (event.getView().getTopInventory().getHolder() instanceof SettingsInventoryHolder)
            event.setCancelled(true);
    }

    @EventHandler
    public void onClose(InventoryCloseEvent event)
    {
        if (event.getInventory().getHolder() instanceof SettingsInventoryHolder holder)
            active.remove(event.getPlayer().getUniqueId(), holder.token());
    }

    private boolean isActive(Player player, SettingsInventoryHolder holder)
    {
        return holder.viewer().equals(player.getUniqueId())
                && holder.token().equals(active.get(player.getUniqueId()));
    }

    private ItemStack icon(String path, PlayerSetting setting, PlayerPreferences preferences)
    {
        Material fallback = fallbackMaterial(setting);
        Material material = Material.matchMaterial(configuration.text(path + ".Material", fallback.name()));
        if (material == null || material.isAir()) material = fallback;
        String value = preferences.display(setting);
        String name = replace(configuration.text(path + ".Name", "&e" + title(setting) + " &7(&f<value>&7)"), value);
        List<String> lore = configuration.lines(path + ".Lore").stream().map(line -> replace(line, value)).toList();
        boolean glow = Boolean.parseBoolean(configuration.text(path + ".Glow", "false"));
        return items.icon(material, name, lore, glow);
    }

    private String replace(String value, String settingValue) { return value.replace("<value>", settingValue); }

    private static String title(PlayerSetting setting)
    {
        String[] words = setting.name().toLowerCase(Locale.ROOT).split("_");
        StringBuilder result = new StringBuilder();
        for (String word : words)
        {
            if (!result.isEmpty()) result.append(' ');
            result.append(Character.toUpperCase(word.charAt(0))).append(word.substring(1));
        }
        return result.toString();
    }

    private static Material fallbackMaterial(PlayerSetting setting)
    {
        return switch (setting)
        {
            case SHOW_OWN_TIER -> Material.NETHER_STAR;
            case SCOREBOARD -> Material.PAPER;
            case PROFILE_KITS -> Material.CHEST;
            case FRIEND_JOIN_NOTIFIER -> Material.BELL;
            case BLAST_PARTICLES -> Material.FIREWORK_ROCKET;
            case DUEL_REQUESTS -> Material.DIAMOND_SWORD;
            case DIRECT_MESSAGES -> Material.WRITABLE_BOOK;
            case PARTY_INVITES -> Material.PLAYER_HEAD;
            case FRIEND_REQUESTS -> Material.NAME_TAG;
        };
    }

    private static Map<PlayerSetting, Integer> defaults()
    {
        Map<PlayerSetting, Integer> result = new EnumMap<>(PlayerSetting.class);
        result.put(PlayerSetting.SHOW_OWN_TIER, 9);
        result.put(PlayerSetting.SCOREBOARD, 10);
        result.put(PlayerSetting.PROFILE_KITS, 11);
        result.put(PlayerSetting.FRIEND_JOIN_NOTIFIER, 12);
        result.put(PlayerSetting.BLAST_PARTICLES, 13);
        result.put(PlayerSetting.DUEL_REQUESTS, 14);
        result.put(PlayerSetting.DIRECT_MESSAGES, 15);
        result.put(PlayerSetting.PARTY_INVITES, 16);
        result.put(PlayerSetting.FRIEND_REQUESTS, 17);
        return Map.copyOf(result);
    }
}
