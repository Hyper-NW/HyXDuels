package me.alphatct3209.duels.commands.subcommands.stats;

import me.alphatct3209.duels.Duels;
import me.alphatct3209.duels.commands.subcommands.DuelsSubCommand;
import me.alphatct3209.duels.game.modes.DuelMode;
import me.alphatct3209.duels.stats.leaderboard.LeaderboardEntry;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;

import java.util.List;
import java.util.Objects;

public class LeaderboardDuelsSubCmd extends DuelsSubCommand
{
    private static final String USAGE = "/duels top [wins|kills|divisions <gamemode>]";

    public LeaderboardDuelsSubCmd(Duels plugin)
    {
        super(plugin, "leaderboard");
    }

    @Override
    public boolean is(String string)
    {
        return string.equalsIgnoreCase("leaderboard") || string.equalsIgnoreCase("lb")
                || string.equalsIgnoreCase("top");
    }

    @Override
    public boolean execute(CommandSender sender, String[] args)
    {
        if (!sender.hasPermission("duels.top"))
        {
            noPerm(sender);
            return true;
        }

        if (args.length == 0 || (args.length == 1 && isWins(args[0])))
        {
            sendAggregateLeaderboard(sender, "Wins",
                    plugin.getLeaderboardService().snapshot().overallWins());
            return true;
        }
        if (args.length == 1 && isKills(args[0]))
        {
            sendAggregateLeaderboard(sender, "Kills",
                    plugin.getLeaderboardService().snapshot().overallKills());
            return true;
        }
        if (args.length == 2 && args[0].equalsIgnoreCase("divisions"))
        {
            DuelMode mode = plugin.getModeManager().resolve(args[1]).orElse(null);
            if (mode == null)
            {
                sendInvalidGamemode(sender, args[1]);
                return true;
            }
            sendDivisionLeaderboard(sender, mode);
            return true;
        }

        incorrectArgs(sender, USAGE);
        return true;
    }

    private void sendAggregateLeaderboard(CommandSender sender, String displayType,
                                           List<LeaderboardEntry> values)
    {
        sendHeader(sender, displayType);
        int place = 1;
        for (LeaderboardEntry entry : values)
        {
            me.alphatct3209.duels.utils.MessageService.send(sender, plugin.getConfig(),
                    "Messages.Leaderboard-Format", java.util.Map.of(
                            "<place>", place++, "<player>", entry.name(), "<stat>", entry.value()));
        }
    }

    private void sendDivisionLeaderboard(CommandSender sender, DuelMode mode)
    {
        List<LeaderboardEntry> values = plugin.getLeaderboardService().snapshot().modes()
                .getOrDefault(mode.key().value(), List.of());
        sendHeader(sender, mode.displayName() + " Divisions");

        int place = 1;
        for (LeaderboardEntry entry : values)
        {
            me.alphatct3209.duels.utils.MessageService.send(sender, plugin.getConfig(),
                    "Messages.Division-Leaderboard-Format", java.util.Map.of(
                            "<place>", place++, "<player>", entry.name(),
                            "<gamemode>", mode.displayName(), "<gamemode_key>", mode.key().value(),
                            "<mode>", mode.displayName(), "<mode_key>", mode.key().value(),
                            "<division>", entry.division(), "<wins>", entry.value()));
        }
    }

    private void sendHeader(CommandSender sender, String displayType)
    {
        me.alphatct3209.duels.utils.MessageService.send(sender, plugin.getConfig(),
                "Messages.Leaderboard-Header", java.util.Map.of("<type>", displayType));
    }

    private boolean isWins(String value)
    {
        return value.equalsIgnoreCase("win") || value.equalsIgnoreCase("wins");
    }

    private boolean isKills(String value)
    {
        return value.equalsIgnoreCase("kill") || value.equalsIgnoreCase("kills");
    }

    private void sendInvalidGamemode(CommandSender sender, String gamemode)
    {
        me.alphatct3209.duels.utils.MessageService.send(sender, plugin.getConfig(),
                "Messages.Invalid-Gamemode", java.util.Map.of("<gamemode>", gamemode),
                "&cUnknown gamemode: &e<gamemode>");
    }
}
