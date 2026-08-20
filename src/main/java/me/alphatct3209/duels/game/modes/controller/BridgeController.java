package me.alphatct3209.duels.game.modes.controller;

import me.alphatct3209.duels.game.modes.ModeAction;
import me.alphatct3209.duels.game.modes.ModeController;
import me.alphatct3209.duels.game.modes.ModeHandlerType;
import me.alphatct3209.duels.game.modes.ModeRuntimeState;

import java.util.Set;
import java.util.UUID;

public final class BridgeController implements ModeController
{
    private static final Set<String> REQUIRED_POINTS = Set.of("goal_1", "goal_2");

    @Override public ModeHandlerType type() { return ModeHandlerType.BRIDGE; }
    @Override public Set<String> requiredPoints() { return REQUIRED_POINTS; }
    @Override public boolean goalBased() { return true; }
    @Override public ModeAction death(ModeRuntimeState state, UUID victim)
    { return ModeAction.respawn(victim); }

    @Override
    public ModeAction goal(ModeRuntimeState state, UUID scorer)
    {
        state.addScore(scorer);
        return state.reachedTarget(scorer)
                ? state.finish(ModeAction.win(scorer)) : ModeAction.roundReset();
    }
}
