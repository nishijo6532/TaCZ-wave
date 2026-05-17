package com.tacz.guns.network.message;

import com.tacz.guns.api.item.IAttachment;
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

public class ClientMessageRefitGun {
    private final int attachmentSlotIndex;
    private final int gunSlotIndex;
    private final AttachmentType attachmentType;

    public ClientMessageRefitGun(int attachmentSlotIndex, int gunSlotIndex, AttachmentType attachmentType) {
        this.attachmentSlotIndex = attachmentSlotIndex;
        this.gunSlotIndex = gunSlotIndex;
        this.attachmentType = attachmentType;
    }

    public static void encode(ClientMessageRefitGun message, FriendlyByteBuf buf) {
        buf.writeInt(message.attachmentSlotIndex);
        buf.writeInt(message.gunSlotIndex);
        buf.writeEnum(message.attachmentType);
    }

    public static ClientMessageRefitGun decode(FriendlyByteBuf buf) {
        return new ClientMessageRefitGun(buf.readInt(), buf.readInt(), buf.readEnum(AttachmentType.class));
    }

    public static void handle(ClientMessageRefitGun message, Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        if (context.getDirection().getReceptionSide().isServer()) {
            context.enqueueWork(() -> {
                ServerPlayer player = context.getSender();
                if (player == null) {
                    return;
                }
                Inventory inventory = player.getInventory();
                ItemStack attachmentItem = inventory.getItem(message.attachmentSlotIndex);
                ItemStack gunItem = inventory.getItem(message.gunSlotIndex);
                IGun iGun = IGun.getIGunOrNull(gunItem);
                if (iGun != null) {
                    // 隴帶ｦ願球驕ｶ・ｯ隴ｬ・｡鬯ｪ遒√・闔会ｽｶ鬮槭・
                    if (iGun.hasAttachmentLock(gunItem)) {
                        return;
                    }
                    boolean allowAttachment = iGun.allowAttachment(gunItem, attachmentItem);
                    if (allowAttachment) {
                        // 闖ｴ・ｿ騾包ｽｨ鬩溷ｺ・ｻ・ｶ霑夲ｽｩ陷ｩ竏ｬ繝ｻ髴・ｽｫ騾ｧ繝ｻ謔・楜讓抵ｽｱ・ｻ陜吝・・ｼ迹堋遒∵直陞ｳ・｢隰鯉ｽｷ驕ｶ・ｯ闔ｨ・ｰ陷茨ｽ･騾ｧ繝ｻattachmentType
                        IAttachment iAttachment = IAttachment.getIAttachmentOrNull(attachmentItem);
                        if (iAttachment == null) {
                            return;
                        }
                        AttachmentType realType = iAttachment.getType(attachmentItem);
                        ItemStack oldAttachmentItem = iGun.getAttachment(gunItem, realType);
                        iGun.installAttachment(gunItem, attachmentItem);
                        ItemStack writtenAttachmentItem = iGun.getAttachment(gunItem, realType);
                        // 陋ｻ・ｷ隴・ｽｰ鬩溷ｺ・ｻ・ｶ隰ｨ・ｰ隰撰ｽｮ
                        AttachmentPropertyManager.postChangeEvent(player, gunItem);
                        inventory.setItem(message.attachmentSlotIndex, oldAttachmentItem);
                        // 陞ｯ繧域｣｡陷奇ｽｸ髴難ｽｽ騾ｧ繝ｻ蠑崎ｬ・ｽｩ陞ｳ・ｹ陟托ｽｹ陋ｹ・｣繝ｻ謔溽憎陷・ｽｺ隰・隴帷甥・ｭ莉呻ｽｼ・ｹ
                        if (realType == AttachmentType.EXTENDED_MAG) {
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
