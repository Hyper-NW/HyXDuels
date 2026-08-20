package me.alphatct3209.duels.gui.config;

import me.alphatct3209.duels.Duels;
import me.alphatct3209.duels.configuration.PluginFiles;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

public final class MenuConfiguration
{
    private final YamlConfiguration yaml;
    private final List<MenuOpener> openers;

    public MenuConfiguration(Duels plugin)
    {
        this(load(Objects.requireNonNull(plugin, "plugin")));
    }

    MenuConfiguration(YamlConfiguration yaml)
    {
        this.yaml = Objects.requireNonNull(yaml, "yaml");
        int version = yaml.getInt("Version", 0);
        if (version != 1)
        {
            throw new IllegalArgumentException("menus.yml Version must be 1");
        }
        openers = parseOpeners();
        validateMainLayout();
        validatePartyLayout();
        validateModeLayout();
        validateSettingsLayout();
    }

    private static YamlConfiguration load(Duels plugin)
    {
        File file = PluginFiles.advanced(plugin, "menus.yml");
        YamlConfiguration configured = YamlConfiguration.loadConfiguration(file);
        // Missing keys use the bundled resource without rewriting or discarding server comments/overrides.
        try (InputStream stream = plugin.getResource(PluginFiles.advancedResource("menus.yml")))
        {
            if (stream == null)
                throw new IllegalStateException("The bundled menus.yml resource is missing");
            YamlConfiguration bundled = YamlConfiguration.loadConfiguration(
                    new InputStreamReader(stream, StandardCharsets.UTF_8));
            mergeMissing(configured, bundled);
        }
        catch (IOException exception)
        {
            throw new IllegalStateException("Could not read the bundled menus.yml defaults", exception);
        }
        return configured;
    }

    static void mergeMissing(YamlConfiguration configured, YamlConfiguration bundled)
    {
        for (Map.Entry<String, Object> entry : bundled.getValues(true).entrySet())
        {
            if (!(entry.getValue() instanceof ConfigurationSection)
                    && !configured.contains(entry.getKey()))
            {
                configured.set(entry.getKey(), entry.getValue());
            }
        }
    }

    public boolean enabled()
    {
        return yaml.getBoolean("Enabled", true);
    }

    public boolean giveOnJoin()
    {
        return yaml.getBoolean("Give-On-Join", true);
    }

    public long giveDelayTicks()
    {
        return Math.max(1L, Math.min(200L, yaml.getLong("Give-Delay-Ticks", 2L)));
    }

    public List<MenuOpener> openers()
    {
        return openers;
    }

    public MenuOpener opener(String id)
    {
        return openers.stream().filter(value -> value.id().equals(id)).findFirst().orElse(null);
    }

    public String text(String path, String fallback)
    {
        return yaml.getString(path, fallback);
    }

    public List<String> lines(String path)
    {
        return List.copyOf(yaml.getStringList(path));
    }


    public List<String> messageLines(String path, String fallback)
    {
        Object raw = yaml.get(path);
        if (raw instanceof List<?> list)
        {
            return list.stream().filter(Objects::nonNull).map(Object::toString).toList();
        }
        return List.of(raw == null ? fallback : raw.toString());
    }

    public int slot(String path, int fallback, int inventorySize)
    {
        int slot = yaml.getInt(path, fallback);
        if (slot < 0 || slot >= inventorySize)
        {
            throw new IllegalArgumentException("menus.yml " + path + " must be from 0 through "
                    + (inventorySize - 1));
        }
        return slot;
    }

    private void validatePartyLayout()
    {
        Set<Integer> slots = new HashSet<>();
        validateUniquePartySlot(slots, "Menus.Party.Actions.Party-FFA.Slot", 10);
        validateUniquePartySlot(slots, "Menus.Party.Actions.Team-Battle.Slot", 12);
        validateUniquePartySlot(slots, "Menus.Party.Actions.Visibility.Slot", 14);
        validateUniquePartySlot(slots, "Menus.Party.Actions.Host-Duel.Slot", 16);
        validateUniquePartySlot(slots, "Menus.Party.Actions.Invite-Friends.Slot", 22);
    }

    private void validateUniquePartySlot(Set<Integer> slots, String path, int fallback)
    {
        int configured = slot(path, fallback, 27);
        if (!slots.add(configured))
            throw new IllegalArgumentException("menus.yml party action slots must be unique; "
                    + path + " duplicates slot " + configured);
    }

