package me.alphatct3209.duels.social;

public enum PlayerSetting
{
    SHOW_OWN_TIER("show-own-tier"),
    SCOREBOARD("scoreboard"),
    PROFILE_KITS("profile-kits"),
    FRIEND_JOIN_NOTIFIER("friend-join-notifier"),
    BLAST_PARTICLES("blast-particles"),
    DUEL_REQUESTS("duel-requests"),
    DIRECT_MESSAGES("direct-messages"),
    PARTY_INVITES("party-invites"),
    FRIEND_REQUESTS("friend-requests");

    private final String key;
    PlayerSetting(String key) { this.key = key; }
    public String key() { return key; }
}
