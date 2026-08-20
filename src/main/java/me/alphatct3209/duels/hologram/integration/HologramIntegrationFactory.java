package me.alphatct3209.duels.hologram.integration;

import me.alphatct3209.duels.hologram.integration.decentholograms.DecentHologramsIntegration;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

import java.util.function.BiConsumer;

public final class HologramIntegrationFactory
{
    private HologramIntegrationFactory() {}

    /** Call only after both PlaceholderAPI and DecentHolograms are confirmed enabled. */
    public static HologramIntegration createDecentHolograms(
            Plugin plugin, BiConsumer<Player, String> clickHandler)
    {
        return new DecentHologramsIntegration(plugin, clickHandler);
    }
}
