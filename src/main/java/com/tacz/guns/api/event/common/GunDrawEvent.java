package com.tacz.guns.api.event.common;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.eventbus.api.event.MutableEvent;
import net.minecraftforge.fml.LogicalSide;

/**
 * 騾墓ｺｽ鮟・堕陝句玄蟲ｩ隰撰ｽ｢隴ｫ・ｪ隴ｴ・ｰ陟托ｽｹ髣包ｽｯ隴鯉ｽｶ髫暦ｽｦ陷ｿ驢榊飭闔蛟ｶ・ｻ・ｶ邵ｲ繝ｻ
 */
public class GunDrawEvent extends MutableEvent implements KubeJSGunEventPoster<GunDrawEvent>{
    private final LivingEntity entity;
    private final ItemStack previousGunItem;
    private final ItemStack currentGunItem;
    private final LogicalSide logicalSide;

    public GunDrawEvent(LivingEntity entity, ItemStack previousGunItem, ItemStack currentGunItem, LogicalSide side) {
        this.entity = entity;
        this.previousGunItem = previousGunItem;
        this.currentGunItem = currentGunItem;
        this.logicalSide = side;
        postEventToKubeJS(this);
    }

    public LivingEntity getEntity() {
        return entity;
    }

    public ItemStack getPreviousGunItem() {
        return previousGunItem;
    }

    public ItemStack getCurrentGunItem() {
        return currentGunItem;
    }

    public LogicalSide getLogicalSide() {
        return logicalSide;
    }
}

