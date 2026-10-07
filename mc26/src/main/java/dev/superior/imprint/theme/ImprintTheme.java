package dev.superior.imprint.theme;

import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.TextColor;

/**
 * High-contrast monochromatic black-and-white theme system for Minecraft 26 (Mojang mappings).
 */
public class ImprintTheme {

    // Monochromatic Palette
    public static final int PURE_WHITE = 0xFFFFFF; // Brilliance / Title
    public static final int PLATINUM   = 0xEAEAEA; // Primary text / High contrast
    public static final int SILVER     = 0xB8B8B8; // Secondary text
    public static final int SLATE      = 0x7E7E7E; // Muted / tertiary text
    public static final int GRAPHITE   = 0x4A4A4A; // Borders / Brackets
    public static final int CHARCOAL   = 0x2A2A2A; // Deep dividers
    public static final int JET_BLACK  = 0x141414; // Contrast accent

    /**
     * Generates a 2-color linear gradient Component object.
     */
    public static MutableComponent gradient(String text, int startRgb, int endRgb, boolean bold) {
        MutableComponent root = Component.empty();
        int length = text.length();
        if (length == 0) return root;
        if (length == 1) {
            return Component.literal(text).withStyle(s -> s.withColor(TextColor.fromRgb(startRgb)).withBold(bold));
        }

        int r1 = (startRgb >> 16) & 0xFF;
        int g1 = (startRgb >> 8) & 0xFF;
        int b1 = startRgb & 0xFF;

        int r2 = (endRgb >> 16) & 0xFF;
        int g2 = (endRgb >> 8) & 0xFF;
        int b2 = endRgb & 0xFF;

        for (int i = 0; i < length; i++) {
            float ratio = (float) i / (float) (length - 1);
            int r = Math.round(r1 + (r2 - r1) * ratio);
            int g = Math.round(g1 + (g2 - g1) * ratio);
            int b = Math.round(b1 + (b2 - b1) * ratio);
            int rgb = (r << 16) | (g << 8) | b;

            String ch = String.valueOf(text.charAt(i));
            root.append(Component.literal(ch).withStyle(s -> s.withColor(TextColor.fromRgb(rgb)).withBold(bold)));
        }

        return root;
    }

    /**
     * Badge prefix: [IMPRINT] rendered with a bold black-to-white chrome gradient.
     */
    public static MutableComponent badge() {
        return Component.literal("[")
                .withStyle(s -> s.withColor(TextColor.fromRgb(GRAPHITE)).withBold(true))
                .append(gradient("IMPRINT", PURE_WHITE, SLATE, true))
                .append(Component.literal("] ").withStyle(s -> s.withColor(TextColor.fromRgb(GRAPHITE)).withBold(true)));
    }

    /**
     * Decorative header with sleek monochrome fading lines.
     */
    public static MutableComponent header(String title) {
        MutableComponent root = Component.empty();
        root.append(gradient("─────", CHARCOAL, SLATE, false));
        root.append(Component.literal(" [ ").withStyle(s -> s.withColor(TextColor.fromRgb(GRAPHITE)).withBold(true)));
        root.append(gradient(title, PURE_WHITE, SILVER, true));
        root.append(Component.literal(" ] ").withStyle(s -> s.withColor(TextColor.fromRgb(GRAPHITE)).withBold(true)));
        root.append(gradient("─────", SLATE, CHARCOAL, false));
        return root;
    }

    public static MutableComponent success(String message) {
        return badge().append(Component.literal(message).withStyle(s -> s.withColor(TextColor.fromRgb(PLATINUM))));
    }

    public static MutableComponent error(String message) {
        return badge().append(Component.literal(message).withStyle(s -> s.withColor(TextColor.fromRgb(PURE_WHITE)).withBold(true)));
    }

    public static MutableComponent info(String message) {
        return badge().append(Component.literal(message).withStyle(s -> s.withColor(TextColor.fromRgb(SILVER))));
    }

    public static MutableComponent highlighted(String label, String value) {
        return Component.literal(label).withStyle(s -> s.withColor(TextColor.fromRgb(SLATE)))
                .append(Component.literal(value).withStyle(s -> s.withColor(TextColor.fromRgb(PURE_WHITE)).withBold(true)));
    }

    public static MutableComponent buttonLoad(String kitName) {
        return Component.literal("[LOAD]")
                .withStyle(s -> s
                        .withColor(TextColor.fromRgb(PURE_WHITE))
                        .withBold(true)
                        .withClickEvent(new ClickEvent.RunCommand("/imprint load " + kitName))
                        .withHoverEvent(new HoverEvent.ShowText(
                                Component.literal("Click to load imprint: " + kitName).withStyle(st -> st.withColor(TextColor.fromRgb(PLATINUM))))));
    }

    public static MutableComponent buttonDelete(String kitName) {
        return Component.literal("[DELETE]")
                .withStyle(s -> s
                        .withColor(TextColor.fromRgb(SLATE))
                        .withBold(false)
                        .withClickEvent(new ClickEvent.SuggestCommand("/imprint delete " + kitName))
                        .withHoverEvent(new HoverEvent.ShowText(
                                Component.literal("Click to suggest delete command for: " + kitName).withStyle(st -> st.withColor(TextColor.fromRgb(SILVER))))));
    }
}
