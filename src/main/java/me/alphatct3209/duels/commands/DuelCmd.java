package me.alphatct3209.duels.commands;

import me.alphatct3209.duels.Duels;
import me.alphatct3209.duels.commands.subcommands.HelpDuelsSubCmd;
import me.alphatct3209.duels.commands.subcommands.HologramDuelsSubCmd;
import me.alphatct3209.duels.commands.subcommands.DuelsSubCommand;
import me.alphatct3209.duels.commands.subcommands.AdminDuelsSubCmd;
import me.alphatct3209.duels.commands.subcommands.ChallengeGroupDuelsSubCmd;
import me.alphatct3209.duels.commands.subcommands.PartyDuelsSubCmd;
import me.alphatct3209.duels.commands.subcommands.QueueDuelsSubCmd;
import me.alphatct3209.duels.commands.subcommands.SocialDuelsSubCmd;
import me.alphatct3209.duels.commands.subcommands.StatsDuelsSubCmd;
import me.alphatct3209.duels.commands.subcommands.arena.ArenaDuelsSubCmd;
import me.alphatct3209.duels.commands.subcommands.duel.KitsDuelsSubCmd;
import me.alphatct3209.duels.commands.subcommands.duel.ModesDuelsSubCmd;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;

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
                new QueueDuelsSubCmd(plugin),
                new ChallengeGroupDuelsSubCmd(plugin),
                new KitsDuelsSubCmd(plugin),
                new ModesDuelsSubCmd(plugin),
                new ArenaDuelsSubCmd(plugin),
                new StatsDuelsSubCmd(plugin),
                new PartyDuelsSubCmd(plugin),
                new SocialDuelsSubCmd(plugin),
                new HologramDuelsSubCmd(plugin),
                new AdminDuelsSubCmd(plugin));
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
        if (matched != null)
        {
            return matched.execute(sender, Arrays.copyOfRange(args, 1, args.length));
        }
        me.alphatct3209.duels.utils.MessageService.send(sender, plugin.getConfig(),
                "Messages.Unknown-Command", java.util.Map.of());
        return true;
    }
}
