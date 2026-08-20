package me.alphatct3209.duels.game.kits;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class GamemodeKeyTest
{
    @Test
    void createsCanonicalSafeKeysFromKitNames()
    {
        assertEquals("uhc_classic", GamemodeKey.fromKitName("  UHC Classic!  "));
        assertEquals("debuff_2v2", GamemodeKey.fromKitName("Debuff---2v2"));
        assertEquals("epee", GamemodeKey.fromKitName("Épée"));
    }

    @Test
    void rejectsNamesWithoutAsciiLettersOrNumbers()
    {
        assertThrows(IllegalArgumentException.class, () -> GamemodeKey.fromKitName("---"));
        assertThrows(NullPointerException.class, () -> GamemodeKey.fromKitName(null));
    }

    @Test
    void rejectsKitNamesThatWouldShareAProgressionKey()
    {
        Kit existing = new Kit(1, "UHC Classic", null, null);

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> KitManager.requireAvailableGamemodeKey("UHC-Classic", List.of(existing)));

        assertTrue(exception.getMessage().contains("uhc_classic"));
        assertTrue(exception.getMessage().contains("UHC Classic"));
    }
}
