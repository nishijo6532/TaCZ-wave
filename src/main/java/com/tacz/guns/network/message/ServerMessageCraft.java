package com.tacz.guns.network.message;

import com.tacz.guns.client.gui.GunSmithTableScreen;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.FriendlyByteBuf;
import com.tacz.guns.network.NetworkEvent;

import java.util.function.Supplier;

public class ServerMessageCraft {
    private final int menuId;

    public ServerMessageCraft(int menuId) {
        this.menuId = menuId;
    }

    public static void encode(ServerMessageCraft message, FriendlyByteBuf buf) {
        buf.writeVarInt(message.menuId);
    }

    public static ServerMessageCraft decode(FriendlyByteBuf buf) {
        return new ServerMessageCraft(buf.readVarInt());
    }

    public static void handle(ServerMessageCraft message, Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        if (context.getDirection().getReceptionSide().isClient()) {
            context.enqueueWork(() -> updateScreen(message.menuId));
        }
        context.setPacketHandled(true);
    }

    private static void updateScreen(int containerId) {
        LocalPlayer player = Minecraft.getInstance().player;
        if (player != null && player.containerMenu.containerId == containerId && Minecraft.getInstance().screen instanceof GunSmithTableScreen screen) {
            try {
                screen.getClass().getMethod("updateIngredientCount").invoke(screen);
            } catch (ReflectiveOperationException ignored) {
            }
        }
    }
}
