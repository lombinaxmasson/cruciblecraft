package com.masson.cruciblecraft.api.tool;

/**
 * A world object that can be triggered by the GT6-style Remote Activator.
 *
 * @return {@code true} when the target remains registered after activation
 */
public interface RemoteActivatable {
    boolean remoteActivate();
}
