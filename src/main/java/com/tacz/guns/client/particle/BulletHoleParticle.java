package com.tacz.guns.client.particle;

import com.tacz.guns.api.TimelessAPI;
import com.tacz.guns.config.client.RenderConfig;
import com.tacz.guns.init.ModBlocks;
import com.tacz.guns.particles.BulletHoleOption;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleProvider;
import net.minecraft.client.particle.SingleQuadParticle;
import net.minecraft.client.renderer.state.level.QuadParticleRenderState;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.LightCoordsUtil;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.joml.Quaternionf;

public class BulletHoleParticle extends SingleQuadParticle {
    private static final double SURFACE_OFFSET = 0.002D;
    private static final Quaternionf ROTATION_DOWN = new Quaternionf().rotationX((float) (Math.PI / 2));
    private static final Quaternionf ROTATION_UP = new Quaternionf().rotationX((float) (-Math.PI / 2));
    private static final Quaternionf ROTATION_NORTH = new Quaternionf().rotationY((float) Math.PI);
    private static final Quaternionf ROTATION_SOUTH = new Quaternionf();
    private static final Quaternionf ROTATION_WEST = new Quaternionf().rotationY((float) (-Math.PI / 2));
    private static final Quaternionf ROTATION_EAST = new Quaternionf().rotationY((float) (Math.PI / 2));
    private final Direction direction;
    private final BlockPos pos;
    private final Quaternionf surfaceRotation;
    private int uOffset;
    private int vOffset;
    private float textureDensity;
    private float baseRed = 1.0F;
    private float baseGreen = 1.0F;
    private float baseBlue = 1.0F;

    public BulletHoleParticle(ClientLevel world, double x, double y, double z, Direction direction, BlockPos pos, String ammoId, String gunId, String gunDisplayId) {
        super(world,
                x + direction.getStepX() * SURFACE_OFFSET,
                y + direction.getStepY() * SURFACE_OFFSET,
                z + direction.getStepZ() * SURFACE_OFFSET,
                resolveSprite(world, pos));
        this.direction = direction;
        this.pos = pos;
        this.surfaceRotation = getSurfaceRotation(direction);
        this.lifetime = this.getLifetimeFromConfig(world);
        this.hasPhysics = false;
        this.gravity = 0.0F;
        this.quadSize = 0.05F;

        BlockState state = world.getBlockState(pos);
        if (state.is(ModBlocks.TARGET.get()) || shouldRemove()) {
            this.remove();
        }

        TimelessAPI.getGunDisplay(com.tacz.guns.util.IdHelper.id(gunDisplayId), com.tacz.guns.util.IdHelper.id(gunId)).ifPresent(gunIndex -> {
            float[] gunTracerColor = gunIndex.getTracerColor();
            if (gunTracerColor != null) {
                setBaseColor(gunTracerColor[0], gunTracerColor[1], gunTracerColor[2]);
            } else {
                TimelessAPI.getClientAmmoIndex(com.tacz.guns.util.IdHelper.id(ammoId)).ifPresent(ammoIndex -> {
                    float[] ammoTracerColor = ammoIndex.getTracerColor();
                    if (ammoTracerColor != null) {
                        setBaseColor(ammoTracerColor[0], ammoTracerColor[1], ammoTracerColor[2]);
                    }
                });
            }
        });
        this.setAlpha(0.9F);
    }

    private static TextureAtlasSprite resolveSprite(ClientLevel world, BlockPos pos) {
        BlockState state = world.getBlockState(pos);
        return Minecraft.getInstance().getModelManager().getBlockStateModelSet().getParticleMaterial(state).sprite();
    }

    private void setBaseColor(float r, float g, float b) {
        this.baseRed = r;
        this.baseGreen = g;
        this.baseBlue = b;
        this.setColor(r, g, b);
    }

