package me.alphatct3209.duels.game.arenas;

import me.alphatct3209.duels.game.modes.ModeControllerFactory;
import me.alphatct3209.duels.game.modes.ModeHandlerType;

import java.util.LinkedHashSet;
import java.util.Locale;
import java.util.Set;

public final class ArenaModeReadiness
{
    private ArenaModeReadiness() {}
    public static Set<String> missing(ModeHandlerType handler, Set<String> configuredPoints)
    {
        Set<String> normalized = new LinkedHashSet<>();
        configuredPoints.forEach(point -> normalized.add(point.toLowerCase(Locale.ROOT)));
        Set<String> missing = new LinkedHashSet<>(ModeControllerFactory.create(handler, 50).requiredPoints());
        missing.removeAll(normalized);
        return Set.copyOf(missing);
    }
    public static boolean ready(ModeHandlerType handler, Set<String> points)
    {
        return missing(handler, points).isEmpty();
    }
}
