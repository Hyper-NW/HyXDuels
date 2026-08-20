package me.alphatct3209.duels.game.arenas;

import me.alphatct3209.duels.Duels;
import me.alphatct3209.duels.divisions.DivisionManager;
import me.alphatct3209.duels.game.Countdown;
import me.alphatct3209.duels.game.Game;
import me.alphatct3209.duels.game.GameState;
import me.alphatct3209.duels.game.kits.Kit;
import me.alphatct3209.duels.game.kits.KitManager;
import me.alphatct3209.duels.game.modes.DuelSelection;
import me.alphatct3209.duels.game.modes.ModeKey;
import me.alphatct3209.duels.game.modes.ModeQueueClaim;
import me.alphatct3209.duels.stats.db.StatisticsDatabase;
import me.alphatct3209.duels.utils.PlayerRestoration;
import me.alphatct3209.duels.world.WorldRegenerationCoordinator;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.data.BlockData;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

public class Arena
{
    private final Duels plugin;
    private final int id;
    private final String name;
    private Location spawnOne;
    private Location spawnTwo;
    private Location lobby;
    private final String mapWorldName;
    private final List<UUID> players = new ArrayList<>();
    private final Map<UUID, Kit> admittedKits = new LinkedHashMap<>();
    private final Map<UUID, DuelSelection> admittedSelections = new LinkedHashMap<>();
    private final Set<ModeKey> allowedModeKeys;
    private final ModeQueueClaim modeClaim = new ModeQueueClaim();
    private final Map<Location, BlockData> originalBlocks = new LinkedHashMap<>();
    private final Map<String, Location> points = new LinkedHashMap<>();
    private final Set<UUID> transientEntities = new LinkedHashSet<>();
    private ArenaObjectiveSettings objectiveSettings;

    private GameState gameState = GameState.IDLE;
    private Game game;
    private Countdown countdown;
    private final int countdownSeconds;
    private ArenaSettings settings;

    public Arena(Duels plugin, int id, String name, Location spawnOne, Location spawnTwo,
                 Location lobby, int countdownSeconds)
    {
        this(plugin, id, name, spawnOne, spawnTwo, lobby, countdownSeconds,
                Set.of(), new ArenaSettings());
    }

    public Arena(Duels plugin, int id, String name, Location spawnOne, Location spawnTwo,
                 Location lobby, int countdownSeconds, Collection<ModeKey> allowedModeKeys,
                 ArenaSettings settings)
    {
        this(plugin, id, name, spawnOne, spawnTwo, lobby, countdownSeconds, allowedModeKeys,
                settings, Map.of(), new ArenaObjectiveSettings());
    }

    public Arena(Duels plugin, int id, String name, Location spawnOne, Location spawnTwo,
                 Location lobby, int countdownSeconds, Collection<ModeKey> allowedModeKeys,
                 ArenaSettings settings, Map<String, Location> points,
                 ArenaObjectiveSettings objectiveSettings)
    {
        this.plugin = Objects.requireNonNull(plugin, "plugin");
        this.id = id;
        this.name = Objects.requireNonNull(name, "Arena name cannot be null");
        this.spawnOne = requireBound(spawnOne, "Spawn-One");
        this.spawnTwo = requireBound(spawnTwo, "Spawn-Two");
        this.lobby = requireBound(lobby, "Lobby");
        String firstWorld = this.spawnOne.getWorld().getName();
        String secondWorld = this.spawnTwo.getWorld().getName();
        if (!sameWorld(firstWorld, secondWorld))
        {
            throw new IllegalArgumentException("Arena " + id + " ('" + name
                    + "') has gameplay spawns in different worlds: Spawn-One='" + firstWorld
                    + "', Spawn-Two='" + secondWorld + "'. Both must use one ASP map template.");
        }
        this.mapWorldName = firstWorld;
        this.countdownSeconds = countdownSeconds;
        this.allowedModeKeys = new LinkedHashSet<>(allowedModeKeys);
        this.settings = Objects.requireNonNull(settings, "settings");
        this.objectiveSettings = Objects.requireNonNull(objectiveSettings, "objectiveSettings");
        Objects.requireNonNull(points, "points").forEach((pointName, location) -> {
            String normalized = normalizePointName(pointName);
            Location bound = requireBound(location, "Points." + normalized);
            if (!sameWorld(bound.getWorld().getName(), mapWorldName))
                throw new IllegalArgumentException("Arena point '" + normalized + "' must use gameplay world " + mapWorldName);
            this.points.put(normalized, bound);
        });
        recreateRuntime();
    }

