package me.alphatct3209.duels.game.arenas;

import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.OptionalDouble;

/** Typed view over persisted Arenas.<id>.Objective scalar values. */
public final class ArenaObjectiveSettings
{
    private final Map<String, Double> values;
    public ArenaObjectiveSettings() { this(Map.of()); }
    public ArenaObjectiveSettings(Map<String, ? extends Number> configured)
    {
        Map<String, Double> copy = new LinkedHashMap<>();
        configured.forEach((key, value) -> {
            String normalized = normalize(key);
            double number = value.doubleValue();
            if (!Double.isFinite(number)) throw new IllegalArgumentException("Objective setting " + key + " must be finite");
            copy.put(normalized, number);
        });
        values = Map.copyOf(copy);
    }
    public double radius(String kind) { return positive(kind + "-radius", 1.5D); }
    public int boxingMercyLead() { return positiveInt("boxing-mercy-lead", 50); }
    public long railgunCooldownMillis() { return positiveLong("railgun-cooldown-ms", 850L); }
    public long dashCooldownMillis() { return positiveLong("dash-cooldown-ms", 2000L); }
    public long spleefProjectileCooldownMillis() { return positiveLong("spleef-projectile-cooldown-ms", 150L); }
    public int bedWarsRespawnSeconds() { return positiveInt("bedwars-respawn-seconds", 5); }
    public int bedWarsIronGeneratorTicks() { return positiveInt("bedwars-iron-generator-ticks", 20); }
    public int bedWarsGoldGeneratorTicks() { return positiveInt("bedwars-gold-generator-ticks", 80); }
    public int bedWarsDiamondGeneratorTicks() { return positiveInt("bedwars-diamond-generator-ticks", 600); }
    public int bedWarsEmeraldGeneratorTicks() { return positiveInt("bedwars-emerald-generator-ticks", 1200); }
    public int skyWarsRefillSeconds() { return positiveInt("skywars-refill-seconds", 300); }
    public double railgunRange() { return positive("railgun-range", 100D); }
    public double cellRadius() { return positive("cell-radius", 32D); }
    public OptionalDouble deathY() { return values.containsKey("death-y") ? OptionalDouble.of(values.get("death-y")) : OptionalDouble.empty(); }
    public Map<String, Double> values() { return values; }
    public ArenaObjectiveSettings with(String key, double value)
    {
        Map<String, Double> changed = new LinkedHashMap<>(values); changed.put(normalize(key), value);
        return new ArenaObjectiveSettings(changed);
    }
    public ArenaObjectiveSettings without(String key)
    {
        Map<String, Double> changed = new LinkedHashMap<>(values); changed.remove(normalize(key));
        return new ArenaObjectiveSettings(changed);
    }
    private double positive(String key, double fallback)
    {
        double value = values.getOrDefault(key, fallback);
        if (value <= 0D) throw new IllegalArgumentException(key + " must be positive");
        return value;
    }
    private int positiveInt(String key, int fallback)
    {
        double value = positive(key, fallback);
        if (value != Math.rint(value) || value > Integer.MAX_VALUE) throw new IllegalArgumentException(key + " must be a whole number");
        return (int) value;
    }
    private long positiveLong(String key, long fallback)
    {
        double value = positive(key, fallback);
        if (value != Math.rint(value) || value > Long.MAX_VALUE) throw new IllegalArgumentException(key + " must be a whole number");
        return (long) value;
    }
    public static String normalize(String key)
    {
        if (key == null || !key.toLowerCase(Locale.ROOT).matches("[a-z0-9_-]+"))
            throw new IllegalArgumentException("Objective setting keys use lowercase letters, numbers, '_' or '-'");
        return key.toLowerCase(Locale.ROOT);
    }
}
