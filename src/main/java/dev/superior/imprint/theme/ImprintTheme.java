package dev.superior.imprint.theme;

import net.minecraft.text.ClickEvent;
import net.minecraft.text.HoverEvent;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.text.TextColor;

/**
 * High-contrast monochromatic black-and-white theme system with smooth gradient generators.
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
     * Generates a 2-color linear gradient Text object.
     */
    public static MutableText gradient(String text, int startRgb, int endRgb, boolean bold) {
        MutableText root = Text.empty();
        int length = text.length();
        if (length == 0) return root;
        if (length == 1) {
            return Text.literal(text).styled(s -> s.withColor(TextColor.fromRgb(startRgb)).withBold(bold));
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
            root.append(Text.literal(ch).styled(s -> s.withColor(TextColor.fromRgb(rgb)).withBold(bold)));
        }

        return root;
    }

    /**
     * Badge prefix: [IMPRINT] rendered with a bold black-to-white chrome gradient.
     */
    public static MutableText badge() {
        return Text.literal("[")
                .styled(s -> s.withColor(TextColor.fromRgb(GRAPHITE)).withBold(true))
                .append(gradient("IMPRINT", PURE_WHITE, SLATE, true))
                .append(Text.literal("] ").styled(s -> s.withColor(TextColor.fromRgb(GRAPHITE)).withBold(true)));
    }

    /**
     * Decorative header with sleek monochrome fading lines.
     */
    public static MutableText header(String title) {
        MutableText root = Text.empty();
        root.append(gradient("─────", CHARCOAL, SLATE, false));
        root.append(Text.literal(" [ ").styled(s -> s.withColor(TextColor.fromRgb(GRAPHITE)).withBold(true)));
        root.append(gradient(title, PURE_WHITE, SILVER, true));
        root.append(Text.literal(" ] ").styled(s -> s.withColor(TextColor.fromRgb(GRAPHITE)).withBold(true)));
        root.append(gradient("─────", SLATE, CHARCOAL, false));
        return root;
    }

    public static MutableText success(String message) {
        return badge().append(Text.literal(message).styled(s -> s.withColor(TextColor.fromRgb(PLATINUM))));
    }

    public static MutableText error(String message) {
        return badge().append(Text.literal(message).styled(s -> s.withColor(TextColor.fromRgb(PURE_WHITE)).withBold(true)));
    }

    public static MutableText info(String message) {
        return badge().append(Text.literal(message).styled(s -> s.withColor(TextColor.fromRgb(SILVER))));
    }

    public static MutableText highlighted(String label, String value) {
        return Text.literal(label).styled(s -> s.withColor(TextColor.fromRgb(SLATE)))
                .append(Text.literal(value).styled(s -> s.withColor(TextColor.fromRgb(PURE_WHITE)).withBold(true)));
    }

    public static MutableText buttonLoad(String kitName) {
        MutableText btn = Text.literal("[LOAD]")
                .styled(s -> s
                        .withColor(TextColor.fromRgb(PURE_WHITE))
                        .withBold(true));
        ClickEvent click = runCommandClickEvent("/imprint load " + kitName);
        if (click != null) {
            btn = btn.styled(s -> s.withClickEvent(click));
        }
        HoverEvent hover = showTextHoverEvent(Text.literal("Click to load imprint: " + kitName).styled(st -> st.withColor(TextColor.fromRgb(PLATINUM))));
        if (hover != null) {
            btn = btn.styled(s -> s.withHoverEvent(hover));
        }
        return btn;
    }

    public static MutableText buttonDelete(String kitName) {
        MutableText btn = Text.literal("[DELETE]")
                .styled(s -> s
                        .withColor(TextColor.fromRgb(SLATE))
                        .withBold(false));
        ClickEvent click = suggestCommandClickEvent("/imprint delete " + kitName);
        if (click != null) {
            btn = btn.styled(s -> s.withClickEvent(click));
        }
        HoverEvent hover = showTextHoverEvent(Text.literal("Click to suggest delete command for: " + kitName).styled(st -> st.withColor(TextColor.fromRgb(SILVER))));
        if (hover != null) {
            btn = btn.styled(s -> s.withHoverEvent(hover));
        }
        return btn;
    }

    private static final java.lang.reflect.Constructor<?> RUN_CMD_CTOR;
    private static final Object RUN_CMD_ARG;
    private static final java.lang.reflect.Constructor<?> SUGGEST_CMD_CTOR;
    private static final Object SUGGEST_CMD_ARG;
    private static final java.lang.reflect.Constructor<?> SHOW_TEXT_CTOR;
    private static final Object SHOW_TEXT_ARG;

    static {
        java.lang.reflect.Constructor<?> rCtor = null;
        Object rArg = null;
        try {
            // 1.21.5+ (ClickEvent$RunCommand(String))
            Class<?> clazz = Class.forName("net.minecraft.text.ClickEvent$RunCommand");
            rCtor = clazz.getConstructor(String.class);
        } catch (Throwable t) {
            try {
                // 1.21.0 - 1.21.4 (ClickEvent(Action, String))
                Class<?> actionClass = Class.forName("net.minecraft.text.ClickEvent$Action");
                rArg = Enum.valueOf((Class<Enum>) actionClass, "RUN_COMMAND");
                rCtor = ClickEvent.class.getConstructor(actionClass, String.class);
            } catch (Throwable ignored) {}
        }
        RUN_CMD_CTOR = rCtor;
        RUN_CMD_ARG = rArg;

        java.lang.reflect.Constructor<?> sCtor = null;
        Object sArg = null;
        try {
            // 1.21.5+ (ClickEvent$SuggestCommand(String))
            Class<?> clazz = Class.forName("net.minecraft.text.ClickEvent$SuggestCommand");
            sCtor = clazz.getConstructor(String.class);
        } catch (Throwable t) {
            try {
                // 1.21.0 - 1.21.4
                Class<?> actionClass = Class.forName("net.minecraft.text.ClickEvent$Action");
                sArg = Enum.valueOf((Class<Enum>) actionClass, "SUGGEST_COMMAND");
                sCtor = ClickEvent.class.getConstructor(actionClass, String.class);
            } catch (Throwable ignored) {}
        }
        SUGGEST_CMD_CTOR = sCtor;
        SUGGEST_CMD_ARG = sArg;

        java.lang.reflect.Constructor<?> hCtor = null;
        Object hArg = null;
        try {
            // 1.21.5+ (HoverEvent$ShowText(Text))
            Class<?> clazz = Class.forName("net.minecraft.text.HoverEvent$ShowText");
            hCtor = clazz.getConstructor(Text.class);
        } catch (Throwable t) {
            try {
                // 1.21.0 - 1.21.4
                Class<?> actionClass = Class.forName("net.minecraft.text.HoverEvent$Action");
                hArg = actionClass.getField("SHOW_TEXT").get(null);
                hCtor = HoverEvent.class.getConstructor(actionClass, Text.class);
            } catch (Throwable ignored) {}
        }
        SHOW_TEXT_CTOR = hCtor;
        SHOW_TEXT_ARG = hArg;
    }

    public static ClickEvent runCommandClickEvent(String command) {
        if (RUN_CMD_CTOR == null) return null;
        try {
            if (RUN_CMD_ARG != null) {
                return (ClickEvent) RUN_CMD_CTOR.newInstance(RUN_CMD_ARG, command);
            } else {
                return (ClickEvent) RUN_CMD_CTOR.newInstance(command);
            }
        } catch (Throwable t) {
            return null;
        }
    }

    public static ClickEvent suggestCommandClickEvent(String command) {
        if (SUGGEST_CMD_CTOR == null) return null;
        try {
            if (SUGGEST_CMD_ARG != null) {
                return (ClickEvent) SUGGEST_CMD_CTOR.newInstance(SUGGEST_CMD_ARG, command);
            } else {
                return (ClickEvent) SUGGEST_CMD_CTOR.newInstance(command);
            }
        } catch (Throwable t) {
            return null;
        }
    }

    public static HoverEvent showTextHoverEvent(Text text) {
        if (SHOW_TEXT_CTOR == null) return null;
        try {
            if (SHOW_TEXT_ARG != null) {
                return (HoverEvent) SHOW_TEXT_CTOR.newInstance(SHOW_TEXT_ARG, text);
            } else {
                return (HoverEvent) SHOW_TEXT_CTOR.newInstance(text);
            }
        } catch (Throwable t) {
            return null;
        }
    }
}
