package me.alphatct3209.duels.game.arenas;

import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ArenaObjectiveSettingsTest
{
    @Test
    void authenticModeTimingsHaveStableDefaults()
    {
        ArenaObjectiveSettings settings = new ArenaObjectiveSettings();
        assertEquals(5, settings.bedWarsRespawnSeconds());
        assertEquals(20, settings.bedWarsIronGeneratorTicks());
        assertEquals(80, settings.bedWarsGoldGeneratorTicks());
        assertEquals(600, settings.bedWarsDiamondGeneratorTicks());
        assertEquals(1200, settings.bedWarsEmeraldGeneratorTicks());
        assertEquals(300, settings.skyWarsRefillSeconds());
    }

    @Test
    void authenticModeTimingsAcceptPerArenaOverrides()
    {
        ArenaObjectiveSettings settings = new ArenaObjectiveSettings(Map.of(
                "bedwars-respawn-seconds", 3,
                "bedwars-iron-generator-ticks", 10,
                "skywars-refill-seconds", 120));
        assertEquals(3, settings.bedWarsRespawnSeconds());
        assertEquals(10, settings.bedWarsIronGeneratorTicks());
        assertEquals(120, settings.skyWarsRefillSeconds());
    }
}
