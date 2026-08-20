package me.alphatct3209.duels.world;

import me.alphatct3209.duels.game.GameState;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class WorldRegenerationCoordinatorTest
{
    @Test
    void pendingWorldRejectsAdmissionsUntilSuccessfulCompletion()
    {
        WorldRegenerationCoordinator coordinator = new WorldRegenerationCoordinator();
        assertTrue(coordinator.canAdmit("Arena-One"));
        assertTrue(coordinator.request("Arena-One"));
        assertFalse(coordinator.canAdmit("arena-one"));
        assertTrue(coordinator.isPending(" ARENA-ONE "));

        coordinator.complete("arena-one");
        assertTrue(coordinator.canAdmit("Arena-One"));
    }

    @Test
    void sharedWorldIsReadyOnlyAfterEveryPlayingArenaDrains()
    {
        WorldRegenerationCoordinator coordinator = new WorldRegenerationCoordinator();
        coordinator.request("shared");

        assertFalse(coordinator.isDrainReady("shared",
                List.of(GameState.REGENERATING, GameState.PLAYING, GameState.REGENERATING)));
        assertTrue(coordinator.isDrainReady("shared",
                List.of(GameState.REGENERATING, GameState.REGENERATING)));
    }

    @Test
    void canonicalWorldNamesAreCaseAndOuterWhitespaceInsensitive()
    {
        assertEquals("map_one", WorldRegenerationCoordinator.canonical("  Map_One "));
        assertThrows(IllegalArgumentException.class,
                () -> WorldRegenerationCoordinator.canonical("  "));
    }

    @Test
    void admissionRequiresTheExactCurrentWorldInstance()
    {
        WorldRegenerationCoordinator coordinator = new WorldRegenerationCoordinator();
        Object oldInstance = new Object();
        Object liveInstance = new Object();

        assertFalse(coordinator.canAdmit("arena", oldInstance, liveInstance));
        assertTrue(coordinator.canAdmit("arena", liveInstance, liveInstance));
        coordinator.request("arena");
        assertFalse(coordinator.canAdmit("arena", liveInstance, liveInstance));
    }

}