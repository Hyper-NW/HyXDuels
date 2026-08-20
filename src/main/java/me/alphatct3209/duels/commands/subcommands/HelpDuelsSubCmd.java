package me.alphatct3209.duels.commands.subcommands;

import me.alphatct3209.duels.Duels;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;

public class HelpDuelsSubCmd extends DuelsSubCommand
{

    public HelpDuelsSubCmd(Duels plugin)
    {
        super(plugin, "help");
    }

    @Override
    public boolean execute(CommandSender sender, String[] args)
    {
        if(!sender.hasPermission("duels.help"))
        {
            noPerm(sender);
            return true;
        }
        if(args.length == 0)
        {
            me.alphatct3209.duels.utils.MessageService.send(sender, plugin.getConfig(),
                    "Messages.Help-Menu", java.util.Map.of());
            return true;
        }
        else
        {
            incorrectArgs(sender, "/duels help");
            return true;
        }
    }
}