    private void validateSettingsLayout()
    {
        Set<Integer> slots = new HashSet<>();
        String[] keys = {"show-own-tier", "scoreboard", "profile-kits", "friend-join-notifier",
                "blast-particles", "duel-requests", "direct-messages", "party-invites", "friend-requests"};
        for (int index = 0; index < keys.length; index++)
        {
            String path = "Menus.Settings.Items." + keys[index] + ".Slot";
            int configured = slot(path, 9 + index, 27);
            if (!slots.add(configured))
                throw new IllegalArgumentException("menus.yml settings item slots must be unique; "
                        + path + " duplicates slot " + configured);
        }
    }

    private void validateModeLayout()
    {
        int legacy = slot("Menus.Mode.Legacy-PvP-Slot", 47, 54);
        if (legacy < 45 || legacy == 45 || legacy == 49 || legacy == 53)
            throw new IllegalArgumentException("Menus.Mode.Legacy-PvP-Slot must be an unused bottom-row slot");
    }

    private void validateMainLayout()
    {
        Set<Integer> slots = new HashSet<>();
        validateUniqueSlot(slots, "Menus.Main.Mode-Slot", 11);
        validateUniqueSlot(slots, "Menus.Main.Map-Slot", 13);
        validateUniqueSlot(slots, "Menus.Main.Opponent-Slot", 15);
        validateUniqueSlot(slots, "Menus.Main.Quick-Join-Slot", 22);
    }

    private void validateUniqueSlot(Set<Integer> slots, String path, int fallback)
    {
        int configured = slot(path, fallback, 27);
        if (!slots.add(configured))
        {
            throw new IllegalArgumentException("menus.yml main-menu slots must be unique; "
                    + path + " duplicates slot " + configured);
        }
    }


    private List<MenuOpener> parseOpeners()
    {
        ConfigurationSection section = yaml.getConfigurationSection("Openers");
        if (section == null)
        {
            throw new IllegalArgumentException("menus.yml Openers section is required");
        }
        List<MenuOpener> values = new ArrayList<>();
        Set<Integer> enabledSlots = new HashSet<>();
        for (String rawId : section.getKeys(false))
        {
            String id = rawId.toLowerCase(Locale.ROOT);
            if (!rawId.equals(id) || !id.matches("[a-z0-9_-]+"))
            {
                throw new IllegalArgumentException("menus.yml opener id '" + rawId
                        + "' must use lowercase letters, numbers, '_' or '-'");
            }
            String path = "Openers." + rawId;
            Material material = Material.matchMaterial(yaml.getString(path + ".Material", ""));
            if (material == null || material == Material.AIR
                    || material == Material.CAVE_AIR || material == Material.VOID_AIR)
            {
                throw new IllegalArgumentException(path + ".Material is not a usable material");
            }
            MenuAction action;
            try
            {
                action = MenuAction.valueOf(yaml.getString(path + ".Action", "")
                        .toUpperCase(Locale.ROOT).replace('-', '_'));
            }
            catch (IllegalArgumentException exception)
            {
                throw new IllegalArgumentException(path
                        + ".Action must be DUEL_MENU, MODE_SELECTOR, MAP_SELECTOR, SETTINGS, PARTY, or KIT_EDITOR");
            }
            int slot = yaml.getInt(path + ".Slot", 0);
            boolean forceSlot = yaml.getBoolean(path + ".Force-Slot", false);
            boolean enabled = yaml.getBoolean(path + ".Enabled", true);
            if (enabled && !enabledSlots.add(slot))
            {
                throw new IllegalArgumentException("Multiple enabled menu items use hotbar slot " + slot);
            }
            Integer model = yaml.isInt(path + ".Custom-Model-Data")
                    ? yaml.getInt(path + ".Custom-Model-Data") : null;
            values.add(new MenuOpener(id, enabled, material, slot,
                    yaml.getString(path + ".Name", "&bDuels"),
                    yaml.getStringList(path + ".Lore"), action,
                    yaml.getBoolean(path + ".Glow", false),
                    yaml.getBoolean(path + ".Locked", true), forceSlot, model));
        }
        return List.copyOf(values);
    }
}
