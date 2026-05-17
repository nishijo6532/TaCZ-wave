package com.tacz.guns.entity;

import com.google.common.collect.Lists;
import com.tacz.guns.GunMod;
import com.tacz.guns.api.DefaultAssets;
import com.tacz.guns.api.GunProperties;
import com.tacz.guns.api.GunProperty;
import com.tacz.guns.api.entity.IGunOperator;
import com.tacz.guns.api.entity.ITargetEntity;
import com.tacz.guns.api.entity.KnockBackModifier;
import com.tacz.guns.api.event.common.EntityHurtByGunEvent;
import com.tacz.guns.api.event.common.EntityKillByGunEvent;
import com.tacz.guns.api.event.server.AmmoHitBlockEvent;
import com.tacz.guns.api.item.gun.AbstractGunItem;
import com.tacz.guns.client.particle.AmmoParticleSpawner;
import com.tacz.guns.config.common.AmmoConfig;
import com.tacz.guns.config.sync.SyncConfig;
import com.tacz.guns.entity.shooter.ShooterDataHolder;
import com.tacz.guns.init.ModDamageTypes;
import com.tacz.guns.network.NetworkHandler;
import com.tacz.guns.network.message.event.ServerMessageGunHurt;
import com.tacz.guns.network.message.event.ServerMessageGunKill;
import com.tacz.guns.particles.BulletHoleOption;
import com.tacz.guns.resource.modifier.AttachmentCacheProperty;
import com.tacz.guns.resource.modifier.custom.*;
import com.tacz.guns.resource.pojo.data.gun.BulletData;
import com.tacz.guns.resource.pojo.data.gun.ExplosionData;
import com.tacz.guns.resource.pojo.data.gun.ExtraDamage.DistanceDamagePair;
import com.tacz.guns.resource.pojo.data.gun.GunData;
import com.tacz.guns.resource.pojo.data.gun.Ignite;
import com.tacz.guns.client.renderer.item.GunItemRendererWrapper;
import com.tacz.guns.util.EntityUtil;
import com.tacz.guns.util.ExplodeUtil;
import com.tacz.guns.util.ForgeEventCompat;
import com.tacz.guns.util.NbtCompat;
import com.tacz.guns.util.TacHitResult;
import com.tacz.guns.util.block.BlockRayTrace;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.Tag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerEntity;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.TagKey;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseFireBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.entity.IEntityAdditionalSpawnData;
import net.minecraftforge.entity.PartEntity;
import net.minecraftforge.fml.LogicalSide;
import org.apache.commons.lang3.tuple.Pair;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.Nullable;
import org.joml.Vector2d;
import org.joml.Vector3d;
import org.joml.Vector3f;

import java.util.*;

import static com.tacz.guns.api.GunProperties.RuntimeOnly.*;
import static com.tacz.guns.api.event.common.GunDamageSourcePart.ARMOR_PIERCING;
import static com.tacz.guns.api.event.common.GunDamageSourcePart.NON_ARMOR_PIERCING;

/**
 * 髯ｷ莨夲ｽｽ・ｨ鬮｢・ｭ繝ｻ・ｽ髮弱・・ｽ・ｦ髯懆ｶ｣・ｽ・ｨ髫ｰ繝ｻﾂｧ郢晢ｽｻ鬨ｾ・ｧ郢晢ｽｻ繝ｻ・ｭ闔牙遜・ｽ・ｼ繝ｻ・ｹ髯橸ｽｳ隶楢ｲｻ・ｽ・ｽ髦ｮ蜊債郢晢ｽｻ
 */
public class EntityKineticBullet extends Projectile implements IEntityAdditionalSpawnData {
    private static final ResourceKey<EntityType<?>> TYPE_KEY = ResourceKey.create(Registries.ENTITY_TYPE, com.tacz.guns.util.IdHelper.id(GunMod.MOD_ID, "bullet"));
    public static final EntityType<EntityKineticBullet> TYPE = EntityType.Builder.<EntityKineticBullet>of(EntityKineticBullet::new, MobCategory.MISC).noSummon().noSave().fireImmune().sized(0.0625F, 0.0625F).clientTrackingRange(5).updateInterval(5).setShouldReceiveVelocityUpdates(false).build(TYPE_KEY);
    public static final TagKey<EntityType<?>> USE_MAGIC_DAMAGE_ON = TagKey.create(Registries.ENTITY_TYPE, com.tacz.guns.util.IdHelper.id("tacz:use_magic_damage_on"));
    public static final TagKey<EntityType<?>> USE_VOID_DAMAGE_ON = TagKey.create(Registries.ENTITY_TYPE, com.tacz.guns.util.IdHelper.id("tacz:use_void_damage_on"));
    public static final TagKey<EntityType<?>> PRETEND_MELEE_DAMAGE_ON = TagKey.create(Registries.ENTITY_TYPE, com.tacz.guns.util.IdHelper.id("tacz:pretend_melee_damage_on"));

    /**
     * 髯ｷ驕ｺ謠｡繝ｻ・ｮ繝ｻ・ｸ髯ｷ闌ｨ・ｽ・ｶ髣泌ｳｨ繝ｻmod 髣厄ｽｴ繝ｻ・ｿ鬨ｾ蛹・ｽｽ・ｨ persistent data郢晢ｽｻ陜捺ｻゑｽｽ・ｰ繝ｻ・ｸ髣包ｽｵ郢晢ｽｻ霎溷､奇ｽｬ謦ｰ・ｽ・ｮ郢晢ｽｻ郢晢ｽｻ髫ｰ證ｦ・ｽ・ｧ髯具ｽｻ繝ｻ・ｶ髫ｴ蜴・ｽｽ・ｳ髯ｷ閧ｲ逕･繝ｻ・ｼ繝ｻ・ｹ鬨ｾ・ｧ郢晢ｽｻ繝ｻ・｢隲帙・讓ｪ髯ｷ・･隶呵ｶ｣・ｽ・ｲ驕会ｽｼ繝ｻ・ｻ郢晢ｽｻ・つ郢晢ｽｻp>
     * 髣厄ｽｴ繝ｻ・ｿ鬨ｾ蛹・ｽｽ・ｨ髮朱ｯ会ｽｽ・ｸ髣包ｽｵ郢晢ｽｻ霎溷､奇ｽｬ謦ｰ・ｽ・ｮ鬨ｾ・ｧ郢晢ｽｻ繝ｻ・･繝ｻ・ｽ髯樊ｺ倥・陟大ｴ取ｺ繝ｻ・ｳ髣厄ｽｴ繝ｻ・ｿ髣比ｼ夲ｽｽ・･髯ｷ・ｷ陷ｿ蛹√◆鬩債繝ｻ・ｻ髯樊ｻゑｽｽ・ｧ髫ｰ・ｾ繝ｻ・ｹ郢晢ｽｻ陟包ｽ｡繝ｻ・ｽ繝ｻ・ｿ鬨ｾ蛹・ｽｽ・ｨ髣費｣ｰ郢晢ｽｻ繝ｻ・ｿ陷ｷ・ｩ繝ｻ・ｸ繝ｻ・ｪ髯ｷ逕ｻ・ｺ・ｯ郢晢ｽｻ鬨ｾ・ｧ郢晢ｽｻ郢晢ｽｻ髣泌ｳｨ繝ｻmod 髣包ｽｵ雋贋ｼ夲ｽｽ・ｸ陜｣・ｺ繝ｻ・ｼ陞｢・ｼ繝ｻ・ｴ繝ｻ・ｩ髮九・繝ｻ・つ郢晢ｽｻp>
     * 髣包ｽｳ驕擾ｽｩ隰ｫ繝ｻ蜿峨・・､髣包ｽｳ繝ｻ・ｪ髯昴・霆ｸ繝ｻ・ｮ繝ｻ・ｵ髫ｴ謫ｾ・ｽ・ｯ persistent data 鬨ｾ・ｧ郢晢ｽｻkey驍ｵ・ｲ郢晢ｽｻp>
     * 鬮ｴ螟ｧ蟋薙・・ｸ繝ｻ・ｪ髯昴・霆ｸ繝ｻ・ｮ繝ｻ・ｵ鬨ｾ・ｧ郢晢ｽｻ・つ繝ｻ・ｼ鬨ｾ・ｧ郢晢ｽｻ繝ｻ・ｱ繝ｻ・ｻ髯懷雀邇・代・int[4]驍ｵ・ｲ郢晢ｽｻp>
     * <p>
     * 髣厄ｽｴ繝ｻ・ｿ鬨ｾ蛹・ｽｽ・ｨ髣懃§繝ｻ繝ｻ・ｼ郢晢ｽｻ
     * <pre>{@code
     *     bullet.getPersistentData().putIntArray(TRACER_COLOR_OVERRIDER_KEY, new int[]{255, 255, 255, 255});
     * }</pre>
     */
    public static final String TRACER_COLOR_OVERRIDER_KEY = GunMod.MOD_ID + ":tracer_override";

    /**
     * 鬮ｴ螟ｧ蟋薙・・ｸ繝ｻ・ｪ髯昴・霆ｸ繝ｻ・ｮ繝ｻ・ｵ鬨ｾ・ｧ郢晢ｽｻ・つ繝ｻ・ｼ鬨ｾ・ｧ郢晢ｽｻ繝ｻ・ｱ繝ｻ・ｻ髯懷雀邇・代・float驍ｵ・ｲ郢晢ｽｻ
     * 1 鬮ｯ・ｦ繝ｻ・ｨ鬩穂ｼ夲ｽｽ・ｺ鬲・ｺｷﾂ・ｩ繝ｻ・ｮ繝ｻ・､髯樊ｻゑｽｽ・ｧ髯昴・蠕励・・ｼ郢晢ｽｻ 鬮ｯ・ｦ繝ｻ・ｨ鬩穂ｼ夲ｽｽ・ｺ 0 髯区ｺｷ隱ｿ驍擾ｽｫ鬩埼｡費ｽ､・ｼ繝ｻ・ｻ郢晢ｽｻ繝ｻ・ｼ闔蛹・ｽｽ・ｸ髢ｧ・ｴ闔画ｨ｣笙ゅ・・ｺ髣費｣ｰ郢晢ｽｻ繝ｻ・ｼ郢晢ｽｻ
     */
    public static final String TRACER_SIZE_OVERRIDER_KEY = GunMod.MOD_ID + ":tracer_size";

