package me.alphatct3209.duels.gui.item;

import me.alphatct3209.duels.Duels;
import me.alphatct3209.duels.gui.config.MenuConfiguration;
import me.alphatct3209.duels.gui.config.MenuOpener;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

import java.util.List;
import java.util.Objects;

public final class MenuItemFactory
{
    private final MenuConfiguration configuration;
    private final NamespacedKey openerKey;

    public MenuItemFactory(Duels plugin, MenuConfiguration configuration)
    {
        this.configuration = Objects.requireNonNull(configuration, "configuration");
        openerKey = new NamespacedKey(Objects.requireNonNull(plugin, "plugin"), "menu_opener");
    }

    public ItemStack opener(MenuOpener configured)
    {
        ItemStack item = icon(configured.material(), configured.name(), configured.lore(), configured.glow());
        ItemMeta meta = item.getItemMeta();
        meta.getPersistentDataContainer().set(openerKey, PersistentDataType.STRING, configured.id());
        if (configured.customModelData() != null)
        {
            meta.setCustomModelData(configured.customModelData());
        }
        meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES, ItemFlag.HIDE_UNBREAKABLE);
        item.setItemMeta(meta);
        return item;
    }

    public ItemStack icon(Material material, String name, List<String> lore, boolean glow)
    {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(color(name));
        meta.setLore(lore.stream().map(this::color).toList());
        if (glow)
        {
            meta.addEnchant(Enchantment.UNBREAKING, 1, true);
            meta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
        }
        item.setItemMeta(meta);
        return item;
    }

    /** Fills currently empty slots without replacing any interactive or content item. */
    public void fillEmpty(Inventory inventory)
    {
        fillEmpty(inventory, 0, inventory.getSize());
    }

    /** Fills currently empty slots in the half-open range, preserving editable GUI regions. */
    public void fillEmpty(Inventory inventory, int fromInclusive, int toExclusive)
    {
        Objects.requireNonNull(inventory, "inventory");
        if (!configuration.fillerEnabled()) return;
        if (fromInclusive < 0 || toExclusive > inventory.getSize() || fromInclusive > toExclusive)
            throw new IllegalArgumentException("Invalid filler slot range");
        ItemStack template = icon(configuration.fillerMaterial(), configuration.fillerName(),
                configuration.fillerLore(), configuration.fillerGlow());
        for (int slot = fromInclusive; slot < toExclusive; slot++)
        {
            ItemStack current = inventory.getItem(slot);
            if (current == null || current.getType() == Material.AIR
                    || current.getType() == Material.CAVE_AIR || current.getType() == Material.VOID_AIR)
                inventory.setItem(slot, template.clone());
        }
    }

    public MenuOpener configured(ItemStack item)
    {
        if (item == null || item.getType().isAir() || !item.hasItemMeta())
        {
            return null;
        }
        String id = item.getItemMeta().getPersistentDataContainer()
                .get(openerKey, PersistentDataType.STRING);
        return id == null ? null : configuration.opener(id);
    }

    public boolean isOpener(ItemStack item)
    {
        return configured(item) != null;
    }

    public String color(String value)
    {
        return ChatColor.translateAlternateColorCodes('&', value == null ? "" : value);
    }
}
