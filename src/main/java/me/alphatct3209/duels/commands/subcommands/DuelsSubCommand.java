package me.alphatct3209.duels.commands.subcommands;

import me.alphatct3209.duels.Duels;
import me.alphatct3209.duels.utils.MessageService;
import org.bukkit.command.CommandSender;

import java.util.Map;
import java.util.Objects;

public abstract class DuelsSubCommand
{
    protected final Duels plugin;
    private final String subCommand;

    protected DuelsSubCommand(Duels plugin, String subCommand)
    {
        this.plugin = Objects.requireNonNull(plugin, "plugin");
        this.subCommand = Objects.requireNonNull(subCommand, "subCommand");
    }

    protected void unknownCommand(CommandSender sender)
    {
        MessageService.send(sender, plugin.getConfig(), "Messages.Unknown-Command", Map.of());
    }

    protected void noPerm(CommandSender sender)
    {
        MessageService.send(sender, plugin.getConfig(), "Messages.No-Permission", Map.of());
    }

    protected void incorrectArgs(CommandSender sender, String suggestion)
    {
        MessageService.send(sender, plugin.getConfig(), "Messages.Incorrect-Args",
                Map.of("<suggestion>", suggestion));
    }

    public final String getSubCommand() { return subCommand; }
    public boolean is(String string) { return string.equalsIgnoreCase(subCommand); }
    public abstract boolean execute(CommandSender sender, String[] args);
}
