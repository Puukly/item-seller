package com.example.itemseller;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.text.Text;

/**
 * A button widget that doesn't render anything but still captures clicks.
 */
public class InvisibleButtonWidget extends ButtonWidget {

    public InvisibleButtonWidget(int x, int y, int width, int height, PressAction onPress) {
        super(x, y, width, height, Text.empty(), onPress, DEFAULT_NARRATION_SUPPLIER);
    }

    @Override
    protected void renderWidget(DrawContext context, int mouseX, int mouseY, float delta) {
        // Don't render anything - completely invisible
    }
}