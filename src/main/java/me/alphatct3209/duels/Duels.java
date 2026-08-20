package me.alphatct3209.duels;

import de.tr7zw.changeme.nbtapi.NBT;
import me.alphatct3209.duels.challenge.ChallengeManager;
import me.alphatct3209.duels.commands.DuelCmd;
import me.alphatct3209.duels.commands.DuelTabCompleter;
import me.alphatct3209.duels.configuration.GameDataConfiguration;
import me.alphatct3209.duels.configuration.AdvancedConfiguration;
import me.alphatct3209.duels.divisions.DivisionManager;
import me.alphatct3209.duels.display.DisplayManager;
import me.alphatct3209.duels.game.arenas.ArenaManager;
import me.alphatct3209.duels.game.modes.DuelSelectionService;
import me.alphatct3209.duels.game.modes.ModeManager;
import me.alphatct3209.duels.game.kits.KitManager;
import me.alphatct3209.duels.game.kits.KitEditorCommand;
import me.alphatct3209.duels.game.kits.KitLayoutEditor;
import me.alphatct3209.duels.gui.DuelMenuManager;
import me.alphatct3209.duels.hologram.HologramManager;
import me.alphatct3209.duels.listeners.ArenaGameplayListener;
import me.alphatct3209.duels.listeners.ArenaListener;
import me.alphatct3209.duels.listeners.BedWarsShopListener;
import me.alphatct3209.duels.listeners.GameListener;
import me.alphatct3209.duels.listeners.GoldenHeadListener;
import me.alphatct3209.duels.listeners.LegacyCombatListener;
import me.alphatct3209.duels.listeners.ModeMechanicsListener;
import me.alphatct3209.duels.listeners.PlayerListener;
import me.alphatct3209.duels.listeners.StatsListener;
import me.alphatct3209.duels.party.PartyManager;
import me.alphatct3209.duels.party.command.PartyCommand;
import me.alphatct3209.duels.party.command.PartyTabCompleter;
import me.alphatct3209.duels.party.gui.PartyGui;
import me.alphatct3209.duels.queue.DuelQueueManager;
import me.alphatct3209.duels.social.SocialManager;
import me.alphatct3209.duels.social.command.FriendCommand;
import me.alphatct3209.duels.social.command.MessageCommand;
import me.alphatct3209.duels.social.command.SettingsCommand;
import me.alphatct3209.duels.social.gui.SettingsGui;
import me.alphatct3209.duels.stats.StatisticsManager;
import me.alphatct3209.duels.stats.db.DatabaseType;
import me.alphatct3209.duels.stats.leaderboard.LeaderboardService;
import me.alphatct3209.duels.stats.filter.FilteredLeaderboardService;
import me.alphatct3209.duels.stats.filter.LeaderboardFilterGui;
import me.alphatct3209.duels.stats.filter.LeaderboardFilterManager;
import me.alphatct3209.duels.utils.KitChecker;
import me.alphatct3209.duels.utils.PapiHook;
import me.alphatct3209.duels.world.SlimeWorldManager;
import org.bukkit.Bukkit;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;

import java.time.Duration;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.Locale;
import java.util.logging.Level;

public class Duels extends JavaPlugin
{

    private ArenaManager arenaManager;
    private GameDataConfiguration gameDataConfiguration;
    private KitManager kitManager;
    private ModeManager modeManager;
    private DuelSelectionService selectionService;
    private StatisticsManager statisticsManager;
    private DivisionManager divisionManager;
    private SlimeWorldManager slimeWorldManager;
    private ChallengeManager challengeManager;
    private DuelQueueManager queueManager;
    private DuelMenuManager duelMenuManager;
    private PartyManager partyManager;
    private PartyGui partyGui;
    private SocialManager socialManager;
    private SettingsGui settingsGui;
    private KitLayoutEditor kitLayoutEditor;
    private DisplayManager displayManager;
    private HologramManager hologramManager;
    private LeaderboardService leaderboardService;
    private LeaderboardFilterManager leaderboardFilterManager;
    private FilteredLeaderboardService filteredLeaderboardService;
    private LeaderboardFilterGui leaderboardFilterGui;
    private BukkitTask leaderboardRefreshTask;
    private BukkitTask leaderboardDebounceTask;
    private long leaderboardDebounceTicks;

