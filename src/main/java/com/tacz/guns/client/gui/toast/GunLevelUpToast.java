package com.tacz.guns.client.gui.toast;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.toasts.Toast;
import net.minecraft.client.gui.components.toasts.ToastManager;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.NotNull;

import javax.annotation.Nullable;

public class GunLevelUpToast implements Toast {
    private static final long DISPLAY_MS = 5000L;

    private final Component title;
    private final Component subTitle;
    private final ItemStack icon;
    private Visibility wantedVisibility = Visibility.SHOW;

    public GunLevelUpToast(ItemStack icon, Component titleComponent, @Nullable Component subtitle) {
        this.icon = icon;
        this.title = titleComponent;
        this.subTitle = subtitle;
    }

    @Override
    public void update(@NotNull ToastManager toastManager, long timeSinceLastVisible) {
        wantedVisibility = timeSinceLastVisible >= DISPLAY_MS ? Visibility.HIDE : Visibility.SHOW;
    }

    @Override
    public void extractRenderState(@NotNull GuiGraphicsExtractor gui, @NotNull Font font, long timeSinceLastVisible) {
        gui.item(icon, 8, 8);
        gui.text(font, title, 30, 7, 0xFFFFFF00, false);
        if (subTitle != null) {
            gui.text(font, subTitle, 30, 18, 0xFFFFFFFF, false);
        }
    }

    @NotNull
    @Override
    public Visibility getWantedVisibility() {
        return wantedVisibility;
    }
}
