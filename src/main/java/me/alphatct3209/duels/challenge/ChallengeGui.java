package me.alphatct3209.duels.challenge;

import me.alphatct3209.duels.Duels;
import me.alphatct3209.duels.game.kits.Kit;
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
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

public final class ChallengeGui implements Listener
{
    private final Duels plugin;
    private final ChallengeManager challenges;
    private final Map<UUID, UUID> activeSessions = new HashMap<>();

    ChallengeGui(Duels plugin, ChallengeManager challenges)
    {
        this.plugin = plugin;
        this.challenges = challenges;
    }

    public void open(Player viewer, Player target, int requestedPage)
    {
        me.alphatct3209.duels.game.modes.DuelMode selectedMode = plugin.getModeManager().require(
                plugin.getSelectionService().resolve(viewer.getUniqueId()).modeKey());
        List<Kit> kits = plugin.getKitManager().getKitList().stream()
                .filter(Objects::nonNull)
                .filter(kit -> selectedMode.allowsKit(kit.getKey()))
                .filter(kit -> plugin.getKitManager().getKitByCanonicalKey(kit.getKey()) == kit)
                .toList();
        ChallengeGuiLayout.Page<Kit> page = ChallengeGuiLayout.page(kits, requestedPage);
        Map<Integer, String> mappedSlots = new HashMap<>();
        page.slots().forEach((slot, kit) -> mappedSlots.put(slot, kit.getKey()));

        UUID token = UUID.randomUUID();
        ChallengeInventoryHolder holder = new ChallengeInventoryHolder(token,
                viewer.getUniqueId(), target.getUniqueId(), page.index(), page.count(), mappedSlots);
        String title = color(plugin.getConfig().getString("Challenges.Gui.Title", "Challenge <player>"))
                .replace("<player>", target.getName())
                .replace("<page>", Integer.toString(page.index() + 1))
                .replace("<pages>", Integer.toString(page.count()));
        Inventory inventory = Bukkit.createInventory(holder, ChallengeGuiLayout.INVENTORY_SIZE, title);
        holder.attach(inventory);

        page.slots().forEach((slot, kit) -> inventory.setItem(slot, kitItem(kit, target)));
        if (page.hasPrevious())
        {
            inventory.setItem(ChallengeGuiLayout.PREVIOUS_SLOT,
                    named(Material.ARROW, "Challenges.Gui.Previous", "&ePrevious page"));
        }
        inventory.setItem(ChallengeGuiLayout.INFO_SLOT, infoItem(target, page));
        if (page.hasNext())
        {
            inventory.setItem(ChallengeGuiLayout.NEXT_SLOT,
                    named(Material.ARROW, "Challenges.Gui.Next", "&eNext page"));
        }

        activeSessions.put(viewer.getUniqueId(), token);
        viewer.openInventory(inventory);
    }

    @EventHandler
    public void onClick(InventoryClickEvent event)
    {
        if (!(event.getView().getTopInventory().getHolder() instanceof ChallengeInventoryHolder holder))
        {
            return;
        }
        // Cancel every mode in both inventories, including shift, number-key, double and outside clicks.
        event.setCancelled(true);
        if (!(event.getWhoClicked() instanceof Player player)
                || !isActive(player, holder))
        {
            return;
        }

        int rawSlot = event.getRawSlot();
        if (rawSlot < 0 || rawSlot >= ChallengeGuiLayout.INVENTORY_SIZE)
        {
            return;
        }
        if (rawSlot == ChallengeGuiLayout.PREVIOUS_SLOT && holder.page() > 0)
        {
            Player target = Bukkit.getPlayer(holder.target());
            if (target != null)
            {
                open(player, target, holder.page() - 1);
            }
            return;
        }
        if (rawSlot == ChallengeGuiLayout.NEXT_SLOT && holder.page() + 1 < holder.pageCount())
        {
            Player target = Bukkit.getPlayer(holder.target());
            if (target != null)
            {
                open(player, target, holder.page() + 1);
            }
            return;
        }

        String kitKey = holder.kitSlots().get(rawSlot);
        if (kitKey == null)
        {
            return;
        }
        // Consume the GUI token before invoking challenge creation, so repeated click packets are inert.
        if (!activeSessions.remove(player.getUniqueId(), holder.sessionToken()))
        {
            return;
        }
        player.closeInventory();
        Player target = Bukkit.getPlayer(holder.target());
        Kit kit = plugin.getKitManager().getKitByCanonicalKey(kitKey);
        if (kit != null)
        {
            challenges.send(player, target, kit);
        }
    }

    @EventHandler
    public void onDrag(InventoryDragEvent event)
    {
        if (event.getView().getTopInventory().getHolder() instanceof ChallengeInventoryHolder)
        {
            // Cancel all drags while this GUI is open, even drags wholly inside the player inventory.
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onClose(InventoryCloseEvent event)
    {
        if (event.getInventory().getHolder() instanceof ChallengeInventoryHolder holder)
        {
            activeSessions.remove(event.getPlayer().getUniqueId(), holder.sessionToken());
        }
    }

    private boolean isActive(Player player, ChallengeInventoryHolder holder)
    {
        return player.getUniqueId().equals(holder.viewer())
                && holder.sessionToken().equals(activeSessions.get(player.getUniqueId()));
    }

    private ItemStack kitItem(Kit kit, Player target)
    {
        ItemStack item = new ItemStack(Material.CHEST);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(color(plugin.getConfig().getString("Challenges.Gui.Kit-Name", "&a<kit>"))
                .replace("<kit>", kit.getName()));
        List<String> lore = new ArrayList<>();
        for (String line : plugin.getConfig().getStringList("Challenges.Gui.Kit-Lore"))
        {
            lore.add(color(line.replace("<kit>", kit.getName()).replace("<player>", target.getName())));
        }
        meta.setLore(lore);
        item.setItemMeta(meta);
        return item;
    }

    private ItemStack infoItem(Player target, ChallengeGuiLayout.Page<Kit> page)
    {
        ItemStack item = named(Material.PAPER, "Challenges.Gui.Info", "&bChallenge <player>");
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(meta.getDisplayName()
                .replace("<player>", target.getName())
                .replace("<page>", Integer.toString(page.index() + 1))
                .replace("<pages>", Integer.toString(page.count())));
        item.setItemMeta(meta);
        return item;
    }

    private ItemStack named(Material material, String path, String fallback)
    {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(color(plugin.getConfig().getString(path, fallback)));
        item.setItemMeta(meta);
        return item;
    }

    private String color(String value)
    {
        return ChatColor.translateAlternateColorCodes('&', value);
    }
}
