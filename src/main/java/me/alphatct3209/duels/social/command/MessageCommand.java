package me.alphatct3209.duels.social.command;

import me.alphatct3209.duels.Duels;
import me.alphatct3209.duels.social.SocialManager;
import me.alphatct3209.duels.utils.MessageService;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.Arrays;
import java.util.Map;
import java.util.Objects;

public final class MessageCommand implements CommandExecutor
{
    private final Duels plugin;
    public MessageCommand(Duels plugin) { this.plugin = Objects.requireNonNull(plugin); }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args)
    {
        if (!(sender instanceof Player player))
        {
            sender.sendMessage("Direct messages require a player.");
            return true;
        }
        if (!player.hasPermission("duels.message"))
        {
            MessageService.send(player, plugin.getConfig(), "Messages.No-Permission", Map.of());
            return true;
        }
        if (args.length < 2)
        {
            MessageService.send(player, plugin.getConfig(), "Messages.Message-Usage", Map.of(),
                    "&cUsage: /" + label + " <player> <message>");
            return true;
        }
        SocialManager social = plugin.getSocialManager();
        Player target = social.exactOnline(args[0]);
        if (target == null || !player.canSee(target))
        {
            MessageService.send(player, plugin.getConfig(), "Messages.Message-Player-Unavailable", Map.of(),
                    "&cThat player is unavailable.");
            return true;
        }
        if (!social.allowsDirectMessage(player.getUniqueId(), target.getUniqueId()))
        {
            MessageService.send(player, plugin.getConfig(), "Messages.Message-Private", Map.of(
                    "<player>", target.getName()), "&c<player> only accepts messages from friends.");
            return true;
        }
        String text = String.join(" ", Arrays.copyOfRange(args, 1, args.length));
        MessageService.send(player, plugin.getConfig(), "Messages.Message-Sent", Map.of(
                "<player>", target.getName(), "<message>", text), "&7[You -> <player>] &f<message>");
        MessageService.send(target, plugin.getConfig(), "Messages.Message-Received", Map.of(
                "<player>", player.getName(), "<message>", text), "&7[<player> -> You] &f<message>");
        return true;
    }
}
