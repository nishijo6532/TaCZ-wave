package com.tacz.guns.network.message;

import com.tacz.guns.api.entity.IGunOperator;
import com.tacz.guns.api.item.IGun;
import com.tacz.guns.api.item.gun.FireMode;
import com.tacz.guns.entity.shooter.LivingEntityShoot;
import com.tacz.guns.entity.shooter.ShooterDataHolder;
import com.tacz.guns.network.NetworkEvent;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;

import java.util.function.Supplier;

public class ClientMessagePlayerAutoShoot {
    private final boolean shooting;

    public ClientMessagePlayerAutoShoot(boolean shooting) {
        this.shooting = shooting;
    }

    public static void encode(ClientMessagePlayerAutoShoot message, FriendlyByteBuf buf) {
        buf.writeBoolean(message.shooting);
    }

    public static ClientMessagePlayerAutoShoot decode(FriendlyByteBuf buf) {
        return new ClientMessagePlayerAutoShoot(buf.readBoolean());
    }

    public static void handle(ClientMessagePlayerAutoShoot message, Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        if (context.getDirection().getReceptionSide().isServer()) {
            context.enqueueWork(() -> {
                ServerPlayer player = context.getSender();
                if (player == null) {
                    return;
                }
                IGunOperator operator = IGunOperator.fromLivingEntity(player);
                ShooterDataHolder data = operator.getDataHolder();
                if (!message.shooting) {
                    data.isAutoShooting = false;
                    data.autoShootLastNanos = -1L;
                    data.autoShootAccumulator = 0;
                    data.autoFireProfile = null;
                    return;
                }
                ItemStack mainHandItem = player.getMainHandItem();
                if (!(mainHandItem.getItem() instanceof IGun gun)) {
                    return;
                }
                FireMode fireMode = gun.getFireMode(mainHandItem);
                if (!LivingEntityShoot.isAutoShootMode(fireMode, gun, mainHandItem)) {
                    return;
                }
                data.isAutoShooting = true;
                data.autoShootLastNanos = -1L;
                data.autoShootAccumulator = 0;
                data.autoFireProfile = null;
            });
        }
        context.setPacketHandled(true);
    }
}