    private static final ExplosionData DEFAULT_EXPLOSION_DATA = new ExplosionData(false, 0, 0, false, 30, false);
    private static final double EXPLOSION_SURFACE_OFFSET = 0.05D;
    private static final double FIRST_PERSON_PREDICTION_BLEND_START_DISTANCE = 5D;
    private static final double FIRST_PERSON_PREDICTION_BLEND_END_DISTANCE = 10D;
    private static final double FIRST_PERSON_BALLISTIC_VISUAL_BLEND_START_DISTANCE = 5D;
    private static final double FIRST_PERSON_BALLISTIC_VISUAL_BLEND_END_DISTANCE = 10D;

    private Identifier ammoId = DefaultAssets.EMPTY_AMMO_ID;
    private int life = 200;
    @Deprecated
    private float speed = 1;
    private float gravity = 0;
    private float friction = 0.01F;
    private LinkedList<DistanceDamagePair> damageAmount = Lists.newLinkedList();
    private float distanceAmount = 0;
    private float knockback = 0;
    private boolean explosion = false;
    private boolean igniteEntity = false;
    private boolean igniteBlock = false;
    private int igniteEntityTime = 2;
    private float explosionDamage = 3;
    private float explosionRadius = 3;
    private int explosionDelayCount = Integer.MAX_VALUE;
    private boolean explosionKnockback = false;
    private boolean explosionDestroyBlock = false;
    private float damageModifier = 1;
    // 鬩包ｽｨ繝ｻ・ｿ鬯ｨ・ｾ闕ｵ遉ｼ繝ｻ
    private int pierce = 1;
    // 髯具ｽｻ隴取得・ｽ・ｧ陋滂ｽｶ繝ｻ・ｽ陷･・ｲ繝ｻ・ｽ繝ｻ・ｮ
    private Vec3 startPos;
    // 髫ｴ蜴・ｽｽ・ｳ髯ｷ閧ｲ逕･繝ｻ・ｼ繝ｻ・ｹ
    private boolean isTracerAmmo;
    // 髣比ｼ夲ｽｽ・･髣包ｽｳ陷ｿ・･郢晢ｽｻ髣包ｽｳ繝ｻ・ｪ髫ｴ謫ｾ・ｽ・ｯ髯ｷ・ｿ繝ｻ・ｪ髯昴・・ｽ・ｹ髯橸ｽｳ繝ｻ・｢髫ｰ魃会ｽｽ・ｷ鬩包ｽｶ繝ｻ・ｯ髫ｴ蟷・・騾｡鮃ｹﾂ・ｧ郢晢ｽｻ陝ｲ蜻ｵ諤ｦ霑壼遜・ｽ・ｼ繝ｻ・ｹ髫ｰ・ｨ繝ｻ・ｰ髫ｰ謦ｰ・ｽ・ｮ
    private float cameraXRot;
    private float cameraYRot;
    private Vector3f firstPersonRenderOffset;
    private Vec3 clientFirstPersonRenderOrigin;
    private long clientFirstPersonRenderCapturedAt = -1L;
    private long shootTimestamp = -1L;
    // 髯ｷ・ｿ陞滂ｽｧ繝ｻ・ｰ郢晢ｽｻ陜趣ｽｪ髫ｴ・ｫ繝ｻ・ｪ髫ｴ・ｴ繝ｻ・ｰ ID
    private Identifier gunId;
    // 髫ｴ・ｫ繝ｻ・ｪ髫ｴ・ｴ繝ｻ・ｰdisplay ID
    private Identifier gunDisplayId;
    private float armorIgnore;
    private float headShot;

    public EntityKineticBullet(EntityType<? extends Projectile> type, Level worldIn) {
        super(type, worldIn);
    }

    public EntityKineticBullet(EntityType<? extends Projectile> type, double x, double y, double z, Level worldIn) {
        this(type, worldIn);
        this.setPos(x, y, z);
    }

    public EntityKineticBullet(Level worldIn, LivingEntity throwerIn, ItemStack gunItem, Identifier ammoId, Identifier gunId,
                               Identifier gunDisplayId, boolean isTracerAmmo, GunData gunData, BulletData bulletData) {
        this(TYPE, worldIn, throwerIn, gunItem, ammoId, gunId, gunDisplayId, isTracerAmmo, gunData, bulletData);
    }

    public EntityKineticBullet(Level worldIn, LivingEntity throwerIn, ItemStack gunItem, Identifier ammoId, Identifier gunId, boolean isTracerAmmo, GunData gunData, BulletData bulletData) {
        this(TYPE, worldIn, throwerIn, gunItem, ammoId, gunId, DefaultAssets.DEFAULT_GUN_DISPLAY_ID, isTracerAmmo, gunData, bulletData);
    }

    protected EntityKineticBullet(EntityType<? extends Projectile> type, Level worldIn, LivingEntity throwerIn, ItemStack gunItem,
                                  Identifier ammoId, Identifier gunId, Identifier gunDisplayId,
                                  boolean isTracerAmmo, GunData gunData, BulletData bulletData) {
        this(type, throwerIn.getX(), throwerIn.getEyeY() - (double) 0.1F, throwerIn.getZ(), worldIn);
        this.setOwner(throwerIn);
        // gunId 髫ｰ・ｰ闔牙衷竏夐囗讎雁罰・つ繝ｻ・ｼ郢晢ｽｻ陟包ｽ｡繝ｻ・ｻ繝ｻ・･鬮ｫ・ｶ繝ｻ・ｩ modifyProperty 髯ｷ・ｿ繝ｻ・ｯ髣比ｼ夲ｽｽ・･髯懶ｽｨ繝ｻ・ｨ髫ｴ・ｫ郢晢ｽｻ・つ繝ｻ・ｰ髯ｷ繝ｻ・ｽ・ｽ髫ｰ・ｨ繝ｻ・ｰ髣包ｽｳ繝ｻ・ｭ鬮ｴ螟ｧ鬆・・・｡郢晢ｽｻ
        this.gunId = gunId;
        AttachmentCacheProperty cacheProperty = Objects.requireNonNull(IGunOperator.fromLivingEntity(throwerIn).getCacheProperty());
        this.shootTimestamp = IGunOperator.fromLivingEntity(throwerIn).getDataHolder().shootTimestamp;
        float armorIgnore = modifyProperty(GunProperties.ARMOR_IGNORE, Float.class, cacheProperty.getCache(GunProperties.ARMOR_IGNORE));
        float headshot = modifyProperty(GunProperties.HEADSHOT_MULTIPLIER, Float.class, cacheProperty.getCache(GunProperties.HEADSHOT_MULTIPLIER));
        float knockback = modifyProperty(GunProperties.KNOCKBACK, Float.class, cacheProperty.getCache(GunProperties.KNOCKBACK));
        this.armorIgnore = Mth.clamp(armorIgnore, 0f, 1f);
        this.headShot = Math.max(headshot, 0f);
        this.knockback = Math.max(knockback, 0f);
        this.ammoId = ammoId;
        float lifeSecond = modifyProperty(BULLET_LIFE, Float.class, bulletData.getLifeSecond());
        this.life = Mth.clamp((int) (lifeSecond * 20), 1, Integer.MAX_VALUE);
        // speed 髯昴・霆ｸ繝ｻ・ｮ繝ｻ・ｵ髫ｴ謫ｾ・ｽ・ｯ髫ｴ魃会ｽ｣・ｰ髫ｰ・ｨ髢ｧ・ｲ陜趣ｽｪ郢晢ｽｻ隰疲ｻゑｽｽ・ｮ隶難ｽ｣陋ｻ遘伉蠅難ｽｻ讌｢・ｭ諞ｺﾂ・ｧ郢晢ｽｻ・つ雋・ｽｷ繝ｻ・ｺ繝ｻ・ｦ髫ｴ謫ｾ・ｽ・ｯ shootOnce 鬯ｩ・･陟包ｽ｡繝ｻ・ｼ繝ｻ・ｰ隰・現繝ｻdoBulletSpread 鬨ｾ・ｧ郢晢ｽｻ・つ雋・ｽｷ繝ｻ・ｺ繝ｻ・ｦ
        this.gravity = Mth.clamp(modifyProperty(BULLET_GRAVITY, Float.class, bulletData.getGravity()), 0f, Float.MAX_VALUE);
        this.friction = Mth.clamp(modifyProperty(BULLET_FRICTION, Float.class, bulletData.getFriction()), 0f, Float.MAX_VALUE);
        // 髴難ｽ､繝ｻ・ｹ髴趣ｽｯ郢晢ｽｻ
        Ignite ignite = cacheProperty.getCache(IgniteModifier.ID);
        this.igniteEntity = modifyProperty(IGNITE_ENTITY, Boolean.class, bulletData.getIgnite().isIgniteEntity() || ignite.isIgniteEntity());
        this.igniteEntityTime = Math.max(modifyProperty(IGNITE_ENTITY_TIME, Integer.class, bulletData.getIgniteEntityTime()), 0);
        this.igniteBlock = modifyProperty(IGNITE_BLOCK, Boolean.class, bulletData.getIgnite().isIgniteBlock() || ignite.isIgniteBlock());
        this.damageAmount = cacheProperty.getCache(DamageModifier.ID);
        this.distanceAmount = modifyProperty(GunProperties.EFFECTIVE_RANGE, Float.class, cacheProperty.getCache(GunProperties.EFFECTIVE_RANGE));
        int pierce = modifyProperty(GunProperties.PIERCE, Integer.class, cacheProperty.getCache(GunProperties.PIERCE));
        this.pierce = Mth.clamp(pierce, 1, Integer.MAX_VALUE);
        ExplosionData explosionData = Objects.requireNonNullElse(cacheProperty.getCache(ExplosionModifier.ID), DEFAULT_EXPLOSION_DATA);
        this.explosion = modifyProperty(EXPLODE_ENABLED, Boolean.class, explosionData.isExplode());
        if (this.explosion) {
            var explosionDamage = modifyProperty(EXPLOSION_DAMAGE, Float.class, explosionData.getDamage());
            var explosionRadius = modifyProperty(EXPLOSION_RADIUS, Float.class, explosionData.getRadius());
            this.explosionDamage = (float) Mth.clamp(explosionDamage * SyncConfig.DAMAGE_BASE_MULTIPLIER.get(), 0, Float.MAX_VALUE);
            this.explosionRadius = Mth.clamp(explosionRadius, 0, Float.MAX_VALUE);
            this.explosionKnockback = modifyProperty(EXPLOSION_KNOCKBACK, Boolean.class, explosionData.isKnockback());
            // 鬯ｮ・ｦ繝ｻ・ｲ髮弱・・ｽ・｢鬮ｮ諞ｺ・｡遘倥・郢晢ｽｻ隴ｴ・ｧ驗ゑｽｲ髯ｷ隨ｬ・ｦ鬆代・髯橸ｽｳ郢晢ｽｻ
            int delayTickCount = (int) (modifyProperty(EXPLOSION_DELAY, Float.class, explosionData.getDelay()) * 20);
            if (delayTickCount < 0) {
                delayTickCount = Integer.MAX_VALUE;
            }
            // 鬯ｩ貅ｷ隱ｿ繝ｻ・ｽ繝ｻ・ｮ髫ｴ竏壹・繝ｻ・ｻ繝ｻ・ｶ髯ｷ闌ｨ・ｽ・ｳ鬯ｮ・｣繝ｻ・ｭ髴趣ｽｷ郢晢ｽｻ邵ｺ螟頑・闕ｳ・ｻ繝ｻ・ｿ繝ｻ・ｽ鬨ｾ・｡繝ｻ・･鬮｢・ｼ陞｢・ｽ隰費ｽｽ髯昴・・ｽ・ｹ髴趣ｽｷ郢晢ｽｻ邵ｺ螟奇ｽｭ謫ｾ・ｽ・ｯ髯ｷ・ｷ繝ｻ・ｦ鬩墓腸・ｽ・ｴ髯懶ｽｮ闕ｵ諤懶ｽｩ・ｿ髯懶ｽｮ驕会ｽｼ陜趣ｽｪ髣厄ｽｫ繝ｻ・ｮ髫ｰ・ｾ繝ｻ・ｹ
            this.explosionDestroyBlock = AmmoConfig.EXPLOSIVE_AMMO_DESTROYS_BLOCK.get() && modifyProperty(EXPLOSION_DESTROYS_BLOCK, Boolean.class, explosionData.isDestroyBlock());
            this.explosionDelayCount = Math.max(delayTickCount, 1);
        }
        // 髯昴・・ｻ蜻ｻ・ｽ・ｼ繝ｻ・ｹ髯具ｽｻ隴取得・ｽ・ｧ陋滂ｽｶ繝ｻ・ｽ陷･・ｲ繝ｻ・ｽ繝ｻ・ｮ鬯ｩ・･陷･・ｲ繝ｻ・ｽ繝ｻ・ｮ
        double posX = throwerIn.xOld + (throwerIn.getX() - throwerIn.xOld) / 2.0;
        double posY = throwerIn.yOld + (throwerIn.getY() - throwerIn.yOld) / 2.0 + throwerIn.getEyeHeight();
        double posZ = throwerIn.zOld + (throwerIn.getZ() - throwerIn.zOld) / 2.0;
        this.setPos(posX, posY, posZ);
        this.startPos = this.position();
        this.isTracerAmmo = isTracerAmmo;
        this.gunDisplayId = gunDisplayId;
    }

