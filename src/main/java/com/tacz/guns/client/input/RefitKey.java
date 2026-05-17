package com.tacz.guns.client.input;

import com.mojang.blaze3d.platform.InputConstants;
import com.tacz.guns.api.item.IGun;
import com.tacz.guns.client.gui.GunRefitScreen;
import com.tacz.guns.util.KeyMappingCompat;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.InputEvent;
import net.minecraftforge.client.settings.KeyConflictContext;
import net.minecraftforge.client.settings.KeyModifier;
import net.minecraftforge.eventbus.api.listener.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.lwjgl.glfw.GLFW;

import static com.tacz.guns.util.InputExtraCheck.isInGame;

@Mod.EventBusSubscriber(value = Dist.CLIENT)
public class RefitKey {
    public static final KeyMapping REFIT_KEY = KeyMappingCompat.create("key.tacz.refit.desc",
            KeyConflictContext.IN_GAME,
            KeyModifier.NONE,
            InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_Z,
            "key.category.tacz");

    @SubscribeEvent
    public static void onRefitPress(InputEvent.Key event) {
        if (event.getAction() == GLFW.GLFW_PRESS && KeyMappingCompat.matches(REFIT_KEY, event)) {
            LocalPlayer player = Minecraft.getInstance().player;
            if (player == null || player.isSpectator()) {
                return;
            }
            if (isInGame()) {
                if (IGun.mainHandHoldGun(player) && Minecraft.getInstance().screen == null) {
                    IGun iGun = IGun.getIGunOrNull(player.getMainHandItem());
                    if (iGun != null && iGun.hasAttachmentLock(player.getMainHandItem())) {
                        return;
                    }
                    Minecraft.getInstance().setScreen(new GunRefitScreen());
                }
            } else if (Minecraft.getInstance().screen instanceof GunRefitScreen refitScreen) {
                refitScreen.onClose();
            }
        }
    }
}

