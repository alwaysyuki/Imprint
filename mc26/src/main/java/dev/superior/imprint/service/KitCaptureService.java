package dev.superior.imprint.service;

import dev.superior.imprint.model.Kit;
import dev.superior.imprint.model.KitItem;
import dev.superior.imprint.model.KitSlotType;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;

/**
 * Captures the player's local inventory in Minecraft 26 without modifying anything or contacting the server.
 */
public class KitCaptureService {

    public static Kit captureCurrentInventory(String kitName, LocalPlayer player) {
        if (player == null) {
            throw new IllegalArgumentException("Player cannot be null");
        }

        Inventory inventory = player.getInventory();
        String mcVer = "26.1";
        try {
            mcVer = net.minecraft.SharedConstants.getCurrentVersion().name();
        } catch (Throwable ignored) {}
        Kit kit = new Kit(kitName, mcVer);

        for (int i = 0; i < Kit.TOTAL_SLOTS; i++) {
            ItemStack stack = inventory.getItem(i).copy();
            KitSlotType type = KitSlotType.fromUnifiedIndex(i);
            kit.setItem(i, new KitItem(i, type, stack));
        }

        return kit;
    }
}
