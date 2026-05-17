package com.tacz.guns.client.input;

import com.mojang.blaze3d.platform.InputConstants;
import com.tacz.guns.GunMod;
import com.tacz.guns.api.TimelessAPI;
import com.tacz.guns.api.client.gameplay.IClientPlayerGunOperator;
import com.tacz.guns.api.entity.ShootResult;
import com.tacz.guns.api.item.IGun;
import com.tacz.guns.api.item.gun.FireMode;
import com.tacz.guns.client.gameplay.LocalPlayerSprint;
import com.tacz.guns.client.sound.SoundPlayManager;
import com.tacz.guns.util.KeyMappingCompat;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.resources.Identifier;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.InputEvent;
import net.minecraftforge.client.settings.KeyConflictContext;
import net.minecraftforge.client.settings.KeyModifier;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.listener.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.lwjgl.glfw.GLFW;

import static com.tacz.guns.util.InputExtraCheck.isInGame;

@Mod.EventBusSubscriber(value = Dist.CLIENT)
public class ShootKey {
    public static final KeyMapping SHOOT_KEY = KeyMappingCompat.create("key.tacz.shoot.desc",
            KeyConflictContext.IN_GAME,
            KeyModifier.NONE,
            InputConstants.Type.MOUSE,
            GLFW.GLFW_MOUSE_BUTTON_LEFT,
            "key.category.tacz");
    private static boolean lastTimeShootSuccess = false;
    private static Identifier lastShootGunId = null;
    private static long lastShootTraceLogAt = 0L;

    @SubscribeEvent
    public static void autoShoot(TickEvent.ClientTickEvent.Pre event) {
        autoShoot(false);
    }

    @SubscribeEvent
    public static void autoShoot(TickEvent.ClientTickEvent.Post event) {
        autoShoot(true);
    }

    private static void autoShoot(boolean isEndPhase) {
        if (!isInGame()) {
            return;
        }
        if (!isEndPhase) {
            return;
        }
        LocalPlayerSprint.stopSprint = false;

        Minecraft mc = Minecraft.getInstance();
        LocalPlayer player = mc.player;
        if (player == null || player.isSpectator()) {
            lastShootGunId = null;
            return;
        }
        ItemStack mainHandItem = player.getMainHandItem();
        if (mainHandItem.getItem() instanceof IGun iGun) {
            Identifier currentGunId = iGun.getGunId(mainHandItem);
            if (!currentGunId.equals(lastShootGunId)) {
                Identifier previousGunId = lastShootGunId;
                lastShootGunId = currentGunId;
                lastTimeShootSuccess = false;
                SoundPlayManager.resetDryFireSound();
                GunMod.LOGGER.debug("ShootTrace[client-input] gun switch: {} -> {}", previousGunId, currentGunId);
            }
            FireMode fireMode = iGun.getFireMode(mainHandItem);
            boolean isBurstAuto = fireMode == FireMode.BURST && TimelessAPI.getCommonGunIndex(iGun.getGunId(mainHandItem))
                    .map(index -> index.getGunData().getBurstData().isContinuousShoot())
                    .orElse(false);
            IClientPlayerGunOperator operator = IClientPlayerGunOperator.fromLocalPlayer(player);
            if (SHOOT_KEY.isDown()) {
                // 閭ｽ蠑轣ｫ譌ｶ遖∵ｭ｢蜀ｲ蛻ｺ
                LocalPlayerSprint.stopSprint = true;

                if (fireMode != FireMode.AUTO && !isBurstAuto && lastTimeShootSuccess) {
                    // 髱槫・閾ｪ蜉ｨ諠・・・檎ｦ∵ｭ｢霑樒ｻｭ蠑轣ｫ
                    return;
                }
                ShootResult result = operator.shoot();
                if (result == ShootResult.SUCCESS) {
                    lastTimeShootSuccess = true;
                } else {
                    long now = System.currentTimeMillis();
                    if (now - lastShootTraceLogAt >= 250L) {
                        lastShootTraceLogAt = now;
                        GunMod.LOGGER.debug("ShootTrace[client-input] shoot denied: gunId={}, fireMode={}, result={}, lastSuccess={}",
                                currentGunId, fireMode, result, lastTimeShootSuccess);
                    }
                }
            } else {
                lastTimeShootSuccess = false;
                SoundPlayManager.resetDryFireSound();
            }
        } else {
            lastShootGunId = null;
            lastTimeShootSuccess = false;
        }
    }

