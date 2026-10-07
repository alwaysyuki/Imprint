package dev.superior.imprint;

import dev.superior.imprint.theme.ImprintTheme;
import net.minecraft.text.MutableText;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class ImprintThemeTest {

    @Test
    @DisplayName("Verify gradient generation and text composition")
    void testGradient() {
        MutableText empty = ImprintTheme.gradient("", ImprintTheme.PURE_WHITE, ImprintTheme.CHARCOAL, false);
        assertNotNull(empty);
        assertEquals("", empty.getString());

        MutableText single = ImprintTheme.gradient("A", ImprintTheme.PURE_WHITE, ImprintTheme.CHARCOAL, true);
        assertEquals("A", single.getString());

        MutableText multi = ImprintTheme.gradient("IMPRINT", ImprintTheme.PURE_WHITE, ImprintTheme.SLATE, true);
        assertEquals("IMPRINT", multi.getString());
        assertFalse(multi.getSiblings().isEmpty());
    }

    @Test
    @DisplayName("Verify theme components")
    void testThemeComponents() {
        MutableText badge = ImprintTheme.badge();
        assertTrue(badge.getString().contains("[IMPRINT]"));

        MutableText header = ImprintTheme.header("KITS");
        assertTrue(header.getString().contains("KITS"));
    }
}
