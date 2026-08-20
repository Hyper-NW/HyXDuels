package me.alphatct3209.duels.utils;

import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import me.alphatct3209.duels.Duels;
import me.alphatct3209.duels.divisions.Division;
import me.alphatct3209.duels.divisions.DivisionProgress;
import me.alphatct3209.duels.game.modes.DuelMode;
import me.alphatct3209.duels.stats.db.StatisticsDatabase;
import me.alphatct3209.duels.stats.leaderboard.LeaderboardEntry;
import me.alphatct3209.duels.stats.leaderboard.LeaderboardPlaceholder;
import me.alphatct3209.duels.stats.leaderboard.LeaderboardSnapshot;
import me.alphatct3209.duels.hologram.HologramDefinition;
import org.bukkit.OfflinePlayer;
import org.jetbrains.annotations.NotNull;

import java.util.Locale;
import java.util.Optional;
import java.util.UUID;

public class PapiHook extends PlaceholderExpansion
{
    private static final String YOUR_PREFIX = "your_";
    private static final String TOP_PREFIX = "top_";
    private static final String DECOMPOSED_PREFIX = "lb_";
    private static final String FILTERED_PREFIX = "flb_";
    private final Duels plugin;

    public PapiHook(Duels plugin)
    {
        this.plugin = plugin;
    }

    @Override
    public @NotNull String getIdentifier()
    {
        return "duels";
    }

    @Override
    public @NotNull String getAuthor()
    {
        return plugin.getDescription().getAuthors().getFirst();
    }

    @Override
    public @NotNull String getVersion()
    {
        return plugin.getDescription().getVersion();
    }

    @Override
    public boolean persist()
    {
        return true;
    }

    @Override
    public String onRequest(OfflinePlayer player, String params)
    {
        if (params == null)
        {
            return null;
        }
        String lower = params.toLowerCase(Locale.ROOT);
        if (lower.startsWith(FILTERED_PREFIX))
        {
            return player == null ? "N/A" : filteredValue(player.getUniqueId(), lower);
        }
        LeaderboardPlaceholder decomposed = LeaderboardPlaceholder.parse(params);
        if (decomposed != null)
        {
            return decomposedValue(decomposed);
        }
        if (lower.startsWith(DECOMPOSED_PREFIX))
        {
            return null;
        }
        if (isAggregateTopPlaceholder(lower, "wins"))
        {
            return aggregateTopValue(params, "wins");
        }
        if (isAggregateTopPlaceholder(lower, "kills"))
        {
            return aggregateTopValue(params, "kills");
        }
        if (lower.startsWith(TOP_PREFIX))
        {
            String value = gamemodeTopValue(params, lower, "_division_", true);
            if (value != null)
            {
                return value;
            }
            value = gamemodeTopValue(params, lower, "_wins_", false);
            if (value != null)
            {
                return value;
            }
        }
        if (player == null)
        {
            return null;
        }

        StatisticsDatabase database = plugin.getStatisticsManager().getStatsDB();
        if (lower.equals("your_kills") || lower.equals("your_overall_kills"))
        {
            return String.valueOf(database.getKills(player.getUniqueId()));
        }
        if (lower.equals("your_wins") || lower.equals("your_overall_wins"))
        {
            return String.valueOf(database.getWins(player.getUniqueId()));
        }
        if (lower.equals("your_winstreak") || lower.equals("your_win_streak"))
        {
            return String.valueOf(database.getWinStreak(player.getUniqueId()));
        }
        if (lower.equals("your_highest_winstreak") || lower.equals("your_highest_win_streak"))
        {
            return String.valueOf(database.getHighestWinStreak(player.getUniqueId()));
        }

        String gamemode = extractPersonalMode(params, lower, "_wins_to_next");
        if (gamemode != null)
        {
            return personalGamemodeValue(player.getUniqueId(), gamemode, PersonalValue.WINS_TO_NEXT);
        }
        gamemode = extractPersonalMode(params, lower, "_division");
        if (gamemode != null)
        {
            return personalGamemodeValue(player.getUniqueId(), gamemode, PersonalValue.DIVISION);
        }
        gamemode = extractPersonalMode(params, lower, "_wins");
        if (gamemode != null)
        {
            return personalGamemodeValue(player.getUniqueId(), gamemode, PersonalValue.WINS);
        }
        return null;
    }

    private String filteredValue(UUID viewer, String value)
    {
        String body = value.substring(FILTERED_PREFIX.length());
        int typeEnd = body.indexOf('_');
        if (typeEnd < 1) return null;
        HologramDefinition.Type type = switch (body.substring(0, typeEnd))
        {
            case "wins" -> HologramDefinition.Type.WINS;
            case "kills" -> HologramDefinition.Type.KILLS;
            case "divisions" -> HologramDefinition.Type.DIVISIONS;
            default -> null;
        };
        if (type == null) return null;
        String remainder = body.substring(typeEnd + 1);
        if (remainder.endsWith("_filter"))
        {
            String fallback = remainder.substring(0, remainder.length() - "_filter".length());
            return plugin.getFilteredLeaderboardService().summary(viewer, type, decodeMode(fallback));
        }
        int fieldSeparator = remainder.lastIndexOf('_');
        if (fieldSeparator < 1) return null;
        String field = remainder.substring(fieldSeparator + 1);
        String beforeField = remainder.substring(0, fieldSeparator);
        int rankSeparator = beforeField.lastIndexOf('_');
        if (rankSeparator < 1) return null;
        Integer rank = parsePlace(beforeField.substring(rankSeparator + 1));
        if (rank == null) return null;
        String fallback = decodeMode(beforeField.substring(0, rankSeparator));
        LeaderboardEntry entry = plugin.getFilteredLeaderboardService().entry(viewer, type, fallback, rank);
        if (entry == null) return "N/A";
        return switch (field)
        {
            case "player" -> entry.name();
            case "value", "wins" -> Integer.toString(entry.value());
            case "division" -> entry.division();
            default -> null;
        };
    }