    @Override
    public void onEnable()
    {
        int pluginId = 12801;
        new Metrics(this, pluginId);

        if (!NBT.preloadApi()) {
            getLogger().warning("NBT-API wasn't initialized properly, disabling HyXDuels!");
            getServer().getPluginManager().disablePlugin(this);
            return;
        }

        getConfig().options().copyDefaults(true);
        saveConfig();
        try
        {
            new AdvancedConfiguration(this);
            this.gameDataConfiguration = new GameDataConfiguration(this);
            this.divisionManager = new DivisionManager(this);
        }
        catch (RuntimeException exception)
        {
            getLogger().log(Level.SEVERE,
                    "Could not load divisions.yml; disabling HyXDuels.", exception);
            getServer().getPluginManager().disablePlugin(this);
            return;
        }

        try
        {
            this.slimeWorldManager = new SlimeWorldManager(this);
            this.slimeWorldManager.loadConfiguredWorlds();
        }
        catch (Exception | LinkageError exception)
        {
            getLogger().log(Level.SEVERE,
                    "Could not load the configured AdvancedSlimePaper arena worlds; disabling HyXDuels.",
                    exception);
            getServer().getPluginManager().disablePlugin(this);
            return;
        }

        try
        {
            this.kitManager = new KitManager(this);
            this.modeManager = new ModeManager(this);
            this.selectionService = new DuelSelectionService(modeManager, kitManager);
            this.arenaManager = new ArenaManager(this);
            setupStatisticsManager();
            setupLeaderboardService();
            this.socialManager = new SocialManager(this);
            this.leaderboardFilterManager = new LeaderboardFilterManager(this);
            this.filteredLeaderboardService = new FilteredLeaderboardService(this, leaderboardFilterManager);
            this.leaderboardFilterGui = new LeaderboardFilterGui(
                    this, leaderboardFilterManager, filteredLeaderboardService);
            this.challengeManager = new ChallengeManager(this);
            this.queueManager = new DuelQueueManager(this);
            this.duelMenuManager = new DuelMenuManager(this);
            this.settingsGui = new SettingsGui(this);
            this.kitLayoutEditor = new KitLayoutEditor(this);
            this.partyManager = new PartyManager(this);
            this.partyGui = new PartyGui(this);
            this.displayManager = new DisplayManager(this);
            this.displayManager.start();
        }
        catch (RuntimeException exception)
        {
            getLogger().log(Level.SEVERE,
                    "Could not initialize arenas, kits, statistics, or displays; disabling HyXDuels.", exception);
            getServer().getPluginManager().disablePlugin(this);
            return;
        }

        if (Bukkit.getPluginManager().getPlugin("PlaceholderAPI") != null)
        {
            new PapiHook(this).register();
        }
        else
        {
            getLogger().warning("PlaceholderAPI not found; external statistics placeholders are disabled.");
        }

        this.hologramManager = new HologramManager(this);
        if (!this.hologramManager.reload())
        {
            getLogger().warning("Managed holograms are disabled; HyXDuels will continue normally.");
        }

        DuelCmd duelCommand = new DuelCmd(this);
        getCommand("duel").setExecutor(duelCommand);
        getCommand("duel").setTabCompleter(new DuelTabCompleter(this));
        PartyCommand partyCommand = new PartyCommand(this);
        getCommand("party").setExecutor(partyCommand);
        getCommand("party").setTabCompleter(
                new PartyTabCompleter(this));
        FriendCommand friendCommand = new FriendCommand(this);
        getCommand("friend").setExecutor(friendCommand);
        getCommand("friend").setTabCompleter(friendCommand);
        getCommand("message").setExecutor(new MessageCommand(this));
        getCommand("settings").setExecutor(new SettingsCommand(this));
        KitEditorCommand kitEditorCommand = new KitEditorCommand(this);
        getCommand("kiteditor").setExecutor(kitEditorCommand);
        getCommand("kiteditor").setTabCompleter(kitEditorCommand);

        getServer().getPluginManager().registerEvents(slimeWorldManager, this);
        getServer().getPluginManager().registerEvents(new ArenaListener(this), this);
        getServer().getPluginManager().registerEvents(new ArenaGameplayListener(this), this);
        getServer().getPluginManager().registerEvents(new BedWarsShopListener(this), this);
        getServer().getPluginManager().registerEvents(new GameListener(this), this);
        getServer().getPluginManager().registerEvents(new GoldenHeadListener(this), this);
        getServer().getPluginManager().registerEvents(new LegacyCombatListener(this), this);
        getServer().getPluginManager().registerEvents(new ModeMechanicsListener(this), this);
        getServer().getPluginManager().registerEvents(new StatsListener(this), this);
        getServer().getPluginManager().registerEvents(new PlayerListener(), this);
        getServer().getPluginManager().registerEvents(challengeManager, this);
        getServer().getPluginManager().registerEvents(challengeManager.getGui(), this);
        getServer().getPluginManager().registerEvents(queueManager, this);
        getServer().getPluginManager().registerEvents(duelMenuManager, this);
        getServer().getPluginManager().registerEvents(settingsGui, this);
        getServer().getPluginManager().registerEvents(kitLayoutEditor, this);
        getServer().getPluginManager().registerEvents(socialManager, this);
        getServer().getPluginManager().registerEvents(partyGui, this);
        getServer().getPluginManager().registerEvents(leaderboardFilterGui, this);
        Bukkit.getOnlinePlayers().forEach(duelMenuManager::giveOpeners);

        KitChecker.kitCheck(null);
    }

