package dev.superior.imprint.permission;

import dev.superior.imprint.theme.ImprintTheme;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.text.Text;

/**
 * Handles permission validation for Imprint operations.
 * Strictly checks for legitimate server permissions (Creative mode or OP level >= 2).
 * Never allows bypasses or unauthorized packets.
 */
public class KitPermissionManager {

    public static boolean hasLoadPermission(MinecraftClient client) {
        if (client == null || client.player == null) {
            return false;
        }

        ClientPlayerEntity player = client.player;

        if (isCreative(player)) {
            return true;
        }

        if (isOp(player)) {
            return true;
        }

        return false;
    }

    public static boolean isCreative(ClientPlayerEntity player) {
        if (player == null) return false;
        if (player.getAbilities() != null && player.getAbilities().creativeMode) {
            return true;
        }
        try {
            java.lang.reflect.Method m = player.getClass().getMethod("isCreative");
            return (Boolean) m.invoke(player);
        } catch (Throwable ignored) {}
        return false;
    }

    public static boolean isOp(ClientPlayerEntity player) {
        if (player == null) return false;

        // 1. Try hasPermissionLevel(2) (1.21.0 - 1.21.4)
        try {
            java.lang.reflect.Method m = player.getClass().getMethod("hasPermissionLevel", int.class);
            return (Boolean) m.invoke(player, 2);
        } catch (Throwable ignored) {}

        // 2. Try getPermissionLevel() >= 2 (1.21.5 - 1.21.10)
        try {
            java.lang.reflect.Method m = player.getClass().getMethod("getPermissionLevel");
            return ((Integer) m.invoke(player)) >= 2;
        } catch (Throwable ignored) {}

        // 3. Try getPermissions() (1.21.11+)
        try {
            java.lang.reflect.Method m = player.getClass().getMethod("getPermissions");
            Object permSet = m.invoke(player);
            if (permSet != null) {
                Class<?> permsClass = Class.forName("net.minecraft.command.permission.Permissions");
                Object gamemasterPerm = permsClass.getField("COMMANDS_GAMEMASTER").get(null);
                java.lang.reflect.Method hasPerm = permSet.getClass().getMethod("hasPermission", Class.forName("net.minecraft.command.permission.Permission"));
                return (Boolean) hasPerm.invoke(permSet, gamemasterPerm);
            }
        } catch (Throwable ignored) {}

        return false;
    }

    public static final String RAW_DENIED_MESSAGE = "You need permission to load a kit on this server.";

    public static Text getPermissionDeniedText() {
        return ImprintTheme.error(RAW_DENIED_MESSAGE);
    }
}
