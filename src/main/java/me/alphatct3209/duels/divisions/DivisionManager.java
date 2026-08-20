package me.alphatct3209.duels.divisions;

import me.alphatct3209.duels.configuration.PluginFiles;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.function.Consumer;

/**
 * Loads ordered division definitions and derives all division state from wins.
 */
public final class DivisionManager
{
    private static final Set<String> ROOT_KEYS = Set.of("Config-Version", "tiers");
    private static final Set<String> TIER_KEYS = Set.of("wins-per-step", "levels");
    private static final Set<String> LEVEL_KEYS = Set.of("rewards");

    private final List<Division> divisions;
    private final Consumer<String> commandDispatcher;

    /**
     * Copies the bundled divisions.yml when absent, then strictly loads it.
     */
    public DivisionManager(JavaPlugin plugin)
    {
        this(copyAndLoad(Objects.requireNonNull(plugin, "plugin")),
                command -> plugin.getServer().dispatchCommand(plugin.getServer().getConsoleSender(), command));
    }

    /**
     * Configuration-only constructor, suitable for tests and tooling.
     */
    public DivisionManager(ConfigurationSection configuration)
    {
        this(configuration, null);
    }

    DivisionManager(ConfigurationSection configuration, Consumer<String> commandDispatcher)
    {
        this.divisions = parse(Objects.requireNonNull(configuration, "configuration"));
        this.commandDispatcher = commandDispatcher;
    }

    public List<Division> getDivisions()
    {
        return divisions;
    }

    public Optional<Division> getCurrentDivision(long wins)
    {
        requireValidWins(wins);
        Division current = null;
        for (Division division : divisions)
        {
            if (division.requiredWins() > wins)
            {
                break;
            }
            current = division;
        }
        return Optional.ofNullable(current);
    }

    public Optional<Division> getNextDivision(long wins)
    {
        requireValidWins(wins);
        return divisions.stream()
                .filter(division -> division.requiredWins() > wins)
                .findFirst();
    }

    public DivisionProgress getProgress(long wins)
    {
        requireValidWins(wins);
        Optional<Division> current = getCurrentDivision(wins);
        Optional<Division> next = getNextDivision(wins);
        if (next.isEmpty())
        {
            return new DivisionProgress(wins, current, next, 0, 0);
        }

        long stepStart = current.map(Division::requiredWins).orElse(0L);
        long stepEnd = next.get().requiredWins();
        return new DivisionProgress(wins, current, next, wins - stepStart, stepEnd - stepStart);
    }

    /**
     * Returns every destination division crossed in ascending threshold order.
     */
    public List<Division> getCrossedDivisions(long previousWins, long currentWins)
    {
        requireValidWins(previousWins);
        requireValidWins(currentWins);
        if (currentWins <= previousWins)
        {
            return List.of();
        }
        return divisions.stream()
                .filter(division -> division.requiredWins() > previousWins
                        && division.requiredWins() <= currentWins)
                .toList();
    }

    public void executeRewards(Player player, String gamemode, Division division, long wins)
    {
        Objects.requireNonNull(player, "player");
        Objects.requireNonNull(gamemode, "gamemode");
        Objects.requireNonNull(division, "division");
        requireValidWins(wins);
        if (gamemode.isBlank())
        {
            throw new IllegalArgumentException("Gamemode cannot be blank");
        }
        if (commandDispatcher == null)
        {
            throw new IllegalStateException("No console command dispatcher is configured");
        }

        for (String reward : division.rewards())
        {
            String command = reward
                    .replace("{player}", player.getName())
                    .replace("{uuid}", player.getUniqueId().toString())
                    .replace("{gamemode}", gamemode)
                    .replace("{division}", division.name())
                    .replace("{level}", Integer.toString(division.level()))
                    .replace("{wins}", Long.toString(wins));
            commandDispatcher.accept(command.startsWith("/") ? command.substring(1) : command);
        }
    }

    public void executeCrossedRewards(Player player, String gamemode, long previousWins, long currentWins)
    {
        for (Division division : getCrossedDivisions(previousWins, currentWins))
        {
            executeRewards(player, gamemode, division, currentWins);
        }
    }

    private static ConfigurationSection copyAndLoad(JavaPlugin plugin)
    {
        File file = PluginFiles.advanced(plugin, "divisions.yml");

        YamlConfiguration configuration = new YamlConfiguration();
        try
        {
            configuration.load(file);
            return configuration;
        }
        catch (IOException | InvalidConfigurationException exception)
        {
            throw new DivisionConfigurationException("Could not load " + file.getAbsolutePath(), exception);
        }
    }

