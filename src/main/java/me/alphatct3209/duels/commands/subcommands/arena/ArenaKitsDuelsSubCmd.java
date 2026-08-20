package me.alphatct3209.duels.commands.subcommands.arena;

import me.alphatct3209.duels.Duels;
import me.alphatct3209.duels.commands.subcommands.DuelsSubCommand;
import me.alphatct3209.duels.game.arenas.Arena;
import me.alphatct3209.duels.game.modes.DuelMode;
import me.alphatct3209.duels.game.modes.ModeKey;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;

import java.util.Set;

/** Core arena mode routing command. The old /arenakits spelling is a deprecated alias. */
public class ArenaKitsDuelsSubCmd extends DuelsSubCommand
{
    public ArenaKitsDuelsSubCmd(Duels plugin) { super(plugin, "arenamodes"); }

    @Override public boolean is(String value)
    {
        return value.equalsIgnoreCase("arenamodes") || value.equalsIgnoreCase("arenakits");
    }

    @Override public boolean execute(CommandSender sender, String[] args)
    {
        if (!sender.hasPermission("duels.arenamodes") && !sender.hasPermission("duels.arenakits"))
        {
            noPerm(sender); return true;
        }
        if (args.length < 2 || args.length > 3)
        {
            incorrectArgs(sender, "/duel arenamodes <id> list|add|remove|clear [mode]"); return true;
        }
        Arena arena = arena(sender, args[0]);
        if (arena == null) return true;
        String action = args[1];
        if (action.equalsIgnoreCase("list") && args.length == 2)
        {
            Set<ModeKey> keys = arena.getAllowedModeKeys();
            sender.sendMessage(ChatColor.YELLOW + "Arena " + arena.getId() + " allowed modes: "
                    + (keys.isEmpty() ? "(globally-unassigned mode pool)" : String.join(", ", keys.stream().map(ModeKey::value).toList())));
            return true;
        }
        if (action.equalsIgnoreCase("clear") && args.length == 2)
        {
            plugin.getArenaManager().clearAllowedModes(arena); changed(sender, arena); return true;
        }
        if ((action.equalsIgnoreCase("add") || action.equalsIgnoreCase("remove")) && args.length == 3)
        {
            DuelMode mode = plugin.getModeManager().resolve(args[2]).orElse(null);
            if (mode == null)
            {
                sender.sendMessage(ChatColor.RED + "Unknown mode: " + args[2]); return true;
            }
            if (action.equalsIgnoreCase("add")) plugin.getArenaManager().addAllowedMode(arena, mode);
            else plugin.getArenaManager().removeAllowedMode(arena, mode);
            changed(sender, arena); return true;
        }
        incorrectArgs(sender, "/duel arenamodes <id> list|add|remove|clear [mode]"); return true;
    }

    private Arena arena(CommandSender sender, String value)
    {
        try
        {
            Arena arena = plugin.getArenaManager().getArena(Integer.parseInt(value));
            if (arena != null) return arena;
        }
        catch (NumberFormatException ignored) {}
        sender.sendMessage(ChatColor.RED + "Invalid arena id: " + value); return null;
    }

    private void changed(CommandSender sender, Arena arena)
    {
        me.alphatct3209.duels.utils.MessageService.send(sender, plugin.getConfig(),
                "Messages.Arena-Modes-Updated", java.util.Map.of("<arena_id>", arena.getId()),
                "&aUpdated allowed modes for arena &e<arena_id>&a.");
    }
}
