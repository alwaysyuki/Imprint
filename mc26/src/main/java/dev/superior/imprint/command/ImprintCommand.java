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
import net.fabricmc.fabric.api.client.command.v2.ClientCommands;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.minecraft.commands.CommandBuildContext;
import net.minecraft.core.HolderLookup;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.TextColor;

import java.util.List;

import static net.fabricmc.fabric.api.client.command.v2.ClientCommands.argument;
import static net.fabricmc.fabric.api.client.command.v2.ClientCommands.literal;

/**
 * Registers and handles /imprint (and legacy /kitcopy) commands for Minecraft 26.
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

    public static void register(CommandDispatcher<FabricClientCommandSource> dispatcher, CommandBuildContext registryAccess) {
        LiteralArgumentBuilder<FabricClientCommandSource> imprintTree = buildCommandTree("imprint", registryAccess);
        dispatcher.register(imprintTree);

        LiteralArgumentBuilder<FabricClientCommandSource> legacyTree = buildCommandTree("kitcopy", registryAccess);
        dispatcher.register(legacyTree);
    }

    private static LiteralArgumentBuilder<FabricClientCommandSource> buildCommandTree(String rootLiteral, CommandBuildContext registryAccess) {
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

        source.sendFeedback(Component.literal(" • ").withStyle(s -> s.withColor(TextColor.fromRgb(ImprintTheme.GRAPHITE)))
                .append(Component.literal("/imprint save <name>").withStyle(s -> s.withColor(TextColor.fromRgb(ImprintTheme.PURE_WHITE)).withBold(true)))
                .append(Component.literal(" — Save current inventory as a local imprint").withStyle(s -> s.withColor(TextColor.fromRgb(ImprintTheme.SILVER)))));

        source.sendFeedback(Component.literal(" • ").withStyle(s -> s.withColor(TextColor.fromRgb(ImprintTheme.GRAPHITE)))
                .append(Component.literal("/imprint load <name>").withStyle(s -> s.withColor(TextColor.fromRgb(ImprintTheme.PURE_WHITE)).withBold(true)))
                .append(Component.literal(" — Restore an imprint (requires OP / Creative)").withStyle(s -> s.withColor(TextColor.fromRgb(ImprintTheme.SILVER)))));

        source.sendFeedback(Component.literal(" • ").withStyle(s -> s.withColor(TextColor.fromRgb(ImprintTheme.GRAPHITE)))
                .append(Component.literal("/imprint list").withStyle(s -> s.withColor(TextColor.fromRgb(ImprintTheme.PURE_WHITE)).withBold(true)))
                .append(Component.literal(" — Display all locally saved imprints").withStyle(s -> s.withColor(TextColor.fromRgb(ImprintTheme.SILVER)))));

        source.sendFeedback(Component.literal(" • ").withStyle(s -> s.withColor(TextColor.fromRgb(ImprintTheme.GRAPHITE)))
                .append(Component.literal("/imprint delete <name>").withStyle(s -> s.withColor(TextColor.fromRgb(ImprintTheme.PURE_WHITE)).withBold(true)))
                .append(Component.literal(" — Delete a locally saved imprint").withStyle(s -> s.withColor(TextColor.fromRgb(ImprintTheme.SILVER)))));

        return 1;
    }

    private static int executeSave(CommandContext<FabricClientCommandSource> context, String kitName, HolderLookup.Provider fallbackRegistry) {
        FabricClientCommandSource source = context.getSource();

        if (source.getPlayer() == null) {
            source.sendError(ImprintTheme.error("Player is not available."));
            return 0;
        }

        if (kitName == null || kitName.isBlank()) {
            source.sendError(ImprintTheme.error("Please specify a valid imprint name."));
            return 0;
        }

        HolderLookup.Provider registries = source.getLevel() != null
                ? source.getLevel().registryAccess()
                : fallbackRegistry;

        try {
            Kit kit = KitCaptureService.captureCurrentInventory(kitName, source.getPlayer());
            boolean success = KitStorageManager.getInstance().saveKit(kit, registries);

            if (success) {
                MutableComponent feedback = ImprintTheme.badge()
                        .append(Component.literal("Imprint ").withStyle(s -> s.withColor(TextColor.fromRgb(ImprintTheme.SILVER))))
                        .append(ImprintTheme.gradient(kitName, ImprintTheme.PURE_WHITE, ImprintTheme.PLATINUM, true))
                        .append(Component.literal(" saved successfully! ").withStyle(s -> s.withColor(TextColor.fromRgb(ImprintTheme.PLATINUM))))
                        .append(Component.literal("(" + kit.getTotalNonEmptyItems() + " items saved)").withStyle(s -> s.withColor(TextColor.fromRgb(ImprintTheme.SLATE))));

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

    private static int executeLoad(CommandContext<FabricClientCommandSource> context, String kitName, HolderLookup.Provider fallbackRegistry) {
        FabricClientCommandSource source = context.getSource();

        if (!KitPermissionManager.hasLoadPermission(source.getClient())) {
            source.sendError(KitPermissionManager.getPermissionDeniedText());
            return 0;
        }

        if (!KitStorageManager.getInstance().kitExists(kitName)) {
            MutableComponent notFound = ImprintTheme.error("Imprint '")
                    .append(Component.literal(kitName).withStyle(s -> s.withColor(TextColor.fromRgb(ImprintTheme.PURE_WHITE))))
                    .append(Component.literal("' not found. Use ").withStyle(s -> s.withColor(TextColor.fromRgb(ImprintTheme.SILVER))))
                    .append(Component.literal("/imprint list").withStyle(s -> s.withColor(TextColor.fromRgb(ImprintTheme.PLATINUM)).withBold(true)))
                    .append(Component.literal(" to view saved imprints.").withStyle(s -> s.withColor(TextColor.fromRgb(ImprintTheme.SILVER))));

            source.sendError(notFound);
            return 0;
        }

        HolderLookup.Provider registries = source.getLevel() != null
                ? source.getLevel().registryAccess()
                : fallbackRegistry;

        Kit kit = KitStorageManager.getInstance().loadKit(kitName, registries);
        if (kit == null) {
            source.sendError(ImprintTheme.error("Failed to load imprint data for '" + kitName + "'."));
            return 0;
        }

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
            MutableComponent emptyMsg = ImprintTheme.badge()
                    .append(Component.literal("No saved imprints found. Use ").withStyle(s -> s.withColor(TextColor.fromRgb(ImprintTheme.SILVER))))
                    .append(Component.literal("/imprint save <name>").withStyle(s -> s.withColor(TextColor.fromRgb(ImprintTheme.PURE_WHITE)).withBold(true)))
                    .append(Component.literal(" to save your current inventory.").withStyle(s -> s.withColor(TextColor.fromRgb(ImprintTheme.SILVER))));

            source.sendFeedback(emptyMsg);
            return 1;
        }

        source.sendFeedback(ImprintTheme.header("IMPRINT KITS (" + kits.size() + ")"));

        for (KitMetadata kit : kits) {
            MutableComponent entry = Component.literal(" • ").withStyle(s -> s.withColor(TextColor.fromRgb(ImprintTheme.GRAPHITE)))
                    .append(ImprintTheme.gradient(kit.name(), ImprintTheme.PURE_WHITE, ImprintTheme.SILVER, true))
                    .append(Component.literal(" (" + kit.totalItems() + " items, " + kit.formattedDate() + ") ").withStyle(s -> s.withColor(TextColor.fromRgb(ImprintTheme.SLATE))));

            entry.append(ImprintTheme.buttonLoad(kit.name()))
                 .append(Component.literal(" "))
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
            MutableComponent feedback = ImprintTheme.badge()
                    .append(Component.literal("Imprint ").withStyle(s -> s.withColor(TextColor.fromRgb(ImprintTheme.SILVER))))
                    .append(ImprintTheme.gradient(kitName, ImprintTheme.PURE_WHITE, ImprintTheme.PLATINUM, true))
                    .append(Component.literal(" has been deleted.").withStyle(s -> s.withColor(TextColor.fromRgb(ImprintTheme.PLATINUM))));

            source.sendFeedback(feedback);
            return 1;
        } else {
            source.sendError(ImprintTheme.error("Failed to delete imprint '" + kitName + "'."));
            return 0;
        }
    }
}
