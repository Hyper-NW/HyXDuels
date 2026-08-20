package me.alphatct3209.duels.gui.config;

import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class MenuConfigurationTest
{
    @Test
    void acceptsValidMainLayoutAndOpener()
    {
        MenuConfiguration configuration = new MenuConfiguration(valid());
        assertEquals(1, configuration.openers().size());
        assertEquals(11, configuration.slot("Menus.Main.Mode-Slot", 0, 27));
    }

    @Test
    void rejectsDuplicateOrOutOfRangeMainSlotsAtLoadTime()
    {
        YamlConfiguration duplicate = valid();
        duplicate.set("Menus.Main.Map-Slot", 11);
        assertThrows(IllegalArgumentException.class, () -> new MenuConfiguration(duplicate));

        YamlConfiguration outOfRange = valid();
        outOfRange.set("Menus.Main.Quick-Join-Slot", 27);
        assertThrows(IllegalArgumentException.class, () -> new MenuConfiguration(outOfRange));
    }

    @Test
    void rejectsDuplicateEnabledOpenerSlots()
    {
        YamlConfiguration yaml = valid();
        yaml.set("Openers.mode.Enabled", true);
        yaml.set("Openers.mode.Material", "NETHER_STAR");
        yaml.set("Openers.mode.Slot", 0);
        yaml.set("Openers.mode.Action", "MODE_SELECTOR");
        yaml.set("Openers.mode.Name", "Mode");
        assertThrows(IllegalArgumentException.class, () -> new MenuConfiguration(yaml));
    }

    @Test
    void inheritsNewBundledOpenersWithoutReplacingConfiguredValues()
    {
        YamlConfiguration configured = valid();
        configured.set("Openers.duel-menu.Name", "Server Queue");
        YamlConfiguration defaults = valid();
        defaults.set("Openers.party.Enabled", true);
        defaults.set("Openers.party.Material", "LECTERN");
        defaults.set("Openers.party.Slot", 4);
        defaults.set("Openers.party.Action", "PARTY");
        defaults.set("Openers.party.Name", "Party");
        MenuConfiguration.mergeMissing(configured, defaults);

        MenuConfiguration configuration = new MenuConfiguration(configured);
        assertEquals("Server Queue", configuration.opener("duel-menu").name());
        assertEquals(MenuAction.PARTY, configuration.opener("party").action());
    }

    @Test
    void rejectsDuplicatePartyActionsAndModeNavigationCollisions()
    {
        YamlConfiguration duplicate = valid();
        duplicate.set("Menus.Party.Actions.Party-FFA.Slot", 10);
        duplicate.set("Menus.Party.Actions.Team-Battle.Slot", 10);
        assertThrows(IllegalArgumentException.class, () -> new MenuConfiguration(duplicate));

        YamlConfiguration collision = valid();
        collision.set("Menus.Mode.Legacy-PvP-Slot", 49);
        assertThrows(IllegalArgumentException.class, () -> new MenuConfiguration(collision));
    }

    @Test
    void rejectsDuplicateOrOutOfRangeSettingsSlots()
    {
        YamlConfiguration duplicate = valid();
        duplicate.set("Menus.Settings.Items.show-own-tier.Slot", 9);
        duplicate.set("Menus.Settings.Items.scoreboard.Slot", 9);
        assertThrows(IllegalArgumentException.class, () -> new MenuConfiguration(duplicate));

        YamlConfiguration outOfRange = valid();
        outOfRange.set("Menus.Settings.Items.friend-requests.Slot", 27);
        assertThrows(IllegalArgumentException.class, () -> new MenuConfiguration(outOfRange));
    }

    private YamlConfiguration valid()
    {
        YamlConfiguration yaml = new YamlConfiguration();
        yaml.set("Version", 1);
        yaml.set("Openers.duel-menu.Enabled", true);
        yaml.set("Openers.duel-menu.Material", "DIAMOND_SWORD");
        yaml.set("Openers.duel-menu.Slot", 0);
        yaml.set("Openers.duel-menu.Action", "DUEL_MENU");
        yaml.set("Openers.duel-menu.Name", "Duels");
        yaml.set("Menus.Main.Mode-Slot", 11);
        yaml.set("Menus.Main.Map-Slot", 13);
        yaml.set("Menus.Main.Opponent-Slot", 15);
        yaml.set("Menus.Main.Quick-Join-Slot", 22);
        return yaml;
    }
}
