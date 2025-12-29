package com.example.slimeseller;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.minecraft.item.Item;
import net.minecraft.item.Items;
import net.minecraft.registry.Registries;
import net.minecraft.util.Identifier;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;

/**
 * Configuration manager for the Slime Seller mod.
 * Handles saving and loading settings to/from a JSON file.
 */
public class ModConfig {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final File CONFIG_DIR = new File("config");
    private static final File CONFIG_FILE = new File(CONFIG_DIR, "slimeseller.json");

    private static ConfigData currentConfig = new ConfigData();

    /**
     * Load configuration from file. If file doesn't exist, create default config.
     */
    public static void load() {
        try {
            if (!CONFIG_FILE.exists()) {
                System.out.println("[SlimeSeller Config] No config file found, creating default");
                currentConfig = new ConfigData();
                currentConfig.selectedItemId = "minecraft:slime_ball";
                currentConfig.autoDropMinecarts = false;
                save();
                return;
            }

            try (FileReader reader = new FileReader(CONFIG_FILE)) {
                ConfigData loaded = GSON.fromJson(reader, ConfigData.class);
                if (loaded != null) {
                    currentConfig = loaded;
                    // Ensure defaults for new fields
                    if (currentConfig.selectedItemId == null) {
                        currentConfig.selectedItemId = "minecraft:slime_ball";
                    }
                    System.out.println("[SlimeSeller Config] Loaded config:");
                    System.out.println("  - Selected item: " + currentConfig.selectedItemId);
                    System.out.println("  - Auto-drop minecarts: " + currentConfig.autoDropMinecarts);
                } else {
                    System.out.println("[SlimeSeller Config] Invalid config file, using defaults");
                    currentConfig = new ConfigData();
                }
            }
        } catch (IOException e) {
            System.err.println("[SlimeSeller Config] Failed to load config: " + e.getMessage());
            currentConfig = new ConfigData();
        }
    }

    /**
     * Save current configuration to file.
     */
    public static void save() {
        try {
            // Ensure config directory exists
            if (!CONFIG_DIR.exists()) {
                CONFIG_DIR.mkdirs();
            }

            try (FileWriter writer = new FileWriter(CONFIG_FILE)) {
                GSON.toJson(currentConfig, writer);
                System.out.println("[SlimeSeller Config] Saved config");
            }
        } catch (IOException e) {
            System.err.println("[SlimeSeller Config] Failed to save config: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Get the currently selected item from config.
     * @return The selected Item, or SLIME_BALL as fallback
     */
    public static Item getSelectedItem() {
        try {
            if (currentConfig.selectedItemId == null || currentConfig.selectedItemId.isEmpty()) {
                return Items.SLIME_BALL;
            }

            Identifier id = Identifier.of(currentConfig.selectedItemId);
            Item item = Registries.ITEM.get(id);

            // Check if item exists (won't be AIR if valid)
            if (item != null && item != Items.AIR) {
                return item;
            }

            System.err.println("[SlimeSeller Config] Invalid item ID in config: " + currentConfig.selectedItemId);
            return Items.SLIME_BALL;
        } catch (Exception e) {
            System.err.println("[SlimeSeller Config] Error getting selected item: " + e.getMessage());
            return Items.SLIME_BALL;
        }
    }

    /**
     * Get the ID of the currently selected item.
     * @return Item identifier string
     */
    public static String getSelectedItemId() {
        return currentConfig.selectedItemId;
    }

    /**
     * Set the selected item and save to config file.
     * @param item The item to select
     */
    public static void setSelectedItem(Item item) {
        try {
            Identifier id = Registries.ITEM.getId(item);
            currentConfig.selectedItemId = id.toString();
            save();
            System.out.println("[SlimeSeller Config] Selected item updated to: " + currentConfig.selectedItemId);
        } catch (Exception e) {
            System.err.println("[SlimeSeller Config] Error setting selected item: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Check if an item is currently selected.
     * @param item The item to check
     * @return true if this item is selected
     */
    public static boolean isItemSelected(Item item) {
        try {
            Identifier id = Registries.ITEM.getId(item);
            return id.toString().equals(currentConfig.selectedItemId);
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Check if auto-drop minecarts is enabled.
     * @return true if auto-drop is enabled
     */
    public static boolean isAutoDropMinecarts() {
        return currentConfig.autoDropMinecarts;
    }

    /**
     * Set auto-drop minecarts and save to config.
     * @param enabled true to enable, false to disable
     */
    public static void setAutoDropMinecarts(boolean enabled) {
        currentConfig.autoDropMinecarts = enabled;
        save();
        System.out.println("[SlimeSeller Config] Auto-drop minecarts: " + enabled);
    }

    /**
     * Toggle auto-drop minecarts setting.
     */
    public static void toggleAutoDropMinecarts() {
        setAutoDropMinecarts(!currentConfig.autoDropMinecarts);
    }

    /**
     * Reset configuration to defaults.
     */
    public static void reset() {
        currentConfig = new ConfigData();
        currentConfig.selectedItemId = "minecraft:slime_ball";
        currentConfig.autoDropMinecarts = false;
        save();
        System.out.println("[SlimeSeller Config] Configuration reset to defaults");
    }

    /**
     * Internal configuration data class.
     */
    private static class ConfigData {
        String selectedItemId = "minecraft:slime_ball";
        boolean autoDropMinecarts = false;

        // Future config options can be added here:
        // int autoSellMinDelay = 40;
        // int autoSellMaxDelay = 100;
        // boolean playSound = true;
        // List<String> favoriteItems = new ArrayList<>();
    }
}