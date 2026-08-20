package me.alphatct3209.duels.party.command;

import me.alphatct3209.duels.Duels;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public final class PartyTabCompleter implements TabCompleter
{
    private final Duels plugin;
    public PartyTabCompleter(Duels plugin) { this.plugin = plugin; }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args)
    {
        List<String> values = new ArrayList<>();
        if (args.length == 1)
            values.addAll(List.of("invite", "kick", "promote", "demote", "transfer",
                    "accept", "deny", "leave", "disband", "list", "menu"));
        else if (args.length == 2 && List.of("invite", "kick", "promote", "demote", "transfer")
                .contains(args[0].toLowerCase(Locale.ROOT)))
        {
            Bukkit.getOnlinePlayers().stream().filter(player -> !(sender instanceof Player p) || !p.equals(player))
                    .map(Player::getName).forEach(values::add);
        }
        String prefix = args.length == 0 ? "" : args[args.length - 1].toLowerCase(Locale.ROOT);
        return values.stream().distinct().filter(value -> value.toLowerCase(Locale.ROOT).startsWith(prefix)).toList();
    }
}
