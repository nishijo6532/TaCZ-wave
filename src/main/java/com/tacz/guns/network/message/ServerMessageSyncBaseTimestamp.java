package com.tacz.guns.network.message;

import com.tacz.guns.GunMod;
import com.tacz.guns.api.client.gameplay.IClientPlayerGunOperator;
import com.tacz.guns.client.gameplay.LocalPlayerDataHolder;
import com.tacz.guns.network.NetworkHandler;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.FriendlyByteBuf;
import com.tacz.guns.network.NetworkEvent;
import org.apache.logging.log4j.Marker;
import org.apache.logging.log4j.MarkerManager;

import java.util.function.Supplier;

public class ServerMessageSyncBaseTimestamp {
    private static final Marker MARKER = MarkerManager.getMarker("SYNC_BASE_TIMESTAMP");
    private static volatile long pendingClientBaseTimestamp = -1L;

    public ServerMessageSyncBaseTimestamp() { }

    public static void encode(ServerMessageSyncBaseTimestamp message, FriendlyByteBuf buf) { }

    public static ServerMessageSyncBaseTimestamp decode(FriendlyByteBuf buf) {
        return new ServerMessageSyncBaseTimestamp();
    }

    public static void handle(ServerMessageSyncBaseTimestamp message, Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        if (context.getDirection().getReceptionSide().isClient()) {
            long timestamp = System.currentTimeMillis();
            context.enqueueWork(() -> {
                updateBaseTimestamp(timestamp);
                if (Minecraft.getInstance().getConnection() != null) {
                    NetworkHandler.sendToServer(new ClientMessageSyncBaseTimestamp());
                }
            });
        }
        context.setPacketHandled(true);
    }

    private static void updateBaseTimestamp(long timestamp) {
        LocalPlayer player = Minecraft.getInstance().player;
        if (player == null) {
            pendingClientBaseTimestamp = timestamp;
            return;
        }
        applyBaseTimestamp(player, timestamp);
        pendingClientBaseTimestamp = -1L;
    }

    public static void applyPendingBaseTimestamp(LocalPlayer player) {
        long pending = pendingClientBaseTimestamp;
        if (pending < 0 || player == null) {
            return;
        }
        applyBaseTimestamp(player, pending);
        pendingClientBaseTimestamp = -1L;
    }

    private static void applyBaseTimestamp(LocalPlayer player, long timestamp) {
        LocalPlayerDataHolder dataHolder = IClientPlayerGunOperator.fromLocalPlayer(player).getDataHolder();
        dataHolder.clientBaseTimestamp = timestamp;
        GunMod.LOGGER.debug(MARKER, "Update client base timestamp: {}", dataHolder.clientBaseTimestamp);
    }
}
