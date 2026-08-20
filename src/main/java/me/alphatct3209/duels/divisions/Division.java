package me.alphatct3209.duels.divisions;

import java.util.List;
import java.util.Objects;

/**
 * An immutable, attainable division and its cumulative win threshold.
 */
public record Division(String name, int level, long requiredWins, List<String> rewards)
{
    public Division
    {
        Objects.requireNonNull(name, "name");
        Objects.requireNonNull(rewards, "rewards");
        if (name.isBlank())
        {
            throw new IllegalArgumentException("Division name cannot be blank");
        }
        if (level <= 0)
        {
            throw new IllegalArgumentException("Division level must be positive");
        }
        if (requiredWins <= 0)
        {
            throw new IllegalArgumentException("Division win threshold must be positive");
        }
        rewards = List.copyOf(rewards);
    }

    public String displayName()
    {
        return name + " " + level;
    }
}
