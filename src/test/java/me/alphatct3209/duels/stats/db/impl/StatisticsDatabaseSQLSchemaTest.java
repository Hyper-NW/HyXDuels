package me.alphatct3209.duels.stats.db.impl;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class StatisticsDatabaseSQLSchemaTest
{
    @Test
    void freshPlayerSchemaAndRegistrationContainAggregateAndStreakColumns()
    {
        assertEquals("CREATE TABLE IF NOT EXISTS duels_player ("
                        + "Player_UUID VARCHAR(37) NOT NULL, "
                        + "Last_Known_Name VARCHAR(32) NOT NULL, "
                        + "Wins INT NOT NULL, Losses INT NOT NULL, Kills INT NOT NULL, Deaths INT NOT NULL, "
                        + "WinStreak INT NOT NULL DEFAULT 0, HighestWinStreak INT NOT NULL DEFAULT 0, "
                        + "PRIMARY KEY (Player_UUID))",
                StatisticsDatabaseSQL.Constants.CREATE_PLAYER_TABLE);
        assertEquals("INSERT INTO duels_player "
                        + "(Player_UUID, Last_Known_Name, Wins, Losses, Kills, Deaths, WinStreak, HighestWinStreak) "
                        + "VALUES (?, ?, 0, 0, 0, 0, 0, 0)",
                StatisticsDatabaseSQL.Constants.REGISTER_PLAYER);
        assertTrue(StatisticsDatabaseSQL.Constants.GET_WIN_STREAK.contains("WinStreak"));
        assertTrue(StatisticsDatabaseSQL.Constants.GET_HIGHEST_WIN_STREAK.contains("HighestWinStreak"));
    }

    @Test
    void gamemodeSchemaUsesCompoundKeyAndIdempotentMysqlUpsert()
    {
        String create = StatisticsDatabaseSQL.Constants.CREATE_GAMEMODE_STATS_TABLE.toLowerCase();
        String upsert = StatisticsDatabaseSQL.Constants.SET_GAMEMODE_WINS.toLowerCase();
        String leaderboard = StatisticsDatabaseSQL.Constants.GET_TOP_TEN_GAMEMODE_WINS.toLowerCase();

        assertTrue(create.startsWith("create table if not exists duels_gamemode_stats"));
        assertTrue(create.contains("primary key (player_uuid, gamemode)"));
        assertTrue(upsert.contains("on duplicate key update wins = values(wins)"));
        assertTrue(leaderboard.contains("where gamemode = ?"));
        assertTrue(leaderboard.contains("limit 10"));
    }

    @Test
    void periodSchemaSupportsModeAndCalendarFilters()
    {
        String create = StatisticsDatabaseSQL.Constants.CREATE_PERIOD_STATS_TABLE.toLowerCase();
        assertTrue(create.startsWith("create table if not exists duels_period_stats"));
        assertTrue(create.contains("stat_date date not null"));
        assertTrue(create.contains("primary key (player_uuid, stat_date, gamemode)"));
        assertTrue(StatisticsDatabaseSQL.Constants.INCREMENT_PERIOD_WINS
                .contains("Wins = Wins + 1"));
        assertTrue(StatisticsDatabaseSQL.Constants.INCREMENT_PERIOD_KILLS
                .contains("Kills = Kills + 1"));
    }
}
