package com.tacz.guns.client.event;

import com.tacz.guns.GunMod;
import com.tacz.guns.api.TimelessAPI;
import com.tacz.guns.client.animation.statemachine.GunAnimationConstant;
import com.tacz.guns.client.renderer.item.AnimateGeoItemRenderer;
import com.tacz.guns.util.ClientRenderCompat;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.listener.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(value = Dist.CLIENT, modid = GunMod.MOD_ID)
public class TickAnimationEvent {
    @SubscribeEvent
    public static void tickAnimation(TickEvent.ClientTickEvent.Pre event) {
        tickAnimation();
    }

    private static void tickAnimation() {
        LocalPlayer player = Minecraft.getInstance().player;
        if (player == null) {
            return;
        }
        ItemStack mainHandItem = player.getMainHandItem();
        TimelessAPI.getGunDisplay(mainHandItem).ifPresent(gunIndex -> {
            var animationStateMachine = gunIndex.getAnimationStateMachine();
            // 鄒､扈・恪蛻・ｸ也阜蟇ｼ閾ｴ逧・音谿・BUG 螟・炊・梧ｭ｣蟶ｸ諠・・荳堺ｼ夐∞蛻ｰ豁､髣ｮ鬚・
            if (player.input == null) {
                animationStateMachine.trigger(GunAnimationConstant.INPUT_IDLE);
                return;
            }
            if (!player.isMovingSlowly() && player.isSprinting()) {
                // 螯よ棡邇ｩ螳ｶ豁｣蝨ｨ遘ｻ蜉ｨ・梧眺謾ｾ遘ｻ蜉ｨ蜉ｨ逕ｻ・悟凄蛻呎眺謾ｾ idle 蜉ｨ逕ｻ
                animationStateMachine.trigger(GunAnimationConstant.INPUT_RUN);
            } else if (!player.isMovingSlowly() && player.input.getMoveVector().length() > 0.01) {
                animationStateMachine.trigger(GunAnimationConstant.INPUT_WALK);
            } else {
                animationStateMachine.trigger(GunAnimationConstant.INPUT_IDLE);
            }
        });
    }

    @SubscribeEvent
    public static void tickAnimation(TickEvent.RenderTickEvent.Pre event) {
        if (Minecraft.getInstance().options.getCameraType().isFirstPerson()) {
            return;
        }
        LocalPlayer player = Minecraft.getInstance().player;
        if (player == null) {
            return;
        }
        ItemStack mainHandItem = player.getMainHandItem();
        // 貂ｲ譟鍋嶌蜈ｳ蜀・ｮｹ謨ｴ逅・芦迚ｩ蜩∫噪IClientItemExtensions莠・ｼ瑚ｿ吩ｸｪ謗･蜿｣譛牙ｾ・ｿ帑ｸ豁･謚ｽ雎｡
        if (ClientRenderCompat.getCustomRenderer(mainHandItem) instanceof AnimateGeoItemRenderer<?, ?> renderer) {
            // 螯よ棡迚ｩ蜩∽ｸ堺ｸ譬ｷ莠・ｼ悟・蟆晁ｯ募・蟋句喧迥ｶ諤∵惻
            if (renderer.needReInit(mainHandItem)) {
                renderer.tryInit(mainHandItem, player, ClientRenderCompat.getPartialTicks());
            }
            renderer.visualUpdate(mainHandItem);
        }
    }
}

