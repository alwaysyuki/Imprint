package dev.superior.imprint.storage;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import dev.superior.imprint.model.Kit;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.core.HolderLookup;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Stream;

/**
 * Manages local file persistence of Imprints in config/imprint/kits/ (Minecraft 26).
 */
public class KitStorageManager {
    private static final Logger LOGGER = LoggerFactory.getLogger("Imprint/Storage");
    private static final KitStorageManager INSTANCE = new KitStorageManager();
    private final Path kitsDirectory;

    private static Path resolveConfigDir() {
        try {
            if (FabricLoader.getInstance() != null && FabricLoader.getInstance().getConfigDir() != null) {
                return FabricLoader.getInstance().getConfigDir();
            }
        } catch (Throwable ignored) {}
        return Path.of("config");
    }

    public KitStorageManager() {
        this(resolveConfigDir());
    }

    public KitStorageManager(Path baseConfigDir) {
        this.kitsDirectory = baseConfigDir.resolve("imprint").resolve("kits");
        ensureDirectoryExists();
        migrateLegacyKits(baseConfigDir);
    }

    public static KitStorageManager getInstance() {
        return INSTANCE;
    }

    public Path getKitsDirectory() {
        return kitsDirectory;
    }

    private void ensureDirectoryExists() {
        try {
            if (!Files.exists(kitsDirectory)) {
                Files.createDirectories(kitsDirectory);
            }
        } catch (IOException e) {
            LOGGER.error("Failed to create imprint kits directory: {}", kitsDirectory, e);
        }
    }

    private void migrateLegacyKits(Path baseConfigDir) {
        try {
            Path legacyDir = baseConfigDir.resolve("kitcopier").resolve("kits");
            if (Files.isDirectory(legacyDir)) {
                try (Stream<Path> stream = Files.list(legacyDir)) {
                    stream.filter(p -> Files.isRegularFile(p) && p.getFileName().toString().endsWith(".json"))
                          .forEach(legacyFile -> {
                              Path target = kitsDirectory.resolve(legacyFile.getFileName());
                              if (!Files.exists(target)) {
                                  try {
                                      Files.copy(legacyFile, target, StandardCopyOption.COPY_ATTRIBUTES);
                                      LOGGER.info("Migrated legacy kit '{}' to imprint directory.", legacyFile.getFileName());
                                  } catch (IOException ignored) {}
                              }
                          });
                }
            }
        } catch (Throwable ignored) {}
    }

    public static String sanitizeFileName(String name) {
        if (name == null || name.isBlank()) {
            return "unnamed";
        }
        String sanitized = name.trim().replaceAll("[^a-zA-Z0-9._-]", "_");
        return sanitized.isEmpty() ? "imprint" : sanitized;
    }

    public Path getKitFilePath(String name) {
        String fileName = sanitizeFileName(name) + ".json";
        return kitsDirectory.resolve(fileName);
    }

    public boolean kitExists(String name) {
        return Files.isRegularFile(getKitFilePath(name));
    }

