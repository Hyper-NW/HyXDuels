package me.alphatct3209.duels.game.modes;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

/** Bukkit-free state captured once per match. All mutations reject outsiders and terminal re-entry. */
public final class ModeRuntimeState
{
    private final UUID first;
    private final UUID second;
    private final int targetScore;
    private final Map<UUID, Integer> scores = new LinkedHashMap<>();
    private final Map<UUID, Boolean> beds = new LinkedHashMap<>();
    private final Map<UUID, Integer> checkpoints = new LinkedHashMap<>();
    private final Map<UUID, Integer> deaths = new LinkedHashMap<>();
    private ModeAction terminal;

    public ModeRuntimeState(UUID first, UUID second, int targetScore)
    {
        this.first = Objects.requireNonNull(first, "first");
        this.second = Objects.requireNonNull(second, "second");
        if (first.equals(second)) throw new IllegalArgumentException("Participants must differ");
        if (targetScore < 1) throw new IllegalArgumentException("targetScore must be positive");
        this.targetScore = targetScore;
        scores.put(first, 0); scores.put(second, 0);
        beds.put(first, true); beds.put(second, true);
        checkpoints.put(first, 0); checkpoints.put(second, 0);
        deaths.put(first, 0); deaths.put(second, 0);
    }

    public boolean participant(UUID player) { return first.equals(player) || second.equals(player); }
    public UUID opponent(UUID player) { require(player); return first.equals(player) ? second : first; }
    public int score(UUID player) { require(player); return scores.get(player); }
    public int addScore(UUID player)
    {
        requireOpen(player);
        int next = scores.get(player) == Integer.MAX_VALUE ? Integer.MAX_VALUE : scores.get(player) + 1;
        scores.put(player, next); return next;
    }
    public boolean reachedTarget(UUID player) { return score(player) >= targetScore; }
    public int targetScore() { return targetScore; }
    public boolean bedAlive(UUID player) { require(player); return beds.get(player); }
    public boolean breakBed(UUID owner) { requireOpen(owner); return beds.replace(owner, true, false); }
    public int checkpoint(UUID player) { require(player); return checkpoints.get(player); }
    public int deaths(UUID player) { require(player); return deaths.get(player); }
    public int recordDeath(UUID player)
    {
        requireOpen(player);
        int next = deaths.get(player) == Integer.MAX_VALUE ? Integer.MAX_VALUE : deaths.get(player) + 1;
        deaths.put(player, next); return next;
    }
    public boolean checkpoint(UUID player, int orderedIndex)
    {
        requireOpen(player);
        if (orderedIndex != checkpoints.get(player) + 1) return false;
        checkpoints.put(player, orderedIndex); return true;
    }
    public Optional<UUID> scoreLeader()
    {
        int one = score(first), two = score(second);
        return one == two ? Optional.empty() : Optional.of(one > two ? first : second);
    }
    public Optional<UUID> checkpointLeader()
    {
        int one = checkpoint(first), two = checkpoint(second);
        return one == two ? Optional.empty() : Optional.of(one > two ? first : second);
    }
    public ModeAction finish(ModeAction result)
    {
        Objects.requireNonNull(result, "result");
        if (!result.terminal()) throw new IllegalArgumentException("Only terminal actions may finish state");
        if (terminal == null) terminal = result;
        return terminal;
    }
    public Optional<ModeAction> terminal() { return Optional.ofNullable(terminal); }
    public UUID first() { return first; }
    public UUID second() { return second; }

    private void require(UUID player)
    {
        if (!participant(player)) throw new IllegalArgumentException("Unknown duel participant");
    }
    private void requireOpen(UUID player)
    {
        require(player);
        if (terminal != null) throw new IllegalStateException("Match is already terminal");
    }
}
