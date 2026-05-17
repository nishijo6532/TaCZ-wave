package com.tacz.guns.client.gui.components.smith;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;

import java.util.function.Consumer;

public class ResultButton extends Button {
    private final ItemStack stack;
    private boolean isSelected = false;

    public ResultButton(int pX, int pY, ItemStack stack, Button.OnPress onPress) {
        super(pX, pY, 94, 16, Component.empty(), b -> {
            if (b instanceof ResultButton resultButton) {
                resultButton.isSelected = true;
            }
            onPress.onPress(b);
        }, DEFAULT_NARRATION);
        this.stack = stack;
    }

    @Override
    protected void extractContents(GuiGraphicsExtractor gui, int pMouseX, int pMouseY, float pPartialTick) {
        int x = this.getX();
        int y = this.getY();
        int bg = isSelected ? 0xAA44AA44 : 0x66333333;
        if (isHoveredOrFocused()) {
            bg = 0xAA666666;
        }
        gui.fill(x, y, x + this.width, y + this.height, bg);
        if (!stack.isEmpty()) {
            var font = net.minecraft.client.Minecraft.getInstance().font;
            gui.item(stack, x + 1, y);
            String label = this.stack.getHoverName().getString();
            int maxWidth = this.width - 22;
            gui.text(font, trimToWidth(font, label, maxWidth), x + 20, y + 4, 0xFFFFFFFF, false);
        }
    }

    private static String trimToWidth(net.minecraft.client.gui.Font font, String text, int maxWidth) {
        int end = 0;
        while (end < text.length() && font.width(text.substring(0, end + 1)) <= maxWidth) {
            end++;
        }
        return text.substring(0, end);
    }

    public void setSelected(boolean selected) {
        isSelected = selected;
    }

    public void renderTooltips(Consumer<ItemStack> consumer) {
        if (this.isHoveredOrFocused() && !this.stack.isEmpty()) {
            consumer.accept(this.stack);
        }
    }
}


