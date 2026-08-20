package me.alphatct3209.duels.game.modes;

import me.alphatct3209.duels.Duels;
import me.alphatct3209.duels.configuration.PluginFiles;
import me.alphatct3209.duels.game.kits.Kit;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

public final class ModeManager
{
    private static final String FILE_NAME = "modes.yml";
    private final Duels plugin;
    private final Map<ModeKey, DuelMode> modes;
    private final Map<String, ModeKey> identities;

    public ModeManager(Duels plugin)
    {
        this.plugin = plugin;
        File file = PluginFiles.advanced(plugin, FILE_NAME);
        YamlConfiguration yaml = YamlConfiguration.loadConfiguration(file);
        Set<String> rootKeys = yaml.getKeys(false);
        if (!rootKeys.equals(Set.of("Modes"))
                && !rootKeys.equals(Set.of("Config-Version", "Modes")))
            throw new IllegalStateException(
                    "modes.yml must contain only Config-Version and the top-level Modes section");
        ConfigurationSection section = yaml.getConfigurationSection("Modes");
        if (section == null) throw new IllegalStateException("modes.yml must contain a Modes section");
        Map<String, Object> raw = toMap(section);
        Set<String> kitKeys = new LinkedHashSet<>();
        for (Kit kit : plugin.getKitManager().getKitList()) kitKeys.add(kit.getKey());
        Map<ModeKey, DuelMode> loaded = new LinkedHashMap<>(ModeConfigParser.parse(raw, kitKeys));
        for (DuelMode mode : loaded.values())
        {
            if (org.bukkit.Material.matchMaterial(mode.icon()) == null)
                throw new IllegalStateException("Modes." + mode.key() + ".icon is not a valid material: '"
                        + mode.icon() + "'");
        }
        Map<String, ModeKey> aliases = identities(loaded.values());

        // Old kit-as-gamemode buckets remain addressable without changing modes.yml or statistics.
        for (Kit kit : plugin.getKitManager().getKitList())
        {
            String keyValue = kit.getKey();
            if (aliases.containsKey(keyValue)) continue;
            ModeKey key = ModeKey.parse(keyValue);
            DuelMode legacy = new DuelMode(key, kit.getName(), "CHEST", ModeHandlerType.LAST_STANDING,
                    ResetPolicy.WORLD, CombatFlags.standard(), 1,
                    new ModeDurationPolicy(Duration.ofMinutes(15),
                            ModeDurationPolicy.TimeoutPolicy.DRAW), keyValue, Set.of(keyValue),
                    Set.of(), true, true, true);
            loaded.put(key, legacy);
            aliases.put(keyValue, key);
            plugin.getLogger().info("Synthesized legacy duel mode '" + keyValue
                    + "' from kit '" + kit.getName() + "' without rewriting modes.yml or statistics.");
        }
        this.modes = Map.copyOf(loaded);
        this.identities = Map.copyOf(aliases);
    }

    public Collection<DuelMode> modes() { return modes.values(); }
    public List<DuelMode> enabledModes() { return modes.values().stream().filter(DuelMode::enabled).toList(); }
    public Set<String> leaderboardKeys()
    {
        Set<String> result = new LinkedHashSet<>();
        modes.values().stream().filter(DuelMode::enabled).filter(DuelMode::leaderboardEnabled)
                .forEach(mode -> result.add(mode.key().value()));
        return Set.copyOf(result);
    }

    public DuelMode require(ModeKey key)
    {
        DuelMode mode = modes.get(key);
        if (mode == null) throw new IllegalArgumentException("Unknown mode '" + key + "'");
        return mode;
    }

    public Optional<DuelMode> resolve(String input)
    {
        if (input == null) return Optional.empty();
        ModeKey key = identities.get(input.trim().toLowerCase(Locale.ROOT));
        return Optional.ofNullable(key == null ? null : modes.get(key));
    }

    public boolean isKitReferenced(String kitKey)
    {
        return modes.values().stream().anyMatch(mode -> mode.defaultKitKey().equals(kitKey)
                || mode.allowedKitKeys().contains(kitKey));
    }

    /** Mode key/alias wins. Otherwise a legacy kit must map to exactly one mode. */
    public ModeKey resolveLegacyArenaEntry(String entry, String path)
    {
        Optional<DuelMode> direct = resolve(entry);
        if (direct.isPresent()) return direct.get().key();
        Kit kit = plugin.getKitManager().getKitByNameOrKey(entry);
        if (kit == null) throw new IllegalStateException(path + " references unknown mode/kit '" + entry + "'");
        List<DuelMode> matches = modes.values().stream()
                .filter(mode -> mode.allowedKitKeys().contains(kit.getKey())).toList();
        if (matches.size() != 1)
            throw new IllegalStateException(path + " legacy kit '" + entry + "' maps to " + matches.size()
                    + " modes " + matches.stream().map(mode -> mode.key().value()).toList()
                    + "; configure Allowed-Modes explicitly");
        return matches.getFirst().key();
    }

    private static Map<String, ModeKey> identities(Collection<DuelMode> modes)
    {
        Map<String, ModeKey> result = new LinkedHashMap<>();
        for (DuelMode mode : modes)
        {
            putIdentity(result, mode.key().value(), mode.key());
            for (String alias : mode.aliases()) putIdentity(result, alias, mode.key());
        }
        return result;
    }

    private static void putIdentity(Map<String, ModeKey> result, String identity, ModeKey key)
    {
        ModeKey prior = result.putIfAbsent(identity, key);
        if (prior != null && !prior.equals(key))
            throw new IllegalStateException("Mode identity '" + identity + "' conflicts between " + prior + " and " + key);
    }

    private static Map<String, Object> toMap(ConfigurationSection section)
    {
        Map<String, Object> result = new LinkedHashMap<>();
        for (String key : section.getKeys(false))
        {
            Object value = section.get(key);
            result.put(key, value instanceof ConfigurationSection nested ? toMap(nested) : value);
        }
        return result;
    }
}
