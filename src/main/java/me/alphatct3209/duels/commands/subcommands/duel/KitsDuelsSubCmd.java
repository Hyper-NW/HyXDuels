package me.alphatct3209.duels.commands.subcommands.duel;

import me.alphatct3209.duels.Duels;
import me.alphatct3209.duels.commands.subcommands.DuelsSubCommand;
import me.alphatct3209.duels.game.kits.Kit;
import me.alphatct3209.duels.game.kits.KitManager;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.Objects;

public class KitsDuelsSubCmd extends DuelsSubCommand
{
    public KitsDuelsSubCmd(Duels plugin)
    {
        super(plugin, "kits");
    }

    @Override
    public boolean is(String string)
    {
        return string.equalsIgnoreCase("kits") || string.equalsIgnoreCase("kit");
    }

    @Override
    public boolean execute(CommandSender sender, String[] args)
    {
        if(sender instanceof Player player)
        {
            if(args.length == 1)
            {
                if(args[0].equalsIgnoreCase("list"))
                {
                    if(!sender.hasPermission("duels.kits.list"))
                    {
                        noPerm(sender);
                        return true;
                    }
                    for(Kit kit : plugin.getKitManager().getKitList())
                    {
                        player.sendMessage("- " + kit.getName());
                    }
                    return true;
                }
                else
                {
                    incorrectArgs(player, "/duels help");
                    return true;
                }
            }
            else if (args.length == 2)
            {
                if(args[0].equalsIgnoreCase("create"))
                {
                    if(!sender.hasPermission("duels.kits.create"))
                    {
                        noPerm(sender);
                        return true;
                    }
                    String kitName = args[1];
                    for(Kit kit : plugin.getKitManager().getKitList())
                    {
                        if(kit.getName().equalsIgnoreCase(kitName))
                        {
                            player.sendMessage(ChatColor.RED + "A kit with that name already exists!");
                            return true;
                        }
                    }

                    try
                    {
                        plugin.getKitManager().createKit(kitName, player);
                    }
                    catch (IllegalArgumentException exception)
                    {
                        player.sendMessage(ChatColor.RED + exception.getMessage());
                        return true;
                    }
                    me.alphatct3209.duels.utils.MessageService.send(player, plugin.getConfig(),
                            "Messages.Kit-Created", java.util.Map.of("<kit_name>", kitName));
                    return true;
                }
                else if (args[0].equalsIgnoreCase("delete"))
                {
                    if(!sender.hasPermission("duels.kits.delete"))
                    {
                        noPerm(sender);
                        return true;
                    }
                    String kitName = args[1];
                    try
                    {
                        plugin.getKitManager().deleteKit(kitName);
                    }
                    catch (IllegalStateException exception)
                    {
                        player.sendMessage(ChatColor.RED + exception.getMessage());
                        return true;
                    }
                    me.alphatct3209.duels.utils.MessageService.send(player, plugin.getConfig(),
                            "Messages.Kit-Deleted", java.util.Map.of("<kit_name>", kitName));
                    return true;
                }
                else if (args[0].equalsIgnoreCase("select"))
                {
                    if(!sender.hasPermission("duels.kits.select"))
                    {
                        noPerm(sender);
                        return true;
                    }
                    String kitName = args[1];
                    Kit kit = plugin.getKitManager().getKit(kitName);
                    if(kit != null)
                    {
                        me.alphatct3209.duels.game.modes.DuelSelection current =
                                plugin.getSelectionService().resolve(player.getUniqueId());
                        try
                        {
                            plugin.getSelectionService().select(player.getUniqueId(),
                                    current.modeKey().value(), kit.getKey());
                        }
                        catch (IllegalArgumentException exception)
                        {
                            player.sendMessage(ChatColor.RED + exception.getMessage());
                            return true;
                        }
                        me.alphatct3209.duels.utils.MessageService.send(player, plugin.getConfig(),
                                "Messages.Kit-Selected", java.util.Map.of("<kit_name>", kit.getName()));
                    }
                    return true;
                }
                else
                {
                    incorrectArgs(player, "/duels kits <create/delete/select> <kit name>");
                    return true;
                }
            }
            else
            {
                incorrectArgs(player, "/duels help");
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
