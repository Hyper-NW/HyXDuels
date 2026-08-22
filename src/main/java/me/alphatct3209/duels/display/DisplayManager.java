package me.alphatct3209.duels.display;

import me.clip.placeholderapi.PlaceholderAPI;
import me.alphatct3209.duels.Duels;
import me.alphatct3209.duels.game.arenas.Arena;
import me.alphatct3209.duels.game.modes.DuelMode;
import me.alphatct3209.duels.game.kits.Kit;
import me.alphatct3209.duels.stats.leaderboard.LeaderboardSnapshot;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.scoreboard.Scoreboard;
import org.bukkit.scoreboard.ScoreboardManager;

import java.util.HashSet;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

/** Owns all HyXDuels sidebar, synthetic tab, and leaderboard-cache display state. */
public final class DisplayManager implements Listener
{
    private final Duels plugin;
    private final long refreshTicks;
    private final SidebarConfig lobbySidebar;
    private final SidebarConfig arenaSidebar;
    private final SidebarConfig countdownSidebar;
    private final SidebarConfig playingSidebar;
    private final TabConfig tab;
    private final FakePlayerTabList fakeTab;
    private final Map<String, LeaderboardDefinition> leaderboardDefinitions;
    private final Map<UUID, PlayerDisplay> players = new HashMap<>();
    private final boolean papi;
    private Map<String, List<LeaderboardEntry>> leaderboardCache = Map.of();
    private BukkitTask task;

    public DisplayManager(Duels plugin)
    {
        this.plugin = Objects.requireNonNull(plugin, "plugin");
        FileConfiguration config = plugin.getConfig();
        refreshTicks = Math.max(20L, config.getLong("Display.Refresh-Ticks", 20L));
        if (config.getLong("Display.Refresh-Ticks", 20L) < 20L)
        {
            plugin.getLogger().warning("Display.Refresh-Ticks is below 20; using 20 ticks.");
        }
        lobbySidebar = sidebar(config, "Display.Scoreboards.Lobby");
        arenaSidebar = sidebar(config, "Display.Scoreboards.Arena");
        countdownSidebar = optionalSidebar(config, "Display.Scoreboards.Arena-Countdown", arenaSidebar);
        playingSidebar = optionalSidebar(config, "Display.Scoreboards.Arena-Playing", arenaSidebar);
        tab = tab(config);
        fakeTab = tab.enabled() ? new FakePlayerTabList() : null;
        leaderboardDefinitions = leaderboardDefinitions(config);
        papi = Bukkit.getPluginManager().getPlugin("PlaceholderAPI") != null;
    }

    /** Registers listeners and starts the manager's only repeating task. */
    public void start()
    {
        if (task != null)
        {
            throw new IllegalStateException("DisplayManager is already started");
        }
        Bukkit.getPluginManager().registerEvents(this, plugin);
        task = Bukkit.getScheduler().runTaskTimer(plugin, this::refreshAll, 1L, refreshTicks);
    }

    /** Stops updates and restores sidebar, header/footer, real listings, and synthetic entries. */
    public void shutdown()
    {
        if (task != null)
        {
            task.cancel();
            task = null;
        }
        for (PlayerDisplay display : List.copyOf(players.values()))
        {
            display.restore();
        }
        players.clear();
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onJoin(PlayerJoinEvent event)
    {
        UUID uuid = event.getPlayer().getUniqueId();
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            Player player = Bukkit.getPlayer(uuid);
            if (player != null && player.isOnline())
            {
                refreshAll();
            }
        }, 1L);
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onQuit(PlayerQuitEvent event)
    {
        PlayerDisplay display = players.remove(event.getPlayer().getUniqueId());
        if (display != null)
        {
            display.restore();
        }
    }

    private void refreshAll()
    {
        leaderboardCache = displayLeaderboardCache(plugin.getLeaderboardService().snapshot());
        for (Player player : Bukkit.getOnlinePlayers())
        {
            initialize(player).refresh();
        }
    }

    private PlayerDisplay initialize(Player player)
    {
        return players.computeIfAbsent(player.getUniqueId(), ignored -> new PlayerDisplay(player));
    }

