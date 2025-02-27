package com.dominicfeliton.chatpolls;

import com.dominicfeliton.chatpolls.util.GenericRunnable;

public abstract class ChatPollsHelper {

    public abstract void registerEventHandlers();

    // Scheduler Methods
    public enum SchedulerType {
        GLOBAL,
        REGION,
        ENTITY,
        SYNC,
        ASYNC;
    }

    public abstract void cleanupTasks();

    public abstract void runAsync(GenericRunnable in, SchedulerType schedulerType);

    public abstract void runAsync(GenericRunnable in, SchedulerType schedulerType, Object[] schedulerObj);

    public abstract void runAsync(boolean serverMustBeRunning, GenericRunnable in, SchedulerType schedulerType, Object[] schedulerObj);

    public abstract void runAsync(boolean serverMustBeRunning, int delay, GenericRunnable in, SchedulerType schedulerType, Object[] schedulerObj);

    public abstract void runAsyncRepeating(boolean serverMustBeRunning, int delay, int repeatTime, GenericRunnable in, SchedulerType schedulerType, Object[] schedulerObj);

    public abstract void runAsyncRepeating(boolean serverMustBeRunning, int repeatTime, GenericRunnable in, SchedulerType schedulerType, Object[] schedulerObj);

    public abstract void runSync(GenericRunnable in, SchedulerType schedulerType);

    public abstract void runSync(GenericRunnable in, SchedulerType schedulerType, Object[] schedulerObj);

    public abstract void runSync(boolean serverMustBeRunning, GenericRunnable in, SchedulerType schedulerType, Object[] schedulerObj);

    public abstract void runSync(boolean serverMustBeRunning, int delay, GenericRunnable in, SchedulerType schedulerType, Object[] schedulerObj);

    public abstract void runSyncRepeating(boolean serverMustBeRunning, int delay, int repeatTime, GenericRunnable in, SchedulerType schedulerType, Object[] schedulerObj);

    public abstract void runSyncRepeating(boolean serverMustBeRunning, int repeatTime, GenericRunnable in, SchedulerType schedulerType, Object[] schedulerObj);

}
