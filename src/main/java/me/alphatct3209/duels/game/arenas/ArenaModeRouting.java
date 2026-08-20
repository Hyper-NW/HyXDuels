package me.alphatct3209.duels.game.arenas;

import me.alphatct3209.duels.game.modes.ModeKey;

import java.util.Collection;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

/** Exact global matrix: assigned modes use listing arenas; globally unassigned modes use empty arenas. */
public final class ArenaModeRouting
{
    private ArenaModeRouting() {}

    public static boolean isCompatible(ModeKey mode, Collection<ModeKey> arenaModes,
                                       Collection<? extends Collection<ModeKey>> allArenaModes)
    {
        Objects.requireNonNull(mode, "mode");
        Objects.requireNonNull(arenaModes, "arenaModes");
        Objects.requireNonNull(allArenaModes, "allArenaModes");
        if (arenaModes.contains(mode)) return true;
        if (!arenaModes.isEmpty()) return false;
        Set<ModeKey> globallyAssigned = new HashSet<>();
        allArenaModes.forEach(globallyAssigned::addAll);
        return !globallyAssigned.contains(mode);
    }
}
