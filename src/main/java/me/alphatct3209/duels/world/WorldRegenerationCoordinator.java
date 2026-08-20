package me.alphatct3209.duels.world;

import me.alphatct3209.duels.game.GameState;

import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.Locale;
import java.util.Set;

/** Pure state coordinator for world-level admission locks and shared-map draining. */
public final class WorldRegenerationCoordinator
{
    private final Set<String> pending = new LinkedHashSet<>();

    public boolean request(String worldName)
    {
        return pending.add(canonical(worldName));
    }

    public boolean isPending(String worldName)
    {
        return worldName != null && pending.contains(canonical(worldName));
    }

    public boolean canAdmit(String worldName)
    {
        return !isPending(worldName);
    }

    public boolean canAdmit(String worldName, Object configuredWorldInstance, Object liveWorldInstance)
    {
        return canAdmit(worldName) && configuredWorldInstance != null
                && configuredWorldInstance == liveWorldInstance;
    }

    public boolean isDrainReady(String worldName, Collection<GameState> states)
    {
        return isPending(worldName) && states.stream().noneMatch(state -> state == GameState.PLAYING);
    }

    public void complete(String worldName)
    {
        pending.remove(canonical(worldName));
    }

    public Set<String> pendingWorlds()
    {
        return Set.copyOf(pending);
    }

    public static String canonical(String worldName)
    {
        if (worldName == null || worldName.isBlank())
        {
            throw new IllegalArgumentException("World name cannot be blank");
        }
        return worldName.trim().toLowerCase(Locale.ROOT);
    }
}
