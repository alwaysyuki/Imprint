package dev.superior.imprint.model;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Represents a complete saved imprint containing hotbar, main inventory, armor, and offhand.
 */
public class Kit {
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")
            .withZone(ZoneId.systemDefault());

    public static final int TOTAL_SLOTS = 41; // 36 main (0-8 hotbar, 9-35 upper) + 4 armor + 1 offhand

    private final String name;
    private final long timestamp;
    private final String mcVersion;
    private final Map<Integer, KitItem> slots = new HashMap<>();

    public Kit(String name, String mcVersion) {
        this(name, System.currentTimeMillis(), mcVersion);
    }

    public Kit(String name, long timestamp, String mcVersion) {
        this.name = name;
        this.timestamp = timestamp;
        this.mcVersion = mcVersion != null ? mcVersion : "1.21";
    }

    public String getName() {
        return name;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public String getFormattedDate() {
        return DATE_FORMATTER.format(Instant.ofEpochMilli(timestamp));
    }

    public String getMcVersion() {
        return mcVersion;
    }

    public void setItem(int slotIndex, KitItem item) {
        slots.put(slotIndex, item);
    }

    public KitItem getItem(int slotIndex) {
        return slots.get(slotIndex);
    }

    public Map<Integer, KitItem> getSlots() {
        return Collections.unmodifiableMap(slots);
    }

    public int getTotalNonEmptyItems() {
        int count = 0;
        for (KitItem item : slots.values()) {
            if (item != null && !item.isEmpty()) {
                count++;
            }
        }
        return count;
    }

    public List<KitItem> getHotbarItems() {
        List<KitItem> result = new ArrayList<>();
        for (int i = 0; i <= 8; i++) {
            KitItem item = slots.get(i);
            if (item != null) result.add(item);
        }
        return result;
    }

    public List<KitItem> getMainInventoryItems() {
        List<KitItem> result = new ArrayList<>();
        for (int i = 9; i <= 35; i++) {
            KitItem item = slots.get(i);
            if (item != null) result.add(item);
        }
        return result;
    }

    public List<KitItem> getArmorItems() {
        List<KitItem> result = new ArrayList<>();
        for (int i = 36; i <= 39; i++) {
            KitItem item = slots.get(i);
            if (item != null) result.add(item);
        }
        return result;
    }

    public KitItem getOffhandItem() {
        return slots.get(40);
    }
}
