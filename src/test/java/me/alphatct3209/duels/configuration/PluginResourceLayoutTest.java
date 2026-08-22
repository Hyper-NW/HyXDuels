package me.alphatct3209.duels.configuration;

import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class PluginResourceLayoutTest
{
    @Test
    void separatesCoreAdvancedAndRuntimeOwnedConfiguration()
    {
        File resources = new File("src/main/resources");
        assertTrue(new File(resources, "config.yml").isFile());
        for (String name : new String[]{"modes.yml", "menus.yml", "divisions.yml", "holograms.yml",
                "display.yml", "social.yml", "messages.yml", "golden-heads.yml"})
        {
            assertTrue(new File(resources, "advanced/" + name).isFile(), name);
            assertFalse(new File(resources, name).exists(), name);
        }
        for (String name : new String[]{"arenas.yml", "kits.yml", "statistics.yml"})
            assertTrue(new File(resources, "data/" + name).isFile(), name);

        YamlConfiguration core = YamlConfiguration.loadConfiguration(new File(resources, "config.yml"));
        assertFalse(core.contains("Arenas"));
        assertFalse(core.contains("Kits"));
        assertFalse(core.contains("Display"));
        assertFalse(core.contains("Messages"));
        assertNotNull(YamlConfiguration.loadConfiguration(
                new File(resources, "data/arenas.yml")).getConfigurationSection("Arenas"));
        assertNotNull(YamlConfiguration.loadConfiguration(
                new File(resources, "data/kits.yml")).getConfigurationSection("Kits"));
    }

    @Test
    void exposesSafeCanonicalRelativeResourcePaths()
    {
        assertEquals("advanced/modes.yml", PluginFiles.advancedResource("modes.yml"));
        assertEquals("data/statistics.yml", PluginFiles.dataResource("statistics.yml"));
        assertThrows(IllegalArgumentException.class,
                () -> PluginFiles.advancedResource("../config.yml"));
    }

    @Test
    void everyShippedYamlFileParsesAndLobbyLinesUseNaturalOrder() throws Exception
    {
        File resources = new File("src/main/resources");
        for (File file : List.of(
                new File(resources, "config.yml"),
                new File(resources, "plugin.yml"),
                new File(resources, "advanced/display.yml"),
                new File(resources, "advanced/divisions.yml"),
                new File(resources, "advanced/holograms.yml"),
                new File(resources, "advanced/golden-heads.yml"),
                new File(resources, "advanced/menus.yml"),
                new File(resources, "advanced/messages.yml"),
                new File(resources, "advanced/modes.yml"),
                new File(resources, "advanced/social.yml"),
                new File(resources, "data/arenas.yml"),
                new File(resources, "data/kits.yml"),
                new File(resources, "data/statistics.yml")))
        {
            YamlConfiguration yaml = new YamlConfiguration();
            yaml.load(file);
        }

        YamlConfiguration display = YamlConfiguration.loadConfiguration(
                new File(resources, "advanced/display.yml"));
        List<String> lobby = display.getStringList("Display.Scoreboards.Lobby.Lines");
        assertEquals("&7<date>", lobby.getFirst());
        assertEquals("&ewww.hyxduels.net", lobby.getLast());
        assertTrue(display.isList("Display.Scoreboards.Arena-Countdown.Lines"));
        assertTrue(display.isList("Display.Scoreboards.Arena-Playing.Lines"));
    }
}
