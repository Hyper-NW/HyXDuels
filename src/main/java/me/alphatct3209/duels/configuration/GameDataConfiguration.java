package me.alphatct3209.duels.configuration;

import me.alphatct3209.duels.Duels;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.Map;
import java.util.Objects;

/** Keeps mutable arenas/kits out of config.yml while exposing a compatibility view to existing code. */
public final class GameDataConfiguration
{
    private final Duels plugin;
    private final File arenasFile;
    private final File kitsFile;

    public GameDataConfiguration(Duels plugin)
    {
        this.plugin = Objects.requireNonNull(plugin, "plugin");
        arenasFile = load("arenas.yml", "Arenas");
        kitsFile = load("kits.yml", "Kits");
    }

    public void saveArenas() { save("Arenas", arenasFile); }
    public void saveKits() { save("Kits", kitsFile); }

    private File load(String fileName, String sectionName)
    {
        File expected = new File(new File(plugin.getDataFolder(), PluginFiles.DATA_DIRECTORY), fileName);
        boolean newLayoutMissing = !expected.isFile();
        File file = PluginFiles.data(plugin, fileName, true);

        ConfigurationSection legacy = plugin.getConfig().getConfigurationSection(sectionName);
        if (newLayoutMissing && legacy != null)
        {
            YamlConfiguration migrated = new YamlConfiguration();
            copySection(legacy, migrated.createSection(sectionName));
            saveYaml(migrated, file, sectionName);
            plugin.getLogger().info("Migrated config.yml " + sectionName + " into data/"
                    + fileName + "; config.yml was retained as a backup.");
        }

        YamlConfiguration yaml = YamlConfiguration.loadConfiguration(file);
        ConfigurationSection source = yaml.getConfigurationSection(sectionName);
        plugin.getConfig().set(sectionName, null);
        ConfigurationSection target = plugin.getConfig().createSection(sectionName);
        if (source != null) copySection(source, target);
        return file;
    }

    private void save(String sectionName, File file)
    {
        YamlConfiguration yaml = new YamlConfiguration();
        ConfigurationSection target = yaml.createSection(sectionName);
        ConfigurationSection source = plugin.getConfig().getConfigurationSection(sectionName);
        if (source != null) copySection(source, target);
        saveYaml(yaml, file, sectionName);
    }

    private void saveYaml(YamlConfiguration yaml, File file, String sectionName)
    {
        try
        {
            yaml.save(file);
        }
        catch (IOException exception)
        {
            throw new IllegalStateException("Could not save " + sectionName + " data to "
                    + file.getAbsolutePath(), exception);
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
