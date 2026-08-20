package me.alphatct3209.duels.configuration;

import me.alphatct3209.duels.Duels;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/** Loads focused advanced files into the legacy read-only config view used throughout the plugin. */
public final class AdvancedConfiguration
{
    private static final String LOBBY_LINES = "Display.Scoreboards.Lobby.Lines";
    private static final String HELP_MENU = "Messages.Help-Menu";
    private static final List<String> LEGACY_REVERSED_LOBBY = List.of(
            "&ewww.hyxduels.net", "",
            "&fOverall Kills: &a%duels_your_overall_kills%",
            "&fOverall Wins: &a%duels_your_overall_wins%", "",
            "&fBest Winstreak: &a%duels_your_highest_winstreak%",
            "&fOverall Winstreak: &a%duels_your_winstreak%", "",
            "&fTokens: &a0", "", "&e/duel <player>",
            "&bDuel other players with:", "", "&7<date>");
    private static final List<String> NATURAL_LOBBY = List.of(
            "&7<date>", "", "&bDuel other players with:",
            "&e/duel <player>", "", "&fTokens: &a0", "",
            "&fOverall Winstreak: &a%duels_your_winstreak%",
            "&fBest Winstreak: &a%duels_your_highest_winstreak%", "",
            "&fOverall Wins: &a%duels_your_overall_wins%",
            "&fOverall Kills: &a%duels_your_overall_kills%", "",
            "&ewww.hyxduels.net");
    private static final List<String> LEGACY_HELP = List.of(
            "&9Duels Help Menu&f:",
            "&6/duels help&f: &eDisplay this help menu",
            "&6/duels menu&f: &eOpen the mode and kit queue GUI",
            "&6/duels join [id]&f: &eJoin an available arena directly (administrative/legacy flow)",
            "&6/duel <player> &7or &6/duel challenge <player>&f: &eUse the same mode/kit menu to send a request",
            "&6/duel accept &7or &6/duel deny&f: &eAnswer your incoming challenge",
            "&6/duels listarenas&f: &eList all arenas",
            "&6/duels createarena <name> [block-break] [block-place]&f: &eBegin an arena with its protection rules",
            "&6/duels setlobby &7or &6setspawn1 &7or &6setspawn2&f: &eSet the respective location of the arena you are currently making",
            "&6/duels finisharena&f: &eSave and finish the arena you are currently making",
            "&6/duels kits list&f: &eList all kits",
            "&6/duels kits <create/delete/select> <kit name>&f: &eManage reusable kits; select changes the kit within your selected mode",
            "&6/duels modes list &7or &6/duels modes select <mode> [kit]&f: &eList or select a first-class mode and allowed kit",
            "&6/duels arenamodes <id> list|add|remove|clear [mode]&f: &eConfigure canonical mode routes for an arena (&7arenakits&e is deprecated)",
            "&6/duels arenasettings <id> [list|<flag> <true|false>]&f: &eView or update administrator arena rules",
            "&6/p invite|kick|promote|demote|transfer <player>&f: &eManage party members and roles",
            "&6/p accept|deny|leave|disband|list|menu&f: &eAnswer invites or use the leader party GUI",
            "&6/friend add|accept|deny|remove|best|list [player]&f: &eManage persistent friends",
            "&6/msg <player> <message>&f: &eSend a privacy-aware direct message",
            "&6/settings&f: &eCustomize displays, effects, and social privacy",
            "&6/kiteditor <kit>&f: &eSafely edit a kit layout (administrator)",
            "&6/duels stats [player] [gamemode]&f: &eView aggregate stats and gamemode division progress",
            "&6/duels top [wins|kills|divisions <gamemode>]&f: &eView aggregate or gamemode division top 10 players",
            "&6/duel hologram status|list|reload&f: &eInspect or reconcile managed holograms",
            "&6/duel hologram create <id> <wins|kills|divisions> [gamemode]&f: &eCreate at your stable-world location",
            "&6/duel hologram move|delete <id>&f: &eMove to your location or delete a managed definition");
    private static final List<String> VERSION_1_2_HELP = List.of(
            "&6/duel <player>&f: &eOpen a duel request for an online player",
            "&6/duel challenge <player>&f: &eOpen the mode and kit request flow",
            "&6/duel accept|deny&f: &eAnswer an incoming challenge",
            "&6/duels menu&f: &eOpen the duel queue GUI",
            "&6/duels join [id] &7or &6/duels leave&f: &eJoin or leave duel gameplay",
            "&6/duels kits help&f: &eShow structured kit management commands",
            "&6/duels modes list|select <mode> [kit]&f: &eList or select modes",
            "&6/duels arena help&f: &eShow structured arena administration commands",
            "&6/duels stats [player] [mode]&f: &eView aggregate and mode statistics",
            "&6/duels top [wins|kills|divisions <mode>]&f: &eView leaderboards",
            "&6/p invite|kick|promote|demote|transfer <player>&f: &eManage party members and roles",
            "&6/p accept|deny|leave|disband|list|menu&f: &eAnswer invites or use the leader party GUI",
            "&6/friend add|accept|deny|remove|best|list [player]&f: &eManage persistent friends",
            "&6/msg <player> <message>&f: &eSend a privacy-aware direct message",
            "&6/settings&f: &eCustomize displays, effects, and social privacy",
            "&6/kiteditor <kit>&f: &eSafely edit a kit layout (administrator)",
            "&6/duels hologram status|list|create|move|delete|reload&f: &eManage leaderboard holograms");
    private static final List<String> VERSION_1_3_HELP = List.of(
            "&6/duels help [page]&f: &eShow this command index without flooding chat",
            "&6/duel <player>&f: &eOpen a duel request for an online player",
            "&6/duels queue open|join|leave&f: &eUse matchmaking and queue controls",
            "&6/duels challenge send|accept|deny&f: &eManage duel challenges",
            "&6/duels kits help&f: &eShow structured kit management commands",
            "&6/duels modes list|select <mode> [kit]&f: &eList or select modes",
            "&6/duels arena help&f: &eShow structured arena administration commands",
            "&6/duels stats view|leaderboard ...&f: &eView statistics and rankings",
            "&6/duels party <subcommand>&f: &eManage parties and invitations",
            "&6/duels social friends <subcommand>&f: &eManage friends and best friends",
            "&6/duels social message <player> <message>&f: &eSend a direct message",
            "&6/duels social settings&f: &eCustomize privacy and display preferences",
            "&6/duels hologram status|list|create|move|delete|reload&f: &eManage leaderboard holograms",
            "&6/duels admin update|load-old-stats|file-to-sql&f: &eRun maintenance operations");
    private static final Map<String, Object> VERSION_1_2_MESSAGE_DEFAULTS = Map.ofEntries(
            Map.entry("Messages.Party-Usage", List.of(
                    "&e/p invite|kick|promote|demote|transfer <player>",
                    "&e/p accept|deny|leave|disband|list|menu")),
            Map.entry("Messages.Party-Invite-Received", List.of(
                    "&e<player> &ainvited you to &e<leader>'s &aparty.",
                    "&7Use &e/p accept &7or &e/p deny&7.")),
            Map.entry("Messages.Challenge-Received", List.of(
                    "&e<player> &ahas challenged you!",
                    "&7Mode: &f<mode> &8| &7Kit: &f<kit>",
                    "&7Combat: &f<combat> &8| &7Map: &f<arena>",
                    "&7Use &e/duel accept &7or &e/duel deny&7.")),
            Map.entry("Messages.Message-Usage", List.of("&cUsage: /msg <player> <message>")),
            Map.entry("Messages.Friend-Usage", List.of(
                    "&e/friend add|accept|deny|remove|best <player>", "&e/friend list")),
            Map.entry("Messages.Friend-Request-Received", List.of(
                    "&e<player> &asent you a friend request.",
                    "&7Use &e/friend accept <player> &7or &e/friend deny <player>&7.")),
            Map.entry("Messages.Kit-Editor-Usage", List.of("&cUsage: /kiteditor <kit>")));

