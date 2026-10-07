package dev.superior.imprint.service;

import dev.superior.imprint.model.Kit;
import dev.superior.imprint.model.KitItem;
import dev.superior.imprint.model.KitSlotType;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.ItemStack;

/**
 * Captures the player's local inventory without modifying anything or contacting the server.
 */
public class KitCaptureService {

    public static Kit captureCurrentInventory(String kitName, ClientPlayerEntity player) {
        if (player == null) {
            throw new IllegalArgumentException("Player cannot be null");
        }

        PlayerInventory inventory = player.getInventory();
        String mcVer = "1.21";
        try {
            Object gv = net.minecraft.SharedConstants.getGameVersion();
            try {
                mcVer = (String) gv.getClass().getMethod("getName").invoke(gv);
            } catch (NoSuchMethodException e) {
                mcVer = (String) gv.getClass().getMethod("name").invoke(gv);
            }
        } catch (Throwable ignored) {}
        Kit kit = new Kit(kitName, mcVer);

        for (int i = 0; i < Kit.TOTAL_SLOTS; i++) {
            ItemStack stack = inventory.getStack(i).copy();
            KitSlotType type = KitSlotType.fromUnifiedIndex(i);
            kit.setItem(i, new KitItem(i, type, stack));
        }

        return kit;
    }
}
