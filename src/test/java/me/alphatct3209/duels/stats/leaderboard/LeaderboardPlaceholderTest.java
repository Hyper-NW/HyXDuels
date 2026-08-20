package me.alphatct3209.duels.stats.leaderboard;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class LeaderboardPlaceholderTest
{
    @Test
    void parsesExactOverallFormsAndRejectsMalformedOrOutOfRangeForms()
    {
        assertEquals(new LeaderboardPlaceholder(LeaderboardPlaceholder.Board.OVERALL_WINS,
                        null, 1, LeaderboardPlaceholder.Field.PLAYER),
                LeaderboardPlaceholder.parse("lb_overall_wins_1_player"));
        assertEquals(new LeaderboardPlaceholder(LeaderboardPlaceholder.Board.OVERALL_KILLS,
                        null, 10, LeaderboardPlaceholder.Field.VALUE),
                LeaderboardPlaceholder.parse("LB_OVERALL_KILLS_10_VALUE"));
        assertNull(LeaderboardPlaceholder.parse("lb_overall_wins_0_player"));
        assertNull(LeaderboardPlaceholder.parse("lb_overall_wins_11_value"));
        assertNull(LeaderboardPlaceholder.parse("lb_overall_wins_1_score"));
        assertNull(LeaderboardPlaceholder.parse("lb_overall_wins_1_player_extra"));
    }

    @Test
    void usesFinalRankAndFieldForCompoundAndReservedModeKeys()
    {
        assertEquals(new LeaderboardPlaceholder(LeaderboardPlaceholder.Board.MODE,
                        "wins_player_division", 10, LeaderboardPlaceholder.Field.DIVISION),
                LeaderboardPlaceholder.parse("lb_mode_wins_player_division_10_division"));
        assertEquals(new LeaderboardPlaceholder(LeaderboardPlaceholder.Board.MODE,
                        "nodebuff_ranked", 2, LeaderboardPlaceholder.Field.WINS),
                LeaderboardPlaceholder.parse("lb_mode_nodebuff_ranked_2_wins"));
        assertNull(LeaderboardPlaceholder.parse("lb_mode_nodebuff-ranked_2_wins"));
        assertNull(LeaderboardPlaceholder.parse("lb_mode__2_player"));
        assertNull(LeaderboardPlaceholder.parse("lb_mode_nodebuff_ranked_two_player"));
        assertNull(LeaderboardPlaceholder.parse("lb_mode_nodebuff_ranked_2_value"));
    }
}
