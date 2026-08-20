package me.alphatct3209.duels.utils;

import de.tr7zw.changeme.nbtapi.NBT;
import de.tr7zw.changeme.nbtapi.iface.ReadWriteNBT;

import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;

public class ItemSerializationUtils
{

    private static final String EMPTY_ITEM = "";

    public static String serialize(ItemStack item) {
        if (item == null || isEmpty(item.getType(), item.getAmount())) {
            return EMPTY_ITEM;
        }

        ReadWriteNBT nbt = NBT.itemStackToNBT(item);
        return nbt.toString();
    }

    static boolean isEmpty(Material material, int amount) {
        return amount <= 0
                || material == Material.AIR
                || material == Material.CAVE_AIR
                || material == Material.VOID_AIR;
    }

    public static ItemStack deserialize(String itemStr) {
        if (itemStr == null || itemStr.isBlank()) {
            return null;
        }

        ReadWriteNBT nbt = NBT.parseNBT(itemStr);
        return NBT.itemStackFromNBT(nbt);
    }

}
