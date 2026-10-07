package dev.superior.imprint.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import dev.superior.imprint.model.Kit;
import dev.superior.imprint.permission.KitPermissionManager;
import dev.superior.imprint.service.KitCaptureService;
import dev.superior.imprint.service.KitRestoreService;
import dev.superior.imprint.service.RestoreResult;
import dev.superior.imprint.storage.KitMetadata;
import dev.superior.imprint.storage.KitStorageManager;
import dev.superior.imprint.theme.ImprintTheme;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandManager;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.minecraft.command.CommandRegistryAccess;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.text.TextColor;

import java.util.List;

import static net.fabricmc.fabric.api.client.command.v2.ClientCommandManager.argument;
import static net.fabricmc.fabric.api.client.command.v2.ClientCommandManager.literal;

/**
 * Registers and handles /imprint (and legacy /kitcopy) commands with black & white gradient aesthetics.
 */
public class ImprintCommand {

    private static final SuggestionProvider<FabricClientCommandSource> KIT_SUGGESTIONS = (context, builder) -> {
        String remaining = builder.getRemainingLowerCase();
        for (String name : KitStorageManager.getInstance().getAllKitNames()) {
            if (name.toLowerCase().startsWith(remaining)) {
                builder.suggest(name);
            }
        }
        return builder.buildFuture();
    };

    public static void register(CommandDispatcher<FabricClientCommandSource> dispatcher, CommandRegistryAccess registryAccess) {
        // Register primary /imprint
        LiteralArgumentBuilder<FabricClientCommandSource> imprintTree = buildCommandTree("imprint", registryAccess);
        dispatcher.register(imprintTree);

        // Register /kitcopy alias for seamless compatibility
        LiteralArgumentBuilder<FabricClientCommandSource> legacyTree = buildCommandTree("kitcopy", registryAccess);
        dispatcher.register(legacyTree);
    }

    private static LiteralArgumentBuilder<FabricClientCommandSource> buildCommandTree(String rootLiteral, CommandRegistryAccess registryAccess) {
        return literal(rootLiteral)
                .executes(ImprintCommand::executeHelp)
                .then(literal("help")
                    .executes(ImprintCommand::executeHelp)
                )
                .then(literal("save")
                    .then(argument("name", StringArgumentType.word())
                        .executes(context -> executeSave(context, StringArgumentType.getString(context, "name"), registryAccess))
                    )
                )
                .then(literal("load")
                    .then(argument("name", StringArgumentType.word())
                        .suggests(KIT_SUGGESTIONS)
                        .executes(context -> executeLoad(context, StringArgumentType.getString(context, "name"), registryAccess))
                    )
                )
                .then(literal("list")
                    .executes(ImprintCommand::executeList)
                )
                .then(literal("delete")
                    .then(argument("name", StringArgumentType.word())
                        .suggests(KIT_SUGGESTIONS)
                        .executes(context -> executeDelete(context, StringArgumentType.getString(context, "name")))
                    )
                );
    }

