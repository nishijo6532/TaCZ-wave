package com.tacz.guns.client.input;

import com.mojang.blaze3d.platform.InputConstants;
import com.tacz.guns.client.gui.compat.ClothConfigScreen;
import com.tacz.guns.compat.cloth.MenuIntegration;
import com.tacz.guns.util.KeyMappingCompat;
import com.tacz.guns.util.ModListCompat;
import net.minecraft.ChatFormatting;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.network.chat.MutableComponent;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.InputEvent;
import net.minecraftforge.client.settings.KeyConflictContext;
import net.minecraftforge.client.settings.KeyModifier;
import net.minecraftforge.eventbus.api.listener.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.lwjgl.glfw.GLFW;

import java.net.URI;

import static com.tacz.guns.util.InputExtraCheck.isInGame;

@Mod.EventBusSubscriber(value = Dist.CLIENT)
public class ConfigKey {
    private static final String CLOTH_CONFIG_MODID = "cloth_config";
    public static final KeyMapping OPEN_CONFIG_KEY = KeyMappingCompat.create("key.tacz.open_config.desc",
            KeyConflictContext.IN_GAME,
            KeyModifier.ALT,
            InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_T,
            "key.category.tacz");

    @SubscribeEvent
    public static void onOpenConfig(InputEvent.Key event) {
        if (isInGame() && event.getAction() == GLFW.GLFW_PRESS
                && KeyMappingCompat.matches(OPEN_CONFIG_KEY, event)) {
            LocalPlayer player = Minecraft.getInstance().player;
            if (player == null || player.isSpectator()) {
                return;
            }
            if (!ModListCompat.isLoaded(CLOTH_CONFIG_MODID)) {
                ClickEvent clickEvent = new ClickEvent.OpenUrl(URI.create(ClothConfigScreen.CLOTH_CONFIG_URL));
                HoverEvent hoverEvent = new HoverEvent.ShowText(Component.translatable("gui.tacz.cloth_config_warning.download"));
                MutableComponent component = Component.translatable("gui.tacz.cloth_config_warning.tips").withStyle(style ->
                        style.applyFormat(ChatFormatting.BLUE).applyFormat(ChatFormatting.UNDERLINE).withClickEvent(clickEvent).withHoverEvent(hoverEvent));
                player.sendSystemMessage(component);
            } else {
                Minecraft.getInstance().setScreen(MenuIntegration.getConfigScreen(null));
            }
        }
    }
}