    @Override
    public void onDisable()
    {
        if (leaderboardRefreshTask != null)
        {
            leaderboardRefreshTask.cancel();
            leaderboardRefreshTask = null;
        }
        if (leaderboardDebounceTask != null)
        {
            leaderboardDebounceTask.cancel();
            leaderboardDebounceTask = null;
        }
        if (challengeManager != null)
        {
            challengeManager.shutdown();
        }
        if (queueManager != null)
        {
            queueManager.shutdown();
        }
        if (duelMenuManager != null)
        {
            duelMenuManager.shutdown();
        }
        if (settingsGui != null)
        {
            settingsGui.shutdown();
        }
        if (kitLayoutEditor != null)
        {
            kitLayoutEditor.shutdown();
        }
        if (socialManager != null)
        {
            socialManager.shutdown();
        }
        if (leaderboardFilterGui != null)
        {
            leaderboardFilterGui.shutdown();
        }
        if (leaderboardFilterManager != null)
        {
            leaderboardFilterManager.shutdown();
        }
        if (partyGui != null)
        {
            partyGui.shutdown();
        }
        if (partyManager != null)
        {
            partyManager.shutdown();
        }
        if (displayManager != null)
        {
            displayManager.shutdown();
        }
        if (hologramManager != null)
        {
            hologramManager.shutdown();
        }
        if (arenaManager != null)
        {
            arenaManager.shutdown();
        }
        if (slimeWorldManager != null)
        {
            slimeWorldManager.shutdown();
        }
    }

    private void setupLeaderboardService()
    {
        long configuredRefresh = getConfig().getLong("Leaderboard-Cache.Refresh-Ticks", 600L);
        long refreshTicks = Math.max(100L, Math.min(72_000L, configuredRefresh));
        if (refreshTicks != configuredRefresh)
        {
            getLogger().warning("Leaderboard-Cache.Refresh-Ticks must be between 100 and 72000; using "
                    + refreshTicks + ".");
        }
        long configuredDebounce = getConfig().getLong(
                "Leaderboard-Cache.Score-Refresh-Debounce-Ticks", 20L);
        leaderboardDebounceTicks = Math.max(1L, Math.min(200L, configuredDebounce));
        if (leaderboardDebounceTicks != configuredDebounce)
        {
            getLogger().warning("Leaderboard-Cache.Score-Refresh-Debounce-Ticks must be between 1 and 200; using "
                    + leaderboardDebounceTicks + ".");
        }
        long warningSeconds = Math.max(10L, Math.min(3600L,
                getConfig().getLong("Leaderboard-Cache.Warning-Interval-Seconds", 60L)));

        leaderboardService = new LeaderboardService(statisticsManager.getStatsDB(),
                this::leaderboardModeKeys,
                wins -> divisionManager.getCurrentDivision(wins)
                        .map(division -> division.displayName()).orElse("Unranked"),
                message -> getLogger().warning(message), System::currentTimeMillis,
                Duration.ofSeconds(warningSeconds));
        leaderboardService.refresh();
        leaderboardRefreshTask = Bukkit.getScheduler().runTaskTimer(
                this, leaderboardService::refresh, refreshTicks, refreshTicks);
    }

