package me.alphatct3209.duels.utils;

import org.bukkit.Material;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ItemSerializationUtilsTest {

    @Test
    void detectsStacksThatCannotBeSerializedByNbtApi() {
        assertEquals("", ItemSerializationUtils.serialize(null));
        assertTrue(ItemSerializationUtils.isEmpty(Material.AIR, 1));
        assertTrue(ItemSerializationUtils.isEmpty(Material.STONE, 0));
        assertFalse(ItemSerializationUtils.isEmpty(Material.STONE, 1));
    }

    @Test
    void deserializesEmptyMarkersAsEmptySlots() {
        assertNull(ItemSerializationUtils.deserialize(null));
        assertNull(ItemSerializationUtils.deserialize(""));
        assertNull(ItemSerializationUtils.deserialize("   "));
    }
}
