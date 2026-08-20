package me.alphatct3209.duels.social.gui;

import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;

import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import me.alphatct3209.duels.social.PlayerSetting;

public final class SettingsInventoryHolder implements InventoryHolder
{
    private final UUID token;
    private final UUID viewer;
    private final Map<Integer, PlayerSetting> choices;
    private Inventory inventory;

    SettingsInventoryHolder(UUID token, UUID viewer, Map<Integer, PlayerSetting> choices)
    {
        this.token = Objects.requireNonNull(token);
        this.viewer = Objects.requireNonNull(viewer);
        this.choices = java.util.Collections.unmodifiableMap(Objects.requireNonNull(choices));
    }

    void attach(Inventory inventory) { this.inventory = inventory; }
    @Override public Inventory getInventory() { return inventory; }
    public UUID token() { return token; }
    public UUID viewer() { return viewer; }
    public Map<Integer, PlayerSetting> choices() { return choices; }
}