    private static int executeHelp(CommandContext<FabricClientCommandSource> context) {
        FabricClientCommandSource source = context.getSource();

        source.sendFeedback(ImprintTheme.header("IMPRINT COMMANDS"));

        source.sendFeedback(Text.literal(" • ").styled(s -> s.withColor(TextColor.fromRgb(ImprintTheme.GRAPHITE)))
                .append(Text.literal("/imprint save <name>").styled(s -> s.withColor(TextColor.fromRgb(ImprintTheme.PURE_WHITE)).withBold(true)))
                .append(Text.literal(" — Save current inventory as a local imprint").styled(s -> s.withColor(TextColor.fromRgb(ImprintTheme.SILVER)))));

        source.sendFeedback(Text.literal(" • ").styled(s -> s.withColor(TextColor.fromRgb(ImprintTheme.GRAPHITE)))
                .append(Text.literal("/imprint load <name>").styled(s -> s.withColor(TextColor.fromRgb(ImprintTheme.PURE_WHITE)).withBold(true)))
                .append(Text.literal(" — Restore an imprint (requires OP / Creative)").styled(s -> s.withColor(TextColor.fromRgb(ImprintTheme.SILVER)))));

        source.sendFeedback(Text.literal(" • ").styled(s -> s.withColor(TextColor.fromRgb(ImprintTheme.GRAPHITE)))
                .append(Text.literal("/imprint list").styled(s -> s.withColor(TextColor.fromRgb(ImprintTheme.PURE_WHITE)).withBold(true)))
                .append(Text.literal(" — Display all locally saved imprints").styled(s -> s.withColor(TextColor.fromRgb(ImprintTheme.SILVER)))));

        source.sendFeedback(Text.literal(" • ").styled(s -> s.withColor(TextColor.fromRgb(ImprintTheme.GRAPHITE)))
                .append(Text.literal("/imprint delete <name>").styled(s -> s.withColor(TextColor.fromRgb(ImprintTheme.PURE_WHITE)).withBold(true)))
                .append(Text.literal(" — Delete a locally saved imprint").styled(s -> s.withColor(TextColor.fromRgb(ImprintTheme.SILVER)))));

        return 1;
    }

    private static int executeSave(CommandContext<FabricClientCommandSource> context, String kitName, RegistryWrapper.WrapperLookup fallbackRegistry) {
        FabricClientCommandSource source = context.getSource();

        if (source.getPlayer() == null) {
            source.sendError(ImprintTheme.error("Player is not available."));
            return 0;
        }

        if (kitName == null || kitName.isBlank()) {
            source.sendError(ImprintTheme.error("Please specify a valid imprint name."));
            return 0;
        }

        RegistryWrapper.WrapperLookup registries = source.getWorld() != null
                ? source.getWorld().getRegistryManager()
                : fallbackRegistry;

        try {
            Kit kit = KitCaptureService.captureCurrentInventory(kitName, source.getPlayer());
            boolean success = KitStorageManager.getInstance().saveKit(kit, registries);

            if (success) {
                MutableText feedback = ImprintTheme.badge()
                        .append(Text.literal("Imprint ").styled(s -> s.withColor(TextColor.fromRgb(ImprintTheme.SILVER))))
                        .append(ImprintTheme.gradient(kitName, ImprintTheme.PURE_WHITE, ImprintTheme.PLATINUM, true))
                        .append(Text.literal(" saved successfully! ").styled(s -> s.withColor(TextColor.fromRgb(ImprintTheme.PLATINUM))))
                        .append(Text.literal("(" + kit.getTotalNonEmptyItems() + " items saved)").styled(s -> s.withColor(TextColor.fromRgb(ImprintTheme.SLATE))));

                source.sendFeedback(feedback);
                return 1;
            } else {
                source.sendError(ImprintTheme.error("Failed to save imprint '" + kitName + "'. Check logs for details."));
                return 0;
            }
        } catch (Exception e) {
            source.sendError(ImprintTheme.error("Error saving imprint: " + e.getMessage()));
            return 0;
        }
    }

