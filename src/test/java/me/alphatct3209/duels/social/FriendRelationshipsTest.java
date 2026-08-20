package me.alphatct3209.duels.social;

import org.junit.jupiter.api.Test;

import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FriendRelationshipsTest
{
    @Test
    void addAndRemoveAlwaysMutateBothSides()
    {
        FriendRelationships relationships = new FriendRelationships();
        UUID first = UUID.randomUUID();
        UUID second = UUID.randomUUID();
        relationships.add(first, second);

        assertTrue(relationships.contains(first, second));
        assertTrue(relationships.get(first).contains(second));
        assertTrue(relationships.get(second).contains(first));
        assertTrue(relationships.remove(second, first));
        assertFalse(relationships.contains(first, second));
    }

    @Test
    void loadingCanHealOneSidedDataAndSnapshotsCannotBeModified()
    {
        FriendRelationships relationships = new FriendRelationships();
        UUID first = UUID.randomUUID();
        UUID second = UUID.randomUUID();
        relationships.loadOneWay(first, second);
        assertFalse(relationships.contains(first, second));
        relationships.healSymmetry();
        assertTrue(relationships.contains(first, second));
        Set<UUID> snapshot = relationships.get(first);
        assertThrows(UnsupportedOperationException.class, () -> snapshot.remove(second));
    }

    @Test
    void selfFriendshipIsRejected()
    {
        FriendRelationships relationships = new FriendRelationships();
        UUID player = UUID.randomUUID();
        assertThrows(IllegalArgumentException.class, () -> relationships.add(player, player));
    }
}
