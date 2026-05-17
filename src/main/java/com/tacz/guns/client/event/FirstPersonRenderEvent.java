package com.tacz.guns.client.event;

import com.tacz.guns.GunMod;
import com.tacz.guns.api.client.animation.statemachine.AnimationStateMachine;
import com.tacz.guns.api.client.other.KeepingItemRenderer;
import com.tacz.guns.api.item.IGun;
import com.tacz.guns.client.renderer.item.AnimateGeoItemRenderer;
import com.tacz.guns.compat.oculus.OculusCompat;
import com.tacz.guns.util.ClientRenderCompat;
import com.tacz.guns.util.RenderHelper;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderHandEvent;
import net.minecraftforge.eventbus.api.listener.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(value = Dist.CLIENT, modid = GunMod.MOD_ID)
public class FirstPersonRenderEvent {
    private static AnimationStateMachine<?> lastStateMachine = null;

    @SubscribeEvent
    public static boolean onRenderHand(RenderHandEvent event) {
        LocalPlayer player = Minecraft.getInstance().player;
        if (player == null) {
            return false;
        }
        if (event.getHand() == InteractionHand.OFF_HAND) {
            ItemStack stack = KeepingItemRenderer.getRenderer().getCurrentItem();
            if (stack.getItem() instanceof IGun) {
                return true;
            }
            return false;
        }
        // 莠倶ｻｶ莠倶ｻｶ扈咏噪譏ｯ陲ｫ蟒ｶ髟ｿ貂ｲ譟謎ｿｮ謾ｹ霑・錘逧・黄蜩・ｼ御ｸ肴弍邇ｩ螳ｶ螳樣刔謇区戟逧・
        ItemStack stack = event.getItemStack();

        // 闔ｷ蜿・TransformType
        ItemDisplayContext transformType;
        if (event.getHand() == InteractionHand.MAIN_HAND) {
            transformType = ItemDisplayContext.FIRST_PERSON_RIGHT_HAND;
        } else {
            transformType = ItemDisplayContext.FIRST_PERSON_LEFT_HAND;
        }

        // 貂ｲ譟鍋嶌蜈ｳ蜀・ｮｹ謨ｴ逅・芦迚ｩ蜩∫噪IClientItemExtensions莠・ｼ瑚ｿ吩ｸｪ謗･蜿｣譛牙ｾ・ｿ帑ｸ豁･謚ｽ雎｡
        if (ClientRenderCompat.getCustomRenderer(stack) instanceof AnimateGeoItemRenderer<?, ?> renderer) {
            // 螯よ棡譌ｧ逧・憾諤∵惻蟾ｲ扈丈ｸ榊・菴ｿ逕ｨ荳疲悴豁｣蟶ｸ騾蜃ｺ・御ｽｿ蜈ｶ髱咎ｻ倬蜃ｺ
            AnimationStateMachine<?> machine = renderer.getStateMachine(stack);
            if (machine != lastStateMachine) {
                if (lastStateMachine != null && lastStateMachine.isInitialized()) {
                    lastStateMachine.exit();
                }
                lastStateMachine = machine;
            }
            // 迚ｩ蜩∝､・ｺ主錘蜿ｰ譌ｶ・碁仆豁｢迥ｶ諤∵惻蛻晏ｧ句喧
            boolean flag = ItemStack.matches(player.getMainHandItem(), stack);
            if (flag && renderer.needReInit(stack)) {
                renderer.tryInit(stack, player, event.getPartialTick());
            }

			// 髦ｲ豁｢蜀・ｭ俶ｳ・ｼ・
			var bufferSource = Minecraft.getInstance().renderBuffers().bufferSource();
			OculusCompat.endBatch(bufferSource);

            RenderHelper.setFirstPersonArmNodeCollector(event.getNodeCollector());
            try {
                renderer.renderFirstPerson(player, stack, transformType, event.getPoseStack(), bufferSource,
                        event.getPackedLight(), event.getPartialTick());
            } finally {
                RenderHelper.clearFirstPersonArmNodeCollector();
            }
            return true;
        }
        return false;
    }
}