    public Arena(Duels plugin, ArenaConfig arenaConfig)
    {
        this(plugin, arenaConfig.getId(), arenaConfig.getName(), arenaConfig.getSpawnOne(),
                arenaConfig.getSpawnTwo(), arenaConfig.getLobby(), arenaConfig.getCountdownSeconds(),
                Set.of(), arenaConfig.getSettings());
    }

    private static Location requireBound(Location location, String description)
    {
        Location copy = Objects.requireNonNull(location, description + " location cannot be null").clone();
        if (copy.getWorld() == null)
        {
            throw new IllegalArgumentException(description + " location must reference a loaded world");
        }
        return copy;
    }

    private static boolean sameWorld(String first, String second)
    {
        return WorldRegenerationCoordinator.canonical(first)
                .equals(WorldRegenerationCoordinator.canonical(second));
    }

    /** Atomically claims a PLAYING duel's one terminal transition. */
    public boolean beginDuelCompletion()
    {
        if (gameState != GameState.PLAYING)
        {
            return false;
        }
        if (claimedResetPolicy() == me.alphatct3209.duels.game.modes.ResetPolicy.WORLD)
        {
            plugin.getSlimeWorldManager().lockRegeneration(mapWorldName);
        }
        gameState = GameState.REGENERATING;
        return true;
    }

    /** Restores participants and delegates immutable-template replacement after terminal scoring. */
    public void finishDuelCompletion()
    {
        if (gameState != GameState.REGENERATING)
        {
            throw new IllegalStateException("Arena " + id + " is not completing a duel");
        }
        if (claimedResetPolicy() == me.alphatct3209.duels.game.modes.ResetPolicy.CELL)
        {
            for (Map.Entry<Location, BlockData> entry : originalBlocks.entrySet())
            {
                Block block = entry.getKey().getBlock();
                block.setBlockData(entry.getValue(), false);

            }
            for (UUID entityId : List.copyOf(transientEntities))
            {
                org.bukkit.entity.Entity entity = Bukkit.getEntity(entityId);
                if (entity != null) entity.remove();
            }
            clearForWorldReplacement(false);
            recreateRuntime();
            gameState = GameState.IDLE;
            return;
        }
        clearForWorldReplacement(false);
        plugin.getSlimeWorldManager().requestRegeneration(mapWorldName);
    }

    private me.alphatct3209.duels.game.modes.ResetPolicy claimedResetPolicy()
    {
        ModeKey key = modeClaim.claimedMode().orElseThrow(
                () -> new IllegalStateException("Arena " + id + " has no captured mode"));
        return plugin.getModeManager().require(key).resetPolicy();
    }

    /** Makes a non-playing arena unavailable and clears all state before its shared map unloads. */
    public void prepareForRegeneration()
    {
        if (gameState == GameState.PLAYING)
        {
            throw new IllegalStateException("Cannot tear down active arena " + id + " before it finishes");
        }
        if (gameState != GameState.REGENERATING && !players.isEmpty())
        {
            sendConfiguredToPlayers("Messages.Player-Left-Cancelled");
        }
        gameState = GameState.REGENERATING;
        clearForWorldReplacement(false);
    }

    /** Cancels occupants of a lobby that is about to unload for another arena's map reload. */
    public void cancelQueueForLobbyReload()
    {
        if (gameState == GameState.PLAYING || players.isEmpty())
        {
            return;
        }
        sendConfiguredToPlayers("Messages.Player-Left-Cancelled");
        cancelCountdown();
        restorePlayers(false);
        players.clear();
        admittedKits.clear();
        admittedSelections.clear();
        modeClaim.clear();
        originalBlocks.clear();
        gameState = GameState.IDLE;
        recreateRuntime();
    }

    private void clearForWorldReplacement(boolean quitting)
    {
        cancelCountdown();
        if (game != null) game.shutdown();
        restorePlayers(quitting);
        players.clear();
        admittedKits.clear();
        admittedSelections.clear();
        modeClaim.clear();
        originalBlocks.clear();
        transientEntities.clear();
        countdown = null;
        game = null;
    }

