package me.alphatct3209.duels.game.modes.bedwars;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class BedWarsLoadoutTest
{
    @Test
    void permanentPurchasesSurviveAndToolsDowngradeOneTierOnDeath()
    {
        BedWarsLoadout loadout = new BedWarsLoadout();
        assertTrue(loadout.purchase(BedWarsUpgrade.IRON_ARMOR));
        assertTrue(loadout.purchase(BedWarsUpgrade.DIAMOND_PICKAXE));
        assertTrue(loadout.purchase(BedWarsUpgrade.SHEARS));
        assertFalse(loadout.purchase(BedWarsUpgrade.CHAINMAIL_ARMOR));
        assertFalse(loadout.purchase(BedWarsUpgrade.SHEARS));

        loadout.afterDeath();
        assertEquals("IRON", loadout.armorTier());
        assertEquals("IRON", loadout.pickaxeTier());
        assertTrue(loadout.hasShears());
    }
}
