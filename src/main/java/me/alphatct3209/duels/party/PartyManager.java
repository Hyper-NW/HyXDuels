package me.alphatct3209.duels.party;

import me.alphatct3209.duels.Duels;
import me.alphatct3209.duels.utils.MessageService;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

public final class PartyManager
{
    private final Duels plugin;
    private final Map<UUID, Party> byMember = new HashMap<>();
    private final Map<UUID, Invite> invites = new HashMap<>();
    private final int maxSize;
    private final long inviteMillis;

    public PartyManager(Duels plugin)
    {
        this.plugin = Objects.requireNonNull(plugin, "plugin");
        maxSize = Math.max(2, Math.min(100, plugin.getConfig().getInt("Party.Max-Size", 16)));
        inviteMillis = Math.max(5, plugin.getConfig().getInt("Party.Invite-Timeout-Seconds", 60)) * 1000L;
    }

    public Party getParty(UUID player) { return byMember.get(player); }
    public boolean isInParty(UUID player) { return byMember.containsKey(player); }

    public Party getOrCreate(Player leader)
    {
        Party existing = byMember.get(leader.getUniqueId());
        if (existing != null) return existing;
        Party created = new Party(leader.getUniqueId());
        byMember.put(leader.getUniqueId(), created);
        return created;
    }

    public boolean invite(Player sender, Player target)
    {
        if (target == null || !target.isOnline() || sender.equals(target))
            return fail(sender, "Messages.Party-Player-Unavailable", "&cThat player is unavailable.");
        if (!plugin.getSocialManager().allowsPartyInvite(sender.getUniqueId(), target.getUniqueId()))
            return fail(sender, "Messages.Party-Invites-Friends-Only",
                    "&cThat player only accepts party invites from friends.");
        Party party = getOrCreate(sender);
        if (!canManage(party, sender.getUniqueId()))
            return fail(sender, "Messages.Party-No-Authority", "&cYou cannot manage this party.");
        if (isInParty(target.getUniqueId()))
            return fail(sender, "Messages.Party-Already-In-Party", "&cThat player is already in a party.");
        if (party.size() >= maxSize)
            return fail(sender, "Messages.Party-Full", "&cYour party is full.");
        invites.put(target.getUniqueId(), new Invite(party, sender.getUniqueId(),
                System.currentTimeMillis() + inviteMillis));
        send(sender, "Messages.Party-Invite-Sent", Map.of("<player>", target.getName()),
                "&aInvited &e<player> &ato your party.");
        send(target, "Messages.Party-Invite-Received", Map.of(
                        "<player>", sender.getName(), "<leader>", name(party.leader())),
                "&e<player> &ainvited you to a party. Use &e/duels party accept&a.");
        return true;
    }

    public boolean accept(Player player)
    {
        Invite invite = validInvite(player.getUniqueId());
        if (invite == null)
            return fail(player, "Messages.Party-No-Invite", "&cYou have no pending party invite.");
        Party party = invite.party();
        if (party == null || byMember.get(party.leader()) != party
                || party.size() >= maxSize || isInParty(player.getUniqueId()))
        {
            invites.remove(player.getUniqueId());
            return fail(player, "Messages.Party-Invite-Invalid", "&cThat party invite is no longer valid.");
        }
        party.add(player.getUniqueId());
        byMember.put(player.getUniqueId(), party);
        invites.remove(player.getUniqueId());
        broadcast(party, "Messages.Party-Joined", Map.of("<player>", player.getName()),
                "&a<player> joined the party.");
        return true;
    }

    public boolean deny(Player player)
    {
        Invite invite = validInvite(player.getUniqueId());
        if (invite == null)
            return fail(player, "Messages.Party-No-Invite", "&cYou have no pending party invite.");
        invites.remove(player.getUniqueId());
        send(player, "Messages.Party-Invite-Denied", Map.of(), "&cParty invite denied.");
        Player inviter = Bukkit.getPlayer(invite.inviter());
        if (inviter != null)
            send(inviter, "Messages.Party-Invite-Denied-Other", Map.of("<player>", player.getName()),
                    "&c<player> denied your party invite.");
        return true;
    }

    public boolean kick(Player actor, Player target)
    {
        Party party = required(actor);
        if (party == null || target == null || !party.contains(target.getUniqueId())
                || target.getUniqueId().equals(party.leader()))
            return fail(actor, "Messages.Party-Player-Not-Member", "&cThat player is not a removable party member.");
        PartyRole actorRole = party.role(actor.getUniqueId());
        PartyRole targetRole = party.role(target.getUniqueId());
        if (actorRole == null || !actorRole.canManageMembers()
                || (actorRole != PartyRole.LEADER && targetRole != PartyRole.MEMBER))
            return fail(actor, "Messages.Party-No-Authority", "&cYou cannot manage that member.");
        party.remove(target.getUniqueId());
        byMember.remove(target.getUniqueId());
        send(target, "Messages.Party-Kicked", Map.of("<player>", actor.getName()),
                "&cYou were kicked from the party by <player>.");
        broadcast(party, "Messages.Party-Kick-Broadcast", Map.of("<player>", target.getName()),
                "&c<player> was removed from the party.");
        return true;
    }

