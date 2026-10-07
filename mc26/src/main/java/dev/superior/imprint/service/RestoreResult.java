package dev.superior.imprint.service;

import net.minecraft.network.chat.Component;

/**
 * Result of attempting to restore an imprint (Minecraft 26).
 */
public record RestoreResult(
        boolean success,
        boolean permissionDenied,
        Component feedback
) {
    public static RestoreResult success(Component feedback) {
        return new RestoreResult(true, false, feedback);
    }

    public static RestoreResult permissionDenied(Component feedback) {
        return new RestoreResult(false, true, feedback);
    }

    public static RestoreResult error(Component feedback) {
        return new RestoreResult(false, false, feedback);
    }

    public boolean isSuccess() {
        return success;
    }

    public boolean isPermissionDenied() {
        return permissionDenied;
    }
}
