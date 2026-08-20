package me.alphatct3209.duels.game.modes;

import java.util.Optional;
import java.util.UUID;

/** Result of one handler transition. Target is the player to respawn/reset; winner is terminal. */
public record ModeAction(Type type, UUID target, UUID winner)
{
    public enum Type { NONE, RESPAWN, ROUND_RESET, WIN, DRAW }

    public static ModeAction none() { return new ModeAction(Type.NONE, null, null); }
    public static ModeAction respawn(UUID target) { return new ModeAction(Type.RESPAWN, target, null); }
    public static ModeAction roundReset() { return new ModeAction(Type.ROUND_RESET, null, null); }
    public static ModeAction win(UUID winner) { return new ModeAction(Type.WIN, null, winner); }
    public static ModeAction draw() { return new ModeAction(Type.DRAW, null, null); }
    public boolean terminal() { return type == Type.WIN || type == Type.DRAW; }
    public Optional<UUID> winnerOptional() { return Optional.ofNullable(winner); }
}
