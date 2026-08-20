package me.alphatct3209.duels.game.modes.controller;

import me.alphatct3209.duels.game.modes.ModeAction;
import me.alphatct3209.duels.game.modes.ModeController;
import me.alphatct3209.duels.game.modes.ModeDurationPolicy;
import me.alphatct3209.duels.game.modes.ModeHandlerType;
import me.alphatct3209.duels.game.modes.ModeRuntimeState;

import java.util.Set;
import java.util.UUID;

public final class ParkourController implements ModeController
{
    @Override public ModeHandlerType type() { return ModeHandlerType.PARKOUR; }
    @Override public Set<String> requiredPoints() { return Set.of("finish"); }
    @Override public boolean finishBased() { return true; }
    @Override public boolean healthDamage() { return false; }
    @Override public boolean inventoryAllowed() { return false; }
    @Override public ModeAction death(ModeRuntimeState state, UUID victim)
    { return ModeAction.respawn(victim); }
    @Override public ModeAction finish(ModeRuntimeState state, UUID player)
    { return state.finish(ModeAction.win(player)); }
    @Override public ModeAction timeout(ModeRuntimeState state, ModeDurationPolicy.TimeoutPolicy ignored)
    { return state.checkpointLeader().map(ModeAction::win).orElseGet(ModeAction::draw); }
}
