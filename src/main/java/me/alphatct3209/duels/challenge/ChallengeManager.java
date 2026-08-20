package me.alphatct3209.duels.challenge;

import me.alphatct3209.duels.Duels;
import me.alphatct3209.duels.game.arenas.Arena;
import me.alphatct3209.duels.game.kits.Kit;
import me.alphatct3209.duels.game.modes.DuelMode;
import me.alphatct3209.duels.game.modes.DuelSelection;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;

import java.util.List;
import java.util.Objects;
import java.util.UUID;

public final class ChallengeManager implements Listener
{
    private final Duels plugin;
    private final ChallengeState state = new ChallengeState();
    private final ChallengeGui gui;
    private final long timeoutMillis;

    public ChallengeManager(Duels plugin)
    {
        this.plugin = Objects.requireNonNull(plugin, "plugin");
        int seconds = Math.max(1, plugin.getConfig().getInt("Challenges.Timeout-Seconds", 60));
        this.timeoutMillis = seconds * 1000L;
        this.gui = new ChallengeGui(plugin, this);
    }

    public ChallengeGui getGui()
    {
        return gui;
    }

    public Player findExactOnline(String name)
    {
        for (Player player : Bukkit.getOnlinePlayers())
        {
            if (player.getName().equalsIgnoreCase(name))
            {
                return player;
            }
        }
        return null;
    }

    public boolean openSelection(Player challenger, Player target)
    {
        if (!validateParticipants(challenger, target, true))
        {
            return false;
        }
        plugin.getDuelMenuManager().openChallenge(challenger, target);
        return true;
    }

    public boolean send(Player challenger, Player target, Kit kit)
    {
        return send(challenger, target, kit, null);
    }

    public boolean send(Player challenger, Player target, Kit kit, Integer preferredArenaId)
    {
        expireDue();
        if (!validateParticipants(challenger, target, true))
        {
            return false;
        }
        Kit current = plugin.getKitManager().getKitByCanonicalKey(kit.getKey());
        DuelSelection base = plugin.getSelectionService().resolve(challenger.getUniqueId());
        DuelMode mode = plugin.getModeManager().require(base.modeKey());
        final DuelSelection selection;
        try
        {
            selection = plugin.getSelectionService().create(mode, kit.getKey(), base.legacyPvp());
        }
        catch (IllegalArgumentException exception)
        {
            message(challenger, "Messages.Challenge-Kit-Unavailable", "<kit>", kit.getName());
            return false;
        }
        if (current != kit)
        {
            message(challenger, "Messages.Challenge-Kit-Unavailable", "<kit>", kit.getName());
            return false;
        }
        Arena preferred = preferredArenaId == null ? null : plugin.getArenaManager().getArena(preferredArenaId);
        if (preferredArenaId != null && (preferred == null || !preferred.readyFor(mode)
                || !plugin.getArenaManager().isCompatible(preferred, mode.key())))
        {
            message(challenger, "Messages.Challenge-No-Arena");
            return false;
        }

        long expiresAt = System.currentTimeMillis() + timeoutMillis;
        ChallengeState.Pending pending;
        try
        {
            pending = state.create(challenger.getUniqueId(), target.getUniqueId(),
                    selection.modeKey().value(), selection.kitKey(), preferredArenaId,
                    selection.legacyPvp(), expiresAt);
        }
        catch (IllegalStateException exception)
        {
            pendingConflict(challenger, target);
            return false;
        }

        String arenaName = preferred == null ? "Any" : preferred.getName();
        message(challenger, "Messages.Challenge-Sent", "<player>", target.getName(),
                "<kit>", kit.getName(), "<arena>", arenaName,
                "<mode>", mode.displayName(), "<mode_key>", mode.key().value(),
                "<legacy_pvp>", Boolean.toString(selection.legacyPvp()),
                "<combat>", selection.legacyPvp() ? "Legacy 1.8" : "Modern");
        message(target, "Messages.Challenge-Received", "<player>", challenger.getName(),
                "<kit>", kit.getName(), "<arena>", arenaName,
                "<mode>", mode.displayName(), "<mode_key>", mode.key().value(),
                "<legacy_pvp>", Boolean.toString(selection.legacyPvp()),
                "<combat>", selection.legacyPvp() ? "Legacy 1.8" : "Modern");
        scheduleExpiry(pending);
        return true;
    }

