package me.alphatct3209.duels.commands.subcommands.duel;

import me.alphatct3209.duels.Duels;
import me.alphatct3209.duels.commands.subcommands.DuelsSubCommand;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public final class DenyChallengeDuelsSubCmd extends DuelsSubCommand
{
    public DenyChallengeDuelsSubCmd(Duels plugin)
    {
        super(plugin, "deny");
    }

    @Override
    public boolean execute(CommandSender sender, String[] args)
    {
        if (!(sender instanceof Player player))
        {
            sender.sendMessage(ChatColor.RED + "You must be a Player to do that!");
            return true;
        }
        if (args.length != 0)
        {
            incorrectArgs(sender, "/duels challenge deny");
            return true;
        }
        plugin.getChallengeManager().deny(player);
        return true;
    }
}
