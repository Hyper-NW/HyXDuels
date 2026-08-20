package me.alphatct3209.duels.gui.config;

import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.Test;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class DefaultMenuResourceTest
{
    @Test
    void shippedMenuConfigurationParsesAndContainsQueueKitAndPartyLayouts()
    {
        InputStream stream = getClass().getClassLoader().getResourceAsStream("advanced/menus.yml");
        assertNotNull(stream);
        YamlConfiguration yaml = YamlConfiguration.loadConfiguration(
                new InputStreamReader(stream, StandardCharsets.UTF_8));
        MenuConfiguration configuration = new MenuConfiguration(yaml);
        assertEquals("DIAMOND_SWORD", configuration.text("Openers.duel-menu.Material", ""));
        assertEquals("Queue Duels", org.bukkit.ChatColor.stripColor(
                org.bukkit.ChatColor.translateAlternateColorCodes('&',
                        configuration.opener("duel-menu").name())));
        assertEquals(MenuAction.PARTY, configuration.opener("party").action());
        assertEquals("LECTERN", configuration.opener("party").material().name());
        assertEquals(1, configuration.opener("party").slot());
        assertEquals(true, configuration.opener("party").forceSlot());
        assertEquals(47, configuration.slot("Menus.Mode.Legacy-PvP-Slot", -1, 54));
        assertEquals(MenuAction.SETTINGS, configuration.opener("settings").action());
        assertEquals(MenuAction.KIT_EDITOR, configuration.opener("kit-editor").action());
        assertEquals(6, configuration.opener("kit-editor").slot());
        assertEquals(9, yaml.getConfigurationSection("Menus.Settings.Items").getKeys(false).size());
        assertNotNull(yaml.getConfigurationSection("Menus.Kit-Editor.Controls"));
        assertNotNull(yaml.getConfigurationSection("Menus.Kit"));
        assertNotNull(yaml.getConfigurationSection("Menus.Party.Actions"));
    }
}
