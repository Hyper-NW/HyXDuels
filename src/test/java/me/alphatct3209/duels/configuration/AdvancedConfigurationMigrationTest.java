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

    @Test
    void migratesOnlyTheFormerDefaultKitEditorMenuText()
    {
        YamlConfiguration configured = new YamlConfiguration();
        configured.set("Version", 1);
        configured.set("Openers.kit-editor.Lore", List.of(
                "&7Edit the hotbar layout for any duel kit.", "&eRight-click to open."));
        configured.set("Menus.Kit-Editor-Selector.Title", "&8My custom selector");
        configured.set("Menus.Kit-Editor-Selector.Item-Lore", List.of(
                "&7Kit key: &f<kit_key>", "&eClick to edit this layout."));
        YamlConfiguration bundled = YamlConfiguration.loadConfiguration(
                new java.io.File("src/main/resources/advanced/menus.yml"));

        assertTrue(AdvancedConfiguration.migrate("menus.yml", configured, bundled));
        assertEquals(bundled.getStringList("Openers.kit-editor.Lore"),
                configured.getStringList("Openers.kit-editor.Lore"));
        assertEquals("&8My custom selector",
                configured.getString("Menus.Kit-Editor-Selector.Title"));
        assertEquals(bundled.getStringList("Menus.Kit-Editor-Selector.Item-Lore"),
                configured.getStringList("Menus.Kit-Editor-Selector.Item-Lore"));
    }

    @Test
    void migratesOnlyUnchangedSoloDuelsPresentationDefaults()
    {
        YamlConfiguration menus = new YamlConfiguration();
        menus.set("Config-Version", 2);
        menus.set("Openers.duel-menu.Name", "&b&lQueue Duels");
        menus.set("Openers.duel-menu.Lore", List.of(
                "&7Choose a duel mode and queue.", "&eRight-click to open."));
        menus.set("Menus.Mode.Title", "&9⚔ Queue Duels &7(<page>/<pages>)");
        menus.set("Menus.Mode.Item-Name", "&dMy Custom Mode");
        menus.set("Menus.Mode.Item-Lore", List.of(
                "&7Key: &f<mode_key>", "&7Default kit: &f<kit>",
                "&eClick to queue or choose a kit."));
        YamlConfiguration bundledMenus = YamlConfiguration.loadConfiguration(
                new java.io.File("src/main/resources/advanced/menus.yml"));

        assertTrue(AdvancedConfiguration.migrate("menus.yml", menus, bundledMenus));
        assertEquals("&b&lQueue Duels", menus.getString("Openers.duel-menu.Name"));
        assertEquals("Solo Duels", menus.getString("Menus.Mode.Title"));
        assertEquals("&dMy Custom Mode", menus.getString("Menus.Mode.Item-Name"));
        assertEquals(bundledMenus.getStringList("Menus.Mode.Item-Lore"),
                menus.getStringList("Menus.Mode.Item-Lore"));

        YamlConfiguration messages = new YamlConfiguration();
        messages.set("Config-Version", 4);
        messages.set("Messages.Kill", List.of(
                "&c<victim> &7was killed by &a<killer>&7.",
                "&7Killer health: &c<killer_health>❤&7/&c<killer_max_health>❤"));
        messages.set("Messages.Win", List.of("&dCustom victory"));
        YamlConfiguration bundledMessages = YamlConfiguration.loadConfiguration(
                new java.io.File("src/main/resources/advanced/messages.yml"));

        assertTrue(AdvancedConfiguration.migrate("messages.yml", messages, bundledMessages));
        assertEquals(bundledMessages.getStringList("Messages.Kill"),
                messages.getStringList("Messages.Kill"));
        assertEquals(List.of("&dCustom victory"), messages.getStringList("Messages.Win"));
    }

    @Test
    void migratesOnlyTheFormerDefaultSoloOpenerToQueueDuels()
    {
        List<String> formerLore = List.of(
                "&7Click to play a &cSolo Duel &7against another player!", "",
                "&b&lDuel Types:", "&7• Bow Duel", "&7• Classic Duel", "&7• OP Duel",
                "&7• UHC Duel", "&7• NoDebuff Duel", "&7• Mega Walls Duel",
                "&7• Blitz Duel", "&7• SkyWars Duel", "&7• Combo Duel", "&7• Spleef Duel",
                "&7• Sumo Duel", "&7• Quakecraft Duel", "&7• Boxing Duel",
                "&7• Bridge Duel", "&7• Bed Wars Duel", "&7• Duel Arena",
                "&7• Parkour Duel", "", "&eRight-click to play!");
        YamlConfiguration menus = new YamlConfiguration();
        menus.set("Config-Version", 5);
        menus.set("Openers.duel-menu.Name", "&c&lSolo Duels");
        menus.set("Openers.duel-menu.Lore", formerLore);
        YamlConfiguration bundled = YamlConfiguration.loadConfiguration(
                new java.io.File("src/main/resources/advanced/menus.yml"));

        assertTrue(AdvancedConfiguration.migrate("menus.yml", menus, bundled));
        assertEquals("&b&lQueue Duels", menus.getString("Openers.duel-menu.Name"));
        assertEquals(bundled.getStringList("Openers.duel-menu.Lore"),
                menus.getStringList("Openers.duel-menu.Lore"));

        YamlConfiguration custom = new YamlConfiguration();
        custom.set("Config-Version", 5);
        custom.set("Openers.duel-menu.Name", "&dServer Duels");
        custom.set("Openers.duel-menu.Lore", List.of("&7Custom lore"));
        assertFalse(AdvancedConfiguration.migrate("menus.yml", custom, bundled));
        assertEquals("&dServer Duels", custom.getString("Openers.duel-menu.Name"));
        assertEquals(List.of("&7Custom lore"), custom.getStringList("Openers.duel-menu.Lore"));
    }

    @Test
    void migratesDefaultDisbandButtonsAndPreservesCustomizedConfirmation()
    {
        String base = "Menus.Party.Disband-Confirmation.";
        YamlConfiguration menus = new YamlConfiguration();
        menus.set("Config-Version", 7);
        menus.set(base + "Cancel.Slot", 13);
        menus.set(base + "Cancel.Material", "CAKE");
        menus.set(base + "Cancel.Name", "&aKeep Party");
        menus.set(base + "Cancel.Lore", List.of("&7Return to party management."));
        menus.set(base + "Confirm.Slot", 31);
        menus.set(base + "Confirm.Material", "BARRIER");
        menus.set(base + "Confirm.Name", "&c✖ Disband Party");
        menus.set(base + "Confirm.Lore", List.of(
                "&7This permanently disbands the party.", "&eClick to confirm."));
        YamlConfiguration custom = new YamlConfiguration();
        for (String path : menus.getKeys(true))
            if (!menus.isConfigurationSection(path)) custom.set(path, menus.get(path));
        custom.set(base + "Cancel.Name", "&dReturn");
        YamlConfiguration bundled = YamlConfiguration.loadConfiguration(
                new java.io.File("src/main/resources/advanced/menus.yml"));

        assertTrue(AdvancedConfiguration.migrate("menus.yml", menus, bundled));
        assertEquals(13, menus.getInt(base + "Confirm.Slot"));
        assertEquals("CAKE", menus.getString(base + "Confirm.Material"));
        assertEquals(31, menus.getInt(base + "Cancel.Slot"));
        assertEquals("BARRIER", menus.getString(base + "Cancel.Material"));
        assertEquals("&cGo Back", menus.getString(base + "Cancel.Name"));

        assertFalse(AdvancedConfiguration.migrate("menus.yml", custom, bundled));
        assertEquals("&dReturn", custom.getString(base + "Cancel.Name"));
    }

    @Test
    void migratesOnlyDefaultLobbyCommandArgumentToLiteralTarget()
    {
        YamlConfiguration bundled = YamlConfiguration.loadConfiguration(
                new java.io.File("src/main/resources/advanced/display.yml"));
        List<String> former = bundled.getStringList(LOBBY_LINES).stream()
                .map(line -> line.replace("/duel <target>", "/duel <player>"))
                .toList();
        YamlConfiguration display = new YamlConfiguration();
        display.set("Config-Version", 3);
        display.set(LOBBY_LINES, former);

        assertTrue(AdvancedConfiguration.migrate("display.yml", display, bundled));
        assertTrue(display.getStringList(LOBBY_LINES).contains("&e/duel <target>"));

        YamlConfiguration custom = new YamlConfiguration();
        custom.set("Config-Version", 3);
        custom.set(LOBBY_LINES, List.of("&dUse /duel <player> now"));
        assertFalse(AdvancedConfiguration.migrate("display.yml", custom, bundled));
        assertEquals(List.of("&dUse /duel <player> now"), custom.getStringList(LOBBY_LINES));
    }

    @Test
    void disablesNaturalRegenerationInLegacyModeConfigurations()
    {
        YamlConfiguration modes = new YamlConfiguration();
        modes.set("Config-Version", 1);
        modes.set("Modes.classic.combat.natural-regeneration", true);
        modes.set("Modes.uhc.combat.natural-regeneration", false);

        assertTrue(AdvancedConfiguration.migrate("modes.yml", modes));
        assertFalse(modes.getBoolean("Modes.classic.combat.natural-regeneration"));
        assertFalse(modes.getBoolean("Modes.uhc.combat.natural-regeneration"));
        assertFalse(AdvancedConfiguration.migrate("modes.yml", modes));
    }
}