    private String decodeMode(String value)
    {
        return value.equals("all") ? null : value;
    }

    private String decomposedValue(LeaderboardPlaceholder placeholder)
    {
        LeaderboardSnapshot snapshot = plugin.getLeaderboardService().snapshot();
        Optional<LeaderboardEntry> row = switch (placeholder.board())
        {
            case OVERALL_WINS -> snapshot.overallWins(placeholder.rank());
            case OVERALL_KILLS -> snapshot.overallKills(placeholder.rank());
            case MODE -> snapshot.mode(placeholder.mode(), placeholder.rank());
        };
        if (row.isEmpty())
        {
            return "N/A";
        }
        LeaderboardEntry entry = row.get();
        return switch (placeholder.field())
        {
            case PLAYER -> entry.name();
            case VALUE, WINS -> Integer.toString(entry.value());
            case DIVISION -> entry.division();
        };
    }

    private String personalGamemodeValue(UUID uuid, String gamemode, PersonalValue requestedValue)
    {
        DuelMode mode = plugin.getModeManager().resolve(gamemode).orElse(null);
        if (mode == null)
        {
            return error("your_<safe_mode_key>_" + requestedValue.syntax());
        }
        String key = mode.key().value();

        int wins = plugin.getStatisticsManager().getStatsDB().getGamemodeWins(uuid, key);
        DivisionProgress progress = plugin.getDivisionManager().getProgress(wins);
        return switch (requestedValue)
        {
            case WINS -> String.valueOf(wins);
            case DIVISION -> progress.current().map(Division::displayName).orElse("Unranked");
            case WINS_TO_NEXT -> progress.next()
                    .map(next -> String.valueOf(Math.max(0L, next.requiredWins() - wins)))
                    .orElse("0");
        };
    }

    static boolean isAggregateTopPlaceholder(String identifier, String type)
    {
        String prefix = TOP_PREFIX + type + "_";
        if (!identifier.startsWith(prefix))
        {
            return false;
        }
        String place = identifier.substring(prefix.length());
        return !place.isEmpty() && place.indexOf('_') < 0;
    }

    static ParsedGamemodeTop parseGamemodeTop(String params, String lower, String marker)
    {
        int separator = lower.lastIndexOf(marker);
        if (!lower.startsWith(TOP_PREFIX) || separator <= TOP_PREFIX.length())
        {
            return null;
        }
        return new ParsedGamemodeTop(
                params.substring(TOP_PREFIX.length(), separator),
                params.substring(separator + marker.length()));
    }

    private String aggregateTopValue(String params, String type)
    {
        String[] split = params.split("_");
        if (split.length != 3)
        {
            return error("top_" + type + "_<1-10>");
        }
        Integer place = parsePlace(split[2]);
        if (place == null)
        {
            return error("top_" + type + "_<1-10>");
        }

        LeaderboardSnapshot snapshot = plugin.getLeaderboardService().snapshot();
        Optional<LeaderboardEntry> row = type.equals("wins")
                ? snapshot.overallWins(place) : snapshot.overallKills(place);
        return row.map(entry -> entry.name() + " (" + entry.value() + " " + type + ")")
                .orElse("N/A");
    }

    private String gamemodeTopValue(String params, String lower, String marker, boolean includeDivision)
    {
        ParsedGamemodeTop parsed = parseGamemodeTop(params, lower, marker);
        if (parsed == null)
        {
            return null;
        }

        Integer place = parsePlace(parsed.place());
        String format = "top_<safe_mode_key>_" + (includeDivision ? "division" : "wins") + "_<1-10>";
        if (place == null)
        {
            return error(format);
        }

        DuelMode mode = plugin.getModeManager().resolve(parsed.gamemode()).orElse(null);
        if (mode == null)
        {
            return error(format);
        }
        String key = mode.key().value();

        Optional<LeaderboardEntry> row = plugin.getLeaderboardService().snapshot().mode(key, place);
        if (row.isEmpty())
        {
            return "N/A";
        }
        LeaderboardEntry entry = row.get();
        return includeDivision
                ? entry.name() + " (" + entry.division() + ", " + entry.value() + " wins)"
                : entry.name() + " (" + entry.value() + " " + key + " wins)";
    }

    private String extractPersonalMode(String params, String lower, String suffix)
    {
        if (!lower.startsWith(YOUR_PREFIX) || !lower.endsWith(suffix))
        {
            return null;
        }
        int end = params.length() - suffix.length();
        return end > YOUR_PREFIX.length() ? params.substring(YOUR_PREFIX.length(), end) : null;
    }

    private Integer parsePlace(String value)
    {
        try
        {
            int place = Integer.parseInt(value);
            return place >= 1 && place <= 10 ? place : null;
        }
        catch (NumberFormatException exception)
        {
            return null;
        }
    }

    private String error(String format)
    {
        return "Err: Format should be 'duels_" + format + "'";
    }

    record ParsedGamemodeTop(String gamemode, String place) {}

    private enum PersonalValue
    {
        WINS("wins"), DIVISION("division"), WINS_TO_NEXT("wins_to_next");

        private final String syntax;

        PersonalValue(String syntax)
        {
            this.syntax = syntax;
        }

        String syntax()
        {
            return syntax;
        }
    }
}
