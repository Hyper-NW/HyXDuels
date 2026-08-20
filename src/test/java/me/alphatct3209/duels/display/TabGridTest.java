package me.alphatct3209.duels.display;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TabGridTest
{
    @Test
    void firstDelimiterIsStructuralAndEachColumnAdvancesIndependently()
    {
        List<List<String>> columns = TabGridParser.parse(List.of(
                "2|B1|preserved", "1|A1", "1|A2", "4|D1"));
        assertEquals(List.of("A1", "A2"), columns.get(0));
        assertEquals(List.of("B1|preserved"), columns.get(1));
        assertEquals(List.of(), columns.get(2));
        assertEquals(List.of("D1"), columns.get(3));

        List<String> cells = TabGridLayout.cells(columns);
        assertEquals(80, cells.size());
        assertEquals("A1", cells.get(0));
        assertEquals("A2", cells.get(1));
        assertEquals("B1|preserved", cells.get(20));
        assertEquals("", cells.get(40));
        assertEquals("D1", cells.get(60));
    }

    @Test
    void unusedRowsAreBlankFakePlayers()
    {
        List<String> cells = TabGridLayout.cells(TabGridParser.parse(List.of("1|§aAB", "2|C")));
        assertEquals("§aAB", cells.get(0));
        assertEquals("", cells.get(19));
        assertEquals("C", cells.get(20));
        assertEquals("", cells.get(79));
    }

    @Test
    void validatesDelimiterColumnCountAndTwentyRowsPerColumn()
    {
        assertThrows(IllegalArgumentException.class, () -> TabGridParser.parse(List.of("1 no pipe")));
        assertThrows(IllegalArgumentException.class, () -> TabGridParser.parse(List.of("5|bad")));
        assertThrows(IllegalArgumentException.class,
                () -> TabGridLayout.cells(List.of(List.of())));

        List<String> tooMany = new ArrayList<>();
        for (int row = 0; row < 21; row++)
        {
            tooMany.add("3|row" + row);
        }
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> TabGridParser.parse(tooMany));
        assertTrue(exception.getMessage().contains("20 rows"));
    }
}
