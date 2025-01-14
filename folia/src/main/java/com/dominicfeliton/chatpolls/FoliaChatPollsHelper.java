package com.dominicfeliton.chatpolls;

import com.dominicfeliton.chatpolls.ChatPollsHelper.SchedulerType;
import com.dominicfeliton.chatpolls.util.CommonRefs;
import com.dominicfeliton.chatpolls.util.GenericRunnable;
import io.papermc.paper.threadedregions.scheduler.ScheduledTask;

import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Entity;

import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

public class FoliaChatPollsHelper extends ChatPollsHelper {

    private final ChatPolls main = ChatPolls.instance;
    private final CommonRefs refs = main.getCommonRefs();

    @Override
    public void registerEventHandlers() {
    //    TODO
    }

    @Override
    public void cleanupTasks() {
        refs.debugMsg("Folia cleanup tasks...");
        // Cancel all tasks for this plugin on Folia’s schedulers
        main.getServer().getGlobalRegionScheduler().cancelTasks(main);
        main.getServer().getAsyncScheduler().cancelTasks(main);
    }

    // ------------------------------------------------------------------------
    //  runAsync + runSync methods
    // ------------------------------------------------------------------------

    @Override
    public void runAsync(GenericRunnable in, SchedulerType schedulerType) {
        runAsync(true, in, schedulerType, null);
    }

    @Override
    public void runAsync(GenericRunnable in, SchedulerType schedulerType, Object[] schedulerObj) {
        runAsync(true, in, schedulerType, schedulerObj);
    }

    @Override
    public void runAsync(boolean serverMustBeRunning, GenericRunnable in, SchedulerType schedulerType, Object[] schedulerObj) {
        runAsync(serverMustBeRunning, 0, in, schedulerType, schedulerObj);
    }

    @Override
    public void runAsync(boolean serverMustBeRunning, int delay, GenericRunnable in, SchedulerType schedulerType, Object[] schedulerObj) {
        // Non-repeating call => pass repeatTime = 0
        runAsyncRepeating(serverMustBeRunning, delay, 0, in, schedulerType, schedulerObj);
    }

    @Override
    public void runAsyncRepeating(boolean serverMustBeRunning, int delay, int repeatTime,
                                  GenericRunnable in, SchedulerType schedulerType, Object[] schedulerObj) {
        refs.debugMsg("Requesting an async (Folia) task: " + schedulerType.name() +
                " | Delay: " + delay + " | Repeat: " + repeatTime);

        // If server must be running, skip if server is stopping
        if (serverMustBeRunning && refs.serverIsStopping()) {
            return;
        }
        // Otherwise schedule
        runTask(schedulerType, in, schedulerObj, delay, repeatTime);
    }

    @Override
    public void runAsyncRepeating(boolean serverMustBeRunning, int repeatTime, GenericRunnable in,
                                  SchedulerType schedulerType, Object[] schedulerObj) {
        runAsyncRepeating(serverMustBeRunning, 0, repeatTime, in, schedulerType, schedulerObj);
    }

    // ------------------------------------------------------------------------
    //  "Sync" calls on Folia 
    //  (Folia has no true single-thread "main thread," so we just route sync->async or use global region.)
    // ------------------------------------------------------------------------

    @Override
    public void runSync(GenericRunnable in, SchedulerType schedulerType) {
        runSync(true, in, schedulerType, null);
    }

    @Override
    public void runSync(GenericRunnable in, SchedulerType schedulerType, Object[] schedulerObj) {
        runSync(true, in, schedulerType, schedulerObj);
    }

    @Override
    public void runSync(boolean serverMustBeRunning, GenericRunnable in, SchedulerType schedulerType, Object[] schedulerObj) {
        runSync(serverMustBeRunning, 0, in, schedulerType, schedulerObj);
    }

    @Override
    public void runSync(boolean serverMustBeRunning, int delay, GenericRunnable in, SchedulerType schedulerType, Object[] schedulerObj) {
        // For Folia, let's just route "sync" to the same approach 
        // or specifically run on "global region" if you desire. 
        // For simplicity, do the same:
        runAsync(serverMustBeRunning, delay, in, schedulerType, schedulerObj);
    }

    @Override
    public void runSyncRepeating(boolean serverMustBeRunning, int delay, int repeatTime,
                                 GenericRunnable in, SchedulerType schedulerType, Object[] schedulerObj) {
        runAsyncRepeating(serverMustBeRunning, delay, repeatTime, in, schedulerType, schedulerObj);
    }

    @Override
    public void runSyncRepeating(boolean serverMustBeRunning, int repeatTime, GenericRunnable in,
                                 SchedulerType schedulerType, Object[] schedulerObj) {
        runSyncRepeating(serverMustBeRunning, 0, repeatTime, in, schedulerType, schedulerObj);
    }

