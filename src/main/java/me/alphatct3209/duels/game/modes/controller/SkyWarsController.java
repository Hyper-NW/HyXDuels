package me.alphatct3209.duels.game.modes.controller;

import me.alphatct3209.duels.game.modes.ModeAction;
import me.alphatct3209.duels.game.modes.ModeController;
import me.alphatct3209.duels.game.modes.ModeHandlerType;
import me.alphatct3209.duels.game.modes.ModeRuntimeState;

import java.util.Set;
import java.util.UUID;

public final class SkyWarsController implements ModeController
{
    private static final Set<String> REQUIRED_POINTS = Set.of("chest_1", "chest_2", "mid_chest");

    @Override public ModeHandlerType type() { return ModeHandlerType.SKY_WARS; }
    @Override public Set<String> requiredPoints() { return REQUIRED_POINTS; }
    @Override public boolean ringout() { return true; }
    @Override public ModeAction death(ModeRuntimeState state, UUID victim)
    { return state.finish(ModeAction.win(state.opponent(victim))); }
}
