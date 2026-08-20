package me.alphatct3209.duels.commands.subcommands.duel;

import me.alphatct3209.duels.Duels;
import me.alphatct3209.duels.commands.subcommands.DuelsSubCommand;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public final class ChallengeDuelsSubCmd extends DuelsSubCommand
{
    public ChallengeDuelsSubCmd(Duels plugin)
    {
        super(plugin, "challenge");
    }

    @Override
    public boolean execute(CommandSender sender, String[] args)
    {
        if (!(sender instanceof Player player))
        {
            sender.sendMessage(ChatColor.RED + "You must be a Player to do that!");
            return true;
        }
        if (args.length != 1)
        {
            incorrectArgs(sender, "/duels challenge send <player>");
            return true;
        }
        plugin.getChallengeManager().openSelection(player,
                plugin.getChallengeManager().findExactOnline(args[0]));
        return true;
    }
}
