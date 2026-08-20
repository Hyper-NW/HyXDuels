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
        if (configured.getInt("Config-Version", 0) >= 2) return false;
        if ("display.yml".equals(fileName)
                && configured.getStringList(LOBBY_LINES).equals(LEGACY_REVERSED_LOBBY))
        {
            configured.set(LOBBY_LINES, NATURAL_LOBBY);
            return true;
        }
        if ("messages.yml".equals(fileName) && bundled != null
                && configured.getStringList(HELP_MENU).equals(LEGACY_HELP))
        {
            configured.set(HELP_MENU, bundled.getStringList(HELP_MENU));
            return true;
        }
        return false;
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
        Map<String, List<String>> retired = Map.of();
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