    private Map<String, List<LeaderboardEntry>> displayLeaderboardCache(LeaderboardSnapshot snapshot)
    {
        if (leaderboardDefinitions.isEmpty())
        {
            return Map.of();
        }
        Map<String, List<LeaderboardEntry>> projected = new LinkedHashMap<>();
        for (Map.Entry<String, LeaderboardDefinition> configured : leaderboardDefinitions.entrySet())
        {
            List<me.alphatct3209.duels.stats.leaderboard.LeaderboardEntry> rows =
                    switch (configured.getValue().type())
                    {
                        case WINS -> snapshot.overallWins();
                        case KILLS -> snapshot.overallKills();
                        case GAMEMODE_WINS -> {
                            String key = plugin.getModeManager().resolve(configured.getValue().gamemode())
                                    .map(value -> value.key().value()).orElse(configured.getValue().gamemode());
                            yield snapshot.modes().getOrDefault(key, List.of());
                        }
                    };
            projected.put(configured.getKey(), rows.stream()
                    .map(row -> new LeaderboardEntry(row.name(), row.value()))
                    .toList());
        }
        return Map.copyOf(projected);
    }

    private DisplayTokenContext context(Player player, Arena arena)
    {
        String world = player.getWorld() == null ? "-" : player.getWorld().getName();
        if (arena == null)
        {
            return new DisplayTokenContext(player.getName(), player.getUniqueId().toString(),
                    Bukkit.getOnlinePlayers().size(), world, "Lobby", "-", "LOBBY", "-", "-",
                    "-", "-", 0, 0, 0, 0L, "-", 0,
                    player.getHealth(), player.getMaxHealth(), 0D, 0D,
                    1, 1, 0, plugin.getDescription().getVersion());
        }

        String opponent = "-";
        double opponentHealth = 0D;
        double opponentMaxHealth = 0D;
        UUID opponentId = null;
        for (UUID participant : arena.getPlayers())
        {
            if (!participant.equals(player.getUniqueId()))
            {
                opponentId = participant;
                Player other = Bukkit.getPlayer(participant);
                opponent = other == null ? "-" : other.getName();
                opponentHealth = other == null ? 0D : other.getHealth();
                opponentMaxHealth = other == null ? 0D : other.getMaxHealth();
                break;
            }
        }
        String kit = arena.getCapturedKit(player.getUniqueId()).map(Kit::getName).orElse("-");
        DuelMode mode = arena.getCapturedSelection(player.getUniqueId())
                .map(selection -> plugin.getModeManager().require(selection.modeKey())).orElse(null);
        me.alphatct3209.duels.game.Game game = arena.getGame();
        int score = game.score(player.getUniqueId());
        int opponentScore = opponentId == null ? 0 : game.score(opponentId);
        return new DisplayTokenContext(player.getName(), player.getUniqueId().toString(),
                Bukkit.getOnlinePlayers().size(), world, arena.getName(), Integer.toString(arena.getId()),
                arena.getGameState().name(), opponent, kit,
                mode == null ? "-" : mode.displayName(), mode == null ? "-" : mode.key().value(),
                arena.getCountdownSecondsRemaining(), score, opponentScore, game.remainingSeconds(),
                game.getRuntimeState().isPresent() ? (game.bedAlive(player.getUniqueId()) ? "Alive" : "Broken") : "-",
                game.checkpoint(player.getUniqueId()), player.getHealth(), player.getMaxHealth(),
                opponentHealth, opponentMaxHealth, arena.getPlayers().size(), 2,
                0, plugin.getDescription().getVersion());
    }

    private String expand(Player player, String input, DisplayTokenContext context)
    {
        return ChatColor.translateAlternateColorCodes('&', expandRaw(player, input, context));
    }

    private String expandRaw(Player player, String input, DisplayTokenContext context)
    {
        String expanded = DisplayTokenEngine.expand(input, context, leaderboardCache);
        return papi ? PlaceholderAPI.setPlaceholders(player, expanded) : expanded;
    }

