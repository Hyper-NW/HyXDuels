package me.alphatct3209.duels.commands.subcommands;

import me.alphatct3209.duels.Duels;
import me.alphatct3209.duels.commands.subcommands.stats.LoadStatsDuelsSubCmd;
import me.alphatct3209.duels.commands.subcommands.stats.StatsFileToSQLCmd;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;

import java.util.Arrays;
import java.util.Locale;
import java.util.Map;

public final class AdminDuelsSubCmd extends DuelsSubCommand
{
    private final Map<String, DuelsSubCommand> actions;

    public AdminDuelsSubCmd(Duels plugin)
    {
        super(plugin, "admin");
        actions = Map.of("update", new UpdateDuelsSubCommand(plugin),
                "load-old-stats", new LoadStatsDuelsSubCmd(plugin),
                "file-to-sql", new StatsFileToSQLCmd(plugin));
    }

    @Override public boolean execute(CommandSender sender, String[] args)
    {
        if (!sender.hasPermission("duels.admin")) { noPerm(sender); return true; }
        if (args.length == 0 || args[0].equalsIgnoreCase("help"))
        {
            sender.sendMessage(ChatColor.DARK_AQUA + "HyXDuels Administration Commands");
            sender.sendMessage(ChatColor.YELLOW + "/duels admin update");
            sender.sendMessage(ChatColor.YELLOW + "/duels admin load-old-stats");
            sender.sendMessage(ChatColor.YELLOW + "/duels admin file-to-sql");
            return true;
        }
        DuelsSubCommand action = actions.get(args[0].toLowerCase(Locale.ROOT));
        if (action == null) { incorrectArgs(sender, "/duels admin help"); return true; }
        return action.execute(sender, Arrays.copyOfRange(args, 1, args.length));
    }
}
