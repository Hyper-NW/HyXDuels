package me.alphatct3209.duels.gui.config;

import org.bukkit.Material;

import java.util.List;
import java.util.Objects;

public record MenuOpener(String id, boolean enabled, Material material, int slot,
                         String name, List<String> lore, MenuAction action,
                         boolean glow, boolean locked, boolean forceSlot,
                         Integer customModelData)
{
    public MenuOpener
    {
        if (id == null || !id.matches("[a-z0-9_-]+"))
        {
            throw new IllegalArgumentException("Menu item id must use lowercase letters, numbers, '_' or '-'");
        }
        Objects.requireNonNull(material, "material");
        Objects.requireNonNull(name, "name");
        Objects.requireNonNull(action, "action");
        lore = List.copyOf(Objects.requireNonNull(lore, "lore"));
        if (slot < 0 || slot > 8)
        {
            throw new IllegalArgumentException("Menu opener slot must be from 0 through 8");
        }
        if (customModelData != null && customModelData < 0)
        {
            throw new IllegalArgumentException("Custom-Model-Data cannot be negative");
        }
    }
}
