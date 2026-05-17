package com.tacz.guns.client.gui.components.refit;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.network.chat.Component;

import java.util.Collections;
import java.util.List;
import java.util.function.Consumer;

public class RefitUnloadButton extends Button implements IComponentTooltip {
    public RefitUnloadButton(int pX, int pY, Button.OnPress pOnPress) {
        super(pX, pY, 8, 8, Component.empty(), pOnPress, DEFAULT_NARRATION);
    }

    @Override
    protected void extractContents(GuiGraphicsExtractor graphics, int pMouseX, int pMouseY, float pPartialTick) {
        int x = getX();
        int y = getY();
        int bg = isHoveredOrFocused() ? 0xAAFFFFFF : 0x66333333;
        graphics.fill(x, y, x + width, y + height, bg);
        graphics.fill(x + 2, y + 2, x + width - 2, y + height - 2, 0xCC222222);
    }

    @Override
    public void renderTooltip(Consumer<List<Component>> consumer) {
        if (this.isHoveredOrFocused()) {
            consumer.accept(Collections.singletonList(Component.translatable("tooltip.tacz.refit.unload")));
        }
    }
}
