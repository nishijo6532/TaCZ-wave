package com.tacz.guns.api.event.server;

import com.tacz.guns.api.event.common.KubeJSGunEventPoster;
import com.tacz.guns.entity.EntityKineticBullet;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraftforge.eventbus.api.bus.EventBus;
import net.minecraftforge.eventbus.api.event.MutableEvent;
import net.minecraftforge.eventbus.api.event.characteristic.Cancellable;
import net.minecraftforge.eventbus.api.event.characteristic.SelfPosting;

/**
 * 鬯ｮ・ｯ隴擾ｽｴ郢晢ｽｻ郢晢ｽｻ繝ｻ・ｻ髯ｷ・ｻ繝ｻ・ｻ郢晢ｽｻ繝ｻ・ｽ郢晢ｽｻ繝ｻ・ｼ驛｢譎｢・ｽ・ｻ郢晢ｽｻ繝ｻ・ｹ鬯ｮ・ｯ繝ｻ・ｷ驛｢譎｢・ｽ・ｻ郢晢ｽｻ繝ｻ・ｽ郢晢ｽｻ繝ｻ・ｻ鬯ｮ・｣陋ｹ繝ｻ・ｽ・ｽ繝ｻ・ｳ驛｢譎｢・ｽ・ｻ郢晢ｽｻ繝ｻ・ｭ鬯ｮ・ｫ繝ｻ・ｴ驛｢譎｢・ｽ・ｻ郢晢ｽｻ繝ｻ・ｽ郢晢ｽｻ繝ｻ・ｹ鬯ｮ・ｯ隲幢ｽｶ繝ｻ・ｽ繝ｻ・ｮ鬯ｩ髦ｪ繝ｻ繝ｻ・ｽ繝ｻ・ｲ鬮ｫ・ｲ繝ｻ・ｷ郢晢ｽｻ繝ｻ・ｮ鬯ｯ・ｮ繝ｻ・ｫ髫ｴ莨夲ｽｽ・ｦ郢晢ｽｻ繝ｻ・ｽ郢晢ｽｻ繝ｻ・ｦ鬯ｮ・ｯ繝ｻ・ｷ郢晢ｽｻ繝ｻ・ｿ鬯ｯ・ｩ繝ｻ・｢髫ｶ諠ｹ・ｼ繝ｻ・ｽ・｣繝ｻ・ｭ鬯ｮ・｣髮具ｽｻ繝ｻ・｣繝ｻ・ｰ鬮ｯ蛹ｺ・ｻ繧托ｽｽ・ｽ繝ｻ・ｶ驛｢譎｢・ｽ・ｻ郢晢ｽｻ繝ｻ・ｻ驛｢譎｢・ｽ・ｻ郢晢ｽｻ繝ｻ・ｶ鬩幢ｽ｢隴趣ｽ｢繝ｻ・ｽ繝ｻ・ｻ鬮ｫ・ｶ陷ｷ・ｩ繝ｻ・ｸ繝ｻ・ｻ郢晢ｽｻ繝ｻ・ｲ郢晢ｽｻ繝ｻ・ｼ鬯ｮ・ｯ繝ｻ・ｷ髯樊ｻゑｽｽ・ｧ郢晢ｽｻ繝ｻ・ｰ郢晢ｽｻ繝ｻ・ｺ驛｢譎｢・ｽ・ｻ郢晢ｽｻ繝ｻ・ｻ鬩幢ｽ｢隴趣ｽ｢繝ｻ・ｽ繝ｻ・ｻ鬮ｫ・ｲ繝ｻ・ｰ郢晢ｽｻ繝ｻ・ｰ鬯ｮ・ｫ繝ｻ・ｴ髯晢ｽｶ繝ｻ・ｶ郢晢ｽｻ繝ｻ・ｦ鬯ｯ莨慊・｡陝ｶ・･鬯ｯ・ｩ陋ｹ繝ｻ・ｽ・ｽ繝ｻ・ｶ驛｢譎｢・ｽ・ｻ郢晢ｽｻ繝ｻ・ｯ鬯ｯ・ｮ繝ｻ・ｫ髫ｴ莨夲ｽｽ・ｦ郢晢ｽｻ繝ｻ・ｽ郢晢ｽｻ繝ｻ・ｦ鬯ｮ・ｯ繝ｻ・ｷ郢晢ｽｻ繝ｻ・ｿ鬩幢ｽ｢隴趣ｽ｢繝ｻ・ｽ繝ｻ・ｻ
 */
public class AmmoHitBlockEvent extends MutableEvent implements KubeJSGunEventPoster<AmmoHitBlockEvent>, Cancellable, SelfPosting<AmmoHitBlockEvent> {
    public static final EventBus<AmmoHitBlockEvent> BUS = EventBus.create(AmmoHitBlockEvent.class);

    private final Level level;
    private final BlockHitResult hitResult;
    private final BlockState state;
    private final EntityKineticBullet ammo;

    public AmmoHitBlockEvent(Level level, BlockHitResult hitResult, BlockState state, EntityKineticBullet ammo) {
        this.level = level;
        this.hitResult = hitResult;
        this.state = state;
        this.ammo = ammo;
        postServerEventToKubeJS(this);
    }
    public Level getLevel() {
        return level;
    }

    public BlockHitResult getHitResult() {
        return hitResult;
    }

    public BlockState getState() {
        return state;
    }

    public EntityKineticBullet getAmmo() {
        return ammo;
    }

    @Override
    public EventBus<AmmoHitBlockEvent> getDefaultBus() {
        return BUS;
    }
}

