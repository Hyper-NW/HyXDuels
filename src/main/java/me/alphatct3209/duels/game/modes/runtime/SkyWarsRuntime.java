package me.alphatct3209.duels.game.modes.runtime;

import me.alphatct3209.duels.game.arenas.Arena;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Container;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.BooleanSupplier;

/** Owns initial island/middle loot and timed SkyWars chest refills for one match. */
public final class SkyWarsRuntime extends TrackedModeRuntime
{
    private final Arena arena;
    private final BooleanSupplier active;

    public SkyWarsRuntime(Arena arena, BooleanSupplier active)
    {
        this.arena = arena;
        this.active = active;
    }

    @Override
    public void start()
    {
        refill(false);
        long delay = Math.multiplyExact((long) arena.getObjectiveSettings().skyWarsRefillSeconds(), 20L);
        track(arena.scheduleRepeating(() -> {
            if (!active.getAsBoolean()) return;
            refill(true);
            arena.sendConfiguredToPlayers("Messages.SkyWars-Refill", java.util.Map.of(),
                    "&eSkyWars chests have refilled with upgraded loot!");
        }, delay, delay));
    }

    private void refill(boolean upgraded)
    {
        Set<Container> populated = new LinkedHashSet<>();
        arena.getPoints().forEach((name, location) -> {
            boolean island = name.equals("chest_1") || name.equals("chest_2")
                    || name.matches("chest_[12]_[1-9][0-9]*");
            boolean middle = name.equals("mid_chest") || name.matches("mid_chest_[1-9][0-9]*");
            if (!island && !middle) return;
            Container container = findContainer(location);
            if (container == null)
                throw new IllegalStateException("SkyWars marker '" + name
                        + "' has no chest/container within 2 blocks");
            if (populated.add(container)) populate(container.getInventory(), middle, upgraded);
        });
    }

    private Container findContainer(Location marker)
    {
        for (int radius = 0; radius <= 2; radius++)
            for (int x = -radius; x <= radius; x++)
                for (int y = -radius; y <= radius; y++)
                    for (int z = -radius; z <= radius; z++)
                        if (marker.getBlock().getRelative(x, y, z).getState() instanceof Container container)
                            return container;
        return null;
    }

    private void populate(Inventory inventory, boolean middle, boolean upgraded)
    {
        inventory.clear();
        List<ItemStack> loot = middle ? middleLoot(upgraded) : islandLoot(upgraded);
        Collections.shuffle(loot);
        List<Integer> slots = new ArrayList<>();
        for (int slot = 0; slot < inventory.getSize(); slot++) slots.add(slot);
        Collections.shuffle(slots);
        int count = Math.min(loot.size(), Math.min(slots.size(), 8));
        for (int index = 0; index < count; index++) inventory.setItem(slots.get(index), loot.get(index));
    }

    private List<ItemStack> islandLoot(boolean upgraded)
    {
        ThreadLocalRandom random = ThreadLocalRandom.current();
        List<ItemStack> loot = new ArrayList<>();
        loot.add(item(random.nextBoolean() ? Material.STONE_SWORD : Material.IRON_SWORD, 1));
        loot.add(item(random.nextBoolean() ? Material.OAK_PLANKS : Material.STONE, 32));
        loot.add(item(Material.COOKED_BEEF, 8));
        loot.add(item(random.nextBoolean() ? Material.EGG : Material.SNOWBALL, 16));
        loot.add(item(Material.IRON_PICKAXE, 1));
        loot.add(item(random.nextBoolean() ? Material.IRON_CHESTPLATE : Material.IRON_LEGGINGS, 1));
        loot.add(item(Material.BOW, 1));
        loot.add(item(Material.ARROW, upgraded ? 24 : 12));
        if (random.nextBoolean()) loot.add(item(Material.WATER_BUCKET, 1));
        if (upgraded) loot.add(item(Material.ENDER_PEARL, 1));
        return loot;
    }

    private List<ItemStack> middleLoot(boolean upgraded)
    {
        List<ItemStack> loot = new ArrayList<>();
        loot.add(item(Material.DIAMOND_SWORD, 1));
        loot.add(item(Material.DIAMOND_CHESTPLATE, 1));
        loot.add(item(Material.DIAMOND_BOOTS, 1));
        loot.add(item(Material.GOLDEN_APPLE, upgraded ? 4 : 2));
        loot.add(item(Material.ENDER_PEARL, upgraded ? 4 : 2));
        loot.add(item(Material.BOW, 1));
        loot.add(item(Material.ARROW, 32));
        loot.add(item(Material.TNT, 4));
        loot.add(item(Material.OAK_PLANKS, 64));
        if (upgraded) loot.add(item(Material.ENCHANTED_GOLDEN_APPLE, 1));
        return loot;
    }

    private ItemStack item(Material material, int amount) { return new ItemStack(material, amount); }
}