    private final Duels plugin;

    public AdvancedConfiguration(Duels plugin)
    {
        this.plugin = Objects.requireNonNull(plugin, "plugin");
        load("display.yml", "Leaderboard-Cache", "Display");
        load("social.yml", "Party", "Challenges");
        load("messages.yml", "Messages");
        synchronize("modes.yml");
        synchronize("menus.yml");
        synchronize("divisions.yml");
        synchronize("holograms.yml");
        synchronize("golden-heads.yml");
    }

    private void load(String fileName, String... sections)
    {
        File expected = new File(new File(plugin.getDataFolder(), PluginFiles.ADVANCED_DIRECTORY), fileName);
        if (!expected.isFile() && containsAnyLegacySection(sections))
        {
            File parent = expected.getParentFile();
            if (!parent.isDirectory() && !parent.mkdirs() && !parent.isDirectory())
                throw new IllegalStateException("Could not create " + parent.getAbsolutePath());
            YamlConfiguration migrated = new YamlConfiguration();
            for (String sectionName : sections)
            {
                ConfigurationSection source = plugin.getConfig().getConfigurationSection(sectionName);
                if (source != null) copySection(source, migrated.createSection(sectionName));
            }
            save(migrated, expected);
            plugin.getLogger().info("Migrated config.yml sections " + String.join(", ", sections)
                    + " into advanced/" + fileName + "; config.yml was retained as a backup.");
        }

        File file = PluginFiles.advanced(plugin, fileName);
        synchronize(fileName, file);
        YamlConfiguration configured = loadYaml(file);
        for (String sectionName : sections)
        {
            ConfigurationSection source = configured.getConfigurationSection(sectionName);
            plugin.getConfig().set(sectionName, null);
            if (source != null)
                copySection(source, plugin.getConfig().createSection(sectionName));
        }
    }

