package dev.superior.imprint;

import dev.superior.imprint.model.KitSlotType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class KitSlotTypeTest {

    @Test
    @DisplayName("Verify slot type mapping across all 41 unified slots")
    void testSlotTypeMapping() {
        for (int i = 0; i <= 8; i++) {
            assertEquals(KitSlotType.HOTBAR, KitSlotType.fromUnifiedIndex(i));
        }

        for (int i = 9; i <= 35; i++) {
            assertEquals(KitSlotType.MAIN, KitSlotType.fromUnifiedIndex(i));
        }

        assertEquals(KitSlotType.ARMOR_FEET, KitSlotType.fromUnifiedIndex(36));
        assertEquals(KitSlotType.ARMOR_LEGS, KitSlotType.fromUnifiedIndex(37));
        assertEquals(KitSlotType.ARMOR_CHEST, KitSlotType.fromUnifiedIndex(38));
        assertEquals(KitSlotType.ARMOR_HEAD, KitSlotType.fromUnifiedIndex(39));
        assertEquals(KitSlotType.OFFHAND, KitSlotType.fromUnifiedIndex(40));

        assertThrows(IllegalArgumentException.class, () -> KitSlotType.fromUnifiedIndex(-1));
        assertThrows(IllegalArgumentException.class, () -> KitSlotType.fromUnifiedIndex(41));
    }

    @Test
    @DisplayName("Verify container slot mappings for creative packets")
    void testContainerSlotMapping() {
        assertEquals(36, KitSlotType.toContainerSlot(0));
        assertEquals(44, KitSlotType.toContainerSlot(8));

        assertEquals(9, KitSlotType.toContainerSlot(9));
        assertEquals(35, KitSlotType.toContainerSlot(35));

        assertEquals(8, KitSlotType.toContainerSlot(36)); // Feet
        assertEquals(7, KitSlotType.toContainerSlot(37)); // Legs
        assertEquals(6, KitSlotType.toContainerSlot(38)); // Chest
        assertEquals(5, KitSlotType.toContainerSlot(39)); // Head

        assertEquals(45, KitSlotType.toContainerSlot(40)); // Offhand
    }

    @Test
    @DisplayName("Verify Minecraft command slot names for /item replace")
    void testCommandSlotNames() {
        assertEquals("hotbar.0", KitSlotType.toCommandSlot(0));
        assertEquals("hotbar.8", KitSlotType.toCommandSlot(8));

        assertEquals("inventory.0", KitSlotType.toCommandSlot(9));
        assertEquals("inventory.26", KitSlotType.toCommandSlot(35));

        assertEquals("armor.feet", KitSlotType.toCommandSlot(36));
        assertEquals("armor.legs", KitSlotType.toCommandSlot(37));
        assertEquals("armor.chest", KitSlotType.toCommandSlot(38));
        assertEquals("armor.head", KitSlotType.toCommandSlot(39));

        assertEquals("weapon.offhand", KitSlotType.toCommandSlot(40));
    }
}
