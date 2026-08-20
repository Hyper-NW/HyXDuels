package me.alphatct3209.duels.commands.subcommands.duel;

import me.alphatct3209.duels.Duels;
import me.alphatct3209.duels.commands.subcommands.DuelsSubCommand;
import me.alphatct3209.duels.game.kits.Kit;
import me.alphatct3209.duels.game.modes.DuelMode;
import me.alphatct3209.duels.game.modes.DuelSelection;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public final class ModesDuelsSubCmd extends DuelsSubCommand
{
    public ModesDuelsSubCmd(Duels plugin) { super(plugin, "modes"); }
    @Override public boolean is(String value) { return value.equalsIgnoreCase("modes") || value.equalsIgnoreCase("mode"); }

    @Override public boolean execute(CommandSender sender, String[] args)
    {
        if (!(sender instanceof Player player))
        {
            sender.sendMessage(ChatColor.RED + "You must be a Player to do that!"); return true;
        }
        if (args.length == 1 && args[0].equalsIgnoreCase("list"))
        {
            if (!sender.hasPermission("duels.modes.list")) { noPerm(sender); return true; }
            player.sendMessage(ChatColor.GOLD + "Enabled duel modes:");
            for (DuelMode mode : plugin.getModeManager().enabledModes())
            {
                Kit kit = plugin.getKitManager().getKitByCanonicalKey(mode.defaultKitKey());
                player.sendMessage(ChatColor.YELLOW + "- " + mode.key() + ChatColor.GRAY + " ("
                        + mode.displayName() + ", default kit " + (kit == null ? mode.defaultKitKey() : kit.getName()) + ")");
            }
            return true;
        }
        if ((args.length == 2 || args.length == 3) && args[0].equalsIgnoreCase("select"))
        {
            if (!sender.hasPermission("duels.modes.select")) { noPerm(sender); return true; }
            try
            {
                DuelSelection selected = plugin.getSelectionService().select(player.getUniqueId(), args[1],
                        args.length == 3 ? args[2] : null);
                DuelMode mode = plugin.getModeManager().require(selected.modeKey());
                Kit kit = plugin.getSelectionService().kit(selected);
                player.sendMessage(ChatColor.GREEN + "Selected " + ChatColor.YELLOW + mode.displayName()
                        + ChatColor.GREEN + " with kit " + ChatColor.YELLOW + kit.getName() + ChatColor.GREEN + ".");
            }
            catch (IllegalArgumentException | IllegalStateException exception)
            {
                player.sendMessage(ChatColor.RED + exception.getMessage());
            }
            return true;
        }
        incorrectArgs(sender, "/duels modes list|select <mode> [kit]"); return true;
    }
}
