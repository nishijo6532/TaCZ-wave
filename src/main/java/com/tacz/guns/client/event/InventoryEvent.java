package com.tacz.guns.client.event;

import com.tacz.guns.GunMod;
import com.tacz.guns.api.client.event.SwapItemWithOffHand;
import com.tacz.guns.api.client.gameplay.IClientPlayerGunOperator;
import com.tacz.guns.api.item.IAnimationItem;
import com.tacz.guns.api.item.IGun;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ClientPlayerNetworkEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.listener.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(value = Dist.CLIENT, modid = GunMod.MOD_ID)
public class InventoryEvent {
    // 逕ｨ莠主・譫ｪ騾ｻ霎・
    private static int oldHotbarSelected = -1;
    private static ItemStack oldHotbarSelectItem = ItemStack.EMPTY;

    @SubscribeEvent
    public static void onPlayerChangeSelect(TickEvent.ClientTickEvent.Pre event) {
        onPlayerChangeSelect();
    }

    @SubscribeEvent
    public static void onPlayerChangeSelect(TickEvent.ClientTickEvent.Post event) {
        onPlayerChangeSelect();
    }

    private static void onPlayerChangeSelect() {
        LocalPlayer player = Minecraft.getInstance().player;
        if (player == null) {
            return;
        }
        Inventory inventory = player.getInventory();
        int selectedSlot = inventory.getSelectedSlot();
        // 邇ｩ螳ｶ蛻・困騾我ｸｭ譯・噪諠・・
        if (oldHotbarSelected != selectedSlot) {
            if (oldHotbarSelected == -1) {
                IClientPlayerGunOperator.fromLocalPlayer(player).draw(ItemStack.EMPTY);
            } else {
                IClientPlayerGunOperator.fromLocalPlayer(player).draw(inventory.getItem(oldHotbarSelected));
            }
            oldHotbarSelected = selectedSlot;
            oldHotbarSelectItem = inventory.getItem(selectedSlot).copy();
            return;
        }
        // 邇ｩ螳ｶ騾我ｸｭ逧・黄蜩∵隼蜿倡噪諠・・
        ItemStack currentItem = inventory.getItem(selectedSlot);
        if (currentItem.getItem() instanceof IAnimationItem item ) {
            if (!item.isSame(oldHotbarSelectItem, currentItem)) {
                IClientPlayerGunOperator.fromLocalPlayer(player).draw(oldHotbarSelectItem);
            }
        } else {
            if (!ItemStack.matches(oldHotbarSelectItem, currentItem)) {
                IClientPlayerGunOperator.fromLocalPlayer(player).draw(oldHotbarSelectItem);
            }
        }

        if (!ItemStack.matches(oldHotbarSelectItem, currentItem)) {
            oldHotbarSelectItem = currentItem.copy();
        }
    }

    @SubscribeEvent
    public static void onPlayerSwapMainHand(SwapItemWithOffHand event) {
        LocalPlayer player = Minecraft.getInstance().player;
        if (player == null) {
            return;
        }
        IClientPlayerGunOperator.fromLocalPlayer(player).draw(player.getMainHandItem());
    }

    @SubscribeEvent
    public static void onPlayerLoggedOut(ClientPlayerNetworkEvent.LoggingOut event) {
        // 遖ｻ蠑貂ｸ謌乗慮驥咲ｽｮ螳｢謌ｷ遶ｯ draw 迥ｶ諤・
        oldHotbarSelected = -1;
        oldHotbarSelectItem = ItemStack.EMPTY;
    }

    private static boolean isSame(ItemStack i, ItemStack j) {
        IGun iGun1 = IGun.getIGunOrNull(i);
        IGun iGun2 = IGun.getIGunOrNull(j);
        if (iGun1 != null && iGun2 != null) {
            return iGun1.getGunId(i).equals(iGun2.getGunId(j));
        }
        if (i.isEmpty() || j.isEmpty()) {
            return i.isEmpty() && j.isEmpty();
        }
        return ItemStack.matches(i, j);
    }
}

