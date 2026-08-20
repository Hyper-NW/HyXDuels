package me.alphatct3209.duels.social.command;

import me.alphatct3209.duels.Duels;
import me.alphatct3209.duels.social.SocialManager;
import me.alphatct3209.duels.utils.MessageService;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;

public final class FriendCommand implements CommandExecutor, TabCompleter
{
    private final Duels plugin;
    public FriendCommand(Duels plugin) { this.plugin = Objects.requireNonNull(plugin); }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args)
    {
        if (!(sender instanceof Player player))
        {
            sender.sendMessage("Friend commands require a player.");
            return true;
        }
        if (!player.hasPermission("duels.friend"))
        {
            MessageService.send(player, plugin.getConfig(), "Messages.No-Permission", Map.of());
            return true;
        }
        if (args.length == 0 || args[0].equalsIgnoreCase("list"))
        {
            showList(player);
            return true;
        }
        if (args.length != 2)
        {
            usage(player);
            return true;
        }
        SocialManager social = plugin.getSocialManager();
        switch (args[0].toLowerCase(Locale.ROOT))
        {
            case "add" -> social.sendFriendRequest(player, social.exactOnline(args[1]));
            case "accept" -> social.acceptFriend(player, args[1]);
            case "deny" -> social.denyFriend(player, args[1]);
            case "remove" -> social.removeFriend(player, args[1]);
            case "best" -> social.toggleBestFriend(player, args[1]);
            default -> usage(player);
        }
        return true;
    }

    private void showList(Player player)
    {
        SocialManager social = plugin.getSocialManager();
        MessageService.send(player, plugin.getConfig(), "Messages.Friend-List-Header", Map.of(
                "<count>", social.friends(player.getUniqueId()).size()), "&6Friends &7(<count>)");
        if (social.friends(player.getUniqueId()).isEmpty())
        {
            MessageService.send(player, plugin.getConfig(), "Messages.Friend-List-Empty", Map.of(),
                    "&7You have no friends yet.");
            return;
        }
        social.friends(player.getUniqueId()).stream()
                .sorted((a, b) -> social.name(a).compareToIgnoreCase(social.name(b)))
                .forEach(uuid -> MessageService.send(player, plugin.getConfig(),
                        "Messages.Friend-List-Entry", Map.of("<player>", social.name(uuid),
                                "<status>", (social.isBestFriend(player.getUniqueId(), uuid) ? "Best Friend, " : "")
                                        + (Bukkit.getPlayer(uuid) == null ? "Offline" : "Online")),
                        "&7- &f<player> &8[&e<status>&8]"));
    }

    private void usage(Player player)
    {
        MessageService.send(player, plugin.getConfig(), "Messages.Friend-Usage", Map.of(),
                "&e/friend add|accept|deny|remove|best <player>", "&e/friend list");
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args)
    {
        if (args.length == 1) return filter(List.of("add", "accept", "deny", "remove", "best", "list"), args[0]);
        if (args.length == 2 && List.of("add", "accept", "deny", "remove", "best")
                .contains(args[0].toLowerCase(Locale.ROOT)))
            return filter(Bukkit.getOnlinePlayers().stream().map(Player::getName).toList(), args[1]);
        return List.of();
    }

    private static List<String> filter(List<String> values, String prefix)
    {
        String lower = prefix.toLowerCase(Locale.ROOT);
        List<String> result = new ArrayList<>();
        for (String value : values) if (value.toLowerCase(Locale.ROOT).startsWith(lower)) result.add(value);
        return result;
    }
}
