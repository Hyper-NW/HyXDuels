package me.alphatct3209.duels.game.modes;

import java.util.LinkedHashSet;
import java.util.Objects;
import java.util.Set;

public record DuelMode(ModeKey key, String displayName, String icon,
                       ModeHandlerType handlerType, ResetPolicy resetPolicy,
                       CombatFlags combat, int targetScore,
                       ModeDurationPolicy durationPolicy, String defaultKitKey,
                       Set<String> allowedKitKeys, Set<String> aliases,
                       boolean enabled, boolean leaderboardEnabled, boolean synthesized)
{
    public DuelMode
    {
        Objects.requireNonNull(key, "key");
        displayName = requireText(displayName, "displayName");
        icon = requireText(icon, "icon");
        Objects.requireNonNull(handlerType, "handlerType");
        Objects.requireNonNull(resetPolicy, "resetPolicy");
        Objects.requireNonNull(combat, "combat");
        Objects.requireNonNull(durationPolicy, "durationPolicy");
        defaultKitKey = requireText(defaultKitKey, "defaultKitKey");
        if (targetScore < 1)
        {
            throw new IllegalArgumentException("targetScore must be at least 1");
        }
        allowedKitKeys = Set.copyOf(new LinkedHashSet<>(Objects.requireNonNull(allowedKitKeys,
                "allowedKitKeys")));
        aliases = Set.copyOf(new LinkedHashSet<>(Objects.requireNonNull(aliases, "aliases")));
        if (!allowedKitKeys.contains(defaultKitKey))
        {
            throw new IllegalArgumentException("Default kit '" + defaultKitKey
                    + "' must also be in allowedKitKeys for mode " + key);
        }
    }

    public boolean allowsKit(String kitKey)
    {
        return allowedKitKeys.contains(kitKey);
    }

    private static String requireText(String value, String field)
    {
        if (value == null || value.isBlank())
        {
            throw new IllegalArgumentException(field + " cannot be blank");
        }
        return value;
    }
}
