package com.tacz.guns.util.block;

import com.google.common.collect.Sets;
import com.tacz.guns.config.common.AmmoConfig;
import com.tacz.guns.util.HitboxHelper;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.PrimedTnt;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import com.tacz.guns.util.EnchantmentCompat;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Explosion;
import net.minecraft.world.level.ExplosionDamageCalculator;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseFireBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

public class ProjectileExplosion implements Explosion {
    private static final ExplosionDamageCalculator DEFAULT_CONTEXT = new ExplosionDamageCalculator();
    private final ServerLevel level;
    private final double x;
    private final double y;
    private final double z;
    private final float power;
    private final float radius;
    private final boolean knockback;
    private final Entity owner;
    private final Entity exploder;
    private final ExplosionDamageCalculator damageCalculator;
    private final Explosion.BlockInteraction mode;
    private final boolean fire;
    private final DamageSource damageSource;
    private final List<BlockPos> toBlow = new ArrayList<>();
    private final Map<Player, Vec3> hitPlayers = new HashMap<>();
    private final List<LivingHit> hitLivingEntities = new ArrayList<>();

    public ProjectileExplosion(
            Level level,
            Entity owner,
            Entity exploder,
            @Nullable DamageSource source,
            @Nullable ExplosionDamageCalculator damageCalculator,
            double x,
            double y,
            double z,
            float power,
            float radius,
            boolean knockback,
            Explosion.BlockInteraction mode
    ) {
        if (!(level instanceof ServerLevel serverLevel)) {
            throw new IllegalArgumentException("ProjectileExplosion requires ServerLevel");
        }
        this.level = serverLevel;
        this.x = x;
        this.y = y;
        this.z = z;
        this.power = power;
        this.radius = radius;
        this.owner = owner;
        this.exploder = exploder;
        this.damageCalculator = damageCalculator == null ? DEFAULT_CONTEXT : damageCalculator;
        this.knockback = knockback;
        this.mode = mode;
        this.fire = AmmoConfig.EXPLOSIVE_AMMO_FIRE.get();
        this.damageSource = source == null ? this.level.damageSources().explosion(this) : source;
    }

