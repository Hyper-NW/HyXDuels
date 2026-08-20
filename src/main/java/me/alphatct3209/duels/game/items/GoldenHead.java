package me.alphatct3209.duels.game.items;

import me.alphatct3209.duels.Duels;
import me.alphatct3209.duels.configuration.PluginFiles;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Sound;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.io.File;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

/** Configurable player-head consumable used by UHC and administrator give commands. */
public final class GoldenHead
{
    public static final int DEFAULT_UHC_AMOUNT = 3;
    public static final int REGENERATION_DURATION_TICKS = 5 * 20;
    public static final int REGENERATION_AMPLIFIER = 2;
    public static final int ABSORPTION_DURATION_TICKS = 2 * 60 * 20;
    public static final int ABSORPTION_AMPLIFIER = 0;
    private static final NamespacedKey KEY = new NamespacedKey("hyxduels", "golden_head");

    private final File file;
    private boolean enabled;
    private boolean giveOnUhcStart;
    private int uhcAmount;
    private boolean restrictConsumptionToUhc;
    private int commandDefaultAmount;
    private int commandMaximumAmount;
    private String name;
    private List<String> lore;
    private boolean glow;
    private Integer customModelData;
    private boolean regenerationEnabled;
    private int regenerationDurationTicks;
    private int regenerationAmplifier;
    private boolean absorptionEnabled;
    private int absorptionDurationTicks;
    private int absorptionAmplifier;
    private Sound sound;
    private float soundVolume;
    private float soundPitch;

    public GoldenHead(Duels plugin)
    {
        Objects.requireNonNull(plugin);
        file = PluginFiles.advanced(plugin, "golden-heads.yml");
        reload();
    }

    public void reload()
    {
        YamlConfiguration config = new YamlConfiguration();
        try { config.load(file); }
        catch (Exception exception)
        {
            throw new IllegalStateException("Could not load advanced/golden-heads.yml", exception);
        }
        boolean parsedEnabled = config.getBoolean("Enabled", true);
        boolean parsedGiveOnUhcStart = config.getBoolean("UHC.Give-On-Match-Start", true);
        int parsedUhcAmount = bounded(config.getInt("UHC.Amount", DEFAULT_UHC_AMOUNT), 1, 64,
                "UHC.Amount");
        boolean parsedRestrictConsumptionToUhc = config.getBoolean("Consumption.Only-In-UHC", true);
        int parsedCommandDefaultAmount = bounded(config.getInt("Command.Default-Amount", 1), 1, 64,
                "Command.Default-Amount");
        int parsedCommandMaximumAmount = bounded(config.getInt("Command.Maximum-Amount", 64), 1, 2304,
                "Command.Maximum-Amount");
        if (parsedCommandDefaultAmount > parsedCommandMaximumAmount)
            throw new IllegalStateException("Command.Default-Amount cannot exceed Command.Maximum-Amount");
        String parsedName = Objects.requireNonNull(config.getString("Item.Name", "&6Golden Head"));
        List<String> parsedLore = List.copyOf(config.getStringList("Item.Lore"));
        boolean parsedGlow = config.getBoolean("Item.Glow", false);
        int model = config.getInt("Item.Custom-Model-Data", 0);
        Integer parsedCustomModelData = model > 0 ? model : null;
        boolean parsedRegenerationEnabled = config.getBoolean("Effects.Regeneration.Enabled", true);
        int regenerationLevel = bounded(config.getInt("Effects.Regeneration.Level", 3), 1, 256,
                "Effects.Regeneration.Level");
        int parsedRegenerationAmplifier = regenerationLevel - 1;
        int parsedRegenerationDurationTicks = secondsToTicks(config.getDouble(
                "Effects.Regeneration.Duration-Seconds", 5.0), "Effects.Regeneration.Duration-Seconds");
        boolean parsedAbsorptionEnabled = config.getBoolean("Effects.Absorption.Enabled", true);
        int absorptionLevel = bounded(config.getInt("Effects.Absorption.Level", 1), 1, 256,
                "Effects.Absorption.Level");
        int parsedAbsorptionAmplifier = absorptionLevel - 1;
        int parsedAbsorptionDurationTicks = secondsToTicks(config.getDouble(
                "Effects.Absorption.Duration-Seconds", 120.0), "Effects.Absorption.Duration-Seconds");
        Sound parsedSound;
        try
        {
            parsedSound = Sound.valueOf(config.getString("Consumption.Sound", "ENTITY_GENERIC_EAT")
                    .toUpperCase(Locale.ROOT));
        }
        catch (IllegalArgumentException exception)
        {
            throw new IllegalStateException("Consumption.Sound is not a valid Bukkit sound", exception);
        }
        float parsedSoundVolume = finiteFloat(config.getDouble("Consumption.Sound-Volume", 1.0),
                0.0, 10.0, "Consumption.Sound-Volume");
        float parsedSoundPitch = finiteFloat(config.getDouble("Consumption.Sound-Pitch", 1.0),
                0.0, 2.0, "Consumption.Sound-Pitch");

        enabled = parsedEnabled;
        giveOnUhcStart = parsedGiveOnUhcStart;
        uhcAmount = parsedUhcAmount;
        restrictConsumptionToUhc = parsedRestrictConsumptionToUhc;
        commandDefaultAmount = parsedCommandDefaultAmount;
        commandMaximumAmount = parsedCommandMaximumAmount;
        name = parsedName;
        lore = parsedLore;
        glow = parsedGlow;
        customModelData = parsedCustomModelData;
        regenerationEnabled = parsedRegenerationEnabled;
        regenerationAmplifier = parsedRegenerationAmplifier;
        regenerationDurationTicks = parsedRegenerationDurationTicks;
        absorptionEnabled = parsedAbsorptionEnabled;
        absorptionAmplifier = parsedAbsorptionAmplifier;
        absorptionDurationTicks = parsedAbsorptionDurationTicks;
        sound = parsedSound;
        soundVolume = parsedSoundVolume;
        soundPitch = parsedSoundPitch;
    }

