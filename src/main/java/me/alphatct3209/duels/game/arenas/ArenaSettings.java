package me.alphatct3209.duels.game.arenas;

import java.util.Arrays;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

public final class ArenaSettings
{
    public enum Flag
    {
        BLOCK_BREAK("block-break", false),
        BLOCK_PLACE("block-place", false),
        ENTITY_PLACEMENT("entity-placement", false),
        EXPLOSIONS("explosions", true),
        EXPLOSION_BLOCK_DAMAGE("explosion-block-damage", false),
        FIRE_SPREAD("fire-spread", false),
        ITEM_DROP("item-drop", false),
        ITEM_PICKUP("item-pickup", false);

        private static final Map<String, Flag> BY_KEY = Arrays.stream(values())
                .collect(Collectors.toUnmodifiableMap(Flag::key, Function.identity()));
        private final String key;
        private final boolean defaultValue;

        Flag(String key, boolean defaultValue)
        {
            this.key = key;
            this.defaultValue = defaultValue;
        }

        public String key()
        {
            return key;
        }

        public boolean defaultValue()
        {
            return defaultValue;
        }

        public static Optional<Flag> parse(String value)
        {
            if (value == null)
            {
                return Optional.empty();
            }
            return Optional.ofNullable(BY_KEY.get(value.toLowerCase(Locale.ROOT)));
        }
    }

    private final boolean[] values;

    public ArenaSettings()
    {
        this.values = new boolean[Flag.values().length];
        for (Flag flag : Flag.values())
        {
            values[flag.ordinal()] = flag.defaultValue();
        }
    }

    private ArenaSettings(boolean[] values)
    {
        this.values = values;
    }

    /** Pure map loader used by configuration adapters and tests. */
    public static ArenaSettings fromMap(Map<String, ?> configured)
    {
        ArenaSettings settings = new ArenaSettings();
        if (configured == null)
        {
            return settings;
        }
        for (Flag flag : Flag.values())
        {
            Object value = configured.get(flag.key());
            if (value instanceof Boolean bool)
            {
                settings.values[flag.ordinal()] = bool;
            }
        }
        return settings;
    }

    public boolean get(Flag flag)
    {
        return values[flag.ordinal()];
    }

    public ArenaSettings with(Flag flag, boolean value)
    {
        boolean[] copy = values.clone();
        copy[flag.ordinal()] = value;
        return new ArenaSettings(copy);
    }
}
