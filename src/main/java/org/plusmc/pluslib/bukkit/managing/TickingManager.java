package org.plusmc.pluslib.bukkit.managing;

import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;
import org.plusmc.pluslib.bukkit.managed.Loadable;
import org.plusmc.pluslib.bukkit.managed.Tickable;
import org.plusmc.pluslibcore.reflection.bukkitpaper.timings.WrappedTimings;

import java.util.*;

/**
 * Provides a way to register and unregister {@link Tickable} objects.
 */
@SuppressWarnings("unused")
public class TickingManager extends BaseManager {
    private List<Map.Entry<Tickable, WrappedTimings>> tickables;
    private List<Tickable> markForRemoval;
    private BukkitTask tickingTask;
    private BukkitTask asyncTickingTask;
    private long tick;
    private long asyncTick;

    protected TickingManager(JavaPlugin plugin) {
        super(plugin);
    }

    @Override
    protected void init() {
        tick = 0;
        asyncTick = 0;
        tickables = new ArrayList<>();
        markForRemoval = new ArrayList<>();
        tickingTask = Bukkit.getScheduler().runTaskTimer(getPlugin(), this::tick, 0L, 1L);
        asyncTickingTask = Bukkit.getScheduler().runTaskTimerAsynchronously(getPlugin(), this::asyncTick, 0L, 1L);
    }

    private void tick() {
        for (Iterator<Map.Entry<Tickable, WrappedTimings>> iterator = tickables.iterator(); iterator.hasNext(); ) {
            Map.Entry<Tickable, WrappedTimings> entry = iterator.next();
            Tickable tickable = entry.getKey();
            WrappedTimings timings = entry.getValue();
            if (markForRemoval.contains(tickable)) {
                iterator.remove();
                markForRemoval.remove(tickable);
                continue;
            }

            if (!(tickable.isRunning() && !tickable.isAsync())) continue;
            timings.startTiming();
            try {
                tickable.tick(tick);
            } catch (Exception e) {
                e.printStackTrace();
            }
            timings.stopTiming();
        }
        tick++;
    }

    private void asyncTick() {
        for (Iterator<Map.Entry<Tickable, WrappedTimings>> iterator = tickables.iterator(); iterator.hasNext(); ) {
            Map.Entry<Tickable, WrappedTimings> entry = iterator.next();
            Tickable tickable = entry.getKey();
            WrappedTimings timings = entry.getValue();
            if (markForRemoval.contains(tickable)) {
                iterator.remove();
                markForRemoval.remove(tickable);
                continue;
            }

            if (!(tickable.isRunning() && tickable.isAsync())) continue;
            timings.startTiming();
            try {
                tickable.tick(tick);
            } catch (Exception e) {
                e.printStackTrace();
            }
            timings.stopTiming();
        }
        asyncTick++;
    }

    @Override
    public Class<? extends Loadable> getManaged() {
        return Tickable.class;
    }

    @Override
    protected void register(Loadable loadable) {
        if (!(loadable instanceof Tickable tickable)) return;
        tickables.add(new HashMap.SimpleEntry<>(tickable, WrappedTimings.create(getPlugin(), tickable.getName() + "-(tickable)")));
        getPlugin().getLogger().info("Registered " + tickable.getName() + " to the ticking manager.");
    }

    @Override
    protected void unregister(Loadable loadable) {
        if (!(loadable instanceof Tickable tickable)) return;
        tickables.forEach(entry -> {
            if (entry.getKey().equals(tickable))
                markForRemoval.add(tickable);
        });
        getPlugin().getLogger().info("Unregistered " + tickable.getClass().getSimpleName() + " from the ticking manager.");
    }

    @Override
    protected void shutdown() {
        tickingTask.cancel();
        asyncTickingTask.cancel();
        tickables.clear();
    }
}
