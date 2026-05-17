package com.tacz.guns.client.input;

import com.mojang.blaze3d.platform.InputConstants;
import com.tacz.guns.api.client.gameplay.IClientPlayerGunOperator;
import com.tacz.guns.api.item.IGun;
import com.tacz.guns.config.client.KeyConfig;
import com.tacz.guns.util.InputExtraCheck;
import com.tacz.guns.util.KeyMappingCompat;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
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
public class AimKey {
    public static final KeyMapping AIM_KEY = KeyMappingCompat.create("key.tacz.aim.desc",
            KeyConflictContext.IN_GAME,
            KeyModifier.NONE,
            InputConstants.Type.MOUSE,
            GLFW.GLFW_MOUSE_BUTTON_RIGHT,
            "key.category.tacz");

    @SubscribeEvent
    public static void onAimPress(InputEvent.MouseButton.Post event) {
        if (isInGame() && KeyMappingCompat.matchesMouse(AIM_KEY, event)) {
            LocalPlayer player = Minecraft.getInstance().player;
            if (player == null || player.isSpectator()) {
                return;
            }
            if (!(player instanceof IClientPlayerGunOperator operator)) {
                return;
            }
            if (IGun.mainHandHoldGun(player)) {
                boolean action = true;
                if (!KeyConfig.HOLD_TO_AIM.get()) {
                    action = !operator.isAim();
                }
                if (event.getAction() == GLFW.GLFW_PRESS) {
                    IClientPlayerGunOperator.fromLocalPlayer(player).aim(action);
                }
                if (KeyConfig.HOLD_TO_AIM.get() && event.getAction() == GLFW.GLFW_RELEASE) {
                    IClientPlayerGunOperator.fromLocalPlayer(player).aim(false);
                }
            }
        }
    }

    /**
     * 隸･逶大成蝎ｨ閭ｽ豁｣遑ｮ螟・炊 謖我ｽ冗桷蜃・ｨ｡蠑・荳狗噪
     * 1.鬚・ｾ灘・・亥・蝙具ｼ壽潔菴冗桷蜃・・謐｢豁ｦ蝎ｨ蜷趣ｼ瑚・菫晄戟迸・㊥迥ｶ諤・ｼ・
     * 2.髞ｮ逶俶潔髞ｮ霎灘・
     * 
     * 蟒ｺ隶ｮ蟆・潔荳句・謐｢迸・㊥荵滓髪謖・髞ｮ逶俶潔髞ｮ霎灘・
     * */
    @SubscribeEvent
    public static void onAimHoldingPreInput(TickEvent.ClientTickEvent.Pre event) {
        onAimHoldingPreInput();
    }

    @SubscribeEvent
    public static void onAimHoldingPreInput(TickEvent.ClientTickEvent.Post event) {
        onAimHoldingPreInput();
    }

    private static void onAimHoldingPreInput() {
        if (!KeyConfig.HOLD_TO_AIM.get()) {
            return;
        }
        Minecraft mc = Minecraft.getInstance();
        boolean press = AimKey.AIM_KEY.isDown();
        if (InputExtraCheck.isInGame()) {
            LocalPlayer player = mc.player;
            if (player == null || player.isSpectator()) {
                return;
            }
            if (!(player instanceof IClientPlayerGunOperator operator)) {
                return;
            }
            if (operator.isAim() && press) {
                return;
            }
            if (!operator.isAim()) {
                if (!press) {
                    return;
                }
            }
            if (IGun.mainHandHoldGun(player)) {
                IClientPlayerGunOperator.fromLocalPlayer(player).aim(press);
            }
        }
    }

    public static boolean onAimControllerPress(boolean isPress) {
        if (!isInGame()) {
            return false;
        }
        LocalPlayer player = Minecraft.getInstance().player;
        if (player == null || player.isSpectator()) {
            return false;
        }
        if (!(player instanceof IClientPlayerGunOperator operator)) {
            return false;
        }
        if (!IGun.mainHandHoldGun(player)) {
            return false;
        }
        boolean action = true;
        if (!KeyConfig.HOLD_TO_AIM.get()) {
            action = !operator.isAim();
        }
        if (isPress) {
            IClientPlayerGunOperator.fromLocalPlayer(player).aim(action);
            return true;
        }
        if (KeyConfig.HOLD_TO_AIM.get()) {
            IClientPlayerGunOperator.fromLocalPlayer(player).aim(false);
            return true;
        }
        return false;
    }

    @SubscribeEvent
    public static void cancelAim(TickEvent.ClientTickEvent.Post event) {
        Minecraft mc = Minecraft.getInstance();
        LocalPlayer player = mc.player;
        if (!(player instanceof IClientPlayerGunOperator operator)) {
            return;
        }
        if (operator.isAim() && (!isInGame() || player.isSpectator())) {
            IClientPlayerGunOperator.fromLocalPlayer(player).aim(false);
        }
    }
}

