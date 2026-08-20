package me.alphatct3209.duels.game.modes.controller;

import me.alphatct3209.duels.game.modes.ModeAction;
import me.alphatct3209.duels.game.modes.ModeController;
import me.alphatct3209.duels.game.modes.ModeHandlerType;
import me.alphatct3209.duels.game.modes.ModeRuntimeState;

import java.util.Set;
import java.util.UUID;

public final class BedWarsController implements ModeController
{
    private static final Set<String> REQUIRED_POINTS = Set.of(
            "bed_1", "bed_2", "generator_1", "generator_2", "shop_1", "shop_2");

    @Override public ModeHandlerType type() { return ModeHandlerType.BED_WARS; }
    @Override public Set<String> requiredPoints() { return REQUIRED_POINTS; }
    @Override public boolean ringout() { return true; }

    @Override
    public ModeAction death(ModeRuntimeState state, UUID victim)
    {
        return state.bedAlive(victim) ? ModeAction.respawn(victim)
                : state.finish(ModeAction.win(state.opponent(victim)));
    }

    @Override
    public ModeAction bedBreak(ModeRuntimeState state, UUID owner, UUID breaker)
    {
        if (!state.opponent(owner).equals(breaker)) return ModeAction.none();
        state.breakBed(owner);
        return ModeAction.none();
    }
}
