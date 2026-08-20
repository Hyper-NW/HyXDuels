package me.alphatct3209.duels.commands.subcommands.duel;

import me.alphatct3209.duels.Duels;
import me.alphatct3209.duels.commands.subcommands.DuelsSubCommand;
import me.alphatct3209.duels.game.GameState;
import me.alphatct3209.duels.game.arenas.Arena;
import me.alphatct3209.duels.utils.PlayerRestoration;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.Objects;

public class LeaveDuelsSubCmd extends DuelsSubCommand
{
    public LeaveDuelsSubCmd(Duels plugin)
    {
        super(plugin, "leave");
    }

    @Override
    public boolean execute(CommandSender sender, String[] args)
    {
        if(!sender.hasPermission("duels.leave"))
        {
            noPerm(sender);
            return true;
        }
        if(sender instanceof Player player)
        {
            if(args.length == 0)
            {
                if (plugin.getQueueManager().isQueued(player.getUniqueId()))
                {
                    plugin.getQueueManager().leave(player);
                    return true;
                }
                Arena arena = plugin.getArenaManager().getArena(player);
                if(arena == null)
                {
                    me.alphatct3209.duels.utils.MessageService.send(player, plugin.getConfig(),
                            "Messages.Not-In-Duel", java.util.Map.of(),
                            "&cYou are not currently in a duel or queue!");
                    return true;
                }
                else
                {
                    if(arena.getGameState() == GameState.PLAYING)
                    {
                        arena.getGame().kill(player);
                    }
                    else
                    {
                        arena.removePlayer(player);
                        PlayerRestoration.restorePlayer(player, false);
                    }
                }
                return true;
            }
            else
            {
                incorrectArgs(sender, "/duels leave");
                return true;
            }
        }
        else
        {
            sender.sendMessage(ChatColor.RED + "You must be a Player to do that!");
            return false;
        }
    }
}
