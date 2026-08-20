package me.alphatct3209.duels.game.kits;

import java.text.Normalizer;
import java.util.Locale;
import java.util.Objects;

/**
 * Creates stable, lowercase keys suitable for configuration and persistence paths.
 */
public final class GamemodeKey
{
    private GamemodeKey()
    {
    }

    public static String fromKitName(String kitName)
    {
        Objects.requireNonNull(kitName, "kitName");
        String normalized = Normalizer.normalize(kitName, Normalizer.Form.NFKD)
                .replaceAll("\\p{M}+", "")
                .toLowerCase(Locale.ROOT)
                .replaceAll("[^a-z0-9]+", "_")
                .replaceAll("^_+|_+$", "");
        if (normalized.isEmpty())
        {
            throw new IllegalArgumentException("Kit name must contain at least one letter or number");
        }
        return normalized;
    }
}
