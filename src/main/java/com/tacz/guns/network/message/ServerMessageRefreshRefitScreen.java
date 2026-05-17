package com.tacz.guns.network.message;

import com.tacz.guns.client.gui.GunRefitScreen;
import com.tacz.guns.resource.modifier.AttachmentPropertyManager;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.FriendlyByteBuf;
import com.tacz.guns.network.NetworkEvent;

import java.util.function.Supplier;

public class ServerMessageRefreshRefitScreen {
    public static void encode(ServerMessageRefreshRefitScreen message, FriendlyByteBuf buf) {
    }

    public static ServerMessageRefreshRefitScreen decode(FriendlyByteBuf buf) {
        return new ServerMessageRefreshRefitScreen();
    }

    public static void handle(ServerMessageRefreshRefitScreen message, Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        if (context.getDirection().getReceptionSide().isClient()) {
            context.enqueueWork(ServerMessageRefreshRefitScreen::updateScreen);
        }
        context.setPacketHandled(true);
    }

    private static void updateScreen() {
        LocalPlayer player = Minecraft.getInstance().player;
        if (player != null && Minecraft.getInstance().screen instanceof GunRefitScreen screen) {
            screen.init();
            // 陋ｻ・ｷ隴・ｽｰ鬩溷ｺ・ｻ・ｶ隰ｨ・ｰ隰撰ｽｮ繝ｻ謔滂ｽｮ・｢隰鯉ｽｷ驕ｶ・ｯ騾ｧ繝ｻ
            AttachmentPropertyManager.postChangeEvent(player, player.getMainHandItem());
        }
    }
}