    public static boolean autoShootController() {
        if (!isInGame()) {
            return false;
        }
        Minecraft mc = Minecraft.getInstance();
        LocalPlayer player = mc.player;
        if (player == null || player.isSpectator()) {
            return false;
        }
        ItemStack mainHandItem = player.getMainHandItem();
        if (mainHandItem.getItem() instanceof IGun iGun) {
            FireMode fireMode = iGun.getFireMode(mainHandItem);
            boolean isBurstAuto = fireMode == FireMode.BURST && TimelessAPI.getCommonGunIndex(iGun.getGunId(mainHandItem))
                    .map(index -> index.getGunData().getBurstData().isContinuousShoot())
                    .orElse(false);
            IClientPlayerGunOperator operator = IClientPlayerGunOperator.fromLocalPlayer(player);
            if (fireMode == FireMode.AUTO || isBurstAuto) {
                return operator.shoot() == ShootResult.SUCCESS;
            }
        }
        return false;
    }

    @SubscribeEvent
    public static void semiShoot(InputEvent.MouseButton.Post event) {
        if (isInGame() && KeyMappingCompat.matchesMouse(SHOOT_KEY, event)) {
            // 譚ｾ蠑鮠譬・ｼ碁㍾鄂ｮ DryFire 迥ｶ諤・
            if (event.getAction() == GLFW.GLFW_RELEASE) {
                SoundPlayManager.resetDryFireSound();
                return;
            }
            Minecraft mc = Minecraft.getInstance();
            LocalPlayer player = mc.player;
            if (player == null || player.isSpectator()) {
                return;
            }
            ItemStack mainHandItem = player.getMainHandItem();
            if (mainHandItem.getItem() instanceof IGun iGun) {
                FireMode fireMode = iGun.getFireMode(mainHandItem);
                boolean isBurstSemi = fireMode == FireMode.BURST && TimelessAPI.getCommonGunIndex(iGun.getGunId(mainHandItem))
                        .map(index -> !index.getGunData().getBurstData().isContinuousShoot())
                        .orElse(false);
                if (fireMode == FireMode.UNKNOWN) {
                    player.sendSystemMessage(Component.translatable("message.tacz.fire_select.fail"));
                }
                if (fireMode == FireMode.SEMI || isBurstSemi) {
                    lastTimeShootSuccess = IClientPlayerGunOperator.fromLocalPlayer(player).shoot() == ShootResult.SUCCESS;
                }
            }
        }
    }

    public static boolean semiShootController(boolean isPress) {
        if (!isInGame()) {
            return false;
        }
        // 譚ｾ蠑鮠譬・ｼ碁㍾鄂ｮ DryFire 迥ｶ諤・
        if (!isPress) {
            SoundPlayManager.resetDryFireSound();
            return false;
        }
        Minecraft mc = Minecraft.getInstance();
        LocalPlayer player = mc.player;
        if (player == null || player.isSpectator()) {
            return false;
        }
        ItemStack mainHandItem = player.getMainHandItem();
        if (mainHandItem.getItem() instanceof IGun iGun) {
            FireMode fireMode = iGun.getFireMode(mainHandItem);
            boolean isBurstSemi = fireMode == FireMode.BURST && TimelessAPI.getCommonGunIndex(iGun.getGunId(mainHandItem))
                    .map(index -> !index.getGunData().getBurstData().isContinuousShoot())
                    .orElse(false);
            if (fireMode == FireMode.UNKNOWN) {
                player.sendSystemMessage(Component.translatable("message.tacz.fire_select.fail"));
                return false;
            }
            if (fireMode == FireMode.SEMI || isBurstSemi) {
                return IClientPlayerGunOperator.fromLocalPlayer(player).shoot() == ShootResult.SUCCESS;
            }
        }
        return false;
    }
}

