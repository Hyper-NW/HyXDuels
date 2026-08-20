package me.alphatct3209.duels.utils;

import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.Test;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DefaultConfigurationResourceTest
{
    @Test
    void shippedConfigParsesWithMultilineQueueGameChallengeAndPartyMessages()
    {
        InputStream stream = getClass().getClassLoader().getResourceAsStream("advanced/messages.yml");
        assertNotNull(stream);
        YamlConfiguration yaml = YamlConfiguration.loadConfiguration(
                new InputStreamReader(stream, StandardCharsets.UTF_8));
        assertNotNull(yaml.getConfigurationSection("Messages"));
        assertTrue(yaml.isList("Messages.Round"));
        assertTrue(yaml.isList("Messages.Kill"));
        assertTrue(yaml.isList("Messages.Queue-Joined"));
        assertTrue(yaml.isList("Messages.Challenge-Received"));
        assertTrue(yaml.isList("Messages.Party-Usage"));
    }
}
