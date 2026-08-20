package me.alphatct3209.duels.social;

public enum Privacy
{
    ANYONE,
    FRIENDS_ONLY;

    public Privacy next() { return this == ANYONE ? FRIENDS_ONLY : ANYONE; }
    public String display() { return this == ANYONE ? "Anyone" : "Friends Only"; }
}
