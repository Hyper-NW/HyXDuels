package me.alphatct3209.duels.utils;

import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ProjectMetadataTest
{
    @Test
    void usesTheAlphatctNamespaceAndOnePointThreeFeatureRelease() throws Exception
    {
        String pom = Files.readString(Path.of("pom.xml"));
        assertTrue(pom.contains("<groupId>me.alphatct3209</groupId>"));
        assertTrue(pom.contains("<version>1.3.0</version>"));
        YamlConfiguration plugin = YamlConfiguration.loadConfiguration(
                new File("src/main/resources/plugin.yml"));
        assertEquals("me.alphatct3209.duels.Duels", plugin.getString("main"));
        assertEquals("alphatct3209", plugin.getString("author"));
        assertEquals(Set.of("duel", "duels"),
                plugin.getConfigurationSection("commands").getKeys(false));
        assertFalse(plugin.getStringList("commands.duel.aliases").contains("duels"));
        assertEquals("/<command> <player>", plugin.getString("commands.duel.usage"));
    }
}
