package com.dominicfeliton.chatpolls;

import com.dominicfeliton.chatpolls.util.CommonRefs;
import com.dominicfeliton.chatpolls.util.GenericRunnable;
import org.bukkit.Bukkit;
import org.bukkit.scheduler.BukkitTask;

public class BukkitChatPollsHelper extends ChatPollsHelper {

    protected ChatPolls main = ChatPolls.instance;
    protected CommonRefs refs = main.getCommonRefs();

    @Override
    public void registerEventHandlers() {
        // TODO
    }

    @Override
    public void cleanupTasks() {
        // Cancel + remove all tasks
        Bukkit.getScheduler().cancelTasks(main);
    }

    /*
     * -------------- RUN ASYNC --------------
     */
    @Override
    public void runAsync(GenericRunnable in, SchedulerType schedulerType) {
        runAsync(true, in, schedulerType, null);
    }

    @Override
    public void runAsync(GenericRunnable in, SchedulerType schedulerType, Object[] schedulerObj) {
        runAsync(true, in, schedulerType, schedulerObj);
    }

    @Override
    public void runAsync(boolean serverMustBeRunning, GenericRunnable in,
                         SchedulerType schedulerType, Object[] schedulerObj) {
        runAsync(serverMustBeRunning, 0, in, schedulerType, schedulerObj);
    }

    @Override
    public void runAsync(boolean serverMustBeRunning, int delay, GenericRunnable in,
                         SchedulerType schedulerType, Object[] schedulerObj) {
        refs.debugMsg("We are an async runnable on " + main.getPlatformType() +
                " Scheduler Type " + schedulerType + "! Delay: " + delay + "!");

        if (!serverMustBeRunning || !refs.serverIsStopping()) {
            // Schedule asynchronously (with optional delay)
            BukkitTask bukkitTask = Bukkit.getScheduler()
                    .runTaskLaterAsynchronously(main, in, delay);
            // Optionally store the handle in the GenericRunnable:
            in.setPlatformTaskHandle(bukkitTask);
        }
    }

    @Override
    public void runAsyncRepeating(boolean serverMustBeRunning, int delay, int repeatTime,
                                  GenericRunnable in, SchedulerType schedulerType, Object[] schedulerObj) {
        refs.debugMsg("We are an async runnable on " + main.getPlatformType() +
                " Scheduler Type " + schedulerType + "! Delay: " + delay +
                "! Repeat: " + repeatTime + "!");

        if (!serverMustBeRunning || !refs.serverIsStopping()) {
            // Repeating async
            BukkitTask bukkitTask = Bukkit.getScheduler()
                    .runTaskTimerAsynchronously(main, in, delay, repeatTime);
            in.setPlatformTaskHandle(bukkitTask);
        }
    }

    @Override
    public void runAsyncRepeating(boolean serverMustBeRunning, int repeatTime,
                                  GenericRunnable in, SchedulerType schedulerType, Object[] schedulerObj) {
        runAsyncRepeating(serverMustBeRunning, 0, repeatTime, in, schedulerType, schedulerObj);
    }

    /*
     * -------------- RUN SYNC --------------
     */
    @Override
    public void runSync(GenericRunnable in, SchedulerType schedulerType) {
        runSync(true, in, schedulerType, null);
    }

    @Override
    public void runSync(GenericRunnable in, SchedulerType schedulerType, Object[] schedulerObj) {
        runSync(true, in, schedulerType, schedulerObj);
    }

    @Override
    public void runSync(boolean serverMustBeRunning, GenericRunnable in,
                        SchedulerType schedulerType, Object[] schedulerObj) {
        runSync(serverMustBeRunning, 0, in, schedulerType, schedulerObj);
    }

    @Override
    public void runSync(boolean serverMustBeRunning, int delay, GenericRunnable in,
                        SchedulerType schedulerType, Object[] schedulerObj) {
        refs.debugMsg("We are a sync runnable on " + main.getPlatformType() +
                " Scheduler Type " + schedulerType + "! Delay: " + delay + "!");

        if (!serverMustBeRunning || !refs.serverIsStopping()) {
            BukkitTask bukkitTask = Bukkit.getScheduler()
                    .runTaskLater(main, in, delay);
            in.setPlatformTaskHandle(bukkitTask);
        }
    }

    @Override
    public void runSyncRepeating(boolean serverMustBeRunning, int delay, int repeatTime,
                                 GenericRunnable in, SchedulerType schedulerType, Object[] schedulerObj) {
        refs.debugMsg("We are a sync runnable on " + main.getPlatformType() +
                " Scheduler Type " + schedulerType + "! Delay: " + delay +
                "! Repeat: " + repeatTime + "!");

        if (!serverMustBeRunning || !refs.serverIsStopping()) {
            BukkitTask bukkitTask = Bukkit.getScheduler()
                    .runTaskTimer(main, in, delay, repeatTime);
            in.setPlatformTaskHandle(bukkitTask);
        }
    }

    @Override
    public void runSyncRepeating(boolean serverMustBeRunning, int repeatTime,
                                 GenericRunnable in, SchedulerType schedulerType, Object[] schedulerObj) {
        runSyncRepeating(serverMustBeRunning, 0, repeatTime, in, schedulerType, schedulerObj);
    }
}