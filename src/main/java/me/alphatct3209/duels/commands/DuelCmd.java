package me.alphatct3209.duels.commands;

import me.alphatct3209.duels.Duels;
import me.alphatct3209.duels.commands.subcommands.HelpDuelsSubCmd;
import me.alphatct3209.duels.commands.subcommands.HologramDuelsSubCmd;
import me.alphatct3209.duels.commands.subcommands.DuelsSubCommand;
import me.alphatct3209.duels.commands.subcommands.UpdateDuelsSubCommand;
import me.alphatct3209.duels.commands.subcommands.arena.ArenaKitsDuelsSubCmd;
import me.alphatct3209.duels.commands.subcommands.arena.ArenaObjectiveDuelsSubCmd;
import me.alphatct3209.duels.commands.subcommands.arena.ArenaPointDuelsSubCmd;
import me.alphatct3209.duels.commands.subcommands.arena.ArenaSettingsDuelsSubCmd;
import me.alphatct3209.duels.commands.subcommands.arena.CreateArenaDuelsSubCmd;
import me.alphatct3209.duels.commands.subcommands.arena.FinishArenaDuelsSubCmd;
import me.alphatct3209.duels.commands.subcommands.arena.ListArenasDuelsSubCmd;
import me.alphatct3209.duels.commands.subcommands.arena.SetLobbyDuelsSubCmd;
import me.alphatct3209.duels.commands.subcommands.arena.SetSpawn1DuelsSubCmd;
import me.alphatct3209.duels.commands.subcommands.arena.SetSpawn2DuelsSubCmd;
import me.alphatct3209.duels.commands.subcommands.duel.AcceptChallengeDuelsSubCmd;
import me.alphatct3209.duels.commands.subcommands.duel.ChallengeDuelsSubCmd;
import me.alphatct3209.duels.commands.subcommands.duel.DenyChallengeDuelsSubCmd;
import me.alphatct3209.duels.commands.subcommands.duel.JoinDuelsSubCmd;
import me.alphatct3209.duels.commands.subcommands.duel.KitsDuelsSubCmd;
import me.alphatct3209.duels.commands.subcommands.duel.LeaveDuelsSubCmd;
import me.alphatct3209.duels.commands.subcommands.duel.MenuDuelsSubCmd;
import me.alphatct3209.duels.commands.subcommands.duel.ModesDuelsSubCmd;
import me.alphatct3209.duels.commands.subcommands.stats.GetStatsDuelsSubCmd;
import me.alphatct3209.duels.commands.subcommands.stats.LeaderboardDuelsSubCmd;
import me.alphatct3209.duels.commands.subcommands.stats.LoadStatsDuelsSubCmd;
import me.alphatct3209.duels.commands.subcommands.stats.StatsFileToSQLCmd;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.Arrays;
import java.util.List;
import java.util.Objects;

public final class DuelCmd implements CommandExecutor
{
    private final Duels plugin;
    private final List<DuelsSubCommand> subCommands;
    private final DuelsSubCommand help;

    public DuelCmd(Duels plugin)
    {
        this.plugin = Objects.requireNonNull(plugin, "plugin");
        help = new HelpDuelsSubCmd(plugin);
        subCommands = List.of(
                help,
                new MenuDuelsSubCmd(plugin),
                new JoinDuelsSubCmd(plugin),
                new LeaveDuelsSubCmd(plugin),
                new ChallengeDuelsSubCmd(plugin),
                new AcceptChallengeDuelsSubCmd(plugin),
                new DenyChallengeDuelsSubCmd(plugin),
                new KitsDuelsSubCmd(plugin),
                new ModesDuelsSubCmd(plugin),
                new ListArenasDuelsSubCmd(plugin),
                new CreateArenaDuelsSubCmd(plugin),
                new SetLobbyDuelsSubCmd(plugin),
                new SetSpawn1DuelsSubCmd(plugin),
                new SetSpawn2DuelsSubCmd(plugin),
                new FinishArenaDuelsSubCmd(plugin),
                new ArenaKitsDuelsSubCmd(plugin),
                new ArenaSettingsDuelsSubCmd(plugin),
                new ArenaPointDuelsSubCmd(plugin),
                new ArenaObjectiveDuelsSubCmd(plugin),
                new GetStatsDuelsSubCmd(plugin),
                new LeaderboardDuelsSubCmd(plugin),
                new LoadStatsDuelsSubCmd(plugin),
                new StatsFileToSQLCmd(plugin),
                new HologramDuelsSubCmd(plugin),
                new UpdateDuelsSubCommand(plugin));
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args)
    {
        if (args.length == 0)
        {
            return help.execute(sender, new String[0]);
        }

        DuelsSubCommand matched = subCommands.stream()
                .filter(subCommand -> subCommand.is(args[0]))
                .findFirst().orElse(null);
        List<String> visibleNames = sender instanceof Player viewer
                ? Bukkit.getOnlinePlayers().stream()
                        .filter(player -> !player.equals(viewer))
                        .filter(viewer::canSee)
                        .map(Player::getName)
                        .toList()
                : List.of();
        DuelCommandParser.Route route = DuelCommandParser.resolve(
                args[0], matched != null, visibleNames);
        if (route == DuelCommandParser.Route.SUBCOMMAND)
        {
            return matched.execute(sender, Arrays.copyOfRange(args, 1, args.length));
        }
        if (route == DuelCommandParser.Route.DIRECT_PLAYER && args.length == 1
                && sender instanceof Player player)
        {
            plugin.getChallengeManager().openSelection(player,
                    plugin.getChallengeManager().findExactOnline(args[0]));
            return true;
        }
        me.alphatct3209.duels.utils.MessageService.send(sender, plugin.getConfig(),
                "Messages.Unknown-Command", java.util.Map.of());
        return true;
    }
}
