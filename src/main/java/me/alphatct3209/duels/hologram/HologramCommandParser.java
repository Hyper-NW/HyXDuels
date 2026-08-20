package me.alphatct3209.duels.hologram;

import java.util.Locale;

public final class HologramCommandParser
{
    private HologramCommandParser() {}

    public static Parsed parse(String[] args)
    {
        if (args.length == 0)
        {
            throw new IllegalArgumentException("/duels hologram status|list|create|move|delete|reload");
        }
        Action action;
        try
        {
            action = Action.valueOf(args[0].toUpperCase(Locale.ROOT));
        }
        catch (IllegalArgumentException exception)
        {
            throw new IllegalArgumentException("unknown hologram action '" + args[0] + "'");
        }
        return switch (action)
        {
            case STATUS, LIST, RELOAD -> {
                requireLength(args, 1, action);
                yield new Parsed(action, null, null, null);
            }
            case MOVE, DELETE -> {
                requireLength(args, 2, action);
                yield new Parsed(action, args[1], null, null);
            }
            case CREATE -> {
                if (args.length < 3 || args.length > 4)
                {
                    throw new IllegalArgumentException(
                            "/duels hologram create <id> <wins|kills|divisions> [gamemode]");
                }
                HologramDefinition.Type type = HologramDefinition.Type.parse(args[2]);
                String mode = args.length == 4 ? args[3] : null;
                if (type == HologramDefinition.Type.DIVISIONS && mode == null)
                {
                    throw new IllegalArgumentException("divisions requires a gamemode");
                }
                if (type != HologramDefinition.Type.DIVISIONS && mode != null)
                {
                    throw new IllegalArgumentException(type.configValue() + " does not accept a gamemode");
                }
                yield new Parsed(action, args[1], type, mode);
            }
        };
    }

    private static void requireLength(String[] args, int expected, Action action)
    {
        if (args.length != expected)
        {
            throw new IllegalArgumentException("hologram " + action.name().toLowerCase(Locale.ROOT)
                    + " received unexpected arguments");
        }
    }

    public record Parsed(Action action, String id, HologramDefinition.Type type, String gamemode) {}
    public enum Action { STATUS, LIST, CREATE, MOVE, DELETE, RELOAD }
}
