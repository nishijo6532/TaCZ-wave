package com.tacz.guns.api.client.other;

import net.minecraft.client.Minecraft;
import net.minecraft.world.item.ItemStack;

/**
 * 用来在收物品时，让其保持一段时间渲染的接口
 */
public interface KeepingItemRenderer {
    /**
     * 物品保持渲染的时间
     *
     * @param itemStack 保持的物品
     * @param timeMs    时间，单位毫秒
     */
    void keep(ItemStack itemStack, long timeMs);

    /**
     * 获取当前主手正在渲染的物品
     */
    ItemStack getCurrentItem();

    /**
     * ItemInHandRenderer 通过 Mixin 的方式实现了此接口。
     * @return 返回 ItemInHandRenderer 实例
     */
    static KeepingItemRenderer getRenderer(){
        Object renderer = Minecraft.getInstance().getEntityRenderDispatcher().getItemInHandRenderer();
        if (renderer instanceof KeepingItemRenderer keepingItemRenderer) {
            return keepingItemRenderer;
        }
        return FallbackRenderer.INSTANCE;
    }

    final class FallbackRenderer implements KeepingItemRenderer {
        private static final FallbackRenderer INSTANCE = new FallbackRenderer();
        private ItemStack keepItem = ItemStack.EMPTY;
        private long keepTimeMs = 0L;
        private long keepTimestamp = 0L;

        private FallbackRenderer() {
        }

        @Override
        public void keep(ItemStack itemStack, long timeMs) {
            keepItem = itemStack;
            keepTimeMs = Math.max(0L, timeMs);
            keepTimestamp = System.currentTimeMillis();
        }

        @Override
        public ItemStack getCurrentItem() {
            if (!keepItem.isEmpty() && System.currentTimeMillis() - keepTimestamp < keepTimeMs) {
                return keepItem;
            }
            if (Minecraft.getInstance().player != null) {
                return Minecraft.getInstance().player.getMainHandItem();
            }
            return ItemStack.EMPTY;
        }
    }
}
