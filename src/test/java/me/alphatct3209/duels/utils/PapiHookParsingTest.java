package me.alphatct3209.duels.utils;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PapiHookParsingTest
{
    @Test
    void matchesOnlyExactAggregateLeaderboardPlaceholders()
    {
        assertTrue(PapiHook.isAggregateTopPlaceholder("top_wins_1", "wins"));
        assertTrue(PapiHook.isAggregateTopPlaceholder("top_kills_10", "kills"));
        assertFalse(PapiHook.isAggregateTopPlaceholder("top_wins_mode_wins_1", "wins"));
        assertFalse(PapiHook.isAggregateTopPlaceholder("top_kills_mode_division_1", "kills"));
        assertFalse(PapiHook.isAggregateTopPlaceholder("top_wins_wins_1", "wins"));
        assertFalse(PapiHook.isAggregateTopPlaceholder("top_kills_kills_1", "kills"));
    }

    @Test
    void parsesReservedAndCompoundGamemodeKeysUsingTheLastMarker()
    {
        assertParsed("wins", "1", "top_wins_wins_1", "_wins_");
        assertParsed("kills", "2", "top_kills_division_2", "_division_");
        assertParsed("wins_mode", "3", "top_wins_mode_wins_3", "_wins_");
        assertParsed("kills_mode", "4", "top_kills_mode_division_4", "_division_");
        assertParsed("sword_and-shield", "10", "top_sword_and-shield_wins_10", "_wins_");
    }

    private void assertParsed(String gamemode, String place, String params, String marker)
    {
        PapiHook.ParsedGamemodeTop parsed = PapiHook.parseGamemodeTop(
                params, params.toLowerCase(), marker);
        assertEquals(gamemode, parsed.gamemode());
        assertEquals(place, parsed.place());
    }
}
