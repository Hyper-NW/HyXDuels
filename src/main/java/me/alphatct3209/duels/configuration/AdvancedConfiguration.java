package me.alphatct3209.duels.configuration;

import me.alphatct3209.duels.Duels;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.Objects;

/** Loads focused advanced files into the legacy read-only config view used throughout the plugin. */
public final class AdvancedConfiguration
{
    private final Duels plugin;

    public AdvancedConfiguration(Duels plugin)
    {
        this.plugin = Objects.requireNonNull(plugin, "plugin");
        load("display.yml", "Leaderboard-Cache", "Display");
        load("social.yml", "Party", "Challenges");
        load("messages.yml", "Messages");
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
        YamlConfiguration configured = YamlConfiguration.loadConfiguration(file);
        mergeBundledDefaults(configured, fileName);
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

    private void mergeBundledDefaults(YamlConfiguration configured, String fileName)
    {
        String resource = PluginFiles.advancedResource(fileName);
        try (InputStream stream = plugin.getResource(resource))
        {
            if (stream == null) throw new IllegalStateException("Missing bundled resource " + resource);
            YamlConfiguration bundled = YamlConfiguration.loadConfiguration(
                    new InputStreamReader(stream, StandardCharsets.UTF_8));
            for (Map.Entry<String, Object> entry : bundled.getValues(true).entrySet())
                if (!(entry.getValue() instanceof ConfigurationSection)
                        && !configured.contains(entry.getKey()))
                    configured.set(entry.getKey(), entry.getValue());
        }
        catch (IOException exception)
        {
            throw new IllegalStateException("Could not read bundled resource " + resource, exception);
        }
    }

    private void save(YamlConfiguration yaml, File file)
    {
        try { yaml.save(file); }
        catch (IOException exception)
        { throw new IllegalStateException("Could not save " + file.getAbsolutePath(), exception); }
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
