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
}
