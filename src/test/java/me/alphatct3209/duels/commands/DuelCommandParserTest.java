package me.alphatct3209.duels.commands;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class DuelCommandParserTest
{
    @Test
    void knownSubcommandsTakePrecedenceOverSameNamedPlayers()
    {
        assertEquals(DuelCommandParser.Route.SUBCOMMAND,
                DuelCommandParser.resolve("accept", true, List.of("accept")));
    }

    @Test
    void fallbackRequiresAnExactVisibleOnlineNameIgnoringCase()
    {
        assertEquals(DuelCommandParser.Route.DIRECT_PLAYER,
                DuelCommandParser.resolve("Alice", false, List.of("aLiCe", "Bob")));
        assertEquals(DuelCommandParser.Route.UNKNOWN,
                DuelCommandParser.resolve("Ali", false, List.of("Alice")));
        assertEquals(DuelCommandParser.Route.UNKNOWN,
                DuelCommandParser.resolve("Hidden", false, List.of("Alice")));
    }
}
