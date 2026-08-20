package me.alphatct3209.duels.commands.subcommands;

import me.alphatct3209.duels.Duels;
import me.alphatct3209.duels.social.command.FriendCommand;
import me.alphatct3209.duels.social.command.MessageCommand;
import me.alphatct3209.duels.social.command.SettingsCommand;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;

import java.util.Arrays;

public final class SocialDuelsSubCmd extends DuelsSubCommand
{
    private final FriendCommand friends;
    private final MessageCommand messages;
    private final SettingsCommand settings;

    public SocialDuelsSubCmd(Duels plugin)
    {
        super(plugin, "social");
        friends = new FriendCommand(plugin);
        messages = new MessageCommand(plugin);
        settings = new SettingsCommand(plugin);
    }

    @Override public boolean execute(CommandSender sender, String[] args)
    {
        if (args.length == 0 || args[0].equalsIgnoreCase("help"))
        {
            sender.sendMessage(ChatColor.DARK_AQUA + "HyXDuels Social Commands");
            sender.sendMessage(ChatColor.YELLOW + "/duels social friends <subcommand>");
            sender.sendMessage(ChatColor.YELLOW + "/duels social message <player> <message>");
            sender.sendMessage(ChatColor.YELLOW + "/duels social settings");
            return true;
        }
        String action = args[0].toLowerCase(java.util.Locale.ROOT);
        String[] remaining = Arrays.copyOfRange(args, 1, args.length);
        return switch (action)
        {
            case "friend", "friends" -> friends.onCommand(sender, null, "duels social friends", remaining);
            case "message", "msg" -> messages.onCommand(sender, null, "duels social message", remaining);
            case "settings" -> {
                if (remaining.length != 0)
                {
                    incorrectArgs(sender, "/duels social settings");
                    yield true;
                }
                yield settings.onCommand(sender, null, "duels social settings", remaining);
            }
            default -> { incorrectArgs(sender, "/duels social help"); yield true; }
        };
    }
}
