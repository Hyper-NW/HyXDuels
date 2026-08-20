package me.alphatct3209.duels.game.arenas;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ArenaKitRoutingTest
{
    @Test
    void completeCompatibilityMatrixSeparatesAssignedAndUnassignedPools()
    {
        Set<String> empty = Set.of();
        Set<String> swordOnly = Set.of("sword");
        Set<String> swordAndBow = Set.of("sword", "bow");
        List<Set<String>> routes = List.of(empty, swordOnly, swordAndBow);

        record Case(String kit, Set<String> arena, boolean compatible) {}
        List<Case> matrix = List.of(
                new Case("default", empty, true),
                new Case("default", swordOnly, false),
                new Case("default", swordAndBow, false),
                new Case("sword", empty, false),
                new Case("sword", swordOnly, true),
                new Case("sword", swordAndBow, true),
                new Case("bow", empty, false),
                new Case("bow", swordOnly, false),
                new Case("bow", swordAndBow, true));

        for (Case testCase : matrix)
        {
            assertEquals(testCase.compatible(), ArenaKitRouting.isCompatible(
                    testCase.kit(), testCase.arena(), routes), testCase.toString());
        }
    }

    @Test
    void allEmptyOrMissingRoutesFormTheUnassignedPool()
    {
        List<Set<String>> routes = List.of(Set.of(), Set.of());
        assertEquals(true, ArenaKitRouting.isCompatible("default", routes.get(0), routes));
        assertEquals(true, ArenaKitRouting.isCompatible("default", routes.get(1), routes));
    }
}
