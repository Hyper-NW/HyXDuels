package me.alphatct3209.duels.party;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

public final class Party
{
    private UUID leader;
    private final LinkedHashMap<UUID, PartyRole> members = new LinkedHashMap<>();
    private boolean visible = true;

    Party(UUID leader)
    {
        this.leader = Objects.requireNonNull(leader, "leader");
        members.put(leader, PartyRole.LEADER);
    }

    public UUID leader() { return leader; }
    public boolean visible() { return visible; }
    public int size() { return members.size(); }
    public Set<UUID> members() { return Set.copyOf(members.keySet()); }
    public Map<UUID, PartyRole> roles() { return Map.copyOf(members); }
    public boolean contains(UUID player) { return members.containsKey(player); }
    public PartyRole role(UUID player) { return members.get(player); }

    void add(UUID player)
    {
        if (members.putIfAbsent(Objects.requireNonNull(player), PartyRole.MEMBER) != null)
            throw new IllegalStateException("Player is already in this party");
    }

    void remove(UUID player)
    {
        if (leader.equals(player)) throw new IllegalStateException("The leader cannot leave without transfer or disband");
        members.remove(player);
    }

    void promote(UUID player)
    {
        if (members.get(player) != PartyRole.MEMBER)
            throw new IllegalStateException("Only party members can be promoted");
        members.put(player, PartyRole.MODERATOR);
    }

    void demote(UUID player)
    {
        if (members.get(player) != PartyRole.MODERATOR)
            throw new IllegalStateException("Only party moderators can be demoted");
        members.put(player, PartyRole.MEMBER);
    }

    void transfer(UUID player)
    {
        if (!members.containsKey(player) || leader.equals(player))
            throw new IllegalStateException("The new leader must be another party member");
        members.put(leader, PartyRole.MODERATOR);
        leader = player;
        members.put(player, PartyRole.LEADER);
    }

    boolean toggleVisible()
    {
        visible = !visible;
        return visible;
    }
}
