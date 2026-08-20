package me.alphatct3209.duels.game.kits;

import me.alphatct3209.duels.Duels;
import me.alphatct3209.duels.utils.MessageService;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;

public final class KitEditorCommand implements CommandExecutor, TabCompleter
{
    private final Duels plugin;
    public KitEditorCommand(Duels plugin) { this.plugin = Objects.requireNonNull(plugin); }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args)
    {
        if (!(sender instanceof Player player))
        {
            sender.sendMessage("The kit editor requires a player.");
            return true;
        }
        if (!player.hasPermission("duels.kits.edit"))
        {
            MessageService.send(player, plugin.getConfig(), "Messages.No-Permission", Map.of());
            return true;
        }
        if (args.length != 1)
        {
            MessageService.send(player, plugin.getConfig(), "Messages.Kit-Editor-Usage", Map.of(),
                    "&cUsage: /" + label + " <kit>");
            return true;
        }
        Kit kit = plugin.getKitManager().getKitByNameOrKey(args[0]);
        if (kit == null)
        {
            MessageService.send(player, plugin.getConfig(), "Messages.Kit-Editor-Not-Found",
                    Map.of("<kit>", args[0]), "&cThat kit does not exist.");
            return true;
        }
        plugin.getKitLayoutEditor().open(player, kit);
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args)
    {
        if (args.length != 1 || !sender.hasPermission("duels.kits.edit")) return List.of();
        String prefix = args[0].toLowerCase(Locale.ROOT);
        return plugin.getKitManager().getKitList().stream().map(Kit::getKey).distinct().sorted()
                .filter(name -> name.toLowerCase(Locale.ROOT).startsWith(prefix)).toList();
    }
}
