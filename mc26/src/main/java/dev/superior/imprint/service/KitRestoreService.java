package dev.superior.imprint.service;

import dev.superior.imprint.model.Kit;
import dev.superior.imprint.model.KitItem;
import dev.superior.imprint.model.KitSlotType;
import dev.superior.imprint.permission.KitPermissionManager;
import dev.superior.imprint.theme.ImprintTheme;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.TextColor;
import net.minecraft.world.item.ItemStack;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Handles imprint restoration in Minecraft 26 using legitimate server-authorized mechanisms.
 */
public class KitRestoreService {
    private static final Logger LOGGER = LoggerFactory.getLogger("Imprint/Restore");

    public static RestoreResult restoreKit(Kit kit, Minecraft client) {
        if (client == null || client.player == null || client.gameMode == null) {
            return RestoreResult.error(ImprintTheme.error("Player or game mode is unavailable."));
        }

        if (kit == null) {
            return RestoreResult.error(ImprintTheme.error("Imprint is null."));
        }

        if (!KitPermissionManager.hasLoadPermission(client)) {
            return RestoreResult.permissionDenied(KitPermissionManager.getPermissionDeniedText());
        }

        boolean isCreative = KitPermissionManager.isCreative(client.player);
        boolean isOp = KitPermissionManager.isOp(client.player);

        if (isCreative) {
            return restoreViaCreativePackets(kit, client);
        } else if (isOp) {
            return restoreViaOpCommands(kit, client);
        } else {
            return RestoreResult.permissionDenied(KitPermissionManager.getPermissionDeniedText());
        }
    }

    private static RestoreResult restoreViaCreativePackets(Kit kit, Minecraft client) {
        try {
            int restoredCount = 0;
            boolean anyChanged = false;
            for (int i = 0; i < Kit.TOTAL_SLOTS; i++) {
                KitItem item = kit.getItem(i);
                ItemStack stack = item != null ? item.getItemStack().copy() : ItemStack.EMPTY;
                ItemStack currentStack = getPlayerUnifiedStack(client.player, i);

                boolean targetEmpty = stack == null || stack.isEmpty();
                boolean currentEmpty = currentStack == null || currentStack.isEmpty();

                if (targetEmpty && currentEmpty) {
                    continue;
                }

                if (!targetEmpty && !currentEmpty && ItemStack.matches(currentStack, stack)) {
                    restoredCount++;
                    continue;
                }

                int containerSlot = KitSlotType.toContainerSlot(i);
                ItemStack finalStack = targetEmpty ? ItemStack.EMPTY : stack.copy();

                client.gameMode.handleCreativeModeItemAdd(finalStack.copy(), containerSlot);
                setPlayerUnifiedStack(client.player, i, finalStack.copy());

                if (client.player.inventoryMenu.slots.size() > containerSlot) {
                    client.player.inventoryMenu.getSlot(containerSlot).set(finalStack.copy());
                }

                if (!targetEmpty) {
                    restoredCount++;
                }
                anyChanged = true;
            }

            if (anyChanged) {
                client.player.getInventory().setChanged();
                client.player.inventoryMenu.broadcastChanges();
                if (client.player.containerMenu != null && client.player.containerMenu != client.player.inventoryMenu) {
                    client.player.containerMenu.broadcastChanges();
                }
            }

            LOGGER.info("Imprint '{}' restored via creative packets. Total items: {}", kit.getName(), restoredCount);

            MutableComponent feedback = ImprintTheme.badge()
                    .append(Component.literal("Loaded imprint ").withStyle(s -> s.withColor(TextColor.fromRgb(ImprintTheme.SILVER))))
                    .append(ImprintTheme.gradient(kit.getName(), ImprintTheme.PURE_WHITE, ImprintTheme.PLATINUM, true))
                    .append(Component.literal(" (" + restoredCount + " items restored).").withStyle(s -> s.withColor(TextColor.fromRgb(ImprintTheme.SLATE))));

            return RestoreResult.success(feedback);
        } catch (Exception e) {
            LOGGER.error("Failed to restore imprint '{}' via creative packets", kit.getName(), e);
            return RestoreResult.error(ImprintTheme.error("Failed to restore imprint: " + e.getMessage()));
        }
    }

    private static RestoreResult restoreViaOpCommands(Kit kit, Minecraft client) {
        try {
            int restoredCount = 0;
            HolderLookup.Provider registries = client.level != null
                    ? client.level.registryAccess()
                    : null;

            for (int i = 0; i < Kit.TOTAL_SLOTS; i++) {
                KitItem item = kit.getItem(i);
                ItemStack stack = item != null ? item.getItemStack() : ItemStack.EMPTY;
                String commandSlot = KitSlotType.toCommandSlot(i);

                ItemStack currentStack = getPlayerUnifiedStack(client.player, i);
                boolean targetEmpty = stack == null || stack.isEmpty();
                boolean currentEmpty = currentStack == null || currentStack.isEmpty();

                if (targetEmpty && currentEmpty) {
                    continue;
                }

                if (!targetEmpty && !currentEmpty && ItemStack.matches(currentStack, stack)) {
                    restoredCount++;
                    continue;
                }

                String itemStr;
                if (targetEmpty) {
                    itemStr = "air";
                } else {
                    String formatted = BuiltInRegistries.ITEM.getKey(stack.getItem()).toString();
                    itemStr = formatted + " " + stack.getCount();
                    restoredCount++;
                }

                client.player.connection.sendCommand("item replace entity @s " + commandSlot + " with " + itemStr);
                setPlayerUnifiedStack(client.player, i, stack.copy());
            }

            if (client.player != null) {
                client.player.getInventory().setChanged();
                client.player.inventoryMenu.broadcastChanges();
            }

            LOGGER.info("Imprint '{}' restored via OP commands. Total items: {}", kit.getName(), restoredCount);

            MutableComponent feedback = ImprintTheme.badge()
                    .append(Component.literal("Loaded imprint ").withStyle(s -> s.withColor(TextColor.fromRgb(ImprintTheme.SILVER))))
                    .append(ImprintTheme.gradient(kit.getName(), ImprintTheme.PURE_WHITE, ImprintTheme.PLATINUM, true))
                    .append(Component.literal(" via OP commands (" + restoredCount + " items).").withStyle(s -> s.withColor(TextColor.fromRgb(ImprintTheme.SLATE))));

            return RestoreResult.success(feedback);
        } catch (Exception e) {
            LOGGER.error("Failed to restore imprint '{}' via OP commands", kit.getName(), e);
            return RestoreResult.error(ImprintTheme.error("Failed to execute restoration commands: " + e.getMessage()));
        }
    }

    public static ItemStack getPlayerUnifiedStack(LocalPlayer player, int unifiedIndex) {
        if (player == null || player.getInventory() == null) return ItemStack.EMPTY;
        if (unifiedIndex < 0 || unifiedIndex >= Kit.TOTAL_SLOTS) return ItemStack.EMPTY;
        return player.getInventory().getItem(unifiedIndex);
    }

    public static void setPlayerUnifiedStack(LocalPlayer player, int unifiedIndex, ItemStack stack) {
        if (player == null || player.getInventory() == null) return;
        if (unifiedIndex < 0 || unifiedIndex >= Kit.TOTAL_SLOTS) return;
        player.getInventory().setItem(unifiedIndex, stack);
    }
}
