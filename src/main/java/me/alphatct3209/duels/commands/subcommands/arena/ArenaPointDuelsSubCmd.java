package me.alphatct3209.duels.commands.subcommands.arena;

import me.alphatct3209.duels.Duels;
import me.alphatct3209.duels.commands.subcommands.DuelsSubCommand;
import me.alphatct3209.duels.game.arenas.Arena;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public final class ArenaPointDuelsSubCmd extends DuelsSubCommand
{
    public ArenaPointDuelsSubCmd(Duels plugin) { super(plugin, "arenapoint"); }
    public boolean execute(CommandSender sender, String[] args)
    {
        if (!sender.hasPermission("duels.arenapoint")) { noPerm(sender); return true; }
        if (args.length < 2) { incorrectArgs(sender, "/duel arenapoint <id> list|set|remove [name]"); return true; }
        Arena arena;
        try { arena = plugin.getArenaManager().getArena(Integer.parseInt(args[0])); }
        catch (NumberFormatException exception) { arena = null; }
        if (arena == null) { sender.sendMessage(ChatColor.RED + "Unknown arena id."); return true; }
        if (args[1].equalsIgnoreCase("list"))
        {
            sender.sendMessage(ChatColor.YELLOW + "Points: " + (arena.getPoints().isEmpty()
                    ? "none" : String.join(", ", arena.getPoints().keySet())));
            return true;
        }
        if (args.length != 3) { incorrectArgs(sender, "/duel arenapoint <id> set|remove <name>"); return true; }
        try
        {
            if (args[1].equalsIgnoreCase("remove")) plugin.getArenaManager().removePoint(arena, args[2]);
            else if (args[1].equalsIgnoreCase("set"))
            {
                if (!(sender instanceof Player player)) { sender.sendMessage(ChatColor.RED + "Setting a point requires a player."); return true; }
                plugin.getArenaManager().setPoint(arena, args[2], player.getLocation());
            }
            else { incorrectArgs(sender, "/duel arenapoint <id> list|set|remove [name]"); return true; }
            sender.sendMessage(ChatColor.GREEN + "Arena point updated.");
        }
        catch (IllegalArgumentException exception) { sender.sendMessage(ChatColor.RED + exception.getMessage()); }
        return true;
    }
}
