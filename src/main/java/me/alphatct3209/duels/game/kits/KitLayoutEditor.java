package me.alphatct3209.duels.game.kits;

import me.alphatct3209.duels.Duels;
import me.alphatct3209.duels.gui.config.MenuConfiguration;
import me.alphatct3209.duels.gui.item.MenuItemFactory;
import me.alphatct3209.duels.utils.MessageService;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

public final class KitLayoutEditor implements Listener
{
    private static final int SIZE = 54;
    private static final int SAVE_SLOT = 45;
    private static final int RESET_SLOT = 49;
    private static final int CANCEL_SLOT = 53;
    private final Duels plugin;
    private final MenuConfiguration configuration;
    private final MenuItemFactory items;
    private final Map<UUID, UUID> active = new HashMap<>();

    public KitLayoutEditor(Duels plugin)
    {
        this.plugin = Objects.requireNonNull(plugin);
        configuration = new MenuConfiguration(plugin);
        items = new MenuItemFactory(plugin, configuration);
    }

    public boolean openPersonal(Player player, Kit kit)
    {
        if (kit == null)
        {
            message(player, "Messages.Kit-Editor-Not-Found", "&cThat kit does not exist.");
            return false;
        }
        PlayerKitLayoutManager.Layout layout = plugin.getPlayerKitLayoutManager()
                .layout(player.getUniqueId(), kit);
        return open(player, kit, KitEditorInventoryHolder.Scope.PERSONAL,
                layout.storage(), layout.armor(), layout.offhand());
    }

    public boolean openShared(Player player, Kit kit)
    {
        return open(player, kit, KitEditorInventoryHolder.Scope.SHARED,
                kit == null ? null : kit.getInventoryContents(),
                kit == null ? null : kit.getArmorContents(), kit == null ? null : kit.getOffhand());
    }

    private boolean open(Player player, Kit kit, KitEditorInventoryHolder.Scope scope,
                         ItemStack[] storage, ItemStack[] armor, ItemStack offhand)
    {
        ItemStack cursor = player.getItemOnCursor();
        if (cursor != null && !cursor.getType().isAir())
        {
            message(player, "Messages.Kit-Editor-Cursor", "&cEmpty your cursor before opening the kit editor.");
            return false;
        }
        if (kit == null)
        {
            message(player, "Messages.Kit-Editor-Not-Found", "&cThat kit does not exist.");
            return false;
        }
        UUID token = UUID.randomUUID();
        KitEditorInventoryHolder holder = new KitEditorInventoryHolder(token, player.getUniqueId(),
                kit, scope, storage, armor, offhand);
        String titlePath = scope == KitEditorInventoryHolder.Scope.PERSONAL
                ? "Menus.Kit-Editor.Personal-Title" : "Menus.Kit-Editor.Title";
        String fallbackTitle = scope == KitEditorInventoryHolder.Scope.PERSONAL
                ? "&8Your Layout: &f<kit>" : "&8Shared Kit: &f<kit>";
        String title = configuration.text(titlePath, fallbackTitle)
                .replace("<kit>", kit.getName());
        Inventory inventory = Bukkit.createInventory(holder, SIZE, items.color(title));
        holder.attach(inventory);
        restore(inventory, holder);
        inventory.setItem(SAVE_SLOT, control("Menus.Kit-Editor.Controls.Save", Material.LIME_DYE,
                "&aSave Layout", List.of("&7Persist this layout.")));
        inventory.setItem(RESET_SLOT, control("Menus.Kit-Editor.Controls.Reset", Material.ORANGE_DYE,
                "&eReset", List.of("&7Restore the layout from when this editor opened.")));
        inventory.setItem(CANCEL_SLOT, control("Menus.Kit-Editor.Controls.Cancel", Material.RED_DYE,
                "&cCancel", List.of("&7Discard changes.")));
        // Slots 0-40 represent the editable kit and must remain genuinely empty when the kit is empty.
        items.fillEmpty(inventory, 41, SIZE);
        active.put(player.getUniqueId(), token);
        player.openInventory(inventory);
        return true;
    }

