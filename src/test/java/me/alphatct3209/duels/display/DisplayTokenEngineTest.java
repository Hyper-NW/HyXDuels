package me.alphatct3209.duels.display;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

class DisplayTokenEngineTest
{
    @Test
    void expandsEveryNativeTokenNullSafely()
    {
        DisplayTokenContext context = new DisplayTokenContext("Alex", "uuid", 12, "lobby",
                "Bridge", "3", "COUNTDOWN", "Steve", "NoDebuff", "The Bridge", "bridge", 7);
        assertEquals("Alex|uuid|12|lobby|Bridge|3|COUNTDOWN|Steve|NoDebuff|The Bridge|bridge|7",
                DisplayTokenEngine.expand("<player>|<uuid>|<online>|<world>|<arena>|<arena_id>|"
                        + "<state>|<opponent>|<kit>|<mode>|<mode_key>|<countdown>", context, Map.of()));
        assertEquals("-|Lobby|-|0", DisplayTokenEngine.expand(
                "<player>|<arena>|<opponent>|<countdown>", null, null));
        assertEquals("/duel <target>", DisplayTokenEngine.expand(
                "/duel <target>", context, Map.of()));
    }

    @Test
    void cachedLeaderboardTokensAreCaseInsensitiveAndHaveSafeMissingDefaults()
    {
        Map<String, List<LeaderboardEntry>> cache = Map.of("wins",
                List.of(new LeaderboardEntry("A$lex", 42)));
        assertEquals("A$lex:42 N/A:N/A", DisplayTokenEngine.expand(
                "<leaderboard_WINS_1_player>:<leaderboard_wins_1_score> "
                        + "<leaderboard_wins_10_player>:<leaderboard_wins_10_score>",
                DisplayTokenContext.empty(0), cache));
    }

    @Test
    void formatsMatchTimeAndHealthForCompactArenaHud()
    {
        DisplayTokenContext context = new DisplayTokenContext("Alex", "uuid", 2, "arena",
                "Tennis Court", "1", "PLAYING", "Steve", "Classic", "Classic", "classic",
                0, 1, 0, 476L, "-", 0,
                9.5D, 20D, 14D, 20D, 2, 2, 0, "1.6.7");
        assertEquals("07:56|9.5/20|Steve:14❤|2/2|1.6.7", DisplayTokenEngine.expand(
                "<time_formatted>|<health>/<max_health>|<opponent>:<opponent_health>❤|"
                        + "<players>/<max_players>|<version>", context, Map.of()));
    }
}
