package me.alphatct3209.duels.game.items;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class GoldenHeadTest
{
    @Test
    void usesRequestedUhcQuantityAndEffects()
    {
        assertEquals(3, GoldenHead.DEFAULT_UHC_AMOUNT);
        assertEquals(100, GoldenHead.REGENERATION_DURATION_TICKS);
        assertEquals(2, GoldenHead.REGENERATION_AMPLIFIER);
        assertEquals(2400, GoldenHead.ABSORPTION_DURATION_TICKS);
        assertEquals(0, GoldenHead.ABSORPTION_AMPLIFIER);
    }
}
