package com.tacz.guns.client.input;

import com.mojang.blaze3d.platform.InputConstants;
import com.tacz.guns.api.TimelessAPI;
import com.tacz.guns.api.client.gameplay.IClientPlayerGunOperator;
import com.tacz.guns.api.item.IGun;
import com.tacz.guns.config.client.KeyConfig;
import com.tacz.guns.resource.pojo.data.gun.Bolt;
import com.tacz.guns.util.KeyMappingCompat;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.InputEvent;
import net.minecraftforge.client.settings.KeyConflictContext;
import net.minecraftforge.client.settings.KeyModifier;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.listener.SubscribeEvent;
import net.minecraftforge.fml.LogicalSide;
import net.minecraftforge.fml.common.Mod;
import org.lwjgl.glfw.GLFW;

import static com.tacz.guns.util.InputExtraCheck.isInGame;

@Mod.EventBusSubscriber(value = Dist.CLIENT)
public class ReloadKey {
    public static final KeyMapping RELOAD_KEY = KeyMappingCompat.create("key.tacz.reload.desc",
            KeyConflictContext.IN_GAME,
            KeyModifier.NONE,
            InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_R,
            "key.category.tacz");

    @SubscribeEvent
    public static void onReloadPress(InputEvent.Key event) {
        if (isInGame() && event.getAction() == GLFW.GLFW_PRESS && KeyMappingCompat.matches(RELOAD_KEY, event)) {
            LocalPlayer player = Minecraft.getInstance().player;
            if (player == null || player.isSpectator()) {
                return;
            }
            if (player.getMainHandItem().getItem() instanceof IGun iGun) {
                // 螯よ棡菴ｿ逕ｨ閭悟桁逶ｴ隸ｻ・御ｸ疲ｲ｡譛画困蠑ｹ蜀ｷ蜊ｴ譛ｺ蛻ｶ・悟・蝨ｨ霎灘・譌ｶ蟆ｱ螻剰反謐｢蠑ｹ
                if (iGun.useInventoryAmmo(player.getMainHandItem())) {
                    return;
                }
                IClientPlayerGunOperator.fromLocalPlayer(player).reload();
            }
        }
    }

    public static boolean onReloadControllerPress(boolean isPress) {
        if (isInGame() && isPress) {
            LocalPlayer player = Minecraft.getInstance().player;
            if (player == null || player.isSpectator()) {
                return false;
            }
            if (IGun.mainHandHoldGun(player)) {
                IClientPlayerGunOperator.fromLocalPlayer(player).reload();
                return true;
            }
        }
        return false;
    }

    @SubscribeEvent
    public static void autoReload(TickEvent.PlayerTickEvent.Pre event) {
        if (event.side() != LogicalSide.CLIENT) {
            return;
        }

        if (!KeyConfig.AUTO_RELOAD.get()) {
            return;
        }

        LocalPlayer player = Minecraft.getInstance().player;
        if (player == null || player.isSpectator() || player.tickCount % 5 != 0) {
            return;
        }
        ItemStack currentGunItem = player.getMainHandItem();
        if (player.getMainHandItem().getItem() instanceof IGun iGun) {
            // 螯よ棡菴ｿ逕ｨ閭悟桁逶ｴ隸ｻ・御ｸ疲ｲ｡譛画困蠑ｹ蜀ｷ蜊ｴ譛ｺ蛻ｶ・悟・蝨ｨ霎灘・譌ｶ蟆ｱ螻剰反謐｢蠑ｹ
            if (iGun.useInventoryAmmo(player.getMainHandItem())) {
                return;
            }
            boolean flag = TimelessAPI.getCommonGunIndex(iGun.getGunId(currentGunItem))
                    .map(gunIndex -> gunIndex.getGunData().getBolt() != Bolt.OPEN_BOLT)
                    .orElse(false);

            int ammoCount = iGun.getCurrentAmmoCount(currentGunItem) + (iGun.hasBulletInBarrel(currentGunItem) && flag ? 1 : 0);
            if (ammoCount > 0) {
                return;
            }
            IClientPlayerGunOperator.fromLocalPlayer(player).reload();
        }
    }
}

