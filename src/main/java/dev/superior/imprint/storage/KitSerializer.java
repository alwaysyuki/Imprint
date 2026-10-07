package dev.superior.imprint.storage;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.JsonOps;
import dev.superior.imprint.model.Kit;
import dev.superior.imprint.model.KitItem;
import dev.superior.imprint.model.KitSlotType;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.RegistryOps;
import net.minecraft.registry.RegistryWrapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Handles JSON serialization and deserialization of Imprints using Minecraft 1.21's native codecs.
 * Preserves all components, NBT, durability, enchantments, and lore.
 */
public class KitSerializer {
    private static final Logger LOGGER = LoggerFactory.getLogger("Imprint/Serializer");
    public static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    public static JsonObject serializeKit(Kit kit, RegistryWrapper.WrapperLookup registries) {
        JsonObject root = new JsonObject();
        root.addProperty("name", kit.getName());
        root.addProperty("timestamp", kit.getTimestamp());
        root.addProperty("formattedDate", kit.getFormattedDate());
        root.addProperty("mcVersion", kit.getMcVersion());
        root.addProperty("totalItems", kit.getTotalNonEmptyItems());

        RegistryOps<JsonElement> ops = registries != null ? registries.getOps(JsonOps.INSTANCE) : null;

        JsonArray slotsArray = new JsonArray();
        for (int i = 0; i < Kit.TOTAL_SLOTS; i++) {
            KitItem kitItem = kit.getItem(i);
            if (kitItem == null) {
                kitItem = new KitItem(i, KitSlotType.fromUnifiedIndex(i), ItemStack.EMPTY);
            }

            JsonObject slotObj = new JsonObject();
            slotObj.addProperty("slot", i);
            slotObj.addProperty("type", kitItem.getSlotType().name());
            slotObj.addProperty("slotName", kitItem.getSlotType().getDisplayName());
            slotObj.addProperty("summary", kitItem.getSummary());

            ItemStack stack = kitItem.getItemStack();
            final int slotIdx = i;
            if (ops != null && stack != null && !stack.isEmpty()) {
                DataResult<JsonElement> encoded = ItemStack.OPTIONAL_CODEC.encodeStart(ops, stack);
                encoded.resultOrPartial(err -> LOGGER.warn("Partial/error encoding item at slot {}: {}", slotIdx, err))
                       .ifPresent(jsonEl -> slotObj.add("item", jsonEl));
            } else if (kitItem.getRawJson() != null) {
                slotObj.add("item", kitItem.getRawJson());
            }

            slotsArray.add(slotObj);
        }

        root.add("slots", slotsArray);
        return root;
    }

    public static Kit deserializeKit(JsonObject json, RegistryWrapper.WrapperLookup registries) {
        String name = json.has("name") ? json.get("name").getAsString() : "unnamed";
        long timestamp = json.has("timestamp") ? json.get("timestamp").getAsLong() : System.currentTimeMillis();
        String mcVersion = json.has("mcVersion") ? json.get("mcVersion").getAsString() : "1.21";

        Kit kit = new Kit(name, timestamp, mcVersion);
        RegistryOps<JsonElement> ops = registries != null ? registries.getOps(JsonOps.INSTANCE) : null;

        if (json.has("slots") && json.get("slots").isJsonArray()) {
            JsonArray slotsArray = json.getAsJsonArray("slots");
            for (JsonElement el : slotsArray) {
                if (!el.isJsonObject()) continue;
                JsonObject slotObj = el.getAsJsonObject();

                int slotIndex = slotObj.has("slot") ? slotObj.get("slot").getAsInt() : -1;
                if (slotIndex < 0 || slotIndex >= Kit.TOTAL_SLOTS) continue;

                KitSlotType slotType = KitSlotType.fromUnifiedIndex(slotIndex);
                ItemStack stack = ItemStack.EMPTY;
                JsonObject itemJson = null;

                if (slotObj.has("item") && !slotObj.get("item").isJsonNull()) {
                    JsonElement itemElement = slotObj.get("item");
                    if (itemElement.isJsonObject()) {
                        itemJson = itemElement.getAsJsonObject();
                    }
                    if (ops != null) {
                        DataResult<ItemStack> parsed = ItemStack.OPTIONAL_CODEC.parse(ops, itemElement);
                        if (parsed.result().isPresent()) {
                            stack = parsed.result().get();
                        } else {
                            LOGGER.warn("Failed to parse item at slot {}: {}", slotIndex, parsed.error().map(Object::toString).orElse("unknown"));
                        }
                    }
                }

                kit.setItem(slotIndex, new KitItem(slotIndex, slotType, stack, itemJson));
            }
        }

        for (int i = 0; i < Kit.TOTAL_SLOTS; i++) {
            if (kit.getItem(i) == null) {
                kit.setItem(i, new KitItem(i, KitSlotType.fromUnifiedIndex(i), ItemStack.EMPTY));
            }
        }

        return kit;
    }
}
