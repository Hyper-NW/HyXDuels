package me.alphatct3209.duels.world;

import com.infernalsuite.asp.api.AdvancedSlimePaperAPI;
import com.infernalsuite.asp.api.loaders.SlimeLoader;
import com.infernalsuite.asp.api.world.SlimeWorld;
import com.infernalsuite.asp.api.world.SlimeWorldInstance;
import com.infernalsuite.asp.api.world.properties.SlimePropertyMap;
import com.infernalsuite.asp.loaders.file.FileLoader;
import me.alphatct3209.duels.Duels;
import me.alphatct3209.duels.game.GameState;
import me.alphatct3209.duels.game.arenas.Arena;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.world.WorldUnloadEvent;
import org.bukkit.scheduler.BukkitTask;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/** Owns immutable AdvancedSlimePaper templates used by duel arena locations. */
public final class SlimeWorldManager implements Listener
{
    private static final String DEFAULT_DIRECTORY = "slime_worlds";
    static final boolean PUBLISH_BUKKIT_WORLD_LOAD_EVENT = false;
    private static final long MAX_RETRY_TICKS = 20L * 60L;
    private static final long PLAYER_DRAIN_GRACE_TICKS = 2L;

    private final Duels plugin;
    private final AdvancedSlimePaperAPI api;
    private final SlimeLoader loader;
    /** Canonical name -> exact template name. */
    private final Map<String, String> managedWorlds = new LinkedHashMap<>();
    private final WorldRegenerationCoordinator coordinator = new WorldRegenerationCoordinator();
    private final Map<String, BukkitTask> retryTasks = new LinkedHashMap<>();
    private final Map<String, Integer> attempts = new LinkedHashMap<>();
    /** Pending world instance whose players were evacuated on an earlier server tick. */
    private final Map<String, World> drainingWorlds = new LinkedHashMap<>();
    private boolean stopping;

    public SlimeWorldManager(Duels plugin)
    {
        this.plugin = plugin;
        this.api = getApi();
        String configuredPath = plugin.getConfig().getString(
                "World-Loader.Slime-World-Directory", DEFAULT_DIRECTORY);
        if (configuredPath == null || configuredPath.isBlank())
        {
            throw new IllegalArgumentException("World-Loader.Slime-World-Directory cannot be empty");
        }
        this.loader = new FileLoader(new File(configuredPath).getAbsoluteFile());
    }

    /** Preloads every .slime template and validates every configured arena against that set. */
    public void loadConfiguredWorlds() throws Exception
    {
        for (String templateName : loader.listWorlds())
        {
            addManagedName(templateName);
        }

        ConfigurationSection arenas = plugin.getConfig().getConfigurationSection("Arenas");
        List<String> validationErrors = new ArrayList<>();
        if (arenas != null)
        {
            for (String arenaId : arenas.getKeys(false))
            {
                String base = "Arenas." + arenaId + ".";
                collectConfiguredWorld(base + "Spawn-One.World", validationErrors);
                collectConfiguredWorld(base + "Spawn-Two.World", validationErrors);
                collectConfiguredWorld(base + "Lobby.World", validationErrors);
            }
        }
        if (!validationErrors.isEmpty())
        {
            throw new IllegalStateException("Invalid arena world configuration:\n - "
                    + String.join("\n - ", validationErrors));
        }

        for (String exactName : managedWorlds.values())
        {
            World world = loadInitialWorld(exactName);
            if (verifiedLoadedWorld(exactName) != world)
            {
                throw new IllegalStateException("Arena world '" + exactName
                        + "' was loaded but is not simultaneously available from Bukkit and ASP");
            }
        }
    }

    /** Registers a runtime arena location only when HyXDuels preloaded its immutable template. */
    public void registerLoadedWorld(String worldName)
    {
        String exactName = requireName(worldName);
        String canonical = WorldRegenerationCoordinator.canonical(exactName);
        String existing = managedWorlds.get(canonical);
        if (existing == null)
        {
            throw new IllegalStateException("World '" + exactName + "' was not preloaded from "
                    + "World-Loader.Slime-World-Directory. Add its .slime template and restart "
                    + "the server before creating an arena there");
        }
        requireAvailable(existing);
    }

