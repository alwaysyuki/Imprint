package dev.superior.imprint.storage;

/**
 * Lightweight metadata summary of a stored imprint.
 */
public record KitMetadata(
        String name,
        long timestamp,
        String formattedDate,
        int totalItems,
        String mcVersion
) {
}
