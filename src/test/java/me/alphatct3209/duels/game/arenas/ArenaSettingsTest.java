package me.alphatct3209.duels.game.arenas;

import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ArenaSettingsTest
{
    @Test
    void missingConfigurationUsesSafeDefaultsForEveryFlag()
    {
        ArenaSettings settings = ArenaSettings.fromMap(Map.of());
        assertFalse(settings.get(ArenaSettings.Flag.BLOCK_BREAK));
        assertFalse(settings.get(ArenaSettings.Flag.BLOCK_PLACE));
        assertFalse(settings.get(ArenaSettings.Flag.ENTITY_PLACEMENT));
        assertTrue(settings.get(ArenaSettings.Flag.EXPLOSIONS));
        assertFalse(settings.get(ArenaSettings.Flag.EXPLOSION_BLOCK_DAMAGE));
        assertFalse(settings.get(ArenaSettings.Flag.FIRE_SPREAD));
        assertFalse(settings.get(ArenaSettings.Flag.ITEM_DROP));
        assertFalse(settings.get(ArenaSettings.Flag.ITEM_PICKUP));
    }

    @Test
    void configuredBooleansOverrideIndependentlyAndMalformedValuesStaySafe()
    {
        ArenaSettings settings = ArenaSettings.fromMap(Map.of(
                "block-break", true,
                "explosions", false,
                "item-drop", "not-a-boolean"));
        assertTrue(settings.get(ArenaSettings.Flag.BLOCK_BREAK));
        assertFalse(settings.get(ArenaSettings.Flag.EXPLOSIONS));
        assertFalse(settings.get(ArenaSettings.Flag.ITEM_DROP));
        assertFalse(settings.get(ArenaSettings.Flag.BLOCK_PLACE));
    }

    @Test
    void flagsUseStableConfigAndCommandKeys()
    {
        for (ArenaSettings.Flag flag : ArenaSettings.Flag.values())
        {
            assertEquals(flag, ArenaSettings.Flag.parse(flag.key().toUpperCase()).orElseThrow());
        }
        assertTrue(ArenaSettings.Flag.parse("unknown").isEmpty());
    }
}
