package me.alphatct3209.duels.game.modes;

import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

/** Compatibility facade over the unified mode runtime state. */
public final class ModeObjective
{
    private final DuelMode mode;
    private final ModeRuntimeState state;
    public ModeObjective(DuelMode mode, UUID first, UUID second)
    {
        this(mode, new ModeRuntimeState(first, second, mode.targetScore()));
    }
    public ModeObjective(DuelMode mode, ModeRuntimeState state)
    {
        this.mode = Objects.requireNonNull(mode, "mode");
        this.state = Objects.requireNonNull(state, "state");
    }
    public boolean score(UUID player)
    {
        if (state.terminal().isPresent()) return false;
        state.addScore(player);
        if (state.reachedTarget(player)) state.finish(ModeAction.win(player));
        return true;
    }
    public boolean destroyBed(UUID owner)
    {
        if (mode.handlerType() != ModeHandlerType.BED_WARS || state.terminal().isPresent()) return false;
        return state.breakBed(owner);
    }
    public Optional<UUID> eliminate(UUID player)
    {
        if (state.terminal().isPresent()) return state.terminal().flatMap(ModeAction::winnerOptional);
        if (mode.handlerType() == ModeHandlerType.BED_WARS && state.bedAlive(player)) return Optional.empty();
        return state.finish(ModeAction.win(state.opponent(player))).winnerOptional();
    }
    public Optional<UUID> timeoutWinner()
    {
        if (state.terminal().isPresent()) return state.terminal().flatMap(ModeAction::winnerOptional);
        return mode.durationPolicy().timeoutPolicy() == ModeDurationPolicy.TimeoutPolicy.HIGHEST_SCORE
                ? state.scoreLeader() : Optional.empty();
    }
    public int scoreOf(UUID player) { return state.score(player); }
    public boolean hasBed(UUID player) { return state.bedAlive(player); }
    public Optional<UUID> winner() { return state.terminal().flatMap(ModeAction::winnerOptional); }
    public ModeRuntimeState state() { return state; }
}
