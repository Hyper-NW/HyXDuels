package me.alphatct3209.duels.game.modes.runtime;

import org.bukkit.scheduler.BukkitTask;

import java.util.ArrayList;
import java.util.List;

abstract class TrackedModeRuntime implements ModeRuntimeService
{
    private final List<BukkitTask> tasks = new ArrayList<>();

    protected final void track(BukkitTask task) { tasks.add(task); }

    @Override
    public final void stop()
    {
        for (BukkitTask task : List.copyOf(tasks))
            if (!task.isCancelled()) task.cancel();
        tasks.clear();
    }
}