    private static List<Division> parse(ConfigurationSection configuration)
    {
        rejectUnknownKeys(configuration, ROOT_KEYS, "root");
        ConfigurationSection tiers = requireSection(configuration, "tiers");
        if (tiers.getKeys(false).isEmpty())
        {
            throw invalid("tiers must contain at least one tier");
        }

        List<Division> parsed = new ArrayList<>();
        Set<String> tierNames = new HashSet<>();
        long threshold = 0;
        for (String tierName : tiers.getKeys(false))
        {
            if (tierName.isBlank() || !tierNames.add(tierName.toLowerCase(Locale.ROOT)))
            {
                throw invalid("Tier names must be non-blank and unique (ignoring case): " + tierName);
            }
            String tierPath = "tiers." + tierName;
            ConfigurationSection tier = requireSection(tiers, tierName);
            rejectUnknownKeys(tier, TIER_KEYS, tierPath);
            long winsPerStep = requirePositiveWholeNumber(tier, "wins-per-step", tierPath);
            ConfigurationSection levels = requireSection(tier, "levels");
            if (levels.getKeys(false).isEmpty())
            {
                throw invalid(tierPath + ".levels must contain at least one level");
            }

            int previousLevel = 0;
            for (String levelKey : levels.getKeys(false))
            {
                int level = parseLevel(levelKey, tierPath);
                if (level <= previousLevel)
                {
                    throw invalid(tierPath + ".levels must be in strictly increasing numeric order");
                }
                previousLevel = level;
                String levelPath = tierPath + ".levels." + levelKey;
                ConfigurationSection levelSection = requireSection(levels, levelKey);
                rejectUnknownKeys(levelSection, LEVEL_KEYS, levelPath);
                List<String> rewards = readRewards(levelSection, levelPath);
                try
                {
                    threshold = Math.addExact(threshold, winsPerStep);
                }
                catch (ArithmeticException exception)
                {
                    throw new DivisionConfigurationException("Cumulative division threshold exceeds long range", exception);
                }
                parsed.add(new Division(tierName, level, threshold, rewards));
            }
        }
        return List.copyOf(parsed);
    }

    private static List<String> readRewards(ConfigurationSection section, String path)
    {
        if (!section.contains("rewards"))
        {
            return List.of();
        }
        Object value = section.get("rewards");
        if (!(value instanceof List<?> list))
        {
            throw invalid(path + ".rewards must be a list of console commands");
        }
        List<String> rewards = new ArrayList<>();
        for (Object item : list)
        {
            if (!(item instanceof String command) || command.isBlank())
            {
                throw invalid(path + ".rewards entries must be non-blank strings");
            }
            rewards.add(command);
        }
        return rewards;
    }

    private static int parseLevel(String key, String tierPath)
    {
        try
        {
            int level = Integer.parseInt(key);
            if (level <= 0)
            {
                throw invalid(tierPath + ".levels keys must be positive integers");
            }
            return level;
        }
        catch (NumberFormatException exception)
        {
            throw new DivisionConfigurationException(tierPath + ".levels key is not an integer: " + key, exception);
        }
    }

    private static long requirePositiveWholeNumber(ConfigurationSection section, String key, String path)
    {
        Object value = section.get(key);
        if (!(value instanceof Number number))
        {
            throw invalid(path + "." + key + " must be a positive whole number");
        }
        long result = number.longValue();
        if (result <= 0 || number.doubleValue() != (double) result)
        {
            throw invalid(path + "." + key + " must be a positive whole number");
        }
        return result;
    }

    private static ConfigurationSection requireSection(ConfigurationSection root, String path)
    {
        ConfigurationSection section = root.getConfigurationSection(path);
        if (section == null)
        {
            throw invalid(path + " must be a configuration section");
        }
        return section;
    }

    private static void rejectUnknownKeys(ConfigurationSection section, Set<String> allowed, String path)
    {
        for (String key : section.getKeys(false))
        {
            if (!allowed.contains(key))
            {
                throw invalid("Unknown key " + path + "." + key);
            }
        }
    }

    private static void requireValidWins(long wins)
    {
        if (wins < 0)
        {
            throw new IllegalArgumentException("Wins cannot be negative");
        }
    }

    private static DivisionConfigurationException invalid(String message)
    {
        return new DivisionConfigurationException("Invalid divisions.yml: " + message);
    }
}