    @ApiStatus.Internal
    public void applyShotgunDamageSpread(int bulletCount) {
        // 鬯ｮ・ｴ繝ｻ・ｰ髯滓汚・ｽ・ｹ髫ｲ・ｰ郢晢ｽｻ郢晢ｽｻ郢晢ｽｻ隴ｴ・ｧ繝ｻ・ｯ闕ｳ闌ｨ・ｽ・ｸ繝ｻ・ｪ髣費ｽｨ繝ｻ・､髯橸ｽｳ繝ｻ・ｳ鬮ｫ陬懈桶隹ｿ・ｴ髯ｷ・ｴ繝ｻ・ｻ
        if (bulletCount > 1) {
            this.damageModifier = 1f / bulletCount;
        }
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
    }

    @Override
    public void tick() {
        super.tick();
        // 鬮ｫ・ｹ郢晢ｽｻ騾｡繝ｻTaC 髯昴・・ｻ蜻ｻ・ｽ・ｼ繝ｻ・ｹ髫ｴ蟶ｶ・ｦ鬘倡帥髯懆ｶ｣・ｽ・ｨ髣費｣ｰ陋滂ｽｶ繝ｻ・ｻ繝ｻ・ｶ
        this.onBulletTick();
        // 鬩埼｡假ｽｲ讖ｸ・ｽ・ｭ陷磯メ・ｭ諛・ｽｭ・ｫ郢晢ｽｻ
        if (this.level().isClientSide()) {
            AmmoParticleSpawner.addParticle(this);
        }
        // 髯昴・・ｻ蜻ｻ・ｽ・ｼ繝ｻ・ｹ髫ｶ髮｣・ｽ・｡髯懷衷霍晁搦・ｪ髫ｴ讙惹ｺ九・・ｽ繝ｻ・ｬ髣包ｽｳ陷ｿ螟懶ｽｴ・ｨ髴大､ｲ・ｽ・ｩ鬩幢ｽ､繝ｻ・ｿ
        Vec3 movement = this.getDeltaMovement();
        double x = movement.x;
        double y = movement.y;
        double z = movement.z;
        double distance = movement.horizontalDistance();
        this.setYRot((float) Math.toDegrees(Mth.atan2(x, z)));
        this.setXRot((float) Math.toDegrees(Mth.atan2(y, distance)));
        // 髯昴・・ｻ蜻ｻ・ｽ・ｼ繝ｻ・ｹ髯具ｽｻ隴取得・ｽ・ｧ霑｢諤憺｣ｭ髫ｴ蟶ｶ蜑碁ｬｮ繝ｻ蝙ｳ繝ｻ・ｾ鬩励ｑ・ｽ・ｮ
        if (this.xRotO == 0.0F && this.yRotO == 0.0F) {
            this.yRotO = this.getYRot();
            this.xRotO = this.getXRot();
        }
        // 髯昴・・ｻ蜻ｻ・ｽ・ｼ繝ｻ・ｹ鬮ｴ蜿ｰ・ｻ蜷晞矯髫ｴ魃会ｽｽ・ｶ鬨ｾ・ｧ郢晢ｽｻ鬮ｮ・ｷ鬮ｴ髮｣・ｽ・ｬ郢晢ｽｻ闔蛹・ｽｽ・ｸ隶朱托ｽ｡竏ｬ諠ｺ繝ｻ・ｫ鬮｢・ｾ繝ｻ・ｪ鬮ｴ髮｣・ｽ・ｬ郢晢ｽｻ郢晢ｽｻ
        this.setXRot(lerpRotation(this.xRotO, this.getXRot()));
        this.setYRot(lerpRotation(this.yRotO, this.getYRot()));
        // 髯昴・・ｻ蜻ｻ・ｽ・ｼ繝ｻ・ｹ髣厄ｽｴ陷･・ｲ繝ｻ・ｽ繝ｻ・ｮ髫ｴ蜴・ｽｽ・ｴ髫ｴ繝ｻ・ｽ・ｰ
        double nextPosX = this.getX() + x;
        double nextPosY = this.getY() + y;
        double nextPosZ = this.getZ() + z;
        this.setPos(nextPosX, nextPosY, nextPosZ);
        float friction = this.friction;
        float gravity = this.gravity;
        // 髯昴・・ｻ蜻ｻ・ｽ・ｼ繝ｻ・ｹ髯ｷ闌ｨ・ｽ・･髮朱ｯ会ｽｽ・ｴ髯ｷ・ｷ陞ｳ莠･鬟ｭ鬮ｫ・ｹ郢晢ｽｻ驍上・
        if (this.isInWater()) {
            for (int i = 0; i < 4; i++) {
                this.level().addParticle(ParticleTypes.BUBBLE, nextPosX - x * 0.25F, nextPosY - y * 0.25F, nextPosZ - z * 0.25F, x, y, z);
            }
            // 髯懶ｽｨ繝ｻ・ｨ髮朱ｯ会ｽｽ・ｴ髣包ｽｳ繝ｻ・ｭ鬨ｾ・ｧ郢晢ｽｻ闔峨・諤冗ｹ晢ｽｻ
            friction = 0.4F;
            gravity *= 0.6F;
        }
        // 鬯ｩ・･隶惹ｼ・ｽｴ・ｨ髣包ｽｳ隴幢ｽｱ闔峨・諤剰涕・ｶ陝ｲ・ｩ髫ｴ繝ｻ・ｽ・ｰ鬯ｨ・ｾ雋・ｽｷ繝ｻ・ｺ繝ｻ・ｦ髴托ｽ･繝ｻ・ｶ髫ｲ・､郢晢ｽｻ
        this.setDeltaMovement(this.getDeltaMovement().scale(1 - friction));
        this.setDeltaMovement(this.getDeltaMovement().add(0, -gravity, 0));
        // 髯昴・・ｻ蜻ｻ・ｽ・ｼ繝ｻ・ｹ鬨ｾ蠅難ｽｺ・ｷ陞溷｣ｽ豐ｿ隰撰ｽｺ隰ｫ繝ｻ
        if (this.tickCount >= this.life - 1) {
            this.discard();
        }
    }

