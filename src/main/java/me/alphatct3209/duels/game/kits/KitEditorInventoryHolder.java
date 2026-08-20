package me.alphatct3209.duels.game.kits;

import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;

import java.util.Objects;
import java.util.UUID;

final class KitEditorInventoryHolder implements InventoryHolder
{
    private final UUID token;
    private final UUID viewer;
    private final Kit kit;
    private final Scope scope;
    private final ItemStack[] storage;
    private final ItemStack[] armor;
    private final ItemStack offhand;
    private Inventory inventory;

    KitEditorInventoryHolder(UUID token, UUID viewer, Kit kit)
    {
        this(token, viewer, kit, Scope.SHARED, normalizeStorage(kit.getInventoryContents()),
                kit.getArmorContents(), kit.getOffhand());
    }

    KitEditorInventoryHolder(UUID token, UUID viewer, Kit kit, Scope scope,
                             ItemStack[] storage, ItemStack[] armor, ItemStack offhand)
    {
        this.token = Objects.requireNonNull(token);
        this.viewer = Objects.requireNonNull(viewer);
        this.kit = Objects.requireNonNull(kit);
        this.scope = Objects.requireNonNull(scope);
        this.storage = normalizeStorage(storage);
        this.armor = cloneItems(armor);
        this.offhand = offhand == null ? null : offhand.clone();
    }

    void attach(Inventory inventory) { this.inventory = inventory; }
    @Override public Inventory getInventory() { return inventory; }
    UUID token() { return token; }
    UUID viewer() { return viewer; }
    Kit kit() { return kit; }
    Scope scope() { return scope; }
    ItemStack[] originalStorage() { return cloneItems(storage); }
    ItemStack[] originalArmor() { return cloneItems(armor); }
    ItemStack originalOffhand() { return offhand == null ? null : offhand.clone(); }

    enum Scope { PERSONAL, SHARED }

    private static ItemStack[] normalizeStorage(ItemStack[] source)
    {
        ItemStack[] result = new ItemStack[KitSchema.STORAGE_SIZE];
        if (source != null)
            for (int index = 0; index < Math.min(source.length, result.length); index++)
                result[index] = source[index] == null ? null : source[index].clone();
        return result;
    }

    private static ItemStack[] cloneItems(ItemStack[] source)
    {
        ItemStack[] result = new ItemStack[source.length];
        for (int index = 0; index < source.length; index++)
            result[index] = source[index] == null ? null : source[index].clone();
        return result;
    }
}
