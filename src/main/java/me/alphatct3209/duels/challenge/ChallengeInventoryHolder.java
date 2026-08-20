package me.alphatct3209.duels.challenge;

import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;

import java.util.Map;
import java.util.Objects;
import java.util.UUID;

public final class ChallengeInventoryHolder implements InventoryHolder
{
    private final UUID sessionToken;
    private final UUID viewer;
    private final UUID target;
    private final int page;
    private final int pageCount;
    private final Map<Integer, String> kitSlots;
    private Inventory inventory;

    ChallengeInventoryHolder(UUID sessionToken, UUID viewer, UUID target, int page,
                             int pageCount, Map<Integer, String> kitSlots)
    {
        this.sessionToken = Objects.requireNonNull(sessionToken, "sessionToken");
        this.viewer = Objects.requireNonNull(viewer, "viewer");
        this.target = Objects.requireNonNull(target, "target");
        this.page = page;
        this.pageCount = pageCount;
        this.kitSlots = Map.copyOf(kitSlots);
    }

    void attach(Inventory inventory)
    {
        if (this.inventory != null)
        {
            throw new IllegalStateException("Inventory already attached");
        }
        this.inventory = Objects.requireNonNull(inventory, "inventory");
    }

    public UUID sessionToken() { return sessionToken; }
    public UUID viewer() { return viewer; }
    public UUID target() { return target; }
    public int page() { return page; }
    public int pageCount() { return pageCount; }
    public Map<Integer, String> kitSlots() { return kitSlots; }

    @Override
    public Inventory getInventory()
    {
        return inventory;
    }
}
