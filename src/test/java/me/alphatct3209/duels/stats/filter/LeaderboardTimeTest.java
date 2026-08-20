package me.alphatct3209.duels.stats.filter;

import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class LeaderboardTimeTest
{
    @Test
    void derivesInclusiveCalendarWindows()
    {
        LocalDate today = LocalDate.of(2026, 8, 21);
        assertEquals(today, LeaderboardTime.DAILY.since(today));
        assertEquals(LocalDate.of(2026, 8, 15), LeaderboardTime.WEEKLY.since(today));
        assertEquals(LocalDate.of(2026, 8, 1), LeaderboardTime.MONTHLY.since(today));
        assertNull(LeaderboardTime.LIFETIME.since(today));
    }
}
