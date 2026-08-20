package me.alphatct3209.duels.game.arenas;

import org.bukkit.Location;

import java.util.HashMap;
import java.util.UUID;

/**
 * ArenaConfig -- The idea is to create all the information an Arena needs, then create it.
 * This is better that what I used to do because if you created only half an Arena, problems could occur.
 * This shouldn't happen now.
 *
 * @author Austin Dart (alphatct3209)
 */
public class ArenaConfig
{

    public static final HashMap<UUID, ArenaConfig> creationMap = new HashMap<>();

    private final int id;
    private final String name;

    private Location spawnOne;
    private Location spawnTwo;
    private Location lobby;
    private int countdownSeconds;
    private ArenaSettings settings = new ArenaSettings();

    public ArenaConfig(int id, String name)
    {
        this.id = id;
        this.name = name;
        this.countdownSeconds = 15;
    }

    public boolean isFinished()
    {
        return spawnOne != null && spawnTwo != null && lobby != null;
    }

    public int getId()
    {
        return id;
    }

    public String getName()
    {
        return name;
    }

    public Location getSpawnOne()
    {
        return spawnOne == null ? null : spawnOne.clone();
    }

    public void setSpawnOne(Location spawnOne)
    {
        this.spawnOne = spawnOne == null ? null : spawnOne.clone();
    }

    public Location getSpawnTwo()
    {
        return spawnTwo == null ? null : spawnTwo.clone();
    }

    public void setSpawnTwo(Location spawnTwo)
    {
        this.spawnTwo = spawnTwo == null ? null : spawnTwo.clone();
    }

    public Location getLobby()
    {
        return lobby == null ? null : lobby.clone();
    }

    public void setLobby(Location lobby)
    {
        this.lobby = lobby == null ? null : lobby.clone();
    }

    public int getCountdownSeconds()
    {
        return countdownSeconds;
    }

    public void setCountdownSeconds(int countdownSeconds)
    {
        this.countdownSeconds = countdownSeconds;
    }

    public ArenaSettings getSettings()
    {
        return settings;
    }

    public void setSetting(ArenaSettings.Flag flag, boolean value)
    {
        settings = settings.with(flag, value);
    }

}
