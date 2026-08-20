package me.alphatct3209.duels.stats.leaderboard;

import java.util.Locale;

/** Exact parser for decomposed leaderboard PlaceholderAPI identifiers. */
public record LeaderboardPlaceholder(Board board, String mode, int rank, Field field)
{
    private static final String OVERALL_WINS = "lb_overall_wins_";
    private static final String OVERALL_KILLS = "lb_overall_kills_";
    private static final String MODE = "lb_mode_";

    public static LeaderboardPlaceholder parse(String identifier)
    {
        if (identifier == null)
        {
            return null;
        }
        String value = identifier.toLowerCase(Locale.ROOT);
        if (value.startsWith(OVERALL_WINS))
        {
            return parseOverall(value.substring(OVERALL_WINS.length()), Board.OVERALL_WINS);
        }
        if (value.startsWith(OVERALL_KILLS))
        {
            return parseOverall(value.substring(OVERALL_KILLS.length()), Board.OVERALL_KILLS);
        }
        if (!value.startsWith(MODE))
        {
            return null;
        }

        String remainder = value.substring(MODE.length());
        int fieldSeparator = remainder.lastIndexOf('_');
        if (fieldSeparator <= 0)
        {
            return null;
        }
        Field field = switch (remainder.substring(fieldSeparator + 1))
        {
            case "player" -> Field.PLAYER;
            case "wins" -> Field.WINS;
            case "division" -> Field.DIVISION;
            default -> null;
        };
        if (field == null)
        {
            return null;
        }

        String beforeField = remainder.substring(0, fieldSeparator);
        int rankSeparator = beforeField.lastIndexOf('_');
        if (rankSeparator <= 0)
        {
            return null;
        }
        String mode = beforeField.substring(0, rankSeparator);
        Integer rank = rank(beforeField.substring(rankSeparator + 1));
        if (rank == null || !mode.matches("[a-z0-9]+(?:_[a-z0-9]+)*"))
        {
            return null;
        }
        return new LeaderboardPlaceholder(Board.MODE, mode, rank, field);
    }

    private static LeaderboardPlaceholder parseOverall(String remainder, Board board)
    {
        int separator = remainder.lastIndexOf('_');
        if (separator <= 0)
        {
            return null;
        }
        Integer rank = rank(remainder.substring(0, separator));
        Field field = switch (remainder.substring(separator + 1))
        {
            case "player" -> Field.PLAYER;
            case "value" -> Field.VALUE;
            default -> null;
        };
        return rank == null || field == null
                ? null : new LeaderboardPlaceholder(board, null, rank, field);
    }

    private static Integer rank(String value)
    {
        try
        {
            int parsed = Integer.parseInt(value);
            return parsed >= 1 && parsed <= 10 ? parsed : null;
        }
        catch (NumberFormatException exception)
        {
            return null;
        }
    }

    public enum Board { OVERALL_WINS, OVERALL_KILLS, MODE }
    public enum Field { PLAYER, VALUE, WINS, DIVISION }
}
