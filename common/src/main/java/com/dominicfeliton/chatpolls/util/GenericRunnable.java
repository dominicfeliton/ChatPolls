package com.dominicfeliton.chatpolls.util;

import java.lang.reflect.Method;

/**
 * A generic runnable wrapper that can schedule logic on various platforms
 * (e.g., Bukkit, Folia, Velocity) and store a handle to the scheduled task.
 * 
 * <p>When {@link #cancel()} is called, it attempts to cancel the underlying
 * scheduled task if recognized (e.g., BukkitTask or Folia's ScheduledTask).
 */
public abstract class GenericRunnable implements Cloneable, Runnable {

    // Reflection for Bukkit tasks
    private static Class<?> bukkitTaskClass;
    private static Method bukkitCancelMethod;

    // Reflection for Folia tasks
    private static Class<?> foliaScheduledTaskClass;
    private static Method foliaScheduledTaskCancelMethod;

    static {
        // Attempt to load BukkitTask + its cancel() method
        try {
            bukkitTaskClass = Class.forName("org.bukkit.scheduler.BukkitTask");
            bukkitCancelMethod = bukkitTaskClass.getMethod("cancel");
        } catch (ClassNotFoundException | NoSuchMethodException e) {
            // Not running on Bukkit-based platform, or method signature changed
            bukkitTaskClass = null;
            bukkitCancelMethod = null;
        }

        // Attempt to load Folia ScheduledTask + its cancel() method
        try {
            foliaScheduledTaskClass = Class.forName("io.papermc.paper.threadedregions.scheduler.ScheduledTask");
            foliaScheduledTaskCancelMethod = foliaScheduledTaskClass.getMethod("cancel");
        } catch (ClassNotFoundException | NoSuchMethodException e) {
            // Not running on Folia, or method signature changed
            foliaScheduledTaskClass = null;
            foliaScheduledTaskCancelMethod = null;
        }
    }

    /**
     * Whether this runnable has been manually cancelled (our own logic flag).
     */
    private volatile boolean isCancelled = false;

    /**
     * A handle to the underlying scheduled task (BukkitTask, Folia ScheduledTask, etc.).
     * May be null if not scheduled or if scheduling platform is unknown.
     */
    private Object platformTaskHandle;

    /**
     * A human-readable name for logging/debugging.
     */
    private String name = "";

    @Override
    public final void run() {
        // If already cancelled, try to cancel the platform's task (if any),
        // then skip execution.
        if (isCancelled) {
            cancelPlatformTaskIfNeeded();
            return;
        }
        execute();
    }

    /**
     * The core logic to be run, implemented by concrete subclasses.
     */
    protected abstract void execute();

    /**
     * Cancels this runnable (so future runs do nothing) and attempts to cancel
     * the platform’s scheduled task via reflection if recognized.
     */
    public void cancel() {
        isCancelled = true;
        cancelPlatformTaskIfNeeded();
    }

    /**
     * If we detect that platformTaskHandle is a known task type (e.g., BukkitTask or Folia),
     * we invoke the appropriate cancel() method. Otherwise, we do nothing.
     */
    private void cancelPlatformTaskIfNeeded() {
        if (platformTaskHandle == null) {
            return;
        }

        // 1) Check for BukkitTask
        if (bukkitTaskClass != null && bukkitCancelMethod != null && bukkitTaskClass.isInstance(platformTaskHandle)) {
            try {
                bukkitCancelMethod.invoke(platformTaskHandle);
                return;
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        // 2) Check for Folia ScheduledTask
        if (foliaScheduledTaskClass != null && foliaScheduledTaskCancelMethod != null
                && foliaScheduledTaskClass.isInstance(platformTaskHandle)) {
            try {
                // The cancel() method returns a ScheduledTask.CancelledState, but we don't need it
                foliaScheduledTaskCancelMethod.invoke(platformTaskHandle);
                return;
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        // Could add more reflection checks for other platforms here.
    }

    /**
     * @return true if this runnable has been cancelled via {@link #cancel()}.
     */
    public boolean isCancelled() {
        return isCancelled;
    }

    /**
     * Sets the underlying platform's scheduled task reference.
     * E.g., a BukkitTask or Folia ScheduledTask instance.
     */
    public void setPlatformTaskHandle(Object handle) {
        this.platformTaskHandle = handle;
    }

    /**
     * @return the underlying platform's scheduled task handle, if any.
     */
    public Object getPlatformTaskHandle() {
        return platformTaskHandle;
    }

    /**
     * @return the assigned name of this task (for logging).
     */
    public String getName() {
        return name;
    }

    /**
     * Sets a human-readable name for logging/debugging.
     */
    public void setName(String name) {
        this.name = name;
    }

    /**
     * Creates a clone of this runnable, typically resetting cancellation status
     * and clearing the platform task handle.
     */
    @Override
    public GenericRunnable clone() {
        try {
            GenericRunnable cloned = (GenericRunnable) super.clone();
            cloned.isCancelled = false;         // reset the cancellation flag
            cloned.platformTaskHandle = null;   // new clone => no existing scheduled handle
            return cloned;
        } catch (CloneNotSupportedException e) {
            // Should never happen, since we implement Cloneable
            throw new AssertionError(e);
        }
    }
}