    public void shutdown()
    {
        for (Player player : Bukkit.getOnlinePlayers())
            if (player.getOpenInventory().getTopInventory().getHolder() instanceof KitEditorInventoryHolder)
                player.closeInventory();
        active.clear();
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onClick(InventoryClickEvent event)
    {
        if (!(event.getView().getTopInventory().getHolder() instanceof KitEditorInventoryHolder holder)) return;
        if (!(event.getWhoClicked() instanceof Player player) || !isActive(player, holder))
        {
            event.setCancelled(true);
            return;
        }
        int raw = event.getRawSlot();
        if (raw == SAVE_SLOT)
        {
            event.setCancelled(true);
            save(player, holder, event.getView().getTopInventory());
            return;
        }
        if (raw == RESET_SLOT)
        {
            event.setCancelled(true);
            player.setItemOnCursor(null);
            restore(event.getView().getTopInventory(), holder);
            return;
        }
        if (raw == CANCEL_SLOT)
        {
            event.setCancelled(true);
            player.setItemOnCursor(null);
            player.closeInventory();
            message(player, "Messages.Kit-Editor-Cancelled", "&cKit layout changes discarded.");
            return;
        }
        boolean editableTop = raw >= 0 && raw <= 40;
        boolean ordinaryClick = event.getClick() == ClickType.LEFT
                || (holder.scope() == KitEditorInventoryHolder.Scope.SHARED
                && event.getClick() == ClickType.RIGHT);
        if (!editableTop || !ordinaryClick) event.setCancelled(true);
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onDrag(InventoryDragEvent event)
    {
        if (!(event.getView().getTopInventory().getHolder() instanceof KitEditorInventoryHolder holder)) return;
        if (!(event.getWhoClicked() instanceof Player player) || !isActive(player, holder)
                || holder.scope() == KitEditorInventoryHolder.Scope.PERSONAL
                || event.getRawSlots().stream().anyMatch(slot -> slot < 0 || slot > 40))
            event.setCancelled(true);
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onClose(InventoryCloseEvent event)
    {
        if (event.getInventory().getHolder() instanceof KitEditorInventoryHolder holder)
        {
            // The cursor was empty on entry and bottom-inventory transfer is impossible, so any
            // remaining cursor stack is an editor clone and must never escape into player state.
            event.getPlayer().setItemOnCursor(null);
            active.remove(event.getPlayer().getUniqueId(), holder.token());
        }
    }

    private void save(Player player, KitEditorInventoryHolder holder, Inventory inventory)
    {
        ItemStack cursor = player.getItemOnCursor();
        if (cursor != null && !cursor.getType().isAir())
        {
            message(player, "Messages.Kit-Editor-Place-Cursor", "&cPlace the cursor item into the layout before saving.");
            return;
        }
        ItemStack[] storage = new ItemStack[KitSchema.STORAGE_SIZE];
        for (int slot = 0; slot < storage.length; slot++) storage[slot] = cloneItem(inventory.getItem(slot));
        ItemStack[] armor = new ItemStack[]{cloneItem(inventory.getItem(36)), cloneItem(inventory.getItem(37)),
                cloneItem(inventory.getItem(38)), cloneItem(inventory.getItem(39))};
        try
        {
            if (holder.scope() == KitEditorInventoryHolder.Scope.PERSONAL)
            {
                plugin.getPlayerKitLayoutManager().save(player.getUniqueId(), holder.kit(),
                        storage, armor, cloneItem(inventory.getItem(40)));
            }
            else
            {
                plugin.getKitManager().saveLayout(holder.kit(), storage, armor,
                        cloneItem(inventory.getItem(40)));
            }
            player.closeInventory();
            String messagePath = holder.scope() == KitEditorInventoryHolder.Scope.PERSONAL
                    ? "Messages.Personal-Kit-Layout-Saved" : "Messages.Shared-Kit-Saved";
            MessageService.send(player, plugin.getConfig(), messagePath,
                    Map.of("<kit>", holder.kit().getName()), "&aSaved the layout for &e<kit>&a.");
        }
        catch (RuntimeException exception)
        {
            plugin.getLogger().warning("Could not save "
                    + holder.scope().name().toLowerCase(java.util.Locale.ROOT) + " kit layout '"
                    + holder.kit().getName()
                    + "': " + exception.getMessage());
            message(player, "Messages.Kit-Editor-Save-Failed", "&cCould not save that kit layout.");
        }
    }

    private void restore(Inventory inventory, KitEditorInventoryHolder holder)
    {
        ItemStack[] contents = editorContents(holder);
        for (int slot = 0; slot <= 40; slot++) inventory.setItem(slot, contents[slot]);
    }

    private ItemStack[] editorContents(KitEditorInventoryHolder holder)
    {
        ItemStack[] result = new ItemStack[SIZE];
        ItemStack[] storage = holder.originalStorage();
        System.arraycopy(storage, 0, result, 0, storage.length);
        ItemStack[] armor = holder.originalArmor();
        result[36] = cloneItem(armor[0]);
        result[37] = cloneItem(armor[1]);
        result[38] = cloneItem(armor[2]);
        result[39] = cloneItem(armor[3]);
        result[40] = holder.originalOffhand();
        return result;
    }

    private ItemStack control(String path, Material fallbackMaterial, String fallbackName, List<String> fallbackLore)
    {
        Material material = Material.matchMaterial(configuration.text(path + ".Material", fallbackMaterial.name()));
        if (material == null || material.isAir()) material = fallbackMaterial;
        List<String> lore = configuration.lines(path + ".Lore");
        if (lore.isEmpty()) lore = fallbackLore;
        return items.icon(material, configuration.text(path + ".Name", fallbackName), lore,
                Boolean.parseBoolean(configuration.text(path + ".Glow", "false")));
    }

    private boolean isActive(Player player, KitEditorInventoryHolder holder)
    {
        return player.getUniqueId().equals(holder.viewer())
                && holder.token().equals(active.get(player.getUniqueId()));
    }

    private void message(Player player, String path, String fallback)
    {
        MessageService.send(player, plugin.getConfig(), path, Map.of(), fallback);
    }

    private static ItemStack cloneItem(ItemStack item) { return item == null ? null : item.clone(); }
}
