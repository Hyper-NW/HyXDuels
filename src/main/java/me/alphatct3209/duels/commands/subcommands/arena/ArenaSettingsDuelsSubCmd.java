package me.alphatct3209.duels.commands.subcommands.arena;

import me.alphatct3209.duels.Duels;
import me.alphatct3209.duels.commands.subcommands.DuelsSubCommand;
import me.alphatct3209.duels.game.arenas.Arena;
import me.alphatct3209.duels.game.arenas.ArenaSettings;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;

public class ArenaSettingsDuelsSubCmd extends DuelsSubCommand
{
    public ArenaSettingsDuelsSubCmd(Duels plugin)
    {
        super(plugin, "arenasettings");
    }

    @Override
    public boolean execute(CommandSender sender, String[] args)
    {
        if (!sender.hasPermission("duels.arenasettings"))
        {
            noPerm(sender);
            return true;
        }
        if (args.length < 1 || args.length > 3)
        {
            usage(sender);
            return true;
        }
        Arena arena = arena(sender, args[0]);
        if (arena == null)
        {
            return true;
        }
        if (args.length == 1 || (args.length == 2 && args[1].equalsIgnoreCase("list")))
        {
            sender.sendMessage(ChatColor.YELLOW + "Arena " + arena.getId() + " gameplay settings:");
            for (ArenaSettings.Flag flag : ArenaSettings.Flag.values())
            {
                sender.sendMessage(ChatColor.GRAY + "- " + flag.key() + ": "
                        + ChatColor.GOLD + arena.getSettings().get(flag));
            }
            return true;
        }
        if (args.length == 3)
        {
            ArenaSettings.Flag flag = ArenaSettings.Flag.parse(args[1]).orElse(null);
            if (flag == null)
            {
                sender.sendMessage(ChatColor.RED + "Unknown arena setting: " + args[1]);
                return true;
            }
            if (!args[2].equalsIgnoreCase("true") && !args[2].equalsIgnoreCase("false"))
            {
                sender.sendMessage(ChatColor.RED + "Setting value must be true or false.");
                return true;
            }
            boolean value = Boolean.parseBoolean(args[2]);
            plugin.getArenaManager().setSetting(arena, flag, value);
            me.alphatct3209.duels.utils.MessageService.send(sender, plugin.getConfig(),
                    "Messages.Arena-Setting-Updated", java.util.Map.of(
                            "<flag>", flag.key(), "<value>", value,
                            "<arena_id>", arena.getId()),
                    "&aSet &e<flag> &ato &e<value> &afor arena &e<arena_id>&a.");
            return true;
        }
        usage(sender);
        return true;
    }

    private Arena arena(CommandSender sender, String value)
    {
        try
        {
            Arena arena = plugin.getArenaManager().getArena(Integer.parseInt(value));
            if (arena != null)
            {
                return arena;
            }
        }
        catch (NumberFormatException ignored)
        {
        }
        sender.sendMessage(ChatColor.RED + "Invalid arena id: " + value);
        return null;
    }

    private void usage(CommandSender sender)
    {
        incorrectArgs(sender, "/duel arenasettings <id> [list|<flag> <true|false>]");
    }
}
