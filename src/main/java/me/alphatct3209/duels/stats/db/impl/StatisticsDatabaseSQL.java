package me.alphatct3209.duels.stats.db.impl;

import me.alphatct3209.duels.Duels;
import me.alphatct3209.duels.game.modes.ModeKey;
import me.alphatct3209.duels.stats.StatisticsManager;
import me.alphatct3209.duels.stats.db.LeaderboardMetric;
import me.alphatct3209.duels.stats.db.StatisticsDatabase;
import org.bukkit.Bukkit;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public class StatisticsDatabaseSQL implements StatisticsDatabase
{
    private final Duels plugin;
    private Connection connection;

    public StatisticsDatabaseSQL(Duels plugin, StatisticsManager manager)
    {
        this.plugin = plugin;
        setup();
    }

    private void setup()
    {
        Connection connection = requireConnection();
        try (Statement statement = connection.createStatement())
        {
            statement.executeUpdate(Constants.CREATE_PLAYER_TABLE);
            statement.executeUpdate(Constants.CREATE_GAMEMODE_STATS_TABLE);
            statement.executeUpdate(Constants.CREATE_PERIOD_STATS_TABLE);
            ensurePlayerColumn(connection, statement, "WinStreak");
            ensurePlayerColumn(connection, statement, "HighestWinStreak");
        }
        catch (SQLException exception)
        {
            throw new IllegalStateException("Could not initialize the HyXDuels SQL schema", exception);
        }
    }

    private void ensurePlayerColumn(Connection connection, Statement statement, String column)
            throws SQLException
    {
        boolean present = false;
        try (ResultSet columns = connection.getMetaData().getColumns(
                connection.getCatalog(), null, "duels_player", null))
        {
            while (columns.next())
            {
                if (column.equalsIgnoreCase(columns.getString("COLUMN_NAME")))
                {
                    present = true;
                    break;
                }
            }
        }
        if (!present)
        {
            statement.executeUpdate("ALTER TABLE duels_player ADD COLUMN " + column
                    + " INT NOT NULL DEFAULT 0");
        }
    }

    private Connection getConnection()
    {
        String address = plugin.getConfig().getString("Statistics.SQL-Info.Address");
        String port = plugin.getConfig().getString("Statistics.SQL-Info.Port");
        String db = plugin.getConfig().getString("Statistics.SQL-Info.Database");
        String user = plugin.getConfig().getString("Statistics.SQL-Info.Username");
        String pass = plugin.getConfig().getString("Statistics.SQL-Info.Password");
        String url = "jdbc:mysql://" + address + ":" + port + "/" + db + "?characterEncoding=utf8";
        try
        {
            if (connection == null || connection.isClosed())
            {
                connection = DriverManager.getConnection(url, user, pass);
            }
        }
        catch (SQLException exception)
        {
            Bukkit.getLogger().severe("Duels could not connect to the SQL database: " + exception);
        }
        return connection;
    }

    private Connection requireConnection()
    {
        Connection connection = getConnection();
        if (connection == null)
        {
            throw new IllegalStateException("Could not connect to the configured HyXDuels SQL database");
        }
        return connection;
    }

    @Override
    public int getWins(UUID uuid)
    {
        return getPlayerStatistic(uuid, Constants.GET_WINS, "Wins");
    }

    @Override
    public int getLosses(UUID uuid)
    {
        return getPlayerStatistic(uuid, Constants.GET_LOSSES, "Losses");
    }

    @Override
    public int getKills(UUID uuid)
    {
        return getPlayerStatistic(uuid, Constants.GET_KILLS, "Kills");
    }

    @Override
    public int getDeaths(UUID uuid)
    {
        return getPlayerStatistic(uuid, Constants.GET_DEATHS, "Deaths");
    }

    @Override
    public int getWinStreak(UUID uuid)
    {
        return getPlayerStatistic(uuid, Constants.GET_WIN_STREAK, "WinStreak");
    }

    @Override
    public int getHighestWinStreak(UUID uuid)
    {
        return getPlayerStatistic(uuid, Constants.GET_HIGHEST_WIN_STREAK, "HighestWinStreak");
    }

    private int getPlayerStatistic(UUID uuid, String sql, String column)
    {
        try (PreparedStatement statement = requireConnection().prepareStatement(sql))
        {
            statement.setString(1, uuid.toString());
            try (ResultSet result = statement.executeQuery())
            {
                return result.next() ? result.getInt(column) : 0;
            }
        }
        catch (SQLException exception)
        {
            Bukkit.getLogger().severe("Duels could not retrieve " + column + " from SQL: " + exception);
            return 0;
        }
    }

    @Override
    public int getGamemodeWins(UUID uuid, String gamemode)
    {
        String key = ModeKey.parse(gamemode).value();
        try (PreparedStatement statement = requireConnection().prepareStatement(Constants.GET_GAMEMODE_WINS))
        {
            statement.setString(1, uuid.toString());
            statement.setString(2, key);
            try (ResultSet result = statement.executeQuery())
            {
                return result.next() ? result.getInt("Wins") : 0;
            }
        }
        catch (SQLException exception)
        {
            Bukkit.getLogger().severe("Duels could not retrieve gamemode wins from SQL: " + exception);
            return 0;
        }
    }

    @Override
    public void setWins(UUID uuid, int wins)
    {
        setPlayerStatistic(uuid, Constants.SET_WINS, wins, "wins");
    }

    @Override
    public void setLosses(UUID uuid, int losses)
    {
        setPlayerStatistic(uuid, Constants.SET_LOSSES, losses, "losses");
    }

    @Override
    public void setKills(UUID uuid, int kills)
    {
        setPlayerStatistic(uuid, Constants.SET_KILLS, kills, "kills");
    }

    @Override
    public void setDeaths(UUID uuid, int deaths)
    {
        setPlayerStatistic(uuid, Constants.SET_DEATHS, deaths, "deaths");
    }

    @Override
    public void setWinStreak(UUID uuid, int winStreak)
    {
        setPlayerStatistic(uuid, Constants.SET_WIN_STREAK, Math.max(0, winStreak), "win streak");
    }

    @Override
    public void setHighestWinStreak(UUID uuid, int highestWinStreak)
    {
        setPlayerStatistic(uuid, Constants.SET_HIGHEST_WIN_STREAK,
                Math.max(0, highestWinStreak), "highest win streak");
    }

    private void setPlayerStatistic(UUID uuid, String sql, int value, String statistic)
    {
        try (PreparedStatement statement = requireConnection().prepareStatement(sql))
        {
            statement.setInt(1, value);
            statement.setString(2, uuid.toString());
            statement.executeUpdate();
        }
        catch (SQLException exception)
        {
            Bukkit.getLogger().severe("Duels could not update " + statistic + " in SQL: " + exception);
        }
    }

    @Override
    public void setGamemodeWins(UUID uuid, String gamemode, int wins)
    {
        String key = ModeKey.parse(gamemode).value();
        try (PreparedStatement statement = requireConnection().prepareStatement(Constants.SET_GAMEMODE_WINS))
        {
            statement.setString(1, uuid.toString());
            statement.setString(2, key);
            statement.setInt(3, Math.max(0, wins));
            statement.executeUpdate();
        }
        catch (SQLException exception)
        {
            Bukkit.getLogger().severe("Duels could not update gamemode wins in SQL: " + exception);
        }
    }

    @Override
    public void incrementPeriodic(UUID player, ModeKey mode, LeaderboardMetric metric, LocalDate date)
    {
        String sql = metric == LeaderboardMetric.WINS
                ? Constants.INCREMENT_PERIOD_WINS : Constants.INCREMENT_PERIOD_KILLS;
        try (PreparedStatement statement = requireConnection().prepareStatement(sql))
        {
            statement.setString(1, player.toString());
            statement.setDate(2, java.sql.Date.valueOf(date));
            statement.setString(3, mode.value());
            statement.executeUpdate();
        }
        catch (SQLException exception)
        {
            Bukkit.getLogger().severe("Duels could not update dated leaderboard statistics in SQL: "
                    + exception);
        }
    }

    @Override
    public HashMap<UUID, Integer> getFilteredLeaderboard(LeaderboardMetric metric, String mode,
                                                         LocalDate since, Set<UUID> players)
    {
        if (players != null && players.isEmpty())
        {
            return new LinkedHashMap<>();
        }
        String modeKey = mode == null ? null : ModeKey.parse(mode).value();
        String sql;
        if (since == null && modeKey == null)
        {
            sql = "SELECT Player_UUID, " + metric.column() + " AS Value FROM duels_player";
        }
        else if (since == null && metric == LeaderboardMetric.WINS)
        {
            sql = "SELECT Player_UUID, Wins AS Value FROM duels_gamemode_stats WHERE Gamemode = ?";
        }
        else
        {
            sql = "SELECT Player_UUID, SUM(" + metric.column() + ") AS Value "
                    + "FROM duels_period_stats WHERE 1 = 1"
                    + (since == null ? "" : " AND Stat_Date >= ?")
                    + (modeKey == null ? "" : " AND Gamemode = ?")
                    + " GROUP BY Player_UUID";
        }

        Map<UUID, Integer> values = new HashMap<>();
        try (PreparedStatement statement = requireConnection().prepareStatement(sql))
        {
            int parameter = 1;
            if (since == null && modeKey != null && metric == LeaderboardMetric.WINS)
            {
                statement.setString(parameter, modeKey);
            }
            else if (!(since == null && modeKey == null))
            {
                if (since != null)
                {
                    statement.setDate(parameter++, java.sql.Date.valueOf(since));
                }
                if (modeKey != null)
                {
                    statement.setString(parameter, modeKey);
                }
            }
            try (ResultSet result = statement.executeQuery())
            {
                while (result.next())
                {
                    UUID uuid = UUID.fromString(result.getString("Player_UUID"));
                    if (players == null || players.contains(uuid))
                    {
                        values.put(uuid, Math.max(0, result.getInt("Value")));
                    }
                }
            }
        }
        catch (SQLException exception)
        {
            throw new IllegalStateException("Could not retrieve a filtered leaderboard from SQL", exception);
        }
        return topTen(values);
    }

    private HashMap<UUID, Integer> topTen(Map<UUID, Integer> values)
    {
        HashMap<UUID, Integer> result = new LinkedHashMap<>();
        values.entrySet().stream()
                .sorted(Map.Entry.<UUID, Integer>comparingByValue().reversed()
                        .thenComparing(entry -> entry.getKey().toString()))
                .limit(10)
                .forEach(entry -> result.put(entry.getKey(), entry.getValue()));
        return result;
    }

    @Override
    public void registerNewPlayer(UUID uuid, String name)
    {
        try (PreparedStatement statement = requireConnection().prepareStatement(Constants.REGISTER_PLAYER))
        {
            statement.setString(1, uuid.toString());
            statement.setString(2, name);
            statement.executeUpdate();
        }
        catch (SQLException exception)
        {
            Bukkit.getLogger().severe("Duels could not register a player in SQL: " + exception);
        }
    }

    @Override
    public void updateLastKnownName(UUID uuid, String name)
    {
        try (PreparedStatement statement = requireConnection().prepareStatement(Constants.UPDATE_NAME))
        {
            statement.setString(1, name);
            statement.setString(2, uuid.toString());
            statement.executeUpdate();
        }
        catch (SQLException exception)
        {
            Bukkit.getLogger().severe("Duels could not update a username in SQL: " + exception);
        }
    }

    @Override
    public boolean isRegistered(UUID uuid)
    {
        return !"[Unknown]".equals(getLastKnownName(uuid));
    }

    @Override
    public UUID getUUID(String playerName)
    {
        try (PreparedStatement statement = requireConnection().prepareStatement(Constants.GET_UUID))
        {
            statement.setString(1, playerName);
            try (ResultSet result = statement.executeQuery())
            {
                return result.next() ? UUID.fromString(result.getString("Player_UUID")) : null;
            }
        }
        catch (SQLException exception)
        {
            Bukkit.getLogger().severe("Duels could not retrieve a UUID from SQL: " + exception);
            return null;
        }
    }

    @Override
    public String getLastKnownName(UUID uuid)
    {
        try (PreparedStatement statement = requireConnection().prepareStatement(Constants.GET_NAME))
        {
            statement.setString(1, uuid.toString());
            try (ResultSet result = statement.executeQuery())
            {
                return result.next() ? result.getString("Last_Known_Name") : "[Unknown]";
            }
        }
        catch (SQLException exception)
        {
            throw new IllegalStateException("Could not retrieve a leaderboard username from SQL", exception);
        }
    }

    @Override
    public HashMap<UUID, Integer> getTopTenWins()
    {
        return getTopTen(Constants.GET_TOP_TEN_WINS, null);
    }

    @Override
    public HashMap<UUID, Integer> getTopTenKills()
    {
        return getTopTen(Constants.GET_TOP_TEN_KILLS, null);
    }

    @Override
    public HashMap<UUID, Integer> getTopTenGamemodeWins(String gamemode)
    {
        return getTopTen(Constants.GET_TOP_TEN_GAMEMODE_WINS, ModeKey.parse(gamemode).value());
    }

    private HashMap<UUID, Integer> getTopTen(String sql, String gamemode)
    {
        HashMap<UUID, Integer> values = new HashMap<>();
        try (PreparedStatement statement = requireConnection().prepareStatement(sql))
        {
            if (gamemode != null)
            {
                statement.setString(1, gamemode);
            }
            try (ResultSet result = statement.executeQuery())
            {
                while (result.next())
                {
                    values.put(UUID.fromString(result.getString("Player_UUID")), result.getInt("Wins"));
                }
            }
        }
        catch (SQLException exception)
        {
            throw new IllegalStateException("Could not retrieve a leaderboard from SQL", exception);
        }
        return values;
    }

    static final class Constants
    {
        static final String CREATE_PLAYER_TABLE = "CREATE TABLE IF NOT EXISTS duels_player ("
                + "Player_UUID VARCHAR(37) NOT NULL, "
                + "Last_Known_Name VARCHAR(32) NOT NULL, "
                + "Wins INT NOT NULL, Losses INT NOT NULL, Kills INT NOT NULL, Deaths INT NOT NULL, "
                + "WinStreak INT NOT NULL DEFAULT 0, HighestWinStreak INT NOT NULL DEFAULT 0, "
                + "PRIMARY KEY (Player_UUID))";
        static final String CREATE_GAMEMODE_STATS_TABLE = "CREATE TABLE IF NOT EXISTS duels_gamemode_stats ("
                + "Player_UUID VARCHAR(37) NOT NULL, Gamemode VARCHAR(191) NOT NULL, Wins INT NOT NULL DEFAULT 0, "
                + "PRIMARY KEY (Player_UUID, Gamemode))";
        static final String CREATE_PERIOD_STATS_TABLE = "CREATE TABLE IF NOT EXISTS duels_period_stats ("
                + "Player_UUID VARCHAR(37) NOT NULL, Stat_Date DATE NOT NULL, "
                + "Gamemode VARCHAR(191) NOT NULL, Wins INT NOT NULL DEFAULT 0, Kills INT NOT NULL DEFAULT 0, "
                + "PRIMARY KEY (Player_UUID, Stat_Date, Gamemode))";
        static final String REGISTER_PLAYER = "INSERT INTO duels_player "
                + "(Player_UUID, Last_Known_Name, Wins, Losses, Kills, Deaths, WinStreak, HighestWinStreak) "
                + "VALUES (?, ?, 0, 0, 0, 0, 0, 0)";
        static final String UPDATE_NAME = "UPDATE duels_player SET Last_Known_Name = ? WHERE Player_UUID = ?";
        static final String GET_NAME = "SELECT Last_Known_Name FROM duels_player WHERE Player_UUID = ?";
        static final String GET_UUID = "SELECT Player_UUID FROM duels_player WHERE Last_Known_Name = ?";
        static final String GET_WINS = "SELECT Wins FROM duels_player WHERE Player_UUID = ?";
        static final String GET_LOSSES = "SELECT Losses FROM duels_player WHERE Player_UUID = ?";
        static final String GET_KILLS = "SELECT Kills FROM duels_player WHERE Player_UUID = ?";
        static final String GET_DEATHS = "SELECT Deaths FROM duels_player WHERE Player_UUID = ?";
        static final String GET_WIN_STREAK = "SELECT WinStreak FROM duels_player WHERE Player_UUID = ?";
        static final String GET_HIGHEST_WIN_STREAK = "SELECT HighestWinStreak FROM duels_player WHERE Player_UUID = ?";
        static final String GET_GAMEMODE_WINS = "SELECT Wins FROM duels_gamemode_stats "
                + "WHERE Player_UUID = ? AND Gamemode = ?";
        static final String SET_WINS = "UPDATE duels_player SET Wins = ? WHERE Player_UUID = ?";
        static final String SET_LOSSES = "UPDATE duels_player SET Losses = ? WHERE Player_UUID = ?";
        static final String SET_KILLS = "UPDATE duels_player SET Kills = ? WHERE Player_UUID = ?";
        static final String SET_DEATHS = "UPDATE duels_player SET Deaths = ? WHERE Player_UUID = ?";
        static final String SET_WIN_STREAK = "UPDATE duels_player SET WinStreak = ? WHERE Player_UUID = ?";
        static final String SET_HIGHEST_WIN_STREAK = "UPDATE duels_player SET HighestWinStreak = ? WHERE Player_UUID = ?";
        static final String SET_GAMEMODE_WINS = "INSERT INTO duels_gamemode_stats "
                + "(Player_UUID, Gamemode, Wins) VALUES (?, ?, ?) "
                + "ON DUPLICATE KEY UPDATE Wins = VALUES(Wins)";
        static final String INCREMENT_PERIOD_WINS = "INSERT INTO duels_period_stats "
                + "(Player_UUID, Stat_Date, Gamemode, Wins, Kills) VALUES (?, ?, ?, 1, 0) "
                + "ON DUPLICATE KEY UPDATE Wins = Wins + 1";
        static final String INCREMENT_PERIOD_KILLS = "INSERT INTO duels_period_stats "
                + "(Player_UUID, Stat_Date, Gamemode, Wins, Kills) VALUES (?, ?, ?, 0, 1) "
                + "ON DUPLICATE KEY UPDATE Kills = Kills + 1";
        static final String GET_TOP_TEN_WINS = "SELECT Player_UUID, Wins FROM duels_player "
                + "ORDER BY Wins DESC, Player_UUID ASC LIMIT 10";
        static final String GET_TOP_TEN_KILLS = "SELECT Player_UUID, Kills AS Wins FROM duels_player "
                + "ORDER BY Kills DESC, Player_UUID ASC LIMIT 10";
        static final String GET_TOP_TEN_GAMEMODE_WINS = "SELECT Player_UUID, Wins FROM duels_gamemode_stats "
                + "WHERE Gamemode = ? ORDER BY Wins DESC, Player_UUID ASC LIMIT 10";

        private Constants()
        {
        }
    }
}
