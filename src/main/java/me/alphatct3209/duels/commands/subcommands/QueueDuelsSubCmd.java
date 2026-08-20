package me.alphatct3209.duels.commands.subcommands;

import me.alphatct3209.duels.Duels;
import me.alphatct3209.duels.commands.subcommands.duel.JoinDuelsSubCmd;
import me.alphatct3209.duels.commands.subcommands.duel.LeaveDuelsSubCmd;
import me.alphatct3209.duels.commands.subcommands.duel.MenuDuelsSubCmd;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;

import java.util.Arrays;
import java.util.Locale;
import java.util.Map;

public final class QueueDuelsSubCmd extends DuelsSubCommand
{
    private final Map<String, DuelsSubCommand> actions;

    public QueueDuelsSubCmd(Duels plugin)
    {
        super(plugin, "queue");
        DuelsSubCommand menu = new MenuDuelsSubCmd(plugin);
        actions = Map.of("open", menu, "menu", menu, "join", new JoinDuelsSubCmd(plugin),
                "leave", new LeaveDuelsSubCmd(plugin));
    }

    @Override public boolean execute(CommandSender sender, String[] args)
    {
        if (args.length == 0 || args[0].equalsIgnoreCase("help"))
        {
            sender.sendMessage(ChatColor.DARK_AQUA + "HyXDuels Queue Commands");
            sender.sendMessage(ChatColor.YELLOW + "/duels queue open");
            sender.sendMessage(ChatColor.YELLOW + "/duels queue join [arena-id]");
            sender.sendMessage(ChatColor.YELLOW + "/duels queue leave");
            return true;
        }
        DuelsSubCommand action = actions.get(args[0].toLowerCase(Locale.ROOT));
        if (action == null) { incorrectArgs(sender, "/duels queue help"); return true; }
        return action.execute(sender, Arrays.copyOfRange(args, 1, args.length));
    }
}
