package me.alphatct3209.duels.hologram;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class HologramCommandParserTest
{
    @Test
    void parsesAllAdministrativeActions()
    {
        assertEquals(HologramCommandParser.Action.STATUS,
                HologramCommandParser.parse(new String[]{"status"}).action());
        assertEquals(HologramCommandParser.Action.LIST,
                HologramCommandParser.parse(new String[]{"list"}).action());
        assertEquals(HologramCommandParser.Action.RELOAD,
                HologramCommandParser.parse(new String[]{"reload"}).action());
        assertEquals("board", HologramCommandParser.parse(new String[]{"move", "board"}).id());
        assertEquals("board", HologramCommandParser.parse(new String[]{"delete", "board"}).id());
        HologramCommandParser.Parsed create = HologramCommandParser.parse(
                new String[]{"create", "ranked", "divisions", "NoDebuff"});
        assertEquals(HologramDefinition.Type.DIVISIONS, create.type());
        assertEquals("NoDebuff", create.gamemode());
    }

    @Test
    void rejectsUnknownActionsInvalidArityAndModeCombinations()
    {
        assertThrows(IllegalArgumentException.class,
                () -> HologramCommandParser.parse(new String[]{"wat"}));
        assertThrows(IllegalArgumentException.class,
                () -> HologramCommandParser.parse(new String[]{"move"}));
        assertThrows(IllegalArgumentException.class,
                () -> HologramCommandParser.parse(new String[]{"create", "x", "divisions"}));
        assertThrows(IllegalArgumentException.class,
                () -> HologramCommandParser.parse(new String[]{"create", "x", "wins", "mode"}));
    }
}
