package me.alphatct3209.duels.social;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

final class FriendRelationships
{
    private final Map<UUID, Set<UUID>> byPlayer = new HashMap<>();

    boolean contains(UUID first, UUID second)
    {
        return first != null && second != null && byPlayer.getOrDefault(first, Set.of()).contains(second)
                && byPlayer.getOrDefault(second, Set.of()).contains(first);
    }

    Set<UUID> get(UUID player) { return Set.copyOf(byPlayer.getOrDefault(player, Set.of())); }

    void add(UUID first, UUID second)
    {
        if (first == null || second == null || first.equals(second))
            throw new IllegalArgumentException("Friendships require two different players");
        mutable(first).add(second);
        mutable(second).add(first);
    }

    boolean remove(UUID first, UUID second)
    {
        boolean changed = mutable(first).remove(second);
        changed |= mutable(second).remove(first);
        return changed;
    }

    void loadOneWay(UUID player, UUID friend) { mutable(player).add(friend); }

    void healSymmetry()
    {
        for (Map.Entry<UUID, Set<UUID>> entry : new ArrayList<>(byPlayer.entrySet()))
            for (UUID friend : List.copyOf(entry.getValue()))
                if (!entry.getKey().equals(friend)) mutable(friend).add(entry.getKey());
    }

    private Set<UUID> mutable(UUID player)
    {
        return byPlayer.computeIfAbsent(player, ignored -> new LinkedHashSet<>());
    }
}
