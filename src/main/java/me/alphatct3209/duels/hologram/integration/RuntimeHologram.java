package me.alphatct3209.duels.hologram.integration;

import org.bukkit.Location;

import java.util.List;
import java.util.Objects;

public record RuntimeHologram(String name, Location location, int updateIntervalTicks,
                              List<String> lines)
{
    public RuntimeHologram
    {
        name = Objects.requireNonNull(name, "name");
        location = Objects.requireNonNull(location, "location").clone();
        lines = List.copyOf(lines);
    }

    @Override
    public Location location()
    {
        return location.clone();
    }
}
