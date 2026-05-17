package com.tacz.guns.network.message;

import com.tacz.guns.client.resource.ClientIndexManager;
import com.tacz.guns.resource.network.CommonNetworkCache;
import com.tacz.guns.resource.network.DataType;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.Identifier;
import com.tacz.guns.network.NetworkEvent;

import java.util.Map;
import java.util.function.Supplier;


public class ServerMessageSyncGunPack {
    private final Map<DataType, Map<Identifier, String>> cache;

    public ServerMessageSyncGunPack(Map<DataType, Map<Identifier, String>> cache) {
        this.cache = cache;
    }

    public static void encode(ServerMessageSyncGunPack message, FriendlyByteBuf buf) {
        buf.writeMap(message.getCache(), FriendlyByteBuf::writeEnum, (buf1, map) -> {
            buf1.writeMap(map, FriendlyByteBuf::writeIdentifier, FriendlyByteBuf::writeUtf);
        });
    }

    public static ServerMessageSyncGunPack decode(FriendlyByteBuf buf) {
        var map = buf.readMap(buf1 -> buf1.readEnum(DataType.class), buf2 -> {
            return buf2.readMap(FriendlyByteBuf::readIdentifier, FriendlyByteBuf::readUtf);
        });
        return new ServerMessageSyncGunPack(map);
    }

    public static void handle(ServerMessageSyncGunPack message, Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        if (context.getDirection().getReceptionSide().isClient()) {
            context.enqueueWork(() -> doSync(message));
        }
        context.setPacketHandled(true);
    }


    public Map<DataType, Map<Identifier, String>> getCache() {
        return cache;
    }

    private static void doSync(ServerMessageSyncGunPack message) {
        CommonNetworkCache.INSTANCE.fromNetwork(message.cache);
        // 鬨ｾ螟り｡崎楜・｢隰鯉ｽｷ驕ｶ・ｯ鬩･閧ｴ逵隴ｫ繝ｻ・ｻ・ｺClientIndex
        ClientIndexManager.reload();
    }
}


