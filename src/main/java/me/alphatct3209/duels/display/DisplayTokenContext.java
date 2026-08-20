package me.alphatct3209.duels.display;

import java.util.Objects;

/** Values available to native display placeholders. */
public record DisplayTokenContext(
        String player, String uuid, int online, String world, String arena, String arenaId,
        String state, String opponent, String kit, String mode, String modeKey, int countdown,
        int score, int opponentScore, long time, String bed, int checkpoint)
{
    public DisplayTokenContext
    {
        player = value(player, "-"); uuid = value(uuid, "-"); online = Math.max(0, online);
        world = value(world, "-"); arena = value(arena, "Lobby"); arenaId = value(arenaId, "-");
        state = value(state, "LOBBY"); opponent = value(opponent, "-"); kit = value(kit, "-");
        mode = value(mode, "-"); modeKey = value(modeKey, "-"); countdown = Math.max(0, countdown);
        score = Math.max(0, score); opponentScore = Math.max(0, opponentScore); time = Math.max(0L, time);
        bed = value(bed, "-"); checkpoint = Math.max(0, checkpoint);
    }
    public DisplayTokenContext(String player, String uuid, int online, String world, String arena,
                               String arenaId, String state, String opponent, String kit,
                               String mode, String modeKey, int countdown)
    {
        this(player, uuid, online, world, arena, arenaId, state, opponent, kit, mode, modeKey,
                countdown, 0, 0, 0L, "-", 0);
    }
    public DisplayTokenContext(String player, String uuid, int online, String world, String arena,
                               String arenaId, String state, String opponent, String kit, int countdown)
    {
        this(player, uuid, online, world, arena, arenaId, state, opponent, kit, "-", "-", countdown);
    }
    public static DisplayTokenContext empty(int online)
    {
        return new DisplayTokenContext("-", "-", online, "-", "Lobby", "-", "LOBBY", "-", "-", "-", "-", 0);
    }
    private static String value(String input, String fallback)
    { return input == null || input.isEmpty() ? Objects.requireNonNull(fallback) : input; }
}
