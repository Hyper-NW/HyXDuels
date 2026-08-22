package me.alphatct3209.duels.queue;

import me.alphatct3209.duels.Duels;
import me.alphatct3209.duels.game.arenas.Arena;
import me.alphatct3209.duels.game.kits.Kit;
import me.alphatct3209.duels.game.modes.DuelMode;
import me.alphatct3209.duels.game.modes.DuelSelection;
import me.alphatct3209.duels.game.modes.ModeKey;
import me.alphatct3209.duels.utils.MessageService;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;

import java.util.ArrayDeque;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

/** Synchronous one-versus-one matchmaking; a queue key fully captures duel behavior. */
public final class DuelQueueManager implements Listener
{
    private final Duels plugin;
    private final Map<QueueKey, ArrayDeque<UUID>> queues = new HashMap<>();
    private final Map<UUID, QueueKey> queuedPlayers = new HashMap<>();

    public DuelQueueManager(Duels plugin)
    {
        this.plugin = Objects.requireNonNull(plugin, "plugin");
    }

    public boolean join(Player player, DuelSelection selection)
    {
        return join(player, selection, null);
    }

    public boolean join(Player player, DuelSelection selection, Integer preferredArenaId)
    {
        if (!player.hasPermission("duels.queue"))
        {
            send(player, "Messages.Queue-No-Permission", Map.of(), "&cYou cannot join duel queues.");
            return false;
        }
        if (plugin.getArenaManager().getArena(player) != null)
        {
            send(player, "Messages.Queue-In-Arena", Map.of(), "&cYou are already in an arena.");
            return false;
        }
        leave(player.getUniqueId(), false);
        QueueKey key = QueueKey.from(selection, preferredArenaId);
        ArrayDeque<UUID> queue = queues.computeIfAbsent(key, ignored -> new ArrayDeque<>());
        Player opponent = pollOpponent(queue, player.getUniqueId());
        if (opponent == null)
        {
            queue.addLast(player.getUniqueId());
            queuedPlayers.put(player.getUniqueId(), key);
            DuelMode mode = plugin.getSelectionService().mode(selection);
            Kit kit = plugin.getSelectionService().kit(selection);
            send(player, "Messages.Queue-Joined", values(mode, kit, selection),
                    "&aQueued for &e<mode> &7(<kit>, <combat>)&a.");
            return true;
        }

        queuedPlayers.remove(opponent.getUniqueId());
        Arena arena;
        if (key.preferredArenaId() == null)
        {
            arena = plugin.getArenaManager().findCompletelyEmptyCompatibleArena(selection);
        }
        else
        {
            arena = plugin.getArenaManager().getArena(key.preferredArenaId());
            if (arena != null && (!arena.isCompletelyEmpty()
                    || !plugin.getArenaManager().canAdmit(arena, selection,
                    plugin.getSelectionService().kit(selection)))) arena = null;
        }
        Kit kit = plugin.getSelectionService().kit(selection);
        if (arena == null || !plugin.getArenaManager().admitChallenge(
                opponent, player, arena, selection, kit, kit))
        {
            queue.addFirst(opponent.getUniqueId());
            queuedPlayers.put(opponent.getUniqueId(), key);
            queue.addLast(player.getUniqueId());
            queuedPlayers.put(player.getUniqueId(), key);
            send(player, "Messages.Queue-Waiting-Arena", Map.of(),
                    "&eA match was found, but no compatible arena is ready. You remain queued.");
            return true;
        }

        DuelMode mode = plugin.getSelectionService().mode(selection);
        Map<String, Object> playerValues = new HashMap<>(values(mode, kit, selection));
        playerValues.put("<opponent>", opponent.getName());
        playerValues.put("<arena>", arena.getName());
        Map<String, Object> opponentValues = new HashMap<>(values(mode, kit, selection));
        opponentValues.put("<opponent>", player.getName());
        opponentValues.put("<arena>", arena.getName());
        send(player, "Messages.Queue-Matched", playerValues,
                "&aMatched against &e<opponent> &ain &e<arena>&a!");
        send(opponent, "Messages.Queue-Matched", opponentValues,
                "&aMatched against &e<opponent> &ain &e<arena>&a!");
        return true;
    }

    public boolean leave(Player player)
    {
        boolean removed = leave(player.getUniqueId(), true);
        if (!removed) send(player, "Messages.Queue-Not-Queued", Map.of(), "&cYou are not queued.");
        return removed;
    }

    public boolean isQueued(UUID player)
    {
        return queuedPlayers.containsKey(player);
    }

    public int queuedCount(ModeKey mode)
    {
        Objects.requireNonNull(mode, "mode");
        return Math.toIntExact(queuedPlayers.values().stream()
                .filter(key -> key.mode().equals(mode)).count());
    }

    public boolean cancel(UUID player)
    {
        return leave(player, false);
    }

    public void shutdown()
    {
        queues.clear();
        queuedPlayers.clear();
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event)
    {
        leave(event.getPlayer().getUniqueId(), false);
    }

    private boolean leave(UUID player, boolean notify)
    {
        QueueKey key = queuedPlayers.remove(player);
        if (key == null) return false;
        ArrayDeque<UUID> queue = queues.get(key);
        if (queue != null)
        {
            queue.remove(player);
            if (queue.isEmpty()) queues.remove(key);
        }
        if (notify)
        {
            Player online = Bukkit.getPlayer(player);
            if (online != null) send(online, "Messages.Queue-Left", Map.of(), "&cYou left the duel queue.");
        }
        return true;
    }

    private Player pollOpponent(ArrayDeque<UUID> queue, UUID joining)
    {
        while (!queue.isEmpty())
        {
            UUID candidateId = queue.removeFirst();
            queuedPlayers.remove(candidateId);
            Player candidate = Bukkit.getPlayer(candidateId);
            if (!candidateId.equals(joining) && candidate != null && candidate.isOnline()
                    && plugin.getArenaManager().getArena(candidate) == null)
                return candidate;
        }
        return null;
    }

    private Map<String, Object> values(DuelMode mode, Kit kit, DuelSelection selection)
    {
        return Map.of("<mode>", mode.displayName(), "<mode_key>", mode.key().value(),
                "<kit>", kit.getName(), "<kit_key>", kit.getKey(),
                "<legacy_pvp>", selection.legacyPvp(),
                "<combat>", selection.legacyPvp() ? "Legacy 1.8" : "Modern");
    }

    private void send(Player player, String path, Map<String, ?> replacements, String fallback)
    {
        MessageService.send(player, plugin.getConfig(), path, replacements, fallback);
    }

    public record QueueKey(ModeKey mode, String kit, boolean legacyPvp, Integer preferredArenaId)
    {
        public QueueKey(ModeKey mode, String kit, boolean legacyPvp)
        {
            this(mode, kit, legacyPvp, null);
        }

        public QueueKey
        {
            Objects.requireNonNull(mode, "mode");
            Objects.requireNonNull(kit, "kit");
        }

        public static QueueKey from(DuelSelection selection)
        {
            return from(selection, null);
        }

        public static QueueKey from(DuelSelection selection, Integer preferredArenaId)
        {
            return new QueueKey(selection.modeKey(), selection.kitKey(),
                    selection.legacyPvp(), preferredArenaId);
        }
    }
}
