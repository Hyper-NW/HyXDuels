package me.alphatct3209.duels.challenge;

import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class ChallengeStateTest
{
    @Test
    void enforcesOnePendingChallengePerParticipantAndOnlyTargetCanConsume()
    {
        ChallengeState state = new ChallengeState();
        UUID challenger = UUID.randomUUID();
        UUID target = UUID.randomUUID();
        UUID third = UUID.randomUUID();
        ChallengeState.Pending pending = state.create(challenger, target, "default", 100L);

        assertThrows(IllegalStateException.class,
                () -> state.create(challenger, third, "other", 100L));
        assertThrows(IllegalStateException.class,
                () -> state.create(third, target, "other", 100L));
        assertNull(state.forTarget(challenger));
        assertNull(state.removeForTarget(challenger, pending.token()));
        assertSame(pending, state.removeForTarget(target, pending.token()));
        assertNull(state.forParticipant(challenger));
        assertNull(state.forParticipant(target));
    }

    @Test
    void staleExpiryTokenCannotRemoveAReplacementChallenge()
    {
        ChallengeState state = new ChallengeState();
        UUID challenger = UUID.randomUUID();
        UUID target = UUID.randomUUID();
        ChallengeState.Pending old = state.create(challenger, target, "default", 10L);
        assertSame(old, state.removeParticipant(challenger));

        ChallengeState.Pending replacement = state.create(challenger, target, "default", 50L);
        assertNull(state.expire(old.token(), 100L));
        assertSame(replacement, state.forTarget(target));
        assertNull(state.expire(replacement.token(), 49L));
        assertSame(replacement, state.expire(replacement.token(), 50L));
    }

    @Test
    void participantCleanupRemovesBothIndexes()
    {
        ChallengeState state = new ChallengeState();
        UUID challenger = UUID.randomUUID();
        UUID target = UUID.randomUUID();
        ChallengeState.Pending pending = state.create(challenger, target, "default", 100L);

        assertSame(pending, state.removeParticipant(target));
        assertNull(state.forParticipant(challenger));
        assertNull(state.forParticipant(target));
        assertNull(state.expire(pending.token(), 200L));
    }

    @Test
    void carriesOptionalPreferredArenaWithoutChangingLegacyCreation()
    {
        ChallengeState state = new ChallengeState();
        UUID first = UUID.randomUUID();
        UUID second = UUID.randomUUID();
        ChallengeState.Pending preferred = state.create(first, second,
                "classic", "default", 7, 100L);
        assertEquals(7, preferred.arenaId());
        state.removeParticipant(first);

        ChallengeState.Pending automatic = state.create(first, second,
                "classic", "default", 200L);
        assertNull(automatic.arenaId());
    }

    @Test
    void capturesLegacyCombatPreference()
    {
        ChallengeState state = new ChallengeState();
        ChallengeState.Pending pending = state.create(UUID.randomUUID(), UUID.randomUUID(),
                "classic", "classic", null, true, 100L);
        assertTrue(pending.legacyPvp());
    }

}
