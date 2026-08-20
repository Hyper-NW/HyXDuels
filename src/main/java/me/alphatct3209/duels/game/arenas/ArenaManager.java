package me.alphatct3209.duels.game.arenas;

import me.alphatct3209.duels.Duels;
import me.alphatct3209.duels.game.GameState;
import me.alphatct3209.duels.game.kits.Kit;
import me.alphatct3209.duels.game.modes.DuelMode;
import me.alphatct3209.duels.game.modes.DuelSelection;
import me.alphatct3209.duels.game.modes.ModeKey;
import me.alphatct3209.duels.utils.ConfigUtils;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

public class ArenaManager
{
    private final Duels plugin;
    private final List<Arena> arenaList = new ArrayList<>();

    public ArenaManager(Duels plugin)
    {
        this.plugin = plugin;
        loadArenas();
    }

    private void loadArenas()
    {
        FileConfiguration config = plugin.getConfig();
        if (!config.contains("Arenas") || config.getConfigurationSection("Arenas") == null) return;
        for (String arenaIdStr : config.getConfigurationSection("Arenas").getKeys(false))
        {
            String path = "Arenas." + arenaIdStr;
            int id;
            try { id = Integer.parseInt(arenaIdStr); }
            catch (NumberFormatException exception)
            {
                Bukkit.getLogger().severe("Failed to parse integer '" + arenaIdStr + "' from config.yml (arena id)");
                continue;
            }

            Set<ModeKey> allowed = readAllowedModes(config, path);
            Map<String, Object> configuredSettings = new LinkedHashMap<>();
            for (ArenaSettings.Flag flag : ArenaSettings.Flag.values())
            {
                Object value = config.get(path + ".Settings." + flag.key());
                if (value != null)
                {
                    if (!(value instanceof Boolean))
                        throw new IllegalStateException(path + ".Settings." + flag.key() + " must be true or false");
                    configuredSettings.put(flag.key(), value);
                }
            }

            Map<String, Location> points = new LinkedHashMap<>();
            if (config.getConfigurationSection(path + ".Points") != null)
            {
                for (String point : config.getConfigurationSection(path + ".Points").getKeys(false))
                    points.put(point, ConfigUtils.getLocation(path + ".Points." + point));
            }
            Map<String, Number> objective = new LinkedHashMap<>();
            if (config.getConfigurationSection(path + ".Objective") != null)
            {
                for (String key : config.getConfigurationSection(path + ".Objective").getKeys(false))
                {
                    Object raw = config.get(path + ".Objective." + key);
                    if (!(raw instanceof Number number))
                        throw new IllegalStateException(path + ".Objective." + key + " must be numeric");
                    objective.put(key, number);
                }
            }

            Arena arena = new Arena(plugin, id, config.getString(path + ".Name"),
                    ConfigUtils.getLocation(path + ".Spawn-One"), ConfigUtils.getLocation(path + ".Spawn-Two"),
                    ConfigUtils.getLocation(path + ".Lobby"), config.getInt(path + ".Countdown-Seconds"),
                    allowed, ArenaSettings.fromMap(configuredSettings), points,
                    new ArenaObjectiveSettings(objective));
            plugin.getSlimeWorldManager().requireAvailable(arena.getMapWorldName());
            plugin.getSlimeWorldManager().requireAvailable(Objects.requireNonNull(arena.getLobby().getWorld()).getName());
            arenaList.add(arena);
        }
    }

    private Set<ModeKey> readAllowedModes(FileConfiguration config, String arenaPath)
    {
        String modernPath = arenaPath + ".Allowed-Modes";
        if (config.isSet(modernPath))
        {
            if (config.isSet(arenaPath + ".Allowed-Kits"))
                plugin.getLogger().warning(arenaPath + " has both Allowed-Modes and legacy Allowed-Kits; "
                        + "Allowed-Modes is authoritative and legacy data is left untouched.");
            List<String> values = stringList(config, modernPath);
            Set<ModeKey> result = new LinkedHashSet<>();
            for (int index = 0; index < values.size(); index++)
            {
                String value = values.get(index);
                DuelMode mode = plugin.getModeManager().resolve(value).orElse(null);
                if (mode == null)
                    throw new IllegalStateException(modernPath + "[" + index + "] references unknown mode '" + value + "'");
                if (!mode.key().value().equals(value))
                    throw new IllegalStateException(modernPath + "[" + index + "] must use canonical key '"
                            + mode.key() + "', not alias '" + value + "'");
                if (!result.add(mode.key()))
                    throw new IllegalStateException(modernPath + " contains duplicate mode '" + value + "'");
            }
            return result;
        }

        String legacyPath = arenaPath + ".Allowed-Kits";
        List<String> legacy = config.isSet(legacyPath) ? stringList(config, legacyPath) : List.of();
        Set<ModeKey> result = new LinkedHashSet<>();
        for (int index = 0; index < legacy.size(); index++)
        {
            ModeKey resolved = plugin.getModeManager().resolveLegacyArenaEntry(legacy.get(index),
                    legacyPath + "[" + index + "]");
            if (!result.add(resolved))
                throw new IllegalStateException(legacyPath + " contains duplicate resolved mode '" + resolved + "'");
        }
        return result;
    }

