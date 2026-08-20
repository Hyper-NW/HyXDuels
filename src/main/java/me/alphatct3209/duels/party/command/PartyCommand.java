package me.alphatct3209.duels.party.command;

import me.alphatct3209.duels.Duels;
import me.alphatct3209.duels.party.Party;
import me.alphatct3209.duels.party.PartyManager;
import me.alphatct3209.duels.utils.MessageService;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.Map;
import java.util.Objects;

public final class PartyCommand implements CommandExecutor
{
    private final Duels plugin;

    public PartyCommand(Duels plugin) { this.plugin = Objects.requireNonNull(plugin); }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args)
    {
        if (!(sender instanceof Player player))
        {
            sender.sendMessage("Party commands require a player.");
            return true;
        }
        if (!player.hasPermission("duels.party"))
        {
            MessageService.send(player, plugin.getConfig(), "Messages.No-Permission", Map.of());
            return true;
        }
        PartyManager parties = plugin.getPartyManager();
        if (args.length == 0 || args[0].equalsIgnoreCase("menu"))
        {
            Party party = parties.getParty(player.getUniqueId());
            if (party != null && party.leader().equals(player.getUniqueId()))
                plugin.getPartyGui().open(player);
            else showList(player);
            return true;
        }
        String action = args[0].toLowerCase(java.util.Locale.ROOT);
        switch (action)
        {
            case "invite" -> withTarget(player, args, parties::invite);
            case "kick" -> withTarget(player, args, parties::kick);
            case "promote" -> withTarget(player, args, parties::promote);
            case "demote" -> withTarget(player, args, parties::demote);
            case "transfer" -> withTarget(player, args, parties::transfer);
            case "accept" -> parties.accept(player);
            case "deny" -> parties.deny(player);
            case "leave" -> parties.leave(player);
            case "disband" -> parties.disband(player);
            case "list" -> showList(player);
            default -> MessageService.send(player, plugin.getConfig(), "Messages.Party-Usage", Map.of(),
                    "&e/duels party invite|kick|promote|demote|transfer <player>",
                    "&e/duels party accept|deny|leave|disband|list|menu");
        }
        return true;
    }

    private void withTarget(Player actor, String[] args, PartyAction action)
    {
        if (args.length != 2)
        {
            MessageService.send(actor, plugin.getConfig(), "Messages.Party-Usage", Map.of(),
                    "&cUsage: /duels party " + args[0] + " <player>");
            return;
        }
        Player target = plugin.getPartyManager().exactOnline(args[1]);
        action.run(actor, target);
    }

    private void showList(Player viewer)
    {
        Party party = plugin.getPartyManager().getParty(viewer.getUniqueId());
        if (party == null)
        {
            MessageService.send(viewer, plugin.getConfig(), "Messages.Party-Not-In-Party", Map.of(),
                    "&cYou are not in a party.");
            return;
        }
        MessageService.send(viewer, plugin.getConfig(), "Messages.Party-List-Header", Map.of(
                "<leader>", playerName(party.leader()), "<party_size>", party.size(),
                "<visibility>", party.visible() ? "visible" : "private"),
                "&6Party &7(<party_size>) &8- &e<visibility>");
        party.roles().forEach((uuid, role) -> MessageService.send(viewer, plugin.getConfig(),
                "Messages.Party-List-Member", Map.of("<player>", playerName(uuid),
                        "<role>", role.name().toLowerCase(java.util.Locale.ROOT),
                        "<online>", Bukkit.getPlayer(uuid) != null),
                "&7- &f<player> &8[&e<role>&8]"));
    }

    private String playerName(java.util.UUID uuid)
    {
        Player player = Bukkit.getPlayer(uuid);
        return player == null ? uuid.toString() : player.getName();
    }

    @FunctionalInterface
    private interface PartyAction { boolean run(Player actor, Player target); }
}
