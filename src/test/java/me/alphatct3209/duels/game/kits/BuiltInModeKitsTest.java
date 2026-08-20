package me.alphatct3209.duels.game.kits;

import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;

class BuiltInModeKitsTest
{
    @Test
    void includesEveryModeSpecificLoadoutKey()
    {
        assertEquals(Set.of("bed_wars", "blitz", "bow", "boxing", "bridge", "classic",
                "combo", "mega_walls", "nodebuff", "op", "parkour", "quakecraft",
                "skywars", "spleef", "sumo", "uhc"), BuiltInModeKits.keys());
    }
}
