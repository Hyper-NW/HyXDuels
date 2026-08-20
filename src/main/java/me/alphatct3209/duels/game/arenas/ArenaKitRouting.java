package me.alphatct3209.duels.game.arenas;

import java.util.Collection;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

/** Pure routing rules for canonical kit gamemode keys. */
public final class ArenaKitRouting
{
    private ArenaKitRouting()
    {
    }

    /**
     * Listed kits can only use arenas listing them. An arena with no listed kits
     * is the pool for kits that are not listed by any arena.
     */
    public static boolean isCompatible(String kitKey, Collection<String> arenaKeys,
                                       Collection<? extends Collection<String>> allArenaKeys)
    {
        Objects.requireNonNull(kitKey, "kitKey");
        Objects.requireNonNull(arenaKeys, "arenaKeys");
        Objects.requireNonNull(allArenaKeys, "allArenaKeys");
        if (arenaKeys.contains(kitKey))
        {
            return true;
        }
        if (!arenaKeys.isEmpty())
        {
            return false;
        }

        Set<String> globallyAssigned = new HashSet<>();
        for (Collection<String> keys : allArenaKeys)
        {
            globallyAssigned.addAll(keys);
        }
        return !globallyAssigned.contains(kitKey);
    }
}
