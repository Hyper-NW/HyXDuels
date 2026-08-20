package me.alphatct3209.duels.hologram;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class HologramGeneratedLinesTest
{
    @Test
    void generatedWinsBoardIsViewerFilteredAndClickable()
    {
        List<String> lines = HologramManager.generatedLines(HologramDefinition.Type.WINS, null);
        assertEquals(13, lines.size());
        assertTrue(lines.get(1).contains("%duels_flb_wins_all_filter%"));
        assertTrue(lines.get(2).contains("%duels_flb_wins_all_1_player%"));
        assertTrue(lines.getLast().contains("Right-click to filter"));
    }

    @Test
    void generatedDivisionBoardRetainsItsModeFallback()
    {
        List<String> lines = HologramManager.generatedLines(
                HologramDefinition.Type.DIVISIONS, "mega_walls");
        assertTrue(lines.get(1).contains("flb_divisions_mega_walls_filter"));
        assertTrue(lines.get(2).contains("flb_divisions_mega_walls_1_division"));
    }
}