    public boolean promote(Player leader, Player target)
    {
        Party party = leaderParty(leader);
        if (party == null || target == null || !party.contains(target.getUniqueId()))
            return fail(leader, "Messages.Party-Player-Not-Member", "&cThat player is not in your party.");
        try { party.promote(target.getUniqueId()); }
        catch (IllegalStateException exception) { return fail(leader, "Messages.Party-Cannot-Promote", "&cThat player cannot be promoted."); }
        broadcast(party, "Messages.Party-Promoted", Map.of("<player>", target.getName()),
                "&a<player> was promoted to moderator.");
        return true;
    }

    public boolean demote(Player leader, Player target)
    {
        Party party = leaderParty(leader);
        if (party == null || target == null || !party.contains(target.getUniqueId()))
            return fail(leader, "Messages.Party-Player-Not-Member", "&cThat player is not in your party.");
        try { party.demote(target.getUniqueId()); }
        catch (IllegalStateException exception) { return fail(leader, "Messages.Party-Cannot-Demote", "&cThat player cannot be demoted."); }
        broadcast(party, "Messages.Party-Demoted", Map.of("<player>", target.getName()),
                "&e<player> was demoted to member.");
        return true;
    }

    public boolean transfer(Player leader, Player target)
    {
        Party party = leaderParty(leader);
        if (party == null || target == null || !party.contains(target.getUniqueId()))
            return fail(leader, "Messages.Party-Player-Not-Member", "&cThat player is not in your party.");
        try { party.transfer(target.getUniqueId()); }
        catch (IllegalStateException exception) { return fail(leader, "Messages.Party-Cannot-Transfer", "&cLeadership cannot be transferred to that player."); }
        broadcast(party, "Messages.Party-Transferred", Map.of("<player>", target.getName()),
                "&e<player> is now the party leader.");
        plugin.getDuelMenuManager().giveOpeners(leader);
        plugin.getDuelMenuManager().giveOpeners(target);
        return true;
    }

    public boolean leave(Player player)
    {
        Party party = required(player);
        if (party == null) return false;
        if (party.leader().equals(player.getUniqueId()))
            return fail(player, "Messages.Party-Leader-Cannot-Leave",
                    "&cTransfer leadership or disband the party first.");
        party.remove(player.getUniqueId());
        byMember.remove(player.getUniqueId());
        broadcast(party, "Messages.Party-Left", Map.of("<player>", player.getName()),
                "&e<player> left the party.");
        send(player, "Messages.Party-You-Left", Map.of(), "&cYou left the party.");
        return true;
    }

    public boolean disband(Player leader)
    {
        Party party = leaderParty(leader);
        if (party == null) return false;
        broadcast(party, "Messages.Party-Disbanded", Map.of("<player>", leader.getName()),
                "&cThe party was disbanded by <player>.");
        party.members().forEach(byMember::remove);
        invites.entrySet().removeIf(entry -> entry.getValue().party() == party);
        return true;
    }

    public boolean toggleVisibility(Player leader)
    {
        Party party = leaderParty(leader);
        if (party == null) return false;
        boolean visible = party.toggleVisible();
        broadcast(party, "Messages.Party-Visibility", Map.of(
                        "<visibility>", visible ? "visible" : "private", "<visible>", visible),
                "&eParty visibility is now <visibility>.");
        return true;
    }

    public void broadcastAction(Party party, String action)
    {
        broadcast(party, "Messages.Party-Action", Map.of("<action>", action),
                "&aParty action started: &e<action>&a.");
    }

    public Player exactOnline(String name)
    {
        if (name == null) return null;
        return Bukkit.getOnlinePlayers().stream()
                .filter(player -> player.getName().equalsIgnoreCase(name)).findFirst().orElse(null);
    }

    public void shutdown()
    {
        invites.clear();
        byMember.clear();
    }

    private Party required(Player player)
    {
        Party party = byMember.get(player.getUniqueId());
        if (party == null) fail(player, "Messages.Party-Not-In-Party", "&cYou are not in a party.");
        return party;
    }

    private Party leaderParty(Player player)
    {
        Party party = required(player);
        if (party != null && !party.leader().equals(player.getUniqueId()))
        {
            fail(player, "Messages.Party-Leader-Only", "&cOnly the party leader can do that.");
            return null;
        }
        return party;
    }

    private boolean canManage(Party party, UUID player)
    {
        PartyRole role = party.role(player);
        return role != null && role.canManageMembers();
    }

    private Invite validInvite(UUID target)
    {
        Invite invite = invites.get(target);
        if (invite != null && invite.expiresAt() <= System.currentTimeMillis())
        {
            invites.remove(target);
            return null;
        }
        return invite;
    }

    private void broadcast(Party party, String path, Map<String, ?> replacements, String fallback)
    {
        Map<String, Object> values = new LinkedHashMap<>(replacements);
        values.put("<leader>", name(party.leader()));
        values.put("<party_size>", party.size());
        for (UUID member : party.members())
        {
            Player online = Bukkit.getPlayer(member);
            if (online != null) send(online, path, values, fallback);
        }
    }

    private boolean fail(Player player, String path, String fallback)
    {
        send(player, path, Map.of(), fallback);
        return false;
    }

    private void send(Player player, String path, Map<String, ?> replacements, String fallback)
    {
        MessageService.send(player, plugin.getConfig(), path, replacements, fallback);
    }

    private String name(UUID player)
    {
        Player online = Bukkit.getPlayer(player);
        return online == null ? player.toString() : online.getName();
    }

    private record Invite(Party party, UUID inviter, long expiresAt) {}
}
