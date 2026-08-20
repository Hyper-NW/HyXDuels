package me.alphatct3209.duels.game.kits;

import me.alphatct3209.duels.Duels;
import me.alphatct3209.duels.utils.MessageService;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.List;
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
        if (!player.hasPermission("duels.kits.layout"))
        {
            MessageService.send(player, plugin.getConfig(), "Messages.No-Permission", Map.of());
            return true;
        }
        if (args.length != 0)
        {
            MessageService.send(player, plugin.getConfig(), "Messages.Personal-Kit-Editor-Usage", Map.of(),
                    "&cUsage: /kiteditor");
            return true;
        }
        Kit kit = plugin.getKitManager().resolveKit(player);
        plugin.getKitLayoutEditor().openPersonal(player, kit);
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args)
    {
        return List.of();
    }
}
