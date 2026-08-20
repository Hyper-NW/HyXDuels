package me.alphatct3209.duels.commands.subcommands;

import me.alphatct3209.duels.Duels;
import me.alphatct3209.duels.commands.subcommands.stats.GetStatsDuelsSubCmd;
import me.alphatct3209.duels.commands.subcommands.stats.LeaderboardDuelsSubCmd;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;

import java.util.Arrays;
import java.util.Locale;
import java.util.Map;

public final class StatsDuelsSubCmd extends DuelsSubCommand
{
    private final Map<String, DuelsSubCommand> actions;

    public StatsDuelsSubCmd(Duels plugin)
    {
        super(plugin, "stats");
        DuelsSubCommand view = new GetStatsDuelsSubCmd(plugin);
        DuelsSubCommand leaderboard = new LeaderboardDuelsSubCmd(plugin);
        actions = Map.of("view", view, "get", view, "leaderboard", leaderboard,
                "top", leaderboard, "lb", leaderboard);
    }

    @Override public boolean execute(CommandSender sender, String[] args)
    {
        if (args.length == 0 || args[0].equalsIgnoreCase("help"))
        {
            sender.sendMessage(ChatColor.DARK_AQUA + "HyXDuels Statistics Commands");
            sender.sendMessage(ChatColor.YELLOW + "/duels stats view [player] [mode]");
            sender.sendMessage(ChatColor.YELLOW
                    + "/duels stats leaderboard [wins|kills|divisions <mode>]");
            return true;
        }
        DuelsSubCommand action = actions.get(args[0].toLowerCase(Locale.ROOT));
        if (action == null) { incorrectArgs(sender, "/duels stats help"); return true; }
        return action.execute(sender, Arrays.copyOfRange(args, 1, args.length));
    }
}
