package me.alphatct3209.duels.game.kits;

import org.bukkit.Color;
import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.LeatherArmorMeta;
import org.bukkit.inventory.meta.PotionMeta;
import org.bukkit.potion.PotionType;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * Runtime defaults for the stable duel-mode roster. A configured kit with the same canonical key
 * always wins, so server owners can replace any loadout without editing Java or losing their kit.
 */
final class BuiltInModeKits
{
    private static final List<String> KEYS = List.of("bed_wars", "blitz", "bow", "boxing",
            "bridge", "classic", "combo", "mega_walls", "nodebuff", "op", "parkour",
            "quakecraft", "skywars", "spleef", "sumo", "uhc");

    private BuiltInModeKits() {}

    static Set<String> keys()
    {
        return Set.copyOf(new LinkedHashSet<>(KEYS));
    }

    static void addMissingTo(List<Kit> kits)
    {
        Set<String> configured = new LinkedHashSet<>();
        for (Kit kit : kits) configured.add(kit.getKey());
        int id = -1;
        for (String key : KEYS)
        {
            if (!configured.contains(key)) kits.add(create(id--, key));
        }
    }

    private static Kit create(int id, String key)
    {
        return switch (key)
        {
            case "bed_wars" -> kit(id, "Bed Wars", bedWarsArmor(),
                    loadout(item(Material.WOODEN_SWORD)));
            case "blitz" -> kit(id, "Blitz", armor(Material.CHAINMAIL_BOOTS,
                    Material.CHAINMAIL_LEGGINGS, Material.CHAINMAIL_CHESTPLATE, Material.IRON_HELMET),
                    loadout(item(Material.STONE_SWORD), item(Material.FISHING_ROD), item(Material.BOW),
                            item(Material.ARROW, 16), item(Material.GOLDEN_APPLE, 2),
                            item(Material.COOKED_BEEF, 16)));
            case "bow" -> kit(id, "Bow", leatherArmor(Color.fromRGB(0x6B8EED)),
                    loadout(enchanted(Material.BOW, Enchantment.POWER, 2), item(Material.ARROW, 64)));
            case "boxing" -> kit(id, "Boxing", noArmor(), loadout());
            case "bridge" -> kit(id, "Bridge", leatherArmor(Color.fromRGB(0x3C44AA)),
                    loadout(item(Material.IRON_SWORD), item(Material.BOW), item(Material.ARROW, 8),
                            item(Material.GOLDEN_APPLE, 2), item(Material.DIAMOND_PICKAXE),
                            item(Material.WHITE_TERRACOTTA, 64), item(Material.WHITE_TERRACOTTA, 64)));
            case "classic" -> kit(id, "Classic", armor(Material.IRON_BOOTS,
                    Material.IRON_LEGGINGS, Material.IRON_CHESTPLATE, Material.IRON_HELMET),
                    loadout(item(Material.IRON_SWORD), item(Material.FISHING_ROD), item(Material.BOW),
                            item(Material.ARROW, 5), item(Material.FLINT_AND_STEEL),
                            item(Material.GOLDEN_APPLE)));
            case "combo" -> kit(id, "Combo", enchantedArmor(Material.DIAMOND_BOOTS,
                    Material.DIAMOND_LEGGINGS, Material.DIAMOND_CHESTPLATE, Material.DIAMOND_HELMET, 2),
                    loadout(enchanted(Material.DIAMOND_SWORD, Enchantment.SHARPNESS, 2),
                            item(Material.GOLDEN_APPLE, 64), speed(), speed(),
                            item(Material.COOKED_BEEF, 64)));
            case "mega_walls" -> kit(id, "Mega Walls", armor(Material.IRON_BOOTS,
                    Material.IRON_LEGGINGS, Material.IRON_CHESTPLATE, Material.IRON_HELMET),
                    loadout(item(Material.IRON_SWORD), item(Material.IRON_AXE), item(Material.BOW),
                            item(Material.ARROW, 15), item(Material.OAK_PLANKS, 64),
                            item(Material.COOKED_BEEF, 3), healing(), healing()));
            case "nodebuff" -> kit(id, "Nodebuff", enchantedArmor(Material.DIAMOND_BOOTS,
                    Material.DIAMOND_LEGGINGS, Material.DIAMOND_CHESTPLATE, Material.DIAMOND_HELMET, 2),
                    nodebuff());
            case "op" -> kit(id, "OP", enchantedArmor(Material.NETHERITE_BOOTS,
                    Material.NETHERITE_LEGGINGS, Material.NETHERITE_CHESTPLATE, Material.NETHERITE_HELMET, 3),
                    loadout(enchanted(Material.NETHERITE_SWORD, Enchantment.SHARPNESS, 4),
                            enchanted(Material.BOW, Enchantment.POWER, 4), item(Material.ARROW, 32),
                            item(Material.ENCHANTED_GOLDEN_APPLE, 3), item(Material.GOLDEN_APPLE, 16),
                            item(Material.ENDER_PEARL, 8), item(Material.COOKED_BEEF, 64)));
            case "parkour" -> kit(id, "Parkour", noArmor(), loadout());
            case "quakecraft" -> kit(id, "Quakecraft", noArmor(), loadout());
            case "skywars" -> kit(id, "SkyWars", noArmor(), loadout());
            case "spleef" -> kit(id, "Spleef", noArmor(),
                    loadout(enchanted(Material.DIAMOND_SHOVEL, Enchantment.EFFICIENCY, 5),
                            item(Material.SNOWBALL, 16)));
            case "sumo" -> kit(id, "Sumo", noArmor(), loadout());
            case "uhc" -> kit(id, "UHC", armor(Material.DIAMOND_BOOTS,
                    Material.IRON_LEGGINGS, Material.DIAMOND_CHESTPLATE, Material.IRON_HELMET),
                    loadout(item(Material.DIAMOND_SWORD), item(Material.FISHING_ROD), item(Material.BOW),
                            item(Material.ARROW, 32), item(Material.GOLDEN_APPLE, 6),
                            item(Material.OAK_PLANKS, 64), item(Material.WATER_BUCKET),
                            item(Material.LAVA_BUCKET), item(Material.COOKED_BEEF, 32)));
            default -> throw new IllegalArgumentException("Unknown built-in mode kit " + key);
        };
    }

