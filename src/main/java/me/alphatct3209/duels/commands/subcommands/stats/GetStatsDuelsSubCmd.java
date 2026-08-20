package me.alphatct3209.duels.commands.subcommands.stats;

import me.alphatct3209.duels.Duels;
import me.alphatct3209.duels.commands.subcommands.DuelsSubCommand;
import me.alphatct3209.duels.divisions.Division;
import me.alphatct3209.duels.divisions.DivisionProgress;
import me.alphatct3209.duels.game.kits.Kit;
import me.alphatct3209.duels.game.modes.DuelMode;
import me.alphatct3209.duels.game.modes.DuelSelection;
import me.alphatct3209.duels.stats.db.StatisticsDatabase;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.UUID;

public class GetStatsDuelsSubCmd extends DuelsSubCommand
{
    public GetStatsDuelsSubCmd(Duels plugin) { super(plugin, "stats"); }

    private void sendStatsCard(CommandSender sender, Target target, DuelMode mode, Kit kit)
    {
        StatisticsDatabase database = plugin.getStatisticsManager().getStatsDB();
        int modeWins = database.getModeWins(target.uuid(), mode.key());
        DivisionProgress progress = plugin.getDivisionManager().getProgress(modeWins);
        String division = progress.current().map(Division::displayName).orElse("Unranked");
        String nextDivision = progress.next().map(Division::displayName).orElse("Maximum");
        Object winsToNext = progress.next().map(next -> Math.max(0L, next.requiredWins() - modeWins)).orElse(0L);
        boolean ownProfile = sender instanceof Player viewer && viewer.getUniqueId().equals(target.uuid());
        me.alphatct3209.duels.social.PlayerPreferences privacy =
                plugin.getSocialManager().preferences(target.uuid());
        if (!ownProfile && !privacy.showOwnTier())
        {
            division = "Private";
            nextDivision = "Private";
            winsToNext = "Private";
        }
        String kitName = !ownProfile && !privacy.profileKitsPublic() ? "Private" : kit.getName();
        java.util.Map<String, Object> values = java.util.Map.ofEntries(
                java.util.Map.entry("<wins>", database.getWins(target.uuid())),
                java.util.Map.entry("<overall_wins>", database.getWins(target.uuid())),
                java.util.Map.entry("<losses>", database.getLosses(target.uuid())),
                java.util.Map.entry("<kills>", database.getKills(target.uuid())),
                java.util.Map.entry("<overall_kills>", database.getKills(target.uuid())),
                java.util.Map.entry("<deaths>", database.getDeaths(target.uuid())),
                java.util.Map.entry("<winstreak>", database.getWinStreak(target.uuid())),
                java.util.Map.entry("<win_streak>", database.getWinStreak(target.uuid())),
                java.util.Map.entry("<highest_winstreak>", database.getHighestWinStreak(target.uuid())),
                java.util.Map.entry("<highest_win_streak>", database.getHighestWinStreak(target.uuid())),
                java.util.Map.entry("<name>", target.name()),
                java.util.Map.entry("<gamemode>", mode.displayName()),
                java.util.Map.entry("<gamemode_key>", mode.key().value()),
                java.util.Map.entry("<mode>", mode.displayName()),
                java.util.Map.entry("<mode_key>", mode.key().value()),
                java.util.Map.entry("<kit>", kitName),
                java.util.Map.entry("<gamemode_wins>", modeWins),
                java.util.Map.entry("<mode_wins>", modeWins),
                java.util.Map.entry("<division>", division),
                java.util.Map.entry("<next_division>", nextDivision),
                java.util.Map.entry("<wins_to_next>", winsToNext));
        me.alphatct3209.duels.utils.MessageService.send(sender, plugin.getConfig(),
                "Messages.Stats-Card", values);
    }

    @Override public boolean execute(CommandSender sender, String[] args)
    {
        if (!sender.hasPermission("duels.stats")) { noPerm(sender); return true; }
        if (args.length > 2) { incorrectArgs(sender, "/duels stats [player] [mode]"); return true; }
        Target target;
        DuelMode mode;
        Kit kit;
        if (args.length == 0)
        {
            if (!(sender instanceof Player player))
            {
                sender.sendMessage(ChatColor.RED + "You must specify a player from the console."); return true;
            }
            target = new Target(player.getUniqueId(), player.getName(), player);
            DuelSelection selection = plugin.getSelectionService().resolve(player.getUniqueId());
            mode = plugin.getModeManager().require(selection.modeKey()); kit = plugin.getSelectionService().kit(selection);
        }
        else if (args.length == 1)
        {
            target = findTarget(args[0]);
            if (target != null)
            {
                SelectionView selected = selectedOrDefault(target); mode = selected.mode(); kit = selected.kit();
            }
            else if (sender instanceof Player player && plugin.getModeManager().resolve(args[0]).isPresent())
            {
                target = new Target(player.getUniqueId(), player.getName(), player);
                mode = plugin.getModeManager().resolve(args[0]).orElseThrow();
                kit = plugin.getKitManager().getKitByCanonicalKey(mode.defaultKitKey());
            }
            else { sendMissingPlayer(sender, args[0]); return true; }
        }
        else
        {
            target = findTarget(args[0]);
            if (target == null) { sendMissingPlayer(sender, args[0]); return true; }
            mode = plugin.getModeManager().resolve(args[1]).orElse(null);
            if (mode == null) { sendInvalidMode(sender, args[1]); return true; }
            kit = plugin.getKitManager().getKitByCanonicalKey(mode.defaultKitKey());
        }
        if (mode == null || kit == null)
        {
            me.alphatct3209.duels.utils.MessageService.send(sender, plugin.getConfig(),
                    "Messages.No-Default-Gamemode", java.util.Map.of(),
                    "&cNo default mode/kit is configured."); return true;
        }
        sendStatsCard(sender, target, mode, kit); return true;
    }

    private SelectionView selectedOrDefault(Target target)
    {
        DuelSelection selection = target.onlinePlayer() == null
                ? plugin.getSelectionService().resolve(target.uuid())
                : plugin.getSelectionService().resolve(target.onlinePlayer().getUniqueId());
        return new SelectionView(plugin.getModeManager().require(selection.modeKey()),
                plugin.getSelectionService().kit(selection));
    }

    private Target findTarget(String name)
    {
        Player online = Bukkit.getPlayerExact(name);
        if (online != null) return new Target(online.getUniqueId(), online.getName(), online);
        StatisticsDatabase database = plugin.getStatisticsManager().getStatsDB();
        UUID uuid = database.getUUID(name);
        if (uuid == null) return null;
        String knownName = database.getLastKnownName(uuid);
        return new Target(uuid, knownName == null ? name : knownName, null);
    }

    private void sendMissingPlayer(CommandSender sender, String name)
    { sender.sendMessage(ChatColor.RED + "Cannot find statistics for " + ChatColor.YELLOW + name); }
    private void sendInvalidMode(CommandSender sender, String mode)
    { me.alphatct3209.duels.utils.MessageService.send(sender, plugin.getConfig(),
            "Messages.Invalid-Gamemode", java.util.Map.of("<gamemode>", mode),
            "&cUnknown mode: &e<gamemode>"); }
    private record Target(UUID uuid, String name, Player onlinePlayer) {}
    private record SelectionView(DuelMode mode, Kit kit) {}
}
