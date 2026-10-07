package dev.superior.imprint;

import dev.superior.imprint.storage.KitMetadata;
import dev.superior.imprint.storage.KitStorageManager;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class KitStorageManagerTest {

    @Test
    @DisplayName("Verify filename sanitization prevents directory traversal and illegal characters")
    void testSanitizeFileName() {
        assertEquals("diamond_pvp", KitStorageManager.sanitizeFileName("diamond_pvp"));
        assertEquals("kit-1.test", KitStorageManager.sanitizeFileName("kit-1.test"));

        String traversed = KitStorageManager.sanitizeFileName("../../../evil/path");
        assertFalse(traversed.contains("/"));
        assertFalse(traversed.contains("\\"));
        assertEquals(".._.._.._evil_path", traversed);

        assertEquals("my_kit____100_", KitStorageManager.sanitizeFileName("my kit #@!100%"));

        assertEquals("unnamed", KitStorageManager.sanitizeFileName(""));
        assertEquals("unnamed", KitStorageManager.sanitizeFileName("   "));
        assertEquals("unnamed", KitStorageManager.sanitizeFileName(null));
    }

    @Test
    @DisplayName("Verify KitMetadata record properties")
    void testKitMetadata() {
        KitMetadata meta = new KitMetadata("pvp_warrior", 1728212000000L, "2026-10-06 11:00:00", 35, "1.21");
        assertEquals("pvp_warrior", meta.name());
        assertEquals(1728212000000L, meta.timestamp());
        assertEquals("2026-10-06 11:00:00", meta.formattedDate());
        assertEquals(35, meta.totalItems());
        assertEquals("1.21", meta.mcVersion());
    }
}
