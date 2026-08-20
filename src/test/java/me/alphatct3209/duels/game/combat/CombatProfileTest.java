package me.alphatct3209.duels.game.combat;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class CombatProfileTest
{
    @Test
    void modernCombatUsesVanillaCooldownAndInvulnerability()
    {
        assertEquals(new CombatProfile(4D, 20), CombatProfile.resolve(false, false));
    }

    @Test
    void legacyCombatRemovesAttackCooldownButKeepsNormalHitTicks()
    {
        assertEquals(new CombatProfile(24D, 20), CombatProfile.resolve(true, false));
    }

    @Test
    void noHitDelayTakesPrecedenceOverLegacyInvulnerability()
    {
        assertEquals(new CombatProfile(24D, 0), CombatProfile.resolve(true, true));
        assertEquals(new CombatProfile(24D, 0), CombatProfile.resolve(false, true));
    }
}