    private static int executeLoad(CommandContext<FabricClientCommandSource> context, String kitName, RegistryWrapper.WrapperLookup fallbackRegistry) {
        FabricClientCommandSource source = context.getSource();

        // 1. Strict Server Permission Check
        if (!KitPermissionManager.hasLoadPermission(source.getClient())) {
            source.sendError(KitPermissionManager.getPermissionDeniedText());
            return 0;
        }

        // 2. Check if imprint exists locally
        if (!KitStorageManager.getInstance().kitExists(kitName)) {
            MutableText notFound = ImprintTheme.error("Imprint '")
                    .append(Text.literal(kitName).styled(s -> s.withColor(TextColor.fromRgb(ImprintTheme.PURE_WHITE))))
                    .append(Text.literal("' not found. Use ").styled(s -> s.withColor(TextColor.fromRgb(ImprintTheme.SILVER))))
                    .append(Text.literal("/imprint list").styled(s -> s.withColor(TextColor.fromRgb(ImprintTheme.PLATINUM)).withBold(true)))
                    .append(Text.literal(" to view saved imprints.").styled(s -> s.withColor(TextColor.fromRgb(ImprintTheme.SILVER))));

            source.sendError(notFound);
            return 0;
        }

        RegistryWrapper.WrapperLookup registries = source.getWorld() != null
                ? source.getWorld().getRegistryManager()
                : fallbackRegistry;

        Kit kit = KitStorageManager.getInstance().loadKit(kitName, registries);
        if (kit == null) {
            source.sendError(ImprintTheme.error("Failed to load imprint data for '" + kitName + "'."));
            return 0;
        }

        // 3. Authorized Restoration
        RestoreResult result = KitRestoreService.restoreKit(kit, source.getClient());
        if (result.isSuccess()) {
            source.sendFeedback(result.feedback());
            return 1;
        } else {
            source.sendError(result.feedback());
            return 0;
        }
    }

    private static int executeList(CommandContext<FabricClientCommandSource> context) {
        FabricClientCommandSource source = context.getSource();
        List<KitMetadata> kits = KitStorageManager.getInstance().getAllKitsMetadata();

        if (kits.isEmpty()) {
            MutableText emptyMsg = ImprintTheme.badge()
                    .append(Text.literal("No saved imprints found. Use ").styled(s -> s.withColor(TextColor.fromRgb(ImprintTheme.SILVER))))
                    .append(Text.literal("/imprint save <name>").styled(s -> s.withColor(TextColor.fromRgb(ImprintTheme.PURE_WHITE)).withBold(true)))
                    .append(Text.literal(" to save your current inventory.").styled(s -> s.withColor(TextColor.fromRgb(ImprintTheme.SILVER))));

            source.sendFeedback(emptyMsg);
            return 1;
        }

        source.sendFeedback(ImprintTheme.header("IMPRINT KITS (" + kits.size() + ")"));

        for (KitMetadata kit : kits) {
            MutableText entry = Text.literal(" • ").styled(s -> s.withColor(TextColor.fromRgb(ImprintTheme.GRAPHITE)))
                    .append(ImprintTheme.gradient(kit.name(), ImprintTheme.PURE_WHITE, ImprintTheme.SILVER, true))
                    .append(Text.literal(" (" + kit.totalItems() + " items, " + kit.formattedDate() + ") ").styled(s -> s.withColor(TextColor.fromRgb(ImprintTheme.SLATE))));

            entry.append(ImprintTheme.buttonLoad(kit.name()))
                 .append(Text.literal(" "))
                 .append(ImprintTheme.buttonDelete(kit.name()));

            source.sendFeedback(entry);
        }

        return 1;
    }

    private static int executeDelete(CommandContext<FabricClientCommandSource> context, String kitName) {
        FabricClientCommandSource source = context.getSource();

        if (!KitStorageManager.getInstance().kitExists(kitName)) {
            source.sendError(ImprintTheme.error("Imprint '" + kitName + "' does not exist."));
            return 0;
        }

        boolean deleted = KitStorageManager.getInstance().deleteKit(kitName);
        if (deleted) {
            MutableText feedback = ImprintTheme.badge()
                    .append(Text.literal("Imprint ").styled(s -> s.withColor(TextColor.fromRgb(ImprintTheme.SILVER))))
                    .append(ImprintTheme.gradient(kitName, ImprintTheme.PURE_WHITE, ImprintTheme.PLATINUM, true))
                    .append(Text.literal(" has been deleted.").styled(s -> s.withColor(TextColor.fromRgb(ImprintTheme.PLATINUM))));

            source.sendFeedback(feedback);
            return 1;
        } else {
            source.sendError(ImprintTheme.error("Failed to delete imprint '" + kitName + "'."));
            return 0;
        }
    }
}
