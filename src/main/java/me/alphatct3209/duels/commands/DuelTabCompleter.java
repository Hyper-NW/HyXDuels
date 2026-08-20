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
        if (args.length == 1)
        {
            recommendations.addAll(List.of("help", "queue", "challenge", "kits", "modes",
                    "stats", "party", "social"));
            if (canManageArenas(sender)) recommendations.add("arena");
            if (sender.hasPermission("duels.holograms.admin")) recommendations.add("hologram");
            if (sender.hasPermission("duels.admin")) recommendations.add("admin");
        }
        else if (args.length == 2)
        {
            if (matches(args[0], "help")) addHelpPages(recommendations);
            else if (matches(args[0], "queue"))
                recommendations.addAll(List.of("help", "open", "join", "leave"));
            else if (matches(args[0], "challenge"))
                recommendations.addAll(List.of("help", "send", "accept", "deny"));
            else if (matches(args[0], "kits", "kit"))
                recommendations.addAll(List.of("help", "list", "create", "delete", "select", "editor"));
            else if (matches(args[0], "modes")) recommendations.addAll(List.of("list", "select"));
            else if (matches(args[0], "arena") && canManageArenas(sender))
                recommendations.addAll(List.of("help", "list", "create", "setlobby", "setspawn1",
                        "setspawn2", "finish", "modes", "settings", "point", "objective"));
            else if (args[0].equalsIgnoreCase("hologram")
                    && sender.hasPermission("duels.holograms.admin"))
                recommendations.addAll(List.of("status", "list", "create", "move", "delete", "reload"));
            else if (matches(args[0], "stats"))
                recommendations.addAll(List.of("help", "view", "leaderboard"));
            else if (matches(args[0], "party"))
                recommendations.addAll(List.of("help", "invite", "kick", "promote", "demote",
                        "transfer", "accept", "deny", "leave", "disband", "list", "menu"));
            else if (matches(args[0], "social"))
                recommendations.addAll(List.of("help", "friends", "message", "settings"));
            else if (matches(args[0], "admin") && sender.hasPermission("duels.admin"))
                recommendations.addAll(List.of("help", "update", "load-old-stats", "file-to-sql"));
        }
        else if (args.length == 3)
        {
            if (matches(args[0], "queue") && matches(args[1], "join"))
                addArenaIds(recommendations);
            else if (matches(args[0], "challenge") && matches(args[1], "send"))
                addVisiblePlayers(sender, recommendations);
            else if (matches(args[0], "arena") && matches(args[1],
                    "modes", "kits", "settings", "point", "objective"))
                addArenaIds(recommendations);
            else if (args[0].equalsIgnoreCase("hologram")
                    && (args[1].equalsIgnoreCase("move") || args[1].equalsIgnoreCase("delete")))
                recommendations.addAll(plugin.getHologramManager().config().definitions().keySet());
            else if (args[0].equalsIgnoreCase("modes") && args[1].equalsIgnoreCase("select"))
                addModes(recommendations);
            else if (matches(args[0], "kits", "kit")
                    && matches(args[1], "delete", "select", "editor", "edit")) addKits(recommendations);
            else if (matches(args[0], "stats") && matches(args[1], "view"))
            {
                addVisiblePlayers(sender, recommendations);
                addModes(recommendations);
            }
            else if (matches(args[0], "stats") && matches(args[1], "leaderboard"))
                recommendations.addAll(List.of("wins", "kills", "divisions"));
            else if (matches(args[0], "party")
                    && matches(args[1], "invite", "kick", "promote", "demote", "transfer"))
                addVisiblePlayers(sender, recommendations);
            else if (matches(args[0], "social") && matches(args[1], "friends"))
                recommendations.addAll(List.of("add", "accept", "deny", "remove", "best", "list"));
            else if (matches(args[0], "social") && matches(args[1], "message"))
                addVisiblePlayers(sender, recommendations);
        }
        else if (args.length == 4)
        {
            if (matches(args[0], "arena") && matches(args[1], "create"))
                recommendations.addAll(List.of("true", "false"));
            else if (matches(args[0], "arena") && matches(args[1], "modes", "kits"))
                recommendations.addAll(List.of("list", "add", "remove", "clear"));
            else if (matches(args[0], "arena") && matches(args[1], "point", "objective"))
                recommendations.addAll(List.of("list", "set", "remove"));
            else if (matches(args[0], "arena") && matches(args[1], "settings"))
            {
                recommendations.add("list");
                addArenaFlags(recommendations);
            }
            else if (args[0].equalsIgnoreCase("hologram") && args[1].equalsIgnoreCase("create"))
                recommendations.addAll(List.of("wins", "kills", "divisions"));
            else if (args[0].equalsIgnoreCase("modes") && args[1].equalsIgnoreCase("select"))
                addAllowedKits(args[2], recommendations);
            else if (matches(args[0], "stats") && matches(args[1], "view"))
                addModes(recommendations);
            else if (matches(args[0], "stats") && matches(args[1], "leaderboard")
                    && matches(args[2], "divisions")) addModes(recommendations);
            else if (matches(args[0], "social") && matches(args[1], "friends")
                    && matches(args[2], "add", "accept", "deny", "remove", "best"))
                addVisiblePlayers(sender, recommendations);
        }
        else if (args.length == 5)
        {
            if (matches(args[0], "arena") && matches(args[1], "create"))
                recommendations.addAll(List.of("true", "false"));
            else if (matches(args[0], "arena") && matches(args[1], "modes", "kits")
                    && matches(args[3], "add", "remove")) addModes(recommendations);
            else if (matches(args[0], "arena") && matches(args[1], "settings")
                    && me.alphatct3209.duels.game.arenas.ArenaSettings.Flag.parse(args[3]).isPresent())
                recommendations.addAll(List.of("true", "false"));
            else if (args[0].equalsIgnoreCase("hologram")
                    && args[1].equalsIgnoreCase("create") && args[3].equalsIgnoreCase("divisions"))
                addModes(recommendations);
        }

        if (recommendations.isEmpty())
        {
            return List.of();
        }
        String prefix = args[args.length - 1].toLowerCase(Locale.ROOT);
        return recommendations.stream()
                .distinct()
                .filter(value -> value.toLowerCase(Locale.ROOT).startsWith(prefix))
                .toList();
    }

    private boolean canManageArenas(CommandSender sender)
    {
        return sender.hasPermission("duels.admin") || sender.hasPermission("duels.createarena")
                || sender.hasPermission("duels.listarenas") || sender.hasPermission("duels.arenamodes")
                || sender.hasPermission("duels.arenasettings") || sender.hasPermission("duels.arenapoint")
                || sender.hasPermission("duels.arenaobjective");
    }

    private void addHelpPages(List<String> recommendations)
    {
        int entries = plugin.getConfig().getStringList("Messages.Help-Menu").size();
        int pageSize = Math.max(3, Math.min(10,
                plugin.getConfig().getInt("Messages.Help-Page-Size", 6)));
        int pages = Math.max(1, (entries + pageSize - 1) / pageSize);
        for (int page = 1; page <= pages; page++) recommendations.add(Integer.toString(page));
    }

    private void addArenaIds(List<String> recommendations)
    {
        plugin.getArenaManager().getArenaList().forEach(
                arena -> recommendations.add(Integer.toString(arena.getId())));
    }

    private void addKits(List<String> recommendations)
    {
        plugin.getKitManager().getKitList().stream().map(Kit::getName).forEach(recommendations::add);
    }

    private void addAllowedKits(String modeName, List<String> recommendations)
    {
        plugin.getModeManager().resolve(modeName).ifPresent(mode -> mode.allowedKitKeys().forEach(key -> {
            Kit kit = plugin.getKitManager().getKitByCanonicalKey(key);
            recommendations.add(kit == null ? key : kit.getName());
        }));
    }

    private void addArenaFlags(List<String> recommendations)
    {
        for (me.alphatct3209.duels.game.arenas.ArenaSettings.Flag flag
                : me.alphatct3209.duels.game.arenas.ArenaSettings.Flag.values())
            recommendations.add(flag.key());
    }

    private boolean matches(String input, String... values)
    {
        for (String value : values) if (input.equalsIgnoreCase(value)) return true;
        return false;
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

}