    public boolean saveKit(Kit kit, HolderLookup.Provider registries) {
        ensureDirectoryExists();
        Path targetPath = getKitFilePath(kit.getName());
        Path tempPath = kitsDirectory.resolve(sanitizeFileName(kit.getName()) + ".tmp");

        try {
            JsonObject json = KitSerializer.serializeKit(kit, registries);
            try (BufferedWriter writer = Files.newBufferedWriter(tempPath, StandardCharsets.UTF_8)) {
                KitSerializer.GSON.toJson(json, writer);
            }

            try {
                Files.move(tempPath, targetPath, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
            } catch (IOException atomicEx) {
                Files.move(tempPath, targetPath, StandardCopyOption.REPLACE_EXISTING);
            }
            metadataCache.put(kit.getName().toLowerCase(), new KitMetadata(
                    kit.getName(), kit.getTimestamp(), kit.getFormattedDate(), kit.getTotalNonEmptyItems(), kit.getMcVersion()
            ));
            LOGGER.info("Imprint '{}' saved successfully to {}", kit.getName(), targetPath);
            return true;
        } catch (Exception e) {
            LOGGER.error("Failed to save imprint '{}'", kit.getName(), e);
            try {
                Files.deleteIfExists(tempPath);
            } catch (IOException ignored) {}
            return false;
        }
    }

    public Kit loadKit(String name, HolderLookup.Provider registries) {
        Path path = getKitFilePath(name);
        if (!Files.isRegularFile(path)) {
            LOGGER.warn("Imprint file not found: {}", path);
            return null;
        }

        try (BufferedReader reader = Files.newBufferedReader(path, StandardCharsets.UTF_8)) {
            JsonElement element = JsonParser.parseReader(reader);
            if (!element.isJsonObject()) {
                LOGGER.error("Imprint file is not a JSON object: {}", path);
                return null;
            }
            return KitSerializer.deserializeKit(element.getAsJsonObject(), registries);
        } catch (Exception e) {
            LOGGER.error("Failed to load imprint '{}' from {}", name, path, e);
            return null;
        }
    }

    public boolean deleteKit(String name) {
        Path path = getKitFilePath(name);
        if (!Files.exists(path)) {
            return false;
        }

        try {
            boolean deleted = Files.deleteIfExists(path);
            if (deleted) {
                metadataCache.remove(name.toLowerCase());
                LOGGER.info("Imprint '{}' deleted successfully.", name);
            }
            return deleted;
        } catch (IOException e) {
            LOGGER.error("Failed to delete imprint '{}'", name, e);
            return false;
        }
    }

    private final java.util.Map<String, KitMetadata> metadataCache = new java.util.concurrent.ConcurrentHashMap<>();
    private volatile boolean cacheInitialized = false;

    private synchronized void ensureCacheLoaded() {
        if (cacheInitialized) return;
        reloadCacheFromDisk();
        cacheInitialized = true;
    }

    public synchronized void reloadCacheFromDisk() {
        metadataCache.clear();
        if (!Files.isDirectory(kitsDirectory)) return;
        try (Stream<Path> stream = Files.list(kitsDirectory)) {
            stream.filter(p -> Files.isRegularFile(p) && p.getFileName().toString().endsWith(".json"))
                  .forEach(p -> {
                      String fileName = p.getFileName().toString();
                      String fallbackName = fileName.substring(0, fileName.length() - 5);
                      try (BufferedReader reader = Files.newBufferedReader(p, StandardCharsets.UTF_8)) {
                          JsonElement el = JsonParser.parseReader(reader);
                          if (el.isJsonObject()) {
                              JsonObject obj = el.getAsJsonObject();
                              String name = obj.has("name") ? obj.get("name").getAsString() : fallbackName;
                              long timestamp = obj.has("timestamp") ? obj.get("timestamp").getAsLong() : 0L;
                              String formattedDate = obj.has("formattedDate") ? obj.get("formattedDate").getAsString() : "";
                              int totalItems = obj.has("totalItems") ? obj.get("totalItems").getAsInt() : 0;
                              String mcVersion = obj.has("mcVersion") ? obj.get("mcVersion").getAsString() : "26.1";
                              metadataCache.put(name.toLowerCase(), new KitMetadata(name, timestamp, formattedDate, totalItems, mcVersion));
                          }
                      } catch (Exception e) {
                          LOGGER.warn("Failed to parse metadata from imprint file: {}", p, e);
                      }
                  });
        } catch (IOException e) {
            LOGGER.error("Error reading imprints directory: {}", kitsDirectory, e);
        }
    }

    public List<String> getAllKitNames() {
        ensureCacheLoaded();
        List<String> names = new ArrayList<>();
        for (KitMetadata meta : metadataCache.values()) {
            names.add(meta.name());
        }
        names.sort(String.CASE_INSENSITIVE_ORDER);
        return names;
    }

    public List<KitMetadata> getAllKitsMetadata() {
        ensureCacheLoaded();
        List<KitMetadata> list = new ArrayList<>(metadataCache.values());
        list.sort(Comparator.comparingLong(KitMetadata::timestamp).reversed());
        return list;
    }
}