    public void explode() {
        this.level.gameEvent(this.exploder, GameEvent.EXPLODE, BlockPos.containing(this.x, this.y, this.z));
        Set<BlockPos> set = Sets.newHashSet();
        int i = 16;

        for (int x = 0; x < i; ++x) {
            for (int y = 0; y < i; ++y) {
                for (int z = 0; z < i; ++z) {
                    if (x == 0 || x == i - 1 || y == 0 || y == i - 1 || z == 0 || z == i - 1) {
                        double d0 = ((float) x / (i - 1) * 2.0F - 1.0F);
                        double d1 = ((float) y / (i - 1) * 2.0F - 1.0F);
                        double d2 = ((float) z / (i - 1) * 2.0F - 1.0F);
                        double d3 = Math.sqrt(d0 * d0 + d1 * d1 + d2 * d2);
                        d0 /= d3;
                        d1 /= d3;
                        d2 /= d3;
                        float f = this.radius * (0.7F + this.level.getRandom().nextFloat() * 0.6F);
                        double blockX = this.x;
                        double blockY = this.y;
                        double blockZ = this.z;

                        for (float f1 = 0.3F; f > 0.0F; f -= 0.22500001F) {
                            BlockPos pos = BlockPos.containing(blockX, blockY, blockZ);
                            BlockState blockState = this.level.getBlockState(pos);
                            FluidState fluidState = this.level.getFluidState(pos);
                            if (!this.level.isInWorldBounds(pos)) {
                                break;
                            }

                            Optional<Float> optional = this.damageCalculator.getBlockExplosionResistance(this, this.level, pos, blockState, fluidState);
                            if (optional.isPresent()) {
                                f -= (optional.get() + f1) * f1;
                            }

                            if (f > 0.0F && this.damageCalculator.shouldBlockExplode(this, this.level, pos, blockState, f)) {
                                set.add(pos);
                            }

                            blockX += d0 * (double) f1;
                            blockY += d1 * (double) f1;
                            blockZ += d2 * (double) f1;
                        }
                    }
                }
            }
        }

        this.toBlow.addAll(set);
        float r = this.radius;
        int minX = Mth.floor(this.x - (double) r - 1.0D);
        int maxX = Mth.floor(this.x + (double) r + 1.0D);
        int minY = Mth.floor(this.y - (double) r - 1.0D);
        int maxY = Mth.floor(this.y + (double) r + 1.0D);
        int minZ = Mth.floor(this.z - (double) r - 1.0D);
        int maxZ = Mth.floor(this.z + (double) r + 1.0D);
        r *= 2;
        List<Entity> entities = this.level.getEntities(this.exploder, new AABB(minX, minY, minZ, maxX, maxY, maxZ));
        net.minecraftforge.event.ForgeEventFactory.onExplosionDetonate(this.level, this, this.toBlow, entities, r);
        Vec3 explosionPos = new Vec3(this.x, this.y, this.z);

        for (Entity entity : entities) {
            if (entity.ignoreExplosion(this)) {
                continue;
            }

            AABB boundingBox = HitboxHelper.getFixedBoundingBox(entity, this.owner);
            BlockHitResult result;
            double strength;
            double deltaX;
            double deltaY;
            double deltaZ;
            double minDistance = r;

            Vec3[] d = new Vec3[15];

            if (!(entity instanceof LivingEntity)) {
                strength = Math.sqrt(entity.distanceToSqr(explosionPos)) * 2 / r;
                deltaX = entity.getX() - this.x;
                deltaY = (entity instanceof PrimedTnt ? entity.getY() : entity.getEyeY()) - this.y;
                deltaZ = entity.getZ() - this.z;
            } else {
                deltaX = (boundingBox.maxX + boundingBox.minX) / 2;
                deltaY = (boundingBox.maxY + boundingBox.minY) / 2;
                deltaZ = (boundingBox.maxZ + boundingBox.minZ) / 2;
                d[0] = new Vec3(boundingBox.minX, boundingBox.minY, boundingBox.minZ);
                d[1] = new Vec3(boundingBox.minX, boundingBox.minY, boundingBox.maxZ);
                d[2] = new Vec3(boundingBox.minX, boundingBox.maxY, boundingBox.minZ);
                d[3] = new Vec3(boundingBox.maxX, boundingBox.minY, boundingBox.minZ);
                d[4] = new Vec3(boundingBox.minX, boundingBox.maxY, boundingBox.maxZ);
                d[5] = new Vec3(boundingBox.maxX, boundingBox.minY, boundingBox.maxZ);
                d[6] = new Vec3(boundingBox.maxX, boundingBox.maxY, boundingBox.minZ);
                d[7] = new Vec3(boundingBox.maxX, boundingBox.maxY, boundingBox.maxZ);
                d[8] = new Vec3(boundingBox.minX, deltaY, deltaZ);
                d[9] = new Vec3(boundingBox.maxX, deltaY, deltaZ);
                d[10] = new Vec3(deltaX, boundingBox.minY, deltaZ);
                d[11] = new Vec3(deltaX, boundingBox.maxY, deltaZ);
                d[12] = new Vec3(deltaX, deltaY, boundingBox.minZ);
                d[13] = new Vec3(deltaX, deltaY, boundingBox.maxZ);
                d[14] = new Vec3(deltaX, deltaY, deltaZ);
                for (int s = 0; s < 15; s++) {
                    result = BlockRayTrace.rayTraceBlocks(this.level, new ClipContext(explosionPos, d[s], ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, this.exploder));
                    minDistance = (result.getType() != BlockHitResult.Type.BLOCK) ? Math.min(minDistance, explosionPos.distanceTo(d[s])) : minDistance;
                }
                strength = minDistance * 2 / r;
                deltaX -= this.x;
                deltaY -= this.y;
                deltaZ -= this.z;
            }

            if (strength > 1.0D) {
                continue;
            }

            double distanceToExplosion = Math.sqrt(deltaX * deltaX + deltaY * deltaY + deltaZ * deltaZ);

            if (distanceToExplosion != 0.0D) {
                deltaX /= distanceToExplosion;
                deltaY /= distanceToExplosion;
                deltaZ /= distanceToExplosion;
            }

            double damage = 1.0D - strength;
            float damageAmount = (float) damage * this.power;
            boolean wasAlive = entity instanceof LivingEntity livingEntity && !livingEntity.isDeadOrDying();
            entity.hurt(this.damageSource, damageAmount);
            if (wasAlive && entity instanceof LivingEntity livingEntity) {
                this.hitLivingEntities.add(new LivingHit(livingEntity, damageAmount, livingEntity.isDeadOrDying()));
            }

            if (entity instanceof LivingEntity) {
                damage = EnchantmentCompat.getExplosionKnockbackAfterDampener((LivingEntity) entity, damage);
            }

            float multiplier = this.power * r / 500;
            if (AmmoConfig.EXPLOSIVE_AMMO_KNOCK_BACK.get() && this.knockback) {
                entity.setDeltaMovement(entity.getDeltaMovement().add(deltaX * damage * multiplier, deltaY * damage * multiplier, deltaZ * damage * multiplier));
                if (entity instanceof Player player) {
                    if (!player.isSpectator() && (!player.isCreative() || !player.getAbilities().flying)) {
                        this.hitPlayers.put(player, new Vec3(deltaX * damage * multiplier, deltaY * damage * multiplier, deltaZ * damage * multiplier));
                    }
                }
            }
        }
    }

