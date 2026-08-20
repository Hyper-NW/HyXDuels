package me.alphatct3209.duels.commands;

import java.util.Collection;

public final class DuelCommandParser
{
    private DuelCommandParser()
    {
    }

    public static Route resolve(String token, boolean knownSubcommand,
                                Collection<String> exactVisiblePlayerNames)
    {
        if (knownSubcommand)
        {
            return Route.SUBCOMMAND;
        }
        for (String playerName : exactVisiblePlayerNames)
        {
            if (playerName.equalsIgnoreCase(token))
            {
                return Route.DIRECT_PLAYER;
            }
        }
        return Route.UNKNOWN;
    }

    public enum Route
    {
        SUBCOMMAND,
        DIRECT_PLAYER,
        UNKNOWN
    }
}
