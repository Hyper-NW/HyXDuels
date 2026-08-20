package me.alphatct3209.duels.commands.subcommands.duel;

import me.alphatct3209.duels.Duels;
import me.alphatct3209.duels.commands.subcommands.DuelsSubCommand;
import me.alphatct3209.duels.game.arenas.Arena;
import me.alphatct3209.duels.game.kits.Kit;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.Objects;

public class JoinDuelsSubCmd extends DuelsSubCommand
{
    public JoinDuelsSubCmd(Duels plugin)
    {
        super(plugin, "join");
    }

    @Override
    public boolean execute(CommandSender sender, String[] args)
    {
        if (!sender.hasPermission("duels.join"))
        {
            noPerm(sender);
            return true;
        }
        if (!(sender instanceof Player player))
        {
            sender.sendMessage(ChatColor.RED + "You must be a Player to do that!");
            return false;
        }
        if (args.length > 1)
        {
            incorrectArgs(sender, "/duels queue join [arena id]");
            return true;
        }
        if (plugin.getArenaManager().getArena(player) != null)
        {
            configured(player, "Messages.Already-Joined");
            return true;
        }

        Arena arena;
        if (args.length == 0)
        {
            me.alphatct3209.duels.game.modes.DuelSelection selection =
                    plugin.getSelectionService().resolve(player.getUniqueId());
            arena = plugin.getArenaManager().findAvailableArena(selection);
            if (arena == null)
            {
                configured(player, "Messages.No-Available-Arena");
                return true;
            }
        }
        else
        {
            int id;
            try
            {
                id = Integer.parseInt(args[0]);
            }
            catch (NumberFormatException exception)
            {
                configured(player, "Messages.Invalid-Arena-Id", "<arg>", args[0]);
                return true;
            }
            arena = plugin.getArenaManager().getArena(id);
            if (arena == null)
            {
                configured(player, "Messages.Invalid-Arena-Id", "<arg>", args[0]);
                return true;
            }
        }

        // admit() is the single final predicate for both routes and captures the kit atomically.
        if (!plugin.getArenaManager().admit(player, arena))
        {
            configured(player, "Messages.Arena-Not-Available", "<arg>", Integer.toString(arena.getId()));
            return true;
        }
        configured(player, "Messages.Joined-Arena", "<arena_name>", arena.getName());
        return true;
    }

    private void configured(Player player, String path, String... replacement)
    {
        java.util.Map<String, String> values = new java.util.LinkedHashMap<>();
        for (int index = 0; index + 1 < replacement.length; index += 2)
            values.put(replacement[index], replacement[index + 1]);
        me.alphatct3209.duels.utils.MessageService.send(player, plugin.getConfig(), path, values);
    }
}