    public void requireAvailable(String worldName)
    {
        World verified = verifiedLoadedWorld(worldName);
        if (verified == null)
        {
            World bukkit = findBukkitWorld(worldName);
            String detail = bukkit == null ? "Bukkit does not expose it"
                    : "Bukkit exposes it but it is not the same read-only ASP live instance";
            throw new IllegalStateException("ASP arena world '" + worldName
                    + "' is unavailable: " + detail + ". Check the .slime template and startup logs.");
        }
        configureLiveWorld(verified);
    }

    /** Resolves a configured arena location only through ASP's live-world registry. */
    public World requireManagedWorld(String worldName)
    {
        String exact = requireName(worldName);
        if (!managedWorlds.containsKey(WorldRegenerationCoordinator.canonical(exact)))
            throw new IllegalStateException("World '" + exact
                    + "' is not a configured HyXDuels slime world");
        World world = verifiedLoadedWorld(exact);
        if (world == null)
            throw new IllegalStateException("ASP arena world '" + exact
                    + "' is not loaded or no longer owns its live server instance");
        return world;
    }

    /** Lightweight admission check: pending names and stale same-name World objects are rejected. */
    public boolean isAvailable(Location location)
    {
        if (location == null || location.getWorld() == null || stopping) return false;
        String worldName = location.getWorld().getName();
        try
        {
            World live = verifiedLoadedWorld(worldName);
            return coordinator.canAdmit(worldName, location.getWorld(), live);
        }
        catch (RuntimeException | LinkageError failure)
        {
            return false;
        }
    }

    /**
     * Final main-thread teleport gate. getChunkAt deliberately probes ASP's chunk system before the
     * player is moved; a shutting-down world throws here and is rejected instead of crashing the
     * player's following tick.
     */
    public boolean teleportSafely(Player player, Location configuredDestination)
    {
        return teleport(player, configuredDestination);
    }

    public boolean teleportWithinActiveWorld(Player player, Location configuredDestination)
    {
        return teleport(player, configuredDestination);
    }

    private boolean teleport(Player player, Location configuredDestination)
    {
        Objects.requireNonNull(player, "player");
        if (!Bukkit.isPrimaryThread())
            throw new IllegalStateException("Arena teleports must run on the Bukkit main thread");
        try
        {
            Location destination = prepareDestination(configuredDestination);
            World expected = destination.getWorld();
            if (!player.teleport(destination) || player.getWorld() != expected)
            {
                plugin.getLogger().warning("Rejected arena teleport for '" + player.getName()
                        + "': Bukkit did not place the player in the verified world instance");
                return false;
            }
            return true;
        }
        catch (RuntimeException | LinkageError failure)
        {
            String target = configuredDestination == null || configuredDestination.getWorld() == null
                    ? "unknown" : configuredDestination.getWorld().getName();
            plugin.getLogger().log(java.util.logging.Level.WARNING,
                    "Rejected arena teleport for '" + player.getName() + "' into '" + target
                            + "' because the ASP world/chunk system is unavailable", failure);
            return false;
        }
    }

    private Location prepareDestination(Location configured)
    {
        if (configured == null || configured.getWorld() == null)
            throw new IllegalStateException("Arena destination has no loaded world");
        World configuredWorld = configured.getWorld();
        String worldName = configuredWorld.getName();
        World live = verifiedLoadedWorld(worldName);
        boolean currentInstance = configuredWorld == live;
        if (!currentInstance || coordinator.isPending(worldName))
            throw new IllegalStateException(coordinator.isPending(worldName)
                    ? "Arena world is pending regeneration"
                    : "Arena location references a stale or non-ASP World instance");

        // This is the same operation that failed in the reported player tick, but safely before teleport.
        live.getChunkAt(configured.getBlockX() >> 4, configured.getBlockZ() >> 4);
        World verifiedAgain = verifiedLoadedWorld(worldName);
        if (configuredWorld != verifiedAgain || coordinator.isPending(worldName))
            throw new IllegalStateException("Arena world changed while preparing its destination chunk");
        Location destination = configured.clone();
        destination.setWorld(live);
        return destination;
    }

    public boolean isPending(String worldName)
    {
        return worldName != null && coordinator.isPending(worldName);
    }

    public boolean isRegenerating(String worldName)
    {
        return isPending(worldName);
    }

    public Collection<String> pendingWorlds()
    {
        return coordinator.pendingWorlds();
    }

