package me.alphatct3209.duels.game.modes.runtime;

import me.alphatct3209.duels.game.arenas.Arena;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Item;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;

import java.util.function.BooleanSupplier;

/** Owns the 1v1 Bed Wars resource generators for one match. */
public final class BedWarsRuntime extends TrackedModeRuntime
{
    private static final NamespacedKey ARENA_KEY = new NamespacedKey("hyxduels", "arena_id");
    private final Arena arena;
    private final BooleanSupplier active;

    public BedWarsRuntime(Arena arena, BooleanSupplier active)
    {
        this.arena = arena;
        this.active = active;
    }

    @Override
    public void start()
    {
        int iron = arena.getObjectiveSettings().bedWarsIronGeneratorTicks();
        int gold = arena.getObjectiveSettings().bedWarsGoldGeneratorTicks();
        generator("generator_1", Material.IRON_INGOT, iron);
        generator("generator_1", Material.GOLD_INGOT, gold);
        generator("generator_2", Material.IRON_INGOT, iron);
        generator("generator_2", Material.GOLD_INGOT, gold);
        optionalGenerators("diamond_generator", Material.DIAMOND,
                arena.getObjectiveSettings().bedWarsDiamondGeneratorTicks());
        optionalGenerators("emerald_generator", Material.EMERALD,
                arena.getObjectiveSettings().bedWarsEmeraldGeneratorTicks());
    }

    private void optionalGenerators(String prefix, Material material, int period)
    {
        arena.getPoints().keySet().stream()
                .filter(name -> name.equals(prefix) || name.matches(prefix + "_[1-9][0-9]*"))
                .forEach(name -> generator(name, material, period));
    }

    private void generator(String point, Material material, int period)
    {
        Location location = arena.getPoint(point).orElseThrow(
                () -> new IllegalStateException("Missing generator marker " + point));
        track(arena.scheduleRepeating(() -> {
            if (!active.getAsBoolean()) return;
            Item item = location.getWorld().dropItem(location.clone().add(0D, 0.25D, 0D),
                    new ItemStack(material));
            item.setPickupDelay(0);
            item.getPersistentDataContainer().set(ARENA_KEY, PersistentDataType.INTEGER, arena.getId());
            arena.trackTransient(item);
        }, period, period));
    }
}
