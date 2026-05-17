package com.tacz.guns.api.event.common;

import net.minecraft.resources.Identifier;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraftforge.eventbus.api.bus.EventBus;
import net.minecraftforge.eventbus.api.event.MutableEvent;
import net.minecraftforge.eventbus.api.event.characteristic.SelfPosting;
import net.minecraftforge.fml.LogicalSide;
import org.apache.commons.lang3.tuple.Pair;

import javax.annotation.Nullable;
import java.util.Optional;

/**
 * 騾墓ｺｽ鮟・甸・ｫ隴ｫ・ｪ隴ｴ・ｰ陝・ｻ呻ｽｼ・ｹ陷・ｽｻ隴堋隴鯉ｽｶ髫暦ｽｦ陷ｿ驢榊飭闔蛟ｶ・ｻ・ｶ
 */
public class EntityKillByGunEvent extends MutableEvent implements KubeJSGunEventPoster<EntityKillByGunEvent>, SelfPosting<EntityKillByGunEvent> {
    public static final EventBus<EntityKillByGunEvent> BUS = EventBus.create(EntityKillByGunEvent.class);

    private final Entity bullet;
    private final @Nullable LivingEntity killedEntity;
    private final @Nullable LivingEntity attacker;
    private final Identifier gunId;
    private final Identifier gunDisplayId;
    private final float baseDamage;
    private final DamageSource nonApPartDamageSource;
    private final DamageSource apPartDamageSource;
    private final boolean isHeadShot;
    private final float headshotMultiplier;
    private final LogicalSide logicalSide;

    public EntityKillByGunEvent(Entity bullet, @Nullable LivingEntity hurtEntity, @Nullable LivingEntity attacker,
                                Identifier gunId, Identifier gunDisplayId, float baseDamage, @Nullable Pair<DamageSource, DamageSource> sources,
                                boolean isHeadShot, float headshotMultiplier, LogicalSide logicalSide) {
        this.bullet = bullet;
        this.killedEntity = hurtEntity;
        this.attacker = attacker;
        this.gunId = gunId;
        this.gunDisplayId = gunDisplayId;
        this.baseDamage = baseDamage;
        this.nonApPartDamageSource = Optional.ofNullable(sources).map(Pair::getLeft).orElse(null);
        this.apPartDamageSource = Optional.ofNullable(sources).map(Pair::getRight).orElse(null);
        this.isHeadShot = isHeadShot;
        this.headshotMultiplier = headshotMultiplier;
        this.logicalSide = logicalSide;
        postEventToKubeJS(this);
    }

    /**
     * 陜ｨ・ｨ鬨ｾ・ｻ髴主､ｧ・ｮ・｢隰鯉ｽｷ驕ｶ・ｯ闕ｳ蝣ｺ・ｿ譎・ｽｯ竏ｬ繝ｻ騾包ｽｨ
     */
    public Entity getBullet() {
        return bullet;
    }

    @Nullable
    public LivingEntity getKilledEntity() {
        return killedEntity;
    }

    @Nullable
    public LivingEntity getAttacker() {
        return attacker;
    }

    public Identifier getGunId() {
        return gunId;
    }

    public float getBaseDamage() {
        return baseDamage;
    }

    public DamageSource getDamageSource(GunDamageSourcePart part) {
        if (logicalSide.isClient()) {
            throw new UnsupportedOperationException("DamageSource about gun hit is not available on client side!");
        }
        return part == GunDamageSourcePart.ARMOR_PIERCING ? apPartDamageSource : nonApPartDamageSource;
    }

    public boolean isHeadShot() {
        return isHeadShot;
    }

    public float getHeadshotMultiplier() {
        return headshotMultiplier;
    }

    public LogicalSide getLogicalSide() {
        return logicalSide;
    }

    public Identifier getGunDisplayId() {
        return gunDisplayId;
    }

    @Override
    public EventBus<EntityKillByGunEvent> getDefaultBus() {
        return BUS;
    }
}
