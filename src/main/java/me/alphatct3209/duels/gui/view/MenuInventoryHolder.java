package me.alphatct3209.duels.gui.view;

import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;

import java.util.Map;
import java.util.Objects;
import java.util.UUID;

public final class MenuInventoryHolder implements InventoryHolder
{
    private final UUID sessionToken;
    private final UUID viewer;
    private final MenuView view;
    private final int page;
    private final int pageCount;
    private final Map<Integer, String> choices;
    private Inventory inventory;

    public MenuInventoryHolder(UUID sessionToken, UUID viewer, MenuView view,
                               int page, int pageCount, Map<Integer, String> choices)
    {
        this.sessionToken = Objects.requireNonNull(sessionToken, "sessionToken");
        this.viewer = Objects.requireNonNull(viewer, "viewer");
        this.view = Objects.requireNonNull(view, "view");
        this.page = page;
        this.pageCount = pageCount;
        this.choices = java.util.Collections.unmodifiableMap(
                Objects.requireNonNull(choices, "choices"));
    }

    public void attach(Inventory inventory) { this.inventory = inventory; }
    @Override public Inventory getInventory() { return inventory; }
    public UUID sessionToken() { return sessionToken; }
    public UUID viewer() { return viewer; }
    public MenuView view() { return view; }
    public int page() { return page; }
    public int pageCount() { return pageCount; }
    public Map<Integer, String> choices() { return choices; }
}
