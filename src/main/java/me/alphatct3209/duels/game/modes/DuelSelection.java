package me.alphatct3209.duels.game.modes;

import java.util.Objects;

/** A mode, reusable kit, and player-selected combat profile captured for one duel. */
public record DuelSelection(ModeKey modeKey, String kitKey, boolean legacyPvp)
{
    public DuelSelection(ModeKey modeKey, String kitKey)
    {
        this(modeKey, kitKey, false);
    }

    public DuelSelection
    {
        Objects.requireNonNull(modeKey, "modeKey");
        if (kitKey == null || kitKey.isBlank())
        {
            throw new IllegalArgumentException("kitKey cannot be blank");
        }
    }
}