    private void restorePlayers(boolean quitting)
    {
        for (UUID uuid : List.copyOf(players))
        {
            Player player = Bukkit.getPlayer(uuid);
            if (player != null)
            {
                PlayerRestoration.restorePlayer(player, quitting);
            }
        }
    }

    private void cancelCountdown()
    {
        if (countdown == null)
        {
            return;
        }
        try
        {
            countdown.cancel();
        }
        catch (IllegalStateException ignored)
        {
            // BukkitRunnable was never scheduled or already cancelled.
        }
    }

    private void recreateRuntime()
    {
        this.game = new Game(this);
        this.countdown = new Countdown(plugin, this, countdownSeconds);
    }

    /** Called only after the fresh Bukkit world has been verified and every location rebound. */
    public void releaseAfterRegeneration()
    {
        if (gameState != GameState.REGENERATING)
        {
            throw new IllegalStateException("Arena " + id + " was not held for regeneration");
        }
        if (!players.isEmpty() || !admittedKits.isEmpty() || !admittedSelections.isEmpty()
                || modeClaim.participants() != 0)
        {
            throw new IllegalStateException("Arena " + id + " still has admitted players");
        }
        originalBlocks.clear();
        recreateRuntime();
        gameState = GameState.IDLE;
    }

    /** Rebinds all owned locations using a canonical world-name match, never stale identity. */
    public int rebindWorld(String worldName, World freshWorld)
    {
        Objects.requireNonNull(freshWorld, "freshWorld");
        if (!sameWorld(worldName, freshWorld.getName()))
        {
            throw new IllegalArgumentException("Fresh Bukkit world '" + freshWorld.getName()
                    + "' does not match requested world '" + worldName + "'");
        }
        int rebound = 0;
        if (sameWorld(spawnOne.getWorld().getName(), worldName))
        {
            spawnOne = rebind(spawnOne, freshWorld);
            rebound++;
        }
        if (sameWorld(spawnTwo.getWorld().getName(), worldName))
        {
            spawnTwo = rebind(spawnTwo, freshWorld);
            rebound++;
        }
        if (sameWorld(lobby.getWorld().getName(), worldName))
        {
            lobby = rebind(lobby, freshWorld);
            rebound++;
        }
        for (Map.Entry<String, Location> entry : new ArrayList<>(points.entrySet()))
        {
            if (sameWorld(entry.getValue().getWorld().getName(), worldName))
            {
                points.put(entry.getKey(), rebind(entry.getValue(), freshWorld));
                rebound++;
            }
        }
        if (sameWorld(mapWorldName, worldName) && rebound < 2)
        {
            throw new IllegalStateException("Arena " + id + " did not rebind both gameplay spawns for '"
                    + worldName + "'");
        }
        return rebound;
    }

    private static Location rebind(Location old, World freshWorld)
    {
        Location rebound = old.clone();
        rebound.setWorld(freshWorld);
        return rebound;
    }

    public void shutdown()
    {
        gameState = GameState.REGENERATING;
        clearForWorldReplacement(false);
    }

    public void recordOriginal(Block block)
    {
        recordOriginal(block, block.getBlockData());
    }

    public void recordOriginal(Block block, BlockData original)
    {
        originalBlocks.putIfAbsent(block.getLocation().clone(), original.clone());
    }

    public boolean isRuntimePlacedBlock(Block block)
    {
        BlockData original = originalBlocks.get(block.getLocation());
        return original != null && original.getMaterial().isAir();
    }

    public void start()
    {
        if (gameState != GameState.IDLE || plugin.getSlimeWorldManager().isPending(mapWorldName))
        {
            throw new IllegalStateException("Arena " + id + " cannot start while its map is unavailable");
        }
        gameState = GameState.COUNTDOWN;
        countdown.start();
    }

    public void sendMessage(String message)
    {
        for (UUID uuid : List.copyOf(players))
        {
            Player player = Bukkit.getPlayer(uuid);
            if (player != null)
            {
                player.sendMessage(message);
            }
        }
    }

    private void sendConfiguredToPlayers(String path)
    {
        sendConfiguredToPlayers(path, Map.of());
    }

