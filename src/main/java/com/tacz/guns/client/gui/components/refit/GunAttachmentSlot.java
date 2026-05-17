package com.tacz.guns.client.gui.components.refit;

import com.tacz.guns.api.item.IGun;
import com.tacz.guns.api.item.attachment.AttachmentType;
import com.tacz.guns.client.gui.GunRefitScreen;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;

import java.util.Locale;
import java.util.function.Consumer;

public class GunAttachmentSlot extends Button implements IStackTooltip {
    private final AttachmentType type;
    private final Inventory inventory;
    private final int gunItemIndex;
    private final String nameKey;
    private boolean selected = false;
    private ItemStack attachmentItem = ItemStack.EMPTY;

    public GunAttachmentSlot(int pX, int pY, AttachmentType type, int gunItemIndex, Inventory inventory, Button.OnPress onPress) {
        super(pX, pY, GunRefitScreen.SLOT_SIZE, GunRefitScreen.SLOT_SIZE, Component.empty(), onPress, Button.DEFAULT_NARRATION);
        this.type = type;
        this.inventory = inventory;
        this.gunItemIndex = gunItemIndex;
        this.nameKey = String.format("tooltip.tacz.attachment.%s", type.name().toLowerCase(Locale.US));
    }

    @Override
    public void renderTooltip(Consumer<ItemStack> consumer) {
        if (this.isHoveredOrFocused() && !attachmentItem.isEmpty()) {
            consumer.accept(attachmentItem);
        }
    }

    @Override
    protected void extractContents(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        if (this.isHoveredOrFocused()) {
            Font font = Minecraft.getInstance().font;
            int yOffset = this.getY() + 20;
            if (this.selected && !attachmentItem.isEmpty()) {
                yOffset = this.getY() + 30;
            }
            graphics.centeredText(font, Component.translatable(nameKey), this.getX() + this.getWidth() / 2, yOffset, ChatFormatting.WHITE.getColor());
        }

        ItemStack gunItem = inventory.getItem(gunItemIndex);
        IGun iGun = IGun.getIGunOrNull(gunItem);
        if (iGun == null) {
            return;
        }

        int x = this.getX();
        int y = this.getY();
        int bg = (isHoveredOrFocused() || selected) ? 0xAAFFFFFF : 0x66333333;
        graphics.fill(x, y, x + width, y + height, bg);

        this.attachmentItem = iGun.getAttachment(gunItem, type);
        if (!attachmentItem.isEmpty()) {
            graphics.item(attachmentItem, x + 1, y + 1);
        } else {
            int uOffset = GunRefitScreen.getSlotTextureXOffset(gunItem, type);
            if (uOffset >= 0) {
                graphics.blit(RenderPipelines.GUI_TEXTURED, GunRefitScreen.ICONS_TEXTURE, x + 1, y + 1,
                        uOffset, 0, 16, 16, GunRefitScreen.ICON_UV_SIZE, GunRefitScreen.ICON_UV_SIZE,
                        GunRefitScreen.getSlotsTextureWidth(), GunRefitScreen.ICON_UV_SIZE, 0x99FFFFFF);
            }
        }
    }

    public void setSelected(boolean selected) {
        this.selected = selected;
    }

    public AttachmentType getType() {
        return type;
    }

    public ItemStack getAttachmentItem() {
        ItemStack gunItem = inventory.getItem(gunItemIndex);
        IGun iGun = IGun.getIGunOrNull(gunItem);
        if (iGun == null) {
            return ItemStack.EMPTY;
        }
        return iGun.getAttachment(gunItem, type);
    }

    public boolean isAllow() {
        ItemStack gunItem = inventory.getItem(gunItemIndex);
        IGun iGun = IGun.getIGunOrNull(gunItem);
        if (iGun == null) {
            return false;
        }
        return iGun.allowAttachmentType(gunItem, type);
    }
}
