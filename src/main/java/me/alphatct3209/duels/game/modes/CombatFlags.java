package me.alphatct3209.duels.game.modes;

public record CombatFlags(boolean pvp, boolean projectiles, boolean melee, boolean healthDamage,
                          boolean naturalRegeneration, boolean hunger, boolean fallDamage,
                          boolean blockDamage, boolean noHitDelay)
{
    public static CombatFlags standard()
    {
        return new CombatFlags(true, true, true, true, true, true, true, false, false);
    }
}