    // 髯昴・・ｻ蜻ｻ・ｽ・ｼ繝ｻ・ｹ鬨ｾ・ｧ郢晢ｽｻ・つ繝ｻ・ｻ鬮ｴ荳ｻ・､・ｧ繝ｻ・､郢晢ｽｻ霓､繝ｻ
    protected void onBulletTick() {
        // 髫ｴ蟶ｶ・ｦ鬘倡帥髯懆ｶ｣・ｽ・ｨ鬩包ｽｶ繝ｻ・ｯ髯昴・・ｻ蜻ｻ・ｽ・ｼ繝ｻ・ｹ鬯ｨ・ｾ繝ｻ・ｻ鬮ｴ蠑ｱ繝ｻ
        if (!this.level().isClientSide()) {
            // 髯晄慣・ｽ・ｶ鬮ｴ隨ｬ・ｺ・ｽ郢晢ｽｻ髴難ｽ､繝ｻ・ｸ髯具ｽｻ繝ｻ・､髯橸ｽｳ郢晢ｽｻ
            if (this.explosion) {
                if (this.explosionDelayCount > 0) {
                    this.explosionDelayCount--;
                } else {
                    ExplodeUtil.createExplosion(this.getOwner(), this, this.explosionDamage, this.explosionRadius, this.explosionKnockback, this.explosionDestroyBlock, this.position());
                    // 髴趣ｽｷ郢晢ｽｻ邵ｺ螟青・ｶ繝ｻ・ｴ髫ｰ證ｦ・ｽ・･隰・沺迹ｳ隰ｫ螟雁初陷･・ｲ髦ｨ闍難｣ｰ謇假ｽｽ・ｹ髯昴・・ｲ・ｻ繝ｻ・ｼ陟包ｽ｡繝ｻ・ｸ隶主･・ｽｽ・､郢晢ｽｻ霓､鬘伜ｵｯ陷ｿ・･鬪ｭ蛟ｬﾂ・ｧ郢晢ｽｻ・つ繝ｻ・ｻ鬮ｴ蠑ｱ繝ｻ
                    this.discard();
                    return;
                }
            }
            // 髯昴・・ｻ蜻ｻ・ｽ・ｼ繝ｻ・ｹ髯懶ｽｨ繝ｻ・ｨ tick 髫俶誓・ｽ・ｷ髯晉距霍晁搦・ｪ髣厄ｽｴ陷･・ｲ繝ｻ・ｽ繝ｻ・ｮ
            Vec3 startVec = this.position();
            // 髯昴・・ｻ蜻ｻ・ｽ・ｼ繝ｻ・ｹ髯懶ｽｨ繝ｻ・ｨ tick 隰・沺迹ｳ隰ｫ螟青・ｧ郢晢ｽｻ繝ｻ・ｽ陷･・ｲ繝ｻ・ｽ繝ｻ・ｮ
            Vec3 endVec = startVec.add(this.getDeltaMovement());
            // 髯昴・・ｻ蜻ｻ・ｽ・ｼ繝ｻ・ｹ鬨ｾ・ｧ郢晢ｽｻ繝ｻ・｢繝ｻ・ｰ髫ｰ・ｦ隶灘･・ｽｽ・｣・つ髮趣ｽｬ郢晢ｽｻ
            HitResult result = BlockRayTrace.rayTraceBlocks(this.level(), new ClipContext(startVec, endVec, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, this));
            BlockHitResult resultB = (BlockHitResult) result;
            if (resultB.getType() != HitResult.Type.MISS) {
                // 髯昴・・ｻ蜻ｻ・ｽ・ｼ繝ｻ・ｹ髯ｷ繝ｻ・ｽ・ｻ髣包ｽｳ繝ｻ・ｭ髫ｴ繝ｻ・ｽ・ｹ髯懶ｽｮ驍・ｽｲ隲ｷ・ｮ郢晢ｽｻ霑ｹ螟ｲ・ｽ・ｮ繝ｻ・ｾ鬩励ｑ・ｽ・ｮ髯ｷ繝ｻ・ｽ・ｻ髣包ｽｳ繝ｻ・ｭ髫ｴ繝ｻ・ｽ・ｹ髯懶ｽｮ驕会ｽｼ陜趣ｽｪ髣厄ｽｴ陷･・ｲ繝ｻ・ｽ繝ｻ・ｮ髣包ｽｳ繝ｻ・ｺ髯昴・・ｻ蜻ｻ・ｽ・ｼ繝ｻ・ｹ鬨ｾ・ｧ郢晢ｽｻ繝ｻ・ｻ隰撰ｽｺ隰ｫ螟頑割陷･・ｲ繝ｻ・ｽ繝ｻ・ｮ
                endVec = resultB.getLocation();
            }

            List<EntityResult> hitEntities = null;
            // 髯昴・・ｻ蜻ｻ・ｽ・ｼ繝ｻ・ｹ鬨ｾ・ｧ郢晢ｽｻ郢晢ｽｻ髣包ｽｳ繝ｻ・ｭ髫ｴ・ｽ・つ髮趣ｽｬ陷茨ｽｷ繝ｻ・ｼ隶呵ｶ｣・ｽ・ｩ繝ｻ・ｿ鬯ｨ・ｾ闕ｳ闌ｨ・ｽ・ｸ繝ｻ・ｺ 1 髫ｰ謔溘・・つ郢晢ｽｻ郢晢ｽｻ髴難ｽ､繝ｻ・ｸ鬩債繝ｻ・ｻ髯滓汚・ｽ・ｹ鬮｣蛹・ｽｽ・ｯ鬯ｮ・ｯ闔牙雀・ｮ蟷・初繝ｻ・ｺ髣包ｽｳ・つ髣包ｽｳ繝ｻ・ｪ髯橸ｽｳ隶楢ｲｻ・ｽ・ｽ鬪ｰ蜈ｷ・ｽ・ｩ繝ｻ・ｿ鬯ｨ・ｾ闕ｳ讓翫・髯橸ｽｳ郢晢ｽｻ
            if (this.pierce <= 1 || this.explosion) {
                EntityResult entityResult = EntityUtil.findEntityOnPath(this, startVec, endVec);
                // 髯昴・繝ｻ魄溷ｮ壼初繝ｻ・ｪ髯ｷ・ｻ繝ｻ・ｽ髣包ｽｳ繝ｻ・ｭ髫ｴ謫ｾ・ｽ・ｯ髯橸ｽｳ隶楢ｲｻ・ｽ・ｽ霓｣蛟･繝ｻ髯晄慣・ｽ・ｺ髣包ｽｳ繝ｻ・ｺ髯ｷ鬘費ｽｩ繧托ｽｽ・ｸ繝ｻ・ｪ髯ｷﾂ郢晢ｽｻ繝ｻ・ｮ繝ｻ・ｹ鬨ｾ・ｧ郢晢ｽｻlist
                if (entityResult != null) {
                    hitEntities = Collections.singletonList(entityResult);
                }
            } else {
                hitEntities = EntityUtil.findEntitiesOnPath(this, startVec, endVec);
            }
            // 髯滉ｹ淞ｧ繝ｻ・ｭ闔牙遜・ｽ・ｼ繝ｻ・ｹ髯ｷ繝ｻ・ｽ・ｻ髣包ｽｳ繝ｻ・ｭ髯橸ｽｳ隶楢ｲｻ・ｽ・ｽ隰撰ｽｺ隲ｷ・ｮ郢晢ｽｻ霑ｹ螟ｲ・ｽ・ｿ陝ｷ繝ｻ・ｽ・｡霑ｹ螟ｲ・ｽ・｢繝ｻ・ｫ髯ｷ・ｻ繝ｻ・ｽ髣包ｽｳ繝ｻ・ｭ鬨ｾ・ｧ郢晢ｽｻ繝ｻ・ｮ隶楢ｲｻ・ｽ・ｽ隶鯉ｽ｢繝ｻ・ｯ繝ｻ・ｻ髯ｷ・ｿ郢晢ｽｻ
            if (hitEntities != null && !hitEntities.isEmpty()) {
                EntityResult[] hitEntityResult = hitEntities.toArray(new EntityResult[0]);
                // 髯昴・・ｽ・ｹ鬮ｯ・ｲ繝ｻ・ｫ髯ｷ・ｻ繝ｻ・ｽ髣包ｽｳ繝ｻ・ｭ鬨ｾ・ｧ郢晢ｽｻ繝ｻ・ｮ隶楢ｲｻ・ｽ・ｽ隶鯉ｽ｢繝ｻ・ｿ陝ｷ繝ｻ・ｽ・｡隴ｴ・ｧ髮画㊥・ｰ蜿門ｾ励・・ｼ隴ｴ・ｧ雋守｢托ｽｾ・｣繝ｻ・ｧ鬮ｴ閧ｴ蛻ｮ繝ｻ・ｦ繝ｻ・ｻ髯昴・・ｻ蜻ｻ・ｽ・ｼ繝ｻ・ｹ髯ｷ・ｿ陞滂ｽｧ繝ｻ・ｰ郢晢ｽｻ繝ｻ・ｽ陷･・ｲ繝ｻ・ｽ繝ｻ・ｮ鬨ｾ・ｧ郢晢ｽｻ繝ｻ・ｷ隴惹ｼ夲ｽｽ・ｦ繝ｻ・ｻ鬮ｴ螟ｧ・ｹ繝ｻ・ｽ・｡隰疲ｺｷ邊矩辧諠ｹ・ｹ闍難ｽｳ諛・｣ｰ蠑ｱ繝ｻ
                for (int i = 0; (i < this.pierce || i < 1) && i < (hitEntityResult.length - 1); i++) {
                    int k = i;
                    for (int j = i + 1; j < hitEntityResult.length; j++) {
                        if (hitEntityResult[j].hitVec.distanceTo(startVec) < hitEntityResult[k].hitVec.distanceTo(startVec)) {
                            k = j;
                        }
                    }
                    EntityResult t = hitEntityResult[i];
                    hitEntityResult[i] = hitEntityResult[k];
                    hitEntityResult[k] = t;
                }
                for (EntityResult entityResult : hitEntityResult) {
                    result = new TacHitResult(entityResult);
                    this.onHitEntity((TacHitResult) result, startVec, endVec);
                    this.pierce--;
                    if (this.pierce < 1 || this.explosion) {
                        // 髯昴・・ｻ蜻ｻ・ｽ・ｼ繝ｻ・ｹ髯晢ｽｾ繝ｻ・ｲ隰・ｺ･繝ｻ繝ｻ・ｩ繝ｻ・ｿ鬯ｨ・ｾ闕ｵ諤懈″髫ｴ蟶ｷ逕･繝ｻ・ｮ隶楢ｲｻ・ｽ・ｽ鬮ｮ・｣繝ｻ・ｼ隶呵ｶ｣・ｽ・ｻ隰撰ｽｺ隰ｫ螟頑ｰ幄脂蜻ｻ・ｽ・ｼ繝ｻ・ｹ鬨ｾ・ｧ郢晢ｽｻ繝ｻ・｣隶壹・・ｽ・｡郢晢ｽｻ
                        this.discard();
                        return;
                    }
                }
            }
            this.onHitBlock(resultB, startVec, endVec);
        }
    }

