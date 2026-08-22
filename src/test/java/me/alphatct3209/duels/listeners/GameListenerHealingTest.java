package me.alphatct3209.duels.listeners;

import org.bukkit.event.entity.EntityRegainHealthEvent;
import org.junit.jupiter.api.Test;

import java.util.EnumSet;

import static org.junit.jupiter.api.Assertions.assertEquals;

class GameListenerHealingTest
{
    @Test
    void allowsOnlyHealthPotionsAndRegenerationEffects()
    {
        EnumSet<EntityRegainHealthEvent.RegainReason> allowed = EnumSet.noneOf(
                EntityRegainHealthEvent.RegainReason.class);
        for (EntityRegainHealthEvent.RegainReason reason
                : EntityRegainHealthEvent.RegainReason.values())
            if (GameListener.isAllowedDuelHealing(reason)) allowed.add(reason);

        assertEquals(EnumSet.of(EntityRegainHealthEvent.RegainReason.MAGIC,
                EntityRegainHealthEvent.RegainReason.MAGIC_REGEN), allowed);
    }
}
