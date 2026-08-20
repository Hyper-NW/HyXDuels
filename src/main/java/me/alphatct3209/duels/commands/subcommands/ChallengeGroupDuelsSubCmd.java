package me.alphatct3209.duels.commands.subcommands;

import me.alphatct3209.duels.Duels;
import me.alphatct3209.duels.commands.subcommands.duel.AcceptChallengeDuelsSubCmd;
import me.alphatct3209.duels.commands.subcommands.duel.ChallengeDuelsSubCmd;
import me.alphatct3209.duels.commands.subcommands.duel.DenyChallengeDuelsSubCmd;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;

import java.util.Arrays;
import java.util.Locale;
import java.util.Map;

public final class ChallengeGroupDuelsSubCmd extends DuelsSubCommand
{
    private final Map<String, DuelsSubCommand> actions;

    public ChallengeGroupDuelsSubCmd(Duels plugin)
    {
        super(plugin, "challenge");
        DuelsSubCommand send = new ChallengeDuelsSubCmd(plugin);
        actions = Map.of("send", send, "player", send,
                "accept", new AcceptChallengeDuelsSubCmd(plugin),
                "deny", new DenyChallengeDuelsSubCmd(plugin));
    }

    @Override public boolean execute(CommandSender sender, String[] args)
    {
        if (args.length == 0 || args[0].equalsIgnoreCase("help"))
        {
            sender.sendMessage(ChatColor.DARK_AQUA + "HyXDuels Challenge Commands");
            sender.sendMessage(ChatColor.YELLOW + "/duels challenge send <player>");
            sender.sendMessage(ChatColor.YELLOW + "/duels challenge accept");
            sender.sendMessage(ChatColor.YELLOW + "/duels challenge deny");
            return true;
        }
        DuelsSubCommand action = actions.get(args[0].toLowerCase(Locale.ROOT));
        if (action == null) { incorrectArgs(sender, "/duels challenge help"); return true; }
        return action.execute(sender, Arrays.copyOfRange(args, 1, args.length));
    }
}
