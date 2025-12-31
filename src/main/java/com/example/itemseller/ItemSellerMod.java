package com.example.itemseller;

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
import net.minecraft.screen.slot.SlotActionType;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import org.lwjgl.glfw.GLFW;

import java.util.List;
import java.util.Random;

public class ItemSellerMod implements ClientModInitializer {
    private static KeyBinding sellItemKey;
    private static KeyBinding autoSellToggleKey;
    private static KeyBinding openItemSelectorKey;
    private static boolean waitingForContainer = false;
    private static boolean shouldCloseScreen = false;
    private static int ticksWaited = 0;
    private static int closeDelayTicks = 0;
    private static final int MAX_WAIT_TICKS = 40;
    private static final int CLOSE_DELAY_TICKS = 2;

    private static boolean autoSellEnabled = false;
    private static int autoSellDelayTicks = 0;
    private static int autoSellTargetDelay = 0;
    private static final Random random = new Random();
    private static boolean autoSellInProgress = false;

    @Override
    public void onInitializeClient() {
        ModConfig.load();
        List<Item> selectedItems = ModConfig.getSelectedItems();
        System.out.println("[ItemSeller] Loaded " + selectedItems.size() + " selected items");

        KeyBinding.Category itemSellerCategory = KeyBinding.Category.create(
                Identifier.of("itemseller", "main")
        );

        sellItemKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.itemseller.sell",
                InputUtil.Type.KEYSYM,
                GLFW.GLFW_KEY_F6,
                itemSellerCategory
        ));

        autoSellToggleKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.itemseller.autosell",
                InputUtil.Type.KEYSYM,
                GLFW.GLFW_KEY_F7,
                itemSellerCategory
        ));

        openItemSelectorKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.itemseller.selectitem",
                InputUtil.Type.KEYSYM,
                GLFW.GLFW_KEY_F8,
                itemSellerCategory
        ));

        HudRenderCallback.EVENT.register((drawContext, tickDelta) -> {
            if (autoSellEnabled) {
                MinecraftClient client = MinecraftClient.getInstance();
                if (client.player != null) {
                    renderAutoSellIndicator(drawContext);
                }
            }
        });

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if (client.player == null) return;

            if (openItemSelectorKey.wasPressed() && client.currentScreen == null) {
                openItemSelector(client);
            }

            if (autoSellToggleKey.wasPressed()) {
                autoSellEnabled = !autoSellEnabled;
                if (autoSellEnabled) {
                    List<Item> items = ModConfig.getSelectedItems();
                    System.out.println("[ItemSeller] Auto-sell ENABLED for " + items.size() + " items");
                    String itemNames = items.size() == 1 ? items.get(0).getName().getString() : items.size() + " items";
                    client.player.sendMessage(Text.literal("§aAuto-sell enabled for " + itemNames), false);
                    autoSellDelayTicks = 0;
                    autoSellTargetDelay = getRandomDelay();
                    autoSellInProgress = false;
                } else {
                    System.out.println("[ItemSeller] Auto-sell DISABLED");
                    client.player.sendMessage(Text.literal("§cAuto-sell disabled"), false);
                    autoSellInProgress = false;
                    waitingForContainer = false;
                    shouldCloseScreen = false;
                }
            }

            if (sellItemKey.wasPressed() && !waitingForContainer && !autoSellEnabled) {
                System.out.println("[ItemSeller] Manual hotkey pressed");
                executeSellCommand(client);
                waitingForContainer = true;
                ticksWaited = 0;
            }

            if (autoSellEnabled && !autoSellInProgress && !waitingForContainer) {
                autoSellDelayTicks++;
                if (autoSellDelayTicks >= autoSellTargetDelay) {
                    System.out.println("[ItemSeller] Auto-sell cycle starting");
                    executeSellCommand(client);
                    waitingForContainer = true;
                    autoSellInProgress = true;
                    ticksWaited = 0;
                    autoSellDelayTicks = 0;
                }
            }

            if (waitingForContainer) {
                ticksWaited++;

                if (client.currentScreen != null) {
                    System.out.println("[ItemSeller] Screen detected: " + client.currentScreen.getClass().getSimpleName());
                    moveSelectedItems(client);
                    waitingForContainer = false;
                    ticksWaited = 0;
                    shouldCloseScreen = true;
                    closeDelayTicks = 0;
                } else if (ticksWaited >= MAX_WAIT_TICKS) {
                    System.out.println("[ItemSeller] Timeout - no container opened after " + ticksWaited + " ticks");
                    waitingForContainer = false;
                    ticksWaited = 0;
                    autoSellInProgress = false;
                }
            }

            if (shouldCloseScreen) {
                closeDelayTicks++;
                if (closeDelayTicks >= CLOSE_DELAY_TICKS) {
                    closeScreen(client);
                    shouldCloseScreen = false;
                    closeDelayTicks = 0;

                    if (autoSellEnabled) {
                        autoSellInProgress = false;
                        autoSellTargetDelay = getRandomDelay();
                        System.out.println("[ItemSeller] Next cycle in " + (autoSellTargetDelay / 20.0) + " seconds");
                    }
                }
            }
        });
    }

    private void openItemSelector(MinecraftClient client) {
        System.out.println("[ItemSeller] Opening item selector");
        client.setScreen(new ItemSelectorScreen(Text.literal("Select Items to Sell"), this::onItemSelected));
    }

    private void onItemSelected(Item item) {
        // In single-select mode, this gets called
        ModConfig.setSingleItem(item);
        System.out.println("[ItemSeller] Selected item: " + item.getName().getString());

        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player != null) {
            client.player.sendMessage(
                    Text.literal("§aSelected item: §f" + item.getName().getString()),
                    false
            );
        }
    }

    public static Item getSelectedItem() {
        return ModConfig.getSelectedItem();
    }

    private int getRandomDelay() {
        return 40 + random.nextInt(61);
    }

    private void renderAutoSellIndicator(DrawContext drawContext) {
        MinecraftClient client = MinecraftClient.getInstance();
        List<Item> items = ModConfig.getSelectedItems();
        String text = items.size() == 1 ?
                "Auto Selling: " + items.get(0).getName().getString() :
                "Auto Selling: " + items.size() + " items";

        int textWidth = client.textRenderer.getWidth(text);
        int screenWidth = client.getWindow().getScaledWidth();

        int x = screenWidth - textWidth - 10;
        int y = 10;

        drawContext.fill(x - 4, y - 2, x + textWidth + 4, y + 12, 0x80000000);
        drawContext.drawText(client.textRenderer, text, x, y, 0xFF00FF00, true);
    }

    private void executeSellCommand(MinecraftClient client) {
        if (client.player != null && client.player.networkHandler != null) {
            try {
                client.player.networkHandler.sendChatCommand("sell");
                System.out.println("[ItemSeller] Sent /sell command");
            } catch (Exception e) {
                client.player.networkHandler.sendChatMessage("/sell");
            }
        }
    }

    private void moveSelectedItems(MinecraftClient client) {
        if (client.player == null || client.player.currentScreenHandler == null) {
            System.out.println("[ItemSeller] Cannot move items - player or handler is null");
            return;
        }

        try {
            var handler = client.player.currentScreenHandler;
            List<Item> selectedItems = ModConfig.getSelectedItems();

            System.out.println("[ItemSeller] Container opened, moving " + selectedItems.size() + " item types");

            int playerInventoryStart = handler.slots.size() - 36;
            int totalMoved = 0;

            for (int i = playerInventoryStart; i < handler.slots.size(); i++) {
                ItemStack stack = handler.slots.get(i).getStack();

                if (!stack.isEmpty() && selectedItems.contains(stack.getItem())) {
                    client.interactionManager.clickSlot(
                            handler.syncId,
                            i,
                            0,
                            SlotActionType.QUICK_MOVE,
                            client.player
                    );
                    totalMoved++;
                }
            }
            System.out.println("[ItemSeller] Moved " + totalMoved + " stacks total");
        } catch (Exception e) {
            System.err.println("[ItemSeller] Error moving items: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void closeScreen(MinecraftClient client) {
        if (client.currentScreen != null) {
            System.out.println("[ItemSeller] Closing screen");
            client.currentScreen.close();
        }
    }
}