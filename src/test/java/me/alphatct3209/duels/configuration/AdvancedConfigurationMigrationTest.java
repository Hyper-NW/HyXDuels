package me.alphatct3209.duels.configuration;

import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AdvancedConfigurationMigrationTest
{
    private static final String LOBBY_LINES = "Display.Scoreboards.Lobby.Lines";

    @Test
    void addsMissingSchemaValuesWithoutReplacingAdministratorValues()
    {
        YamlConfiguration configured = new YamlConfiguration();
        configured.set("Config-Version", 1);
        configured.set("Display.Refresh-Ticks", 80);
        configured.set("Administrator.Custom-Key", "keep-me");

        YamlConfiguration bundled = new YamlConfiguration();
        bundled.set("Config-Version", 2);
        bundled.set("Display.Refresh-Ticks", 20);
        bundled.set("Display.Scoreboards.Lobby.Enabled", true);

        assertTrue(AdvancedConfiguration.mergeMissing(configured, bundled));
        assertEquals(1, configured.getInt("Config-Version"));
        assertEquals(80, configured.getInt("Display.Refresh-Ticks"));
        assertEquals("keep-me", configured.getString("Administrator.Custom-Key"));
        assertTrue(configured.getBoolean("Display.Scoreboards.Lobby.Enabled"));
        assertFalse(AdvancedConfiguration.mergeMissing(configured, bundled));
        assertTrue(AdvancedConfiguration.updateSchemaVersion(configured, bundled));
        assertEquals(2, configured.getInt("Config-Version"));
        assertFalse(AdvancedConfiguration.updateSchemaVersion(configured, bundled));
    }

    @Test
    void migratesOnlyTheExactLegacyReversedLobbyDefault()
    {
        YamlConfiguration legacy = new YamlConfiguration();
        legacy.set(LOBBY_LINES, List.of(
                "&ewww.hyxduels.net", "",
                "&fOverall Kills: &a%duels_your_overall_kills%",
                "&fOverall Wins: &a%duels_your_overall_wins%", "",
                "&fBest Winstreak: &a%duels_your_highest_winstreak%",
                "&fOverall Winstreak: &a%duels_your_winstreak%", "",
                "&fTokens: &a0", "", "&e/duel <player>",
                "&bDuel other players with:", "", "&7<date>"));

        assertTrue(AdvancedConfiguration.migrate("display.yml", legacy));
        List<String> migrated = legacy.getStringList(LOBBY_LINES);
        assertEquals("&7<date>", migrated.getFirst());
        assertEquals("&ewww.hyxduels.net", migrated.getLast());

        YamlConfiguration custom = new YamlConfiguration();
        custom.set(LOBBY_LINES, List.of("&aMy custom first line", "&bMy custom second line"));
        assertFalse(AdvancedConfiguration.migrate("display.yml", custom));
        assertEquals(List.of("&aMy custom first line", "&bMy custom second line"),
                custom.getStringList(LOBBY_LINES));
    }

    @Test
    void migratesOnlyUnchangedVersionTwoCommandMessages()
    {
        YamlConfiguration configured = new YamlConfiguration();
        configured.set("Config-Version", 2);
        configured.set("Messages.Help-Menu", List.of("&aMy private help entry"));
        configured.set("Messages.Message-Usage",
                List.of("&cUsage: /msg <player> <message>"));
        configured.set("Messages.Party-Invite-Received", List.of(
                "&e<player> &ainvited you to &e<leader>'s &aparty.",
                "&7Use &e/p accept &7or &e/p deny&7."));
        configured.set("Messages.Friend-Usage", List.of("&bMy custom friend syntax"));

        YamlConfiguration bundled = YamlConfiguration.loadConfiguration(
                new java.io.File("src/main/resources/advanced/messages.yml"));

        assertTrue(AdvancedConfiguration.migrate("messages.yml", configured, bundled));
        assertEquals(List.of("&aMy private help entry"),
                configured.getStringList("Messages.Help-Menu"));
        assertEquals(List.of("&cUsage: /duels social message <player> <message>"),
                configured.getStringList("Messages.Message-Usage"));
        assertEquals(List.of(
                        "&e<player> &ainvited you to &e<leader>'s &aparty.",
                        "&7Use &e/duels party accept &7or &e/duels party deny&7."),
                configured.getStringList("Messages.Party-Invite-Received"));
        assertEquals(List.of("&bMy custom friend syntax"),
                configured.getStringList("Messages.Friend-Usage"));
        assertFalse(AdvancedConfiguration.migrate("messages.yml", configured, bundled));
    }
}
