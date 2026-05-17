package com.tacz.guns.client.input;

import com.mojang.blaze3d.platform.InputConstants;
import com.tacz.guns.api.client.gameplay.IClientPlayerGunOperator;
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
public class MeleeKey {
    public static final KeyMapping MELEE_KEY = KeyMappingCompat.create("key.tacz.melee.desc",
            KeyConflictContext.IN_GAME,
            KeyModifier.NONE,
            InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_V,
            "key.category.tacz");

    @SubscribeEvent
    public static void onMeleeKeyPress(InputEvent.Key event) {
        if (isInGame() && event.getAction() == GLFW.GLFW_PRESS && KeyMappingCompat.matches(MELEE_KEY, event)) {
            doMeleeLogic();
        }
    }

    @SubscribeEvent
    public static void onMeleeMousePress(InputEvent.MouseButton.Post event) {
        if (isInGame() && event.getAction() == GLFW.GLFW_PRESS && KeyMappingCompat.matchesMouse(MELEE_KEY, event)) {
            doMeleeLogic();
        }
    }

    public static boolean onMeleeControllerPress(boolean isPress) {
        if (isInGame() && isPress) {
            LocalPlayer player = Minecraft.getInstance().player;
            if (player == null || player.isSpectator()) {
                return false;
            }
            IClientPlayerGunOperator operator = IClientPlayerGunOperator.fromLocalPlayer(player);
            if (!operator.isAim()) {
                operator.melee();
                return true;
            }
        }
        return false;
    }

    private static void doMeleeLogic() {
        LocalPlayer player = Minecraft.getInstance().player;
        if (player == null || player.isSpectator()) {
            return;
        }
        IClientPlayerGunOperator operator = IClientPlayerGunOperator.fromLocalPlayer(player);
        if (!operator.isAim()) {
            operator.melee();
        }
    }
}

