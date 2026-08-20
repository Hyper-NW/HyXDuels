package me.alphatct3209.duels.game.modes;

import java.util.Set;
import java.util.UUID;

/** Reusable, Bukkit-free mechanics strategy selected solely by ModeHandlerType. */
public interface ModeController
{
    ModeHandlerType type();
    default Set<String> requiredPoints() { return Set.of(); }
    default ModeAction meleeHit(ModeRuntimeState state, UUID attacker, UUID victim) { return ModeAction.none(); }
    default ModeAction rangedHit(ModeRuntimeState state, UUID attacker, UUID victim) { return ModeAction.none(); }
    ModeAction death(ModeRuntimeState state, UUID victim);
    default ModeAction bedBreak(ModeRuntimeState state, UUID owner, UUID breaker) { return ModeAction.none(); }
    default ModeAction goal(ModeRuntimeState state, UUID scorer) { return ModeAction.none(); }
    default ModeAction finish(ModeRuntimeState state, UUID player) { return ModeAction.none(); }
    default ModeAction timeout(ModeRuntimeState state, ModeDurationPolicy.TimeoutPolicy policy)
    {
        return policy == ModeDurationPolicy.TimeoutPolicy.HIGHEST_SCORE
                ? state.scoreLeader().map(ModeAction::win).orElseGet(ModeAction::draw)
                : ModeAction.draw();
    }
    default boolean healthDamage() { return true; }
    default boolean inventoryAllowed() { return true; }
    default boolean goalBased() { return false; }
    default boolean finishBased() { return false; }
    default boolean ringout() { return false; }
    default Set<EssentialItem> essentialItems() { return Set.of(); }
    enum EssentialItem { RAILGUN, DASH, COMPASS, SPLEEF_SHOVEL }
}
