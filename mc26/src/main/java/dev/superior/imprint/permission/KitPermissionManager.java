package dev.superior.imprint.permission;

import dev.superior.imprint.theme.ImprintTheme;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.chat.Component;
import net.minecraft.server.permissions.Permissions;

/**
 * Handles permission validation for Imprint operations in Minecraft 26.
 */
public class KitPermissionManager {

    public static boolean hasLoadPermission(Minecraft client) {
        if (client == null || client.player == null) {
            return false;
        }

        LocalPlayer player = client.player;
        return isCreative(player) || isOp(player);
    }

    public static boolean isCreative(LocalPlayer player) {
        if (player == null) return false;
        return player.isCreative() || (player.getAbilities() != null && player.getAbilities().instabuild);
    }

    public static boolean isOp(LocalPlayer player) {
        if (player == null) return false;
        try {
            if (player.permissions() != null && player.permissions().hasPermission(Permissions.COMMANDS_GAMEMASTER)) {
                return true;
            }
        } catch (Throwable ignored) {}
        return false;
    }

    public static final String RAW_DENIED_MESSAGE = "You need permission to load a kit on this server.";

    public static Component getPermissionDeniedText() {
        return ImprintTheme.error(RAW_DENIED_MESSAGE);
    }
}