    public void sendConfiguredToPlayers(String path, Map<String, ?> replacements, String... fallback)
    {
        for (UUID uuid : List.copyOf(players))
        {
            Player player = Bukkit.getPlayer(uuid);
            if (player != null)
                me.alphatct3209.duels.utils.MessageService.send(
                        player, plugin.getConfig(), path, replacements, fallback);
        }
    }

    public void sendConfigured(Player player, String path, Map<String, ?> replacements,
                               String... fallback)
    {
        if (player != null && players.contains(player.getUniqueId()))
            me.alphatct3209.duels.utils.MessageService.send(
                    player, plugin.getConfig(), path, replacements, fallback);
    }

    public KitManager getKitManager() { return plugin.getKitManager(); }
    public me.alphatct3209.duels.game.modes.ModeManager getModeManager() { return plugin.getModeManager(); }
    public StatisticsDatabase getStatisticsDatabase() { return plugin.getStatisticsManager().getStatsDB(); }
    public DivisionManager getDivisionManager() { return plugin.getDivisionManager(); }
    public me.alphatct3209.duels.game.kits.PlayerKitLayoutManager getPlayerKitLayoutManager()
    { return plugin.getPlayerKitLayoutManager(); }
    public me.alphatct3209.duels.game.items.GoldenHead getGoldenHead()
    { return plugin.getGoldenHead(); }
    public void requestLeaderboardRefresh() { plugin.requestLeaderboardRefresh(); }
    public org.bukkit.scheduler.BukkitTask schedule(Runnable task, long ticks)
    {
        return Bukkit.getScheduler().runTaskLater(plugin, task, ticks);
    }
    public org.bukkit.scheduler.BukkitTask scheduleRepeating(Runnable task, long delayTicks, long periodTicks)
    {
        return Bukkit.getScheduler().runTaskTimer(plugin, task, delayTicks, periodTicks);
    }
    public String getConfiguredMessage(String path) { return plugin.getConfig().getString(path); }

    /** Called by ArenaManager after the shared compatibility and pending-world checks. */
    boolean addPlayer(Player player, DuelSelection selection, Kit effectiveKit)
    {
        Objects.requireNonNull(selection, "selection");
        if (!modeClaim.admit(selection.modeKey())) return false;

        // Lobby hotbar items are temporary UI. Restore the real inventory before capturing it.
        plugin.getDuelMenuManager().suspendOpeners(player);
        PlayerRestoration.savePlayer(player);
        if (!plugin.getSlimeWorldManager().teleportSafely(player, getLobby()))
        {
            PlayerRestoration.discardSavedPlayer(player);
            plugin.getDuelMenuManager().giveOpeners(player);
            modeClaim.leave();
            return false;
        }

        players.add(player.getUniqueId());
        admittedSelections.put(player.getUniqueId(), selection);
        admittedKits.put(player.getUniqueId(), Objects.requireNonNull(effectiveKit, "effectiveKit"));
        player.setGameMode(GameMode.ADVENTURE);
        player.getInventory().clear();
        player.getInventory().setArmorContents(new ItemStack[4]);
        player.setMaxHealth(20);
        player.setHealth(20);
        player.setFoodLevel(20);

        if (players.size() == 2) start();
        return true;
    }

    boolean addChallengePlayers(Player challenger, Player target, DuelSelection selection,
                                Kit challengerKit, Kit targetKit)
    {
        if (!isCompletelyEmpty())
            throw new IllegalStateException("Challenge admission requires a completely empty arena");
        if (!addPlayer(challenger, selection, challengerKit)) return false;
        if (addPlayer(target, selection, targetKit)) return true;

        players.remove(challenger.getUniqueId());
        admittedSelections.remove(challenger.getUniqueId());
        admittedKits.remove(challenger.getUniqueId());
        modeClaim.leave();
        PlayerRestoration.restorePlayer(challenger, false);
        return false;
    }

    public Kit getAdmittedKit(UUID player)
    {
        Kit kit = admittedKits.get(player);
        if (kit == null)
        {
            throw new IllegalStateException("No effective kit was captured for arena player " + player);
        }
        return kit;
    }

    /** Returns the immutable kit captured when this player was admitted, if still a member. */

    public DuelSelection getAdmittedSelection(UUID player)
    {
        DuelSelection selection = admittedSelections.get(player);
        if (selection == null)
        {
            throw new IllegalStateException("No duel selection was captured for arena player " + player);
        }
        return selection;
    }

