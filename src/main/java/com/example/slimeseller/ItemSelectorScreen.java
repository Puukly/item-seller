package com.example.slimeseller;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.registry.Registries;
import net.minecraft.screen.ScreenTexts;
import net.minecraft.text.Text;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

public class ItemSelectorScreen extends Screen {
    private final Consumer<Item> onItemSelected;
    private final List<Item> availableItems;
    private final List<Item> filteredItems;
    private int scrollOffset = 0;
    private TextFieldWidget searchField;
    private ButtonWidget autoDropToggleButton;
    private ButtonWidget itemGridButton;

    private static final int ITEMS_PER_ROW = 9;
    private static final int ROWS_VISIBLE = 5;
    private static final int ITEM_SIZE = 32;
    private static final int ITEM_SPACING = 4;
    private static final int PADDING = 20;
    private static final int TOP_SECTION_HEIGHT = 70;

    public ItemSelectorScreen(Text title, Consumer<Item> onItemSelected) {
        super(title);
        this.onItemSelected = onItemSelected;
        this.availableItems = new ArrayList<>();
        this.filteredItems = new ArrayList<>();
        populateAllItems();
    }

    private void populateAllItems() {
        for (Item item : Registries.ITEM) {
            if (item != Items.AIR) {
                availableItems.add(item);
            }
        }
        System.out.println("[ItemSelector] Loaded " + availableItems.size() + " items");
        filteredItems.addAll(availableItems);
    }

    @Override
    protected void init() {
        super.init();

        // Search field
        int searchFieldWidth = 200;
        searchField = new TextFieldWidget(
                this.textRenderer,
                this.width / 2 - searchFieldWidth / 2,
                PADDING + 30,
                searchFieldWidth,
                20,
                Text.literal("Search items...")
        );
        searchField.setMaxLength(50);
        searchField.setChangedListener(this::onSearchChanged);
        searchField.setPlaceholder(Text.literal("Search..."));
        addDrawableChild(searchField);

        // Auto-drop toggle button
        int buttonWidth = 150;
        autoDropToggleButton = ButtonWidget.builder(
                getAutoDropButtonText(),
                button -> {
                    ModConfig.toggleAutoDropMinecarts();
                    button.setMessage(getAutoDropButtonText());
                    System.out.println("[ItemSelector] Minecart toggle: " + ModConfig.isAutoDropMinecarts());
                }
        ).dimensions(this.width / 2 - buttonWidth / 2, PADDING + 55, buttonWidth, 20).build();
        addDrawableChild(autoDropToggleButton);

        // Invisible button covering the item grid area that handles clicks
        int startX = (this.width - (ITEMS_PER_ROW * (ITEM_SIZE + ITEM_SPACING))) / 2;
        int startY = PADDING + TOP_SECTION_HEIGHT + 10;
        int gridWidth = ITEMS_PER_ROW * (ITEM_SIZE + ITEM_SPACING);
        int gridHeight = ROWS_VISIBLE * (ITEM_SIZE + ITEM_SPACING);

        itemGridButton = new ButtonWidget(startX, startY, gridWidth, gridHeight, Text.empty(),
                button -> handleGridClick(), ButtonWidget.DEFAULT_NARRATION_SUPPLIER) {
            @Override
            public void renderWidget(DrawContext context, int mouseX, int mouseY, float delta) {
                // Don't render anything - completely invisible button
            }
        };
        addDrawableChild(itemGridButton);

        System.out.println("[ItemSelector] Grid button at " + startX + "," + startY + " size " + gridWidth + "x" + gridHeight);

        // Done button
        addDrawableChild(ButtonWidget.builder(ScreenTexts.DONE, button -> this.close())
                .dimensions(this.width / 2 - 100, this.height - 30, 200, 20)
                .build());
    }

    private void handleGridClick() {
        // Get mouse position
        MinecraftClient client = MinecraftClient.getInstance();
        double mouseX = client.mouse.getX() * (double)client.getWindow().getScaledWidth() / (double)client.getWindow().getWidth();
        double mouseY = client.mouse.getY() * (double)client.getWindow().getScaledHeight() / (double)client.getWindow().getHeight();

        System.out.println("[ItemSelector] Grid clicked! Mouse at " + mouseX + ", " + mouseY);

        int startX = (this.width - (ITEMS_PER_ROW * (ITEM_SIZE + ITEM_SPACING))) / 2;
        int startY = PADDING + TOP_SECTION_HEIGHT + 10;

        int maxVisibleItems = ITEMS_PER_ROW * ROWS_VISIBLE;
        for (int i = scrollOffset; i < Math.min(filteredItems.size(), scrollOffset + maxVisibleItems); i++) {
            int displayIndex = i - scrollOffset;
            int row = displayIndex / ITEMS_PER_ROW;
            int col = displayIndex % ITEMS_PER_ROW;

            int x = startX + col * (ITEM_SIZE + ITEM_SPACING);
            int y = startY + row * (ITEM_SIZE + ITEM_SPACING);

            if (mouseX >= x && mouseX < x + ITEM_SIZE && mouseY >= y && mouseY < y + ITEM_SIZE) {
                Item selectedItem = filteredItems.get(i);
                System.out.println("[ItemSelector] *** SELECTED: " + selectedItem.getName().getString() + " ***");
                onItemSelected.accept(selectedItem);
                this.close();
                return;
            }
        }
        System.out.println("[ItemSelector] Click was outside all item slots");
    }