    private boolean containsAnyLegacySection(String[] sections)
    {
        for (String section : sections)
            if (plugin.getConfig().isConfigurationSection(section)) return true;
        return false;
    }

    private void synchronize(String fileName)
    {
        synchronize(fileName, PluginFiles.advanced(plugin, fileName));
    }

    private void synchronize(String fileName, File file)
    {
        YamlConfiguration configured = loadYaml(file);
        YamlConfiguration bundled = loadBundled(fileName);
        boolean changed = migrate(fileName, configured, bundled);
        changed |= mergeMissing(configured, bundled);
        changed |= updateSchemaVersion(configured, bundled);
        changed |= removeRetiredPaths(fileName, configured);
        if (changed)
        {
            save(configured, file);
            plugin.getLogger().info("Updated advanced/" + fileName
                    + " with the current non-destructive configuration schema.");
        }
    }

    private YamlConfiguration loadBundled(String fileName)
    {
        String resource = PluginFiles.advancedResource(fileName);
        try (InputStream stream = plugin.getResource(resource))
        {
            if (stream == null) throw new IllegalStateException("Missing bundled resource " + resource);
            YamlConfiguration bundled = new YamlConfiguration();
            bundled.options().parseComments(true);
            bundled.load(new InputStreamReader(stream, StandardCharsets.UTF_8));
            return bundled;
        }
        catch (Exception exception)
        {
            throw new IllegalStateException("Could not read bundled resource " + resource, exception);
        }
    }

    static boolean mergeMissing(YamlConfiguration configured, YamlConfiguration bundled)
    {
        boolean changed = false;
        for (Map.Entry<String, Object> entry : bundled.getValues(true).entrySet())
        {
            if (!(entry.getValue() instanceof ConfigurationSection)
                    && !configured.contains(entry.getKey()))
            {
                configured.set(entry.getKey(), entry.getValue());
                changed = true;
            }
        }
        return changed;
    }

    static boolean migrate(String fileName, YamlConfiguration configured)
    {
        return migrate(fileName, configured, null);
    }

