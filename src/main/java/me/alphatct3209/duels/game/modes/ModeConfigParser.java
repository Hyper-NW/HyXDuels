package me.alphatct3209.duels.game.modes;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/** Strict, Bukkit-free modes.yml parser. */
public final class ModeConfigParser
{
    public static final Set<String> ROSTER = Set.of("bed_wars", "blitz", "bow", "boxing",
            "bridge", "classic", "combo", "duel_arena", "mega_walls", "nodebuff", "op",
            "parkour", "quakecraft", "skywars", "spleef", "sumo", "uhc");
    private static final Set<String> FIELDS = Set.of("display-name", "icon", "handler",
            "reset-policy", "combat", "target-score", "duration-seconds", "timeout-policy",
            "default-kit", "allowed-kits", "aliases", "enabled", "leaderboard-enabled");
    private static final Set<String> COMBAT_FIELDS = Set.of("pvp", "projectiles", "melee",
            "health-damage", "natural-regeneration", "hunger", "fall-damage", "block-damage",
            "no-hit-delay");

    private ModeConfigParser() {}

    public static Map<ModeKey, DuelMode> parse(Map<String, ?> configured,
                                                Collection<String> availableKitKeys)
    {
        if (configured == null) throw new IllegalArgumentException("Modes section is required");
        if (!configured.keySet().equals(ROSTER))
        {
            Set<String> missing = new LinkedHashSet<>(ROSTER); missing.removeAll(configured.keySet());
            Set<String> unknown = new LinkedHashSet<>(configured.keySet()); unknown.removeAll(ROSTER);
            throw new IllegalArgumentException("Modes must contain exactly the 17 built-in keys; missing="
                    + missing + ", unknown=" + unknown);
        }
        Set<String> kits = Set.copyOf(availableKitKeys);
        Map<ModeKey, DuelMode> result = new LinkedHashMap<>();
        Map<String, ModeKey> identities = new LinkedHashMap<>();
        for (String rawKey : configured.keySet()) identities.put(rawKey, ModeKey.parse(rawKey));

        for (String rawKey : configured.keySet())
        {
            String path = "Modes." + rawKey;
            Map<String, ?> values = map(configured.get(rawKey), path);
            rejectUnknown(values, FIELDS, path);
            ModeKey key = ModeKey.parse(rawKey);
            String defaultKit = text(values, "default-kit", path);
            List<String> allowed = strings(values, "allowed-kits", path);
            if (!kits.contains(defaultKit)) fail(path + ".default-kit references unknown kit '" + defaultKit + "'");
            for (String kit : allowed)
                if (!kits.contains(kit)) fail(path + ".allowed-kits references unknown kit '" + kit + "'");

            Map<String, ?> combat = map(values.get("combat"), path + ".combat");
            rejectUnknown(combat, COMBAT_FIELDS, path + ".combat");
            boolean naturalRegeneration = bool(combat, "natural-regeneration", path);
            if (naturalRegeneration)
                fail(path + ".combat.natural-regeneration must be false; duel healing is limited "
                        + "to Regeneration effects and health potions");
            Set<String> aliases = new LinkedHashSet<>(strings(values, "aliases", path));
            if (key.value().equals("classic")) aliases.add("default");
            for (String alias : aliases)
            {
                ModeKey.parse(alias);
                ModeKey prior = identities.putIfAbsent(alias, key);
                if (prior != null && !prior.equals(key))
                    fail(path + ".aliases identity '" + alias + "' conflicts with mode " + prior);
            }

            DuelMode mode = new DuelMode(key, text(values, "display-name", path),
                    text(values, "icon", path), enumeration(values, "handler", path, ModeHandlerType.class),
                    enumeration(values, "reset-policy", path, ResetPolicy.class),
                    new CombatFlags(bool(combat, "pvp", path), bool(combat, "projectiles", path),
                            bool(combat, "melee", path), bool(combat, "health-damage", path),
                            false, bool(combat, "hunger", path),
                            bool(combat, "fall-damage", path), bool(combat, "block-damage", path),
                            bool(combat, "no-hit-delay", path)),
                    integer(values, "target-score", path, 1),
                    new ModeDurationPolicy(Duration.ofSeconds(integer(values, "duration-seconds", path, 0)),
                            enumeration(values, "timeout-policy", path,
                                    ModeDurationPolicy.TimeoutPolicy.class)),
                    defaultKit, new LinkedHashSet<>(allowed), aliases,
                    bool(values, "enabled", path), bool(values, "leaderboard-enabled", path), false);
            result.put(key, mode);
        }
        return Map.copyOf(result);
    }

    private static void rejectUnknown(Map<String, ?> values, Set<String> allowed, String path)
    {
        Set<String> unknown = new LinkedHashSet<>(values.keySet()); unknown.removeAll(allowed);
        if (!unknown.isEmpty()) fail(path + " has unknown keys " + unknown);
    }

    private static Map<String, ?> map(Object value, String path)
    {
        if (!(value instanceof Map<?, ?>)) throw invalid(path + " must be a section");
        Map<?, ?> raw = (Map<?, ?>) value;
        Map<String, Object> result = new LinkedHashMap<>();
        for (Map.Entry<?, ?> entry : raw.entrySet())
        {
            if (!(entry.getKey() instanceof String)) throw invalid(path + " contains a non-string key");
            result.put((String) entry.getKey(), entry.getValue());
        }
        return result;
    }

    private static String text(Map<String, ?> map, String key, String path)
    {
        Object value = map.get(key);
        if (!(value instanceof String) || ((String) value).isBlank())
            throw invalid(path + "." + key + " must be nonblank text");
        return (String) value;
    }

    private static boolean bool(Map<String, ?> map, String key, String path)
    {
        Object value = map.get(key);
        if (!(value instanceof Boolean)) throw invalid(path + "." + key + " must be true or false");
        return (Boolean) value;
    }

    private static int integer(Map<String, ?> map, String key, String path, int minimum)
    {
        Object value = map.get(key);
        if (!(value instanceof Number)) throw invalid(path + "." + key + " must be a whole number >= " + minimum);
        Number number = (Number) value;
        if (number.doubleValue() != number.intValue() || number.intValue() < minimum)
            throw invalid(path + "." + key + " must be a whole number >= " + minimum);
        return number.intValue();
    }

    private static List<String> strings(Map<String, ?> map, String key, String path)
    {
        Object value = map.get(key);
        if (!(value instanceof List<?>)) throw invalid(path + "." + key + " must be a string list");
        List<String> result = new ArrayList<>();
        for (Object item : (List<?>) value)
        {
            if (!(item instanceof String) || ((String) item).isBlank())
                throw invalid(path + "." + key + " contains invalid text");
            String text = (String) item;
            if (!result.add(text)) throw invalid(path + "." + key + " contains duplicate '" + text + "'");
        }
        return result;
    }

    private static <E extends Enum<E>> E enumeration(Map<String, ?> map, String key, String path,
                                                       Class<E> type)
    {
        String value = text(map, key, path).toUpperCase(Locale.ROOT).replace('-', '_');
        try { return Enum.valueOf(type, value); }
        catch (IllegalArgumentException exception) { fail(path + "." + key + " has unsupported value '" + value + "'"); return null; }
    }

    private static IllegalArgumentException invalid(String message) { return new IllegalArgumentException(message); }
    private static void fail(String message) { throw invalid(message); }
}