    public boolean accept(Player target)
    {
        if (!target.hasPermission("duels.accept"))
        {
            message(target, "Messages.No-Permission");
            return false;
        }
        expireDue();
        ChallengeState.Pending pending = state.forTarget(target.getUniqueId());
        if (pending == null)
        {
            message(target, "Messages.Challenge-None");
            return false;
        }

        Player challenger = Bukkit.getPlayer(pending.challenger());
        if (challenger == null || !challenger.isOnline() || !target.isOnline())
        {
            message(target, "Messages.Challenge-Player-Offline");
            return false;
        }
        if (plugin.getArenaManager().getArena(challenger) != null
                || plugin.getArenaManager().getArena(target) != null)
        {
            message(target, "Messages.Challenge-Player-In-Arena");
            return false;
        }

        Kit kit = plugin.getKitManager().getKitByCanonicalKey(pending.kitKey());
        DuelMode mode = plugin.getModeManager().resolve(pending.modeKey()).orElse(null);
        if (kit == null || mode == null)
        {
            message(target, "Messages.Challenge-Kit-Unavailable", "<kit>", pending.kitKey());
            return false;
        }
        DuelSelection selection;
        try { selection = plugin.getSelectionService().create(mode, kit.getKey(), pending.legacyPvp()); }
        catch (IllegalArgumentException exception)
        {
            message(target, "Messages.Challenge-Kit-Unavailable", "<kit>", pending.kitKey());
            return false;
        }
        Arena arena;
        if (pending.arenaId() == null)
        {
            arena = plugin.getArenaManager().findCompletelyEmptyCompatibleArena(selection);
        }
        else
        {
            arena = plugin.getArenaManager().getArena(pending.arenaId());
            if (arena != null && (!arena.isCompletelyEmpty()
                    || !plugin.getArenaManager().canAdmit(arena, selection, kit)))
            {
                arena = null;
            }
        }
        if (arena == null)
        {
            message(target, "Messages.Challenge-No-Arena");
            return false;
        }

        // All Bukkit calls run on the server thread. Admission performs one final validation and
        // captures this exact Kit object for both players before the pending token is consumed.
        if (!plugin.getArenaManager().admitChallenge(challenger, target, arena,
                selection, kit, kit))
        {
            message(target, "Messages.Challenge-No-Arena");
            return false;
        }
        ChallengeState.Pending consumed = state.removeForTarget(target.getUniqueId(), pending.token());
        if (consumed == null)
        {
            throw new IllegalStateException("Challenge token changed during synchronous admission");
        }

        message(challenger, "Messages.Challenge-Accepted", "<player>", target.getName(),
                "<kit>", kit.getName(), "<arena>", arena.getName());
        message(target, "Messages.Challenge-Accepted", "<player>", challenger.getName(),
                "<kit>", kit.getName(), "<arena>", arena.getName());
        return true;
    }

    public boolean deny(Player target)
    {
        if (!target.hasPermission("duels.deny"))
        {
            message(target, "Messages.No-Permission");
            return false;
        }
        expireDue();
        ChallengeState.Pending pending = state.forTarget(target.getUniqueId());
        if (pending == null)
        {
            message(target, "Messages.Challenge-None");
            return false;
        }
        if (state.removeForTarget(target.getUniqueId(), pending.token()) == null)
        {
            return false;
        }
        Player challenger = Bukkit.getPlayer(pending.challenger());
        message(target, "Messages.Challenge-Denied", "<player>",
                challenger == null ? "Unknown" : challenger.getName());
        if (challenger != null)
        {
            message(challenger, "Messages.Challenge-Denied", "<player>", target.getName());
        }
        return true;
    }

