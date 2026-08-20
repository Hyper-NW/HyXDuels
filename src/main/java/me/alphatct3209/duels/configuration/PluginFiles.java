package me.alphatct3209.duels.configuration;

import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.Objects;

/** Canonical plugin file layout plus non-destructive migration from the former flat layout. */
public final class PluginFiles
{
    public static final String ADVANCED_DIRECTORY = "advanced";
    public static final String DATA_DIRECTORY = "data";

    private PluginFiles() { }

    public static File advanced(JavaPlugin plugin, String fileName)
    {
        return provision(plugin, ADVANCED_DIRECTORY, fileName, true);
    }

    public static File data(JavaPlugin plugin, String fileName, boolean bundled)
    {
        return provision(plugin, DATA_DIRECTORY, fileName, bundled);
    }

    public static String advancedResource(String fileName)
    {
        return ADVANCED_DIRECTORY + "/" + validateFileName(fileName);
    }

    public static String dataResource(String fileName)
    {
        return DATA_DIRECTORY + "/" + validateFileName(fileName);
    }

    private static File provision(JavaPlugin plugin, String directory, String fileName, boolean bundled)
    {
        Objects.requireNonNull(plugin, "plugin");
        validateFileName(fileName);
        File folder = new File(plugin.getDataFolder(), directory);
        File destination = new File(folder, fileName);
        if (destination.isFile()) return destination;
        if (!folder.isDirectory() && !folder.mkdirs() && !folder.isDirectory())
            throw new IllegalStateException("Could not create plugin directory " + folder.getAbsolutePath());

        File legacy = new File(plugin.getDataFolder(), fileName);
        if (legacy.isFile())
        {
            try
            {
                Files.copy(legacy.toPath(), destination.toPath(), StandardCopyOption.COPY_ATTRIBUTES);
                plugin.getLogger().info("Migrated " + fileName + " to " + directory
                        + "/" + fileName + "; the legacy file was retained as a backup.");
                return destination;
            }
            catch (IOException exception)
            {
                throw new IllegalStateException("Could not migrate " + legacy.getAbsolutePath()
                        + " to " + destination.getAbsolutePath(), exception);
            }
        }

        if (bundled)
        {
            plugin.saveResource(directory + "/" + fileName, false);
            if (!destination.isFile())
                throw new IllegalStateException("Bundled resource was not created: " + destination);
        }
        return destination;
    }

    private static String validateFileName(String fileName)
    {
        if (fileName == null || !fileName.matches("[a-z0-9_-]+\\.yml"))
            throw new IllegalArgumentException("Plugin configuration names must be safe lowercase .yml files");
        return fileName;
    }
}
