package me.alphatct3209.duels.commands;

import me.alphatct3209.duels.Duels;
import me.alphatct3209.duels.game.kits.Kit;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class DuelTabCompleter implements TabCompleter
{
    private final Duels plugin;

    public DuelTabCompleter(Duels plugin)
    {
        this.plugin = plugin;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command cmd, String label, String[] args)
    {
        List<String> recommendations = new ArrayList<>();
        if (args.length > 0 && args[0].equalsIgnoreCase("hologram")
                && !sender.hasPermission("duels.holograms.admin"))
        {
            return null;
        }

        if (args.length == 1)
        {
            recommendations.addAll(List.of("help", "menu", "join", "leave", "challenge", "accept", "deny",
                    "listarenas", "createarena", "setlobby", "setspawn1", "setspawn2",
                    "finisharena", "kits", "modes", "arenamodes", "arenakits", "arenasettings",
                    "arenapoint", "arenaobjective", "loadoldstats", "stats", "top"));
            if (sender.hasPermission("duels.holograms.admin"))
            {
                recommendations.add("hologram");
            }
            addVisiblePlayers(sender, recommendations);
        }
        else if (args.length == 2)
        {
            if (args[0].equalsIgnoreCase("challenge"))
            {
                addVisiblePlayers(sender, recommendations);
            }
            else if (args[0].equalsIgnoreCase("kits"))
            {
                recommendations.addAll(List.of("list", "create", "delete", "select"));
            }
            else if (args[0].equalsIgnoreCase("modes"))
            {
                recommendations.addAll(List.of("list", "select"));
            }
            else if (args[0].equalsIgnoreCase("hologram")
                    && sender.hasPermission("duels.holograms.admin"))
            {
                recommendations.addAll(List.of("status", "list", "create", "move", "delete", "reload"));
            }
            else if (isLeaderboard(args[0]))
            {
                recommendations.addAll(List.of("wins", "kills", "divisions"));
            }
            else if (args[0].equalsIgnoreCase("stats"))
            {
                Bukkit.getOnlinePlayers().forEach(player -> recommendations.add(player.getName()));
                addModes(recommendations);
            }
            else if (args[0].equalsIgnoreCase("arenamodes") || args[0].equalsIgnoreCase("arenakits")
                    || args[0].equalsIgnoreCase("arenasettings")
                    || args[0].equalsIgnoreCase("arenapoint")
                    || args[0].equalsIgnoreCase("arenaobjective"))
            {
                plugin.getArenaManager().getArenaList().forEach(arena -> recommendations.add(Integer.toString(arena.getId())));
            }
        }
        else if (args.length == 3)
        {
            if (args[0].equalsIgnoreCase("hologram")
                    && (args[1].equalsIgnoreCase("move") || args[1].equalsIgnoreCase("delete")))
            {
                recommendations.addAll(plugin.getHologramManager().config().definitions().keySet());
            }
            else if (args[0].equalsIgnoreCase("modes") && args[1].equalsIgnoreCase("select"))
            {
                addModes(recommendations);
            }
            else if (args[0].equalsIgnoreCase("createarena"))
            {
                recommendations.addAll(List.of("true", "false"));
            }
            else if (args[0].equalsIgnoreCase("arenamodes") || args[0].equalsIgnoreCase("arenakits"))
            {
                recommendations.addAll(List.of("list", "add", "remove", "clear"));
            }
            else if (args[0].equalsIgnoreCase("arenapoint"))
            {
                recommendations.addAll(List.of("list", "set", "remove"));
            }
            else if (args[0].equalsIgnoreCase("arenaobjective"))
            {
                recommendations.addAll(List.of("list", "set", "remove"));
            }
            else if (args[0].equalsIgnoreCase("arenasettings"))
            {
                recommendations.add("list");
                for (me.alphatct3209.duels.game.arenas.ArenaSettings.Flag flag
                        : me.alphatct3209.duels.game.arenas.ArenaSettings.Flag.values())
                {
                    recommendations.add(flag.key());
                }
            }
            else if (args[0].equalsIgnoreCase("kits")
                    && (args[1].equalsIgnoreCase("create") || args[1].equalsIgnoreCase("delete")
                    || args[1].equalsIgnoreCase("select")))
            {
                plugin.getKitManager().getKitList().stream()
                        .map(Kit::getName)
                        .forEach(recommendations::add);
            }
            else if (isLeaderboard(args[0]) && args[1].equalsIgnoreCase("divisions"))
            {
                addModes(recommendations);
            }
            else if (args[0].equalsIgnoreCase("stats"))
            {
                addModes(recommendations);
            }
        }
        else if (args.length == 4)
        {
            if (args[0].equalsIgnoreCase("hologram") && args[1].equalsIgnoreCase("create"))
            {
                recommendations.addAll(List.of("wins", "kills", "divisions"));
            }
            else if (args[0].equalsIgnoreCase("modes") && args[1].equalsIgnoreCase("select"))
            {
                plugin.getModeManager().resolve(args[2]).ifPresent(mode -> mode.allowedKitKeys().forEach(key -> {
                    Kit kit = plugin.getKitManager().getKitByCanonicalKey(key);
                    recommendations.add(kit == null ? key : kit.getName());
                }));
            }
            else if ((args[0].equalsIgnoreCase("arenamodes") || args[0].equalsIgnoreCase("arenakits"))
                    && (args[2].equalsIgnoreCase("add") || args[2].equalsIgnoreCase("remove")))
            {
                addModes(recommendations);
            }
            else if (args[0].equalsIgnoreCase("arenasettings")
                    && me.alphatct3209.duels.game.arenas.ArenaSettings.Flag.parse(args[2]).isPresent())
            {
                recommendations.addAll(List.of("true", "false"));
            }
            else if (args[0].equalsIgnoreCase("createarena"))
            {
                recommendations.addAll(List.of("true", "false"));
            }
        }
        else if (args.length == 5 && args[0].equalsIgnoreCase("hologram")
                && args[1].equalsIgnoreCase("create") && args[3].equalsIgnoreCase("divisions"))
        {
            addModes(recommendations);
        }

        if (recommendations.isEmpty())
        {
            return null;
        }
        String prefix = args[args.length - 1].toLowerCase(Locale.ROOT);
        return recommendations.stream()
                .distinct()
                .filter(value -> value.toLowerCase(Locale.ROOT).startsWith(prefix))
                .toList();
    }

    private void addVisiblePlayers(CommandSender sender, List<String> recommendations)
    {
        if (sender instanceof org.bukkit.entity.Player viewer)
        {
            Bukkit.getOnlinePlayers().stream()
                    .filter(player -> !player.equals(viewer))
                    .filter(viewer::canSee)
                    .map(org.bukkit.entity.Player::getName)
                    .forEach(recommendations::add);
        }
    }

    private void addModes(List<String> recommendations)
    {
        plugin.getModeManager().enabledModes().forEach(mode -> {
            recommendations.add(mode.key().value());
            recommendations.addAll(mode.aliases());
        });
    }

    private boolean isLeaderboard(String value)
    {
        return value.equalsIgnoreCase("top") || value.equalsIgnoreCase("leaderboard")
                || value.equalsIgnoreCase("lb");
    }
}
