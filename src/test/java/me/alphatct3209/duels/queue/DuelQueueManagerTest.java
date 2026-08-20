package me.alphatct3209.duels.queue;

import me.alphatct3209.duels.game.modes.DuelSelection;
import me.alphatct3209.duels.game.modes.ModeKey;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

class DuelQueueManagerTest
{
    @Test
    void queueIdentityIncludesModeKitAndLegacyCombatPreference()
    {
        ModeKey classic = ModeKey.parse("classic");
        DuelQueueManager.QueueKey modern = DuelQueueManager.QueueKey.from(
                new DuelSelection(classic, "classic", false));
        DuelQueueManager.QueueKey legacy = DuelQueueManager.QueueKey.from(
                new DuelSelection(classic, "classic", true));

        assertNotEquals(modern, legacy);
        assertEquals(modern, DuelQueueManager.QueueKey.from(
                new DuelSelection(classic, "classic", false)));
        assertNotEquals(DuelQueueManager.QueueKey.from(
                        new DuelSelection(classic, "classic", false), 1),
                DuelQueueManager.QueueKey.from(
                        new DuelSelection(classic, "classic", false), 2));
    }
}