    public ItemStack create(Player owner, int amount)
    {
        if (amount < 1 || amount > 64)
            throw new IllegalArgumentException("A Golden Head stack must be between 1 and 64");
        ItemStack item = new ItemStack(Material.PLAYER_HEAD, amount);
        SkullMeta meta = (SkullMeta) item.getItemMeta();
        meta.setOwningPlayer(owner);
        meta.setDisplayName(color(replace(name, owner)));
        List<String> renderedLore = lore.isEmpty() ? List.of(
                "&7Right-click to consume",
                "&dRegeneration <regen_level> &7(<regen_seconds>s)",
                "&9Absorption <absorption_level> &7(<absorption_seconds>s)") : lore;
        meta.setLore(renderedLore.stream().map(line -> color(replace(line, owner))).toList());
        meta.setEnchantmentGlintOverride(glow);
        if (customModelData != null) meta.setCustomModelData(customModelData);
        meta.getPersistentDataContainer().set(KEY, PersistentDataType.BYTE, (byte) 1);
        item.setItemMeta(meta);
        return item;
    }

    public static boolean isGoldenHead(ItemStack item)
    {
        if (item == null || item.getType() != Material.PLAYER_HEAD || !item.hasItemMeta()) return false;
        ItemMeta meta = item.getItemMeta();
        return meta.getPersistentDataContainer().has(KEY, PersistentDataType.BYTE);
    }

    public void applyEffects(Player player)
    {
        if (regenerationEnabled)
            player.addPotionEffect(new PotionEffect(PotionEffectType.REGENERATION,
                    regenerationDurationTicks, regenerationAmplifier, false, true, true), true);
        if (absorptionEnabled)
            player.addPotionEffect(new PotionEffect(PotionEffectType.ABSORPTION,
                    absorptionDurationTicks, absorptionAmplifier, false, true, true), true);
    }

    public void playConsumptionSound(Player player)
    {
        player.getWorld().playSound(player.getLocation(), sound, soundVolume, soundPitch);
    }

    public boolean enabled() { return enabled; }
    public boolean giveOnUhcStart() { return giveOnUhcStart; }
    public int uhcAmount() { return uhcAmount; }
    public boolean restrictConsumptionToUhc() { return restrictConsumptionToUhc; }
    public int commandDefaultAmount() { return commandDefaultAmount; }
    public int commandMaximumAmount() { return commandMaximumAmount; }

    private String replace(String value, Player owner)
    {
        return value.replace("<player>", owner.getName())
                .replace("<regen_level>", Integer.toString(regenerationAmplifier + 1))
                .replace("<regen_seconds>", formatSeconds(regenerationDurationTicks))
                .replace("<absorption_level>", Integer.toString(absorptionAmplifier + 1))
                .replace("<absorption_seconds>", formatSeconds(absorptionDurationTicks));
    }

    private static String formatSeconds(int ticks)
    {
        double seconds = ticks / 20.0;
        return seconds == Math.rint(seconds) ? Long.toString(Math.round(seconds)) : Double.toString(seconds);
    }

    private static int secondsToTicks(double seconds, String path)
    {
        if (!Double.isFinite(seconds) || seconds <= 0 || seconds > 86_400)
            throw new IllegalStateException(path + " must be between 0 and 86400 seconds");
        return Math.max(1, (int) Math.round(seconds * 20.0));
    }

    private static int bounded(int value, int minimum, int maximum, String path)
    {
        if (value < minimum || value > maximum)
            throw new IllegalStateException(path + " must be between " + minimum + " and " + maximum);
        return value;
    }

    private static float finiteFloat(double value, double minimum, double maximum, String path)
    {
        if (!Double.isFinite(value) || value < minimum || value > maximum)
            throw new IllegalStateException(path + " must be between " + minimum + " and " + maximum);
        return (float) value;
    }

    private static String color(String value)
    {
        return ChatColor.translateAlternateColorCodes('&', value == null ? "" : value);
    }
}
