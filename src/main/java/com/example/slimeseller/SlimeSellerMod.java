package com.example.slimeseller;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.screen.slot.SlotActionType;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class SlimeSellerMod implements ClientModInitializer {
    private static KeyBinding sellItemKey;
    private static KeyBinding autoSellToggleKey;
    private static KeyBinding openItemSelectorKey;
    private static boolean waitingForContainer = false;
    private static boolean shouldCloseScreen = false;
    private static int ticksWaited = 0;
    private static int closeDelayTicks = 0;
    private static final int MAX_WAIT_TICKS = 40; // 2 seconds at 20 TPS
    private static final int CLOSE_DELAY_TICKS = 2; // Small delay before closing (0.1 seconds)

    // Auto-sell variables
    private static boolean autoSellEnabled = false;
    private static int autoSellDelayTicks = 0;
    private static int autoSellTargetDelay = 0;
    private static final Random random = new Random();
    private static boolean autoSellInProgress = false;

    // Item selection
    private static Item selectedItem = Items.SLIME_BALL; // Default to slime ball
    private static ItemSelectorScreen itemSelectorScreen = null;

    @Override
    public void onInitializeClient() {
        // Create a custom category for our keybinding
        KeyBinding.Category slimeSellerCategory = KeyBinding.Category.create(
                Identifier.of("slimeseller", "main")
        );

        // Register the manual sell keybinding (F6 by default)
        sellItemKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.slimeseller.sell",
                InputUtil.Type.KEYSYM,
                GLFW.GLFW_KEY_F6,
                slimeSellerCategory
        ));

        // Register the auto-sell toggle keybinding (F7 by default)
        autoSellToggleKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.slimeseller.autosell",
                InputUtil.Type.KEYSYM,
                GLFW.GLFW_KEY_F7,
                slimeSellerCategory
        ));

        // Register the item selector keybinding (F8 by default)
        openItemSelectorKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.slimeseller.selectitem",
                InputUtil.Type.KEYSYM,
                GLFW.GLFW_KEY_F8,
                slimeSellerCategory
        ));

        // Register HUD renderer for auto-sell indicator
        HudRenderCallback.EVENT.register((drawContext, tickDelta) -> {
            if (autoSellEnabled) {
                MinecraftClient client = MinecraftClient.getInstance();
                if (client.player != null) {
                    renderAutoSellIndicator(drawContext);
                }
            }
        });

        // Register tick event to check for key press
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if (client.player == null) return;

            // Handle item selector key press
            if (openItemSelectorKey.wasPressed() && client.currentScreen == null) {
                openItemSelector(client);
            }

            // Handle auto-sell toggle key press
            if (autoSellToggleKey.wasPressed()) {
                autoSellEnabled = !autoSellEnabled;
                if (autoSellEnabled) {
                    System.out.println("[SlimeSeller] Auto-sell ENABLED for " + selectedItem.getName().getString());
                    autoSellDelayTicks = 0;
                    autoSellTargetDelay = getRandomDelay();
                    autoSellInProgress = false;
                } else {
                    System.out.println("[SlimeSeller] Auto-sell DISABLED");
                    autoSellInProgress = false;
                    waitingForContainer = false;
                    shouldCloseScreen = false;
                }
            }

            // Handle manual sell key press (only if auto-sell is not running)
            if (sellItemKey.wasPressed() && !waitingForContainer && !autoSellEnabled) {
                System.out.println("[SlimeSeller] Manual hotkey pressed");
                executeSellCommand(client);
                waitingForContainer = true;
                ticksWaited = 0;
            }

            // Auto-sell loop logic
            if (autoSellEnabled && !autoSellInProgress && !waitingForContainer) {
                autoSellDelayTicks++;
                if (autoSellDelayTicks >= autoSellTargetDelay) {
                    System.out.println("[SlimeSeller] Auto-sell cycle starting");
                    executeSellCommand(client);
                    waitingForContainer = true;
                    autoSellInProgress = true;
                    ticksWaited = 0;
                    autoSellDelayTicks = 0;
                }
            }

            // Wait for container to open
            if (waitingForContainer) {
                ticksWaited++;

                // Check if a GUI screen is now open
                if (client.currentScreen != null) {
                    System.out.println("[SlimeSeller] Screen detected: " + client.currentScreen.getClass().getSimpleName());
                    // Container is open, now move items
                    moveSelectedItems(client);
                    waitingForContainer = false;
                    ticksWaited = 0;
                    // Schedule screen close after a small delay
                    shouldCloseScreen = true;
                    closeDelayTicks = 0;
                } else if (ticksWaited >= MAX_WAIT_TICKS) {
                    // Timeout - stop waiting
                    System.out.println("[SlimeSeller] Timeout - no container opened after " + ticksWaited + " ticks");
                    waitingForContainer = false;
                    ticksWaited = 0;
                    autoSellInProgress = false;
                }
            }

            // Handle closing the screen after items are moved
            if (shouldCloseScreen) {
                closeDelayTicks++;
                if (closeDelayTicks >= CLOSE_DELAY_TICKS) {
                    closeScreen(client);
                    shouldCloseScreen = false;
                    closeDelayTicks = 0;

                    // If auto-sell is enabled, prepare for next cycle
                    if (autoSellEnabled) {
                        autoSellInProgress = false;
                        autoSellTargetDelay = getRandomDelay();
                        System.out.println("[SlimeSeller] Next cycle in " + (autoSellTargetDelay / 20.0) + " seconds");
                    }
                }
            }
        });
    }

    private void openItemSelector(MinecraftClient client) {
        System.out.println("[SlimeSeller] Opening item selector");
        itemSelectorScreen = new ItemSelectorScreen(Text.literal("Select Item to Sell"), this::onItemSelected);
        client.setScreen(itemSelectorScreen);
    }

    private void onItemSelected(Item item) {
        selectedItem = item;
        System.out.println("[SlimeSeller] Selected item: " + item.getName().getString());
    }

    private int getRandomDelay() {
        // Random delay between 2-5 seconds (40-100 ticks)
        return 40 + random.nextInt(61); // 40 + (0 to 60) = 40 to 100 ticks
    }

    private void renderAutoSellIndicator(DrawContext drawContext) {
        MinecraftClient client = MinecraftClient.getInstance();
        String text = "Auto Selling Running";

        // Get text width for positioning
        int textWidth = client.textRenderer.getWidth(text);
        int screenWidth = client.getWindow().getScaledWidth();

        // Position at top right with some padding
        int x = screenWidth - textWidth - 10;
        int y = 10;

        // Draw text with green color and shadow (bold effect done by drawing twice with offset)
        // Green color: 0xFF00FF00 (ARGB format)
        drawContext.drawText(client.textRenderer, text, x, y, 0xFF00FF00, true);
        // Draw again slightly offset for bold effect
        drawContext.drawText(client.textRenderer, text, x + 1, y, 0xFF00FF00, true);
    }

    private void executeSellCommand(MinecraftClient client) {
        if (client.player != null && client.player.networkHandler != null) {
            String command = "sell";

            try {
                client.player.networkHandler.sendChatCommand(command);
                System.out.println("[SlimeSeller] Sent command using sendChatCommand()");
            } catch (Exception e) {
                client.player.networkHandler.sendChatMessage("/sell");
                System.out.println("[SlimeSeller] Fallback: sent using sendChatMessage()");
            }
        }
    }

    private void moveSelectedItems(MinecraftClient client) {
        if (client.player == null || client.player.currentScreenHandler == null) {
            System.out.println("[SlimeSeller] Cannot move items - player or handler is null");
            return;
        }

        try {
            var handler = client.player.currentScreenHandler;

            System.out.println("[SlimeSeller] Container opened with " + handler.slots.size() + " slots");

            int playerInventoryStart = handler.slots.size() - 36;

            int movedCount = 0;
            for (int i = playerInventoryStart; i < handler.slots.size(); i++) {
                ItemStack stack = handler.slots.get(i).getStack();

                // Check if the item matches the selected item
                if (!stack.isEmpty() && stack.getItem() == selectedItem) {
                    System.out.println("[SlimeSeller] Moving " + selectedItem.getName().getString() + " from slot " + i);
                    client.interactionManager.clickSlot(
                            handler.syncId,
                            i,
                            0,
                            SlotActionType.QUICK_MOVE,
                            client.player
                    );
                    movedCount++;
                }
            }
            System.out.println("[SlimeSeller] Moved " + movedCount + " " + selectedItem.getName().getString() + " stacks");
        } catch (Exception e) {
            System.err.println("[SlimeSeller] Error moving items: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void closeScreen(MinecraftClient client) {
        if (client.currentScreen != null) {
            System.out.println("[SlimeSeller] Closing screen");
            client.currentScreen.close();
        }
    }
}