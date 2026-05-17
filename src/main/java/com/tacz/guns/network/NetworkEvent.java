package com.tacz.guns.network;

import net.minecraft.network.Connection;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.network.CustomPayloadEvent;
import org.jetbrains.annotations.Nullable;

import java.util.concurrent.CompletableFuture;

public final class NetworkEvent {
    private NetworkEvent() {
    }

    public enum ReceptionSide {
        CLIENT,
        SERVER;

        public boolean isClient() {
            return this == CLIENT;
        }

        public boolean isServer() {
            return this == SERVER;
        }
    }

    public static final class Direction {
        public static final Direction PLAY_TO_CLIENT = new Direction(ReceptionSide.CLIENT);
        public static final Direction PLAY_TO_SERVER = new Direction(ReceptionSide.SERVER);

        private final ReceptionSide receptionSide;

        private Direction(ReceptionSide receptionSide) {
            this.receptionSide = receptionSide;
        }

        public ReceptionSide getReceptionSide() {
            return receptionSide;
        }
    }

    public static class Context {
        private final CustomPayloadEvent.Context delegate;

        public Context(CustomPayloadEvent.Context delegate) {
            this.delegate = delegate;
        }

        public CompletableFuture<Void> enqueueWork(Runnable runnable) {
            return delegate.enqueueWork(runnable);
        }

        @Nullable
        public ServerPlayer getSender() {
            return delegate.getSender();
        }

        public void setPacketHandled(boolean handled) {
            delegate.setPacketHandled(handled);
        }

        public boolean getPacketHandled() {
            return delegate.getPacketHandled();
        }

        public Connection getNetworkManager() {
            return delegate.getConnection();
        }

        public Direction getDirection() {
            return delegate.isServerSide() ? Direction.PLAY_TO_SERVER : Direction.PLAY_TO_CLIENT;
        }

        public CustomPayloadEvent.Context unwrap() {
            return delegate;
        }
    }
}
