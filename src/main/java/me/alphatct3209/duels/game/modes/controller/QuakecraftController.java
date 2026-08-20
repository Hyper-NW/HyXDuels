package me.alphatct3209.duels.game.modes.controller;

import me.alphatct3209.duels.game.modes.ModeAction;
import me.alphatct3209.duels.game.modes.ModeController;
import me.alphatct3209.duels.game.modes.ModeHandlerType;
import me.alphatct3209.duels.game.modes.ModeRuntimeState;

import java.util.Set;
import java.util.UUID;

public final class QuakecraftController implements ModeController
{
    @Override public ModeHandlerType type() { return ModeHandlerType.QUAKECRAFT; }
    @Override public boolean healthDamage() { return false; }
    @Override public Set<EssentialItem> essentialItems()
    { return Set.of(EssentialItem.RAILGUN, EssentialItem.DASH, EssentialItem.COMPASS); }
    @Override public ModeAction death(ModeRuntimeState state, UUID victim)
    { return ModeAction.respawn(victim); }

    @Override
    public ModeAction rangedHit(ModeRuntimeState state, UUID attacker, UUID victim)
    {
        if (!state.opponent(attacker).equals(victim)) return ModeAction.none();
        state.addScore(attacker);
        return state.reachedTarget(attacker)
                ? state.finish(ModeAction.win(attacker)) : ModeAction.respawn(victim);
    }
}