    /** True for every configured/runtime arena map or lobby world, including pending regeneration. */
    public boolean isManagedWorld(String worldName)
    {
        if (worldName == null || worldName.isBlank())
        {
            return false;
        }
        return managedWorlds.containsKey(WorldRegenerationCoordinator.canonical(worldName));
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onUnexpectedWorldUnload(WorldUnloadEvent event)
    {
        String worldName = event.getWorld().getName();
        if (stopping || !isManagedWorld(worldName) || coordinator.isPending(worldName)) return;
        String canonical = WorldRegenerationCoordinator.canonical(worldName);
        List<Arena> mapArenas = plugin.getArenaManager().getArenaList().stream()
                .filter(arena -> arena.mapUsesWorld(canonical)).toList();
        boolean busy = !event.getWorld().getPlayers().isEmpty()
                || mapArenas.stream().anyMatch(arena -> arena.getGameState() == GameState.PLAYING);
        if (busy)
        {
            event.setCancelled(true);
            plugin.getLogger().warning("Blocked an external unload of active ASP arena world '"
                    + worldName + "'. Move its players out or let the match finish first.");
            return;
        }

        // Allow tools such as Multiverse to forget stale folder-backed registrations. The world is
        // immediately admission-locked and restored exclusively from the immutable slime template.
        coordinator.request(canonical);
        for (Arena arena : mapArenas) arena.prepareForRegeneration();
        for (Arena arena : plugin.getArenaManager().getArenaList())
        {
            if (!arena.mapUsesWorld(canonical) && arena.lobbyUsesWorld(canonical))
                arena.cancelQueueForLobbyReload();
        }
        scheduleAttempt(canonical, 1L);
        plugin.getLogger().info("Allowed external registry cleanup for ASP arena world '"
                + worldName + "'; its read-only .slime instance will be restored next tick.");
    }


    /** Acquires the world admission lock without tearing down the terminal arena mid-scoring. */
    public void lockRegeneration(String worldName)
    {
        if (stopping || !plugin.isEnabled())
        {
            throw new IllegalStateException("Cannot lock arena regeneration while the plugin is stopping");
        }
        if (!Bukkit.isPrimaryThread())
        {
            throw new IllegalStateException("Arena world regeneration must be locked on the Bukkit main thread");
        }
        String canonical = WorldRegenerationCoordinator.canonical(worldName);
        if (!managedWorlds.containsKey(canonical))
        {
            throw new IllegalStateException("Cannot regenerate unmanaged ASP world '" + worldName + "'");
        }
        coordinator.request(canonical);
    }

    /** Called after a terminal duel has atomically moved its arena to REGENERATING and torn down. */
    public void requestRegeneration(String worldName)
    {
        if (stopping || !plugin.isEnabled())
        {
            return;
        }
        if (!Bukkit.isPrimaryThread())
        {
            throw new IllegalStateException("Arena world regeneration must be requested on the Bukkit main thread");
        }
        String canonical = WorldRegenerationCoordinator.canonical(worldName);
        if (!managedWorlds.containsKey(canonical))
        {
            throw new IllegalStateException("Cannot regenerate unmanaged ASP world '" + worldName + "'");
        }
        coordinator.request(canonical);
        drainAndSchedule(canonical);
    }

    private void drainAndSchedule(String canonical)
    {
        List<Arena> mapArenas = plugin.getArenaManager().getArenaList().stream()
                .filter(arena -> arena.mapUsesWorld(canonical)).toList();
        for (Arena arena : mapArenas)
        {
            if (arena.getGameState() != GameState.PLAYING)
            {
                arena.prepareForRegeneration();
            }
        }
        // A target map may also be another arena's separate lobby. Cancel occupants before unload.
        for (Arena arena : plugin.getArenaManager().getArenaList())
        {
            if (!arena.mapUsesWorld(canonical) && arena.lobbyUsesWorld(canonical))
            {
                arena.cancelQueueForLobbyReload();
            }
        }

        List<GameState> states = mapArenas.stream().map(Arena::getGameState).toList();
        if (coordinator.isDrainReady(canonical, states))
        {
            scheduleAttempt(canonical, 1L);
        }
    }

    private void scheduleAttempt(String canonical, long delay)
    {
        if (stopping || retryTasks.containsKey(canonical))
        {
            return;
        }
        BukkitTask task = Bukkit.getScheduler().runTaskLater(plugin, () -> {
            retryTasks.remove(canonical);
            regenerate(canonical);
        }, delay);
        retryTasks.put(canonical, task);
    }

    private void regenerate(String canonical)
    {
        if (stopping || !coordinator.isPending(canonical))
        {
            return;
        }
        List<Arena> mapArenas = plugin.getArenaManager().getArenaList().stream()
                .filter(arena -> arena.mapUsesWorld(canonical)).toList();
        if (mapArenas.stream().anyMatch(arena -> arena.getGameState() == GameState.PLAYING))
        {
            return;
        }
        try
        {
            for (Arena arena : mapArenas)
            {
                arena.prepareForRegeneration();
            }
            String exactName = exactName(canonical);
            World oldWorld = findBukkitWorld(exactName);
            if (oldWorld != null)
            {
                if (!playerDrainSettled(canonical, oldWorld)) return;
                if (!Bukkit.unloadWorld(oldWorld, false))
                {
                    throw new IllegalStateException("Bukkit.unloadWorld(world, false) returned false");
                }
                drainingWorlds.remove(canonical);
                if (findBukkitWorld(exactName) != null)
                {
                    throw new IllegalStateException("Bukkit still exposes the world after unload");
                }
            }

            // Always reread the immutable template, including retries after an already-completed unload.
            SlimeWorld template = readImmutableTemplate(exactName);
            SlimeWorldInstance loaded = api.loadWorld(template, PUBLISH_BUKKIT_WORLD_LOAD_EVENT);
            World freshWorld = loaded == null ? null : loaded.getBukkitWorld();
            World verified = verifiedLoadedWorld(exactName);
            if (freshWorld == null || verified == null || freshWorld != verified)
            {
                throw new IllegalStateException("ASP load completed without a verified Bukkit world instance");
            }
            configureLiveWorld(freshWorld);

            int rebound = 0;
            for (Arena arena : plugin.getArenaManager().getArenaList())
            {
                if (arena.containsWorldName(exactName))
                {
                    rebound += arena.rebindWorld(exactName, freshWorld);
                }
            }
            if (rebound < mapArenas.size() * 2)
            {
                throw new IllegalStateException("Only " + rebound
                        + " location(s) rebound for " + mapArenas.size() + " map arena(s)");
            }
            for (Arena arena : mapArenas)
            {
                arena.releaseAfterRegeneration();
            }

            coordinator.complete(canonical);
            attempts.remove(canonical);
            drainingWorlds.remove(canonical);
            plugin.getLogger().info("Regenerated immutable ASP arena world '" + exactName
                    + "', rebound " + rebound + " location(s), and released "
                    + mapArenas.size() + " map arena(s).");
        }
        catch (Exception | LinkageError exception)
        {
            failAndRetry(canonical, exception);
        }
    }

    private void failAndRetry(String canonical, Throwable failure)
    {
        if (stopping)
        {
            return;
        }
        int attempt = attempts.merge(canonical, 1, Integer::sum);
        if (attempt == 1 || attempt % 5 == 0)
        {
            plugin.getLogger().log(java.util.logging.Level.SEVERE,
                    "Arena world '" + exactName(canonical) + "' remains pending and unavailable after regeneration attempt "
                            + attempt + ". HyXDuels will retry; fix player evacuation, template, or ASP errors.",
                    failure);
        }
        long delay = Math.min(MAX_RETRY_TICKS, 20L << Math.min(attempt, 5));
        scheduleAttempt(canonical, delay);
    }

    private void evacuate(World target, boolean requireUnmanagedDestination)
    {
        if (target.getPlayers().isEmpty())
        {
            return;
        }
        World safe = findSafeWorld(target.getName(), requireUnmanagedDestination);
        if (safe == null)
        {
            throw new IllegalStateException("No safe loaded non-target world is available to evacuate players");
        }
        Location destination = safe.getSpawnLocation();
        for (Player player : List.copyOf(target.getPlayers()))
        {
            boolean teleported = player.teleport(destination);
            World current = player.getWorld();
            if (!teleported || sameWorld(current.getName(), target.getName()))
            {
                throw new IllegalStateException("Could not evacuate online player '" + player.getName()
                        + "' from world '" + target.getName() + "'");
            }
        }
        if (!target.getPlayers().isEmpty())
        {
            throw new IllegalStateException("Target world is not empty after verified teleports");
        }
    }

    /**
     * Player teleports update Bukkit state immediately, but the connection/player tick may still be
     * finishing its work for the old level. Keep the world alive for two complete ticks after the
     * last evacuation and recheck every online player's exact World instance before shutting chunks down.
     */
    private boolean playerDrainSettled(String canonical, World target)
    {
        World previouslyDrained = drainingWorlds.get(canonical);
        if (previouslyDrained != target || hasPlayerReference(target))
        {
            evacuate(target, false);
            if (hasPlayerReference(target))
            {
                throw new IllegalStateException("World still has an online player reference after evacuation");
            }
            drainingWorlds.put(canonical, target);
            scheduleAttempt(canonical, PLAYER_DRAIN_GRACE_TICKS);
            return false;
        }
        return true;
    }

    private boolean hasPlayerReference(World target)
    {
        if (!target.getPlayers().isEmpty()) return true;
        return Bukkit.getOnlinePlayers().stream().anyMatch(player -> player.getWorld() == target);
    }

    private World findSafeWorld(String targetName, boolean requireUnmanaged)
    {
        for (World candidate : Bukkit.getWorlds())
        {
            String canonical = WorldRegenerationCoordinator.canonical(candidate.getName());
            if (sameWorld(candidate.getName(), targetName) || coordinator.isPending(canonical))
            {
                continue;
            }
            if (!managedWorlds.containsKey(canonical))
            {
                return candidate;
            }
        }
        if (!requireUnmanaged)
        {
            for (World candidate : Bukkit.getWorlds())
            {
                if (!sameWorld(candidate.getName(), targetName) && !isPending(candidate.getName()))
                {
                    return candidate;
                }
            }
        }
        return null;
    }

    /** Stops retries, evacuates managed-world players where safe, and unloads without saving. */
    public void shutdown()
    {
        stopping = true;
        retryTasks.values().forEach(BukkitTask::cancel);
        retryTasks.clear();
        drainingWorlds.clear();
        for (String exactName : managedWorlds.values())
        {
            World world = findBukkitWorld(exactName);
            if (world == null)
            {
                continue;
            }
            try
            {
                evacuate(world, true);
                if (!Bukkit.unloadWorld(world, false))
                {
                    plugin.getLogger().warning("Could not unload ASP arena world '" + exactName
                            + "' without saving during disable.");
                }
            }
            catch (RuntimeException exception)
            {
                plugin.getLogger().log(java.util.logging.Level.WARNING,
                        "Could not safely evacuate/unload ASP arena world '" + exactName
                                + "' during disable.", exception);
            }
        }
        managedWorlds.clear();
    }

    /** Backward-compatible disable entry point. */
    public void unloadWorlds()
    {
        shutdown();
    }

    private void collectConfiguredWorld(String path, List<String> errors)
    {
        String worldName = plugin.getConfig().getString(path);
        if (worldName == null || worldName.isBlank())
        {
            errors.add(path + " must be a nonblank ASP template name");
            return;
        }
        try
        {
            String exact = requireName(worldName);
            String template = managedWorlds.get(WorldRegenerationCoordinator.canonical(exact));
            if (template == null)
            {
                errors.add(path + ": no .slime template named '" + exact
                        + "' exists in World-Loader.Slime-World-Directory");
            }
            else if (!template.equals(exact))
            {
                errors.add(path + ": use exact template name '" + template + "'");
            }
        }
        catch (IllegalArgumentException exception)
        {
            errors.add(path + ": " + exception.getMessage());
        }
    }

    private void addManagedName(String worldName)
    {
        String exact = requireName(worldName);
        String canonical = WorldRegenerationCoordinator.canonical(exact);
        String previous = managedWorlds.putIfAbsent(canonical, exact);
        if (previous != null && !previous.equals(exact))
        {
            throw new IllegalArgumentException("world name conflicts by case with '" + previous + "'");
        }
    }

    private World loadInitialWorld(String worldName) throws Exception
    {
        if (!loader.worldExists(worldName))
        {
            throw new IllegalStateException("Missing immutable arena template '" + worldName
                    + "' in World-Loader.Slime-World-Directory");
        }
        World verified = verifiedLoadedWorld(worldName);
        if (verified != null)
        {
            configureLiveWorld(verified);
            plugin.getLogger().info("Verified preloaded ASP arena world '" + worldName + "'.");
            return verified;
        }
        World bukkitWorld = findBukkitWorld(worldName);
        if (bukkitWorld != null)
        {
            plugin.getLogger().warning("Replacing preloaded conflicting world '" + worldName
                    + "' with the configured read-only .slime template.");
            evacuate(bukkitWorld, true);
            if (!Bukkit.unloadWorld(bukkitWorld, false))
            {
                throw new IllegalStateException("Bukkit refused to unload the conflicting world '"
                        + worldName + "'. If it is the server level-name/default world, configure a "
                        + "different primary world; HyXDuels cannot replace a primary Anvil world "
                        + "after server world initialization");
            }
            if (findBukkitWorld(worldName) != null)
            {
                throw new IllegalStateException("Bukkit still exposes conflicting world '"
                        + worldName + "' after unload; the .slime template was not loaded");
            }
        }
        SlimeWorld template = readImmutableTemplate(worldName);
        SlimeWorldInstance instance = api.loadWorld(template, PUBLISH_BUKKIT_WORLD_LOAD_EVENT);
        World world = instance == null ? null : instance.getBukkitWorld();
        if (world == null || verifiedLoadedWorld(worldName) != world)
        {
            throw new IllegalStateException("AdvancedSlimePaper did not expose a verified Bukkit world for '"
                    + worldName + "'");
        }
        configureLiveWorld(world);
        plugin.getLogger().info("Preloaded ASP arena world '" + worldName + "'.");
        return world;
    }

    private SlimeWorld readImmutableTemplate(String worldName) throws Exception
    {
        SlimeWorld template = api.readWorld(loader, worldName, true, new SlimePropertyMap());
        if (!template.isReadOnly())
            throw new IllegalStateException("ASP template '" + worldName + "' was not opened read-only");
        return template;
    }

    private void configureLiveWorld(World world)
    {
        world.setAutoSave(false);
    }

    private World verifiedLoadedWorld(String worldName)
    {
        String exact = requireName(worldName);
        World bukkit = findBukkitWorld(exact);
        SlimeWorldInstance direct = api.getLoadedWorld(exact);
        SlimeWorldInstance instance = selectOwnedInstance(
                exact, bukkit, direct, api.getLoadedWorlds());
        return instance == null ? null : bukkit;
    }

    /**
     * ASP ownership is established by the exact Bukkit World object exposed by a read-only live
     * instance. Writable instances are deliberately rejected so runtime arena creation can never
     * adopt a normal/imported world in place of HyXDuels' immutable .slime template.
     */
    static SlimeWorldInstance selectOwnedInstance(String worldName, World bukkit,
                                                   SlimeWorldInstance direct,
                                                   Collection<? extends SlimeWorldInstance> loaded)
    {
        if (ownsBukkitWorld(worldName, bukkit, direct)) return direct;
        if (loaded == null) return null;
        for (SlimeWorldInstance candidate : loaded)
        {
            if (ownsBukkitWorld(worldName, bukkit, candidate)) return candidate;
        }
        return null;
    }

    private static boolean ownsBukkitWorld(String worldName, World bukkit,
                                           SlimeWorldInstance instance)
    {
        if (bukkit == null || instance == null || !instance.isReadOnly()) return false;
        World aspBukkit = instance.getBukkitWorld();
        return aspBukkit == bukkit
                && sameWorld(instance.getName(), worldName)
                && sameWorld(aspBukkit.getName(), worldName);
    }

    private World findBukkitWorld(String worldName)
    {
        for (World world : Bukkit.getWorlds())
        {
            if (sameWorld(world.getName(), worldName))
            {
                return world;
            }
        }
        return null;
    }

    private String exactName(String canonical)
    {
        String exact = managedWorlds.get(WorldRegenerationCoordinator.canonical(canonical));
        if (exact == null)
        {
            throw new IllegalStateException("World '" + canonical + "' is not lifecycle-managed");
        }
        return exact;
    }

    private static String requireName(String worldName)
    {
        if (worldName == null || worldName.isBlank())
        {
            throw new IllegalArgumentException("World name cannot be blank");
        }
        return worldName.trim();
    }

    private static boolean sameWorld(String first, String second)
    {
        return WorldRegenerationCoordinator.canonical(first)
                .equals(WorldRegenerationCoordinator.canonical(second));
    }

    private AdvancedSlimePaperAPI getApi()
    {
        try
        {
            return Objects.requireNonNull(AdvancedSlimePaperAPI.instance());
        }
        catch (RuntimeException | LinkageError exception)
        {
            throw new IllegalStateException(
                    "HyXDuels requires an AdvancedSlimePaper server with the ASP 4.x API.", exception);
        }
    }
}
