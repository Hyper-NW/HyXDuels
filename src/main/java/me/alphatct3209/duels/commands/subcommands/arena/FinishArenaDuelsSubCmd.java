package me.alphatct3209.duels.commands.subcommands.arena;

import me.alphatct3209.duels.Duels;
import me.alphatct3209.duels.commands.subcommands.DuelsSubCommand;
import me.alphatct3209.duels.game.arenas.ArenaConfig;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.Objects;
import java.util.UUID;

public class FinishArenaDuelsSubCmd extends DuelsSubCommand
{
    public FinishArenaDuelsSubCmd(Duels plugin)
    {
        super(plugin, "finisharena");
    }

    @Override
    public boolean execute(CommandSender sender, String[] args)
    {
        if(!sender.hasPermission("duels.createarena"))
        {
            noPerm(sender);
            return true;
        }
        if(sender instanceof Player player)
        {
            if(args.length == 0)
            {
                UUID uuid = player.getUniqueId();
                ArenaConfig arenaConfig = ArenaConfig.creationMap.get(uuid);

                if(arenaConfig == null)
                {
                    player.sendMessage(ChatColor.RED + "You must do " + ChatColor.YELLOW +
                            "/duels arena create <arena name> " +
                            ChatColor.RED + "before doing that!");
                    return true;
                }

                if(!arenaConfig.isFinished())
                {
                    me.alphatct3209.duels.utils.MessageService.send(player, plugin.getConfig(),
                            "Messages.Arena-Not-Finished", java.util.Map.of());
                    return true;
                }

                try
                {
                    plugin.getArenaManager().save(arenaConfig);
                }
                catch (RuntimeException exception)
                {
                    player.sendMessage(ChatColor.RED + "Arena could not be activated: " + exception.getMessage());
                    plugin.getLogger().log(java.util.logging.Level.WARNING,
                            "Rejected runtime arena '" + arenaConfig.getName() + "'.", exception);
                    return true;
                }
                ArenaConfig.creationMap.remove(uuid, arenaConfig);
                me.alphatct3209.duels.utils.MessageService.send(player, plugin.getConfig(),
                        "Messages.Arena-Finished", java.util.Map.of());
                return true;
            }
            else
            {
                incorrectArgs(sender, "/duels arena finish");
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
