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
import com.tacz.guns.entity.shooter.LivingEntityShoot;
import com.tacz.guns.network.NetworkHandler;
import com.tacz.guns.network.message.ClientMessagePlayerAutoShoot;
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
    private static boolean autoShootSent = false;
    private static boolean autoShootInitialShotPending = false;
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
            stopServerAutoShoot();
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
            stopServerAutoShoot();
            return;
        }
        ItemStack mainHandItem = player.getMainHandItem();
        if (mainHandItem.getItem() instanceof IGun iGun) {
            Identifier currentGunId = iGun.getGunId(mainHandItem);
            if (!currentGunId.equals(lastShootGunId)) {
                Identifier previousGunId = lastShootGunId;
                lastShootGunId = currentGunId;
                lastTimeShootSuccess = false;
                stopServerAutoShoot();
                SoundPlayManager.resetDryFireSound();
                GunMod.LOGGER.debug("ShootTrace[client-input] gun switch: {} -> {}", previousGunId, currentGunId);
            }
            FireMode fireMode = iGun.getFireMode(mainHandItem);
            boolean isAutoMode = LivingEntityShoot.isAutoShootMode(fireMode, iGun, mainHandItem);
            boolean useServerAuto = shouldUseServerAuto(iGun, mainHandItem, fireMode);
            IClientPlayerGunOperator operator = IClientPlayerGunOperator.fromLocalPlayer(player);
            boolean isShootDown = SHOOT_KEY.isDown();
            if (operator.chargeShoot(isShootDown)) {
                // 髢ｭ・ｽ陟題ｽ｣・ｫ隴鯉ｽｶ驕問扱・ｭ・｢陷・ｲ陋ｻ・ｺ
                LocalPlayerSprint.stopSprint = true;

                if (!isAutoMode && lastTimeShootSuccess) {
                    // 鬮ｱ讒ｫ繝ｻ髢ｾ・ｪ陷会ｽｨ隲繝ｻ繝ｻ繝ｻ讙趣ｽｦ竏ｵ・ｭ・｢髴第ｨ抵ｽｻ・ｭ陟題ｽ｣・ｫ
                    return;
                }
                if (useServerAuto && !autoShootSent) {
                    autoShootInitialShotPending = true;
                }
                ShootResult result = operator.shoot();
                if (result == ShootResult.SUCCESS) {
                    lastTimeShootSuccess = true;
                    if (useServerAuto) {
                        startServerAutoShoot();
                    }
                } else {
                    autoShootInitialShotPending = false;
                    long now = System.currentTimeMillis();
                    if (now - lastShootTraceLogAt >= 250L) {
                        lastShootTraceLogAt = now;
                        GunMod.LOGGER.debug("ShootTrace[client-input] shoot denied: gunId={}, fireMode={}, result={}, lastSuccess={}",
                                currentGunId, fireMode, result, lastTimeShootSuccess);
                    }
                }
            }
            if (isShootDown) {
                LocalPlayerSprint.stopSprint = true;
            } else {
                lastTimeShootSuccess = false;
                stopServerAutoShoot();
                SoundPlayManager.resetDryFireSound();
            }
        } else {
            lastShootGunId = null;
            lastTimeShootSuccess = false;
            stopServerAutoShoot();
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
            IClientPlayerGunOperator operator = IClientPlayerGunOperator.fromLocalPlayer(player);
            boolean useServerAuto = shouldUseServerAuto(iGun, mainHandItem, fireMode);
            if (!LivingEntityShoot.isAutoShootMode(fireMode, iGun, mainHandItem)) {
                return false;
            }
            if (useServerAuto) {
                if (!autoShootSent) {
                    autoShootInitialShotPending = true;
                }
                boolean success = operator.shoot() == ShootResult.SUCCESS;
                if (success) {
                    startServerAutoShoot();
                } else {
                    autoShootInitialShotPending = false;
                }
                return success;
            }
            return operator.shoot() == ShootResult.SUCCESS;
        }
        return false;
    }

    private static boolean hasChargeData(IGun gun, ItemStack gunItem, FireMode fireMode) {
        return TimelessAPI.getClientGunIndex(gun.getGunId(gunItem))
                .map(index -> index.getGunData().getChargeData(fireMode) != null)
                .orElse(false);
    }

    private static boolean shouldUseServerAuto(IGun gun, ItemStack gunItem, FireMode fireMode) {
        if (!LivingEntityShoot.isAutoShootMode(fireMode, gun, gunItem)) {
            return false;
        }
        if (hasChargeData(gun, gunItem, fireMode)) {
            return false;
        }
        return false;
    }

    private static void startServerAutoShoot() {
        if (!autoShootSent) {
            NetworkHandler.sendToServer(new ClientMessagePlayerAutoShoot(true));
            autoShootSent = true;
        }
    }

    private static void stopServerAutoShoot() {
        if (autoShootSent) {
            NetworkHandler.sendToServer(new ClientMessagePlayerAutoShoot(false));
            autoShootSent = false;
        }
        autoShootInitialShotPending = false;
    }

    public static boolean consumeAutoShootInitialShotPending() {
        boolean pending = autoShootInitialShotPending;
        autoShootInitialShotPending = false;
        return pending;
    }

    @SubscribeEvent
    public static void semiShoot(InputEvent.MouseButton.Post event) {
        if (isInGame() && KeyMappingCompat.matchesMouse(SHOOT_KEY, event)) {
            // Reset dry-fire sound when the shoot key is released.
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
        // Reset dry-fire sound when the shoot key is released.
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

