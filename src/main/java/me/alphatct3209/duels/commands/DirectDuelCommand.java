package me.alphatct3209.duels.commands;

import me.alphatct3209.duels.Duels;
import me.alphatct3209.duels.utils.MessageService;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.List;
import java.util.Locale;

/** Singular direct-player shortcut; all non-player actions live under /duels. */
public final class DirectDuelCommand implements CommandExecutor, TabCompleter
{
    private final Duels plugin;

    public DirectDuelCommand(Duels plugin)
    {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args)
    {
        if (!(sender instanceof Player player))
        {
            sender.sendMessage(org.bukkit.ChatColor.RED + "Only players can issue direct challenges.");
            return true;
        }
        if (args.length != 1)
        {
            MessageService.send(sender, plugin.getConfig(), "Messages.Incorrect-Usage",
                    java.util.Map.of("<usage>", "/duel <player>"));
            return true;
        }
        plugin.getChallengeManager().openSelection(player,
                plugin.getChallengeManager().findExactOnline(args[0]));
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args)
    {
        if (args.length != 1 || !(sender instanceof Player viewer)) return List.of();
        String prefix = args[0].toLowerCase(Locale.ROOT);
        return Bukkit.getOnlinePlayers().stream()
                .filter(player -> !player.equals(viewer) && viewer.canSee(player))
                .map(Player::getName)
                .filter(name -> name.toLowerCase(Locale.ROOT).startsWith(prefix))
                .sorted(String.CASE_INSENSITIVE_ORDER)
                .toList();
    }
}
