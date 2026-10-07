package dev.superior.imprint.service;

import net.minecraft.text.Text;

/**
 * Result of an imprint restoration attempt.
 */
public record RestoreResult(
        Status status,
        Text feedback
) {
    public enum Status {
        SUCCESS,
        PERMISSION_DENIED,
        ERROR
    }

    public static RestoreResult success(Text text) {
        return new RestoreResult(Status.SUCCESS, text);
    }

    public static RestoreResult permissionDenied(Text text) {
        return new RestoreResult(Status.PERMISSION_DENIED, text);
    }

    public static RestoreResult error(Text text) {
        return new RestoreResult(Status.ERROR, text);
    }

    public boolean isSuccess() {
        return status == Status.SUCCESS;
    }
}
