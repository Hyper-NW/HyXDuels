package me.alphatct3209.duels.game.combat;

/** Attribute values applied for one active duel and restored from a player snapshot afterward. */
public record CombatProfile(double attackSpeed, int maximumNoDamageTicks)
{
    public static final double MODERN_ATTACK_SPEED = 4D;
    public static final double UNCOOLED_ATTACK_SPEED = 24D;
    public static final int NORMAL_INVULNERABILITY_TICKS = 20;

    public CombatProfile
    {
        if (!Double.isFinite(attackSpeed) || attackSpeed <= 0D)
        {
            throw new IllegalArgumentException("attackSpeed must be finite and positive");
        }
        if (maximumNoDamageTicks < 0)
        {
            throw new IllegalArgumentException("maximumNoDamageTicks cannot be negative");
        }
    }

    public static CombatProfile resolve(boolean legacy18, boolean noHitDelay)
    {
        if (noHitDelay)
        {
            return new CombatProfile(UNCOOLED_ATTACK_SPEED, 0);
        }
        if (legacy18)
        {
            return new CombatProfile(UNCOOLED_ATTACK_SPEED, NORMAL_INVULNERABILITY_TICKS);
        }
        return new CombatProfile(MODERN_ATTACK_SPEED, NORMAL_INVULNERABILITY_TICKS);
    }
}
