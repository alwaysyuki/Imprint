package dev.superior.imprint.model;

/**
 * Categorizes inventory slots and provides mappings between inventory indices,
 * container slot IDs (for creative actions), and command slot identifiers.
 */
public enum KitSlotType {
    HOTBAR("Hotbar", 0, 8),
    MAIN("Main Inventory", 9, 35),
    ARMOR_FEET("Boots", 36, 36),
    ARMOR_LEGS("Leggings", 37, 37),
    ARMOR_CHEST("Chestplate", 38, 38),
    ARMOR_HEAD("Helmet", 39, 39),
    OFFHAND("Offhand", 40, 40);

    private final String displayName;
    private final int startUnifiedIndex;
    private final int endUnifiedIndex;

    KitSlotType(String displayName, int startUnifiedIndex, int endUnifiedIndex) {
        this.displayName = displayName;
        this.startUnifiedIndex = startUnifiedIndex;
        this.endUnifiedIndex = endUnifiedIndex;
    }

    public String getDisplayName() {
        return displayName;
    }

    public int getStartUnifiedIndex() {
        return startUnifiedIndex;
    }

    public int getEndUnifiedIndex() {
        return endUnifiedIndex;
    }

    public static KitSlotType fromUnifiedIndex(int unifiedIndex) {
        if (unifiedIndex >= 0 && unifiedIndex <= 8) {
            return HOTBAR;
        } else if (unifiedIndex >= 9 && unifiedIndex <= 35) {
            return MAIN;
        } else if (unifiedIndex == 36) {
            return ARMOR_FEET;
        } else if (unifiedIndex == 37) {
            return ARMOR_LEGS;
        } else if (unifiedIndex == 38) {
            return ARMOR_CHEST;
        } else if (unifiedIndex == 39) {
            return ARMOR_HEAD;
        } else if (unifiedIndex == 40) {
            return OFFHAND;
        }
        throw new IllegalArgumentException("Invalid unified slot index: " + unifiedIndex);
    }

    public static int toContainerSlot(int unifiedIndex) {
        if (unifiedIndex >= 0 && unifiedIndex <= 8) {
            return 36 + unifiedIndex; // 36..44
        } else if (unifiedIndex >= 9 && unifiedIndex <= 35) {
            return unifiedIndex; // 9..35
        } else if (unifiedIndex == 36) {
            return 8; // Boots
        } else if (unifiedIndex == 37) {
            return 7; // Leggings
        } else if (unifiedIndex == 38) {
            return 6; // Chestplate
        } else if (unifiedIndex == 39) {
            return 5; // Helmet
        } else if (unifiedIndex == 40) {
            return 45; // Offhand
        }
        throw new IllegalArgumentException("Invalid unified slot index: " + unifiedIndex);
    }

    public static String toCommandSlot(int unifiedIndex) {
        if (unifiedIndex >= 0 && unifiedIndex <= 8) {
            return "hotbar." + unifiedIndex;
        } else if (unifiedIndex >= 9 && unifiedIndex <= 35) {
            return "inventory." + (unifiedIndex - 9);
        } else if (unifiedIndex == 36) {
            return "armor.feet";
        } else if (unifiedIndex == 37) {
            return "armor.legs";
        } else if (unifiedIndex == 38) {
            return "armor.chest";
        } else if (unifiedIndex == 39) {
            return "armor.head";
        } else if (unifiedIndex == 40) {
            return "weapon.offhand";
        }
        throw new IllegalArgumentException("Invalid unified slot index: " + unifiedIndex);
    }
}
