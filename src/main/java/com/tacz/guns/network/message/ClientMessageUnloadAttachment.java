package com.tacz.guns.network.message;

import com.tacz.guns.api.item.IGun;
import com.tacz.guns.api.item.attachment.AttachmentType;
import com.tacz.guns.network.NetworkHandler;
import com.tacz.guns.resource.modifier.AttachmentPropertyManager;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import com.tacz.guns.network.NetworkEvent;

import java.util.function.Supplier;

public class ClientMessageUnloadAttachment {
    private final int gunSlotIndex;
    private final AttachmentType attachmentType;

    public ClientMessageUnloadAttachment(int gunSlotIndex, AttachmentType attachmentType) {
        this.gunSlotIndex = gunSlotIndex;
        this.attachmentType = attachmentType;
    }

    public static void encode(ClientMessageUnloadAttachment message, FriendlyByteBuf buf) {
        buf.writeInt(message.gunSlotIndex);
        buf.writeEnum(message.attachmentType);
    }

    public static ClientMessageUnloadAttachment decode(FriendlyByteBuf buf) {
        return new ClientMessageUnloadAttachment(buf.readInt(), buf.readEnum(AttachmentType.class));
    }

    public static void handle(ClientMessageUnloadAttachment message, Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        if (context.getDirection().getReceptionSide().isServer()) {
            context.enqueueWork(() -> {
                ServerPlayer player = context.getSender();
                if (player == null) {
                    return;
                }
                Inventory inventory = player.getInventory();
                ItemStack gunItem = inventory.getItem(message.gunSlotIndex);
                IGun iGun = IGun.getIGunOrNull(gunItem);
                if (iGun != null) {
                    // 隴帶ｦ願球驕ｶ・ｯ隴ｬ・｡鬯ｪ遒√・闔会ｽｶ鬮槭・
                    if (iGun.hasAttachmentLock(gunItem)) {
                        return;
                    }
                    ItemStack attachmentItem = iGun.getAttachment(gunItem, message.attachmentType);
                    if (!attachmentItem.isEmpty() && inventory.add(attachmentItem)) {
                        iGun.unloadAttachment(gunItem, message.attachmentType);
                        ItemStack writtenAttachmentItem = iGun.getAttachment(gunItem, message.attachmentType);
                        // 陋ｻ・ｷ隴・ｽｰ鬩溷ｺ・ｻ・ｶ隰ｨ・ｰ隰撰ｽｮ
                        AttachmentPropertyManager.postChangeEvent(player, gunItem);
                        // 陞ｯ繧域｣｡陷奇ｽｸ髴難ｽｽ騾ｧ繝ｻ蠑崎ｬ・ｽｩ陞ｳ・ｹ陟托ｽｹ陋ｹ・｣繝ｻ謔溽憎陷・ｽｺ隰・隴帷甥・ｭ莉呻ｽｼ・ｹ
                        if (message.attachmentType == AttachmentType.EXTENDED_MAG) {
                            iGun.dropAllAmmo(player, gunItem);
                        }
                        player.inventoryMenu.broadcastChanges();
                        NetworkHandler.sendToClientPlayer(new ServerMessageRefreshRefitScreen(), player);
                    } else {
                    }
                } else {
                }
            });
        }
        context.setPacketHandled(true);
    }

}