    private List<String> stringList(FileConfiguration config, String path)
    {
        Object raw = config.get(path);
        if (!(raw instanceof List<?> list)) throw new IllegalStateException(path + " must be a list");
        List<String> values = new ArrayList<>();
        for (int index = 0; index < list.size(); index++)
        {
            if (!(list.get(index) instanceof String value) || value.isBlank())
                throw new IllegalStateException(path + "[" + index + "] must be nonblank text");
            values.add(value);
        }
        return values;
    }

    public void save(ArenaConfig arenaConfig)
    {
        if (getArena(arenaConfig.getId()) != null)
            throw new IllegalStateException("Arena id " + arenaConfig.getId() + " already exists");
        Arena arena = new Arena(plugin, arenaConfig);
        Location spawnOne = arena.getSpawnOne();
        Location spawnTwo = arena.getSpawnTwo();
        Location lobby = arena.getLobby();
        plugin.getSlimeWorldManager().registerLoadedWorld(Objects.requireNonNull(spawnOne.getWorld()).getName());
        plugin.getSlimeWorldManager().registerLoadedWorld(Objects.requireNonNull(spawnTwo.getWorld()).getName());
        plugin.getSlimeWorldManager().registerLoadedWorld(Objects.requireNonNull(lobby.getWorld()).getName());

        String path = "Arenas." + arenaConfig.getId();
        FileConfiguration config = plugin.getConfig();
        config.set(path + ".Name", arenaConfig.getName());
        config.set(path + ".Countdown-Seconds", arenaConfig.getCountdownSeconds());
        saveLocation(config, path + ".Spawn-One", spawnOne);
        saveLocation(config, path + ".Spawn-Two", spawnTwo);
        saveLocation(config, path + ".Lobby", lobby);
        config.set(path + ".Allowed-Modes", List.of());
        ArenaSettings settings = arenaConfig.getSettings();
        for (ArenaSettings.Flag flag : ArenaSettings.Flag.values())
            config.set(path + ".Settings." + flag.key(), settings.get(flag));
        plugin.saveArenaData();
        arenaList.add(arena);
    }

    private void saveLocation(FileConfiguration config, String path, Location location)
    {
        config.set(path + ".World", Objects.requireNonNull(location.getWorld()).getName());
        config.set(path + ".X", location.getX()); config.set(path + ".Y", location.getY());
        config.set(path + ".Z", location.getZ()); config.set(path + ".Yaw", location.getYaw());
        config.set(path + ".Pitch", location.getPitch());
    }

    public boolean admit(Player player, Arena arena)
    {
        DuelSelection selection = plugin.getSelectionService().resolve(player.getUniqueId());
        Kit kit = plugin.getSelectionService().kit(selection);
        if (getArena(player) != null || !canAdmit(arena, selection, kit)) return false;
        boolean admitted = arena.addPlayer(player, selection, kit);
        if (admitted) plugin.getQueueManager().cancel(player.getUniqueId());
        return admitted;
    }

    public boolean admitChallenge(Player challenger, Player target, Arena arena,
                                  DuelSelection selection, Kit challengerKit, Kit targetKit)
    {
        Objects.requireNonNull(challenger, "challenger"); Objects.requireNonNull(target, "target");
        if (challenger.equals(target) || !challenger.isOnline() || !target.isOnline()
                || getArena(challenger) != null || getArena(target) != null || arena == null
                || !arena.isCompletelyEmpty() || !canAdmit(arena, selection, challengerKit)
                || !canAdmit(arena, selection, targetKit)) return false;
        boolean admitted = arena.addChallengePlayers(challenger, target, selection,
                challengerKit, targetKit);
        if (admitted)
        {
            plugin.getQueueManager().cancel(challenger.getUniqueId());
            plugin.getQueueManager().cancel(target.getUniqueId());
        }
        return admitted;
    }

    /** Shared explicit/automatic/challenge compatibility predicate. */
    public boolean canAdmit(Arena arena, DuelSelection selection, Kit kit)
    {
        if (arena == null || selection == null || kit == null || !isOpen(arena)) return false;
        DuelMode mode;
        try { mode = plugin.getModeManager().require(selection.modeKey()); }
        catch (IllegalArgumentException exception) { return false; }
        return mode.enabled() && arena.readyFor(mode) && mode.allowsKit(kit.getKey()) && selection.kitKey().equals(kit.getKey())
                && plugin.getKitManager().getKitByCanonicalKey(kit.getKey()) == kit
                && arena.canClaimSelection(selection) && isCompatible(arena, selection.modeKey());
    }