    private static Loadout nodebuff()
    {
        Loadout loadout = new Loadout();
        loadout.put(0, enchanted(Material.DIAMOND_SWORD, Enchantment.SHARPNESS, 2));
        loadout.put(1, item(Material.ENDER_PEARL, 16));
        loadout.put(2, item(Material.COOKED_BEEF, 64));
        loadout.put(3, speed());
        loadout.put(4, speed());
        for (int slot = 5; slot < KitSchema.STORAGE_SIZE; slot++) loadout.put(slot, healing());
        return loadout;
    }

    private static Kit kit(int id, String name, ItemStack[] armor, Loadout loadout)
    {
        return Kit.positionalV2(id, name, armor, loadout.storage, null);
    }

    private static Loadout loadout(ItemStack... items)
    {
        Loadout loadout = new Loadout();
        for (int slot = 0; slot < items.length; slot++) loadout.put(slot, items[slot]);
        return loadout;
    }

    private static ItemStack item(Material material) { return item(material, 1); }
    private static ItemStack item(Material material, int amount) { return new ItemStack(material, amount); }

    private static ItemStack enchanted(Material material, Enchantment enchantment, int level)
    {
        ItemStack item = item(material);
        item.addUnsafeEnchantment(enchantment, level);
        return item;
    }

    private static ItemStack healing() { return potion(PotionType.STRONG_HEALING); }
    private static ItemStack speed() { return potion(PotionType.STRONG_SWIFTNESS); }

    private static ItemStack potion(PotionType type)
    {
        ItemStack item = item(Material.SPLASH_POTION);
        PotionMeta meta = (PotionMeta) item.getItemMeta();
        meta.setBasePotionType(type);
        item.setItemMeta(meta);
        return item;
    }

    /** Bukkit armor arrays are ordered boots, leggings, chestplate, helmet. */
    private static ItemStack[] armor(Material boots, Material leggings, Material chestplate, Material helmet)
    {
        return new ItemStack[]{item(boots), item(leggings), item(chestplate), item(helmet)};
    }

    private static ItemStack[] enchantedArmor(Material boots, Material leggings, Material chestplate,
                                               Material helmet, int protection)
    {
        ItemStack[] armor = armor(boots, leggings, chestplate, helmet);
        for (ItemStack item : armor) item.addUnsafeEnchantment(Enchantment.PROTECTION, protection);
        return armor;
    }

    private static ItemStack[] leatherArmor(Color color)
    {
        ItemStack[] armor = armor(Material.LEATHER_BOOTS, Material.LEATHER_LEGGINGS,
                Material.LEATHER_CHESTPLATE, Material.LEATHER_HELMET);
        for (ItemStack item : armor)
        {
            LeatherArmorMeta meta = (LeatherArmorMeta) item.getItemMeta();
            meta.setColor(color);
            item.setItemMeta(meta);
        }
        return armor;
    }

    private static ItemStack[] bedWarsArmor()
    {
        return new ItemStack[]{item(Material.LEATHER_BOOTS), item(Material.LEATHER_LEGGINGS), null, null};
    }

    private static ItemStack[] noArmor() { return new ItemStack[4]; }

    private static final class Loadout
    {
        private final ItemStack[] storage = new ItemStack[KitSchema.STORAGE_SIZE];
        private void put(int slot, ItemStack item) { storage[slot] = item; }
    }
}
