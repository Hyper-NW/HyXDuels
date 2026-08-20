package me.alphatct3209.duels.game.modes.bedwars;

import org.bukkit.Color;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.LeatherArmorMeta;

/** Match-scoped permanent Bed Wars purchases. Tool tiers lose one level on death. */
public final class BedWarsLoadout
{
    private ArmorTier armor = ArmorTier.LEATHER;
    private ToolTier pickaxe = ToolTier.NONE;
    private ToolTier axe = ToolTier.NONE;
    private boolean shears;

    public boolean canPurchase(BedWarsUpgrade upgrade)
    {
        return switch (upgrade)
        {
            case CHAINMAIL_ARMOR -> armor.ordinal() < ArmorTier.CHAINMAIL.ordinal();
            case IRON_ARMOR -> armor.ordinal() < ArmorTier.IRON.ordinal();
            case DIAMOND_ARMOR -> armor.ordinal() < ArmorTier.DIAMOND.ordinal();
            case WOODEN_PICKAXE -> pickaxe == ToolTier.NONE;
            case IRON_PICKAXE -> pickaxe.ordinal() < ToolTier.IRON.ordinal();
            case DIAMOND_PICKAXE -> pickaxe.ordinal() < ToolTier.DIAMOND.ordinal();
            case WOODEN_AXE -> axe == ToolTier.NONE;
            case IRON_AXE -> axe.ordinal() < ToolTier.IRON.ordinal();
            case DIAMOND_AXE -> axe.ordinal() < ToolTier.DIAMOND.ordinal();
            case SHEARS -> !shears;
        };
    }

    public boolean purchase(BedWarsUpgrade upgrade)
    {
        if (!canPurchase(upgrade)) return false;
        switch (upgrade)
        {
            case CHAINMAIL_ARMOR -> armor = ArmorTier.CHAINMAIL;
            case IRON_ARMOR -> armor = ArmorTier.IRON;
            case DIAMOND_ARMOR -> armor = ArmorTier.DIAMOND;
            case WOODEN_PICKAXE -> pickaxe = ToolTier.WOOD;
            case IRON_PICKAXE -> pickaxe = ToolTier.IRON;
            case DIAMOND_PICKAXE -> pickaxe = ToolTier.DIAMOND;
            case WOODEN_AXE -> axe = ToolTier.WOOD;
            case IRON_AXE -> axe = ToolTier.IRON;
            case DIAMOND_AXE -> axe = ToolTier.DIAMOND;
            case SHEARS -> shears = true;
        }
        return true;
    }

    public void afterDeath()
    {
        pickaxe = pickaxe.downgrade();
        axe = axe.downgrade();
    }

    public void apply(Player player, Color teamColor)
    {
        player.getInventory().setBoots(armorPiece(armor.boots, teamColor));
        player.getInventory().setLeggings(armorPiece(armor.leggings, teamColor));
        removeTools(player);
        if (pickaxe != ToolTier.NONE) player.getInventory().addItem(new ItemStack(pickaxe.pickaxe));
        if (axe != ToolTier.NONE) player.getInventory().addItem(new ItemStack(axe.axe));
        if (shears) player.getInventory().addItem(new ItemStack(Material.SHEARS));
    }

    private void removeTools(Player player)
    {
        ItemStack[] storage = player.getInventory().getStorageContents();
        for (int slot = 0; slot < storage.length; slot++)
        {
            ItemStack item = storage[slot];
            if (item != null && (item.getType().name().endsWith("_PICKAXE")
                    || item.getType().name().endsWith("_AXE") || item.getType() == Material.SHEARS))
                storage[slot] = null;
        }
        player.getInventory().setStorageContents(storage);
    }

    public String armorTier() { return armor.name(); }
    public String pickaxeTier() { return pickaxe.name(); }
    public String axeTier() { return axe.name(); }
    public boolean hasShears() { return shears; }

    private ItemStack armorPiece(Material material, Color teamColor)
    {
        ItemStack item = new ItemStack(material);
        if (item.getItemMeta() instanceof LeatherArmorMeta meta)
        {
            meta.setColor(teamColor);
            item.setItemMeta(meta);
        }
        return item;
    }

    private enum ArmorTier
    {
        LEATHER(Material.LEATHER_BOOTS, Material.LEATHER_LEGGINGS),
        CHAINMAIL(Material.CHAINMAIL_BOOTS, Material.CHAINMAIL_LEGGINGS),
        IRON(Material.IRON_BOOTS, Material.IRON_LEGGINGS),
        DIAMOND(Material.DIAMOND_BOOTS, Material.DIAMOND_LEGGINGS);

        private final Material boots;
        private final Material leggings;
        ArmorTier(Material boots, Material leggings)
        {
            this.boots = boots;
            this.leggings = leggings;
        }
    }

    private enum ToolTier
    {
        NONE(null, null),
        WOOD(Material.WOODEN_PICKAXE, Material.WOODEN_AXE),
        STONE(Material.STONE_PICKAXE, Material.STONE_AXE),
        IRON(Material.IRON_PICKAXE, Material.IRON_AXE),
        DIAMOND(Material.DIAMOND_PICKAXE, Material.DIAMOND_AXE);

        private final Material pickaxe;
        private final Material axe;
        ToolTier(Material pickaxe, Material axe)
        {
            this.pickaxe = pickaxe;
            this.axe = axe;
        }
        private ToolTier downgrade()
        {
            return switch (this)
            {
                case DIAMOND -> IRON;
                case IRON -> STONE;
                case STONE, WOOD -> WOOD;
                case NONE -> NONE;
            };
        }
    }
}
