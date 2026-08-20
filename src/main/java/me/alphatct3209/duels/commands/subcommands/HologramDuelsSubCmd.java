package me.alphatct3209.duels.commands.subcommands;

import me.alphatct3209.duels.Duels;
import me.alphatct3209.duels.hologram.HologramCommandParser;
import me.alphatct3209.duels.hologram.HologramDefinition;
import me.alphatct3209.duels.hologram.HologramManager;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.Map;

public final class HologramDuelsSubCmd extends DuelsSubCommand
{
    public HologramDuelsSubCmd(Duels plugin)
    {
        super(plugin, "hologram");
    }

    @Override
    public boolean execute(CommandSender sender, String[] args)
    {
        if (!sender.hasPermission("duels.holograms.admin"))
        {
            noPerm(sender);
            return true;
        }
        final HologramCommandParser.Parsed parsed;
        try
        {
            parsed = HologramCommandParser.parse(args);
        }
        catch (IllegalArgumentException exception)
        {
            send(sender, "Messages.Hologram-Error", "&c<error>", Map.of("<error>", exception.getMessage()));
            return true;
        }

        HologramManager manager = plugin.getHologramManager();
        try
        {
            switch (parsed.action())
            {
                case STATUS -> status(sender, manager.status());
                case LIST -> list(sender, manager);
                case RELOAD -> {
                    if (manager.reload())
                    {
                        send(sender, "Messages.Hologram-Reloaded",
                                "&aReloaded and reconciled advanced/holograms.yml.", Map.of());
                    }
                    else
                    {
                        String error = manager.status().lastError();
                        send(sender, "Messages.Hologram-Error", "&c<error>",
                                Map.of("<error>", error == null ? "Reload failed; check console." : error));
                    }
                }
                case CREATE -> {
                    Player player = requirePlayer(sender, "create");
                    if (player != null)
                    {
                        manager.create(player, parsed.id(), parsed.type(), parsed.gamemode());
                        send(sender, "Messages.Hologram-Created", "&aCreated managed hologram &e<id>&a.",
                                Map.of("<id>", parsed.id()));
                    }
                }
                case MOVE -> {
                    Player player = requirePlayer(sender, "move");
                    if (player != null)
                    {
                        manager.move(player, parsed.id());
                        send(sender, "Messages.Hologram-Moved", "&aMoved managed hologram &e<id>&a.",
                                Map.of("<id>", parsed.id()));
                    }
                }
                case DELETE -> {
                    manager.delete(parsed.id());
                    send(sender, "Messages.Hologram-Deleted", "&aDeleted managed hologram &e<id>&a.",
                            Map.of("<id>", parsed.id()));
                }
            }
        }
        catch (IllegalArgumentException | IllegalStateException exception)
        {
            send(sender, "Messages.Hologram-Error", "&c<error>", Map.of("<error>", exception.getMessage()));
        }
        return true;
    }

    private void status(CommandSender sender, HologramManager.Status status)
    {
        sender.sendMessage(color("&6HyXDuels holograms:&f global=" + status.globallyEnabled()
                + ", PlaceholderAPI=" + availability(status.placeholderApiEnabled())
                + ", DecentHolograms=" + availability(status.decentHologramsEnabled())
                + ", integration=" + (status.integrationActive() ? "active" : "inactive")));
        sender.sendMessage(color("&7Configured: &f" + status.configured() + "&7, runtime-owned: &f"
                + status.owned() + "&7, foreign-name conflicts: &f" + status.foreignConflicts().size()));
        if (status.lastError() != null)
        {
            sender.sendMessage(color("&cLast integration/config error: " + status.lastError()));
        }
        if (!status.placeholderApiEnabled())
        {
            sender.sendMessage(color("&ePlaceholderAPI is missing/disabled; leaderboard placeholders and hologram integration are unavailable."));
        }
        else if (!status.decentHologramsEnabled())
        {
            sender.sendMessage(color("&eDecentHolograms is missing/disabled; PlaceholderAPI leaderboard placeholders still work."));
        }
    }

    private void list(CommandSender sender, HologramManager manager)
    {
        if (manager.config().definitions().isEmpty())
        {
            send(sender, "Messages.Hologram-List-Empty", "&7No managed holograms are configured.", Map.of());
            return;
        }
        sender.sendMessage(color("&6Managed holograms:"));
        for (HologramDefinition definition : manager.config().definitions().values())
        {
            String mode = definition.gamemode() == null ? "" : "/" + definition.gamemode();
            sender.sendMessage(color("&e" + definition.id() + "&7: &f" + definition.name()
                    + " &8(" + definition.type().configValue() + mode + ", "
                    + definition.location().world() + ")"));
        }
    }

    private Player requirePlayer(CommandSender sender, String action)
    {
        if (sender instanceof Player player)
        {
            return player;
        }
        send(sender, "Messages.Hologram-Player-Only", "&cHologram <action> must be run by a player.",
                Map.of("<action>", action));
        return null;
    }

    private void send(CommandSender sender, String path, String fallback, Map<String, String> replacements)
    {
        me.alphatct3209.duels.utils.MessageService.send(sender, plugin.getConfig(), path,
                replacements, fallback);
    }

    private static String availability(boolean enabled)
    {
        return enabled ? "enabled" : "missing/disabled";
    }

    private static String color(String value)
    {
        return ChatColor.translateAlternateColorCodes('&', value);
    }
}
