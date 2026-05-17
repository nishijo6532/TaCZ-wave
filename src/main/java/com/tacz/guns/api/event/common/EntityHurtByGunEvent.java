package com.tacz.guns.api.event.common;

import net.minecraft.resources.Identifier;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraftforge.eventbus.api.bus.EventBus;
import net.minecraftforge.eventbus.api.event.characteristic.Cancellable;
import net.minecraftforge.eventbus.api.event.MutableEvent;
import net.minecraftforge.eventbus.api.event.characteristic.SelfPosting;
import net.minecraftforge.fml.LogicalSide;
import org.apache.commons.lang3.tuple.Pair;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.ApiStatus.Obsolete;

import javax.annotation.Nullable;
import java.util.Optional;

/**
 * 鬨ｾ蠅難ｽｺ・ｽ魄溘・逕ｸ繝ｻ・ｫ髫ｴ・ｫ繝ｻ・ｪ髫ｴ・ｴ繝ｻ・ｰ髯昴・・ｻ蜻ｻ・ｽ・ｼ繝ｻ・ｹ髣費ｽｨ繝ｻ・､髯橸ｽｳ繝ｻ・ｳ髫ｴ魃会ｽｽ・ｶ鬮ｫ證ｦ・ｽ・ｦ髯ｷ・ｿ鬩｢讎企｣ｭ髣費｣ｰ陋滂ｽｶ繝ｻ・ｻ繝ｻ・ｶ
 */
public class EntityHurtByGunEvent extends MutableEvent implements KubeJSGunEventPoster<EntityHurtByGunEvent>, SelfPosting<EntityHurtByGunEvent> {
    public static final EventBus<EntityHurtByGunEvent> BUS = EventBus.create(EntityHurtByGunEvent.class);

    protected final Entity bullet;
    protected @Nullable Entity hurtEntity;
    protected @Nullable LivingEntity attacker;
    protected Identifier gunId;
    protected Identifier gunDisplayId;
    protected float baseAmount;
    protected DamageSource nonApPartDamageSource;
    protected DamageSource apPartDamageSource;
    protected boolean isHeadShot;
    protected float headshotMultiplier;
    protected final LogicalSide logicalSide;

    @ApiStatus.Internal
    protected EntityHurtByGunEvent(Entity bullet, @Nullable Entity hurtEntity, @Nullable LivingEntity attacker,
                                   Identifier gunId, Identifier gunDisplayId,
                                   float baseAmount, @Nullable Pair<DamageSource, DamageSource> sources, boolean isHeadShot,
                                   float headshotMultiplier, LogicalSide logicalSide) {
        this.bullet = bullet;
        this.hurtEntity = hurtEntity;
        this.attacker = attacker;
        this.gunId = gunId;
        this.baseAmount = baseAmount;
        this.nonApPartDamageSource = Optional.ofNullable(sources).map(Pair::getLeft).orElse(null);
        this.apPartDamageSource = Optional.ofNullable(sources).map(Pair::getRight).orElse(null);
        this.isHeadShot = isHeadShot;
        this.headshotMultiplier = headshotMultiplier;
        this.logicalSide = logicalSide;
    }

    /**
     * 髯橸ｽｳ隶楢ｲｻ・ｽ・ｽ霓｣莨懶ｽ･・ｳ髯具ｽｻ繝ｻ・ｰ髫ｴ・ｫ繝ｻ・ｪ髯ｷ繝ｻ・ｽ・ｻ郢晢ｽｻ陟包ｽ｡繝ｻ・ｼ繝ｻ・､髯橸ｽｳ繝ｻ・ｳ髯具ｽｻ繝ｻ・､髯橸ｽｳ陞｢・ｼ霎ｯ證ｮ蝗薙・・ｦ髯ｷ・ｿ鬩｢讎企｣ｭ髣費｣ｰ陋滂ｽｶ繝ｻ・ｻ繝ｻ・ｶ郢晢ｽｻ隰疲ｺｷ・ｺ繝ｻ閼ゅ・・･鬮ｫ・ｶ繝ｻ・ｾ鬩励ｑ・ｽ・ｮ髫ｴ・ｫ繝ｻ・ｪ髯ｷ繝ｻ・ｽ・ｻ鬨ｾ・ｧ郢晢ｽｻ繝ｻ・ｼ繝ｻ・､髯橸ｽｳ繝ｻ・ｳ髯橸ｽｻ隶鍋腸ﾂ繝ｻ・ｧ
     */
    public static class Pre extends EntityHurtByGunEvent  implements Cancellable{
        @ApiStatus.Internal
        public Pre(Entity bullet, @Nullable Entity hurtEntity, @Nullable LivingEntity attacker,
                   Identifier gunId, Identifier gunDisplayId,
                   float amount, @Nullable Pair<DamageSource, DamageSource> sources,
                   boolean isHeadShot, float headshotMultiplier, LogicalSide logicalSide) {
            super(bullet, hurtEntity, attacker, gunId, gunDisplayId, amount, sources, isHeadShot, headshotMultiplier, logicalSide);
            this.headshotMultiplier = headshotMultiplier;
            postEventToKubeJS(this);
        }

