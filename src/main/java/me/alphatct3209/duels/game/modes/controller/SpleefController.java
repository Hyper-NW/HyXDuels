package me.alphatct3209.duels.game.modes.controller;

import me.alphatct3209.duels.game.modes.ModeAction;
import me.alphatct3209.duels.game.modes.ModeController;
import me.alphatct3209.duels.game.modes.ModeHandlerType;
import me.alphatct3209.duels.game.modes.ModeRuntimeState;

import java.util.Set;
import java.util.UUID;

public final class SpleefController implements ModeController
{
    @Override public ModeHandlerType type() { return ModeHandlerType.SPLEEF; }
    @Override public boolean healthDamage() { return false; }
    @Override public boolean ringout() { return true; }
    @Override public Set<EssentialItem> essentialItems()
    { return Set.of(EssentialItem.SPLEEF_SHOVEL); }
    @Override public ModeAction death(ModeRuntimeState state, UUID victim)
    { return state.finish(ModeAction.win(state.opponent(victim))); }
}