    public Optional<DuelSelection> getCapturedSelection(UUID player)
    {
        return Optional.ofNullable(admittedSelections.get(player));
    }

    public Optional<ModeKey> getClaimedMode()
    {
        return modeClaim.claimedMode();
    }

    boolean canClaimMode(ModeKey mode)
    {
        return modeClaim.canAdmit(mode);
    }

    public Optional<Kit> getCapturedKit(UUID player)
    {
        return Optional.ofNullable(admittedKits.get(player));
    }

    /** Returns a live countdown value only while the arena is counting down. */
    public int getCountdownSecondsRemaining()
    {
        return gameState == GameState.COUNTDOWN && countdown != null
                ? countdown.getSecondsRemaining() : 0;
    }

    boolean canClaimSelection(DuelSelection selection)
    {
        if (!modeClaim.canAdmit(selection.modeKey())) return false;
        return admittedSelections.values().stream().findFirst()
                .map(existing -> existing.legacyPvp() == selection.legacyPvp())
                .orElse(true);
    }

    public boolean usesLegacyPvp()
    {
        return admittedSelections.values().stream().findFirst()
                .map(DuelSelection::legacyPvp).orElse(false);
    }

    public void removePlayer(Player player)
    {
        if (players.remove(player.getUniqueId()))
        {
            admittedKits.remove(player.getUniqueId());
            admittedSelections.remove(player.getUniqueId());
            modeClaim.leave();
        }
        if (gameState == GameState.COUNTDOWN)
        {
            cancelCountdown();
            sendConfiguredToPlayers("Messages.Player-Left-Cancelled");
            gameState = GameState.IDLE;
        }
        this.countdown = new Countdown(plugin, this, countdownSeconds);
    }

    public boolean containsWorld(World world)
    {
        return world != null && containsWorldName(world.getName());
    }

    public boolean containsWorldName(String worldName)
    {
        return sameWorld(spawnOne.getWorld().getName(), worldName)
                || sameWorld(spawnTwo.getWorld().getName(), worldName)
                || sameWorld(lobby.getWorld().getName(), worldName);
    }

    public boolean lobbyUsesWorld(String worldName)
    {
        return sameWorld(lobby.getWorld().getName(), worldName);
    }

    public boolean mapUsesWorld(String worldName)
    {
        return sameWorld(mapWorldName, worldName);
    }

    public List<UUID> getPlayers() { return List.copyOf(players); }
    public me.alphatct3209.duels.social.SocialManager getSocialManager() { return plugin.getSocialManager(); }

    public boolean isCompletelyEmpty()
    {
        return gameState == GameState.IDLE && players.isEmpty();
    }

    public Set<ModeKey> getAllowedModeKeys() { return Set.copyOf(allowedModeKeys); }

    void setAllowedModeKeys(Collection<ModeKey> keys)
    {
        allowedModeKeys.clear();
        allowedModeKeys.addAll(keys);
    }

    public Optional<Location> getPoint(String name)
    {
        Location location = points.get(normalizePointName(name));
        return location == null ? Optional.empty() : Optional.of(location.clone());
    }

    public Map<String, Location> getPoints()
    {
        Map<String, Location> copy = new LinkedHashMap<>();
        points.forEach((key, value) -> copy.put(key, value.clone()));
        return Map.copyOf(copy);
    }

    void setPoint(String name, Location location)
    {
        Location bound = requireBound(location, "point");
        if (!sameWorld(bound.getWorld().getName(), mapWorldName))
            throw new IllegalArgumentException("Arena points must be in gameplay world " + mapWorldName);
        points.put(normalizePointName(name), bound);
    }

    void removePoint(String name) { points.remove(normalizePointName(name)); }
    public ArenaObjectiveSettings getObjectiveSettings() { return objectiveSettings; }
    void setObjectiveSettings(ArenaObjectiveSettings settings) { objectiveSettings = Objects.requireNonNull(settings); }
    public double deathY()
    {
        return objectiveSettings.deathY().orElse(Math.min(spawnOne.getY(), spawnTwo.getY()) - 8D);
    }
    public boolean isParticipant(UUID uuid) { return players.contains(uuid); }
    public boolean areOpponents(UUID first, UUID second)
    {
        return players.size() == 2 && ArenaCombatAuthorization.opponents(first, second,
                players.get(0), players.get(1));
    }
    public void trackTransient(org.bukkit.entity.Entity entity)
    {
        if (entity != null) transientEntities.add(entity.getUniqueId());
    }

