package me.alphatct3209.duels.party.gui;

import me.alphatct3209.duels.Duels;
import me.alphatct3209.duels.gui.config.MenuConfiguration;
import me.alphatct3209.duels.gui.item.MenuItemFactory;
import me.alphatct3209.duels.gui.layout.PagedMenuLayout;
import me.alphatct3209.duels.party.Party;
import me.alphatct3209.duels.utils.MessageService;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.SkullMeta;

import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

public final class PartyGui implements Listener
{
    private final Duels plugin;
    private final MenuConfiguration configuration;
    private final MenuItemFactory items;
    private final Map<UUID, UUID> active = new HashMap<>();

    public PartyGui(Duels plugin)
    {
        this.plugin = Objects.requireNonNull(plugin);
        configuration = new MenuConfiguration(plugin);
        items = new MenuItemFactory(plugin, configuration);
    }

    public void open(Player leader)
    {
        Party party = leaderParty(leader);
        if (party == null) return;
        int size = 27;
        Map<Integer, String> choices = new LinkedHashMap<>();
        UUID token = UUID.randomUUID();
        PartyInventoryHolder holder = new PartyInventoryHolder(token, leader.getUniqueId(),
                PartyInventoryHolder.View.MAIN, 0, 1, choices);
        Inventory inventory = Bukkit.createInventory(holder, size,
                items.color(replace(configuration.text("Menus.Party.Title", "&8Party Management"), party)));
        holder.attach(inventory);
        addAction(inventory, choices, size, "Party-FFA", "ffa", 10, Material.TNT,
                "&cParty FFA", List.of("&7Start the configured free-for-all action."), party);
        addAction(inventory, choices, size, "Team-Battle", "team", 12, Material.RED_BANNER,
                "&cRed &7vs &9Blue", List.of("&7Start the configured team battle action."), party);
        addAction(inventory, choices, size, "Visibility", "visibility", 14,
                party.visible() ? Material.ENDER_EYE : Material.ENDER_PEARL,
                "&eParty Visibility: <visibility>", List.of("&7Click to toggle."), party);
        addAction(inventory, choices, size, "Host-Duel", "host", 16, Material.DIAMOND_SWORD,
                "&bHost Duel", List.of("&7Open the duel mode menu."), party);
        addAction(inventory, choices, size, "Invite-Friends", "invite", 22, Material.PLAYER_HEAD,
                "&aInvite Friends", List.of("&7Browse online players."), party);
        items.fillEmpty(inventory);
        active.put(leader.getUniqueId(), token);
        leader.openInventory(inventory);
    }

    private void openInvites(Player leader, int requestedPage)
    {
        Party party = leaderParty(leader);
        if (party == null) return;
        List<Player> candidates = Bukkit.getOnlinePlayers().stream()
                .map(Player.class::cast)
                .filter(player -> !player.equals(leader))
                .filter(player -> !plugin.getPartyManager().isInParty(player.getUniqueId()))
                .filter(leader::canSee)
                .sorted(Comparator.comparing(Player::getName, String.CASE_INSENSITIVE_ORDER)).toList();
        PagedMenuLayout.Page<Player> page = PagedMenuLayout.page(candidates, requestedPage);
        Map<Integer, String> choices = new LinkedHashMap<>();
        UUID token = UUID.randomUUID();
        PartyInventoryHolder holder = new PartyInventoryHolder(token, leader.getUniqueId(),
                PartyInventoryHolder.View.INVITE, page.index(), page.count(), choices);
        String title = configuration.text("Menus.Party.Invite.Title", "&8Invite Friends &7(<page>/<pages>)")
                .replace("<page>", Integer.toString(page.index() + 1))
                .replace("<pages>", Integer.toString(page.count()));
        Inventory inventory = Bukkit.createInventory(holder, PagedMenuLayout.INVENTORY_SIZE, items.color(title));
        holder.attach(inventory);
        page.slots().forEach((slot, player) -> {
            choices.put(slot, player.getUniqueId().toString());
            ItemStack icon = configuredItem("Menus.Party.Invite.Player", Material.PLAYER_HEAD,
                    "&a<player>", List.of("&eClick to invite."), Map.of("<player>", player.getName()), false);
            if (icon.getItemMeta() instanceof SkullMeta skull)
            {
                skull.setOwningPlayer(player);
                icon.setItemMeta(skull);
            }
            inventory.setItem(slot, icon);
        });
        if (page.hasPrevious()) inventory.setItem(PagedMenuLayout.PREVIOUS_SLOT,
                items.icon(Material.ARROW, configuration.text("Menus.Common.Previous", "&ePrevious page"), List.of(), false));
        inventory.setItem(PagedMenuLayout.BACK_SLOT,
                items.icon(Material.BARRIER, configuration.text("Menus.Common.Back", "&cBack"), List.of(), false));
        if (page.hasNext()) inventory.setItem(PagedMenuLayout.NEXT_SLOT,
                items.icon(Material.ARROW, configuration.text("Menus.Common.Next", "&eNext page"), List.of(), false));
        items.fillEmpty(inventory);
        active.put(leader.getUniqueId(), token);
        leader.openInventory(inventory);
    }

