package com.tacz.guns.network.message.handshake;

import com.tacz.guns.GunMod;
import com.tacz.guns.entity.sync.core.SyncedDataKey;
import com.tacz.guns.entity.sync.core.SyncedEntityData;
import com.tacz.guns.network.IMessage;
import com.tacz.guns.network.LoginIndexHolder;
import com.tacz.guns.network.NetworkHandler;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import com.tacz.guns.network.NetworkEvent;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.logging.log4j.Marker;
import org.apache.logging.log4j.MarkerManager;

import java.util.*;
import java.util.concurrent.CountDownLatch;
import java.util.function.Supplier;

public class ServerMessageSyncedEntityDataMapping extends LoginIndexHolder implements IMessage<ServerMessageSyncedEntityDataMapping> {
    public static final Marker HANDSHAKE = MarkerManager.getMarker("TACZ_HANDSHAKE");
    private Map<Identifier, List<Pair<Identifier, Integer>>> keyMap;

    public ServerMessageSyncedEntityDataMapping() {
    }

    private ServerMessageSyncedEntityDataMapping(Map<Identifier, List<Pair<Identifier, Integer>>> keyMap) {
        this.keyMap = keyMap;
    }

    @Override
    public void encode(ServerMessageSyncedEntityDataMapping message, FriendlyByteBuf buffer) {
        Set<SyncedDataKey<?, ?>> keys = SyncedEntityData.instance().getKeys();
        buffer.writeInt(keys.size());
        keys.forEach(key -> {
            int id = SyncedEntityData.instance().getInternalId(key);
            buffer.writeIdentifier(key.classKey().id());
            buffer.writeIdentifier(key.id());
            buffer.writeVarInt(id);
        });
    }

    @Override
    public ServerMessageSyncedEntityDataMapping decode(FriendlyByteBuf buffer) {
        int size = buffer.readInt();
        Map<Identifier, List<Pair<Identifier, Integer>>> keyMap = new HashMap<>();
        for (int i = 0; i < size; i++) {
            Identifier classId = buffer.readIdentifier();
            Identifier keyId = buffer.readIdentifier();
            int id = buffer.readVarInt();
            keyMap.computeIfAbsent(classId, c -> new ArrayList<>()).add(Pair.of(keyId, id));
        }
        return new ServerMessageSyncedEntityDataMapping(keyMap);
    }

    @Override
    public void handle(ServerMessageSyncedEntityDataMapping message, Supplier<NetworkEvent.Context> supplier) {
        GunMod.LOGGER.debug(HANDSHAKE, "Received synced key mappings from server");
        CountDownLatch block = new CountDownLatch(1);
        supplier.get().enqueueWork(() -> {
            if (!SyncedEntityData.instance().updateMappings(message)) {
                supplier.get().getNetworkManager().disconnect(Component.literal("Connection closed - [TacZ] Received unknown synced data keys."));
            }
            block.countDown();
        });
        try {
            block.await();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        supplier.get().setPacketHandled(true);
        NetworkHandler.HANDSHAKE_CHANNEL.reply(new Acknowledge(), supplier.get().unwrap());
    }

    public Map<Identifier, List<Pair<Identifier, Integer>>> getKeyMap() {
        return this.keyMap;
    }
}


