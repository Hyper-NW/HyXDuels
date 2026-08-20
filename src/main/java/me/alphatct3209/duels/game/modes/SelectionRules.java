package me.alphatct3209.duels.game.modes;

import java.util.Collection;
import java.util.Objects;

public final class SelectionRules
{
    private SelectionRules() {}

    public static DuelSelection select(DuelMode mode, String requestedKitKey,
                                       Collection<String> availableKitKeys)
    {
        return select(mode, requestedKitKey, availableKitKeys, false);
    }

    public static DuelSelection select(DuelMode mode, String requestedKitKey,
                                       Collection<String> availableKitKeys, boolean legacyPvp)
    {
        Objects.requireNonNull(mode, "mode");
        Objects.requireNonNull(availableKitKeys, "availableKitKeys");
        if (!mode.enabled()) throw new IllegalArgumentException("Duel mode '" + mode.key() + "' is disabled");
        String kit = requestedKitKey == null || requestedKitKey.isBlank()
                ? mode.defaultKitKey() : requestedKitKey;
        if (!availableKitKeys.contains(kit)) throw new IllegalArgumentException("Unknown kit '" + kit + "'");
        if (!mode.allowsKit(kit))
            throw new IllegalArgumentException("Kit '" + kit + "' is not allowed for mode " + mode.key());
        return new DuelSelection(mode.key(), kit, legacyPvp);
    }
}
