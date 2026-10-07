package dev.superior.imprint;

import dev.superior.imprint.model.Kit;
import dev.superior.imprint.model.KitItem;
import dev.superior.imprint.model.KitSlotType;
import dev.superior.imprint.service.RestoreResult;
import net.minecraft.text.Text;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class KitModelTest {

    @Test
    @DisplayName("Verify Kit creation and slot retrieval")
    void testKitBasics() {
        Kit kit = new Kit("test_kit", 1728212000000L, "1.21");
        assertEquals("test_kit", kit.getName());
        assertEquals(1728212000000L, kit.getTimestamp());
        assertEquals("1.21", kit.getMcVersion());
        assertNotNull(kit.getFormattedDate());

        assertEquals(0, kit.getTotalNonEmptyItems());

        KitItem emptyItem = new KitItem(0, KitSlotType.HOTBAR, null);
        assertTrue(emptyItem.isEmpty());
        assertEquals("Empty", emptyItem.getDisplayName());
        assertEquals("minecraft:air", emptyItem.getItemId());
        assertEquals(0, emptyItem.getCount());

        kit.setItem(0, emptyItem);
        assertEquals(emptyItem, kit.getItem(0));
        assertEquals(0, kit.getTotalNonEmptyItems());
    }

    @Test
    @DisplayName("Verify RestoreResult factory methods and status checks")
    void testRestoreResult() {
        RestoreResult success = RestoreResult.success(Text.literal("Loaded successfully"));
        assertEquals(RestoreResult.Status.SUCCESS, success.status());
        assertTrue(success.isSuccess());
        assertEquals("Loaded successfully", success.feedback().getString());

        RestoreResult denied = RestoreResult.permissionDenied(Text.literal("You need permission to load a kit on this server."));
        assertEquals(RestoreResult.Status.PERMISSION_DENIED, denied.status());
        assertFalse(denied.isSuccess());
        assertEquals("You need permission to load a kit on this server.", denied.feedback().getString());

        RestoreResult err = RestoreResult.error(Text.literal("File corrupted"));
        assertEquals(RestoreResult.Status.ERROR, err.status());
        assertFalse(err.isSuccess());
        assertEquals("File corrupted", err.feedback().getString());
    }
}
