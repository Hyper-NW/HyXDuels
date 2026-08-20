package me.alphatct3209.duels.hologram;

import org.junit.jupiter.api.Test;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;

class HologramConfigParserTest
{
    @Test
    void defaultConfigurationIsDisabledAndEmpty()
    {
        HologramConfig parsed = HologramConfigParser.parse(Map.of());
        assertFalse(parsed.enabled());
        assertEquals(100, parsed.defaultUpdateIntervalTicks());
        assertEquals(Map.of(), parsed.definitions());
    }

    @Test
    void parsesStrictManagedDefinitionAndRejectsDuplicateNames()
    {
        Map<String, Object> first = definition("wins", null, "hyxduels_board");
        HologramConfig parsed = HologramConfigParser.parse(Map.of(
                "Enabled", true,
                "Default-Update-Interval-Ticks", 40,
                "Managed", Map.of("board", first)));
        assertEquals(HologramDefinition.Type.WINS, parsed.definitions().get("board").type());
        assertEquals(40, parsed.definitions().get("board").updateIntervalTicks());

        Map<String, Object> managed = new LinkedHashMap<>();
        managed.put("one", first);
        managed.put("two", definition("kills", null, "HYXDUELS_BOARD"));
        assertThrows(IllegalArgumentException.class,
                () -> HologramConfigParser.parse(Map.of("Managed", managed)));
    }

    @Test
    void divisionsRequireModeAndFiniteCompleteLocation()
    {
        assertThrows(IllegalArgumentException.class, () -> HologramConfigParser.parse(Map.of(
                "Managed", Map.of("bad", definition("divisions", null, "hyxduels_bad")))));
        Map<String, Object> invalid = definition("wins", null, "hyxduels_bad");
        invalid.put("Location", Map.of("World", "world", "X", Double.NaN, "Y", 2, "Z", 3));
        assertThrows(IllegalArgumentException.class,
                () -> HologramConfigParser.parse(Map.of("Managed", Map.of("bad", invalid))));
    }

    private Map<String, Object> definition(String type, String mode, String name)
    {
        Map<String, Object> value = new LinkedHashMap<>();
        value.put("Name", name);
        value.put("Type", type);
        if (mode != null) value.put("Gamemode", mode);
        value.put("Location", Map.of("World", "world", "X", 1, "Y", 2, "Z", 3));
        value.put("Lines", List.of("line"));
        return value;
    }
}
