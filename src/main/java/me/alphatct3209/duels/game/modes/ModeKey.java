package me.alphatct3209.duels.game.modes;

import java.util.Locale;
import java.util.Objects;
import java.util.regex.Pattern;

/** Stable persistence/configuration identity; never derives from a display name. */
public record ModeKey(String value) implements Comparable<ModeKey>
{
    private static final Pattern VALID = Pattern.compile("[a-z0-9]+(?:_[a-z0-9]+)*");

    public ModeKey
    {
        Objects.requireNonNull(value, "value");
        if (!VALID.matcher(value).matches())
        {
            throw new IllegalArgumentException("Mode key '" + value
                    + "' must be lowercase ASCII words separated by single underscores");
        }
    }

    public static ModeKey parse(String value)
    {
        return new ModeKey(value);
    }

    /** Case-insensitive command lookup only; persisted keys still use the strict constructor. */
    public static ModeKey commandInput(String value)
    {
        return new ModeKey(Objects.requireNonNull(value, "value").trim().toLowerCase(Locale.ROOT));
    }

    @Override public String toString() { return value; }
    @Override public int compareTo(ModeKey other) { return value.compareTo(other.value); }
}
