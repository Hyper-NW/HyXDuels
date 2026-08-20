package me.alphatct3209.duels.utils;

import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ProjectMetadataTest
{
    @Test
    void usesTheAlphatctNamespaceAndOnePointOnePatchRelease() throws Exception
    {
        String pom = Files.readString(Path.of("pom.xml"));
        assertTrue(pom.contains("<groupId>me.alphatct3209</groupId>"));
        assertTrue(pom.contains("<version>1.1.1</version>"));
        YamlConfiguration plugin = YamlConfiguration.loadConfiguration(
                new File("src/main/resources/plugin.yml"));
        assertEquals("me.alphatct3209.duels.Duels", plugin.getString("main"));
        assertEquals("alphatct3209", plugin.getString("author"));
    }
}
