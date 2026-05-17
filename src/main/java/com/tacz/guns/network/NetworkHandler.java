package com.tacz.guns.network;

import com.tacz.guns.GunMod;
import com.tacz.guns.network.message.*;
import com.tacz.guns.network.message.event.*;
import com.tacz.guns.network.message.handshake.Acknowledge;
import com.tacz.guns.network.message.handshake.ServerMessageSyncedEntityDataMapping;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraftforge.event.network.CustomPayloadEvent;
import net.minecraftforge.network.ChannelBuilder;
import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.network.SimpleChannel;
import org.apache.commons.lang3.tuple.Pair;

import javax.annotation.Nullable;
import java.lang.reflect.Constructor;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BiConsumer;
import java.util.function.Function;
import java.util.function.Supplier;

public class NetworkHandler {
    private static final int VERSION = 104;

    public static final SimpleChannel HANDSHAKE_CHANNEL = createChannel("handshake");
    public static final SimpleChannel CHANNEL = createChannel("network");

    private static final AtomicInteger ID_COUNT = new AtomicInteger(1);
    private static final AtomicInteger HANDSHAKE_ID_COUNT = new AtomicInteger(1);

    private static SimpleChannel createChannel(String name) {
        return ChannelBuilder.named(com.tacz.guns.util.IdHelper.id(GunMod.MOD_ID, name))
                .networkProtocolVersion(VERSION)
                .acceptedVersions((status, version) -> version == VERSION)
                .simpleChannel();
    }

    private static <M> void registerPlayMessage(Class<M> type,
                                                BiConsumer<M, FriendlyByteBuf> encoder,
                                                Function<FriendlyByteBuf, M> decoder,
                                                BiConsumer<M, Supplier<com.tacz.guns.network.NetworkEvent.Context>> handler,
                                                NetworkDirection<?> direction) {
        CHANNEL.messageBuilder(type, ID_COUNT.getAndIncrement())
                .encoder(encoder)
                .decoder(decoder)
                .consumerNetworkThread((BiConsumer<M, CustomPayloadEvent.Context>) (msg, ctx) -> handler.accept(msg, () -> new com.tacz.guns.network.NetworkEvent.Context(ctx)))
                .direction(direction.direction())
                .add();
    }

