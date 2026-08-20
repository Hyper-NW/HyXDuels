package me.alphatct3209.duels.party;

public enum PartyRole
{
    LEADER,
    MODERATOR,
    MEMBER;

    public boolean canManageMembers()
    {
        return this == LEADER || this == MODERATOR;
    }
}
