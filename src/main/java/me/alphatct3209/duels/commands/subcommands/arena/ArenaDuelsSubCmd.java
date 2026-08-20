package me.alphatct3209.duels.commands.subcommands.arena;

import me.alphatct3209.duels.Duels;
import me.alphatct3209.duels.commands.subcommands.DuelsSubCommand;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;

import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Map;

/** Structured `/duels arena <subcommand>` dispatcher. */
public final class ArenaDuelsSubCmd extends DuelsSubCommand
{
    private final Map<String, DuelsSubCommand> children = new LinkedHashMap<>();

    public ArenaDuelsSubCmd(Duels plugin)
    {
        super(plugin, "arena");
        children.put("list", new ListArenasDuelsSubCmd(plugin));
        children.put("create", new CreateArenaDuelsSubCmd(plugin));
        children.put("setlobby", new SetLobbyDuelsSubCmd(plugin));
        children.put("setspawn1", new SetSpawn1DuelsSubCmd(plugin));
        children.put("setspawn2", new SetSpawn2DuelsSubCmd(plugin));
        children.put("finish", new FinishArenaDuelsSubCmd(plugin));
        DuelsSubCommand modes = new ArenaKitsDuelsSubCmd(plugin);
        children.put("modes", modes);
        children.put("kits", modes);
        children.put("settings", new ArenaSettingsDuelsSubCmd(plugin));
        children.put("point", new ArenaPointDuelsSubCmd(plugin));
        children.put("objective", new ArenaObjectiveDuelsSubCmd(plugin));
    }

    @Override
    public boolean execute(CommandSender sender, String[] args)
    {
        if (args.length == 0 || args[0].equalsIgnoreCase("help"))
        {
            help(sender);
            return true;
        }
        DuelsSubCommand child = children.get(canonicalChild(args[0]));
        if (child == null)
        {
            incorrectArgs(sender, "/duels arena help");
            return true;
        }
        return child.execute(sender, Arrays.copyOfRange(args, 1, args.length));
    }

    static String canonicalChild(String input)
    {
        return switch (input.toLowerCase(java.util.Locale.ROOT))
        {
            case "ls", "listarenas" -> "list";
            case "new", "createarena" -> "create";
            case "finisharena", "save" -> "finish";
            case "arenamodes", "arenakits" -> "modes";
            case "arenasettings" -> "settings";
            case "arenapoint", "points" -> "point";
            case "arenaobjective", "objectives" -> "objective";
            default -> input.toLowerCase(java.util.Locale.ROOT);
        };
    }

    private void help(CommandSender sender)
    {
        sender.sendMessage(ChatColor.DARK_AQUA + "HyXDuels Arena Commands");
        sender.sendMessage(ChatColor.YELLOW + "/duels arena list" + ChatColor.GRAY + " - list arenas");
        sender.sendMessage(ChatColor.YELLOW + "/duels arena create <name> [break] [place]");
        sender.sendMessage(ChatColor.YELLOW + "/duels arena setlobby|setspawn1|setspawn2|finish");
        sender.sendMessage(ChatColor.YELLOW + "/duels arena modes <id> list|add|remove|clear [mode]");
        sender.sendMessage(ChatColor.YELLOW + "/duels arena settings|point|objective <id> ...");
    }
}
