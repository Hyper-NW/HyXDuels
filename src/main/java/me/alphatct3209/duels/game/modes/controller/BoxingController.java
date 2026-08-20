package me.alphatct3209.duels.game.modes.controller;

import me.alphatct3209.duels.game.modes.ModeAction;
import me.alphatct3209.duels.game.modes.ModeController;
import me.alphatct3209.duels.game.modes.ModeHandlerType;
import me.alphatct3209.duels.game.modes.ModeRuntimeState;

import java.util.UUID;

public final class BoxingController implements ModeController
{
    private final int mercyLead;

    public BoxingController(int mercyLead)
    {
        if (mercyLead < 1) throw new IllegalArgumentException("mercyLead must be positive");
        this.mercyLead = mercyLead;
    }

    @Override public ModeHandlerType type() { return ModeHandlerType.BOXING; }
    @Override public boolean healthDamage() { return false; }
    @Override public boolean inventoryAllowed() { return false; }

    @Override
    public ModeAction meleeHit(ModeRuntimeState state, UUID attacker, UUID victim)
    {
        if (!state.opponent(attacker).equals(victim)) return ModeAction.none();
        int score = state.addScore(attacker);
        if (score >= state.targetScore() || score - state.score(victim) >= mercyLead)
            return state.finish(ModeAction.win(attacker));
        return ModeAction.none();
    }

    @Override public ModeAction death(ModeRuntimeState state, UUID victim)
    { return ModeAction.respawn(victim); }
}
