package me.alphatct3209.duels.hologram;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

class HologramDefinitionTest
{
    private static final HologramLocation LOCATION = new HologramLocation("world", 1, 2, 3, 0, 0);

    @Test
    void validatesIdsNamesModesIntervalsCoordinatesAndLines()
    {
        assertDoesNotThrow(() -> new HologramDefinition("ranked", "hyxduels_ranked",
                HologramDefinition.Type.DIVISIONS, "no_debuff", LOCATION, 20, List.of("line")));
        assertThrows(IllegalArgumentException.class, () -> definition("Bad Id", 20, List.of("line")));
        assertThrows(IllegalArgumentException.class, () -> new HologramDefinition("ok", "bad name!",
                HologramDefinition.Type.WINS, null, LOCATION, 20, List.of("line")));
        assertThrows(IllegalArgumentException.class, () -> definition("ok", 19, List.of("line")));
        assertThrows(IllegalArgumentException.class, () -> definition("ok", 72_001, List.of("line")));
        assertThrows(IllegalArgumentException.class, () -> definition("ok", 20, List.of()));
        assertThrows(IllegalArgumentException.class, () -> definition("ok", 20, List.of("")));
        assertThrows(IllegalArgumentException.class, () -> new HologramLocation("world",
                Double.NaN, 0, 0, 0, 0));
    }

    private HologramDefinition definition(String id, int interval, List<String> lines)
    {
        return new HologramDefinition(id, "hyxduels_ok", HologramDefinition.Type.WINS,
                null, LOCATION, interval, lines);
    }
}
