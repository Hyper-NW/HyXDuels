package me.alphatct3209.duels.social;

import me.alphatct3209.duels.Duels;
import me.alphatct3209.duels.configuration.PluginFiles;
import me.alphatct3209.duels.utils.MessageService;
import org.bukkit.Bukkit;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import me.clip.placeholderapi.PlaceholderAPI;

import java.io.File;
import java.io.IOException;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

/** Owns persistent preferences, friendships, names, and pending friend requests. */
public final class SocialManager implements Listener
{
    private final Duels plugin;
    private final File file;
    private final YamlConfiguration yaml;
    private final Map<UUID, PlayerPreferences> preferences = new HashMap<>();
    private final FriendRelationships friends = new FriendRelationships();
    private final FriendRelationships bestFriends = new FriendRelationships();
    private final Map<UUID, Set<UUID>> incomingRequests = new HashMap<>();
    private final Map<String, UUID> names = new HashMap<>();

    public SocialManager(Duels plugin)
    {
        this.plugin = Objects.requireNonNull(plugin);
        file = PluginFiles.data(plugin, "player-data.yml", false);
        yaml = YamlConfiguration.loadConfiguration(file);
        load();
    }

    public PlayerPreferences preferences(UUID player)
    {
        return preferences.computeIfAbsent(player, ignored -> PlayerPreferences.defaults());
    }

    public PlayerPreferences cycle(UUID player, PlayerSetting setting)
    {
        PlayerPreferences changed = preferences(player).cycle(setting);
        preferences.put(player, changed);
        savePlayer(player);
        return changed;
    }

    public boolean isFriend(UUID first, UUID second)
    {
        return friends.contains(first, second);
    }

    public Set<UUID> friends(UUID player)
    {
        return friends.get(player);
    }

    public Set<UUID> bestFriends(UUID player)
    {
        return bestFriends.get(player);
    }

    public boolean isBestFriend(UUID first, UUID second)
    {
        return bestFriends.contains(first, second);
    }

    public boolean toggleBestFriend(Player player, String friendName)
    {
        UUID friend = findKnown(friendName);
        UUID owner = player.getUniqueId();
        if (friend == null || !isFriend(owner, friend))
            return fail(player, "Messages.Friend-Not-Friend", "&cThat player is not your friend.");
        boolean enabled;
        if (bestFriends.contains(owner, friend))
        {
            bestFriends.remove(owner, friend);
            enabled = false;
        }
        else
        {
            bestFriends.add(owner, friend);
            enabled = true;
        }
        savePlayer(owner);
        savePlayer(friend);
        send(player, "Messages.Best-Friend-Changed",
                Map.of("<player>", name(friend), "<status>", enabled ? "enabled" : "disabled"),
                "&aBest-friend status for &e<player> &ais now &e<status>&a.");
        return true;
    }

    public Set<UUID> guildMembers(UUID viewer)
    {
        if (!Bukkit.getPluginManager().isPluginEnabled("PlaceholderAPI")) return Set.of(viewer);
        YamlConfiguration social = YamlConfiguration.loadConfiguration(
                PluginFiles.advanced(plugin, "social.yml"));
        String placeholder = social.getString("Leaderboard-Filters.Guild-Placeholder", "").trim();
        if (placeholder.isEmpty()) return Set.of(viewer);
        String guild = PlaceholderAPI.setPlaceholders(Bukkit.getOfflinePlayer(viewer), placeholder).trim();
        if (guild.isEmpty()) return Set.of(viewer);
        Set<UUID> result = new LinkedHashSet<>();
        for (UUID candidate : knownPlayers())
        {
            String candidateGuild = PlaceholderAPI.setPlaceholders(
                    Bukkit.getOfflinePlayer(candidate), placeholder).trim();
            if (guild.equalsIgnoreCase(candidateGuild)) result.add(candidate);
        }
        result.add(viewer);
        return Set.copyOf(result);
    }

    public Set<UUID> knownPlayers()
    {
        Set<UUID> result = new LinkedHashSet<>(preferences.keySet());
        result.addAll(names.values());
        return Set.copyOf(result);
    }

    public boolean allowsDuelRequest(UUID sender, UUID target)
    {
        return allows(preferences(target).duelRequests(), sender, target);
    }

    public boolean allowsDirectMessage(UUID sender, UUID target)
    {
        return allows(preferences(target).directMessages(), sender, target);
    }

    public boolean allowsPartyInvite(UUID sender, UUID target)
    {
        return allows(preferences(target).partyInvites(), sender, target);
    }

