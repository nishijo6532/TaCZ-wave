package com.tacz.guns.client.gui.components.refit;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.network.chat.Component;

import java.util.Collections;
import java.util.List;
import java.util.function.Consumer;

public class RefitTurnPageButton extends Button implements IComponentTooltip {
    private final boolean isUpPage;

    public RefitTurnPageButton(int pX, int pY, boolean isUpPage, OnPress pOnPress) {
        super(pX, pY, 18, 8, Component.empty(), pOnPress, DEFAULT_NARRATION);
        this.isUpPage = isUpPage;
    }

    @Override
    protected void extractContents(GuiGraphicsExtractor graphics, int pMouseX, int pMouseY, float pPartialTick) {
        int x = getX();
        int y = getY();
        int bg = isHoveredOrFocused() ? 0xAAFFFFFF : 0x66333333;
        graphics.fill(x, y, x + width, y + height, bg);
    }

    @Override
    public void renderTooltip(Consumer<List<Component>> consumer) {
        if (this.isHoveredOrFocused()) {
            String key = isUpPage ? "tooltip.tacz.page.previous" : "tooltip.tacz.page.next";
            consumer.accept(Collections.singletonList(Component.translatable(key)));
        }
    }
}