    private boolean validateParticipants(Player challenger, Player target, boolean checkPending)
    {
        if (!challenger.hasPermission("duels.challenge"))
        {
            message(challenger, "Messages.No-Permission");
            return false;
        }
        if (challenger.equals(target))
        {
            message(challenger, "Messages.Challenge-Self");
            return false;
        }
        if (!challenger.isOnline() || target == null || !target.isOnline())
        {
            message(challenger, "Messages.Challenge-Player-Offline");
            return false;
        }
        if (!challenger.canSee(target))
        {
            message(challenger, "Messages.Challenge-Player-Not-Visible");
            return false;
        }
        if (plugin.getArenaManager().getArena(challenger) != null
                || plugin.getArenaManager().getArena(target) != null)
        {
            message(challenger, "Messages.Challenge-Player-In-Arena");
            return false;
        }
        if (plugin.getQueueManager().isQueued(challenger.getUniqueId())
                || plugin.getQueueManager().isQueued(target.getUniqueId()))
        {
            message(challenger, "Messages.Challenge-Player-In-Queue");
            return false;
        }
        if (!plugin.getSocialManager().allowsDuelRequest(
                challenger.getUniqueId(), target.getUniqueId()))
        {
            message(challenger, "Messages.Challenge-Friends-Only", "<player>", target.getName());
            return false;
        }
        if (checkPending && (state.forParticipant(challenger.getUniqueId()) != null
                || state.forParticipant(target.getUniqueId()) != null))
        {
            pendingConflict(challenger, target);
            return false;
        }
        return true;
    }

    private void pendingConflict(Player challenger, Player target)
    {
        ChallengeState.Pending existing = state.forParticipant(challenger.getUniqueId());
        if (existing == null)
        {
            existing = state.forParticipant(target.getUniqueId());
        }
        if (existing != null && ((existing.challenger().equals(challenger.getUniqueId())
                && existing.target().equals(target.getUniqueId()))
                || (existing.challenger().equals(target.getUniqueId())
                && existing.target().equals(challenger.getUniqueId()))))
        {
            message(challenger, "Messages.Challenge-Duplicate");
        }
        else
        {
            message(challenger, "Messages.Challenge-Busy");
        }
    }

    private void scheduleExpiry(ChallengeState.Pending pending)
    {
        long remaining = Math.max(1L, pending.expiresAt() - System.currentTimeMillis());
        long ticks = Math.max(1L, (remaining + 49L) / 50L);
        Bukkit.getScheduler().runTaskLater(plugin, () -> expire(pending.token()), ticks);
    }

    private void expire(UUID token)
    {
        long now = System.currentTimeMillis();
        ChallengeState.Pending expired = state.expire(token, now);
        if (expired != null)
        {
            notifyExpired(expired);
            return;
        }
        ChallengeState.Pending stillPending = state.forToken(token);
        if (stillPending != null && stillPending.expiresAt() > now)
        {
            scheduleExpiry(stillPending);
        }
    }

    private void expireDue()
    {
        for (ChallengeState.Pending pending : state.expireDue(System.currentTimeMillis()))
        {
            notifyExpired(pending);
        }
    }

    private void notifyExpired(ChallengeState.Pending pending)
    {
        Player challenger = Bukkit.getPlayer(pending.challenger());
        Player target = Bukkit.getPlayer(pending.target());
        if (challenger != null)
        {
            message(challenger, "Messages.Challenge-Expired");
        }
        if (target != null)
        {
            message(target, "Messages.Challenge-Expired");
        }
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event)
    {
        ChallengeState.Pending removed = state.removeParticipant(event.getPlayer().getUniqueId());
        if (removed == null)
        {
            return;
        }
        UUID otherId = removed.challenger().equals(event.getPlayer().getUniqueId())
                ? removed.target() : removed.challenger();
        Player other = Bukkit.getPlayer(otherId);
        if (other != null)
        {
            message(other, "Messages.Challenge-Cancelled-Quit", "<player>", event.getPlayer().getName());
        }
    }

    public void shutdown()
    {
        state.clear();
    }

    private void message(Player player, String path, String... replacements)
    {
        java.util.Map<String, String> values = new java.util.LinkedHashMap<>();
        for (int index = 0; index + 1 < replacements.length; index += 2)
            values.put(replacements[index], replacements[index + 1]);
        me.alphatct3209.duels.utils.MessageService.send(player, plugin.getConfig(), path, values);
    }
}
