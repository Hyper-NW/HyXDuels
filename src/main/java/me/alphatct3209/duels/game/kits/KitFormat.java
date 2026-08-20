package me.alphatct3209.duels.game.kits;

public enum KitFormat
{
    LEGACY_V1(1),
    POSITIONAL_V2(2);

    private final int version;

    KitFormat(int version)
    {
        this.version = version;
    }

    public int getVersion()
    {
        return version;
    }
}
