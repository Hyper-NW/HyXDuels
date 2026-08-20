package me.alphatct3209.duels.commands.subcommands.arena;

import me.alphatct3209.duels.Duels;
import me.alphatct3209.duels.commands.subcommands.DuelsSubCommand;
import me.alphatct3209.duels.game.arenas.ArenaConfig;
import me.alphatct3209.duels.game.arenas.ArenaSettings;
import me.alphatct3209.duels.utils.MessageService;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.Map;

public class CreateArenaDuelsSubCmd extends DuelsSubCommand
{
    public CreateArenaDuelsSubCmd(Duels plugin)
    {
        super(plugin, "createarena");
    }

    @Override
    public boolean execute(CommandSender sender, String[] args)
    {
        if (!sender.hasPermission("duels.createarena"))
        {
            noPerm(sender);
            return true;
        }
        if (!(sender instanceof Player player))
        {
            sender.sendMessage(ChatColor.RED + "You must be a Player to do that!");
            return true;
        }
        if (args.length < 1 || args.length > 3)
        {
            incorrectArgs(sender, "/duels arena create <name> [block-break] [block-place]");
            return true;
        }
        Boolean blockBreak = args.length >= 2 ? booleanValue(args[1]) : Boolean.FALSE;
        Boolean blockPlace = args.length >= 3 ? booleanValue(args[2]) : Boolean.FALSE;
        if (blockBreak == null || blockPlace == null)
        {
            MessageService.send(player, plugin.getConfig(), "Messages.Arena-Creation-Boolean",
                    Map.of(), "&cBlock-break and block-place must be true or false.");
            return true;
        }

        ArenaConfig arenaConfig = new ArenaConfig(plugin.getArenaManager().getNextId(), args[0]);
        arenaConfig.setSetting(ArenaSettings.Flag.BLOCK_BREAK, blockBreak);
        arenaConfig.setSetting(ArenaSettings.Flag.BLOCK_PLACE, blockPlace);
        ArenaConfig.creationMap.put(player.getUniqueId(), arenaConfig);
        MessageService.send(player, plugin.getConfig(), "Messages.Arena-Created", Map.of(
                "<arena_name>", args[0], "<block_break>", blockBreak,
                "<block_place>", blockPlace), "&aArena &e<arena_name> &acreated!");
        return true;
    }

    private Boolean booleanValue(String value)
    {
        if (value.equalsIgnoreCase("true")) return true;
        if (value.equalsIgnoreCase("false")) return false;
        return null;
    }
}