    public void shoot(double pitch, double yaw, float pVelocity, Vector2d vector2d) {
        Vector3d left = new Vector3d(vector2d.x, vector2d.y, 8);

        left.rotateX(pitch * Mth.DEG_TO_RAD);
        left.rotateY(-yaw * Mth.DEG_TO_RAD);

        Vec3 vec3 = new Vec3(left.x, left.y, left.z).normalize().scale(pVelocity);

        this.setDeltaMovement(vec3.x, vec3.y, vec3.z);
        double d0 = vec3.horizontalDistance();
        this.setYRot((float)(Mth.atan2(vec3.x, vec3.z) * (double)(180F / (float)Math.PI)));
        this.setXRot((float)(Mth.atan2(vec3.y, d0) * (double)(180F / (float)Math.PI)));
        this.yRotO = this.getYRot();
        this.xRotO = this.getXRot();
    }

    public void shootFromRotation(Entity pShooter, float pX, float pY, float pZ, float pVelocity, Vector2d vector2d) {
        this.shoot(pX, pY, pVelocity, vector2d);
        Vec3 vec3 = pShooter.getDeltaMovement();
        this.setDeltaMovement(this.getDeltaMovement().add(vec3.x, pShooter.onGround() ? 0.0D : vec3.y, vec3.z));
    }

    public record MaybeMultipartEntity(
            Entity hitPart,
            Entity core
    ) {
        public static MaybeMultipartEntity of(Entity hitPart) {
            var core = (hitPart instanceof PartEntity<?> part)
                    ? part.getParent()
                    : hitPart;
            return new MaybeMultipartEntity(hitPart, core);
        }
    }

    protected void onHitEntity(TacHitResult result, Vec3 startVec, Vec3 endVec) {
        if (result.getEntity() instanceof ITargetEntity targetEntity) {
            DamageSource source = this.damageSources().thrown(this, this.getOwner());
            targetEntity.onProjectileHit(this, result, source, this.getDamage(result.getLocation()));
            // 髫ｰ繝ｻ・ｦ・ｴ隰ｾ諞ｺﾂ・ｶ繝ｻ・ｴ髫ｰ證ｦ・ｽ・･鬮ｴ隨ｬ・ｳ謔滂ｽｱ繝ｻ
            return;
        }
        // 鬮｣雋ｻ・ｽ・ｷ髯ｷ・ｿ鬲托ｽｳre髣費｣ｰ陋滂ｽｶ繝ｻ・ｻ繝ｻ・ｶ髯滂ｽ｢郢晢ｽｻ繝ｻ・ｦ遶擾ｽｫ陜趣ｽｪ髣厄ｽｫ繝ｻ・｡髫ｲ・ｱ繝ｻ・ｯ
        Entity entity = result.getEntity();
        @Nullable Entity owner = this.getOwner();
        // 髫ｰ・ｾ繝ｻ・ｻ髯ｷ繝ｻ・ｽ・ｻ鬮｢・ｰ郢晢ｽｻ
        LivingEntity attacker = owner instanceof LivingEntity ? (LivingEntity) owner : null;
        var sources = createDamageSources(MaybeMultipartEntity.of(entity));
        boolean headshot = result.isHeadshot();
        float damage = this.getDamage(result.getLocation());
        float headShotMultiplier = Math.max(this.headShot, 0);
        // 髯ｷ・ｿ陞滂ｽｧ繝ｻ・ｸ郢ｧ・ｱre髣費｣ｰ陋滂ｽｶ繝ｻ・ｻ繝ｻ・ｶ
        var preEvent = new EntityHurtByGunEvent.Pre(this, entity, attacker, this.gunId, this.gunDisplayId, damage, sources, headshot, headShotMultiplier, LogicalSide.SERVER);
        var cancelled = ForgeEventCompat.post(preEvent);
        if (cancelled) {
            return;
        }
        // 髯具ｽｻ繝ｻ・ｷ髫ｴ繝ｻ・ｽ・ｰ鬨ｾ蛹・ｽｽ・ｱPre髣費｣ｰ陋滂ｽｶ繝ｻ・ｻ繝ｻ・ｶ髣厄ｽｫ繝ｻ・ｮ髫ｰ・ｾ繝ｻ・ｹ髯ｷ・ｷ陞ｳ莠･鬟ｭ髯ｷ・ｿ郢ｧ閧ｲ繝ｻ
        entity = preEvent.getHurtEntity();
        // 髯ｷ・ｿ隲､諛翫・鬨ｾ・ｶ繝ｻ・ｮ髫ｴ・ｬ郢晢ｽｻ
        var parts = MaybeMultipartEntity.of(entity);
        attacker = preEvent.getAttacker();
        var newGunId = preEvent.getGunId();
        damage = preEvent.getBaseAmount();
        sources = Pair.of(preEvent.getDamageSource(NON_ARMOR_PIERCING), preEvent.getDamageSource(ARMOR_PIERCING));
        headshot = preEvent.isHeadShot();
        headShotMultiplier = preEvent.getHeadshotMultiplier();
        if (entity == null) {
            return;
        }
        // 髴難ｽ､繝ｻ・ｹ髴趣ｽｯ郢晢ｽｻ
        if (this.igniteEntity && AmmoConfig.IGNITE_ENTITY.get()) {
            entity.igniteForSeconds(this.igniteEntityTime);
            // 隰・ｺ･蟋薙・・ｺ髢ｧ・ｲ繝ｻ・ｲ髮区ｩｸ・ｽ・ｭ陷磯メ・ｭ諛・ｽｭ・ｫ郢晢ｽｻ
            if (this.level() instanceof ServerLevel serverLevel) {
                serverLevel.sendParticles(ParticleTypes.LAVA, entity.getX(), entity.getY() + entity.getEyeHeight(), entity.getZ(), 1, 0, 0, 0, 0);
            }
        }
        // TODO 髫ｴ莨夲ｽｽ・ｴ髯ｷ繝ｻ・ｽ・ｻ髯具ｽｻ繝ｻ・､髯橸ｽｳ陞滂ｽｲ繝ｻ・ｼ闔蛹・ｽｽ・ｸ髢ｧ・ｴ陟大ｴ趣ｽｾ・ｷ郢晢ｽｻ繝ｻ・､繝ｻ・ｴ郢晢ｽｻ騾包ｽｻ陜呎･｢諤弱・・ｻ髯具ｽｻ繝ｻ・､髯橸ｽｳ陞｢・ｼ郢晢ｽｻ鬯ｩ蟷｢・ｽ・ｨ鬯ｨ・ｾ繝ｻ・ｻ鬮ｴ蜿匁ｱ壹・・ｼ驕停扱ﾂ蜥主寰遶擾ｽｬ繝ｻ・ｾ霓｣蛟･繝ｻ髣包ｽｳ・つ髣包ｽｳ繝ｻ・ｪ髫ｴ謫ｾ・ｽ・ｯ髯ｷ・ｷ繝ｻ・ｦ髫ｴ莨夲ｽｽ・ｴ髯ｷ繝ｻ・ｽ・ｻ鬨ｾ・ｧ郢晢ｽｻflag
        if (headshot) {
            // 鬲・ｺｷﾂ・ｩ繝ｻ・ｮ繝ｻ・､髴趣ｽｷ郢晢ｽｻ繝ｻ・､繝ｻ・ｴ髣費ｽｨ繝ｻ・､髯橸ｽｳ繝ｻ・ｳ髫ｴ謫ｾ・ｽ・ｯ 1x
            damage *= headShotMultiplier;
        }
        // 髯昴・・ｽ・ｹ LivingEntity 鬮ｴ螟ｧ・ｹ繝ｻ・ｽ・｡隰疲ｺ倥・鬯ｨ・ｾ・つ髯滓汚・ｽ・ｺ髯溯ｶ｣・ｽ・ｦ鬨ｾ・ｧ郢晢ｽｻ郢晢ｽｻ髯橸ｽｳ陞｢・ｻ繝ｻ・ｹ郢晢ｽｻ
        if (parts.core() instanceof LivingEntity livingCore) {
            // 髯ｷ・ｿ陋ｹ繝ｻ・ｽ・ｶ闔・･郢晢ｽｻ鬯ｨ・ｾ・つ髫ｰ・ｨ陜捺ｻ難ｽ｣・｡郢晢ｽｻ霑ｹ螟ｲ・ｽ・ｮ繝ｻ・ｾ髯橸ｽｳ陞溽ｿｫ繝ｻ髯晢ｽｾ繝ｻ・ｱ鬨ｾ・ｧ郢晢ｽｻ郢晢ｽｻ鬯ｨ・ｾ・つ髯滓汚・ｽ・ｺ髯溯ｶ｣・ｽ・ｦ
            KnockBackModifier modifier = KnockBackModifier.fromLivingEntity(livingCore);
            modifier.setKnockBackStrength(this.knockback);
            // 髯具ｽｻ陝ｶ蟷｢・ｽ・ｻ繝ｻ・ｺ髣費ｽｨ繝ｻ・､髯橸ｽｳ繝ｻ・ｳ
            tacAttackEntity(parts, damage, sources);
            // 髫ｲ・ｱ繝ｻ・｢髯樊ｻ難ｽｦ鬆托ｽｬ・｡髣厄ｽｴ郢晢ｽｻ
            modifier.resetKnockBackStrength();
        } else {
            // 髯具ｽｻ陝ｶ蟷｢・ｽ・ｻ繝ｻ・ｺ髣費ｽｨ繝ｻ・､髯橸ｽｳ繝ｻ・ｳ
            tacAttackEntity(parts, damage, sources);
        }
        // 髴趣ｽｷ郢晢ｽｻ邵ｺ螟撰ｽｨ・ｾ繝ｻ・ｻ鬮ｴ蠑ｱ繝ｻ
        if (this.explosion) {
            // 髯ｷ・ｿ陋ｹ繝ｻ・ｽ・ｶ陜捺ｺｯ・｣蜑ｰ・ｬ・ｨ隴ｴ・ｧ隲ｷ・ｮ鬯ｮ・｣繝ｻ・ｴ
            parts.core().invulnerableTime = 0;
            ExplodeUtil.createExplosion(this.getOwner(), this, this.explosionDamage, this.explosionRadius, this.explosionKnockback, this.explosionDestroyBlock, result.getLocation());
        }
        // 髯ｷ・ｿ繝ｻ・ｪ髯昴・・ｽ・ｹ LivingEntity 髫ｰ繝ｻ・ｽ・ｧ鬮ｯ・ｦ隰疲ｺ倥・髫ｴ蝣仰髯具ｽｻ繝ｻ・､髯橸ｽｳ郢晢ｽｻ
        if (parts.core() instanceof LivingEntity livingCore) {
            // 髣費｣ｰ陋滂ｽｶ繝ｻ・ｻ繝ｻ・ｶ髯ｷ・ｷ隴ｴ・ｧ繝ｻ・ｭ繝ｻ・･郢晢ｽｻ陟包ｽ｡繝ｻ・ｻ陷ｿ蛹≫・髯ｷ莨夲ｽｽ・｡鬩包ｽｶ繝ｻ・ｯ髯具ｽｻ繝ｻ・ｰ髯橸ｽｳ繝ｻ・｢髫ｰ魃会ｽｽ・ｷ鬩包ｽｶ繝ｻ・ｯ
            if (!level().isClientSide()) {
                int attackerId = attacker == null ? 0 : attacker.getId();
                // 髯橸ｽｯ郢ｧ蝓滂ｽ｣・｡鬨ｾ蠅難ｽｺ・ｽ魄溘・・ｱ繝ｻ・ｽ・ｻ髣費｣ｰ郢晢ｽｻ
                if (livingCore.isDeadOrDying()) {
                    ForgeEventCompat.post(new EntityKillByGunEvent(this, livingCore, attacker, newGunId, gunDisplayId, damage, sources, headshot, headShotMultiplier, LogicalSide.SERVER));
                    NetworkHandler.sendToDimension(new ServerMessageGunKill(getId(), livingCore.getId(), attackerId, newGunId, gunDisplayId, damage, headshot, headShotMultiplier), livingCore);
                } else {
                    ForgeEventCompat.post(new EntityHurtByGunEvent.Post(this, livingCore, attacker, newGunId, gunDisplayId, damage, sources, headshot, headShotMultiplier, LogicalSide.SERVER));
                    NetworkHandler.sendToDimension(new ServerMessageGunHurt(getId(), livingCore.getId(), attackerId, newGunId, gunDisplayId, damage, headshot, headShotMultiplier), livingCore);
                }
            }
        }
    }