    public Arena findAvailableArena(DuelSelection selection)
    {
        Kit kit = plugin.getSelectionService().kit(selection);
        return arenaList.stream().filter(arena -> canAdmit(arena, selection, kit)).findFirst().orElse(null);
    }

    public Arena findCompletelyEmptyCompatibleArena(DuelSelection selection)
    {
        Kit kit = plugin.getSelectionService().kit(selection);
        return arenaList.stream().filter(Arena::isCompletelyEmpty)
                .filter(arena -> canAdmit(arena, selection, kit)).findFirst().orElse(null);
    }

    public boolean isOpen(Arena arena)
    {
        return arena != null && arena.getGameState() == GameState.IDLE && arena.getPlayers().size() < 2
                && plugin.getSlimeWorldManager().isAvailable(arena.getLobby())
                && plugin.getSlimeWorldManager().isAvailable(arena.getSpawnOne())
                && plugin.getSlimeWorldManager().isAvailable(arena.getSpawnTwo());
    }

    public boolean isCompatible(Arena arena, ModeKey mode)
    {
        List<Set<ModeKey>> routes = arenaList.stream().map(Arena::getAllowedModeKeys).toList();
        return ArenaModeRouting.isCompatible(mode, arena.getAllowedModeKeys(), routes);
    }

    public void addAllowedMode(Arena arena, DuelMode mode)
    {
        Set<ModeKey> keys = new LinkedHashSet<>(arena.getAllowedModeKeys()); keys.add(mode.key());
        setAllowedModes(arena, keys);
    }

    public void removeAllowedMode(Arena arena, DuelMode mode)
    {
        Set<ModeKey> keys = new LinkedHashSet<>(arena.getAllowedModeKeys()); keys.remove(mode.key());
        setAllowedModes(arena, keys);
    }

    public void clearAllowedModes(Arena arena) { setAllowedModes(arena, Set.of()); }

    private void setAllowedModes(Arena arena, Collection<ModeKey> keys)
    {
        arena.setAllowedModeKeys(keys);
        plugin.getConfig().set("Arenas." + arena.getId() + ".Allowed-Modes",
                keys.stream().map(ModeKey::value).toList());
        plugin.saveArenaData();
    }

    public void setPoint(Arena arena, String name, Location location)
    {
        Objects.requireNonNull(arena, "arena");
        arena.setPoint(name, location);
        saveLocation(plugin.getConfig(), "Arenas." + arena.getId() + ".Points."
                + name.toLowerCase(java.util.Locale.ROOT), location);
        plugin.saveArenaData();
    }

    public void removePoint(Arena arena, String name)
    {
        arena.removePoint(name);
        plugin.getConfig().set("Arenas." + arena.getId() + ".Points."
                + name.toLowerCase(java.util.Locale.ROOT), null);
        plugin.saveArenaData();
    }

    public void setObjective(Arena arena, String key, Double value)
    {
        ArenaObjectiveSettings changed = value == null
                ? arena.getObjectiveSettings().without(key)
                : arena.getObjectiveSettings().with(key, value);
        arena.setObjectiveSettings(changed);
        plugin.getConfig().set("Arenas." + arena.getId() + ".Objective."
                + ArenaObjectiveSettings.normalize(key), value);
        plugin.saveArenaData();
    }

    public void setSetting(Arena arena, ArenaSettings.Flag flag, boolean value)
    {
        arena.setSettings(arena.getSettings().with(flag, value));
        plugin.getConfig().set("Arenas." + arena.getId() + ".Settings." + flag.key(), value);
        plugin.saveArenaData();
    }

    public List<Arena> getActiveArenas(World world)
    {
        return arenaList.stream().filter(arena -> arena.getGameState() == GameState.PLAYING)
                .filter(arena -> world != null && arena.mapUsesWorld(world.getName())).toList();
    }

    public Optional<Arena> getUniqueActiveArena(World world)
    {
        List<Arena> matches = getActiveArenas(world);
        return matches.size() == 1 ? Optional.of(matches.getFirst()) : Optional.empty();
    }

    public Arena getArena(Player player)
    {
        UUID uuid = player.getUniqueId();
        return arenaList.stream().filter(arena -> arena.getPlayers().contains(uuid)).findFirst().orElse(null);
    }

    public Arena getArena(int id) { return arenaList.stream().filter(arena -> arena.getId() == id).findFirst().orElse(null); }
    public List<Arena> getArenaList() { return List.copyOf(arenaList); }
    public void shutdown() { arenaList.forEach(Arena::shutdown); }
    public int getNextId()
    {
        int active = arenaList.stream().mapToInt(Arena::getId).max().orElse(0);
        int pending = ArenaConfig.creationMap.values().stream()
                .mapToInt(ArenaConfig::getId).max().orElse(0);
        return Math.max(active, pending) + 1;
    }
}
