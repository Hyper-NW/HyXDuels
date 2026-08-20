package me.alphatct3209.duels.display;

import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

/** Expands HyXDuels placeholders without depending on Bukkit. */
public final class DisplayTokenEngine
{
    private static final Pattern LEADERBOARD = Pattern.compile(
            "<leaderboard_([A-Za-z0-9_-]+)_([1-9]|10)_(player|score)>", Pattern.CASE_INSENSITIVE);

    private DisplayTokenEngine() {}

    public static String expand(String input, DisplayTokenContext context,
                                Map<String, List<LeaderboardEntry>> leaderboards)
    {
        if (input == null)
        {
            return "";
        }
        DisplayTokenContext values = Objects.requireNonNullElseGet(context,
                () -> DisplayTokenContext.empty(0));
        String expanded = input
                .replace("<player>", values.player())
                .replace("<uuid>", values.uuid())
                .replace("<online>", Integer.toString(values.online()))
                .replace("<date>", LocalDate.now().format(DateTimeFormatter.ofPattern("MM/dd/yy")))
                .replace("<world>", values.world())
                .replace("<arena>", values.arena())
                .replace("<arena_id>", values.arenaId())
                .replace("<state>", values.state())
                .replace("<opponent>", values.opponent())
                .replace("<kit>", values.kit())
                .replace("<mode>", values.mode())
                .replace("<mode_key>", values.modeKey())
                .replace("<score>", Integer.toString(values.score()))
                .replace("<opponent_score>", Integer.toString(values.opponentScore()))
                .replace("<time>", Long.toString(values.time()))
                .replace("<bed>", values.bed())
                .replace("<checkpoint>", Integer.toString(values.checkpoint()))
                .replace("<countdown>", Integer.toString(values.countdown()));

        Matcher matcher = LEADERBOARD.matcher(expanded);
        StringBuffer result = new StringBuffer();
        while (matcher.find())
        {
            String key = matcher.group(1).toLowerCase(Locale.ROOT);
            int index = Integer.parseInt(matcher.group(2)) - 1;
            List<LeaderboardEntry> rows = leaderboards == null ? null : leaderboards.get(key);
            LeaderboardEntry row = rows != null && index < rows.size() ? rows.get(index) : null;
            String replacement = row == null ? "N/A" : matcher.group(3).equalsIgnoreCase("player")
                    ? row.player() : Integer.toString(row.score());
            matcher.appendReplacement(result, Matcher.quoteReplacement(replacement));
        }
        matcher.appendTail(result);
        return result.toString();
    }
}
