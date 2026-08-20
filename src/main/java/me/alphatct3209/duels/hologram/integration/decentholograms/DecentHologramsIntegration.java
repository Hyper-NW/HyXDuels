package me.alphatct3209.duels.hologram.integration.decentholograms;

import eu.decentsoftware.holograms.api.DHAPI;
import eu.decentsoftware.holograms.api.actions.Action;
import eu.decentsoftware.holograms.api.actions.ActionType;
import eu.decentsoftware.holograms.api.actions.ClickType;
import eu.decentsoftware.holograms.api.holograms.Hologram;
import eu.decentsoftware.holograms.api.holograms.HologramPage;
import eu.decentsoftware.holograms.event.HologramClickEvent;
import me.alphatct3209.duels.hologram.integration.HologramIntegration;
import me.alphatct3209.duels.hologram.integration.RuntimeHologram;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.plugin.Plugin;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.function.BiConsumer;

/** All direct DecentHolograms links are deliberately isolated in this package. */
public final class DecentHologramsIntegration implements HologramIntegration, Listener
{
    /** Exact runtime-created name -> exact object instance returned by DHAPI. */
    private final Map<String, Hologram> owned = new HashMap<>();
    private final BiConsumer<Player, String> clickHandler;

    public DecentHologramsIntegration(Plugin plugin, BiConsumer<Player, String> clickHandler)
    {
        this.clickHandler = clickHandler;
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
    }

    @Override
    public ReconcileResult reconcile(Collection<RuntimeHologram> desired)
    {
        Map<String, RuntimeHologram> wanted = new LinkedHashMap<>();
        desired.forEach(value -> wanted.put(value.name(), value));
        Set<String> created = new HashSet<>();
        Set<String> updated = new HashSet<>();
        Set<String> removed = new HashSet<>();
        Set<String> foreign = new HashSet<>();

        for (String name : Set.copyOf(owned.keySet()))
        {
            if (!wanted.containsKey(name) && removeIfStillOwned(name))
            {
                removed.add(name);
            }
        }

        for (RuntimeHologram definition : wanted.values())
        {
            String name = definition.name();
            Hologram current = DHAPI.getHologram(name);
            Hologram runtimeOwned = owned.get(name);
            if (runtimeOwned != null && current != runtimeOwned)
            {
                owned.remove(name);
                runtimeOwned = null;
            }
            if (current != null && runtimeOwned == null)
            {
                foreign.add(name);
                continue;
            }
            if (current == null)
            {
                // Explicit false is the contract: HyX config is authoritative, never DH YAML.
                current = DHAPI.createHologram(name, definition.location(), false, definition.lines());
                current.setUpdateInterval(definition.updateIntervalTicks());
                makeClickable(current);
                owned.put(name, current);
                created.add(name);
                continue;
            }

            DHAPI.moveHologram(current, definition.location());
            DHAPI.setHologramLines(current, definition.lines());
            current.setUpdateInterval(definition.updateIntervalTicks());
            makeClickable(current);
            updated.add(name);
        }
        return new ReconcileResult(created, updated, removed, foreign);
    }

    @Override
    public void shutdown()
    {
        Throwable firstFailure = null;
        for (String name : Set.copyOf(owned.keySet()))
        {
            try
            {
                removeIfStillOwned(name);
            }
            catch (RuntimeException | LinkageError failure)
            {
                if (firstFailure == null)
                {
                    firstFailure = failure;
                }
            }
        }
        owned.clear();
        HandlerList.unregisterAll(this);
        if (firstFailure instanceof RuntimeException runtime)
        {
            throw runtime;
        }
        if (firstFailure instanceof LinkageError linkage)
        {
            throw linkage;
        }
    }

    @EventHandler
    public void onHologramClick(HologramClickEvent event)
    {
        if (event.getClick() != ClickType.RIGHT && event.getClick() != ClickType.SHIFT_RIGHT) return;
        Hologram expected = owned.get(event.getHologram().getName());
        if (expected == null || expected != event.getHologram()) return;
        event.setCancelled(true);
        clickHandler.accept(event.getPlayer(), event.getHologram().getName());
    }

    private void makeClickable(Hologram hologram)
    {
        HologramPage page = hologram.getPage(0);
        if (page == null) return;
        page.clearActions(ClickType.RIGHT);
        page.clearActions(ClickType.SHIFT_RIGHT);
        page.addAction(ClickType.RIGHT, new Action(ActionType.NONE, ""));
        page.addAction(ClickType.SHIFT_RIGHT, new Action(ActionType.NONE, ""));
    }

    @Override
    public int ownedCount()
    {
        // Drop ownership if DH reloaded/deleted/replaced one of our transient objects.
        for (Map.Entry<String, Hologram> entry : Set.copyOf(owned.entrySet()))
        {
            if (DHAPI.getHologram(entry.getKey()) != entry.getValue())
            {
                owned.remove(entry.getKey());
            }
        }
        return owned.size();
    }

    private boolean removeIfStillOwned(String name)
    {
        Hologram expected = owned.remove(name);
        if (expected == null || DHAPI.getHologram(name) != expected)
        {
            return false;
        }
        DHAPI.removeHologram(name);
        return true;
    }
}
