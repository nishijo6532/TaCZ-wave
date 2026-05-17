package com.tacz.guns.network.message.event;

import com.tacz.guns.api.event.common.GunDrawEvent;
import com.tacz.guns.util.NbtCompat;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.LogicalSide;
import com.tacz.guns.network.NetworkEvent;

import java.util.function.Supplier;

public class ServerMessageGunDraw {
    private final int entityId;
    private final ItemStack previousGunItem;
    private final ItemStack currentGunItem;

    public ServerMessageGunDraw(int entityId, ItemStack previousGunItem, ItemStack currentGunItem) {
        this.entityId = entityId;
        this.previousGunItem = previousGunItem;
        this.currentGunItem = currentGunItem;
    }

    public static void encode(ServerMessageGunDraw message, FriendlyByteBuf buf) {
        buf.writeVarInt(message.entityId);
        buf.writeNbt(NbtCompat.serializeItemStack(message.previousGunItem));
        buf.writeNbt(NbtCompat.serializeItemStack(message.currentGunItem));
    }

    public static ServerMessageGunDraw decode(FriendlyByteBuf buf) {
        int entityId = buf.readVarInt();
        ItemStack previousGunItem = NbtCompat.deserializeItemStack(buf.readNbt());
        ItemStack currentGunItem = NbtCompat.deserializeItemStack(buf.readNbt());
        return new ServerMessageGunDraw(entityId, previousGunItem, currentGunItem);
    }

    public static void handle(ServerMessageGunDraw message, Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        if (context.getDirection().getReceptionSide().isClient()) {
            context.enqueueWork(() -> doClientEvent(message));
        }
        context.setPacketHandled(true);
    }

    private static void doClientEvent(ServerMessageGunDraw message) {
        ClientLevel level = Minecraft.getInstance().level;
        if (level == null) {
            return;
        }
        if (level.getEntity(message.entityId) instanceof LivingEntity livingEntity) {
            GunDrawEvent gunDrawEvent = new GunDrawEvent(livingEntity, message.previousGunItem, message.currentGunItem, LogicalSide.CLIENT);
        com.tacz.guns.util.ForgeEventCompat.post(gunDrawEvent);
        }
    }
}
