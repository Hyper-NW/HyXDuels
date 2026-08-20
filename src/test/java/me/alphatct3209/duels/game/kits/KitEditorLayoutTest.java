package me.alphatct3209.duels.game.kits;

import org.bukkit.inventory.ItemStack;
import org.junit.jupiter.api.Test;

import java.util.UUID;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertFalse;

class KitEditorLayoutTest
{
    @Test
    void legacyLayoutsNormalizeToThirtySixSlotsAndSnapshotsAreDefensive()
    {
        Kit legacy = new Kit(3, "Legacy", new ItemStack[4], new ItemStack[2]);
        KitEditorInventoryHolder holder = new KitEditorInventoryHolder(
                UUID.randomUUID(), UUID.randomUUID(), legacy);

        ItemStack[] first = holder.originalStorage();
        ItemStack[] second = holder.originalStorage();
        assertEquals(36, first.length);
        assertTrue(first != second);
        assertNull(first[35]);
        assertEquals(4, holder.originalArmor().length);
    }

    @Test
    void configuredKitsRetainIdsAndBuiltInsReceiveTheNextPositiveId()
    {
        Kit configured = Kit.positionalV2(7, "Configured", null, new ItemStack[36], null);
        Kit builtIn = Kit.positionalV2(-4, "Built In", null, new ItemStack[36], null);
        assertEquals(7, KitManager.layoutPersistenceId(configured, 12));
        assertEquals(12, KitManager.layoutPersistenceId(builtIn, 12));
        assertThrows(IllegalArgumentException.class,
                () -> KitManager.layoutPersistenceId(builtIn, 0));
    }

    @Test
    void personalLayoutsAcceptOnlyRearrangementsOfTheSharedKit()
    {
        assertTrue(PlayerKitLayoutManager.sameSerializedItems(
                List.of("sword", "golden-apple:3", "bow"),
                List.of("bow", "sword", "golden-apple:3")));
        assertFalse(PlayerKitLayoutManager.sameSerializedItems(
                List.of("sword", "golden-apple:3", "bow"),
                List.of("bow", "sword", "golden-apple:4")));
    }
}
