package me.alphatct3209.duels.game.kits;

import org.bukkit.inventory.ItemStack;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class KitSchemaTest
{
    @Test
    void missingVersionIsLegacyAndKnownVersionsRemainDistinct()
    {
        assertEquals(KitFormat.LEGACY_V1, KitSchema.readFormat(false, null, "Kits.4"));
        assertEquals(KitFormat.LEGACY_V1, KitSchema.readFormat(true, 1, "Kits.4"));
        assertEquals(KitFormat.POSITIONAL_V2, KitSchema.readFormat(true, 2, "Kits.4"));

        Kit legacy = new Kit(4, "Legacy", null, null);
        Kit positional = Kit.positionalV2(5, "Modern", null, new ItemStack[36], null);
        assertEquals(KitFormat.LEGACY_V1, legacy.getFormat());
        assertEquals(KitFormat.POSITIONAL_V2, positional.getFormat());
        assertNull(positional.getOffhand());
    }

    @Test
    void rejectsUnknownOrMalformedVersionsWithTheVersionPath()
    {
        IllegalArgumentException unknown = assertThrows(IllegalArgumentException.class,
                () -> KitSchema.readFormat(true, 3, "Kits.7"));
        assertTrue(unknown.getMessage().contains("Kits.7.Format-Version"));
        assertTrue(unknown.getMessage().contains("unknown version 3"));

        IllegalArgumentException malformed = assertThrows(IllegalArgumentException.class,
                () -> KitSchema.readFormat(true, "2", "Kits.named"));
        assertTrue(malformed.getMessage().contains("Kits.named.Format-Version"));
    }

    @Test
    void v2RequiresExactlyThirtySixPositionalEntries()
    {
        KitSchema.requireV2StorageSize(36, "Kits.2");

        for (int size : new int[]{0, 35, 37})
        {
            IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                    () -> KitSchema.requireV2StorageSize(size, "Kits.2"));
            assertTrue(exception.getMessage().contains("Kits.2.Inventory"));
            assertTrue(exception.getMessage().contains("exactly 36"));
        }

        assertThrows(IllegalArgumentException.class,
                () -> Kit.positionalV2(2, "Invalid", null, new ItemStack[35], null));
    }

    @Test
    void returnedArraysAreDefensiveCopies()
    {
        ItemStack[] storage = new ItemStack[36];
        Kit kit = Kit.positionalV2(2, "Modern", null, storage, null);

        ItemStack[] firstInventoryRead = kit.getInventoryContents();
        ItemStack[] secondInventoryRead = kit.getInventoryContents();
        ItemStack[] firstArmorRead = kit.getArmorContents();
        ItemStack[] secondArmorRead = kit.getArmorContents();

        assertEquals(36, firstInventoryRead.length);
        assertEquals(4, firstArmorRead.length);
        assertTrue(firstInventoryRead != secondInventoryRead);
        assertTrue(firstArmorRead != secondArmorRead);
    }
}
