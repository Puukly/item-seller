package com.example.itemseller;

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
import java.util.ArrayList;
import java.util.List;

public class ModConfig {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final File CONFIG_DIR = new File("config");
    private static final File CONFIG_FILE = new File(CONFIG_DIR, "itemseller.json");

    private static ConfigData currentConfig = new ConfigData();

    public static void load() {
        try {
            if (!CONFIG_FILE.exists()) {
                System.out.println("[ItemSeller Config] No config file found, creating default");
                currentConfig = new ConfigData();
                currentConfig.selectedItemIds = new ArrayList<>();
                currentConfig.selectedItemIds.add("minecraft:sugar_cane");
                currentConfig.multiSelectMode = false;
                save();
                return;
            }

            try (FileReader reader = new FileReader(CONFIG_FILE)) {
                ConfigData loaded = GSON.fromJson(reader, ConfigData.class);
                if (loaded != null) {
                    currentConfig = loaded;
                    if (currentConfig.selectedItemIds == null || currentConfig.selectedItemIds.isEmpty()) {
                        currentConfig.selectedItemIds = new ArrayList<>();
                        currentConfig.selectedItemIds.add("minecraft:sugar_cane");
                    }
                    System.out.println("[ItemSeller Config] Loaded " + currentConfig.selectedItemIds.size() + " selected items");
                    System.out.println("[ItemSeller Config] Multi-select mode: " + currentConfig.multiSelectMode);
                } else {
                    currentConfig = new ConfigData();
                }
            }
        } catch (IOException e) {
            System.err.println("[ItemSeller Config] Failed to load: " + e.getMessage());
            currentConfig = new ConfigData();
        }
    }

    public static void save() {
        try {
            if (!CONFIG_DIR.exists()) {
                CONFIG_DIR.mkdirs();
            }
            try (FileWriter writer = new FileWriter(CONFIG_FILE)) {
                GSON.toJson(currentConfig, writer);
            }
        } catch (IOException e) {
            System.err.println("[ItemSeller Config] Failed to save: " + e.getMessage());
        }
    }

    public static Item getSelectedItem() {
        if (currentConfig.selectedItemIds == null || currentConfig.selectedItemIds.isEmpty()) {
            return Items.SUGAR_CANE;
        }
        try {
            Identifier id = Identifier.of(currentConfig.selectedItemIds.get(0));
            Item item = Registries.ITEM.get(id);
            if (item != null && item != Items.AIR) {
                return item;
            }
        } catch (Exception e) {
            // Fallthrough
        }
        return Items.SUGAR_CANE;
    }

    public static List<Item> getSelectedItems() {
        List<Item> items = new ArrayList<>();
        if (currentConfig.selectedItemIds == null) {
            items.add(Items.SUGAR_CANE);
            return items;
        }
        for (String itemId : currentConfig.selectedItemIds) {
            try {
                Identifier id = Identifier.of(itemId);
                Item item = Registries.ITEM.get(id);
                if (item != null && item != Items.AIR) {
                    items.add(item);
                }
            } catch (Exception ignored) {}
        }
        if (items.isEmpty()) {
            items.add(Items.SUGAR_CANE);
        }
        return items;
    }

    public static boolean isItemSelected(Item item) {
        try {
            Identifier id = Registries.ITEM.getId(item);
            return currentConfig.selectedItemIds != null &&
                    currentConfig.selectedItemIds.contains(id.toString());
        } catch (Exception e) {
            return false;
        }
    }

    public static void setSingleItem(Item item) {
        try {
            Identifier id = Registries.ITEM.getId(item);
            currentConfig.selectedItemIds = new ArrayList<>();
            currentConfig.selectedItemIds.add(id.toString());
            save();
            System.out.println("[ItemSeller Config] Set single item: " + item.getName().getString());
        } catch (Exception e) {
            System.err.println("[ItemSeller Config] Error setting item");
        }
    }

    public static void toggleItem(Item item) {
        try {
            Identifier id = Registries.ITEM.getId(item);
            String itemId = id.toString();

            if (currentConfig.selectedItemIds == null) {
                currentConfig.selectedItemIds = new ArrayList<>();
            }

            if (currentConfig.selectedItemIds.contains(itemId)) {
                if (currentConfig.selectedItemIds.size() > 1) {
                    currentConfig.selectedItemIds.remove(itemId);
                    System.out.println("[ItemSeller Config] Removed: " + item.getName().getString());
                }
            } else {
                currentConfig.selectedItemIds.add(itemId);
                System.out.println("[ItemSeller Config] Added: " + item.getName().getString());
            }
            save();
        } catch (Exception e) {
            System.err.println("[ItemSeller Config] Error toggling item");
        }
    }

    public static boolean isMultiSelectMode() {
        return currentConfig.multiSelectMode;
    }

    public static void toggleMultiSelectMode() {
        currentConfig.multiSelectMode = !currentConfig.multiSelectMode;

        if (!currentConfig.multiSelectMode && currentConfig.selectedItemIds != null &&
                currentConfig.selectedItemIds.size() > 1) {
            String firstItem = currentConfig.selectedItemIds.get(0);
            currentConfig.selectedItemIds.clear();
            currentConfig.selectedItemIds.add(firstItem);
        }

        save();
        System.out.println("[ItemSeller Config] Multi-select: " + currentConfig.multiSelectMode);
    }

    public static int getSelectedItemCount() {
        return currentConfig.selectedItemIds != null ? currentConfig.selectedItemIds.size() : 1;
    }

    private static class ConfigData {
        List<String> selectedItemIds = new ArrayList<>();
        boolean multiSelectMode = false;
    }
}