    public void finalizeExplosion(boolean spawnParticles) {
        if (this.mode != Explosion.BlockInteraction.KEEP) {
            for (BlockPos pos : this.toBlow) {
                this.level.getBlockState(pos).onExplosionHit(this.level, pos, this, (stack, position) -> Block.popResource(this.level, position, stack));
            }
        }
        if (this.fire) {
            for (BlockPos pos : this.toBlow) {
                if (this.level.getRandom().nextInt(3) == 0 && this.level.getBlockState(pos).isAir() && this.level.getBlockState(pos.below()).isSolidRender()) {
                    this.level.setBlockAndUpdate(pos, BaseFireBlock.getState(this.level, pos));
                }
            }
        }
    }

    public List<BlockPos> getToBlow() {
        return this.toBlow;
    }

    public void clearToBlow() {
        this.toBlow.clear();
    }

    public Map<Player, Vec3> getHitPlayers() {
        return this.hitPlayers;
    }

    public List<LivingHit> getHitLivingEntities() {
        return this.hitLivingEntities;
    }

    public DamageSource getDamageSource() {
        return this.damageSource;
    }

    @Override
    public ServerLevel level() {
        return this.level;
    }

    @Override
    public Explosion.BlockInteraction getBlockInteraction() {
        return this.mode;
    }

    @Override
    public @Nullable LivingEntity getIndirectSourceEntity() {
        return Explosion.getIndirectSourceEntity(this.exploder);
    }

    @Override
    public @Nullable Entity getDirectSourceEntity() {
        return this.exploder;
    }

    @Override
    public float radius() {
        return this.radius;
    }

    @Override
    public Vec3 center() {
        return new Vec3(this.x, this.y, this.z);
    }

    @Override
    public boolean canTriggerBlocks() {
        return this.mode == Explosion.BlockInteraction.TRIGGER_BLOCK;
    }

    @Override
    public boolean shouldAffectBlocklikeEntities() {
        return this.mode.shouldAffectBlocklikeEntities();
    }

    public record LivingHit(LivingEntity entity, float damage, boolean killed) {
    }
}
