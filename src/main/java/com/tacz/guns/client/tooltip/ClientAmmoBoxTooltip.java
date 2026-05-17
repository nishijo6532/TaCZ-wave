package com.tacz.guns.client.tooltip;

import com.tacz.guns.api.item.IAmmoBox;
import com.tacz.guns.inventory.tooltip.AmmoBoxTooltip;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.tooltip.ClientTooltipComponent;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;

public class ClientAmmoBoxTooltip implements ClientTooltipComponent {
    private final ItemStack ammo;
    private final Component count;
    private final Component ammoName;

    public ClientAmmoBoxTooltip(AmmoBoxTooltip tooltip) {
        this.ammo = tooltip.getAmmo();
        ItemStack ammoBox = tooltip.getAmmoBox();
        if (ammoBox.getItem() instanceof IAmmoBox box && box.isCreative(ammoBox)) {
            this.count = Component.literal("\u221E");
        } else {
            this.count = Component.translatable("tooltip.tacz.ammo_box.count", tooltip.getCount());
        }
        this.ammoName = this.ammo.getHoverName();
    }

    @Override
    public int getHeight(Font font) {
        return 28;
    }

    @Override
    public int getWidth(Font font) {
        return Math.max(font.width(ammoName), font.width(count)) + 22;
    }

    @Override
    public void extractText(GuiGraphicsExtractor gui, Font font, int pX, int pY) {
        gui.text(font, ammoName, pX + 20, pY + 4, 0xFFFFAA00, false);
        gui.text(font, count, pX + 20, pY + 15, 0xFF666666, false);
    }

    @Override
    public void extractImage(Font font, int pX, int pY, int width, int height, GuiGraphicsExtractor gui) {
        gui.item(ammo, pX, pY + 5);
    }
}
