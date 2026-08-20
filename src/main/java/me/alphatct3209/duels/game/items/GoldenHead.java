package me.alphatct3209.duels.game.items;

import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.util.List;

/** A custom instant UHC consumable represented by the receiving player's own textured head. */
public final class GoldenHead
{
    public static final int DEFAULT_UHC_AMOUNT = 3;
    public static final int REGENERATION_DURATION_TICKS = 5 * 20;
    public static final int REGENERATION_AMPLIFIER = 2; // amplifier 2 displays as Regeneration III
    public static final int ABSORPTION_DURATION_TICKS = 2 * 60 * 20;
    public static final int ABSORPTION_AMPLIFIER = 0; // amplifier 0 displays as Absorption I
    private static final NamespacedKey KEY = new NamespacedKey("hyxduels", "golden_head");

    private GoldenHead() {}

    public static ItemStack create(Player owner, int amount)
    {
        if (amount < 1) throw new IllegalArgumentException("Golden Head amount must be positive");
        ItemStack item = new ItemStack(Material.PLAYER_HEAD, amount);
        SkullMeta meta = (SkullMeta) item.getItemMeta();
        meta.setOwningPlayer(owner);
        meta.setDisplayName(ChatColor.GOLD + "Golden Head");
        meta.setLore(List.of(
                ChatColor.GRAY + "Right-click to consume",
                ChatColor.LIGHT_PURPLE + "Regeneration III " + ChatColor.GRAY + "(0:05)",
                ChatColor.BLUE + "Absorption I " + ChatColor.GRAY + "(2:00)"));
        meta.getPersistentDataContainer().set(KEY, PersistentDataType.BYTE, (byte) 1);
        item.setItemMeta(meta);
        return item;
    }

    public static boolean isGoldenHead(ItemStack item)
    {
        if (item == null || item.getType() != Material.PLAYER_HEAD || !item.hasItemMeta()) return false;
        ItemMeta meta = item.getItemMeta();
        return meta.getPersistentDataContainer().has(KEY, PersistentDataType.BYTE);
    }

    public static void applyEffects(Player player)
    {
        player.addPotionEffect(new PotionEffect(PotionEffectType.REGENERATION,
                REGENERATION_DURATION_TICKS, REGENERATION_AMPLIFIER, false, true, true), true);
        player.addPotionEffect(new PotionEffect(PotionEffectType.ABSORPTION,
                ABSORPTION_DURATION_TICKS, ABSORPTION_AMPLIFIER, false, true, true), true);
    }
}