    // ------------------------------------------------------------------------
    //  Main utility for scheduling tasks on Folia
    // ------------------------------------------------------------------------

    /**
     * Helper that schedules tasks based on the chosen SchedulerType (GLOBAL, REGION, ENTITY, ASYNC).
     * 
     * @param schedulerType which Folia scheduler to use
     * @param task          our GenericRunnable
     * @param taskObjs      optional array containing world/entity/chunk params, etc.
     * @param delay         delay in ticks (0 for immediate)
     * @param period        repeating interval in ticks (0 means no repeat)
     */
    private void runTask(SchedulerType schedulerType, GenericRunnable task, Object[] taskObjs, long delay, long period) {
        // We'll store the resulting Folia ScheduledTask in the GenericRunnable.
        ScheduledTask scheduledTask = null;

        // Convert the GenericRunnable into a Consumer<ScheduledTask>
        Consumer<ScheduledTask> consumer = (ignored) -> task.run();

        switch (schedulerType) {
            case GLOBAL:
                // Global region scheduling
                if (period > 0) {
                    // Repeating
                    scheduledTask = main.getServer().getGlobalRegionScheduler()
                        .runAtFixedRate(main, consumer, delay, period);
                } else if (delay > 0) {
                    // Delayed
                    scheduledTask = main.getServer().getGlobalRegionScheduler()
                        .runDelayed(main, consumer, delay);
                } else {
                    // Immediate
                    scheduledTask = main.getServer().getGlobalRegionScheduler()
                        .run(main, consumer);
                }
                break;

            case REGION:
                if (taskObjs == null || taskObjs.length == 0) {
                    main.getLogger().severe("Requested region scheduler but did not pass a location/world! Please contact the dev.");
                    return;
                }
                // We handle either a Location or World + chunk coords
                if (taskObjs[0] instanceof Location) {
                    Location loc = (Location) taskObjs[0];
                    if (period > 0) {
                        scheduledTask = main.getServer().getRegionScheduler()
                            .runAtFixedRate(main, loc, consumer, delay, period);
                    } else if (delay > 0) {
                        scheduledTask = main.getServer().getRegionScheduler()
                            .runDelayed(main, loc, consumer, delay);
                    } else {
                        scheduledTask = main.getServer().getRegionScheduler()
                            .run(main, loc, consumer);
                    }
                } else if (taskObjs[0] instanceof World) {
                    // Must also have chunkX, chunkZ
                    if (taskObjs.length < 3 || !(taskObjs[1] instanceof Integer) || !(taskObjs[2] instanceof Integer)) {
                        main.getLogger().severe("Requested region scheduler with a World but missing chunkX/ chunkZ!");
                        return;
                    }
                    World w = (World) taskObjs[0];
                    int chunkX = (Integer) taskObjs[1];
                    int chunkZ = (Integer) taskObjs[2];

                    if (period > 0) {
                        scheduledTask = main.getServer().getRegionScheduler()
                            .runAtFixedRate(main, w, chunkX, chunkZ, consumer, delay, period);
                    } else if (delay > 0) {
                        scheduledTask = main.getServer().getRegionScheduler()
                            .runDelayed(main, w, chunkX, chunkZ, consumer, delay);
                    } else {
                        scheduledTask = main.getServer().getRegionScheduler()
                            .run(main, w, chunkX, chunkZ, consumer);
                    }
                } else {
                    main.getLogger().severe("Requested region scheduler but did not pass a valid location or world!");
                    return;
                }
                break;

            case ENTITY:
                if (taskObjs == null || taskObjs.length == 0 || !(taskObjs[0] instanceof Entity)) {
                    main.getLogger().severe("Requested entity scheduler but no Entity passed!");
                    return;
                }
                Entity entity = (Entity) taskObjs[0];

                if (period > 0) {
                    scheduledTask = entity.getScheduler().runAtFixedRate(main, consumer, null, delay, period);
                } else if (delay > 0) {
                    scheduledTask = entity.getScheduler().runDelayed(main, consumer, null, delay);
                } else {
                    scheduledTask = entity.getScheduler().run(main, consumer, null);
                }
                break;

            case ASYNC:
                // Async scheduling uses time-based (seconds) calls
                // dividing the user-specified ticks by 20.
                if (period > 0) {
                    scheduledTask = main.getServer().getAsyncScheduler()
                        .runAtFixedRate(main, consumer, delay / 20, period / 20, TimeUnit.SECONDS);
                } else if (delay > 0) {
                    scheduledTask = main.getServer().getAsyncScheduler()
                        .runDelayed(main, consumer, delay / 20, TimeUnit.SECONDS);
                } else {
                    scheduledTask = main.getServer().getAsyncScheduler()
                        .runNow(main, consumer);
                }
                break;
        }

        // Store the scheduledTask in our GenericRunnable if not null
        if (scheduledTask != null) {
            task.setPlatformTaskHandle(scheduledTask);
        }
    }
}
