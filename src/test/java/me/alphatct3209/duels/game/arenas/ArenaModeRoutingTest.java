package me.alphatct3209.duels.game.arenas;

import me.alphatct3209.duels.game.modes.ModeKey;
import me.alphatct3209.duels.game.modes.ModeQueueClaim;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class ArenaModeRoutingTest
{
    @Test
    void preservesTheExactGlobalAssignedUnassignedMatrix()
    {
        ModeKey classic = ModeKey.parse("classic");
        ModeKey bow = ModeKey.parse("bow");
        ModeKey sumo = ModeKey.parse("sumo");
        Set<ModeKey> empty = Set.of();
        Set<ModeKey> bowOnly = Set.of(bow);
        Set<ModeKey> bowSumo = Set.of(bow, sumo);
        List<Set<ModeKey>> routes = List.of(empty, bowOnly, bowSumo);
        assertTrue(ArenaModeRouting.isCompatible(classic, empty, routes));
        assertFalse(ArenaModeRouting.isCompatible(classic, bowOnly, routes));
        assertFalse(ArenaModeRouting.isCompatible(bow, empty, routes));
        assertTrue(ArenaModeRouting.isCompatible(bow, bowOnly, routes));
        assertTrue(ArenaModeRouting.isCompatible(bow, bowSumo, routes));
        assertFalse(ArenaModeRouting.isCompatible(sumo, bowOnly, routes));
        assertTrue(ArenaModeRouting.isCompatible(sumo, bowSumo, routes));
    }

    @Test
    void firstEntrantClaimsModeAndMismatchIsRejectedUntilQueueEmpties()
    {
        ModeQueueClaim claim = new ModeQueueClaim();
        ModeKey classic = ModeKey.parse("classic");
        ModeKey bow = ModeKey.parse("bow");
        assertTrue(claim.admit(classic));
        assertEquals(classic, claim.claimedMode().orElseThrow());
        assertFalse(claim.canAdmit(bow));
        assertFalse(claim.admit(bow));
        assertTrue(claim.admit(classic));
        assertFalse(claim.canAdmit(classic));
        claim.leave(); claim.leave();
        assertTrue(claim.claimedMode().isEmpty());
        assertTrue(claim.admit(bow));
    }
}
