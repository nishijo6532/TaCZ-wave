package com.tacz.guns.client.event;

import com.mojang.blaze3d.platform.Window;
import com.tacz.guns.GunMod;
import com.tacz.guns.api.TimelessAPI;
import com.tacz.guns.api.client.animation.statemachine.AnimationStateContext;
import com.tacz.guns.api.client.animation.statemachine.AnimationStateMachine;
import com.tacz.guns.api.client.gameplay.IClientPlayerGunOperator;
import com.tacz.guns.api.entity.IGunOperator;
import com.tacz.guns.api.entity.ReloadState;
import com.tacz.guns.api.item.IGun;
import com.tacz.guns.client.gui.GunRefitScreen;
import com.tacz.guns.client.renderer.crosshair.CrosshairType;
import com.tacz.guns.compat.shouldersurfing.ShoulderSurfingCompat;
import com.tacz.guns.config.client.RenderConfig;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Options;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.multiplayer.MultiPlayerGameMode;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameType;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.listener.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(value = Dist.CLIENT, modid = GunMod.MOD_ID)
public class RenderCrosshairEvent {
    private static final Identifier HIT_ICON = com.tacz.guns.util.IdHelper.id(GunMod.MOD_ID, "textures/crosshair/hit/hit_marker.png");
    private static final long KEEP_TIME = 300;

    private static boolean isRefitScreen = false;
    private static long hitTimestamp = -1L;
    private static long killTimestamp = -1L;
    private static long headShotTimestamp = -1L;

    private RenderCrosshairEvent() {
    }

    public static boolean shouldHideVanillaCrosshair() {
        LocalPlayer player = Minecraft.getInstance().player;
        return player != null && IGun.mainHandHoldGun(player);
    }

    public static void extractCrosshairLayer(GuiGraphicsExtractor graphics, float partialTick) {
        LocalPlayer player = Minecraft.getInstance().player;
        if (player == null || !IGun.mainHandHoldGun(player)) {
            return;
        }

        Window window = Minecraft.getInstance().getWindow();
        renderHitMarker(graphics, window);

        ReloadState reloadState = IGunOperator.fromLivingEntity(player).getSynReloadState();
        if (reloadState.getStateType().isReloading() || isRefitScreen) {
            return;
        }

        ItemStack stack = player.getMainHandItem();
        if (!(stack.getItem() instanceof IGun)) {
            return;
        }

        IClientPlayerGunOperator playerGunOperator = IClientPlayerGunOperator.fromLocalPlayer(player);
        TimelessAPI.getGunDisplay(stack).ifPresent(gunIndex -> {
            if (playerGunOperator.getClientAimingProgress(partialTick) > 0.1F) {
                boolean forceShow = gunIndex.isShowCrosshair();
                boolean shoulderSurfingForceShow = ShoulderSurfingCompat.showCrosshair();
                if (!forceShow && !shoulderSurfingForceShow) {
                    return;
                }
            }

            AnimationStateMachine<?> animationStateMachine = gunIndex.getAnimationStateMachine();
            AnimationStateContext context = animationStateMachine.getContext();
            if (context == null || !context.shouldHideCrossHair()) {
                renderCrosshair(graphics, window);
            }
        });
    }

    @SubscribeEvent
    public static void onRenderTick(TickEvent.RenderTickEvent.Post event) {
        isRefitScreen = Minecraft.getInstance().screen instanceof GunRefitScreen;
    }

    private static void renderCrosshair(GuiGraphicsExtractor graphics, Window window) {
        Options options = Minecraft.getInstance().options;
        boolean shoulderSurfingForceShow = ShoulderSurfingCompat.showCrosshair();
        if (!options.getCameraType().isFirstPerson() && !shoulderSurfingForceShow) {
            return;
        }
        if (options.hideGui) {
            return;
        }
        MultiPlayerGameMode gameMode = Minecraft.getInstance().gameMode;
        if (gameMode == null || gameMode.getPlayerMode() == GameType.SPECTATOR) {
            return;
        }

        int width = window.getGuiScaledWidth();
        int height = window.getGuiScaledHeight();
        Identifier location = CrosshairType.getTextureLocation(RenderConfig.CROSSHAIR_TYPE.get());
        float x = width / 2f - 8;
        float y = height / 2f - 8;
        graphics.blit(RenderPipelines.GUI_TEXTURED, location, (int) x, (int) y, 0, 0, 16, 16, 16, 16, 0xE6FFFFFF);
    }

    private static void renderHitMarker(GuiGraphicsExtractor graphics, Window window) {
        long remainHitTime = System.currentTimeMillis() - hitTimestamp;
        long remainKillTime = System.currentTimeMillis() - killTimestamp;
        long remainHeadShotTime = System.currentTimeMillis() - headShotTimestamp;
        float offset = RenderConfig.HIT_MARKET_START_POSITION.get().floatValue();
        float fadeTime;

        if (remainKillTime > KEEP_TIME) {
            if (remainHitTime > KEEP_TIME) {
                return;
            }
            fadeTime = remainHitTime;
        } else {
            offset += (remainKillTime * 4f) / KEEP_TIME;
            fadeTime = remainKillTime;
        }

        int width = window.getGuiScaledWidth();
        int height = window.getGuiScaledHeight();
        float x = width / 2f - 8;
        float y = height / 2f - 8;
        int markerColor = markerColor(remainHeadShotTime, fadeTime);

        graphics.blit(RenderPipelines.GUI_TEXTURED, HIT_ICON, (int) (x - offset), (int) (y - offset), 0, 0, 8, 8, 16, 16, markerColor);
        graphics.blit(RenderPipelines.GUI_TEXTURED, HIT_ICON, (int) (x + 8 + offset), (int) (y - offset), 8, 0, 8, 8, 16, 16, markerColor);
        graphics.blit(RenderPipelines.GUI_TEXTURED, HIT_ICON, (int) (x - offset), (int) (y + 8 + offset), 0, 8, 8, 8, 16, 16, markerColor);
        graphics.blit(RenderPipelines.GUI_TEXTURED, HIT_ICON, (int) (x + 8 + offset), (int) (y + 8 + offset), 8, 8, 8, 8, 16, 16, markerColor);
    }

    private static int markerColor(long remainHeadShotTime, float fadeTime) {
        int alpha = Math.max(0, Math.min(255, (int) ((1 - fadeTime / KEEP_TIME) * 255)));
        int rgb = remainHeadShotTime > KEEP_TIME ? 0xFFFFFF : 0xFF0000;
        return alpha << 24 | rgb;
    }

    public static void markHitTimestamp() {
        hitTimestamp = System.currentTimeMillis();
    }

    public static void markKillTimestamp() {
        long now = System.currentTimeMillis();
        hitTimestamp = now;
        killTimestamp = now;
    }

    public static void markHeadShotTimestamp() {
        headShotTimestamp = System.currentTimeMillis();
    }
}
