package com.tacz.guns.client.gui.components.smith;

import com.tacz.guns.GunMod;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;

public class TypeButton extends Button {
    private static final Identifier TEXTURE = com.tacz.guns.util.IdHelper.id(GunMod.MOD_ID, "textures/gui/gun_smith_table.png");
    private final ItemStack stack;
    private boolean isSelected = false;

    public TypeButton(int pX, int pY, ItemStack stack, Button.OnPress onPress) {
        super(pX, pY, 24, 25, Component.empty(), b -> {
            if (b instanceof TypeButton typeButton) {
                typeButton.isSelected = true;
            }
            onPress.onPress(b);
        }, DEFAULT_NARRATION);
        this.stack = stack;
    }

    @Override
    protected void extractContents(GuiGraphicsExtractor gui, int pMouseX, int pMouseY, float pPartialTick) {
        int vOffset = isHoveredOrFocused() ? 204 + this.height : 204;
        int uOffset = isSelected ? 0 : 26;
        gui.blit(RenderPipelines.GUI_TEXTURED, TEXTURE, this.getX(), this.getY(), uOffset, vOffset, this.width, this.height, 256, 256);
        if (!this.stack.isEmpty()) {
            gui.item(this.stack, this.getX() + 4, this.getY() + 5);
        }
    }

    public void setSelected(boolean selected) {
        isSelected = selected;
    }
}


