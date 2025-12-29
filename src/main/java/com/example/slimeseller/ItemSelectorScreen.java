package com.example.slimeseller;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.Drawable;
import net.minecraft.client.gui.Element;
import net.minecraft.client.gui.Selectable;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
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
    private int hoveredIndex = -1;
    private TextFieldWidget searchField;

    private static final int ITEMS_PER_ROW = 9;
    private static final int ROWS_VISIBLE = 5;
    private static final int ITEM_SIZE = 32;
    private static final int ITEM_SPACING = 4;
    private static final int PADDING = 20;

    private ItemGrid itemGrid;

    public ItemSelectorScreen(Text title, Consumer<Item> onItemSelected) {
        super(title);
        this.onItemSelected = onItemSelected;
        this.availableItems = new ArrayList<>();
        this.filteredItems = new ArrayList<>();

        // Populate with items from player's inventory
        populateItemsFromInventory();
    }

    private void populateItemsFromInventory() {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player == null) return;

        List<Item> foundItems = new ArrayList<>();

        // Scan through player inventory to find unique items
        for (int i = 0; i < client.player.getInventory().size(); i++) {
            ItemStack stack = client.player.getInventory().getStack(i);
            if (!stack.isEmpty() && !foundItems.contains(stack.getItem())) {
                foundItems.add(stack.getItem());
            }
        }

        // If no items in inventory, add some common sellable items as defaults
        if (foundItems.isEmpty()) {
            foundItems.add(net.minecraft.item.Items.SLIME_BALL);
            foundItems.add(net.minecraft.item.Items.DIAMOND);
            foundItems.add(net.minecraft.item.Items.EMERALD);
            foundItems.add(net.minecraft.item.Items.GOLD_INGOT);
            foundItems.add(net.minecraft.item.Items.IRON_INGOT);
            foundItems.add(net.minecraft.item.Items.NETHERITE_INGOT);
            foundItems.add(net.minecraft.item.Items.COPPER_INGOT);
            foundItems.add(net.minecraft.item.Items.COAL);
            foundItems.add(net.minecraft.item.Items.ENDER_PEARL);
            foundItems.add(net.minecraft.item.Items.BLAZE_ROD);
        }

        availableItems.addAll(foundItems);
        filteredItems.addAll(foundItems); // Initially show all items
    }

    @Override
    protected void init() {
        super.init();

        // Add search field at the top
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
        this.addDrawableChild(searchField);

        // Calculate grid position (lower due to search bar)
        int startX = (this.width - (ITEMS_PER_ROW * (ITEM_SIZE + ITEM_SPACING))) / 2;
        int startY = PADDING + 60;

        // Add invisible clickable element for item grid
        itemGrid = new ItemGrid(startX, startY);
        this.addDrawableChild(itemGrid);

        // Add close button at the bottom
        this.addDrawableChild(ButtonWidget.builder(ScreenTexts.DONE, button -> this.close())
                .dimensions(this.width / 2 - 100, this.height - 30, 200, 20)
                .build());
    }

    private void onSearchChanged(String searchText) {
        filteredItems.clear();
        scrollOffset = 0; // Reset scroll when search changes

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
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        // Draw simple background instead of using renderBackground to avoid blur issues
        context.fill(0, 0, this.width, this.height, 0xC0101010);

        // Draw title
        context.drawCenteredTextWithShadow(this.textRenderer, this.title, this.width / 2, PADDING, 0xFFFFFF);

        // Draw instruction text
        String instruction = "Search and click an item to select it";
        context.drawCenteredTextWithShadow(this.textRenderer, instruction, this.width / 2, PADDING + 15, 0xAAAAAA);

        // Calculate grid position
        int startX = (this.width - (ITEMS_PER_ROW * (ITEM_SIZE + ITEM_SPACING))) / 2;
        int startY = PADDING + 60;

        hoveredIndex = -1;

        // Draw items in grid (using filtered items)
        int maxVisibleItems = ITEMS_PER_ROW * ROWS_VISIBLE;
        for (int i = scrollOffset; i < Math.min(filteredItems.size(), scrollOffset + maxVisibleItems); i++) {
            int displayIndex = i - scrollOffset;
            int row = displayIndex / ITEMS_PER_ROW;
            int col = displayIndex % ITEMS_PER_ROW;

            int x = startX + col * (ITEM_SIZE + ITEM_SPACING);
            int y = startY + row * (ITEM_SIZE + ITEM_SPACING);

            Item item = filteredItems.get(i);

            // Draw slot background
            context.fill(x, y, x + ITEM_SIZE, y + ITEM_SIZE, 0xFF8B8B8B);
            context.fill(x + 1, y + 1, x + ITEM_SIZE - 1, y + ITEM_SIZE - 1, 0xFF373737);

            // Show if this is the currently selected item with green border
            if (item == SlimeSellerMod.getSelectedItem()) {
                context.fill(x, y, x + ITEM_SIZE, y + 2, 0xFF00FF00); // Top
                context.fill(x, y + ITEM_SIZE - 2, x + ITEM_SIZE, y + ITEM_SIZE, 0xFF00FF00); // Bottom
                context.fill(x, y, x + 2, y + ITEM_SIZE, 0xFF00FF00); // Left
                context.fill(x + ITEM_SIZE - 2, y, x + ITEM_SIZE, y + ITEM_SIZE, 0xFF00FF00); // Right
            }

            // Check if mouse is hovering over this item
            if (mouseX >= x && mouseX < x + ITEM_SIZE && mouseY >= y && mouseY < y + ITEM_SIZE) {
                hoveredIndex = i;
                // Draw highlight
                context.fill(x + 1, y + 1, x + ITEM_SIZE - 1, y + ITEM_SIZE - 1, 0x80FFFFFF);
            }

            // Draw item
            ItemStack stack = new ItemStack(item);
            context.drawItem(stack, x + 8, y + 8);
        }

        // Draw scroll indicator if needed
        if (filteredItems.size() > maxVisibleItems) {
            String scrollText = "Scroll to see more items (" + filteredItems.size() + " total)";
            context.drawCenteredTextWithShadow(this.textRenderer, scrollText, this.width / 2,
                    startY + ROWS_VISIBLE * (ITEM_SIZE + ITEM_SPACING) + 10, 0x888888);
        } else if (filteredItems.isEmpty()) {
            String noResultsText = "No items found";
            context.drawCenteredTextWithShadow(this.textRenderer, noResultsText, this.width / 2,
                    startY + 50, 0xFF8888);
        }

        // Draw buttons and other widgets
        super.render(context, mouseX, mouseY, delta);

        // Draw tooltip for hovered item (must be after super.render)
        if (hoveredIndex >= 0 && hoveredIndex < filteredItems.size()) {
            Item item = filteredItems.get(hoveredIndex);
            // Draw simple tooltip with item name
            context.drawTooltip(this.textRenderer, item.getName(), mouseX, mouseY);
        }
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
        int maxScroll = Math.max(0, filteredItems.size() - (ITEMS_PER_ROW * ROWS_VISIBLE));
        int scrollRows = (maxScroll + ITEMS_PER_ROW - 1) / ITEMS_PER_ROW;

        if (verticalAmount > 0) {
            // Scroll up
            scrollOffset = Math.max(0, scrollOffset - ITEMS_PER_ROW);
        } else if (verticalAmount < 0) {
            // Scroll down
            scrollOffset = Math.min(scrollRows * ITEMS_PER_ROW, scrollOffset + ITEMS_PER_ROW);
        }

        return super.mouseScrolled(mouseX, mouseY, horizontalAmount, verticalAmount);
    }

    @Override
    public boolean shouldPause() {
        return false;
    }

    // Custom element to handle item grid clicks
    private class ItemGrid implements Element, Drawable, Selectable {
        private final int startX;
        private final int startY;

        public ItemGrid(int startX, int startY) {
            this.startX = startX;
            this.startY = startY;
        }

        @Override
        public void setFocused(boolean focused) {
        }

        @Override
        public boolean isFocused() {
            return false;
        }

        @Override
        public SelectionType getType() {
            return SelectionType.NONE;
        }

        @Override
        public void appendNarrations(net.minecraft.client.gui.screen.narration.NarrationMessageBuilder builder) {
            builder.put(net.minecraft.client.gui.screen.narration.NarrationPart.HINT,
                    Text.literal("Item grid with " + filteredItems.size() + " items"));
        }

        @Override
        public void render(DrawContext context, int mouseX, int mouseY, float delta) {
            // Rendering is handled by the parent screen
        }

        // This method handles mouse clicks on the grid
        public boolean mouseClicked(double mouseX, double mouseY, int button) {
            if (button != 0) return false; // Only handle left clicks

            System.out.println("[ItemSelector] Mouse clicked at " + mouseX + ", " + mouseY);

            // Check which item was clicked (using filtered items)
            int maxVisibleItems = ITEMS_PER_ROW * ROWS_VISIBLE;
            for (int i = scrollOffset; i < Math.min(filteredItems.size(), scrollOffset + maxVisibleItems); i++) {
                int displayIndex = i - scrollOffset;
                int row = displayIndex / ITEMS_PER_ROW;
                int col = displayIndex % ITEMS_PER_ROW;

                int x = startX + col * (ITEM_SIZE + ITEM_SPACING);
                int y = startY + row * (ITEM_SIZE + ITEM_SPACING);

                if (mouseX >= x && mouseX < x + ITEM_SIZE && mouseY >= y && mouseY < y + ITEM_SIZE) {
                    // Item clicked!
                    Item selectedItem = filteredItems.get(i);
                    System.out.println("[ItemSelector] Item clicked: " + selectedItem.getName().getString());
                    onItemSelected.accept(selectedItem);
                    ItemSelectorScreen.this.close();
                    return true;
                }
            }

            return false;
        }
    }
}