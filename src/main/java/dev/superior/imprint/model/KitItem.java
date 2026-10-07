package dev.superior.imprint.model;

import com.google.gson.JsonObject;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.Registries;

/**
 * Represents a single saved slot within an Imprint.
 */
public class KitItem {
    private final int slotIndex;
    private final KitSlotType slotType;
    private final ItemStack itemStack;
    private JsonObject rawJson;

    public KitItem(int slotIndex, KitSlotType slotType, ItemStack itemStack) {
        this(slotIndex, slotType, itemStack, null);
    }

    public KitItem(int slotIndex, KitSlotType slotType, ItemStack itemStack, JsonObject rawJson) {
        this.slotIndex = slotIndex;
        this.slotType = slotType;
        this.itemStack = itemStack;
        this.rawJson = rawJson;
    }

    public int getSlotIndex() {
        return slotIndex;
    }

    public KitSlotType getSlotType() {
        return slotType;
    }

    public ItemStack getItemStack() {
        return itemStack;
    }

    public JsonObject getRawJson() {
        return rawJson;
    }

    public void setRawJson(JsonObject rawJson) {
        this.rawJson = rawJson;
    }

    public boolean isEmpty() {
        return itemStack == null || itemStack.isEmpty();
    }

    public String getItemId() {
        if (isEmpty()) {
            return "minecraft:air";
        }
        return Registries.ITEM.getId(itemStack.getItem()).toString();
    }

    public int getCount() {
        return isEmpty() ? 0 : itemStack.getCount();
    }

    public String getDisplayName() {
        if (isEmpty()) {
            return "Empty";
        }
        return itemStack.getName().getString();
    }

    public String getSummary() {
        if (isEmpty()) {
            return "Empty";
        }
        return getDisplayName() + " x" + getCount();
    }
}
