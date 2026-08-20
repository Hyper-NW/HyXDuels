package me.alphatct3209.duels.social;

public record PlayerPreferences(boolean showOwnTier, boolean scoreboard,
                                boolean profileKitsPublic, boolean friendJoinNotifier,
                                boolean blastParticles, Privacy duelRequests,
                                Privacy directMessages, Privacy partyInvites,
                                boolean friendRequests)
{
    public PlayerPreferences
    {
        if (duelRequests == null) duelRequests = Privacy.ANYONE;
        if (directMessages == null) directMessages = Privacy.ANYONE;
        if (partyInvites == null) partyInvites = Privacy.ANYONE;
    }

    public static PlayerPreferences defaults()
    {
        return new PlayerPreferences(true, true, true, true, true,
                Privacy.ANYONE, Privacy.ANYONE, Privacy.ANYONE, true);
    }

    public PlayerPreferences cycle(PlayerSetting setting)
    {
        return switch (setting)
        {
            case SHOW_OWN_TIER -> new PlayerPreferences(!showOwnTier, scoreboard, profileKitsPublic,
                    friendJoinNotifier, blastParticles, duelRequests, directMessages, partyInvites, friendRequests);
            case SCOREBOARD -> new PlayerPreferences(showOwnTier, !scoreboard, profileKitsPublic,
                    friendJoinNotifier, blastParticles, duelRequests, directMessages, partyInvites, friendRequests);
            case PROFILE_KITS -> new PlayerPreferences(showOwnTier, scoreboard, !profileKitsPublic,
                    friendJoinNotifier, blastParticles, duelRequests, directMessages, partyInvites, friendRequests);
            case FRIEND_JOIN_NOTIFIER -> new PlayerPreferences(showOwnTier, scoreboard, profileKitsPublic,
                    !friendJoinNotifier, blastParticles, duelRequests, directMessages, partyInvites, friendRequests);
            case BLAST_PARTICLES -> new PlayerPreferences(showOwnTier, scoreboard, profileKitsPublic,
                    friendJoinNotifier, !blastParticles, duelRequests, directMessages, partyInvites, friendRequests);
            case DUEL_REQUESTS -> new PlayerPreferences(showOwnTier, scoreboard, profileKitsPublic,
                    friendJoinNotifier, blastParticles, duelRequests.next(), directMessages, partyInvites, friendRequests);
            case DIRECT_MESSAGES -> new PlayerPreferences(showOwnTier, scoreboard, profileKitsPublic,
                    friendJoinNotifier, blastParticles, duelRequests, directMessages.next(), partyInvites, friendRequests);
            case PARTY_INVITES -> new PlayerPreferences(showOwnTier, scoreboard, profileKitsPublic,
                    friendJoinNotifier, blastParticles, duelRequests, directMessages, partyInvites.next(), friendRequests);
            case FRIEND_REQUESTS -> new PlayerPreferences(showOwnTier, scoreboard, profileKitsPublic,
                    friendJoinNotifier, blastParticles, duelRequests, directMessages, partyInvites, !friendRequests);
        };
    }

    public String display(PlayerSetting setting)
    {
        return switch (setting)
        {
            case SHOW_OWN_TIER -> onOff(showOwnTier);
            case SCOREBOARD -> onOff(scoreboard);
            case PROFILE_KITS -> profileKitsPublic ? "Public" : "Private";
            case FRIEND_JOIN_NOTIFIER -> onOff(friendJoinNotifier);
            case BLAST_PARTICLES -> onOff(blastParticles);
            case DUEL_REQUESTS -> duelRequests.display();
            case DIRECT_MESSAGES -> directMessages.display();
            case PARTY_INVITES -> partyInvites.display();
            case FRIEND_REQUESTS -> onOff(friendRequests);
        };
    }

    private static String onOff(boolean value) { return value ? "On" : "Off"; }
}
