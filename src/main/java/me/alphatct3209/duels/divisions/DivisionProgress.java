package me.alphatct3209.duels.divisions;

import java.util.Objects;
import java.util.Optional;

/**
 * Progress derived solely from a player's wins in one gamemode.
 */
public record DivisionProgress(long wins, Optional<Division> current, Optional<Division> next,
                               long winsIntoStep, long winsForStep)
{
    public DivisionProgress
    {
        Objects.requireNonNull(current, "current");
        Objects.requireNonNull(next, "next");
        if (wins < 0 || winsIntoStep < 0 || winsForStep < 0)
        {
            throw new IllegalArgumentException("Division progress values cannot be negative");
        }
        if (winsIntoStep > winsForStep)
        {
            throw new IllegalArgumentException("Progress cannot exceed the current step");
        }
    }

    public boolean isMaximum()
    {
        return next.isEmpty();
    }

    public double fraction()
    {
        return winsForStep == 0 ? 1.0D : (double) winsIntoStep / winsForStep;
    }
}
