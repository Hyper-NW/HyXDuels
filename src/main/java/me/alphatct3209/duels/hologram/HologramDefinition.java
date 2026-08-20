package me.alphatct3209.duels.hologram;

import java.util.List;
import java.util.Locale;
import java.util.Objects;

/** A validated, Bukkit-independent managed hologram definition. */
public record HologramDefinition(String id, String name, Type type, String gamemode,
                                  HologramLocation location, int updateIntervalTicks,
                                  List<String> lines)
{
    public static final int MIN_UPDATE_INTERVAL = 20;
    public static final int MAX_UPDATE_INTERVAL = 72_000;

    public HologramDefinition
    {
        id = Objects.requireNonNull(id, "id").toLowerCase(Locale.ROOT);
        name = Objects.requireNonNull(name, "name");
        type = Objects.requireNonNull(type, "type");
        location = Objects.requireNonNull(location, "location");
        lines = List.copyOf(Objects.requireNonNull(lines, "lines"));
        if (!id.matches("[a-z0-9_-]{1,32}"))
        {
            throw new IllegalArgumentException("id must contain 1-32 lowercase letters, numbers, '_' or '-'");
        }
        if (!name.matches("[A-Za-z0-9_-]{1,64}"))
        {
            throw new IllegalArgumentException("name must contain 1-64 letters, numbers, '_' or '-'");
        }
        if (type == Type.DIVISIONS)
        {
            if (gamemode == null || !gamemode.matches("[a-z0-9]+(?:_[a-z0-9]+)*"))
            {
                throw new IllegalArgumentException("divisions requires a safe lowercase gamemode key");
            }
        }
        else if (gamemode != null && !gamemode.isBlank())
        {
            throw new IllegalArgumentException(type.configValue() + " does not accept a gamemode");
        }
        if (updateIntervalTicks < MIN_UPDATE_INTERVAL || updateIntervalTicks > MAX_UPDATE_INTERVAL)
        {
            throw new IllegalArgumentException("update interval must be between "
                    + MIN_UPDATE_INTERVAL + " and " + MAX_UPDATE_INTERVAL + " ticks");
        }
        if (lines.isEmpty() || lines.stream().anyMatch(line -> line == null || line.isEmpty()))
        {
            throw new IllegalArgumentException("lines must contain at least one nonempty line");
        }
    }

    public enum Type
    {
        WINS, KILLS, DIVISIONS;

        public static Type parse(String value)
        {
            try
            {
                return valueOf(Objects.requireNonNull(value, "type").trim().toUpperCase(Locale.ROOT));
            }
            catch (IllegalArgumentException | NullPointerException exception)
            {
                throw new IllegalArgumentException("type must be wins, kills, or divisions");
            }
        }

        public String configValue()
        {
            return name().toLowerCase(Locale.ROOT);
        }
    }
}