    public static void init() {
        registerPlayMessage(ClientMessagePlayerShoot.class, ClientMessagePlayerShoot::encode, ClientMessagePlayerShoot::decode, ClientMessagePlayerShoot::handle, NetworkDirection.PLAY_TO_SERVER);
        registerPlayMessage(ClientMessagePlayerReloadGun.class, ClientMessagePlayerReloadGun::encode, ClientMessagePlayerReloadGun::decode, ClientMessagePlayerReloadGun::handle, NetworkDirection.PLAY_TO_SERVER);
        registerPlayMessage(ClientMessagePlayerCancelReload.class, ClientMessagePlayerCancelReload::encode, ClientMessagePlayerCancelReload::decode, ClientMessagePlayerCancelReload::handle, NetworkDirection.PLAY_TO_SERVER);
        registerPlayMessage(ClientMessagePlayerFireSelect.class, ClientMessagePlayerFireSelect::encode, ClientMessagePlayerFireSelect::decode, ClientMessagePlayerFireSelect::handle, NetworkDirection.PLAY_TO_SERVER);
        registerPlayMessage(ClientMessagePlayerAim.class, ClientMessagePlayerAim::encode, ClientMessagePlayerAim::decode, ClientMessagePlayerAim::handle, NetworkDirection.PLAY_TO_SERVER);
        registerPlayMessage(ClientMessagePlayerCrawl.class, ClientMessagePlayerCrawl::encode, ClientMessagePlayerCrawl::decode, ClientMessagePlayerCrawl::handle, NetworkDirection.PLAY_TO_SERVER);
        registerPlayMessage(ClientMessagePlayerDrawGun.class, ClientMessagePlayerDrawGun::encode, ClientMessagePlayerDrawGun::decode, ClientMessagePlayerDrawGun::handle, NetworkDirection.PLAY_TO_SERVER);
        registerPlayMessage(ServerMessageSound.class, ServerMessageSound::encode, ServerMessageSound::decode, ServerMessageSound::handle, NetworkDirection.PLAY_TO_CLIENT);
        registerPlayMessage(ClientMessageCraft.class, ClientMessageCraft::encode, ClientMessageCraft::decode, ClientMessageCraft::handle, NetworkDirection.PLAY_TO_SERVER);
        registerPlayMessage(ServerMessageCraft.class, ServerMessageCraft::encode, ServerMessageCraft::decode, ServerMessageCraft::handle, NetworkDirection.PLAY_TO_CLIENT);
        registerPlayMessage(ClientMessagePlayerZoom.class, ClientMessagePlayerZoom::encode, ClientMessagePlayerZoom::decode, ClientMessagePlayerZoom::handle, NetworkDirection.PLAY_TO_SERVER);
        registerPlayMessage(ClientMessageRefitGun.class, ClientMessageRefitGun::encode, ClientMessageRefitGun::decode, ClientMessageRefitGun::handle, NetworkDirection.PLAY_TO_SERVER);
        registerPlayMessage(ServerMessageRefreshRefitScreen.class, ServerMessageRefreshRefitScreen::encode, ServerMessageRefreshRefitScreen::decode, ServerMessageRefreshRefitScreen::handle, NetworkDirection.PLAY_TO_CLIENT);
        registerPlayMessage(ClientMessageUnloadAttachment.class, ClientMessageUnloadAttachment::encode, ClientMessageUnloadAttachment::decode, ClientMessageUnloadAttachment::handle, NetworkDirection.PLAY_TO_SERVER);
        registerPlayMessage(ServerMessageSwapItem.class, ServerMessageSwapItem::encode, ServerMessageSwapItem::decode, ServerMessageSwapItem::handle, NetworkDirection.PLAY_TO_CLIENT);
        registerPlayMessage(ClientMessagePlayerBoltGun.class, ClientMessagePlayerBoltGun::encode, ClientMessagePlayerBoltGun::decode, ClientMessagePlayerBoltGun::handle, NetworkDirection.PLAY_TO_SERVER);
        registerPlayMessage(ServerMessageLevelUp.class, ServerMessageLevelUp::encode, ServerMessageLevelUp::decode, ServerMessageLevelUp::handle, NetworkDirection.PLAY_TO_CLIENT);
        registerPlayMessage(ServerMessageGunHurt.class, ServerMessageGunHurt::encode, ServerMessageGunHurt::decode, ServerMessageGunHurt::handle, NetworkDirection.PLAY_TO_CLIENT);
        registerPlayMessage(ServerMessageGunKill.class, ServerMessageGunKill::encode, ServerMessageGunKill::decode, ServerMessageGunKill::handle, NetworkDirection.PLAY_TO_CLIENT);
        registerPlayMessage(ServerMessageUpdateEntityData.class, ServerMessageUpdateEntityData::encode, ServerMessageUpdateEntityData::decode, ServerMessageUpdateEntityData::handle, NetworkDirection.PLAY_TO_CLIENT);
        registerPlayMessage(ServerMessageSyncGunPack.class, ServerMessageSyncGunPack::encode, ServerMessageSyncGunPack::decode, ServerMessageSyncGunPack::handle, NetworkDirection.PLAY_TO_CLIENT);
        registerPlayMessage(ClientMessagePlayerMelee.class, ClientMessagePlayerMelee::encode, ClientMessagePlayerMelee::decode, ClientMessagePlayerMelee::handle, NetworkDirection.PLAY_TO_SERVER);

        registerPlayMessage(ServerMessageGunDraw.class, ServerMessageGunDraw::encode, ServerMessageGunDraw::decode, ServerMessageGunDraw::handle, NetworkDirection.PLAY_TO_CLIENT);
        registerPlayMessage(ServerMessageGunFire.class, ServerMessageGunFire::encode, ServerMessageGunFire::decode, ServerMessageGunFire::handle, NetworkDirection.PLAY_TO_CLIENT);
        registerPlayMessage(ServerMessageGunFireSelect.class, ServerMessageGunFireSelect::encode, ServerMessageGunFireSelect::decode, ServerMessageGunFireSelect::handle, NetworkDirection.PLAY_TO_CLIENT);
        registerPlayMessage(ServerMessageGunMelee.class, ServerMessageGunMelee::encode, ServerMessageGunMelee::decode, ServerMessageGunMelee::handle, NetworkDirection.PLAY_TO_CLIENT);
        registerPlayMessage(ServerMessageGunReload.class, ServerMessageGunReload::encode, ServerMessageGunReload::decode, ServerMessageGunReload::handle, NetworkDirection.PLAY_TO_CLIENT);
        registerPlayMessage(ServerMessageGunShoot.class, ServerMessageGunShoot::encode, ServerMessageGunShoot::decode, ServerMessageGunShoot::handle, NetworkDirection.PLAY_TO_CLIENT);
        registerPlayMessage(ServerMessageSyncBaseTimestamp.class, ServerMessageSyncBaseTimestamp::encode, ServerMessageSyncBaseTimestamp::decode, ServerMessageSyncBaseTimestamp::handle, NetworkDirection.PLAY_TO_CLIENT);
        registerPlayMessage(ClientMessageSyncBaseTimestamp.class, ClientMessageSyncBaseTimestamp::encode, ClientMessageSyncBaseTimestamp::decode, ClientMessageSyncBaseTimestamp::handle, NetworkDirection.PLAY_TO_SERVER);

        registerPlayMessage(ClientMessageLaserColor.class, ClientMessageLaserColor::encode, ClientMessageLaserColor::decode, ClientMessageLaserColor::handle, NetworkDirection.PLAY_TO_SERVER);

        registerAcknowledge();
        registerHandshakeMessage(ServerMessageSyncedEntityDataMapping.class, null);
    }

