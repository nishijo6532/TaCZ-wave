package com.tacz.guns.network.message;

import com.tacz.guns.api.entity.IGunOperator;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import com.tacz.guns.network.NetworkEvent;

import java.util.function.Supplier;

public class ClientMessagePlayerShoot {
    /**
     * 霑咎㈹逧・timestamp 蠎碑ｯ･譏ｯ蝓ｺ莠・base timestamp 逧・嶌蟇ｹ蛟ｼ
     */
    private long timestamp;
    private float pitch;
    private float yaw;

    public ClientMessagePlayerShoot() {
    }

    public ClientMessagePlayerShoot(long timestamp, float pitch, float yaw) {
        this.timestamp = timestamp;
        this.pitch = pitch;
        this.yaw = yaw;
    }

    public static void encode(ClientMessagePlayerShoot message, FriendlyByteBuf buf) {
        buf.writeLong(message.timestamp);
        buf.writeFloat(message.pitch);
        buf.writeFloat(message.yaw);
    }

    public static ClientMessagePlayerShoot decode(FriendlyByteBuf buf) {
        return new ClientMessagePlayerShoot(buf.readLong(), buf.readFloat(), buf.readFloat());
    }

    public static void handle(ClientMessagePlayerShoot message, Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        if (context.getDirection().getReceptionSide().isServer()) {
            context.enqueueWork(() -> {
                ServerPlayer entity = context.getSender();
                if (entity == null) {
                    return;
                }
                IGunOperator.fromLivingEntity(entity).shoot(
                        () -> message.pitch,
                        () -> message.yaw,
                        message.timestamp
                );
            });
        }
        context.setPacketHandled(true);
    }
}
