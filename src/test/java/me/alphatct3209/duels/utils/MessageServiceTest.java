package me.alphatct3209.duels.utils;

import org.bukkit.ChatColor;
import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

class MessageServiceTest
{
    @Test
    void rendersScalarAndListMessagesWithPlaceholdersAndColors()
    {
        YamlConfiguration yaml = new YamlConfiguration();
        yaml.set("Messages.Scalar", "&aHello <player>");
        yaml.set("Messages.List", List.of("&cFirst", "Second <player>"));

        assertEquals(List.of(ChatColor.GREEN + "Hello Alex"),
                MessageService.render(yaml, "Messages.Scalar", Map.of("<player>", "Alex")));
        assertEquals(List.of(ChatColor.RED + "First", "Second Alex"),
                MessageService.render(yaml, "Messages.List", Map.of("<player>", "Alex")));
    }
}