    public static void registerAcknowledge() {
        Acknowledge acknowledge = new Acknowledge();
        HANDSHAKE_CHANNEL.messageBuilder(Acknowledge.class, HANDSHAKE_ID_COUNT.getAndIncrement(), NetworkDirection.LOGIN_TO_SERVER)
                .decoder(acknowledge::decode)
                .encoder(acknowledge::encode)
                .consumerNetworkThread((BiConsumer<Acknowledge, CustomPayloadEvent.Context>) (msg, ctx) -> acknowledge.handle(msg, () -> new com.tacz.guns.network.NetworkEvent.Context(ctx)))
                .add();
    }

    public static <T extends LoginIndexHolder & IMessage<T>> void registerHandshakeMessage(Class<T> messageClass, @Nullable Function<Boolean, List<Pair<String, T>>> messages) {
        try {
            Constructor<T> constructor = messageClass.getDeclaredConstructor();
            T message = constructor.newInstance();
            SimpleChannel.MessageBuilder<T, FriendlyByteBuf> builder = HANDSHAKE_CHANNEL.messageBuilder(messageClass, HANDSHAKE_ID_COUNT.getAndIncrement(), NetworkDirection.LOGIN_TO_CLIENT)
                    .encoder(message::encode)
                    .decoder(message::decode)
                    .consumerNetworkThread((BiConsumer<T, CustomPayloadEvent.Context>) (msg, ctx) -> message.handle(msg, () -> new com.tacz.guns.network.NetworkEvent.Context(ctx)));
            builder.add();
        } catch (NoSuchMethodException e) {
            throw new IllegalArgumentException(String.format("The message %s is missing an empty parameter constructor", messageClass.getName()), e);
        } catch (IllegalAccessException e) {
            throw new IllegalArgumentException(String.format("Unable to access the constructor of %s. Make sure the constructor is public.", messageClass.getName()), e);
        } catch (Exception e) {
            GunMod.LOGGER.error("Fail to register handshake message {}", messageClass.getName());
            e.printStackTrace();
        }
    }

    public static void sendToClientPlayer(Object message, Player player) {
        CHANNEL.send(message, PacketDistributor.PLAYER.with((ServerPlayer) player));
    }

    public static void sendToTrackingEntityAndSelf(Entity centerEntity, Object message) {
        CHANNEL.send(message, PacketDistributor.TRACKING_ENTITY_AND_SELF.with(centerEntity));
    }

    public static void sendToServer(Object message) {
        CHANNEL.send(message, PacketDistributor.SERVER.noArg());
    }

    public static void sendToAllPlayers(Object message) {
        CHANNEL.send(message, PacketDistributor.ALL.noArg());
    }

    public static void sendToTrackingEntity(Object message, final Entity centerEntity) {
        CHANNEL.send(message, PacketDistributor.TRACKING_ENTITY.with(centerEntity));
    }

    public static void sendToDimension(Object message, final Entity centerEntity) {
        ResourceKey<Level> dimension = centerEntity.level().dimension();
        CHANNEL.send(message, PacketDistributor.DIMENSION.with(dimension));
    }
}