    static boolean migrate(String fileName, YamlConfiguration configured, YamlConfiguration bundled)
    {
        int version = configured.getInt("Config-Version", 0);
        boolean changed = false;
        if ("display.yml".equals(fileName)
                && version < 2
                && configured.getStringList(LOBBY_LINES).equals(LEGACY_REVERSED_LOBBY))
        {
            configured.set(LOBBY_LINES, NATURAL_LOBBY);
            changed = true;
        }
        if ("messages.yml".equals(fileName) && bundled != null && version < 3)
        {
            List<String> currentHelp = configured.getStringList(HELP_MENU);
            if (currentHelp.equals(LEGACY_HELP) || currentHelp.equals(VERSION_1_2_HELP))
            {
                configured.set(HELP_MENU, bundled.getStringList(HELP_MENU));
                changed = true;
            }
            for (Map.Entry<String, Object> entry : VERSION_1_2_MESSAGE_DEFAULTS.entrySet())
            {
                if (Objects.equals(configured.get(entry.getKey()), entry.getValue()))
                {
                    configured.set(entry.getKey(), bundled.get(entry.getKey()));
                    changed = true;
                }
            }
        }
        if ("messages.yml".equals(fileName) && bundled != null && version < 4
                && configured.getStringList(HELP_MENU).equals(VERSION_1_3_HELP))
        {
            configured.set(HELP_MENU, bundled.getStringList(HELP_MENU));
            changed = true;
        }
        if ("menus.yml".equals(fileName) && bundled != null && version < 2)
        {
            changed |= replaceIfExact(configured, bundled, "Openers.kit-editor.Lore", List.of(
                    "&7Edit the hotbar layout for any duel kit.", "&eRight-click to open."));
            changed |= replaceIfExact(configured, bundled, "Menus.Kit-Editor-Selector.Title",
                    "&8Kit Editor &7(<page>/<pages>)");
            changed |= replaceIfExact(configured, bundled, "Menus.Kit-Editor-Selector.Item-Lore",
                    List.of("&7Kit key: &f<kit_key>", "&eClick to edit this layout."));
        }
        return changed;
    }

    private static boolean replaceIfExact(YamlConfiguration configured, YamlConfiguration bundled,
                                          String path, Object formerDefault)
    {
        if (!Objects.equals(configured.get(path), formerDefault)) return false;
        configured.set(path, bundled.get(path));
        return true;
    }

    static boolean updateSchemaVersion(YamlConfiguration configured, YamlConfiguration bundled)
    {
        int bundledVersion = bundled.getInt("Config-Version", 0);
        if (bundledVersion <= 0 || configured.getInt("Config-Version", 0) >= bundledVersion)
            return false;
        configured.set("Config-Version", bundledVersion);
        return true;
    }

    /** Future migrations list only explicitly retired plugin paths here; unknown admin keys survive. */
    private static boolean removeRetiredPaths(String fileName, YamlConfiguration configured)
    {
        Map<String, List<String>> retired = Map.of("menus.yml", List.of("Version"));
        boolean changed = false;
        for (String path : retired.getOrDefault(fileName, List.of()))
        {
            if (configured.contains(path))
            {
                configured.set(path, null);
                changed = true;
            }
        }
        return changed;
    }

    private YamlConfiguration loadYaml(File file)
    {
        YamlConfiguration yaml = new YamlConfiguration();
        yaml.options().parseComments(true);
        try
        {
            yaml.load(file);
            return yaml;
        }
        catch (Exception exception)
        {
            throw new IllegalStateException("Could not read " + file.getAbsolutePath(), exception);
        }
    }

    private void save(YamlConfiguration yaml, File file)
    {
        File temporary = new File(file.getParentFile(), file.getName() + ".tmp");
        try
        {
            yaml.save(temporary);
            try
            {
                Files.move(temporary.toPath(), file.toPath(), StandardCopyOption.REPLACE_EXISTING,
                        StandardCopyOption.ATOMIC_MOVE);
            }
            catch (AtomicMoveNotSupportedException exception)
            {
                Files.move(temporary.toPath(), file.toPath(), StandardCopyOption.REPLACE_EXISTING);
            }
        }
        catch (IOException exception)
        {
            temporary.delete();
            throw new IllegalStateException("Could not save " + file.getAbsolutePath(), exception);
        }
    }

    private static void copySection(ConfigurationSection source, ConfigurationSection target)
    {
        for (Map.Entry<String, Object> entry : source.getValues(false).entrySet())
        {
            if (entry.getValue() instanceof ConfigurationSection nested)
                copySection(nested, target.createSection(entry.getKey()));
            else
                target.set(entry.getKey(), entry.getValue());
        }
    }
}