    protected void onHitBlock(BlockHitResult result, Vec3 startVec, Vec3 endVec) {
        if (result.getType() == HitResult.Type.MISS) {
            return;
        }
        BlockPos pos = result.getBlockPos();
        Vec3 hitVec = result.getLocation();
        // 鬮ｫ證ｦ・ｽ・ｦ髯ｷ・ｿ陷ｿ・ｰ繝ｻ・ｺ陋滂ｽｶ繝ｻ・ｻ繝ｻ・ｶ
        // 髫ｰ・ｰ闔牙衷竏夐ｫｫ證ｦ・ｽ・ｦ髯ｷ・ｿ陷ｿ・ｰ繝ｻ・ｺ陋滂ｽｶ繝ｻ・ｻ繝ｻ・ｶ髣比ｼ夲ｽｽ・･鬮ｫ・ｶ繝ｻ・ｩ髣費｣ｰ陋滂ｽｶ繝ｻ・ｻ繝ｻ・ｶ髯ｷ・ｿ繝ｻ・ｯ髣比ｼ夲ｽｽ・･髯ｷ・ｿ陋ｹ繝ｻ・ｽ・ｶ闔・･隹ｺ・｡髴大､翫＃陜趣ｽｪ髯ｷ・ｻ繝ｻ・ｽ髣包ｽｳ繝ｻ・ｭ鬮ｯ・ｦ陟包ｽ｡繝ｻ・ｸ繝ｻ・ｺ郢晢ｽｻ闔蛹・ｽｽ・ｾ陷ｿ・･繝ｻ・ｦ郢ｧ閧ｲ・ｲ陋ｾ・ｫ謐ｺ・ｻ繧托ｽｽ・ｼ隴ｴ・ｧ鬩包｣ｰ髯区ｺｷ・ｸ譎・辞髯昴・魘ｻ繝ｻ・ｭ闔ｨ螟ｲ・ｽ・ｼ郢晢ｽｻ
        if (ForgeEventCompat.post(new AmmoHitBlockEvent(this.level(), result, this.level().getBlockState(pos), this))) {
            return;
        }
        super.onHitBlock(result);
        // 髴趣ｽｷ郢晢ｽｻ邵ｺ繝ｻ
        if (this.explosion) {
            Vec3 explosionPos = hitVec.add(
                    result.getDirection().getStepX() * EXPLOSION_SURFACE_OFFSET,
                    result.getDirection().getStepY() * EXPLOSION_SURFACE_OFFSET,
                    result.getDirection().getStepZ() * EXPLOSION_SURFACE_OFFSET
            );
            ExplodeUtil.createExplosion(this.getOwner(), this, this.explosionDamage, this.explosionRadius, this.explosionKnockback, this.explosionDestroyBlock, explosionPos);
            // 髴趣ｽｷ郢晢ｽｻ邵ｺ螟青・ｶ繝ｻ・ｴ髫ｰ證ｦ・ｽ・･隰・沺迹ｳ隰ｫ螟雁初陷･・ｲ髦ｨ闍難｣ｰ謇假ｽｽ・ｹ髯昴・・ｲ・ｻ繝ｻ・ｼ陟包ｽ｡繝ｻ・ｸ隶主･・ｽｽ・､郢晢ｽｻ霓､鬘伜ｵｯ陷ｿ・･鬪ｭ蛟ｬﾂ・ｧ郢晢ｽｻ・つ繝ｻ・ｻ鬮ｴ蠑ｱ繝ｻ
            this.discard();
            return;
        }
        // 髯滓汚・ｽ・ｹ髯昴・・ｯ雋ｻ・ｽ・ｸ陞ｳ蛹ｻ笳矩恷・ｯ郢晢ｽｻ鬮ｻ・ｳ髫ｰ・ｨ郢晢ｽｻ
        if (this.level() instanceof ServerLevel serverLevel) {
            BulletHoleOption bulletHoleOption = new BulletHoleOption(result.getDirection(), result.getBlockPos(), this.ammoId.toString(), this.gunId.toString(), this.gunDisplayId.toString());
            serverLevel.sendParticles(bulletHoleOption, hitVec.x, hitVec.y, hitVec.z, 1, 0, 0, 0, 0);
            if (this.igniteBlock) {
                serverLevel.sendParticles(ParticleTypes.LAVA, hitVec.x, hitVec.y, hitVec.z, 1, 0, 0, 0, 0);
            }
        }
        if (this.igniteBlock && AmmoConfig.IGNITE_BLOCK.get()) {
            BlockPos offsetPos = pos.relative(result.getDirection());
            if (BaseFireBlock.canBePlacedAt(this.level(), offsetPos, result.getDirection())) {
                BlockState fireState = BaseFireBlock.getState(this.level(), offsetPos);
                this.level().setBlock(offsetPos, fireState, Block.UPDATE_ALL_IMMEDIATE);
                ((ServerLevel) this.level()).sendParticles(ParticleTypes.LAVA, hitVec.x - 1.0 + this.random.nextDouble() * 2.0, hitVec.y, hitVec.z - 1.0 + this.random.nextDouble() * 2.0, 4, 0, 0, 0, 0);
            }
        }
        this.discard();
    }

    // 髫ｴ・ｬ繝ｻ・ｹ髫ｰ謦ｰ・ｽ・ｮ鬮ｴ閧ｴ蛻ｮ繝ｻ・ｦ繝ｻ・ｻ鬮ｴ螟ｧ・ｹ繝ｻ・ｽ・｡陟包ｽ｡繝ｻ・ｼ繝ｻ・､髯橸ｽｳ繝ｻ・ｳ鬮ｯ・ｦ繝ｻ・ｰ髯ｷ繝ｻ閾・・・ｮ繝ｻ・ｾ鬮ｫ・ｶ繝ｻ・｡
    public float getDamage(Vec3 hitVec) {
        // 髯橸ｽｯ郢ｧ蝓滂ｽ｣・｡髯滂ｽ｢陋滂ｽｩ繝ｻ・ｮ繝ｻ・ｰ髯ｷﾂ陷ｻ蜿鳴蜻ｵ譽斐・・ｧ髯区ｻゑｽｽ・ｼ郢晢ｽｻ驕停・竕ｦ髫ｰ謔滂ｽ､・ｧ繝ｻ・ｰ繝ｻ・ｱ鬨ｾ・ｶ繝ｻ・ｴ髫ｰ證ｦ・ｽ・･鬮ｫ・ｶ繝ｻ・､髣包ｽｳ繝ｻ・ｺ髣厄ｽｴ繝ｻ・ｰ髣費ｽｨ繝ｻ・､髯橸ｽｳ繝ｻ・ｳ髣包ｽｳ繝ｻ・ｺ 0
        float base = 0;
        // 鬯ｩ蠅難ｽｦ鬘費ｽｴ蜥取≧陝ｷ繝ｻ・ｽ・｡隰疲ｻ薙・髫ｴ繝ｻ・ｽ・ｭ
        double playerDistance = hitVec.distanceTo(this.startPos);
        for (DistanceDamagePair pair : this.damageAmount) {
            float effectiveDistance = this.damageAmount.get(0).getDistance() == pair.getDistance() ? this.distanceAmount : pair.getDistance();
            if (playerDistance < effectiveDistance) {
                float damage = pair.getDamage();
                base = Math.max(damage * this.damageModifier, 0F);
                break;
            }
        }
        // 鬮ｫ・ｶ繝ｻ・ｩ鬮｢・ｼ陞｢・ｽ隰費ｽｽ髣厄ｽｫ繝ｻ・ｮ髫ｰ・ｾ繝ｻ・ｹ髫ｴ・ｫ繝ｻ・ｪ髫ｴ・ｴ繝ｻ・ｰ髣費ｽｨ繝ｻ・､髯橸ｽｳ繝ｻ・ｳ
        return modifyProperty(GunProperties.DAMAGE, Float.class, base);
    }

