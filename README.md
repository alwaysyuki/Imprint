<p align="center">
  <img src="assets/logo.png" alt="Imprint Logo" width="160" height="160" />
</p>

# Imprint 🏛️

A sleek, **black-and-white monochrome themed** client-side Fabric mod for Minecraft 1.21 created by **Superior**.  
Designed to snapshot, manage, and restore player kits and inventory states locally with strict server-permission awareness.

---

## ⚡ Commands

| Command | Permission Required | Description |
| :--- | :--- | :--- |
| `/imprint save <name>` | **None** (Always allowed) | Captures complete local inventory state and writes it locally to `.minecraft/config/imprint/kits/<name>.json`. Never contacts the server or requires OP. |
| `/imprint load <name>` | **OP (Level >= 2) / Creative** | Strictly verifies permission before loading. If permitted, restores items using authorized methods (`clickCreativeStack` in Creative, or `/item replace entity @s` if OP in Survival). If not permitted, aborts without modifying inventory or sending unauthorized packets and warns: `You need permission to load a kit on this server.` |
| `/imprint list` | **None** | Displays all locally saved imprints with item counts, dates, and interactive chat buttons (`[LOAD]` and `[DELETE]`). |
| `/imprint delete <name>` | **None** | Removes the local imprint JSON file. |
| `/imprint help` | **None** | Displays command usage and help. |


---

## 📦 What is Saved

Every imprint snapshot preserves all 41 inventory slots:
1. **Main Inventory**: All 36 slots (Hotbar slots 0–8 + Upper Inventory slots 9–35)
2. **Armor**: Boots (slot 36), Leggings (slot 37), Chestplate (slot 38), Helmet (slot 39)
3. **Offhand**: Offhand shield/totem slot (slot 40)
4. **Complete Data Fidelity (Minecraft 1.21+)**:
   * Item IDs & counts
   * Enchantments
   * Durability & Damage components
   * Custom names & lore
   * Data components & NBT tags (potions, trims, banners, custom data)

---

## 📂 Storage Location

Imprints are stored locally under:
```text
.minecraft/config/imprint/kits/<name>.json
```

---

## 🏗️ Architecture

```text
src/main/java/dev/superior/imprint/
├── ImprintMod.java                   # ClientModInitializer entrypoint (Owner: Superior)
├── theme/
│   └── ImprintTheme.java             # Black & white gradient engine and UI styles
├── model/
│   ├── Kit.java                      # Kit domain model (41 slots)
│   ├── KitItem.java                  # Individual slot item representation
│   └── KitSlotType.java              # Slot classification & container/command mappings
├── storage/
│   ├── KitSerializer.java            # Codec-based JSON serialization
│   ├── KitStorageManager.java        # Atomic file I/O, migration, path sanitization
│   └── KitMetadata.java              # Lightweight record for fast listing
├── permission/
│   └── KitPermissionManager.java     # Server-permission & creative mode validation
├── service/
│   ├── KitCaptureService.java        # Local inventory snapshotter
│   ├── KitRestoreService.java        # Authorized restoration logic
│   └── RestoreResult.java            # Restoration response model
└── command/
    └── ImprintCommand.java           # Brigadier /imprint command tree
```

---

## 🚀 Multi-Version Builds

Imprint provides separate properly compiled Fabric builds for all **15 requested Minecraft versions**:

| Era | Target Versions | Build Module & Toolchain |
| :--- | :--- | :--- |
| **1.21.x Series** | `1.21`, `1.21.1`, `1.21.2`, `1.21.3`, `1.21.4`, `1.21.5`, `1.21.6`, `1.21.7`, `1.21.8`, `1.21.9`, `1.21.10`, `1.21.11` | Fabric Loom Remap, Yarn Mappings, Java 21 |
| **26.x Series** | `26.1`, `26.2`, `26.3` | Fabric Loom Unobfuscated, Mojang Names, Java 25/26 |

### Building All Versions

Run the automated build matrix script:
```bash
# Build all 15 targets into dist/:
python3 build_matrix.py

# Or build a specific target:
python3 build_matrix.py 1.21
python3 build_matrix.py 26.1
```

All 15 standalone compiled mod JARs are located in:
```text
dist/
├── imprint-1.0.0+1.21.jar
├── imprint-1.0.0+1.21.1.jar
├── imprint-1.0.0+1.21.2.jar
├── imprint-1.0.0+1.21.3.jar
├── imprint-1.0.0+1.21.4.jar
├── imprint-1.0.0+1.21.5.jar
├── imprint-1.0.0+1.21.6.jar
├── imprint-1.0.0+1.21.7.jar
├── imprint-1.0.0+1.21.8.jar
├── imprint-1.0.0+1.21.9.jar
├── imprint-1.0.0+1.21.10.jar
├── imprint-1.0.0+1.21.11.jar
├── imprint-1.0.0+26.1.jar
├── imprint-1.0.0+26.2.jar
└── imprint-1.0.0+26.3.jar
```

