package dev.superior.imprint;

import dev.superior.imprint.command.ImprintCommand;
import dev.superior.imprint.storage.KitStorageManager;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Main client-side entrypoint for Imprint mod (Minecraft 26).
 * Author: Superior
 */
public class ImprintMod implements ClientModInitializer {
    public static final String MOD_ID = "imprint";
    public static final Logger LOGGER = LoggerFactory.getLogger("Imprint");

    @Override
    public void onInitializeClient() {
        LOGGER.info("[Imprint] Initializing Imprint mod by Superior...");

        // Ensure storage directory is initialized
        KitStorageManager.getInstance();

        // Register client commands (/imprint and legacy /kitcopy alias)
        ClientCommandRegistrationCallback.EVENT.register((dispatcher, registryAccess) -> {
            ImprintCommand.register(dispatcher, registryAccess);
        });

        LOGGER.info("[Imprint] Imprint mod successfully initialized with Black & White theme.");
    }
}
