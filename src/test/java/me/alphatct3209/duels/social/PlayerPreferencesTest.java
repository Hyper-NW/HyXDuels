package me.alphatct3209.duels.social;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PlayerPreferencesTest
{
    @Test
    void defaultsEnableFeaturesAndAllowAnyone()
    {
        PlayerPreferences value = PlayerPreferences.defaults();
        assertTrue(value.showOwnTier());
        assertTrue(value.scoreboard());
        assertTrue(value.profileKitsPublic());
        assertTrue(value.friendJoinNotifier());
        assertTrue(value.blastParticles());
        assertTrue(value.friendRequests());
        assertEquals(Privacy.ANYONE, value.duelRequests());
        assertEquals(Privacy.ANYONE, value.directMessages());
        assertEquals(Privacy.ANYONE, value.partyInvites());
    }

    @Test
    void everySettingCyclesWithoutMutatingTheOriginal()
    {
        PlayerPreferences original = PlayerPreferences.defaults();
        for (PlayerSetting setting : PlayerSetting.values())
        {
            PlayerPreferences changed = original.cycle(setting);
            assertEquals(original, changed.cycle(setting), setting.name());
            assertFalse(original.display(setting).equals(changed.display(setting)), setting.name());
        }
        assertEquals(PlayerPreferences.defaults(), original);
    }

    @Test
    void privacyUsesStablePersistenceAndFriendlyDisplayValues()
    {
        assertEquals(Privacy.FRIENDS_ONLY, Privacy.ANYONE.next());
        assertEquals(Privacy.ANYONE, Privacy.FRIENDS_ONLY.next());
        assertEquals("Anyone", Privacy.ANYONE.display());
        assertEquals("Friends Only", Privacy.FRIENDS_ONLY.display());
    }
}