    /**
     * @since 1.1.7
     */
    private <T> T modifyProperty(GunProperty<?> prop, Class<T> type, T original) {
        return modifyProperty(prop.name(), type, original);
    }

    /**
     * @since 1.1.7
     */
    private <T> T modifyProperty(String id, Class<T> type, T original) {
        if (getOwner() instanceof LivingEntity shooter) {
            ItemStack gun = shooter.getMainHandItem();
            if (gun.getItem() instanceof AbstractGunItem gunInterface && Objects.equals(this.gunId, gunInterface.getGunId(gun))) {
                ShooterDataHolder dataHolder = IGunOperator.fromLivingEntity(shooter).getDataHolder();
                return gunInterface.modifyProperty(dataHolder, gun, shooter, id, type, original);
            }
        }
        return original;
    }

    /**
     * @return Pair<鬯ｮ・ｱ隶捺慣・ｽ・ｩ繝ｻ・ｿ鬨ｾ蛹・ｽｽ・ｲ髣費ｽｨ繝ｻ・､髯橸ｽｳ繝ｻ・ｳ髮九・閻ｸ繝ｻ・ｼ隶呵ｶ｣・ｽ・ｩ繝ｻ・ｿ鬨ｾ蛹・ｽｽ・ｲ髣費ｽｨ繝ｻ・､髯橸ｽｳ繝ｻ・ｳ髮九・繝ｻ
     */
    private Pair<DamageSource, DamageSource> createDamageSources(MaybeMultipartEntity parts) {
        DamageSource source1, source2;
        var hitPartType = parts.hitPart().getType();
        var directCause = hitPartType.builtInRegistryHolder().is(PRETEND_MELEE_DAMAGE_ON) ? this.getOwner() : this;
        // 隰・ｺ･邯懆｢・ｰ髯溷私・ｽ・ｱ髣費｣ｰ繝ｻ・ｺ鬯ｨ・ｾ繝ｻ・ｰ髫ｰ迹壽巡繝ｻ・ｼ繝ｻ・､髯橸ｽｳ繝ｻ・ｳ
        if (hitPartType.builtInRegistryHolder().is(USE_MAGIC_DAMAGE_ON)) {
            source1 = source2 = this.damageSources().indirectMagic(this, getOwner());
        } else if (hitPartType.builtInRegistryHolder().is(USE_VOID_DAMAGE_ON)) {
            source1 = ModDamageTypes.Sources.bulletVoid(this.level().registryAccess(), directCause, this.getOwner(), false);
            source2 = ModDamageTypes.Sources.bulletVoid(this.level().registryAccess(), directCause, this.getOwner(), true);
        } else {
            source1 = ModDamageTypes.Sources.bullet(this.level().registryAccess(), directCause, this.getOwner(), false);
            source2 = ModDamageTypes.Sources.bullet(this.level().registryAccess(), directCause, this.getOwner(), true);
        }
        return Pair.of(source1, source2);
    }

    private void tacAttackEntity(MaybeMultipartEntity parts, float damage, Pair<DamageSource, DamageSource> sources) {
        var source1 = sources.getLeft();
        var source2 = sources.getRight();
        // 鬩包ｽｨ繝ｻ・ｿ鬨ｾ蛹・ｽｽ・ｲ髣費ｽｨ繝ｻ・､髯橸ｽｳ繝ｻ・ｳ髯ｷ・･隴ｴ・ｧ陷搾ｽｸ鬯ｨ・ｾ陞｢・ｻ繝ｻ・ｼ繝ｻ・､髯橸ｽｳ繝ｻ・ｳ鬨ｾ・ｧ郢晢ｽｻ繝ｻ・ｯ雎郁ｲｻ・ｽ・ｾ驍・私・ｽ・ｮ繝ｻ・｡鬩阪ｅ繝ｻ
        float armorDamagePercent = Mth.clamp(this.armorIgnore, 0.0F, 1.0F);
        float normalDamagePercent = 1 - armorDamagePercent;
        // 髯ｷ・ｿ陋ｹ繝ｻ・ｽ・ｶ陜捺ｺｯ・｣蜑ｰ・ｬ・ｨ隴ｴ・ｧ隲ｷ・ｮ鬯ｮ・｣繝ｻ・ｴ
        parts.core().invulnerableTime = 0;
        // 髫ｴ雜｣・ｽ・ｮ鬯ｨ・ｾ陞｢・ｻ繝ｻ・ｼ繝ｻ・､髯橸ｽｳ繝ｻ・ｳ
        parts.hitPart().hurt(source1, damage * normalDamagePercent);
        // 髯ｷ・ｿ陋ｹ繝ｻ・ｽ・ｶ陜捺ｺｯ・｣蜑ｰ・ｬ・ｨ隴ｴ・ｧ隲ｷ・ｮ鬯ｮ・｣繝ｻ・ｴ
        parts.core().invulnerableTime = 0;
        // 鬩包ｽｨ繝ｻ・ｿ鬨ｾ蛹・ｽｽ・ｲ髣費ｽｨ繝ｻ・､髯橸ｽｳ繝ｻ・ｳ
        parts.hitPart().hurt(source2, damage * armorDamagePercent);
    }

    @Override
    public Packet<ClientGamePacketListener> getAddEntityPacket(ServerEntity serverEntity) {
        return net.minecraftforge.common.ForgeHooks.getEntitySpawnPacket(this);
    }

    @Override
    public void writeSpawnData(FriendlyByteBuf buffer) {
        buffer.writeFloat(getXRot());
        buffer.writeFloat(getYRot());
        buffer.writeDouble(getDeltaMovement().x);
        buffer.writeDouble(getDeltaMovement().y);
        buffer.writeDouble(getDeltaMovement().z);
        Entity entity = getOwner();
        buffer.writeInt(entity != null ? entity.getId() : 0);
        buffer.writeIdentifier(ammoId);
        buffer.writeFloat(this.gravity);
        buffer.writeBoolean(this.explosion);
        buffer.writeBoolean(this.igniteEntity);
        buffer.writeBoolean(this.igniteBlock);
        buffer.writeFloat(this.explosionRadius);
        buffer.writeFloat(this.explosionDamage);
        buffer.writeInt(this.life);
        buffer.writeFloat(this.speed);
        buffer.writeFloat(this.friction);
        buffer.writeInt(this.pierce);
        buffer.writeBoolean(this.isTracerAmmo);
        buffer.writeIdentifier(this.gunId);
        buffer.writeIdentifier(this.gunDisplayId);
        buffer.writeLong(this.shootTimestamp);
    }

    @Override
    public void readSpawnData(FriendlyByteBuf additionalData) {
        setXRot(additionalData.readFloat());
        setYRot(additionalData.readFloat());
        setDeltaMovement(additionalData.readDouble(), additionalData.readDouble(), additionalData.readDouble());
        Entity entity = this.level().getEntity(additionalData.readInt());
        if (entity != null) {
            this.setOwner(entity);
        }
        this.ammoId = additionalData.readIdentifier();
        this.gravity = additionalData.readFloat();
        this.explosion = additionalData.readBoolean();
        this.igniteEntity = additionalData.readBoolean();
        this.igniteBlock = additionalData.readBoolean();
        this.explosionRadius = additionalData.readFloat();
        this.explosionDamage = additionalData.readFloat();
        this.life = additionalData.readInt();
        this.speed = additionalData.readFloat();
        this.friction = additionalData.readFloat();
        this.pierce = additionalData.readInt();
        this.isTracerAmmo = additionalData.readBoolean();
        this.gunId = additionalData.readIdentifier();
        this.gunDisplayId = additionalData.readIdentifier();
        this.shootTimestamp = additionalData.readLong();
        if (this.level().isClientSide() && this.getOwner() instanceof LocalPlayer
                && Minecraft.getInstance().options.getCameraType().isFirstPerson()) {
            GunItemRendererWrapper.ShotMuzzleBasis shotMuzzleBasis = GunItemRendererWrapper.getShotMuzzleBasis(this.shootTimestamp);
            if (shotMuzzleBasis != null) {
                this.cameraXRot = shotMuzzleBasis.cameraXRot();
                this.cameraYRot = shotMuzzleBasis.cameraYRot();
                this.firstPersonRenderOffset = shotMuzzleBasis.offset();
                this.clientFirstPersonRenderOrigin = shotMuzzleBasis.eyePosition();
                this.clientFirstPersonRenderCapturedAt = shotMuzzleBasis.capturedAt();
            } else if (GunItemRendererWrapper.hasRecentShotMuzzleBasis()) {
                this.cameraXRot = GunItemRendererWrapper.getLatestShotCameraXRot();
                this.cameraYRot = GunItemRendererWrapper.getLatestShotCameraYRot();
                this.firstPersonRenderOffset = GunItemRendererWrapper.copyLatestShotMuzzleOffset();
                LocalPlayer player = Minecraft.getInstance().player;
                if (player != null) {
                    this.clientFirstPersonRenderOrigin = player.getEyePosition();
                    this.clientFirstPersonRenderCapturedAt = System.currentTimeMillis();
                }
            } else {
                LocalPlayer player = Minecraft.getInstance().player;
                if (player != null) {
                    this.cameraXRot = player.getXRot();
                    this.cameraYRot = player.getYRot();
                    this.clientFirstPersonRenderOrigin = player.getEyePosition();
                    this.clientFirstPersonRenderCapturedAt = System.currentTimeMillis();
                } else {
                    var camera = Minecraft.getInstance().gameRenderer.getMainCamera();
                    this.cameraXRot = camera.xRot();
                    this.cameraYRot = camera.yRot();
                    this.clientFirstPersonRenderOrigin = camera.position();
                    this.clientFirstPersonRenderCapturedAt = System.currentTimeMillis();
                }
                this.firstPersonRenderOffset = new Vector3f(GunItemRendererWrapper.muzzleRenderOffset);
            }
        }
    }

