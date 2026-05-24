package com.tacz.guns.util;

import com.tacz.guns.config.util.HeadShotAABBConfigRead;
import com.tacz.guns.entity.EntityKineticBullet;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.registries.ForgeRegistries;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Predicate;

public class EntityUtil {
    private static final Predicate<Entity> PROJECTILE_TARGETS = input -> input != null && input.isPickable() && !input.isSpectator();
    private static final Map<EntityType<?>, Optional<AABB>> HEADSHOT_AABB_CACHE = new HashMap<>();

    public static void clearHeadshotAabbCache() {
        HEADSHOT_AABB_CACHE.clear();
    }

    @Nullable
    public static EntityKineticBullet.EntityResult findEntityOnPath(Projectile bulletEntity, Vec3 startVec, Vec3 endVec) {

        Vec3 hitVec = null;
        Entity hitEntity = null;
        boolean headshot = false;
        // 闔ｷ蜿門ｭ仙ｼｹ tick 霍ｯ蠕・ｸ頑園譛臥噪螳樔ｽ・
        List<Entity> entities = bulletEntity.level().getEntities(bulletEntity, bulletEntity.getBoundingBox().expandTowards(bulletEntity.getDeltaMovement()).inflate(1.0), PROJECTILE_TARGETS);
        double closestDistance = Double.MAX_VALUE;
        Entity owner = bulletEntity.getOwner();
        for (Entity entity : entities) {
            // 遖∵ｭ｢蟇ｹ閾ｪ蟾ｱ騾謌蝉ｼ､螳ｳ・亥ｦよ怏髴隕∝庄莉･蠅槫刈 Config 蠑蜷ｯ蟇ｹ閾ｪ蟾ｱ逧・ｼ､螳ｳ・・
            if (!entity.equals(owner)) {
                // 蟆・・譌隗・・蟾ｱ逧・ｽｽ蜈ｷ蜥瑚ｯ･霓ｽ蜈ｷ荳顔噪蜈ｶ莉紋ｹ伜ｮ｢
                if (owner != null && entity.isPassengerOfSameVehicle(owner)) {
                    continue;
                }
                EntityKineticBullet.EntityResult result = getHitResult(bulletEntity, entity, startVec, endVec);
                if (result == null) {
                    continue;
                }
                Vec3 hitPos = result.getHitPos();
                double distanceToHit = startVec.distanceTo(hitPos);
                if (entity.isAlive()) {
                    if (distanceToHit < closestDistance) {
                        hitVec = hitPos;
                        hitEntity = entity;
                        closestDistance = distanceToHit;
                        headshot = result.isHeadshot();
                    }
                }
            }
        }
        return hitEntity != null ? new EntityKineticBullet.EntityResult(hitEntity, hitVec, headshot) : null;
    }

    @NotNull
    public static List<EntityKineticBullet.EntityResult> findEntitiesOnPath(Projectile bulletEntity, Vec3 startVec, Vec3 endVec) {
        List<EntityKineticBullet.EntityResult> hitEntities = new ArrayList<>();
        List<Entity> entities = bulletEntity.level().getEntities(bulletEntity, bulletEntity.getBoundingBox().expandTowards(bulletEntity.getDeltaMovement()).inflate(1.0), PROJECTILE_TARGETS);
        Entity owner = bulletEntity.getOwner();
        for (Entity entity : entities) {
            if (!entity.equals(owner)) {
                if (owner != null && entity.equals(owner.getVehicle())) {
                    continue;
                }
                EntityKineticBullet.EntityResult result = getHitResult(bulletEntity, entity, startVec, endVec);
                if (result == null) {
                    continue;
                }
                if (entity.isAlive()) {
                    hitEntities.add(result);
                }
            }
        }
        return hitEntities;
    }

    @Nullable
    protected static EntityKineticBullet.EntityResult getHitResult(Projectile bulletEntity, Entity entity, Vec3 startVec, Vec3 endVec) {
        AABB boundingBox = HitboxHelper.getFixedBoundingBox(entity, bulletEntity.getOwner());
        // 隶｡邂怜ｰ・ｺｿ荳主ｮ樔ｽ・boundingBox 逧・ｺ､轤ｹ
        Vec3 hitPos = boundingBox.clip(startVec, endVec).orElse(null);
        // 辷・､ｴ蛻､螳・
        if (hitPos == null) {
            return null;
        }
        Vec3 hitBoxPos = hitPos.subtract(entity.position());
        AABB aabb = getHeadshotAabb(entity);
        // 譛蛾・鄂ｮ逧・ｰ・畑驟咲ｽｮ
        if (aabb != null) {
            return new EntityKineticBullet.EntityResult(entity, hitPos, aabb.contains(hitBoxPos));
        }
        // 豐｡譛蛾・鄂ｮ逧・ｻ倩ｮ､扈吩ｸ荳ｪ
        boolean headshot = false;
        float eyeHeight = entity.getEyeHeight();
        if ((eyeHeight - 0.25) < hitBoxPos.y && hitBoxPos.y < (eyeHeight + 0.25)) {
            headshot = true;
        }
        return new EntityKineticBullet.EntityResult(entity, hitPos, headshot);
    }

    @Nullable
    private static AABB getHeadshotAabb(Entity entity) {
        EntityType<?> type = entity.getType();
        Optional<AABB> cached = HEADSHOT_AABB_CACHE.get(type);
        if (cached != null) {
            return cached.orElse(null);
        }
        Identifier entityId = ForgeRegistries.ENTITY_TYPES.getKey(type);
        AABB aabb = entityId == null ? null : HeadShotAABBConfigRead.getAABB(entityId);
        HEADSHOT_AABB_CACHE.put(type, Optional.ofNullable(aabb));
        return aabb;
    }
}