        public final void setHurtEntity(@Nullable Entity hurtEntity) {
            this.hurtEntity = hurtEntity;
        }

        public final void setAttacker(@Nullable LivingEntity attacker) {
            this.attacker = attacker;
        }

        public final void setGunId(Identifier gunId) {
            this.gunId = gunId;
        }

        public final void setBaseAmount(float baseAmount) {
            this.baseAmount = baseAmount;
        }

        public final void setDamageSource(GunDamageSourcePart part, DamageSource value) {
            if (logicalSide.isClient()) {
                throw new UnsupportedOperationException("DamageSource about gun hit is not available on client side!");
            }
            if (part == GunDamageSourcePart.ARMOR_PIERCING) {
                apPartDamageSource = value;
            } else {
                nonApPartDamageSource = value;
            }
        }

        public final void setHeadshot(boolean headshot) {
            this.isHeadShot = headshot;
        }

        public final void setHeadshotMultiplier(float headshotMultiplier) {
            this.headshotMultiplier = headshotMultiplier;
        }
    }

    /**
     * 髯橸ｽｳ隶楢ｲｻ・ｽ・ｽ霓｣莨懶ｽ･・ｳ髯具ｽｻ繝ｻ・ｰ髫ｴ・ｫ繝ｻ・ｪ髯ｷ繝ｻ・ｽ・ｻ郢晢ｽｻ陟包ｽ｡繝ｻ・ｼ繝ｻ・､髯橸ｽｳ繝ｻ・ｳ髯具ｽｻ繝ｻ・､髯橸ｽｳ陞溘ｑ・ｽ・ｻ隰撰ｽｺ隰ｫ螟頑割郢晢ｽｻ繝ｻ・ｲ繝ｻ・｡髫ｴ蟶ｷ蛻､繝ｻ・ｭ繝ｻ・ｻ髣費｣ｰ繝ｻ・｡髯ｷ・ｷ隰・・・ｽ・ｧ繝ｻ・ｦ髯ｷ・ｿ鬩｢讎企｣ｭ髣費｣ｰ陋滂ｽｶ繝ｻ・ｻ繝ｻ・ｶ
     * @see EntityKillByGunEvent 髯橸ｽｳ隶楢ｲｻ・ｽ・ｽ霓｣莨懶ｽｱ蜑ｰ・ｭ・ｫ繝ｻ・ｪ髯ｷ繝ｻ・ｽ・ｻ鬮｢・ｾ繝ｻ・ｴ髮弱・・ｽ・ｻ髫ｴ魃会ｽｽ・ｶ鬮ｫ證ｦ・ｽ・ｦ髯ｷ・ｿ鬩｢讎企｣ｭ髣費｣ｰ陋滂ｽｶ繝ｻ・ｻ繝ｻ・ｶ
     */
    public static class Post extends EntityHurtByGunEvent {
        @ApiStatus.Internal
        public Post(Entity bullet, @Nullable Entity hurtEntity, @Nullable LivingEntity attacker,
                    Identifier gunId, Identifier gunDisplayId,
                    float amount, @Nullable Pair<DamageSource, DamageSource> sources,
                    boolean isHeadShot, float headshotMultiplier, LogicalSide logicalSide) {
            super(bullet, hurtEntity, attacker, gunId, gunDisplayId, amount, sources, isHeadShot, headshotMultiplier, logicalSide);
            postEventToKubeJS(this);
        }
    }

    public Entity getBullet() {
        return bullet;
    }

    @Nullable
    public Entity getHurtEntity() {
        return hurtEntity;
    }

    @Nullable
    public LivingEntity getAttacker() {
        return attacker;
    }

    public Identifier getGunId() {
        return gunId;
    }

    public Identifier getGunDisplayId() {
        return gunDisplayId;
    }

    @Obsolete
    public float getAmount() {
        return baseAmount * headshotMultiplier;
    }

    public float getBaseAmount() {
        return baseAmount;
    }

    public DamageSource getDamageSource(GunDamageSourcePart part) {
        if (logicalSide.isClient()) {
            throw new UnsupportedOperationException("DamageSource about gun hit is not available on client side!");
        }
        return part == GunDamageSourcePart.ARMOR_PIERCING ? apPartDamageSource : nonApPartDamageSource;
    }

    public float getHeadshotMultiplier() {
        return headshotMultiplier;
    }

    public boolean isHeadShot() {
        return isHeadShot;
    }

    public LogicalSide getLogicalSide() {
        return logicalSide;
    }

    @Override
    public EventBus<EntityHurtByGunEvent> getDefaultBus() {
        return BUS;
    }
}

