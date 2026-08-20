package me.alphatct3209.duels.stats.filter;

import me.alphatct3209.duels.Duels;
import me.alphatct3209.duels.game.modes.DuelMode;
import me.alphatct3209.duels.gui.config.MenuConfiguration;
import me.alphatct3209.duels.gui.item.MenuItemFactory;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/** Screenshot-inspired three-filter menu with explicit apply/discard semantics. */
public final class LeaderboardFilterGui implements Listener
{
    private static final int MODE_SLOT = 11;
    private static final int TIME_SLOT = 13;
    private static final int PLAYERS_SLOT = 15;
    private static final int APPLY_SLOT = 21;
    private static final int DISCARD_SLOT = 23;

    private final Duels plugin;
    private final LeaderboardFilterManager filters;
    private final FilteredLeaderboardService service;
    private final MenuItemFactory items;
    private final Map<UUID, LeaderboardFilter> drafts = new HashMap<>();

    public LeaderboardFilterGui(Duels plugin, LeaderboardFilterManager filters,
                                FilteredLeaderboardService service)
    {
        this.plugin = plugin;
        this.filters = filters;
        this.service = service;
        this.items = new MenuItemFactory(plugin, new MenuConfiguration(plugin));
    }

    public void open(Player player)
    {
        LeaderboardFilter draft = filters.get(player.getUniqueId());
        drafts.put(player.getUniqueId(), draft);
        render(player, draft);
    }

    public void shutdown()
    {
        drafts.clear();
    }

    private void render(Player player, LeaderboardFilter draft)
    {
        Inventory inventory = Bukkit.createInventory(new Holder(player.getUniqueId()), 27,
                color("&8Leaderboard Settings"));
        inventory.setItem(MODE_SLOT, option(Material.FISHING_ROD, "&aSelect the Mode!",
                modeLines(draft.mode())));
        inventory.setItem(TIME_SLOT, option(Material.CLOCK, "&eSelect the Time!",
                enumLines(List.of(LeaderboardTime.values()), draft.time())));
        inventory.setItem(PLAYERS_SLOT, option(Material.PLAYER_HEAD, "&bSelect the Players!",
                enumLines(List.of(LeaderboardScope.values()), draft.scope())));
        inventory.setItem(APPLY_SLOT, option(Material.LIME_DYE, "&aApply Changes",
                List.of("&7Save these filters across lobbies.", "&eClick to apply!")));
        inventory.setItem(DISCARD_SLOT, option(Material.RED_DYE, "&cDiscard Changes",
                List.of("&7Keep your previously saved filters.", "&eClick to discard!")));
        items.fillEmpty(inventory);
        player.openInventory(inventory);
    }

    private List<String> modeLines(String selected)
    {
        List<String> lines = new ArrayList<>();
        lines.add(mark(selected == null, "All Modes"));
        plugin.getModeManager().enabledModes().stream()
                .sorted(Comparator.comparing(DuelMode::displayName, String.CASE_INSENSITIVE_ORDER))
                .forEach(mode -> lines.add(mark(mode.key().value().equals(selected), mode.displayName())));
        lines.add("");
        lines.add("&7Left/Right click to change!");
        return lines;
    }

    private <E extends Enum<E>> List<String> enumLines(List<E> values, E selected)
    {
        List<String> lines = new ArrayList<>();
        for (E value : values)
        {
            String name = value instanceof LeaderboardTime time ? time.displayName()
                    : ((LeaderboardScope) value).displayName();
            lines.add(mark(value == selected, name));
        }
        lines.add("");
        lines.add("&7Left/Right click to change!");
        return lines;
    }

    private String mark(boolean selected, String value)
    {
        return selected ? "&a➜ &f" + value : "&7  " + value;
    }

    private ItemStack option(Material material, String name, List<String> lore)
    {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(color(name));
        meta.setLore(lore.stream().map(this::color).toList());
        item.setItemMeta(meta);
        return item;
    }

    @EventHandler
    public void onClick(InventoryClickEvent event)
    {
        if (!(event.getView().getTopInventory().getHolder() instanceof Holder holder)
                || !(event.getWhoClicked() instanceof Player player)
                || !holder.player().equals(player.getUniqueId())) return;
        event.setCancelled(true);
        if (event.getRawSlot() < 0 || event.getRawSlot() >= 27) return;
        LeaderboardFilter draft = drafts.getOrDefault(player.getUniqueId(), filters.get(player.getUniqueId()));
        int direction = event.isRightClick() ? -1 : 1;
        switch (event.getRawSlot())
        {
            case MODE_SLOT -> draft = new LeaderboardFilter(cycleMode(draft.mode(), direction),
                    draft.time(), draft.scope());
            case TIME_SLOT -> draft = new LeaderboardFilter(draft.mode(),
                    cycle(LeaderboardTime.values(), draft.time(), direction), draft.scope());
            case PLAYERS_SLOT -> draft = new LeaderboardFilter(draft.mode(), draft.time(),
                    cycle(LeaderboardScope.values(), draft.scope(), direction));
            case APPLY_SLOT -> {
                filters.set(player.getUniqueId(), draft);
                service.invalidate(player.getUniqueId());
                drafts.remove(player.getUniqueId());
                player.closeInventory();
                player.sendMessage(color("&aLeaderboard filters applied."));
                return;
            }
            case DISCARD_SLOT -> {
                drafts.remove(player.getUniqueId());
                player.closeInventory();
                return;
            }
            default -> { return; }
        }
        drafts.put(player.getUniqueId(), draft);
        render(player, draft);
    }

    @EventHandler
    public void onDrag(InventoryDragEvent event)
    {
        if (event.getView().getTopInventory().getHolder() instanceof Holder) event.setCancelled(true);
    }

    @EventHandler
    public void onClose(InventoryCloseEvent event)
    {
        if (event.getInventory().getHolder() instanceof Holder holder)
            Bukkit.getScheduler().runTask(plugin, () -> {
                if (!(event.getPlayer().getOpenInventory().getTopInventory().getHolder() instanceof Holder))
                    drafts.remove(holder.player());
            });
    }

    private String cycleMode(String current, int direction)
    {
        List<String> modes = new ArrayList<>();
        modes.add(null);
        plugin.getModeManager().enabledModes().stream()
                .sorted(Comparator.comparing(DuelMode::displayName, String.CASE_INSENSITIVE_ORDER))
                .map(mode -> mode.key().value()).forEach(modes::add);
        int index = modes.indexOf(current);
        if (index < 0) index = 0;
        return modes.get(Math.floorMod(index + direction, modes.size()));
    }

    private static <E> E cycle(E[] values, E current, int direction)
    {
        int index = 0;
        for (int candidate = 0; candidate < values.length; candidate++)
            if (values[candidate] == current) index = candidate;
        return values[Math.floorMod(index + direction, values.length)];
    }

    private String color(String value)
    {
        return ChatColor.translateAlternateColorCodes('&', value);
    }

    private record Holder(UUID player) implements InventoryHolder
    {
        @Override public Inventory getInventory() { return null; }
    }
}
