package me.alphatct3209.duels.game.arenas;

import java.util.Objects;
import java.util.UUID;

/** Authorization is identity based; sharing a Bukkit world never grants combat access. */
public final class ArenaCombatAuthorization
{
    private ArenaCombatAuthorization() {}
    public static boolean opponents(UUID victim, UUID attacker, UUID first, UUID second)
    {
        if (victim == null || attacker == null || Objects.equals(victim, attacker)) return false;
        return (victim.equals(first) && attacker.equals(second))
                || (victim.equals(second) && attacker.equals(first));
    }
}