    public boolean sendFriendRequest(Player sender, Player target)
    {
        if (target == null || sender.equals(target))
            return fail(sender, "Messages.Friend-Player-Unavailable", "&cThat player is unavailable.");
        if (!preferences(target.getUniqueId()).friendRequests())
            return fail(sender, "Messages.Friend-Requests-Disabled", "&cThat player has friend requests disabled.");
        if (isFriend(sender.getUniqueId(), target.getUniqueId()))
            return fail(sender, "Messages.Friend-Already", "&cYou are already friends.");
        Set<UUID> pending = incomingRequests.computeIfAbsent(target.getUniqueId(), ignored -> new LinkedHashSet<>());
        if (!pending.add(sender.getUniqueId()))
            return fail(sender, "Messages.Friend-Request-Duplicate", "&cThat friend request is already pending.");
        saveRequests(target.getUniqueId());
        send(sender, "Messages.Friend-Request-Sent", Map.of("<player>", target.getName()),
                "&aFriend request sent to &e<player>&a.");
        send(target, "Messages.Friend-Request-Received", Map.of("<player>", sender.getName()),
                "&e<player> &asent you a friend request. Use &e/friend accept <player>&a.");
        return true;
    }

    public boolean acceptFriend(Player target, String senderName)
    {
        UUID senderId = findKnown(senderName);
        Set<UUID> pending = incomingRequests.getOrDefault(target.getUniqueId(), Set.of());
        if (senderId == null || !pending.contains(senderId))
            return fail(target, "Messages.Friend-Request-None", "&cNo friend request from that player.");
        friends.add(target.getUniqueId(), senderId);
        incomingRequests.get(target.getUniqueId()).remove(senderId);
        savePlayer(target.getUniqueId());
        savePlayer(senderId);
        saveRequests(target.getUniqueId());
        String senderDisplay = name(senderId);
        send(target, "Messages.Friend-Accepted", Map.of("<player>", senderDisplay),
                "&aYou are now friends with &e<player>&a.");
        Player sender = Bukkit.getPlayer(senderId);
        if (sender != null) send(sender, "Messages.Friend-Accepted", Map.of("<player>", target.getName()),
                "&aYou are now friends with &e<player>&a.");
        return true;
    }

    public boolean denyFriend(Player target, String senderName)
    {
        UUID senderId = findKnown(senderName);
        Set<UUID> pending = incomingRequests.get(target.getUniqueId());
        if (senderId == null || pending == null || !pending.remove(senderId))
            return fail(target, "Messages.Friend-Request-None", "&cNo friend request from that player.");
        saveRequests(target.getUniqueId());
        send(target, "Messages.Friend-Request-Denied", Map.of("<player>", name(senderId)),
                "&cFriend request denied.");
        return true;
    }

    public boolean removeFriend(Player player, String friendName)
    {
        UUID friendId = findKnown(friendName);
        if (friendId == null || !isFriend(player.getUniqueId(), friendId))
            return fail(player, "Messages.Friend-Not-Friend", "&cThat player is not your friend.");
        friends.remove(player.getUniqueId(), friendId);
        bestFriends.remove(player.getUniqueId(), friendId);
        savePlayer(player.getUniqueId());
        savePlayer(friendId);
        send(player, "Messages.Friend-Removed", Map.of("<player>", name(friendId)),
                "&cRemoved &e<player> &cfrom your friends.");
        return true;
    }

    public Player exactOnline(String name)
    {
        if (name == null) return null;
        return Bukkit.getOnlinePlayers().stream()
                .filter(player -> player.getName().equalsIgnoreCase(name)).findFirst().orElse(null);
    }

    public String name(UUID uuid)
    {
        Player online = Bukkit.getPlayer(uuid);
        if (online != null) return online.getName();
        String stored = yaml.getString("Players." + uuid + ".Last-Name");
        return stored == null ? uuid.toString() : stored;
    }

    public void shutdown() { save(); }

    @EventHandler
    public void onJoin(PlayerJoinEvent event)
    {
        Player joined = event.getPlayer();
        names.put(joined.getName().toLowerCase(Locale.ROOT), joined.getUniqueId());
        yaml.set("Players." + joined.getUniqueId() + ".Last-Name", joined.getName());
        preferences(joined.getUniqueId());
        savePlayer(joined.getUniqueId());
        for (UUID friendId : friends(joined.getUniqueId()))
        {
            Player friend = Bukkit.getPlayer(friendId);
            if (friend != null && preferences(friendId).friendJoinNotifier())
                send(friend, "Messages.Friend-Joined", Map.of("<player>", joined.getName()),
                        "&aFriend &e<player> &ajoined the server.");
        }
    }

    private boolean allows(Privacy privacy, UUID sender, UUID target)
    {
        return privacy == Privacy.ANYONE || isFriend(sender, target);
    }

    private UUID findKnown(String name)
    {
        if (name == null) return null;
        Player online = exactOnline(name);
        return online != null ? online.getUniqueId() : names.get(name.toLowerCase(Locale.ROOT));
    }