    /** Requests one delayed cache refresh; repeated score changes within the window are coalesced. */
    public void requestLeaderboardRefresh()
    {
        if (leaderboardService == null || !isEnabled() || leaderboardDebounceTask != null)
        {
            return;
        }
        leaderboardDebounceTask = Bukkit.getScheduler().runTaskLater(this, () -> {
            try
            {
                leaderboardService.refresh();
            }
            finally
            {
                leaderboardDebounceTask = null;
            }
        }, leaderboardDebounceTicks);
    }

    private Collection<String> leaderboardModeKeys()
    {
        return modeManager.leaderboardKeys();
    }

    private void setupStatisticsManager()
    {
        String storageType = getConfig().getString("Statistics.Storage-Type");
        if(storageType != null && (storageType.equalsIgnoreCase("sql") || storageType.equalsIgnoreCase("mysql")))
        {
            this.statisticsManager = new StatisticsManager(this, DatabaseType.SQL);
        }
        else if (storageType != null && (storageType.equalsIgnoreCase("yaml") || storageType.equalsIgnoreCase("yml")))
        {
            this.statisticsManager = new StatisticsManager(this, DatabaseType.YAML);
        }
        else
        {
            getLogger().warning("HyXDuels does not recognize Storage-Type '" + storageType
                    + "'. Falling back to YAML storage.");
            this.statisticsManager = new StatisticsManager(this, DatabaseType.YAML);
        }
    }

    public DuelQueueManager getQueueManager()
    {
        return queueManager;
    }

    public PartyManager getPartyManager()
    {
        return partyManager;
    }

    public PartyGui getPartyGui()
    {
        return partyGui;
    }

    public ArenaManager getArenaManager()
    {
        return arenaManager;
    }

    public KitManager getKitManager()
    {
        return kitManager;
    }

    public StatisticsManager getStatisticsManager()
    {
        return statisticsManager;
    }

    public DivisionManager getDivisionManager()
    {
        return divisionManager;
    }

    public ChallengeManager getChallengeManager()
    {
        return challengeManager;
    }

    public DuelMenuManager getDuelMenuManager()
    {
        return duelMenuManager;
    }

    public SocialManager getSocialManager()
    {
        return socialManager;
    }

    public SettingsGui getSettingsGui()
    {
        return settingsGui;
    }

    public KitLayoutEditor getKitLayoutEditor()
    {
        return kitLayoutEditor;
    }

    public void saveArenaData()
    {
        if (gameDataConfiguration == null) throw new IllegalStateException("Game data is not initialized");
        gameDataConfiguration.saveArenas();
    }

    public void saveKitData()
    {
        if (gameDataConfiguration == null) throw new IllegalStateException("Game data is not initialized");
        gameDataConfiguration.saveKits();
    }


    public ModeManager getModeManager()
    {
        return modeManager;
    }

    public DuelSelectionService getSelectionService()
    {
        return selectionService;
    }

    public LeaderboardService getLeaderboardService()
    {
        return leaderboardService;
    }

    public LeaderboardFilterGui getLeaderboardFilterGui()
    {
        return leaderboardFilterGui;
    }

    public LeaderboardFilterManager getLeaderboardFilterManager()
    {
        return leaderboardFilterManager;
    }

    public FilteredLeaderboardService getFilteredLeaderboardService()
    {
        return filteredLeaderboardService;
    }


    public HologramManager getHologramManager()
    {
        return hologramManager;
    }

    public SlimeWorldManager getSlimeWorldManager()
    {
        return slimeWorldManager;
    }
}
