package me.alphatct3209.duels.commands.subcommands.arena;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ArenaDuelsSubCmdTest
{
    @Test
    void normalizesStructuredAndLegacyArenaChildNames()
    {
        assertEquals("list", ArenaDuelsSubCmd.canonicalChild("list"));
        assertEquals("list", ArenaDuelsSubCmd.canonicalChild("listarenas"));
        assertEquals("create", ArenaDuelsSubCmd.canonicalChild("createarena"));
        assertEquals("finish", ArenaDuelsSubCmd.canonicalChild("finisharena"));
        assertEquals("modes", ArenaDuelsSubCmd.canonicalChild("arenamodes"));
        assertEquals("modes", ArenaDuelsSubCmd.canonicalChild("arenakits"));
        assertEquals("settings", ArenaDuelsSubCmd.canonicalChild("arenasettings"));
        assertEquals("objective", ArenaDuelsSubCmd.canonicalChild("OBJECTIVES"));
    }
}
