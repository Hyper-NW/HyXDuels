package me.alphatct3209.duels.commands.subcommands.arena;

import me.alphatct3209.duels.Duels;
import me.alphatct3209.duels.commands.subcommands.DuelsSubCommand;
import me.alphatct3209.duels.game.arenas.Arena;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;

public final class ArenaObjectiveDuelsSubCmd extends DuelsSubCommand
{
    public ArenaObjectiveDuelsSubCmd(Duels plugin) { super(plugin, "arenaobjective"); }
    public boolean execute(CommandSender sender, String[] args)
    {
        if (!sender.hasPermission("duels.arenaobjective")) { noPerm(sender); return true; }
        if (args.length < 2) { incorrectArgs(sender, "/duels arena objective <id> list|set|remove [key] [value]"); return true; }
        Arena arena;
        try { arena = plugin.getArenaManager().getArena(Integer.parseInt(args[0])); }
        catch (NumberFormatException exception) { arena = null; }
        if (arena == null) { sender.sendMessage(ChatColor.RED + "Unknown arena id."); return true; }
        if (args[1].equalsIgnoreCase("list"))
        {
            sender.sendMessage(ChatColor.YELLOW + "Objective: " + arena.getObjectiveSettings().values());
            return true;
        }
        try
        {
            if (args[1].equalsIgnoreCase("remove") && args.length == 3)
                plugin.getArenaManager().setObjective(arena, args[2], null);
            else if (args[1].equalsIgnoreCase("set") && args.length == 4)
                plugin.getArenaManager().setObjective(arena, args[2], Double.parseDouble(args[3]));
            else { incorrectArgs(sender, "/duels arena objective <id> list|set|remove [key] [value]"); return true; }
            sender.sendMessage(ChatColor.GREEN + "Arena objective setting updated.");
        }
        catch (IllegalArgumentException exception) { sender.sendMessage(ChatColor.RED + exception.getMessage()); }
        return true;
    }
}
