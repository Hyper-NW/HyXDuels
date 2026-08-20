package me.alphatct3209.duels.party.gui;

import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;

import java.util.Map;
import java.util.Objects;
import java.util.UUID;

public final class PartyInventoryHolder implements InventoryHolder
{
    public enum View { MAIN, INVITE }

    private final UUID token;
    private final UUID viewer;
    private final View view;
    private final int page;
    private final int pages;
    private final Map<Integer, String> choices;
    private Inventory inventory;

    public PartyInventoryHolder(UUID token, UUID viewer, View view, int page, int pages,
                                Map<Integer, String> choices)
    {
        this.token = Objects.requireNonNull(token);
        this.viewer = Objects.requireNonNull(viewer);
        this.view = Objects.requireNonNull(view);
        this.page = page;
        this.pages = pages;
        this.choices = Map.copyOf(choices);
    }

    public void attach(Inventory inventory) { this.inventory = inventory; }
    @Override public Inventory getInventory() { return inventory; }
    public UUID token() { return token; }
    public UUID viewer() { return viewer; }
    public View view() { return view; }
    public int page() { return page; }
    public int pages() { return pages; }
    public Map<Integer, String> choices() { return choices; }
}