    private int getLifetimeFromConfig(ClientLevel world) {
        int configLife = RenderConfig.BULLET_HOLE_PARTICLE_LIFE.get();
        if (configLife <= 1) {
            return configLife;
        }
        return configLife + world.getRandom().nextInt(configLife / 2);
    }

    @Override
    protected void setSprite(TextureAtlasSprite sprite) {
        super.setSprite(sprite);
        this.uOffset = this.random.nextInt(16);
        this.vOffset = this.random.nextInt(16);
        this.textureDensity = (sprite.getU1() - sprite.getU0()) / 16.0F;
    }

    @Override
    protected float getU0() {
        return this.sprite.getU0() + this.uOffset * this.textureDensity;
    }

    @Override
    protected float getV0() {
        return this.sprite.getV0() + this.vOffset * this.textureDensity;
    }

    @Override
    protected float getU1() {
        return this.getU0() + this.textureDensity;
    }

    @Override
    protected float getV1() {
        return this.getV0() + this.textureDensity;
    }

    @Override
    public void tick() {
        super.tick();
        if (shouldRemove()) {
            this.remove();
        }
    }

    @Override
    public void extract(QuadParticleRenderState state, Camera camera, float partialTicks) {
        int light = Math.max(15 - this.age / 2, 0);
        float colorPercent = light / 15.0F;
        this.setColor(this.baseRed * colorPercent, this.baseGreen * colorPercent, this.baseBlue * colorPercent);

        double threshold = RenderConfig.BULLET_HOLE_PARTICLE_FADE_THRESHOLD.get() * this.lifetime;
        float fade = 1.0F - (float) (Math.max(this.age - threshold, 0) / Math.max(this.lifetime - threshold, 1.0));
        this.setAlpha(0.9F * fade);

        Vec3 view = camera.position();
        float particleX = (float) (Mth.lerp(partialTicks, this.xo, this.x) - view.x());
        float particleY = (float) (Mth.lerp(partialTicks, this.yo, this.y) - view.y());
        float particleZ = (float) (Mth.lerp(partialTicks, this.zo, this.z) - view.z());
        this.extractRotatedQuad(state, this.surfaceRotation, particleX, particleY, particleZ, partialTicks);
    }

    private static Quaternionf getSurfaceRotation(Direction direction) {
        return switch (direction) {
            case DOWN -> ROTATION_DOWN;
            case UP -> ROTATION_UP;
            case NORTH -> ROTATION_NORTH;
            case SOUTH -> ROTATION_SOUTH;
            case WEST -> ROTATION_WEST;
            case EAST -> ROTATION_EAST;
        };
    }

    @Override
    protected int getLightCoords(float partialTicks) {
        int light = Math.max(15 - this.age / 2, 0);
        return LightCoordsUtil.pack(light, light);
    }

    @Override
    protected Layer getLayer() {
        return Layer.TRANSLUCENT_TERRAIN;
    }

    private boolean shouldRemove() {
        BlockState blockState = this.level.getBlockState(this.pos);
        if (blockState.isAir()) {
            return true;
        }
        VoxelShape shape = blockState.getCollisionShape(this.level, this.pos);
        if (shape.isEmpty()) {
            return true;
        }
        AABB blockBoundingBox = shape.bounds().move(this.pos);
        return !blockBoundingBox.intersects(
                this.x - 0.1,
                this.y - 0.1,
                this.z - 0.1,
                this.x + 0.1,
                this.y + 0.1,
                this.z + 0.1
        );
    }

    public static class Provider implements ParticleProvider<BulletHoleOption> {
        @Override
        public @Nullable Particle createParticle(
                @NotNull BulletHoleOption option,
                @NotNull ClientLevel world,
                double x,
                double y,
                double z,
                double pXSpeed,
                double pYSpeed,
                double pZSpeed,
                RandomSource random
        ) {
            return new BulletHoleParticle(world, x, y, z, option.getDirection(), option.getPos(), option.getAmmoId(), option.getGunId(), option.getGunDisplayId());
        }
    }
}
