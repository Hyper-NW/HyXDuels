package me.alphatct3209.duels.hologram;

import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;

public record HologramConfig(boolean enabled, int defaultUpdateIntervalTicks,
                             Map<String, HologramDefinition> definitions)
{
    public HologramConfig
    {
        if (defaultUpdateIntervalTicks < HologramDefinition.MIN_UPDATE_INTERVAL
                || defaultUpdateIntervalTicks > HologramDefinition.MAX_UPDATE_INTERVAL)
        {
            throw new IllegalArgumentException("Default-Update-Interval-Ticks must be between "
                    + HologramDefinition.MIN_UPDATE_INTERVAL + " and "
                    + HologramDefinition.MAX_UPDATE_INTERVAL);
        }
        LinkedHashMap<String, HologramDefinition> copy = new LinkedHashMap<>();
        Map<String, String> names = new LinkedHashMap<>();
        definitions.forEach((id, definition) -> {
            String canonicalId = id.toLowerCase(Locale.ROOT);
            if (copy.putIfAbsent(canonicalId, definition) != null)
            {
                throw new IllegalArgumentException("duplicate managed id '" + id + "'");
            }
            String canonicalName = definition.name().toLowerCase(Locale.ROOT);
            String previous = names.putIfAbsent(canonicalName, id);
            if (previous != null)
            {
                throw new IllegalArgumentException("managed ids '" + previous + "' and '" + id
                        + "' use duplicate hologram name '" + definition.name() + "'");
            }
        });
        definitions = Map.copyOf(copy);
    }
}
