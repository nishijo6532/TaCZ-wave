package com.tacz.guns.network.message.event;

import com.tacz.guns.api.event.common.EntityHurtByGunEvent;
import com.tacz.guns.client.event.ClientHitMark;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.LogicalSide;
import com.tacz.guns.network.NetworkEvent;

import javax.annotation.Nullable;
import java.util.function.Supplier;

public class ServerMessageGunHurt {
    private final int bulletId;
    private final int hurtEntityId;
    private final int attackerId;
    private final Identifier gunId;
    private final Identifier gunDisplayId;
    private final float amount;
    private final boolean isHeadShot;
    private final float headshotMultiplier;

    public ServerMessageGunHurt(int bulletId, int hurtEntityId, int attackerId, Identifier gunId, Identifier gunDisplayId,
                                float amount, boolean isHeadShot, float headshotMultiplier) {
        this.bulletId = bulletId;
        this.hurtEntityId = hurtEntityId;
        this.attackerId = attackerId;
        this.gunId = gunId;
        this.gunDisplayId = gunDisplayId;
        this.amount = amount;
        this.isHeadShot = isHeadShot;
        this.headshotMultiplier = headshotMultiplier;
    }

    public static void encode(ServerMessageGunHurt message, FriendlyByteBuf buf) {
        buf.writeInt(message.bulletId);
        buf.writeInt(message.hurtEntityId);
        buf.writeInt(message.attackerId);
        buf.writeIdentifier(message.gunId);
        buf.writeIdentifier(message.gunDisplayId);
        buf.writeFloat(message.amount);
        buf.writeBoolean(message.isHeadShot);
        buf.writeFloat(message.headshotMultiplier);
    }

    public static ServerMessageGunHurt decode(FriendlyByteBuf buf) {
        int bulletId = buf.readInt();
        int hurtEntityId = buf.readInt();
        int attackerId = buf.readInt();
        Identifier gunId = buf.readIdentifier();
        Identifier gunDisplayId = buf.readIdentifier();
        float amount = buf.readFloat();
        boolean isHeadShot = buf.readBoolean();
        float headshotMultiplier = buf.readFloat();
        return new ServerMessageGunHurt(bulletId, hurtEntityId, attackerId, gunId, gunDisplayId, amount, isHeadShot, headshotMultiplier);
    }

    public static void handle(ServerMessageGunHurt message, Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        if (context.getDirection().getReceptionSide().isClient()) {
            context.enqueueWork(() -> onHurt(message));
        }
        context.setPacketHandled(true);
    }

    private static void onHurt(ServerMessageGunHurt message) {
        ClientLevel level = Minecraft.getInstance().level;
        if (level == null) {
            return;
        }
        @Nullable Entity bullet = level.getEntity(message.bulletId);
        @Nullable Entity hurtEntity = level.getEntity(message.hurtEntityId);
        @Nullable LivingEntity attacker = level.getEntity(message.attackerId) instanceof LivingEntity livingEntity ? livingEntity : null;
        LocalPlayer player = Minecraft.getInstance().player;
        if (attacker == null && player != null && player.getId() == message.attackerId) {
            attacker = player;
        }
        EntityHurtByGunEvent.Post event = new EntityHurtByGunEvent.Post(bullet, hurtEntity, attacker, message.gunId, message.gunDisplayId, message.amount, null, message.isHeadShot, message.headshotMultiplier, LogicalSide.CLIENT);
        ClientHitMark.onEntityHurt(event);
        com.tacz.guns.util.ForgeEventCompat.post(event);
    }
}


