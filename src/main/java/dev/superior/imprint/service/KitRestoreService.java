package dev.superior.imprint.service;

import dev.superior.imprint.model.Kit;
import dev.superior.imprint.model.KitItem;
import dev.superior.imprint.model.KitSlotType;
import dev.superior.imprint.permission.KitPermissionManager;
import dev.superior.imprint.theme.ImprintTheme;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.command.argument.ItemStackArgument;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.Registries;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.text.TextColor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Handles imprint restoration into the player's inventory using legitimate server-authorized mechanisms.
 * - Creative Mode: Uses standard CreativeInventoryAction packets (clickCreativeStack) + immediate client inventory sync.
 * - Operator in Survival: Uses legitimate /item replace entity @s commands with full component/NBT fidelity.
 * - Non-privileged players: Strictly denied without bypassing.
 */
public class KitRestoreService {
    private static final Logger LOGGER = LoggerFactory.getLogger("Imprint/Restore");

    public static RestoreResult restoreKit(Kit kit, MinecraftClient client) {
        if (client == null || client.player == null || client.interactionManager == null) {
            return RestoreResult.error(ImprintTheme.error("Player or interaction manager is unavailable."));
        }

        if (kit == null) {
            return RestoreResult.error(ImprintTheme.error("Imprint is null."));
        }

        // Strict server permission check
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

    private static RestoreResult restoreViaCreativePackets(Kit kit, MinecraftClient client) {
        try {
            int restoredCount = 0;
            boolean anyChanged = false;
            for (int i = 0; i < Kit.TOTAL_SLOTS; i++) {
                KitItem item = kit.getItem(i);
                ItemStack targetStack = item != null ? item.getItemStack() : ItemStack.EMPTY;
                ItemStack currentStack = getPlayerUnifiedStack(client.player, i);

                boolean targetEmpty = targetStack == null || targetStack.isEmpty();
                boolean currentEmpty = currentStack == null || currentStack.isEmpty();

                // If both are empty, nothing to do for this slot
                if (targetEmpty && currentEmpty) {
                    continue;
                }

                // If both non-empty and identical, skip packet
                if (!targetEmpty && !currentEmpty && ItemStack.areEqual(currentStack, targetStack)) {
                    restoredCount++;
                    continue;
                }

                int containerSlot = KitSlotType.toContainerSlot(i);
                ItemStack finalStack = targetEmpty ? ItemStack.EMPTY : targetStack.copy();

                // Send creative inventory packet only for changed slots
                client.interactionManager.clickCreativeStack(finalStack.copy(), containerSlot);

                // Update local inventory memory directly
                setPlayerUnifiedStack(client.player, i, finalStack.copy());

                // Update local screen handler for immediate visual feedback
                if (client.player.playerScreenHandler.slots.size() > containerSlot) {
                    client.player.playerScreenHandler.getSlot(containerSlot).setStack(finalStack.copy());
                }

                if (!targetEmpty) {
                    restoredCount++;
                }
                anyChanged = true;
            }

            if (anyChanged) {
                client.player.getInventory().markDirty();
                client.player.playerScreenHandler.sendContentUpdates();
                if (client.player.currentScreenHandler != null && client.player.currentScreenHandler != client.player.playerScreenHandler) {
                    client.player.currentScreenHandler.sendContentUpdates();
                }
            }

            LOGGER.info("Imprint '{}' restored via creative packets. Total items: {}", kit.getName(), restoredCount);

            MutableText feedback = ImprintTheme.badge()
                    .append(Text.literal("Loaded imprint ").styled(s -> s.withColor(TextColor.fromRgb(ImprintTheme.SILVER))))
                    .append(ImprintTheme.gradient(kit.getName(), ImprintTheme.PURE_WHITE, ImprintTheme.PLATINUM, true))
                    .append(Text.literal(" (" + restoredCount + " items restored).").styled(s -> s.withColor(TextColor.fromRgb(ImprintTheme.SLATE))));

            return RestoreResult.success(feedback);
        } catch (Exception e) {
            LOGGER.error("Failed to restore imprint '{}' via creative packets", kit.getName(), e);
            return RestoreResult.error(ImprintTheme.error("Failed to restore imprint: " + e.getMessage()));
        }
    }

    private static java.lang.reflect.Method SEND_COMMAND_METHOD = null;

    private static RestoreResult restoreViaOpCommands(Kit kit, MinecraftClient client) {
        try {
            int restoredCount = 0;
            RegistryWrapper.WrapperLookup registries = client.world != null
                    ? client.world.getRegistryManager()
                    : null;

            for (int i = 0; i < Kit.TOTAL_SLOTS; i++) {
                KitItem item = kit.getItem(i);
                ItemStack targetStack = item != null ? item.getItemStack() : ItemStack.EMPTY;
                ItemStack currentStack = getPlayerUnifiedStack(client.player, i);

                boolean targetEmpty = targetStack == null || targetStack.isEmpty();
                boolean currentEmpty = currentStack == null || currentStack.isEmpty();

                // Skip sending redundant replacement if slot is already empty
                if (targetEmpty && currentEmpty) {
                    continue;
                }

                // Skip if current slot already matches target item
                if (!targetEmpty && !currentEmpty && ItemStack.areEqual(currentStack, targetStack)) {
                    restoredCount++;
                    continue;
                }

                String commandSlot = KitSlotType.toCommandSlot(i);
                String itemStr;
                if (targetEmpty) {
                    itemStr = "air";
                } else {
                    String formatted = null;
                    if (registries != null) {
                        try {
                            formatted = new ItemStackArgument(targetStack.getRegistryEntry(), targetStack.getComponentChanges()).asString(registries);
                        } catch (Exception e) {
                            LOGGER.debug("Failed to format ItemStackArgument: {}", e.getMessage());
                        }
                    }
                    if (formatted == null || formatted.isBlank()) {
                        formatted = Registries.ITEM.getId(targetStack.getItem()).toString();
                    }
                    itemStr = formatted + " " + targetStack.getCount();
                    restoredCount++;
                }

                try {
                    client.player.networkHandler.sendChatCommand("item replace entity @s " + commandSlot + " with " + itemStr);
                } catch (Throwable t) {
                    try {
                        if (SEND_COMMAND_METHOD == null) {
                            SEND_COMMAND_METHOD = client.player.networkHandler.getClass().getMethod("sendCommand", String.class);
                        }
                        SEND_COMMAND_METHOD.invoke(client.player.networkHandler, "item replace entity @s " + commandSlot + " with " + itemStr);
                    } catch (Throwable ignored) {}
                }

                // Update local memory for immediate responsive feedback
                setPlayerUnifiedStack(client.player, i, targetEmpty ? ItemStack.EMPTY : targetStack.copy());
            }

            if (client.player != null) {
                client.player.getInventory().markDirty();
                client.player.playerScreenHandler.sendContentUpdates();
            }

            LOGGER.info("Imprint '{}' restored via OP commands. Total items: {}", kit.getName(), restoredCount);

            MutableText feedback = ImprintTheme.badge()
                    .append(Text.literal("Loaded imprint ").styled(s -> s.withColor(TextColor.fromRgb(ImprintTheme.SILVER))))
                    .append(ImprintTheme.gradient(kit.getName(), ImprintTheme.PURE_WHITE, ImprintTheme.PLATINUM, true))
                    .append(Text.literal(" via OP commands (" + restoredCount + " items).").styled(s -> s.withColor(TextColor.fromRgb(ImprintTheme.SLATE))));

            return RestoreResult.success(feedback);
        } catch (Exception e) {
            LOGGER.error("Failed to restore imprint '{}' via OP commands", kit.getName(), e);
            return RestoreResult.error(ImprintTheme.error("Failed to execute restoration commands: " + e.getMessage()));
        }
    }

    public static ItemStack getPlayerUnifiedStack(ClientPlayerEntity player, int unifiedIndex) {
        if (player == null || player.getInventory() == null) return ItemStack.EMPTY;
        if (unifiedIndex < 0 || unifiedIndex >= Kit.TOTAL_SLOTS) return ItemStack.EMPTY;
        return player.getInventory().getStack(unifiedIndex);
    }

    public static void setPlayerUnifiedStack(ClientPlayerEntity player, int unifiedIndex, ItemStack stack) {
        if (player == null || player.getInventory() == null) return;
        if (unifiedIndex < 0 || unifiedIndex >= Kit.TOTAL_SLOTS) return;
        player.getInventory().setStack(unifiedIndex, stack);
    }
}