    private void load()
    {
        ConfigurationSection section = yaml.getConfigurationSection("Players");
        if (section == null) return;
        for (String rawUuid : section.getKeys(false))
        {
            UUID uuid;
            try { uuid = UUID.fromString(rawUuid); }
            catch (IllegalArgumentException ignored) { continue; }
            String path = "Players." + rawUuid;
            String lastName = yaml.getString(path + ".Last-Name");
            if (lastName != null) names.put(lastName.toLowerCase(Locale.ROOT), uuid);
            preferences.put(uuid, loadPreferences(path + ".Settings"));
            for (String rawFriend : yaml.getStringList(path + ".Friends"))
            {
                try { friends.loadOneWay(uuid, UUID.fromString(rawFriend)); }
                catch (IllegalArgumentException ignored) { }
            }
            for (String rawFriend : yaml.getStringList(path + ".Best-Friends"))
            {
                try { bestFriends.loadOneWay(uuid, UUID.fromString(rawFriend)); }
                catch (IllegalArgumentException ignored) { }
            }
            for (String rawRequest : yaml.getStringList(path + ".Incoming-Friend-Requests"))
            {
                try { incomingRequests.computeIfAbsent(uuid, ignored -> new LinkedHashSet<>())
                        .add(UUID.fromString(rawRequest)); }
                catch (IllegalArgumentException ignored) { }
            }
        }
        // Heal one-sided legacy/manual friend entries in memory and on next save.
        friends.healSymmetry();
        bestFriends.healSymmetry();
    }

    private PlayerPreferences loadPreferences(String path)
    {
        PlayerPreferences defaults = PlayerPreferences.defaults();
        return new PlayerPreferences(
                yaml.getBoolean(path + ".Show-Own-Tier", defaults.showOwnTier()),
                yaml.getBoolean(path + ".Scoreboard", defaults.scoreboard()),
                yaml.getBoolean(path + ".Profile-Kits-Public", defaults.profileKitsPublic()),
                yaml.getBoolean(path + ".Friend-Join-Notifier", defaults.friendJoinNotifier()),
                yaml.getBoolean(path + ".Blast-Particles", defaults.blastParticles()),
                privacy(path + ".Duel-Requests", defaults.duelRequests()),
                privacy(path + ".Direct-Messages", defaults.directMessages()),
                privacy(path + ".Party-Invites", defaults.partyInvites()),
                yaml.getBoolean(path + ".Friend-Requests", defaults.friendRequests()));
    }

    private Privacy privacy(String path, Privacy fallback)
    {
        try { return Privacy.valueOf(yaml.getString(path, fallback.name()).toUpperCase(Locale.ROOT)); }
        catch (IllegalArgumentException exception) { return fallback; }
    }

    private void savePlayer(UUID uuid)
    {
        String path = "Players." + uuid;
        PlayerPreferences value = preferences(uuid);
        yaml.set(path + ".Settings.Show-Own-Tier", value.showOwnTier());
        yaml.set(path + ".Settings.Scoreboard", value.scoreboard());
        yaml.set(path + ".Settings.Profile-Kits-Public", value.profileKitsPublic());
        yaml.set(path + ".Settings.Friend-Join-Notifier", value.friendJoinNotifier());
        yaml.set(path + ".Settings.Blast-Particles", value.blastParticles());
        yaml.set(path + ".Settings.Duel-Requests", value.duelRequests().name());
        yaml.set(path + ".Settings.Direct-Messages", value.directMessages().name());
        yaml.set(path + ".Settings.Party-Invites", value.partyInvites().name());
        yaml.set(path + ".Settings.Friend-Requests", value.friendRequests());
        yaml.set(path + ".Friends", friends(uuid).stream().map(UUID::toString).sorted().toList());
        yaml.set(path + ".Best-Friends", bestFriends(uuid).stream().map(UUID::toString).sorted().toList());
        save();
    }

    private void saveRequests(UUID uuid)
    {
        yaml.set("Players." + uuid + ".Incoming-Friend-Requests",
                incomingRequests.getOrDefault(uuid, Set.of()).stream().map(UUID::toString).sorted().toList());
        save();
    }

    private void save()
    {
        File temporary = new File(file.getParentFile(), file.getName() + ".tmp");
        try
        {
            file.getParentFile().mkdirs();
            yaml.save(temporary);
            try
            {
                Files.move(temporary.toPath(), file.toPath(), StandardCopyOption.REPLACE_EXISTING,
                        StandardCopyOption.ATOMIC_MOVE);
            }
            catch (AtomicMoveNotSupportedException exception)
            {
                Files.move(temporary.toPath(), file.toPath(), StandardCopyOption.REPLACE_EXISTING);
            }
        }
        catch (IOException exception)
        {
            temporary.delete();
            throw new IllegalStateException("Could not save player-data.yml", exception);
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
}
