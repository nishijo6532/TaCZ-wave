package com.tacz.guns.client.gui.overlay;

import com.tacz.guns.api.client.gameplay.IClientPlayerGunOperator;
import com.tacz.guns.api.item.IGun;
import com.tacz.guns.config.client.RenderConfig;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.client.gui.overlay.ForgeLayer;

public class KillAmountOverlay implements ForgeLayer {
    private static long killTimestamp = -1L;
    private static int killAmount = 0;

    @Override
    public void extract(GuiGraphicsExtractor graphics, DeltaTracker deltaTracker) {
        if (!RenderConfig.KILL_AMOUNT_ENABLE.get()) {
            return;
        }

        int timeout = (int) (RenderConfig.KILL_AMOUNT_DURATION_SECOND.get() * 1000);
        long remainTime = System.currentTimeMillis() - killTimestamp;
        if (remainTime > timeout) {
            return;
        }

        Minecraft mc = Minecraft.getInstance();
        LocalPlayer player = mc.player;
        if (!(player instanceof IClientPlayerGunOperator)) {
            return;
        }
        ItemStack stack = player.getMainHandItem();
        if (!(stack.getItem() instanceof IGun)) {
            return;
        }

        String text = killAmount < 10 ? "\u2620 x 0" + killAmount : "\u2620 x " + killAmount;
        int fontWidth = mc.font.width(text);
        double fadeOutTime = timeout / 3.0 * 2;
        float hue = (1 - Math.min((killAmount / 30f), 1)) * 0.15f;
        int alpha = 0xFF;
        if (remainTime > fadeOutTime) {
            alpha = 0xFF - (int) ((remainTime - fadeOutTime) / (timeout - fadeOutTime) * 0xF0);
        }
        int color = Mth.hsvToRgb(hue, 0.75f, 1) + (alpha << 24);

        int width = mc.getWindow().getGuiScaledWidth();
        int height = mc.getWindow().getGuiScaledHeight();
        graphics.text(mc.font, text, width / 2 - fontWidth / 2, height - 45, color, false);
    }

    public static void markTimestamp() {
        int timeout = (int) (RenderConfig.KILL_AMOUNT_DURATION_SECOND.get() * 1000);
        if (System.currentTimeMillis() - killTimestamp > timeout) {
            killAmount = 0;
        }
        killTimestamp = System.currentTimeMillis();
        killAmount += 1;
    }
}
