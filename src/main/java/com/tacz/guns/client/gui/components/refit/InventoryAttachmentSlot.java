package com.tacz.guns.client.gui.components.refit;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;

import java.util.function.Consumer;

public class InventoryAttachmentSlot extends Button implements IStackTooltip {
    private final int slotIndex;
    private final Inventory inventory;

    public InventoryAttachmentSlot(int pX, int pY, int slotIndex, Inventory inventory, Button.OnPress onPress) {
        super(pX, pY, 18, 18, Component.empty(), onPress, DEFAULT_NARRATION);
        this.slotIndex = slotIndex;
        this.inventory = inventory;
    }

    @Override
    public void renderTooltip(Consumer<ItemStack> consumer) {
        if (this.isHoveredOrFocused() && 0 <= this.slotIndex && this.slotIndex < this.inventory.getContainerSize()) {
            consumer.accept(this.inventory.getItem(slotIndex));
        }
    }

    @Override
    protected void extractContents(GuiGraphicsExtractor graphics, int pMouseX, int pMouseY, float pPartialTick) {
        int x = getX();
        int y = getY();
        int bg = isHoveredOrFocused() ? 0xAAFFFFFF : 0x66333333;
        graphics.fill(x, y, x + width, y + height, bg);
        if (0 <= slotIndex && slotIndex < this.inventory.getContainerSize()) {
            graphics.item(inventory.getItem(slotIndex), x + 1, y + 1);
        }
    }

    public int getSlotIndex() {
        return slotIndex;
    }
}