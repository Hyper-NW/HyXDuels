package me.alphatct3209.duels.gui.config;

import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.Test;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.List;

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
        assertEquals(MenuAction.PARTY_MANAGE, configuration.opener("party-manage").action());
        assertEquals("LECTERN", configuration.opener("party-manage").material().name());
        assertEquals(0, configuration.opener("party-manage").slot());
        assertEquals(MenuAction.PARTY_DISBAND, configuration.opener("party-disband").action());
        assertEquals("BARRIER", configuration.opener("party-disband").material().name());
        assertEquals(1, configuration.opener("party-disband").slot());
        assertEquals(47, configuration.slot("Menus.Mode.Legacy-PvP-Slot", -1, 54));
        assertEquals(17, configuration.integerList("Menus.Mode.Content-Slots", List.of()).size());
        assertEquals("bow", configuration.stringList("Menus.Mode.Order").getFirst());
        assertEquals("Play Duels", configuration.text("Menus.Duel-Type.Title", ""));
        assertEquals("Team Duels", configuration.text("Menus.Team-Mode.Title", ""));
        assertEquals("Other Modes", configuration.text("Menus.Other-Mode.Title", ""));
        assertEquals(List.of("parkour", "duel_arena"),
                configuration.stringList("Menus.Duel-Type.Other.Mode-Keys"));
        assertEquals("Edit Kit Layouts", configuration.text("Menus.Kit-Mode.Title", ""));
        assertEquals(MenuAction.SETTINGS, configuration.opener("settings").action());
        assertEquals(MenuAction.KIT_EDITOR, configuration.opener("kit-editor").action());
        assertEquals(6, configuration.opener("kit-editor").slot());
        assertEquals(9, yaml.getConfigurationSection("Menus.Settings.Items").getKeys(false).size());
        assertNotNull(yaml.getConfigurationSection("Menus.Kit-Editor.Controls"));
        assertNotNull(yaml.getConfigurationSection("Menus.Kit"));
        assertNotNull(yaml.getConfigurationSection("Menus.Party.Actions"));
        assertEquals(13, configuration.slot(
                "Menus.Party.Disband-Confirmation.Confirm.Slot", -1, 36));
        assertEquals("CAKE", configuration.text(
                "Menus.Party.Disband-Confirmation.Confirm.Material", ""));
        assertEquals(31, configuration.slot(
                "Menus.Party.Disband-Confirmation.Cancel.Slot", -1, 36));
        assertEquals("BARRIER", configuration.text(
                "Menus.Party.Disband-Confirmation.Cancel.Material", ""));
        assertEquals(true, configuration.fillerEnabled());
        assertEquals("GRAY_STAINED_GLASS_PANE", configuration.fillerMaterial().name());
        assertEquals(" ", configuration.fillerName());
        assertEquals(List.of("HyXDuels"), configuration.fillerLore());
        assertEquals(false, configuration.fillerGlow());
    }
}
