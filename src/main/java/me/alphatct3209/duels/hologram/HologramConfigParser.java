package me.alphatct3209.duels.hologram;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Strict parser for the independent holograms.yml data model. */
public final class HologramConfigParser
{
    private static final int DEFAULT_INTERVAL = 100;

    private HologramConfigParser() {}

    public static HologramConfig parse(Map<String, ?> root)
    {
        boolean enabled = bool(root.get("Enabled"), false, "Enabled");
        int defaultInterval = integer(root.get("Default-Update-Interval-Ticks"),
                DEFAULT_INTERVAL, "Default-Update-Interval-Ticks");
        Map<String, ?> managed = map(root.get("Managed"), "Managed", true);
        Map<String, HologramDefinition> definitions = new LinkedHashMap<>();
        for (Map.Entry<String, ?> entry : managed.entrySet())
        {
            String id = entry.getKey();
            String path = "Managed." + id;
            Map<String, ?> values = map(entry.getValue(), path, false);
            Object configuredName = values.containsKey("Name") ? values.get("Name") : "hyxduels_" + id;
            String name = string(configuredName, path + ".Name");
            HologramDefinition.Type type = HologramDefinition.Type.parse(
                    string(values.get("Type"), path + ".Type"));
            String gamemode = nullableString(values.get("Gamemode"));
            int interval = integer(values.get("Update-Interval-Ticks"), defaultInterval,
                    path + ".Update-Interval-Ticks");
            Map<String, ?> location = map(values.get("Location"), path + ".Location", false);
            HologramLocation position = new HologramLocation(
                    string(location.get("World"), path + ".Location.World"),
                    decimal(location.get("X"), path + ".Location.X"),
                    decimal(location.get("Y"), path + ".Location.Y"),
                    decimal(location.get("Z"), path + ".Location.Z"),
                    (float) decimal(location.containsKey("Yaw") ? location.get("Yaw") : 0,
                            path + ".Location.Yaw"),
                    (float) decimal(location.containsKey("Pitch") ? location.get("Pitch") : 0,
                            path + ".Location.Pitch"));
            List<String> lines = strings(values.get("Lines"), path + ".Lines");
            try
            {
                HologramDefinition definition = new HologramDefinition(id, name, type,
                        gamemode, position, interval, lines);
                if (definitions.putIfAbsent(definition.id(), definition) != null)
                {
                    throw new IllegalArgumentException("duplicate id after case normalization");
                }
            }
            catch (IllegalArgumentException exception)
            {
                throw new IllegalArgumentException(path + ": " + exception.getMessage(), exception);
            }
        }
        return new HologramConfig(enabled, defaultInterval, definitions);
    }

    private static Map<String, ?> map(Object value, String path, boolean missingAsEmpty)
    {
        if (value == null && missingAsEmpty)
        {
            return Map.of();
        }
        if (!(value instanceof Map<?, ?> raw))
        {
            throw new IllegalArgumentException(path + " must be a section");
        }
        Map<String, Object> result = new LinkedHashMap<>();
        raw.forEach((key, nested) -> result.put(String.valueOf(key), nested));
        return result;
    }

    private static List<String> strings(Object value, String path)
    {
        if (!(value instanceof List<?> raw))
        {
            throw new IllegalArgumentException(path + " must be a list");
        }
        List<String> result = new ArrayList<>();
        for (Object line : raw)
        {
            if (!(line instanceof String text))
            {
                throw new IllegalArgumentException(path + " must contain only strings");
            }
            result.add(text);
        }
        return result;
    }

    private static String string(Object value, String path)
    {
        if (!(value instanceof String text) || text.isBlank())
        {
            throw new IllegalArgumentException(path + " must be a nonempty string");
        }
        return text;
    }

    private static String nullableString(Object value)
    {
        return value == null ? null : String.valueOf(value).trim();
    }

    private static int integer(Object value, int fallback, String path)
    {
        if (value == null)
        {
            return fallback;
        }
        if (!(value instanceof Number number) || number.doubleValue() != number.intValue())
        {
            throw new IllegalArgumentException(path + " must be a whole number");
        }
        return number.intValue();
    }

    private static double decimal(Object value, String path)
    {
        if (!(value instanceof Number number))
        {
            throw new IllegalArgumentException(path + " must be a number");
        }
        return number.doubleValue();
    }

    private static boolean bool(Object value, boolean fallback, String path)
    {
        if (value == null)
        {
            return fallback;
        }
        if (!(value instanceof Boolean result))
        {
            throw new IllegalArgumentException(path + " must be true or false");
        }
        return result;
    }
}