    @EventHandler
    public void onClick(InventoryClickEvent event)
    {
        if (!(event.getView().getTopInventory().getHolder() instanceof PartyInventoryHolder holder)) return;
        event.setCancelled(true);
        if (!(event.getWhoClicked() instanceof Player player)
                || !holder.viewer().equals(player.getUniqueId())
                || !holder.token().equals(active.get(player.getUniqueId()))) return;
        int slot = event.getRawSlot();
        if (holder.view() == PartyInventoryHolder.View.INVITE)
        {
            if (slot == PagedMenuLayout.PREVIOUS_SLOT && holder.page() > 0) openInvites(player, holder.page() - 1);
            else if (slot == PagedMenuLayout.NEXT_SLOT && holder.page() + 1 < holder.pages()) openInvites(player, holder.page() + 1);
            else if (slot == PagedMenuLayout.BACK_SLOT) open(player);
            else invite(player, holder.choices().get(slot));
            return;
        }
        String action = holder.choices().get(slot);
        if (action == null) return;
        switch (action)
        {
            case "ffa" -> runAction(player, "Party-FFA", "Party FFA");
            case "team" -> runAction(player, "Team-Battle", "Team Battle");
            case "visibility" -> { plugin.getPartyManager().toggleVisibility(player); open(player); }
            case "host" -> { active.remove(player.getUniqueId()); plugin.getDuelMenuManager().open(player); }
            case "invite" -> openInvites(player, 0);
            default -> { }
        }
    }

    @EventHandler
    public void onDrag(InventoryDragEvent event)
    {
        if (event.getView().getTopInventory().getHolder() instanceof PartyInventoryHolder) event.setCancelled(true);
    }

    @EventHandler
    public void onClose(InventoryCloseEvent event)
    {
        if (event.getInventory().getHolder() instanceof PartyInventoryHolder holder)
            active.remove(event.getPlayer().getUniqueId(), holder.token());
    }

    public void shutdown()
    {
        for (Player player : Bukkit.getOnlinePlayers())
            if (player.getOpenInventory().getTopInventory().getHolder() instanceof PartyInventoryHolder)
                player.closeInventory();
        active.clear();
    }

    private void invite(Player leader, String raw)
    {
        if (raw == null) return;
        Player target;
        try { target = Bukkit.getPlayer(UUID.fromString(raw)); }
        catch (IllegalArgumentException exception) { target = null; }
        if (plugin.getPartyManager().invite(leader, target)) openInvites(leader, 0);
    }

    private void runAction(Player leader, String key, String display)
    {
        Party party = leaderParty(leader);
        if (party == null) return;
        Map<String, String> values = Map.of("<leader>", leader.getName(),
                "<party_size>", Integer.toString(party.size()), "<action>", display);
        for (String configured : configuration.lines("Menus.Party.Actions." + key + ".Commands"))
        {
            String command = MessageService.replace(configured, values);
            if (command.startsWith("/")) command = command.substring(1);
            Bukkit.dispatchCommand(Bukkit.getConsoleSender(), command);
        }
        plugin.getPartyManager().broadcastAction(party, display);
        active.remove(leader.getUniqueId());
        leader.closeInventory();
    }

    private void addAction(Inventory inventory, Map<Integer, String> choices, int size,
                           String configKey, String action, int fallbackSlot,
                           Material fallbackMaterial, String fallbackName, List<String> fallbackLore,
                           Party party)
    {
        String path = "Menus.Party.Actions." + configKey;
        int slot = configuration.slot(path + ".Slot", fallbackSlot, size);
        choices.put(slot, action);
        inventory.setItem(slot, configuredItem(path, fallbackMaterial, fallbackName, fallbackLore,
                Map.of("<leader>", playerName(party.leader()), "<party_size>", Integer.toString(party.size()),
                        "<visibility>", party.visible() ? "visible" : "private",
                        "<visible>", Boolean.toString(party.visible())),
                configuration.text(path + ".Glow", "false").equalsIgnoreCase("true")));
    }

    private ItemStack configuredItem(String path, Material fallbackMaterial, String fallbackName,
                                     List<String> fallbackLore, Map<String, String> replacements,
                                     boolean glow)
    {
        Material material = Material.matchMaterial(configuration.text(path + ".Material", fallbackMaterial.name()));
        String name = MessageService.replace(configuration.text(path + ".Name", fallbackName), replacements);
        List<String> lore = configuration.lines(path + ".Lore");
        if (lore.isEmpty()) lore = fallbackLore;
        lore = lore.stream().map(line -> MessageService.replace(line, replacements)).toList();
        return items.icon(material == null ? fallbackMaterial : material, name, lore, glow);
    }

    private Party leaderParty(Player player)
    {
        Party party = plugin.getPartyManager().getParty(player.getUniqueId());
        if (party == null || !party.leader().equals(player.getUniqueId()))
        {
            MessageService.send(player, plugin.getConfig(), "Messages.Party-Leader-Only", Map.of(),
                    "&cOnly a party leader can use this menu.");
            return null;
        }
        return party;
    }

    private String replace(String value, Party party)
    {
        return value.replace("<leader>", playerName(party.leader()))
                .replace("<party_size>", Integer.toString(party.size()))
                .replace("<visibility>", party.visible() ? "visible" : "private");
    }

    private String playerName(UUID uuid)
    {
        Player player = Bukkit.getPlayer(uuid);
        return player == null ? uuid.toString() : player.getName();
    }
}
