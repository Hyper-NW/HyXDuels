package me.alphatct3209.duels.game.modes;

import java.time.Duration;
import java.util.Objects;

public record ModeDurationPolicy(Duration maximumDuration, TimeoutPolicy timeoutPolicy)
{
    public ModeDurationPolicy
    {
        Objects.requireNonNull(maximumDuration, "maximumDuration");
        Objects.requireNonNull(timeoutPolicy, "timeoutPolicy");
        if (maximumDuration.isNegative())
        {
            throw new IllegalArgumentException("Maximum duration cannot be negative");
        }
    }

    public enum TimeoutPolicy
    {
        DRAW,
        HIGHEST_SCORE,
        LAST_STANDING
    }
}
