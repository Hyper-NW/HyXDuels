package me.alphatct3209.duels.display;

import io.papermc.paper.scoreboard.numbers.NumberFormat;
import org.bukkit.entity.Player;
import org.bukkit.scoreboard.DisplaySlot;
import org.bukkit.scoreboard.Objective;
import org.bukkit.scoreboard.Scoreboard;
import org.bukkit.scoreboard.ScoreboardManager;
import org.bukkit.scoreboard.Team;

import java.util.List;
import java.util.Objects;

/** One player's stable, private sidebar. */
final class SidebarDisplay
{
    private static final String[] ENTRIES = {
            "§0", "§1", "§2", "§3", "§4", "§5", "§6", "§7",
            "§8", "§9", "§a", "§b", "§c", "§d", "§e"
    };

    private final Scoreboard scoreboard;
    private final Objective objective;
    private final Team[] teams = new Team[ENTRIES.length];

    SidebarDisplay(ScoreboardManager manager, String title)
    {
        scoreboard = Objects.requireNonNull(manager, "scoreboard manager").getNewScoreboard();
        objective = scoreboard.registerNewObjective("hyxduels", "dummy", title);
        objective.numberFormat(NumberFormat.blank());
        objective.setDisplaySlot(DisplaySlot.SIDEBAR);
        for (int index = 0; index < ENTRIES.length; index++)
        {
            Team team = scoreboard.registerNewTeam(String.format("hyx%02d", index));
            team.addEntry(ENTRIES[index]);
            teams[index] = team;
        }
    }

    Scoreboard scoreboard()
    {
        return scoreboard;
    }

    void update(String title, List<String> lines)
    {
        if (!objective.getDisplayName().equals(title))
        {
            objective.setDisplayName(title);
        }
        for (int index = 0; index < ENTRIES.length; index++)
        {
            if (index < lines.size())
            {
                teams[index].setPrefix(lines.get(index));
                scoreboard.getObjective(DisplaySlot.SIDEBAR).getScore(ENTRIES[index])
                        .setScore(scoreForLine(index));
            }
            else
            {
                teams[index].setPrefix("");
                scoreboard.resetScores(ENTRIES[index]);
            }
        }
    }

    /** Minecraft renders larger scores first; descending values preserve config order. */
    static int scoreForLine(int index)
    {
        if (index < 0 || index >= ENTRIES.length)
        {
            throw new IllegalArgumentException("Sidebar line index must be from 0 through 14");
        }
        return ENTRIES.length - index;
    }

    boolean isCurrent(Player player)
    {
        return player.getScoreboard() == scoreboard;
    }
}