    public boolean readyFor(me.alphatct3209.duels.game.modes.DuelMode mode)
    {
        if (!ArenaModeReadiness.ready(mode.handlerType(), points.keySet()))
        {
            return false;
        }
        if (mode.handlerType() != me.alphatct3209.duels.game.modes.ModeHandlerType.SKY_WARS)
        {
            return true;
        }
        return points.entrySet().stream()
                .filter(entry -> entry.getKey().equals("chest_1") || entry.getKey().equals("chest_2")
                        || entry.getKey().equals("mid_chest")
                        || entry.getKey().matches("chest_[12]_[1-9][0-9]*")
                        || entry.getKey().matches("mid_chest_[1-9][0-9]*"))
                .allMatch(entry -> hasNearbyContainer(entry.getValue()));
    }

    private boolean hasNearbyContainer(Location marker)
    {
        for (int radius = 0; radius <= 2; radius++)
        {
            for (int x = -radius; x <= radius; x++)
            {
                for (int y = -radius; y <= radius; y++)
                {
                    for (int z = -radius; z <= radius; z++)
                    {
                        if (marker.getBlock().getRelative(x, y, z).getState()
                                instanceof org.bukkit.block.Container)
                        {
                            return true;
                        }
                    }
                }
            }
        }
        return false;
    }

    public boolean containsObjectiveRegion(Location location)
    {
        if (location == null || location.getWorld() == null
                || !sameWorld(location.getWorld().getName(), mapWorldName)) return false;
        Location cornerOne = points.get("region_1");
        Location cornerTwo = points.get("region_2");
        if (cornerOne != null && cornerTwo != null)
        {
            return location.getX() >= Math.min(cornerOne.getX(), cornerTwo.getX())
                    && location.getX() <= Math.max(cornerOne.getX(), cornerTwo.getX())
                    && location.getY() >= Math.min(cornerOne.getY(), cornerTwo.getY())
                    && location.getY() <= Math.max(cornerOne.getY(), cornerTwo.getY())
                    && location.getZ() >= Math.min(cornerOne.getZ(), cornerTwo.getZ())
                    && location.getZ() <= Math.max(cornerOne.getZ(), cornerTwo.getZ());
        }
        double centerX = (spawnOne.getX() + spawnTwo.getX()) / 2D;
        double centerY = (spawnOne.getY() + spawnTwo.getY()) / 2D;
        double centerZ = (spawnOne.getZ() + spawnTwo.getZ()) / 2D;
        double radius = objectiveSettings.cellRadius();
        return Math.abs(location.getX() - centerX) <= radius
                && Math.abs(location.getY() - centerY) <= radius
                && Math.abs(location.getZ() - centerZ) <= radius;
    }

    private static String normalizePointName(String name)
    {
        if (name == null || !name.toLowerCase(Locale.ROOT).matches("[a-z0-9_]+"))
            throw new IllegalArgumentException("Point names use lowercase letters, numbers, and '_'");
        return name.toLowerCase(Locale.ROOT);
    }


    public ArenaSettings getSettings() { return settings; }
    void setSettings(ArenaSettings settings) { this.settings = Objects.requireNonNull(settings, "settings"); }

    public boolean teleportSafely(Player player, Location destination)
    {
        return plugin.getSlimeWorldManager().teleportWithinActiveWorld(player, destination);
    }

    public Location getSpawnOne() { return spawnOne.clone(); }
    public Location getSpawnTwo() { return spawnTwo.clone(); }
    public Location getLobby() { return lobby.clone(); }
    public String getMapWorldName() { return mapWorldName; }
    public String getLobbyWorldName() { return lobby.getWorld().getName(); }
    public UUID getPlayerOne() { return players.get(0); }
    public UUID getPlayerTwo() { return players.get(1); }
    public int getId() { return id; }
    public String getName() { return name; }
    public Game getGame() { return game; }
    public GameState getGameState() { return gameState; }
    public void setGameState(GameState gameState) { this.gameState = Objects.requireNonNull(gameState, "gameState"); }
}
