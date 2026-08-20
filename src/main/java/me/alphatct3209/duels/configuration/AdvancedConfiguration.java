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
        boolean changed = migrate(fileName, configured);
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
        if (!"display.yml".equals(fileName)
                || configured.getInt("Config-Version", 0) >= 2
                || !configured.getStringList(LOBBY_LINES).equals(LEGACY_REVERSED_LOBBY))
        {
            return false;
        }
        configured.set(LOBBY_LINES, NATURAL_LOBBY);
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
