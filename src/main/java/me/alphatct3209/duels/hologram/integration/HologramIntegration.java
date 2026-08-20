package me.alphatct3209.duels.hologram.integration;

import java.util.Collection;
import java.util.Set;

/** Optional runtime adapter contract; implementations own only objects they created. */
public interface HologramIntegration
{
    ReconcileResult reconcile(Collection<RuntimeHologram> desired);
    void shutdown();
    int ownedCount();

    record ReconcileResult(Set<String> created, Set<String> updated,
                           Set<String> removed, Set<String> foreignConflicts)
    {
        public ReconcileResult
        {
            created = Set.copyOf(created);
            updated = Set.copyOf(updated);
            removed = Set.copyOf(removed);
            foreignConflicts = Set.copyOf(foreignConflicts);
        }
    }
}