    private List<String> expand(Player player, List<String> lines, DisplayTokenContext context)
    {
        return lines.stream().map(line -> expand(player, line, context)).toList();
    }

    private final class PlayerDisplay
    {
        private final Player player;
        private final Scoreboard previousScoreboard;
        private final String previousHeader;
        private final String previousFooter;
        private final java.util.Set<UUID> unlistedPlayers = new HashSet<>();
        private SidebarDisplay sidebar;
        private boolean scoreboardLost;
        private boolean headerFooterOwned;
        private String appliedHeader;
        private String appliedFooter;

        private PlayerDisplay(Player player)
        {
            this.player = player;
            previousScoreboard = player.getScoreboard();
            previousHeader = player.getPlayerListHeader();
            previousFooter = player.getPlayerListFooter();
        }

        private void refresh()
        {
            Arena arena = plugin.getArenaManager().getArena(player);
            DisplayTokenContext values = context(player, arena);
            SidebarConfig configured = lobbySidebar;
            if (arena != null)
            {
                configured = switch (arena.getGameState())
                {
                    case COUNTDOWN -> countdownSidebar;
                    case PLAYING -> playingSidebar;
                    default -> arenaSidebar;
                };
            }
            refreshSidebar(configured, values);
            refreshTab(values);
        }

        private void refreshSidebar(SidebarConfig configured, DisplayTokenContext values)
        {
            if (!plugin.getSocialManager().preferences(player.getUniqueId()).scoreboard())
            {
                restoreScoreboard();
                return;
            }
            if (!configured.enabled())
            {
                restoreScoreboard();
                return;
            }
            String title = expand(player, configured.title(), values);
            List<String> lines = expand(player, configured.lines(), values);
            if (sidebar == null)
            {
                if (scoreboardLost || player.getScoreboard() != previousScoreboard)
                {
                    scoreboardLost = true;
                    return;
                }
                ScoreboardManager manager = Bukkit.getScoreboardManager();
                if (manager == null)
                {
                    return;
                }
                sidebar = new SidebarDisplay(manager, title);
                sidebar.update(title, lines);
                player.setScoreboard(sidebar.scoreboard());
                return;
            }
            if (!sidebar.isCurrent(player))
            {
                scoreboardLost = true;
                sidebar = null;
                return;
            }
            sidebar.update(title, lines);
        }

        private void refreshTab(DisplayTokenContext values)
        {
            if (!tab.enabled())
            {
                restoreTab();
                return;
            }
            List<List<String>> expandedColumns = tab.columns().stream()
                    .map(column -> expand(player, column, values)).toList();
            boolean hasColumns = expandedColumns.stream().anyMatch(column -> !column.isEmpty());
            if (hasColumns)
            {
                for (Player listed : Bukkit.getOnlinePlayers())
                {
                    if (player.isListed(listed) && player.unlistPlayer(listed))
                    {
                        unlistedPlayers.add(listed.getUniqueId());
                    }
                }
                fakeTab.show(player, TabGridLayout.cells(expandedColumns));
            }
            else
            {
                fakeTab.remove(player);
                restoreListedPlayers();
            }
            String header = String.join("\n", expand(player, tab.header(), values));
            String footer = String.join("\n", expand(player, tab.footer(), values));
            claimHeaderFooter(header, footer);
        }

        private void claimHeaderFooter(String header, String footer)
        {
            String currentHeader = player.getPlayerListHeader();
            String currentFooter = player.getPlayerListFooter();
            if ((!headerFooterOwned && Objects.equals(currentHeader, previousHeader)
                    && Objects.equals(currentFooter, previousFooter))
                    || (headerFooterOwned && Objects.equals(currentHeader, appliedHeader)
                    && Objects.equals(currentFooter, appliedFooter)))
            {
                player.setPlayerListHeaderFooter(header, footer);
                appliedHeader = header;
                appliedFooter = footer;
                headerFooterOwned = true;
            }
            else
            {
                headerFooterOwned = false;
            }
        }

        private void restore()
        {
            restoreScoreboard();
            restoreTab();
        }

