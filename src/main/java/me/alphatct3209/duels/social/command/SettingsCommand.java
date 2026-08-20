package me.alphatct3209.duels.social.command;

import me.alphatct3209.duels.Duels;
import me.alphatct3209.duels.utils.MessageService;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.Map;
import java.util.Objects;

public final class SettingsCommand implements CommandExecutor
{
    private final Duels plugin;
    public SettingsCommand(Duels plugin) { this.plugin = Objects.requireNonNull(plugin); }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args)
    {
        if (!(sender instanceof Player player))
        {
            sender.sendMessage("Settings require a player.");
            return true;
        }
        if (!player.hasPermission("duels.settings"))
        {
            MessageService.send(player, plugin.getConfig(), "Messages.No-Permission", Map.of());
            return true;
        }
        plugin.getSettingsGui().open(player);
        return true;
    }
}
