package me.alphatct3209.duels.game.modes.controller;

import me.alphatct3209.duels.game.modes.ModeAction;
import me.alphatct3209.duels.game.modes.ModeController;
import me.alphatct3209.duels.game.modes.ModeHandlerType;
import me.alphatct3209.duels.game.modes.ModeRuntimeState;

import java.util.UUID;

public final class LastStandingController implements ModeController
{
    @Override public ModeHandlerType type() { return ModeHandlerType.LAST_STANDING; }
    @Override public ModeAction death(ModeRuntimeState state, UUID victim)
    { return state.finish(ModeAction.win(state.opponent(victim))); }
}