        private void restoreScoreboard()
        {
            if (sidebar != null && sidebar.isCurrent(player))
            {
                player.setScoreboard(previousScoreboard);
            }
            sidebar = null;
        }

        private void restoreTab()
        {
            if (fakeTab != null)
            {
                fakeTab.remove(player);
            }
            restoreListedPlayers();
            if (headerFooterOwned
                    && Objects.equals(player.getPlayerListHeader(), appliedHeader)
                    && Objects.equals(player.getPlayerListFooter(), appliedFooter))
            {
                player.setPlayerListHeaderFooter(previousHeader, previousFooter);
            }
            headerFooterOwned = false;
        }

        private void restoreListedPlayers()
        {
            for (UUID uuid : List.copyOf(unlistedPlayers))
            {
                Player listed = Bukkit.getPlayer(uuid);
                if (listed != null && listed.isOnline() && player.canSee(listed)
                        && !player.isListed(listed))
                {
                    player.listPlayer(listed);
                }
            }
            unlistedPlayers.clear();
        }
    }

    private SidebarConfig sidebar(FileConfiguration config, String path)
    {
        List<String> lines = config.getStringList(path + ".Lines");
        if (lines.size() > 15)
        {
            plugin.getLogger().warning(path + ".Lines has more than 15 entries; only the first 15 are used.");
            lines = lines.subList(0, 15);
        }
        return new SidebarConfig(config.getBoolean(path + ".Enabled", true),
                config.getString(path + ".Title", "&6&lHyXDuels"), List.copyOf(lines));
    }

    private SidebarConfig optionalSidebar(FileConfiguration config, String path, SidebarConfig fallback)
    {
        return config.getConfigurationSection(path) == null ? fallback : sidebar(config, path);
    }

    private TabConfig tab(FileConfiguration config)
    {
        List<List<String>> columns = TabGridParser.parse(
                config.getStringList("Display.Tab.Columns.Entries"));
        TabGridLayout.cells(columns);
        return new TabConfig(config.getBoolean("Display.Tab.Enabled", true),
                List.copyOf(config.getStringList("Display.Tab.Header")),
                List.copyOf(config.getStringList("Display.Tab.Footer")), columns);
    }

    private Map<String, LeaderboardDefinition> leaderboardDefinitions(FileConfiguration config)
    {
        ConfigurationSection section = config.getConfigurationSection("Display.Leaderboards.Definitions");
        if (section == null)
        {
            return Map.of();
        }
        Map<String, LeaderboardDefinition> definitions = new LinkedHashMap<>();
        for (String rawKey : section.getKeys(false))
        {
            String key = rawKey.toLowerCase(Locale.ROOT);
            if (!key.matches("[a-z0-9_-]+"))
            {
                throw new IllegalArgumentException("Display leaderboard key '" + rawKey
                        + "' may contain only letters, numbers, '_' and '-'");
            }
            String path = section.getCurrentPath() + "." + rawKey;
            LeaderboardType type;
            try
            {
                type = LeaderboardType.valueOf(config.getString(path + ".Type", "WINS")
                        .toUpperCase(Locale.ROOT).replace('-', '_'));
            }
            catch (IllegalArgumentException exception)
            {
                throw new IllegalArgumentException(path + ".Type must be WINS, KILLS, or GAMEMODE_WINS");
            }
            String gamemode = config.getString(path + ".Gamemode", "");
            if (type == LeaderboardType.GAMEMODE_WINS && gamemode.isBlank())
            {
                throw new IllegalArgumentException(path + ".Gamemode is required for GAMEMODE_WINS");
            }
            definitions.put(key, new LeaderboardDefinition(type, gamemode));
        }
        return Map.copyOf(definitions);
    }

    private record SidebarConfig(boolean enabled, String title, List<String> lines) {}

    private record TabConfig(boolean enabled, List<String> header, List<String> footer,
                             List<List<String>> columns) {}

    private record LeaderboardDefinition(LeaderboardType type, String gamemode) {}

    private enum LeaderboardType { WINS, KILLS, GAMEMODE_WINS }
}
