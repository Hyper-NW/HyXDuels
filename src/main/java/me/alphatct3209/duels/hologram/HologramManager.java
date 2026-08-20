package me.alphatct3209.duels.hologram;

import me.alphatct3209.duels.Duels;
import me.alphatct3209.duels.configuration.PluginFiles;
import me.alphatct3209.duels.game.modes.DuelMode;
import me.alphatct3209.duels.hologram.integration.HologramIntegration;
import me.alphatct3209.duels.hologram.integration.HologramIntegrationFactory;
import me.alphatct3209.duels.hologram.integration.RuntimeHologram;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;

/** Owns holograms.yml and isolates all optional integration failures from HyXDuels. */
public final class HologramManager
{
    private static final String FILE_NAME = "holograms.yml";

    private final Duels plugin;
    private final File file;
    private HologramConfig config = new HologramConfig(false, 100, Map.of());
    private HologramIntegration integration;
    private Set<String> foreignConflicts = Set.of();
    private String lastError;

    public HologramManager(Duels plugin)
    {
        this.plugin = plugin;
        this.file = PluginFiles.advanced(plugin, FILE_NAME);
    }

    public boolean reload()
    {
        final HologramConfig candidate;
        try
        {
            candidate = HologramConfigParser.parse(toMap(loadYaml()));
            validateRuntime(candidate);
        }
        catch (RuntimeException exception)
        {
            fail("Invalid holograms.yml: " + exception.getMessage(), exception);
            return false;
        }
        config = candidate;
        lastError = null;
        foreignConflicts = Set.of();

        if (!candidate.enabled())
        {
            disableIntegration();
            return true;
        }
        if (!papiEnabled() || !decentHologramsEnabled())
        {
            disableIntegration();
            return true;
        }

        try
        {
            if (integration == null)
            {
                // This is the only path that loads/instantiates the DH-linked implementation.
                integration = HologramIntegrationFactory.createDecentHolograms(plugin,
                        (player, ignored) -> plugin.getLeaderboardFilterGui().open(player));
            }
            List<RuntimeHologram> desired = new ArrayList<>();
            for (HologramDefinition definition : candidate.definitions().values())
            {
                desired.add(runtime(definition));
            }
            HologramIntegration.ReconcileResult result = integration.reconcile(desired);
            foreignConflicts = result.foreignConflicts();
            for (String name : foreignConflicts)
            {
                plugin.getLogger().warning("Managed hologram name '" + name
                        + "' already belongs to another runtime; HyXDuels left it untouched.");
            }
            return true;
        }
        catch (RuntimeException | LinkageError failure)
        {
            fail("DecentHolograms integration failed and was disabled; HyXDuels remains enabled.", failure);
            return false;
        }
    }

    public void shutdown()
    {
        disableIntegration();
    }

    public HologramConfig config()
    {
        return config;
    }

    public Status status()
    {
        int owned = 0;
        if (integration != null)
        {
            try
            {
                owned = integration.ownedCount();
            }
            catch (RuntimeException | LinkageError failure)
            {
                fail("DecentHolograms integration status failed and was disabled.", failure);
            }
        }
        return new Status(config.enabled(), papiEnabled(), decentHologramsEnabled(),
                integration != null, config.definitions().size(), owned, foreignConflicts, lastError);
    }

    public void create(Player player, String rawId, HologramDefinition.Type type, String rawMode)
    {
        String id = rawId.toLowerCase(Locale.ROOT);
        if (config.definitions().containsKey(id))
        {
            throw new IllegalArgumentException("managed id '" + id + "' already exists");
        }
        String mode = null;
        if (type == HologramDefinition.Type.DIVISIONS)
        {
            DuelMode duelMode = plugin.getModeManager().resolve(rawMode).orElse(null);
            if (duelMode == null)
            {
                throw new IllegalArgumentException("unknown mode '" + rawMode + "'");
            }
            mode = duelMode.key().value();
        }
        Location location = player.getLocation();
        requireStable(location.getWorld());
        String name = "hyxduels_" + id;
        for (HologramDefinition existing : config.definitions().values())
        {
            if (existing.name().equalsIgnoreCase(name))
            {
                throw new IllegalArgumentException("hologram name '" + name + "' is already configured");
            }
        }
        HologramDefinition definition = new HologramDefinition(id, name, type, mode,
                position(location), config.defaultUpdateIntervalTicks(), generatedLines(type, mode));
        persist(definition);
        reloadOrThrow();
    }

    public void move(Player player, String rawId)
    {
        HologramDefinition existing = requireDefinition(rawId);
        Location location = player.getLocation();
        requireStable(location.getWorld());
        persist(new HologramDefinition(existing.id(), existing.name(), existing.type(),
                existing.gamemode(), position(location), existing.updateIntervalTicks(), existing.lines()));
        reloadOrThrow();
    }

    public void delete(String rawId)
    {
        HologramDefinition existing = requireDefinition(rawId);
        YamlConfiguration yaml = loadYaml();
        yaml.set("Managed." + existing.id(), null);
        save(yaml);
        reloadOrThrow();
    }

    private void persist(HologramDefinition definition)
    {
        String base = "Managed." + definition.id();
        YamlConfiguration yaml = loadYaml();
        yaml.set(base + ".Name", definition.name());
        yaml.set(base + ".Type", definition.type().configValue());
        yaml.set(base + ".Gamemode", definition.gamemode());
        yaml.set(base + ".Update-Interval-Ticks", definition.updateIntervalTicks());
        yaml.set(base + ".Location.World", definition.location().world());
        yaml.set(base + ".Location.X", definition.location().x());
        yaml.set(base + ".Location.Y", definition.location().y());
        yaml.set(base + ".Location.Z", definition.location().z());
        yaml.set(base + ".Location.Yaw", definition.location().yaw());
        yaml.set(base + ".Location.Pitch", definition.location().pitch());
        yaml.set(base + ".Lines", definition.lines());
        save(yaml);
    }

