package com.dominicfeliton.chatpolls.util;

import java.lang.reflect.Method;

public abstract class GenericRunnable implements Cloneable, Runnable {

    private static Class<?> bukkitTaskClass;    // Reflection for Bukkit
    private static Method bukkitCancelMethod;   // Reflection for bukkitTaskClass#cancel

    static {
        try {
            // Attempt to load BukkitTask at runtime
            bukkitTaskClass = Class.forName("org.bukkit.scheduler.BukkitTask");
            // Grab its cancel() method
            bukkitCancelMethod = bukkitTaskClass.getMethod("cancel");
        } catch (ClassNotFoundException | NoSuchMethodException e) {
            // Not running on Bukkit-based platform, or method signature changed
            bukkitTaskClass = null;
            bukkitCancelMethod = null;
        }
    }

    /**
     * Whether this runnable has been cancelled (logic flag).
     * If true, it will no longer execute.
     */
    private volatile boolean isCancelled = false;

    /**
     * A handle to the platform’s actual scheduled task (e.g., BukkitTask,
     * Folia ScheduledTask, Velocity task handle, etc.). May be null if not scheduled.
     */
    private Object platformTaskHandle;

    /**
     * A human-readable name for logging/debugging.
     */
    private String name = "";

    @Override
    public final void run() {
        if (isCancelled) {
            cancelPlatformTaskIfNeeded();
            return;
        }
        execute();
    }

    /**
     * Where the actual logic for this runnable is implemented by subclasses.
     */
    protected abstract void execute();

    /**
     * Cancels this runnable, optionally also cancelling the
     * platform’s scheduled task if supported (e.g., Bukkit).
     */
    public void cancel() {
        isCancelled = true;
        cancelPlatformTaskIfNeeded();
    }

    /**
     * Use reflection to see if `platformTaskHandle` is a BukkitTask, and if so, call `cancel()`.
     * Extend this method if you want to also handle Folia or Velocity at runtime without imports.
     */
    private void cancelPlatformTaskIfNeeded() {
        if (platformTaskHandle == null) return;

        // If we found org.bukkit.scheduler.BukkitTask on classpath:
        if (bukkitTaskClass != null && bukkitCancelMethod != null) {
            if (bukkitTaskClass.isInstance(platformTaskHandle)) {
                try {
                    bukkitCancelMethod.invoke(platformTaskHandle);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
    }

    /**
     * @return true if this runnable has been cancelled.
     */
    public boolean isCancelled() {
        return isCancelled;
    }

    /**
     * @return The underlying platform's task handle (e.g., BukkitTask), if any.
     */
    public Object getPlatformTaskHandle() {
        return platformTaskHandle;
    }

    /**
     * Sets the underlying platform's scheduled task reference.
     *
     * @param handle The platform task object (BukkitTask, ScheduledTask, etc.).
     */
    public void setPlatformTaskHandle(Object handle) {
        this.platformTaskHandle = handle;
    }

    /**
     * @return The assigned name of this task (for logging).
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
     * Clone the runnable. Typically you want to reset the cancelled flag
     * and clear out the platform task handle in the clone.
     */
    @Override
    public GenericRunnable clone() {
        try {
            GenericRunnable cloned = (GenericRunnable) super.clone();
            cloned.isCancelled = false;         // reset cancellation
            cloned.platformTaskHandle = null;   // new clone => no scheduled handle
            return cloned;
        } catch (CloneNotSupportedException e) {
            // Should never happen since we implement Cloneable
            throw new AssertionError(e);
        }
    }
}