package me.alphatct3209.duels.commands.subcommands.arena;

import me.alphatct3209.duels.Duels;
import me.alphatct3209.duels.commands.subcommands.DuelsSubCommand;
import me.alphatct3209.duels.game.arenas.Arena;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;

import java.util.Objects;

public class ListArenasDuelsSubCmd extends DuelsSubCommand
{

    public ListArenasDuelsSubCmd(Duels plugin)
    {
        super(plugin, "listarenas");
    }

    @Override
    public boolean execute(CommandSender sender, String[] args)
    {
        if(!sender.hasPermission("duels.listarenas"))
        {
            noPerm(sender);
            return true;
        }
        if(args.length == 0)
        {
            for(Arena arena : plugin.getArenaManager().getArenaList())
            {
                int id = arena.getId();
                String name = arena.getName();
                String state = arena.getGameState().name();
                me.alphatct3209.duels.utils.MessageService.send(sender, plugin.getConfig(),
                        "Messages.Arena-List-Format", java.util.Map.of(
                                "<id>", id, "<arena_name>", name, "<state>", state));
            }
            return true;
        }
        else
        {
            incorrectArgs(sender, "/duels arena list");
            return true;
        }
    }
}
