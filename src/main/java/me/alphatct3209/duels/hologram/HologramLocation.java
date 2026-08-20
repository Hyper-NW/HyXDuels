package me.alphatct3209.duels.hologram;

import java.util.Objects;

/** Bukkit-independent persisted location. */
public record HologramLocation(String world, double x, double y, double z, float yaw, float pitch)
{
    public HologramLocation
    {
        world = Objects.requireNonNull(world, "world").trim();
        if (world.isEmpty())
        {
            throw new IllegalArgumentException("world must not be empty");
        }
        if (!Double.isFinite(x) || !Double.isFinite(y) || !Double.isFinite(z)
                || !Float.isFinite(yaw) || !Float.isFinite(pitch))
        {
            throw new IllegalArgumentException("location coordinates must be finite");
        }
    }
}
