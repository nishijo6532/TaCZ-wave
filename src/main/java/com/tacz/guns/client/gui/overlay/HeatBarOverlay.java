package com.tacz.guns.client.gui.overlay;

import com.tacz.guns.api.TimelessAPI;
import com.tacz.guns.api.item.IGun;
import com.tacz.guns.client.resource.index.ClientGunIndex;
import com.tacz.guns.config.client.RenderConfig;
import com.tacz.guns.resource.pojo.data.gun.GunData;
import com.tacz.guns.resource.pojo.data.gun.GunHeatData;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.client.gui.overlay.ForgeLayer;

import java.text.DecimalFormat;

public class HeatBarOverlay implements ForgeLayer {
    private static final Identifier HEATBASE = com.tacz.guns.util.IdHelper.id("tacz", "textures/hud/heat_base.png");
    private static final DecimalFormat HEAT_FORMAT_PERCENT = new DecimalFormat("0.0%");

    @Override
    public void extract(GuiGraphicsExtractor graphics, DeltaTracker deltaTracker) {
        if (!RenderConfig.GUN_HUD_ENABLE.get()) {
            return;
        }

        Minecraft mc = Minecraft.getInstance();
        LocalPlayer player = mc.player;
        if (player == null) {
            return;
        }
        ItemStack stack = player.getMainHandItem();
        if (!(stack.getItem() instanceof IGun iGun) || !iGun.hasHeatData(stack)) {
            return;
        }

        Identifier gunId = iGun.getGunId(stack);
        GunData gunData = TimelessAPI.getClientGunIndex(gunId).map(ClientGunIndex::getGunData).orElse(null);
        if (gunData == null || gunData.getHeatData() == null) {
            return;
        }

        GunHeatData heatData = gunData.getHeatData();
        float percent = iGun.getHeatAmount(stack) / heatData.getHeatMax();
        percent = Math.max(0f, Math.min(1f, percent));
        boolean locked = iGun.isOverheatLocked(stack);

        int width = mc.getWindow().getGuiScaledWidth();
        int height = mc.getWindow().getGuiScaledHeight();
        int tick = player.tickCount;

        int barColor = getHeatColor(percent, locked, tick);
        graphics.fill(width / 2 - 30, height / 2 + 30, width / 2 - 30 + (int) (percent * 60), height / 2 + 34, barColor);
        graphics.blit(HEATBASE, width / 2 - 64, height / 2 - 44, 0, 0, 128, 128, 128, 128);

        Font font = mc.font;
        String percentString = locked ? "!OVERHEAT!" : HEAT_FORMAT_PERCENT.format(percent);
        int color = locked ? (tick % 20 < 10 ? 0xFFFF0000 : 0xFFFFFF00) : 0xFFFFFFFF;
        graphics.text(font, percentString, width / 2 - (font.width(percentString) / 2), height / 2 + 38, color, true);
    }

    private static int getHeatColor(float percent, boolean locked, int tick) {
        if (locked) {
            return tick % 20 < 10 ? 0x9FFF0000 : 0x9FFFFF00;
        }
        if (percent < 0.4f) {
            return 0x9FFFFFFF;
        }
        if (percent <= 0.65f) {
            float t = (percent - 0.4f) / 0.25f;
            return lerpArgb(0x9FFFFFFF, 0x9FFFFF00, t);
        }
        float t = (percent - 0.65f) / 0.35f;
        return lerpArgb(0x9FFFFF00, 0x9FFF0000, t);
    }

    private static int lerpArgb(int a, int b, float t) {
        t = Math.max(0f, Math.min(1f, t));
        int aA = (a >>> 24) & 0xFF;
        int aR = (a >>> 16) & 0xFF;
        int aG = (a >>> 8) & 0xFF;
        int aB = a & 0xFF;
        int bA = (b >>> 24) & 0xFF;
        int bR = (b >>> 16) & 0xFF;
        int bG = (b >>> 8) & 0xFF;
        int bB = b & 0xFF;
        int oA = (int) (aA + (bA - aA) * t);
        int oR = (int) (aR + (bR - aR) * t);
        int oG = (int) (aG + (bG - aG) * t);
        int oB = (int) (aB + (bB - aB) * t);
        return (oA << 24) | (oR << 16) | (oG << 8) | oB;
    }
}