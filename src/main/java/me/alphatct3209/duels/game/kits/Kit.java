package me.alphatct3209.duels.game.kits;

import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;

import java.util.Objects;

public class Kit
{
    private final int id;
    private final String name;
    private final KitFormat format;

    private final ItemStack[] armor;
    private final ItemStack[] inventory;
    private final ItemStack offhand;

    /**
     * Creates a legacy v1 kit whose inventory is applied with addItem.
     */
    public Kit(int id, String name, ItemStack[] armor, ItemStack[] inventory)
    {
        this(id, name, KitFormat.LEGACY_V1, armor, inventory, null);
    }

    static Kit positionalV2(int id, String name, ItemStack[] armor, ItemStack[] storage, ItemStack offhand)
    {
        return new Kit(id, name, KitFormat.POSITIONAL_V2, armor, storage, offhand);
    }

    private Kit(int id, String name, KitFormat format, ItemStack[] armor,
                ItemStack[] inventory, ItemStack offhand)
    {
        this.id = id;
        this.name = name;
        this.format = Objects.requireNonNull(format, "format");
        if (format == KitFormat.POSITIONAL_V2)
        {
            KitSchema.requireV2StorageSize(inventory == null ? 0 : inventory.length, "kit " + id);
        }
        this.armor = cloneItems(armor == null ? new ItemStack[4] : armor);
        this.inventory = cloneItems(inventory == null ? new ItemStack[0] : inventory);
        this.offhand = cloneItem(offhand);
    }

    public void apply(Player player)
    {
        PlayerInventory playerInventory = player.getInventory();
        playerInventory.clear();

        if (format == KitFormat.LEGACY_V1)
        {
            playerInventory.setArmorContents(cloneItems(armor));
            for (ItemStack item : inventory)
            {
                if (item != null)
                {
                    playerInventory.addItem(item.clone());
                }
            }
            playerInventory.setItemInOffHand(null);
            return;
        }

        playerInventory.setStorageContents(cloneItems(inventory));
        playerInventory.setArmorContents(cloneItems(armor));
        playerInventory.setItemInOffHand(cloneItem(offhand));
    }

    public int getId()
    {
        return id;
    }

    public String getName()
    {
        return name;
    }

    public KitFormat getFormat()
    {
        return format;
    }

    ItemStack[] getInventoryContents()
    {
        return cloneItems(inventory);
    }

    ItemStack[] getArmorContents()
    {
        return cloneItems(armor);
    }

    ItemStack getOffhand()
    {
        return cloneItem(offhand);
    }

    /** Canonical reusable kit identity, independent of duel mode identity. */
    public String getKey()
    {
        return GamemodeKey.fromKitName(name);
    }

    /** @deprecated progression and routing now use first-class ModeKey values. */
    @Deprecated
    public String getGamemodeKey()
    {
        return getKey();
    }

    private static ItemStack[] cloneItems(ItemStack[] items)
    {
        ItemStack[] clone = new ItemStack[items.length];
        for (int i = 0; i < items.length; i++)
        {
            clone[i] = cloneItem(items[i]);
        }
        return clone;
    }

    private static ItemStack cloneItem(ItemStack item)
    {
        return item == null ? null : item.clone();
    }
}