    private Text getAutoDropButtonText() {
        if (ModConfig.isAutoDropMinecarts()) {
            return Text.literal("§aAuto-Drop Minecarts: ON");
        } else {
            return Text.literal("§cAuto-Drop Minecarts: OFF");
        }
    }

    private void onSearchChanged(String searchText) {
        filteredItems.clear();
        scrollOffset = 0;

        if (searchText.isEmpty()) {
            filteredItems.addAll(availableItems);
        } else {
            String lowerSearch = searchText.toLowerCase();
            for (Item item : availableItems) {
                String itemName = item.getName().getString().toLowerCase();
                if (itemName.contains(lowerSearch)) {
                    filteredItems.add(item);
                }
            }
        }
        System.out.println("[ItemSelector] Filtered to " + filteredItems.size() + " items");
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        context.fill(0, 0, this.width, this.height, 0xC0101010);
        context.drawCenteredTextWithShadow(this.textRenderer, this.title, this.width / 2, PADDING, 0xFFFFFF);

        String instruction = "Search and click an item to select it";
        context.drawCenteredTextWithShadow(this.textRenderer, instruction, this.width / 2, PADDING + 15, 0xAAAAAA);

        int startX = (this.width - (ITEMS_PER_ROW * (ITEM_SIZE + ITEM_SPACING))) / 2;
        int startY = PADDING + TOP_SECTION_HEIGHT + 10;

        int hoveredIndex = -1;

        int maxVisibleItems = ITEMS_PER_ROW * ROWS_VISIBLE;
        for (int i = scrollOffset; i < Math.min(filteredItems.size(), scrollOffset + maxVisibleItems); i++) {
            int displayIndex = i - scrollOffset;
            int row = displayIndex / ITEMS_PER_ROW;
            int col = displayIndex % ITEMS_PER_ROW;

            int x = startX + col * (ITEM_SIZE + ITEM_SPACING);
            int y = startY + row * (ITEM_SIZE + ITEM_SPACING);

            Item item = filteredItems.get(i);

            context.fill(x, y, x + ITEM_SIZE, y + ITEM_SIZE, 0xFF8B8B8B);
            context.fill(x + 1, y + 1, x + ITEM_SIZE - 1, y + ITEM_SIZE - 1, 0xFF373737);

            if (item == SlimeSellerMod.getSelectedItem()) {
                context.fill(x, y, x + ITEM_SIZE, y + 2, 0xFF00FF00);
                context.fill(x, y + ITEM_SIZE - 2, x + ITEM_SIZE, y + ITEM_SIZE, 0xFF00FF00);
                context.fill(x, y, x + 2, y + ITEM_SIZE, 0xFF00FF00);
                context.fill(x + ITEM_SIZE - 2, y, x + ITEM_SIZE, y + ITEM_SIZE, 0xFF00FF00);
            }

            if (mouseX >= x && mouseX < x + ITEM_SIZE && mouseY >= y && mouseY < y + ITEM_SIZE) {
                hoveredIndex = i;
                context.fill(x + 1, y + 1, x + ITEM_SIZE - 1, y + ITEM_SIZE - 1, 0x80FFFFFF);
            }

            ItemStack stack = new ItemStack(item);
            context.drawItem(stack, x + 8, y + 8);
        }

        if (filteredItems.size() > maxVisibleItems) {
            String scrollText = "Scroll to see more (" + filteredItems.size() + " total)";
            context.drawCenteredTextWithShadow(this.textRenderer, scrollText, this.width / 2,
                    startY + ROWS_VISIBLE * (ITEM_SIZE + ITEM_SPACING) + 10, 0x888888);
        } else if (filteredItems.isEmpty()) {
            String noResultsText = "No items found";
            context.drawCenteredTextWithShadow(this.textRenderer, noResultsText, this.width / 2,
                    startY + 50, 0xFF8888);
        }

        super.render(context, mouseX, mouseY, delta);

        if (hoveredIndex >= 0 && hoveredIndex < filteredItems.size()) {
            Item item = filteredItems.get(hoveredIndex);
            context.drawTooltip(this.textRenderer, item.getName(), mouseX, mouseY);
        }
    }

    public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
        int maxScroll = Math.max(0, filteredItems.size() - (ITEMS_PER_ROW * ROWS_VISIBLE));
        int scrollRows = (maxScroll + ITEMS_PER_ROW - 1) / ITEMS_PER_ROW;

        if (verticalAmount > 0) {
            scrollOffset = Math.max(0, scrollOffset - ITEMS_PER_ROW);
        } else if (verticalAmount < 0) {
            scrollOffset = Math.min(scrollRows * ITEMS_PER_ROW, scrollOffset + ITEMS_PER_ROW);
        }

        return super.mouseScrolled(mouseX, mouseY, horizontalAmount, verticalAmount);
    }

    public boolean shouldPause() {
        return false;
    }
}