    public Identifier getAmmoId() {
        return ammoId;
    }

    public Identifier getGunId() {
        return gunId;
    }

    public Identifier getGunDisplayId() {
        return gunDisplayId;
    }

    public boolean isTracerAmmo() {
        return isTracerAmmo;
    }

    public boolean isExplosionBullet() {
        return explosion;
    }

    public RandomSource getRandom() {
        return this.random;
    }

    public float getCameraYRot() {
        return cameraYRot;
    }

    public void setCameraYRot(float cameraYRot) {
        this.cameraYRot = cameraYRot;
    }

    public float getCameraXRot() {
        return cameraXRot;
    }

    public void setCameraXRot(float cameraXRot) {
        this.cameraXRot = cameraXRot;
    }

    public Vector3f getFirstPersonRenderOffset() {
        return firstPersonRenderOffset;
    }

    public void setFirstPersonRenderOffset(Vector3f originRenderOffset) {
        this.firstPersonRenderOffset = originRenderOffset;
    }

    public boolean hasClientFirstPersonRenderPrediction() {
        return clientFirstPersonRenderOrigin != null && clientFirstPersonRenderCapturedAt >= 0L;
    }

    public Vec3 getClientFirstPersonRenderPosition(Vec3 renderOrigin, float partialTicks) {
        double elapsedTicks = Math.max(0.0D, this.tickCount + partialTicks);
        Vec3 movement = this.getDeltaMovement();
        Vec3 shotOrigin = this.clientFirstPersonRenderOrigin != null ? this.clientFirstPersonRenderOrigin : renderOrigin;
        Vec3 spreadPredictedPosition = shotOrigin.add(movement.scale(elapsedTicks));
        Vec3 shotForward = getShotCameraForward();
        Vec3 cameraPredictedPosition = shotOrigin.add(shotForward.scale(movement.length() * elapsedTicks));
        double distance = spreadPredictedPosition.distanceTo(shotOrigin);
        double ballisticVisualWeight = getFirstPersonBallisticVisualWeight(distance);
        Vec3 predictedPosition = cameraPredictedPosition.lerp(spreadPredictedPosition, ballisticVisualWeight);
        Vec3 actualPosition = this.getPosition(partialTicks);
        double actualWeight = Mth.clamp(
                (distance - FIRST_PERSON_PREDICTION_BLEND_START_DISTANCE)
                        / (FIRST_PERSON_PREDICTION_BLEND_END_DISTANCE - FIRST_PERSON_PREDICTION_BLEND_START_DISTANCE),
                0.0D,
                1.0D
        );
        return predictedPosition.lerp(actualPosition, actualWeight);
    }

    private static double getFirstPersonBallisticVisualWeight(double distance) {
        return Mth.clamp(
                (distance - FIRST_PERSON_BALLISTIC_VISUAL_BLEND_START_DISTANCE)
                        / (FIRST_PERSON_BALLISTIC_VISUAL_BLEND_END_DISTANCE - FIRST_PERSON_BALLISTIC_VISUAL_BLEND_START_DISTANCE),
                0.0D,
                1.0D
        );
    }

    private Vec3 getShotCameraForward() {
        Vector3d forward = new Vector3d(0.0D, 0.0D, 1.0D);
        forward.rotateX(this.cameraXRot * Mth.DEG_TO_RAD);
        forward.rotateY(-this.cameraYRot * Mth.DEG_TO_RAD);
        return new Vec3(forward.x, forward.y, forward.z).normalize();
    }

    public Optional<float[]> getTracerColorOverride() {
        var pd = getPersistentData();
        if (!NbtCompat.contains(pd, TRACER_COLOR_OVERRIDER_KEY, Tag.TAG_INT_ARRAY)) {
            return Optional.empty();
        } else {
            var ints = NbtCompat.getIntArrayOrEmpty(pd, TRACER_COLOR_OVERRIDER_KEY);
            // 鬮ｫ・ｸ繝ｻ・ｷ鬯ｩ蛹・ｽｽ・ｿ髯ｷ莠･・ｰ・ｺ繝ｻ・ｽ繝ｻ・ｿ鬨ｾ蛹・ｽｽ・ｨ 1 髫ｰ謔溘・・つ郢晢ｽｻ2 髣包ｽｳ繝ｻ・ｪ髯区ｻゑｽｽ・ｼ鬨ｾ・ｧ郢晢ｽｻ霎溷｣ｽ豐ｿ郢晢ｽｻ・つ郢晢ｽｻ
            // 髮弱・・ｽ・､髯樊ｺ倥・1~2 髣包ｽｳ繝ｻ・ｪ髯区ｻゑｽｽ・ｼ鬨ｾ・ｧ郢晢ｽｻ郢晢ｽｻ髫ｰ・ｾ繝ｻ・ｯ髣泌ｳｨ繝ｻ繝ｻ・ｸ繝ｻ・ｺ髣費ｽｨ陋滂ｽｬ陝・歓闊峨・・ｰ髯樊ｺ倥・霓､鬘假｣ｰ莉｣・・・・ｸ繝ｻ・ｸ髫ｲ・ｰ郢晢ｽｻ郢晢ｽｻ髫ｴ螟ｲ・ｽ・･髣比ｼ夲ｽｽ・｣髫ｴ蜴・ｽｽ・ｿ髯晢｣ｰ繝ｻ・ｩ髮九・繝ｻ陜ｨ螳壽割隲帙・鬟ｭ髫ｰ證ｦ・ｽ・ｪ髫ｴ繝ｻ・ｽ・ｽ :(
            switch (ints.length) {
                case 0:
                    return Optional.empty();
                case 1: {
                    var albedo = ints[0] / 255F;
                    return Optional.of(new float[]{albedo, albedo, albedo, 1});
                }
                case 2: {
                    var albedo = ints[0] / 255F;
                    var alpha = ints[1] / 255F;
                    return Optional.of(new float[]{albedo, albedo, albedo, alpha});
                }
                case 3: {
                    var r = ints[0] / 255F;
                    var g = ints[1] / 255F;
                    var b = ints[2] / 255F;
                    return Optional.of(new float[]{r, g, b, 1});
                }
                default: {
                    var r = ints[0] / 255F;
                    var g = ints[1] / 255F;
                    var b = ints[2] / 255F;
                    var a = ints[3] / 255F;
                    return Optional.of(new float[]{r, g, b, a});
                }
            }
        }
    }

    public float getTracerSizeOverride() {
        var pd = getPersistentData();
        return (NbtCompat.contains(pd, TRACER_SIZE_OVERRIDER_KEY, Tag.TAG_BYTE)
                || NbtCompat.contains(pd, TRACER_SIZE_OVERRIDER_KEY, Tag.TAG_SHORT)
                || NbtCompat.contains(pd, TRACER_SIZE_OVERRIDER_KEY, Tag.TAG_INT)
                || NbtCompat.contains(pd, TRACER_SIZE_OVERRIDER_KEY, Tag.TAG_LONG)
                || NbtCompat.contains(pd, TRACER_SIZE_OVERRIDER_KEY, Tag.TAG_FLOAT)
                || NbtCompat.contains(pd, TRACER_SIZE_OVERRIDER_KEY, Tag.TAG_DOUBLE))
                ? NbtCompat.getFloat(pd, TRACER_SIZE_OVERRIDER_KEY)
                : 1;
    }

    @Override
    public boolean ownedBy(@Nullable Entity entity) {
        if (entity == null) {
            return false;
        }
        return super.ownedBy(entity);
    }

    public static class EntityResult {
        private final Entity entity;
        private final Vec3 hitVec;
        private final boolean headshot;

        public EntityResult(Entity entity, Vec3 hitVec, boolean headshot) {
            this.entity = entity;
            this.hitVec = hitVec;
            this.headshot = headshot;
        }

        // 髯昴・・ｻ蜻ｻ・ｽ・ｼ繝ｻ・ｹ髯ｷ・ｻ繝ｻ・ｽ髣包ｽｳ繝ｻ・ｭ鬨ｾ・ｧ郢晢ｽｻ繝ｻ・ｮ隶楢ｲｻ・ｽ・ｽ郢晢ｽｻ
        public Entity getEntity() {
            return this.entity;
        }

        // 髯昴・・ｻ蜻ｻ・ｽ・ｼ繝ｻ・ｹ髯ｷ・ｻ繝ｻ・ｽ髣包ｽｳ繝ｻ・ｭ鬨ｾ・ｧ郢晢ｽｻ繝ｻ・ｽ陷･・ｲ繝ｻ・ｽ繝ｻ・ｮ
        public Vec3 getHitPos() {
            return this.hitVec;
        }

        // 髫ｴ謫ｾ・ｽ・ｯ髯ｷ・ｷ繝ｻ・ｦ髣包ｽｳ繝ｻ・ｺ髴趣ｽｷ郢晢ｽｻ繝ｻ・､繝ｻ・ｴ
        public boolean isHeadshot() {
            return this.headshot;
        }
    }
}