    private void reloadOrThrow()
    {
        if (!reload())
        {
            throw new IllegalStateException(lastError == null ? "hologram reload failed" : lastError);
        }
    }

    private HologramDefinition requireDefinition(String rawId)
    {
        HologramDefinition definition = config.definitions().get(rawId.toLowerCase(Locale.ROOT));
        if (definition == null)
        {
            throw new IllegalArgumentException("unknown managed hologram id '" + rawId + "'");
        }
        return definition;
    }

    private void validateRuntime(HologramConfig candidate)
    {
        for (HologramDefinition definition : candidate.definitions().values())
        {
            World world = Bukkit.getWorld(definition.location().world());
            if (world == null)
            {
                throw new IllegalArgumentException("Managed." + definition.id()
                        + ".Location.World is not loaded: '" + definition.location().world() + "'");
            }
            requireStable(world);
            if (definition.type() == HologramDefinition.Type.DIVISIONS
                    && plugin.getModeManager().resolve(definition.gamemode()).isEmpty())
            {
                throw new IllegalArgumentException("Managed." + definition.id()
                        + ".Gamemode is not a configured kit mode: '" + definition.gamemode() + "'");
            }
        }
    }

    private void requireStable(World world)
    {
        if (world == null)
        {
            throw new IllegalArgumentException("location world is unavailable");
        }
        if (plugin.getSlimeWorldManager().isManagedWorld(world.getName()))
        {
            throw new IllegalArgumentException("world '" + world.getName()
                    + "' is an ASP-managed arena/lobby world and is not stable for holograms");
        }
    }

    private RuntimeHologram runtime(HologramDefinition definition)
    {
        HologramLocation value = definition.location();
        World world = Bukkit.getWorld(value.world());
        if (world == null)
        {
            throw new IllegalArgumentException("world '" + value.world() + "' is not loaded");
        }
        return new RuntimeHologram(definition.name(),
                new Location(world, value.x(), value.y(), value.z(), value.yaw(), value.pitch()),
                definition.updateIntervalTicks(), definition.lines());
    }

    private static HologramLocation position(Location location)
    {
        return new HologramLocation(location.getWorld().getName(), location.getX(), location.getY(),
                location.getZ(), location.getYaw(), location.getPitch());
    }

    public static List<String> generatedLines(HologramDefinition.Type type, String mode)
    {
        List<String> lines = new ArrayList<>();
        String typeKey = switch (type)
        {
            case WINS -> "wins";
            case KILLS -> "kills";
            case DIVISIONS -> "divisions";
        };
        String fallback = mode == null ? "all" : mode;
        switch (type)
        {
            case WINS -> lines.add("&6&lTop Duel Wins");
            case KILLS -> lines.add("&6&lTop Duel Kills");
            case DIVISIONS -> lines.add("&6&lTop " + mode + " Divisions");
        }
        lines.add("&7%duels_flb_" + typeKey + "_" + fallback + "_filter%");
        for (int rank = 1; rank <= 10; rank++)
        {
            String prefix = "&e" + rank + ". &f";
            switch (type)
            {
                case WINS, KILLS -> lines.add(prefix + "%duels_flb_" + typeKey + "_" + fallback
                        + "_" + rank + "_player% &7- &6%duels_flb_" + typeKey + "_" + fallback
                        + "_" + rank + "_value%");
                case DIVISIONS -> lines.add(prefix + "%duels_flb_divisions_" + fallback + "_" + rank
                        + "_player% &7- &6%duels_flb_divisions_" + fallback + "_" + rank
                        + "_division% &7(%duels_flb_divisions_" + fallback + "_" + rank + "_wins%)");
            }
        }
        lines.add("&eRight-click to filter!");
        return List.copyOf(lines);
    }

    private boolean papiEnabled()
    {
        return Bukkit.getPluginManager().isPluginEnabled("PlaceholderAPI");
    }

    private boolean decentHologramsEnabled()
    {
        return Bukkit.getPluginManager().isPluginEnabled("DecentHolograms");
    }

    private void fail(String message, Throwable failure)
    {
        lastError = message;
        plugin.getLogger().log(Level.WARNING, message, failure);
        disableIntegration();
    }

    private void disableIntegration()
    {
        if (integration != null)
        {
            try
            {
                integration.shutdown();
            }
            catch (RuntimeException | LinkageError failure)
            {
                plugin.getLogger().log(Level.WARNING,
                        "Could not completely clean up owned DecentHolograms objects.", failure);
            }
            integration = null;
        }
        foreignConflicts = Set.of();
    }

    private YamlConfiguration loadYaml()
    {
        return YamlConfiguration.loadConfiguration(file);
    }

    private void save(YamlConfiguration yaml)
    {
        try
        {
            yaml.save(file);
        }
        catch (IOException exception)
        {
            throw new IllegalStateException("could not save " + FILE_NAME, exception);
        }
    }

    private static Map<String, Object> toMap(ConfigurationSection section)
    {
        Map<String, Object> result = new LinkedHashMap<>();
        for (String key : section.getKeys(false))
        {
            Object value = section.get(key);
            result.put(key, value instanceof ConfigurationSection nested ? toMap(nested) : value);
        }
        return result;
    }

    public record Status(boolean globallyEnabled, boolean placeholderApiEnabled,
                         boolean decentHologramsEnabled, boolean integrationActive,
                         int configured, int owned, Set<String> foreignConflicts, String lastError) {}
}
