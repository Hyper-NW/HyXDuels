package me.alphatct3209.duels.commands.subcommands;

import me.alphatct3209.duels.Duels;
import me.alphatct3209.duels.party.command.PartyCommand;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;

import java.util.Arrays;

public final class PartyDuelsSubCmd extends DuelsSubCommand
{
    private final PartyCommand command;

    public PartyDuelsSubCmd(Duels plugin)
    {
        super(plugin, "party");
        command = new PartyCommand(plugin);
    }

    @Override public boolean execute(CommandSender sender, String[] args)
    {
        if (args.length == 0 || args[0].equalsIgnoreCase("help"))
        {
            sender.sendMessage(ChatColor.DARK_AQUA + "HyXDuels Party Commands");
            sender.sendMessage(ChatColor.YELLOW
                    + "/duels party invite|kick|promote|demote|transfer <player>");
            sender.sendMessage(ChatColor.YELLOW
                    + "/duels party accept|deny|leave|disband|list|menu");
            return true;
        }
        return command.onCommand(sender, null, "duels party", Arrays.copyOf(args, args.length));
    }
}
