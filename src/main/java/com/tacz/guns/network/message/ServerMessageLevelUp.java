package com.tacz.guns.network.message;

import com.tacz.guns.util.NbtCompat;
import net.minecraft.client.Minecraft;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import com.tacz.guns.network.NetworkEvent;

import java.util.function.Supplier;

public class ServerMessageLevelUp {
    private final ItemStack gun;
    private final int level;

    public ServerMessageLevelUp(ItemStack gun, int level) {
        this.gun = gun;
        this.level = level;
    }

    public static void encode(ServerMessageLevelUp message, FriendlyByteBuf buf) {
        buf.writeNbt(NbtCompat.serializeItemStack(message.gun));
        buf.writeInt(message.level);
    }

    public static ServerMessageLevelUp decode(FriendlyByteBuf buf) {
        ItemStack gun = NbtCompat.deserializeItemStack(buf.readNbt());
        int level = buf.readInt();
        return new ServerMessageLevelUp(gun, level);
    }

    public static void handle(ServerMessageLevelUp message, Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        if (context.getDirection().getReceptionSide().isClient()) {
            context.enqueueWork(() -> onLevelUp(message));
        }
        context.setPacketHandled(true);
    }

    private static void onLevelUp(ServerMessageLevelUp message) {
        int level = message.getLevel();
        ItemStack gun = message.getGun();
        Player player = Minecraft.getInstance().player;
        if (player == null) {
            return;
        }
        // TODO 蝨ｨ螳梧・莠・棯譴ｰ蜊・ｺｧ騾ｻ霎大錘・瑚ｧ｣蟆∽ｸ矩擇逧・ｻ｣遐・
                /*
                if (GunLevelManager.DAMAGE_UP_LEVELS.contains(level)) {
                    Minecraft.getInstance().getToasts().addToast(new GunLevelUpToast(gun,
                            Component.translatable("toast.tacz.level_up"),
                            Component.translatable("toast.tacz.sub.damage_up")));
                } else if (level >= GunLevelManager.MAX_LEVEL) {
                    Minecraft.getInstance().getToasts().addToast(new GunLevelUpToast(gun,
                            Component.translatable("toast.tacz.level_up"),
                            Component.translatable("toast.tacz.sub.final_level")));
                } else {
                    Minecraft.getInstance().getToasts().addToast(new GunLevelUpToast(gun,
                            Component.translatable("toast.tacz.level_up"),
                            Component.translatable("toast.tacz.sub.level_up")));
                }*/
    }

    public ItemStack getGun() {
        return this.gun;
    }

    public int getLevel() {
        return this.level;
    }
}
