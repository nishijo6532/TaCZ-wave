package com.tacz.guns.item;

import com.tacz.guns.GunMod;
import com.tacz.guns.api.DefaultAssets;
import com.tacz.guns.api.GunProperties;
import com.tacz.guns.api.GunProperty;
import com.tacz.guns.api.TimelessAPI;
import com.tacz.guns.api.entity.IGunOperator;
import com.tacz.guns.api.event.common.GunFireEvent;
import com.tacz.guns.api.item.IAmmo;
import com.tacz.guns.api.item.IAmmoBox;
import com.tacz.guns.api.item.attachment.AttachmentType;
import com.tacz.guns.api.item.gun.AbstractGunItem;
import com.tacz.guns.api.item.gun.FireMode;
import com.tacz.guns.api.util.LuaEntityAccessor;
import com.tacz.guns.api.util.LuaNbtAccessor;
import com.tacz.guns.client.animation.statemachine.GunAnimationStateContext;
import com.tacz.guns.config.common.AmmoConfig;
import com.tacz.guns.entity.EntityKineticBullet;
import com.tacz.guns.entity.shooter.ShooterDataHolder;
import com.tacz.guns.network.NetworkHandler;
import com.tacz.guns.network.message.event.ServerMessageGunFire;
import com.tacz.guns.resource.index.CommonGunIndex;
import com.tacz.guns.resource.modifier.AttachmentCacheProperty;
import com.tacz.guns.resource.modifier.custom.SilenceModifier;
import com.tacz.guns.resource.pojo.data.gun.*;
import com.tacz.guns.sound.SoundManager;
import com.tacz.guns.util.AttachmentDataUtils;
import com.tacz.guns.util.CycleTaskHelper;
import com.tacz.guns.util.ForgeEventCompat;
import com.tacz.guns.util.NbtCompat;
import it.unimi.dsi.fastutil.Pair;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.LogicalSide;
import org.luaj.vm2.LuaError;
import org.luaj.vm2.LuaFunction;
import org.luaj.vm2.LuaTable;
import org.luaj.vm2.LuaValue;
import org.luaj.vm2.lib.jse.CoerceJavaToLua;

import java.util.Objects;
import java.util.Optional;
import java.util.function.Supplier;

@SuppressWarnings("unused")
public class ModernKineticGunScriptAPI {
    public static String MARKER = "ScriptAPI";
    private static final float EXPLOSIVE_PROJECTILE_SPEED_SCALE = 0.25F;
    private static final float GRENADE_PROJECTILE_SPEED_SCALE = 0.5F;

    private LivingEntity shooter;

    private ShooterDataHolder dataHolder;

    private ItemStack itemStack;

    private AbstractGunItem abstractGunItem;

    private CommonGunIndex gunIndex;

    private Identifier gunId;

    private Identifier gunDisplayId;

    private Supplier<Float> pitchSupplier;

    private Supplier<Float> yawSupplier;

    private LuaNbtAccessor nbtUtil;

    private LuaEntityAccessor entityAccessor;

    /**
     * 闔ｷ蜿也自螳ｶ逧・ｱ樊ｧ郛灘ｭ倅ｸｭ郛灘ｭ倡噪蛟ｼ縲・
     * 隸ｷ蜍ｿ闔ｷ蜿匁凶譛牙､肴揩謨ｰ謐ｮ扈捺桷逧・ｱ樊ｧ蛟ｼ・瑚ｯ･陦御ｸｺ譏ｯ譛ｪ螳壻ｹ臥噪縲・
     *
     * @param id 螻樊ｧ id・瑚ｯｷ蜿る・ {@link GunProperties}
     * @see com.tacz.guns.api.CacheModifiableByScript
     * @return 螻樊ｧ逧・ｼ
     *
     * @author ChloePrime
     * @since 1.1.7
     */
    public LuaValue getCachedProperty(String id) {
        GunProperty<?> property = GunProperties.all().get(id);
        if (property == null) {
            throw new LuaError("unknown gun property: " + id);
        }
        AttachmentCacheProperty cache = IGunOperator.fromLivingEntity(shooter).getCacheProperty();
        if (cache == null) {
            return LuaValue.NIL;
        }
        return CoerceJavaToLua.coerce(cache.getCache(id));
    }

    /**
     * 謇ｧ陦御ｸ谺｡螳梧紛逧・ｰ・・騾ｻ霎托ｼ御ｼ夊・剔邇ｩ螳ｶ逧・憾諤・譏ｯ蜷ｦ蝨ｨ迸・㊥縲∵弍蜷ｦ蝨ｨ遘ｻ蜉ｨ縲∵弍蜷ｦ蝨ｨ蛹榊倹遲・縲・・莉ｶ謨ｰ蛟ｼ蠖ｱ蜩阪∝､壼ｼｹ荳ｸ謨｣蟆・∬ｿ槫書・梧眺謾ｾ蠑轣ｫ髻ｳ謨医・
     * @param consumeAmmo 譛ｬ谺｡蟆・・譏ｯ蜷ｦ豸郁怜ｼｹ闕ｯ
     */
    public void shootOnce(boolean consumeAmmo){
        GunData gunData = gunIndex.getGunData();
        BulletData bulletData = gunIndex.getBulletData();
        IGunOperator gunOperator = IGunOperator.fromLivingEntity(shooter);

        // 闔ｷ蜿夜・莉ｶ謨ｰ謐ｮ郛灘ｭ・
        AttachmentCacheProperty cacheProperty = gunOperator.getCacheProperty();
        if (cacheProperty == null) {
            return;
        }

        //Handle Heat Data
        float heatInaccuracy = 1f;
        if(hasHeatData()) {
            GunHeatData heatData = Objects.requireNonNull(gunIndex.getGunData().getHeatData());
            float heatMax = modifyProperty(GunProperties.RuntimeOnly.MAX_HEAT, Float.class, heatData.getHeatMax());
            float heatPercentage = (getHeatAmount() / heatMax);
            heatInaccuracy *= Mth.lerp(heatPercentage, heatData.getMinInaccuracy(), heatData.getMaxInaccuracy());
        }

        // 謨｣蟆・ｽｱ蜩・
        InaccuracyType inaccuracyType = InaccuracyType.getInaccuracyType(shooter);
        final float unmodifiedInaccuracy = cacheProperty.getCache(GunProperties.INACCURACY).get(inaccuracyType) * heatInaccuracy;
        final float inaccuracy = Math.max(0, modifyProperty(GunProperties.INACCURACY, Float.class, unmodifiedInaccuracy));

        // 豸磯浹蝎ｨ蠖ｱ蜩・
        // 菴ｿ逕ｨ豸磯浹霑吩ｸｪ騾蛾｡ｹ蟇ｹ莠主ｰ・焔譚･隸ｴ譏ｯ蝨ｨ螳｢謌ｷ遶ｯ螟・炊逧・ｼ瑚・譛ｬ謾ｹ莠・ｲ｡逕ｨ・梧園莉･蟷ｲ閼・ｸ崎ｮｩ謾ｹ莠・
        Pair<Integer, Boolean> silence = cacheProperty.getCache(SilenceModifier.ID);
        final int soundDistance = modifyProperty(GunProperties.RuntimeOnly.SOUND_DISTANCE, Integer.class, silence.left());
        final boolean useSilenceSound = silence.right();

        // 蟄仙ｼｹ鬟櫁｡碁溷ｺｦ
        float speed = modifyProperty(GunProperties.AMMO_SPEED, Float.class, cacheProperty.getCache(GunProperties.AMMO_SPEED));
        speed *= AmmoConfig.GLOBAL_BULLET_SPEED_MODIFIER.get();
        float processedSpeed = Mth.clamp(speed / 20, 0, Float.MAX_VALUE);
        // 蠑ｹ荳ｸ謨ｰ驥・
        int bulletAmount = modifyProperty(GunProperties.RuntimeOnly.BULLET_AMOUNT, Integer.class, Math.max(bulletData.getBulletAmount(), 1));

        // 霑槫書謨ｰ驥・
        FireMode fireMode = abstractGunItem.getFireMode(itemStack);
        int cycles = modifyProperty(GunProperties.RuntimeOnly.BURST_COUNT, Integer.class, fireMode == FireMode.BURST ? gunData.getBurstData().getCount() : 1);
        // 霑槫書髣ｴ髫・
        long period = modifyProperty(GunProperties.RuntimeOnly.BURST_SHOOT_INTERVAL, Long.class, fireMode == FireMode.BURST ? gunData.getBurstShootInterval() : 1);

        CycleTaskHelper.addCycleTask(() -> {
            // 螯よ棡蟆・・閠・ｭｻ莠｡・悟叙豸亥ｰ・・
            if (shooter.isDeadOrDying()) {
                return false;
            }
            // 螯よ棡豁ｦ蝎ｨ蜿倅ｺ・ｼ悟叙豸亥ｰ・・
            if (!shooter.getMainHandItem().equals(itemStack) || shooter.getMainHandItem().isEmpty()) {
                return false;
            }
            // 隗ｦ蜿大・蜿台ｺ倶ｻｶ
            boolean fire = !ForgeEventCompat.post(new GunFireEvent(shooter, itemStack, LogicalSide.SERVER));
            if (fire) {
                NetworkHandler.sendToTrackingEntity(new ServerMessageGunFire(shooter.getId(), itemStack), shooter);
                // 蜑雁㍼蠑ｹ闕ｯ
                if (consumeAmmo) {
                    if (!this.reduceAmmoOnce()) {
                        return false;
                    }
                }
                //Handle Heat Data
                if(gunIndex.getGunData().hasHeatData()) {
                    Optional.ofNullable(gunIndex.getScript())
                            .map(script -> checkFunction(script.get("handle_shoot_heat")))
                            .ifPresentOrElse(
                                    func -> func.call(CoerceJavaToLua.coerce(this)),
                                    this::handleShootHeat
                            );
                }
                // 闔ｷ蜿門ｰ・・譁ｹ蜷托ｼ・itch 蜥・yaw・・
                float pitch = pitchSupplier != null ? pitchSupplier.get() : shooter.getXRot();
                float yaw = yawSupplier != null ? yawSupplier.get() : shooter.getYRot();
                // 逕滓・蟄仙ｼｹ
                Level world = shooter.level();
                Identifier ammoId = gunData.getAmmoId();
                float projectileSpeed = processedSpeed;
                if (bulletData.getExplosionData() != null && bulletData.getExplosionData().isExplode()) {
                    projectileSpeed *= "40mm".equals(ammoId.getPath())
                            ? GRENADE_PROJECTILE_SPEED_SCALE
                            : EXPLOSIVE_PROJECTILE_SPEED_SCALE;
                }
                for (int i = 0; i < bulletAmount; i++) {
                    boolean isTracer = bulletData.hasTracerAmmo() && gunOperator.nextBulletIsTracer(bulletData.getTracerCountInterval());
                    EntityKineticBullet bullet = new EntityKineticBullet(world, shooter, itemStack, ammoId, gunId,
                            gunDisplayId, isTracer, gunData, bulletData);
                    bullet.applyShotgunDamageSpread(bulletAmount);
                    abstractGunItem.doBulletSpread(dataHolder, itemStack, shooter, bullet, i, projectileSpeed,
                            inaccuracy, pitch, yaw);
                    world.addFreshEntity(bullet);
                    if (!world.isClientSide()) {
                        bullet.tick();
                    }
                }
                // 謦ｭ謾ｾ譫ｪ螢ｰ
                if (soundDistance > 0) {
                    String soundId = useSilenceSound ? SoundManager.SILENCE_3P_SOUND : SoundManager.SHOOT_3P_SOUND;
                    SoundManager.sendSoundToNearby(shooter, soundDistance, gunId, gunDisplayId, soundId, 0.8f, 0.9f + shooter.getRandom().nextFloat() * 0.125f);
                }
            }
            return true;
        }, period, cycles);
    }

    private <T> T modifyProperty(GunProperty<?> property, Class<T> type, T value) {
        return abstractGunItem.modifyProperty(dataHolder, itemStack, shooter, property, type, value);
    }

    private <T> T modifyProperty(String id, Class<T> type, T value) {
        return abstractGunItem.modifyProperty(dataHolder, itemStack, shooter, id, type, value);
    }

    /**
     * 螟・炊荳谺｡蟆・・逧・ｿ・Ο蜿伜喧
     */
    public void handleShootHeat() {
        GunHeatData heatData = gunIndex.getGunData().getHeatData();
        if (heatData == null) {
            return;
        }
        float newHeat = Math.min(abstractGunItem.getHeatAmount(itemStack) + heatData.getHeatPerShot(), heatData.getHeatMax());
        abstractGunItem.setHeatAmount(itemStack, newHeat);
        if (newHeat >= heatData.getHeatMax()) {
            abstractGunItem.setOverheatLocked(itemStack, true);
        }
    }

    /**
     * 隶ｩ譫ｪ譴ｰ蜀・噪蟄仙ｼｹ蜃丞ｰ台ｸ蜿代ゆｼ夐・莉取灘勘縲・溜閹帛ｾ・・蜥悟ｼ閹帛ｾ・惻逧・ｧ・ｾ具ｼ梧ｶ郁玲棯邂｡蜀・ｭ仙ｼｹ謌冶・ｼｹ蛹｣蜀・ｭ仙ｼｹ縲・
     * 螯よ棡豐｡譛牙庄莉･豸郁礼噪蟄仙ｼｹ・瑚ｿ吩ｸｪ譁ｹ豕穂ｼ夊ｿ泌屓 false縲ゆｾ句ｦよ灘勘豁･譫ｪ・瑚區辟ｶ蠑ｹ蛹｣蜀・怏蟄仙ｼｹ・御ｽ・弍蝨ｨ bolt 荵句燕譫ｪ邂｡蜀・ｲ｡譛牙ｭ仙ｼｹ・碁ぅ荵亥ｰｱ莨夊ｿ泌屓 false・・
     * @return 譏ｯ蜷ｦ謌仙粥蜃丞ｰ大ｭ仙ｼｹ縲・
     */
    public boolean reduceAmmoOnce() {
        Bolt boltType = TimelessAPI.getCommonGunIndex(abstractGunItem.getGunId(itemStack))
                .map(index -> index.getGunData().getBolt())
                .orElse(null);
        // 閹帛・譏ｯ蜷ｦ譛牙ｭ仙ｼｹ
        boolean hasAmmoInBarrel = abstractGunItem.hasBulletInBarrel(itemStack) && boltType != Bolt.OPEN_BOLT;
        // 閭悟桁蜀・弍蜷ｦ霑俶怏蟄仙ｼｹ (蛻幃讓｡蠑乗弍蜷ｦ豸郁苓レ蛹・､・ｼｹ)
        boolean hasInventoryAmmo = abstractGunItem.hasInventoryAmmo(shooter, itemStack, isShootingNeedConsumeAmmo());
        // 蛻､譁ｭ豐｡譛牙ｭ仙ｼｹ逧・擅莉ｶ (閭悟桁逶ｴ隸ｻ荳泌桁蜀・ｲ｡蟄仙ｼｹ / 髱櫁レ蛹・峩隸ｻ荳泌ｼｹ蛹｣蟄仙ｼｹ謨ｰ < 1)
        boolean noAmmo = useInventoryAmmo() && !hasInventoryAmmo ||
                !useInventoryAmmo() && abstractGunItem.getCurrentAmmoCount(itemStack) < 1;
        if (boltType == null) {
            return false;
        }
        // 譬灘勘騾ｻ霎・
        if (boltType == Bolt.MANUAL_ACTION) {
            // 豐｡譛芽・蜀・ｭ仙ｼｹ譌豕募ｰ・・
            if (!hasAmmoInBarrel) {
                return false;
            }
            // 豐｡譛牙ｼｹ蛹｣蜀・噪蟄仙ｼｹ蛻呎ｶ郁玲棯閹帛・逧・ｭ仙ｼｹ
            abstractGunItem.setBulletInBarrel(itemStack, false);
            return true;
        }
        // 髣ｭ閹幃ｻ霎・
        if (boltType == Bolt.CLOSED_BOLT) {
            // 螯よ棡譛牙ｼｹ蛹｣蜀・噪蟄仙ｼｹ蛻吩ｼ伜・豸郁怜ｼｹ蛹｣蜀・噪蟄仙ｼｹ
            if (!noAmmo) {
                // 螯よ棡閭悟桁逶ｴ隸ｻ蛻呵レ蛹・・蟆・・蜷主ｼｹ闕ｯ - 1
                if (useInventoryAmmo()) {
                    return consumeAmmoFromPlayerForShoot(1) == 1;
                }
                // 螯よ棡髱櫁レ蛹・峩隸ｻ蛻吝ｼｹ蛹｣蜀・ｭ仙ｼｹ - 1
                abstractGunItem.reduceCurrentAmmoCount(itemStack);
                return true;
            }
            // 豐｡譛芽・蜀・ｭ仙ｼｹ譌豕募ｰ・・
            if (!hasAmmoInBarrel) {
                return false;
            }
            // 豐｡譛牙ｼｹ蛹｣蜀・噪蟄仙ｼｹ蛻呎ｶ郁玲棯閹帛・逧・ｭ仙ｼｹ
            abstractGunItem.setBulletInBarrel(itemStack, false);
            return true;
        }
        // 蠑閹幃ｻ霎・
        if (boltType == Bolt.OPEN_BOLT) {
            // 豐｡譛牙ｭ仙ｼｹ譌豕募ｰ・・
            if (noAmmo) {
                return false;
            }
            // 螯よ棡閭悟桁逶ｴ隸ｻ蛻呵レ蛹・・蟆・・蜷主ｼｹ闕ｯ - 1
            if (useInventoryAmmo()) {
                return consumeAmmoFromPlayerForShoot(1) == 1;
            }
            // 螯よ棡髱櫁レ蛹・峩隸ｻ蛻吝ｼｹ蛹｣蜀・ｭ仙ｼｹ - 1
            abstractGunItem.reduceCurrentAmmoCount(itemStack);
            return true;
        }
        // 髱樔ｸ臥ｧ榊ｷｲ遏･ Bolt 邀ｻ蝙・(逶ｮ蜑堺ｸ堺ｼ壼・邇ｰ)・碁ｻ倩ｮ､霑泌屓 false
        return false;
    }

    /**
     * 闔ｷ蜿紋ｻ主ｼ蟋区困蠑ｹ蛻ｰ邇ｰ蝨ｨ扈丞紙逧・慮髣ｴ・悟黒菴堺ｸｺ ms
     *
     * @return 蠑蟋区困蠑ｹ蛻ｰ邇ｰ蝨ｨ扈丞紙逧・慮髣ｴ・悟黒菴堺ｸｺ ms
     */
    public int consumeAmmoFromPlayerForShoot(int neededAmount) {
        if (useInventoryAmmo() && !isShootingNeedConsumeAmmo()) {
            return neededAmount;
        }
        if (abstractGunItem.useDummyAmmo(itemStack)) {
            return abstractGunItem.findAndExtractDummyAmmo(itemStack, neededAmount);
        }
        return abstractGunItem.findAndExtractInventoryAmmo(shooter, itemStack, neededAmount);
    }

    public long getReloadTime() {
        if (dataHolder.reloadTimestamp == -1) {
            return 0;
        }
        return System.currentTimeMillis() - dataHolder.reloadTimestamp;
    }

    /**
     * 闔ｷ蜿紋ｻ主ｼ蟋区級譬灘芦邇ｰ蝨ｨ扈丞紙逧・慮髣ｴ・悟黒菴堺ｸｺ ms
     *
     * @return 蠑蟋区級譬灘芦邇ｰ蝨ｨ扈丞紙逧・慮髣ｴ・悟黒菴堺ｸｺ ms
     */
    public long getBoltTime() {
        if (!dataHolder.isBolting) {
            return 0;
        }
        return System.currentTimeMillis() - dataHolder.boltTimestamp;
    }

    /**
     * 闔ｷ蜿匁棯譴ｰ逧・ｰ・・髣ｴ髫費ｼ悟黒菴肴ｯｫ遘抵ｼ・
     *
     * @return 蟆・・髣ｴ髫・
     */
    public long getShootInterval() {
        FireMode fireMode = abstractGunItem.getFireMode(itemStack);
        if (fireMode == FireMode.BURST) {
            long coolDown = (long) (gunIndex.getGunData().getBurstData().getMinInterval() * 1000f);
            // 扈・5 ms 逧・ｪ怜哨譌ｶ髣ｴ・御ｻ･蟷ｳ陦｡蟒ｶ霑・
            coolDown = coolDown - 5;
            return Math.max(coolDown, 0L);
        }
        long coolDown = gunIndex.getGunData().getShootInterval(this.shooter, fireMode, itemStack);
        // 扈・5 ms 逧・ｪ怜哨譌ｶ髣ｴ・御ｻ･蟷ｳ陦｡蟒ｶ霑・
        coolDown = coolDown - 5;
        return Math.max(coolDown, 0L);
    }

    /**
     * 霑泌屓荳頑ｬ｡蟆・・逧・timestamp(邉ｻ扈滓慮髣ｴ)・悟黒菴堺ｸｺ豈ｫ遘偵よｭ､蛟ｼ蝨ｨ蛻・棯譌ｶ莨夐㍾鄂ｮ荳ｺ -1縲・
     *
     * @return 荳頑ｬ｡蟆・・逧・timestamp・悟惠蛻・棯譌ｶ莨夐㍾鄂ｮ荳ｺ -1縲・
     */
    public long getLastShootTimestamp() {
        return dataHolder.lastShootTimestamp + dataHolder.baseTimestamp;
    }

    /**
     * 隹・紛蟆・・髣ｴ髫斐・
     * 蟆・・髣ｴ髫疲ｯ碑ｾ・音谿奇ｼ悟ｮ・惠螳｢謌ｷ遶ｯ蜥梧恪蜉｡遶ｯ荳頑弍蛻・悪隶｡邂礼噪縲ょ屏豁､菴霑倬怙隕∝惠迥ｶ諤∵惻閼壽悽荳ｭ驥榊､崎ｿ幄｡御ｸ谺｡霑吩ｸｪ謫堺ｽ懊・
     *
     * @param alpha 髴隕∝刈荳頑・蜃丞ｰ醍噪蟆・・髣ｴ髫費ｼ悟黒菴堺ｸｺ豈ｫ遘偵よｭ｣謨ｰ蜊ｳ蠅槫刈蟆・・髣ｴ髫費ｼ瑚ｴ滓焚蛻呎弍蜃丞ｰ代・
     * @see GunAnimationStateContext#adjustClientShootInterval
     */
    public void adjustShootInterval(long alpha) {
        dataHolder.shootTimestamp += alpha;
    }

    /**
     * 隹・紛謐｢蠑ｹ譌ｶ髣ｴ
     *
     * @param alpha 髴隕∝刈荳頑・蜃丞ｰ醍噪謐｢蠑ｹ譌ｶ髣ｴ・悟黒菴堺ｸｺ豈ｫ遘偵よｭ｣謨ｰ蜊ｳ蠅槫刈謐｢蠑ｹ譌ｶ髣ｴ・亥刈蠢ｫ謐｢蠑ｹ霑帛ｺｦ・会ｼ瑚ｴ滓焚蛻呎弍蜃丞ｰ托ｼ亥㍼諷｢謐｢蠑ｹ霑帛ｺｦ・峨・
     */
    public void adjustReloadTime(long alpha) {
        dataHolder.reloadTimestamp -= alpha;
    }

    /**
     * 隹・紛諡画捺慮髣ｴ
     *
     * @param alpha 髴隕∝刈荳頑・蜃丞ｰ醍噪諡画捺慮髣ｴ・悟黒菴堺ｸｺ豈ｫ遘偵よｭ｣謨ｰ蜊ｳ蠅槫刈諡画捺慮髣ｴ・亥刈蠢ｫ諡画楢ｿ帛ｺｦ・会ｼ瑚ｴ滓焚蛻呎弍蜃丞ｰ托ｼ亥㍼諷｢諡画楢ｿ帛ｺｦ・峨・
     */
    public void adjustBoltTime(long alpha) {
        dataHolder.boltTimestamp -= alpha;
    }

    /**
     * 闔ｷ蜿也桷蜃・ｿ帛ｺｦ縲・
     *
     * @return 闌・峩 0~1縲・ 莉｣陦ｨ譛ｪ迸・㊥・・ 莉｣陦ｨ迸・㊥螳梧・縲・
     */
    public float getAimingProgress() {
        return dataHolder.aimingProgress;
    }

    /**
     * 闔ｷ蜿也自螳ｶ蠖灘燕逧・困蠑ｹ迥ｶ諤√・
     *
     * @return 邇ｩ螳ｶ蠖灘燕逧・困蠑ｹ迥ｶ諤・(蠎乗焚)
     */
    public int getReloadStateType() {
        return dataHolder.reloadStateType.ordinal();
    }

    /**
     * 闔ｷ蜿匁棯譴ｰ蠖灘燕逧・ｼ轣ｫ讓｡蠑擾ｼ亥・閾ｪ蜉ｨ縲∝濠閾ｪ蜉ｨ縲∬ｿ槫書遲会ｼ峨・
     *
     * @return 蠑轣ｫ讓｡蠑・(蠎乗焚)
     */
    public int getFireMode() {
        return abstractGunItem.getFireMode(itemStack).ordinal();
    }

    /**
     * 闔ｷ蜿門ｽ灘燕邇ｩ螳ｶ蟆・・譏ｯ蜷ｦ髴隕∵ｶ郁怜ｼｹ闕ｯ縲らｻ剰ｿ・ｮｾ鄂ｮ・悟・騾讓｡蠑冗噪邇ｩ螳ｶ蜿ｯ莉･荳肴ｶ郁怜ｼｹ闕ｯ蟆・・縲・
     *
     * @return 蟆・・譏ｯ蜷ｦ髴隕∵ｶ郁怜ｼｹ闕ｯ
     */
    public boolean isShootingNeedConsumeAmmo() {
        return IGunOperator.fromLivingEntity(shooter).consumesAmmoOrNot();
    }

    /**
     * 闔ｷ蜿門ｽ灘燕邇ｩ螳ｶ謐｢蠑ｹ譏ｯ蜷ｦ髴隕∵ｶ郁怜ｼｹ闕ｯ縲ゆｸ闊ｬ譚･隸ｴ蛻幃讓｡蠑丈ｸ倶ｸ埼怙隕∵ｶ郁怜ｼｹ闕ｯ縲・
     *
     * @return 謐｢蠑ｹ譏ｯ蜷ｦ髴隕∵ｶ郁怜ｼｹ闕ｯ
     */
    public boolean isReloadingNeedConsumeAmmo() {
        return IGunOperator.fromLivingEntity(shooter).needCheckAmmo();
    }

    /**
     * 闔ｷ蜿門ｽ灘燕譫ｪ譴ｰ髴隕∫噪蠑ｹ闕ｯ謨ｰ驥上・
     *
     * @return 蠖灘燕譫ｪ譴ｰ髴隕∫噪蠑ｹ闕ｯ謨ｰ驥・
     */
    public int getNeededAmmoAmount() {
        int maxAmmoCount = AttachmentDataUtils.getAmmoCountWithAttachment(itemStack, gunIndex.getGunData());
        int currentAmmoCount = abstractGunItem.getCurrentAmmoCount(itemStack);
        return maxAmmoCount - currentAmmoCount;
    }

    /**
     * 闔ｷ蜿門ｼｹ蛹｣荳ｭ逧・､・ｼｹ謨ｰ縲・
     *
     * @return 霑泌屓蠑ｹ蛹｣荳ｭ逧・､・ｼｹ謨ｰ・御ｸ崎ｮ｡邂怜ｷｲ蝨ｨ譫ｪ邂｡荳ｭ逧・ｼｹ闕ｯ縲・
     */
    public int getAmmoAmount() {
        return abstractGunItem.getCurrentAmmoCount(itemStack);
    }

    /**
     * 闔ｷ蜿匁棯譴ｰ蠑ｹ蛹｣逧・怙螟ｧ螟・ｼｹ謨ｰ縲・
     *
     * @return 霑泌屓譫ｪ譴ｰ蠑ｹ蛹｣逧・怙螟ｧ螟・ｼｹ謨ｰ・御ｸ崎ｮ｡邂怜ｷｲ蝨ｨ譫ｪ邂｡荳ｭ逧・ｼｹ闕ｯ縲・
     */
    public int getMaxAmmoCount() {
        return AttachmentDataUtils.getAmmoCountWithAttachment(itemStack, gunIndex.getGunData());
    }

    /**
     * 闔ｷ蜿匁棯譴ｰ謇ｩ螳ｹ遲臥ｺｧ縲・
     *
     * @return 謇ｩ螳ｹ遲臥ｺｧ・瑚激蝗ｴ 0 ~ 3縲・ 陦ｨ遉ｺ豐｡譛牙ｮ芽｣・黄螳ｹ蠑ｹ蛹｣・・ ~ 3 陦ｨ遉ｺ螳芽｣・ｺ・黄螳ｹ遲臥ｺｧ 1 ~ 3 逧・黄螳ｹ蠑ｹ蛹｣
     */
    public int getMagExtentLevel() {
        return AttachmentDataUtils.getMagExtendLevel(itemStack, gunIndex.getGunData());
    }

    /**
     * 蟆ｽ蜿ｯ閭ｽ螟壼慍莉守自螳ｶ霄ｫ荳・(謌冶・劒諡溷､・ｼｹ) 豸郁玲脂蠑ｹ闕ｯ・瑚ｿ泌屓豸郁礼噪謨ｰ驥・
     *
     * @param neededAmount 髴隕∫噪蠑ｹ闕ｯ謨ｰ驥・
     * @return 螳樣刔豸郁礼噪蠑ｹ闕ｯ謨ｰ驥・
     */
    public int consumeAmmoFromPlayer(int neededAmount) {
        // 螯よ棡螟・ｺ手レ蛹・峩隸ｻ蟷ｶ荳泌・騾讓｡蠑丈ｸ肴ｶ郁礼噪諠・・
        if (useInventoryAmmo() && !isReloadingNeedConsumeAmmo()) {
            return neededAmount;
        }
        int extracted;
        if (abstractGunItem.useDummyAmmo(itemStack)) {
            extracted = abstractGunItem.findAndExtractDummyAmmo(itemStack, neededAmount);
        } else {
            extracted = abstractGunItem.findAndExtractInventoryAmmo(shooter, itemStack, neededAmount);
        }
        if (neededAmount > 0) {
            GunMod.LOGGER.debug("consumeAmmoFromPlayer: gunId={}, needed={}, extracted={}, useDummyAmmo={}, dummyAmmo={}, hasConsumableAmmo={}",
                    gunId, neededAmount, extracted, abstractGunItem.useDummyAmmo(itemStack), abstractGunItem.getDummyAmmoAmount(itemStack),
                    abstractGunItem.hasConsumableAmmo(shooter, itemStack));
        }
        return extracted;
    }

    /**
     * 譽譟･邇ｩ螳ｶ霄ｫ荳奇ｼ域・閠・劒諡溷､・ｼｹ・画弍蜷ｦ譛牙ｼｹ闕ｯ蜿ｯ莉･豸郁暦ｼ碁壼ｸｸ逕ｨ莠主ｾｪ邇ｯ謐｢蠑ｹ逧・遠譁ｭ縲・
     * 蛻幃讓｡蠑冗噪邇ｩ螳ｶ莨夂峩謗･霑泌屓 true
     * @return 邇ｩ螳ｶ霄ｫ荳奇ｼ域・閠・劒諡溷､・ｼｹ・画弍蜷ｦ譛牙ｼｹ闕ｯ蜿ｯ莉･豸郁・
     */
    public boolean hasAmmoToConsume(){
        if (!isReloadingNeedConsumeAmmo()) {
            return true;
        }
        if (abstractGunItem.useDummyAmmo(itemStack)) {
            return abstractGunItem.getDummyAmmoAmount(itemStack) > 0;
        }
        return abstractGunItem.hasConsumableAmmo(shooter, itemStack);
    }

    /**
     * 蟆・ｭ仙ｼｹ謗ｨ蜈･蠑ｹ蛹｣縲・
     *
     * @param amount 髴隕∵耳蜈･逧・ｭ仙ｼｹ謨ｰ驥・
     * @return 螟壻ｽ咏噪蟄仙ｼｹ
     */
    public int putAmmoInMagazine(int amount) {
        if (amount < 0) {
            return 0;
        }
        int maxAmmoCount = AttachmentDataUtils.getAmmoCountWithAttachment(itemStack, gunIndex.getGunData());
        int currentAmmoCount = abstractGunItem.getCurrentAmmoCount(itemStack);
        int newAmmoCount = currentAmmoCount + amount;
        if (maxAmmoCount < newAmmoCount) {
            abstractGunItem.setCurrentAmmoCount(itemStack, maxAmmoCount);
            markShooterInventoryDirty();
            return newAmmoCount - maxAmmoCount;
        } else {
            abstractGunItem.setCurrentAmmoCount(itemStack, newAmmoCount);
            markShooterInventoryDirty();
            return 0;
        }
    }

    /**
     * 蟆・ｭ仙ｼｹ莉主ｼｹ蛹｣遘ｻ髯､縲・
     *
     * @param amount 髴隕∫ｧｻ髯､逧・焚驥・
     * @return 謌仙粥遘ｻ髯､逧・焚驥・
     */
    public int removeAmmoFromMagazine(int amount) {
        if (amount < 0) {
            return 0;
        }
        int currentAmmoCount = abstractGunItem.getCurrentAmmoCount(itemStack);
        if (currentAmmoCount < amount) {
            abstractGunItem.setCurrentAmmoCount(itemStack, 0);
            markShooterInventoryDirty();
            return currentAmmoCount;
        } else {
            abstractGunItem.setCurrentAmmoCount(itemStack, currentAmmoCount - amount);
            markShooterInventoryDirty();
            return amount;
        }
    }

    private void markShooterInventoryDirty() {
        if (!shooter.level().isClientSide() && shooter instanceof Player player) {
            player.getInventory().setChanged();
            player.inventoryMenu.broadcastChanges();
        }
    }

    /**
     * 闔ｷ蜿門ｼｹ蛹｣蜀・ｭ仙ｼｹ謨ｰ驥上・
     *
     * @return 蠑ｹ蛹｣蜀・ｭ仙ｼｹ謨ｰ驥・
     */
    public int getAmmoCountInMagazine() {
        return abstractGunItem.getCurrentAmmoCount(itemStack);
    }

    /**
     * 闔ｷ蜿匁棯閹帛・譏ｯ蜷ｦ譛牙ｭ仙ｼｹ縲・
     *
     * @return 譫ｪ閹帛・譏ｯ蜷ｦ譛牙ｭ仙ｼｹ.螯よ棡譏ｯ蠑閹帛ｾ・・逧・棯譴ｰ・悟・豁､譁ｹ豕戊ｿ泌屓 false縲・
     */
    public boolean hasAmmoInBarrel() {
        Bolt boltType = gunIndex.getGunData().getBolt();
        return boltType != Bolt.OPEN_BOLT && abstractGunItem.hasBulletInBarrel(itemStack);
    }

    /**
     * 隶ｾ鄂ｮ譫ｪ閹帛・譏ｯ蜷ｦ譛牙ｭ仙ｼｹ
     */
    public void setAmmoInBarrel(boolean ammoInBarrel) {
        abstractGunItem.setBulletInBarrel(itemStack, ammoInBarrel);
    }

    /**
     * 蟆・ｻｻ諢・lua 蟇ｹ雎｡謨ｰ謐ｮ郛灘ｭ伜芦邇ｩ螳ｶ謨ｰ謐ｮ荳ｭ縲ら畑莠手・譛ｬ荳ｭ蠑よｭ･莨騾呈焚謐ｮ・梧・閠・ｷｨ譁ｹ豕穂ｼ騾呈焚謐ｮ縲・
     *
     * @param luaValue 郛灘ｭ倡噪 lua 蟇ｹ雎｡
     */
    public void cacheScriptData(LuaValue luaValue) {
        this.dataHolder.scriptData = luaValue;
    }

    /**
     * 蟆・自螳ｶ謨ｰ謐ｮ荳ｭ郛灘ｭ倡噪 lua 蟇ｹ雎｡蜿門・縲・
     *
     * @return 郛灘ｭ倡噪 lua 蟇ｹ雎｡
     */
    public LuaValue getCachedScriptData() {
        return dataHolder.scriptData;
    }

    /**
     * 闔ｷ蜿門惠譫ｪ譴ｰ data 荳ｭ螢ｰ譏守噪閼壽悽蜿よ焚
     *
     * @return 閼壽悽蜿よ焚陦ｨ
     */
    public LuaTable getScriptParams() {
        LuaTable param = gunIndex.getScriptParam();
        return param == null ? new LuaTable() : param;
    }

    /**
     * 蟋疲汚蟒ｶ霑溽噪蠕ｪ邇ｯ莉ｻ蜉｡・悟惠荳ｻ郤ｿ遞区鴬陦鯉ｼ梧弍郤ｿ遞句ｮ牙・逧・ｼ御ｽ・弍譌ｶ髣ｴ荳肴弍荳･譬ｼ逧・ｼ檎ｲ貞ｺｦ蜿門・莠・TPS縲・
     *
     * @param value    蠎泌ｽ捺弍荳荳ｪ霑泌屓 boolean 逧・LuaFunction縲ょｦよ棡霑泌屓 false ・悟・蟆・蜃ｺ蠕ｪ邇ｯ縲・
     * @param delayMs  蟒ｶ霑滓鴬陦檎噪譌ｶ髣ｴ縲・
     * @param periodMs 蠕ｪ邇ｯ謇ｧ陦檎噪髣ｴ髫斐・
     * @param cycles   譛螟ｧ蠕ｪ邇ｯ谺｡謨ｰ縲・1 莉｣陦ｨ譌髯先ｬ｡縲・
     */
    public void safeAsyncTask(LuaValue value, long delayMs, long periodMs, int cycles) {
        LuaFunction func = value.checkfunction();
        CycleTaskHelper.addCycleTask(() -> func.call().checkboolean(), delayMs, periodMs, cycles);
    }

    /**
     * 闔ｷ蜿門ｽ灘燕邉ｻ扈滓慮髣ｴ・悟黒菴肴ｯｫ遘偵・
     *
     * @return 蠖灘燕邉ｻ扈滓慮髣ｴ
     */
    public long getCurrentTimestamp() {
        return System.currentTimeMillis();
    }

    /**
     * 闔ｷ蜿匁棯譴ｰ逧・・莉ｶ ID
     *
     * @return 驟堺ｻｶ ID, 螯よ棡邀ｻ蝙矩漠隸ｯ謌冶・ｯｹ蠎皮噪驟堺ｻｶ荳榊ｭ伜惠蛻呵ｿ泌屓遨ｺ驟堺ｻｶ ID 'tacz:empty'
     */
    public String getAttachment(String type) {
        try {
            AttachmentType t = AttachmentType.valueOf(type);
            return abstractGunItem.getAttachmentId(itemStack, t).toString();
        } catch (IllegalArgumentException e) {
            return DefaultAssets.EMPTY_ATTACHMENT_ID.toString();
        }
    }

    /**
     * 霑泌屓荳荳ｪ蠖灘燕譫ｪ譴ｰ迚ｩ蜩∫噪 NBT 隶ｿ髣ｮ蝎ｨ縲る勁髱櫁ｦ∽ｿ晏ｭ俶戟荵・喧謨ｰ謐ｮ・御ｽ荳榊ｺ碑ｯ･鬚醍ｹ∬ｰ・畑霑吩ｸｪ譁ｹ豕輔・br/>
     * 蜿りｧ・{@link LuaNbtAccessor}
     * @return NBT 隶ｿ髣ｮ蝎ｨ
     */
    public LuaNbtAccessor getNbt() {
        return nbtUtil;
    }

    /**
     * 霑泌屓荳荳ｪ蜈ｳ莠主ｽ灘燕蠑譫ｪ螳樔ｽ鍋噪蟾･蜈ｷ縲りｿ吩ｸｪ隶ｿ髣ｮ蝎ｨ謠蝉ｾ帑ｺ・ｸ莠帛ｸｸ逕ｨ逧・婿豕包ｼ御ｾ句ｦょ書騾∫ｳｻ扈滓ｶ域・縲∝書騾、ctionBar縲∝・蟒ｺ譁・悽扈・ｻｶ遲峨・br/>
     * 蜿りｧ・{@link LuaEntityAccessor}
     * @return 螳樔ｽ楢ｮｿ髣ｮ蝎ｨ
     */
    public LuaEntityAccessor getEntityUtil() {
        if (entityAccessor == null) {
            entityAccessor = new LuaEntityAccessor(shooter);
        }
        return entityAccessor;
    }

    public void setShooter(LivingEntity shooter) {
        this.shooter = shooter;
    }

    public void setItemStack(ItemStack itemStack) {
        this.itemStack = itemStack;
        initGunItem();
    }

    public void setPitchSupplier(Supplier<Float> pitchSupplier) {
        this.pitchSupplier = pitchSupplier;
    }

    public void setYawSupplier(Supplier<Float> yawSupplier) {
        this.yawSupplier = yawSupplier;
    }

    public LivingEntity getShooter() {
        return shooter;
    }

    public ItemStack getItemStack() {
        return itemStack;
    }

    public AbstractGunItem getAbstractGunItem() {
        return abstractGunItem;
    }

    public CommonGunIndex getGunIndex() {
        return gunIndex;
    }

    public void setHeatAmount(float amount) {
        abstractGunItem.setHeatAmount(itemStack, amount);
    }

    public float getHeatAmount() {
        return abstractGunItem.getHeatAmount(itemStack);
    }

    public boolean hasHeatData() {
        return gunIndex.getGunData().getHeatData() != null;
    }

    public float getHeatMinRpm() {
        if(hasHeatData()) return gunIndex.getGunData().getHeatData().getMinRpmMod();
        return 0f;
    }

    public float getHeatMaxRpm() {
        if(hasHeatData()) return gunIndex.getGunData().getHeatData().getMaxRpmMod();
        return 0f;
    }

    public float getHeatMinInaccuracy() {
        if(hasHeatData()) return gunIndex.getGunData().getHeatData().getMinInaccuracy();
        return 0f;
    }

    public float getHeatMaxInaccuracy() {
        if(hasHeatData()) return gunIndex.getGunData().getHeatData().getMaxInaccuracy();
        return 0f;
    }

    public float getHeatMax() {
        if(hasHeatData()) return gunIndex.getGunData().getHeatData().getHeatMax();
        return 0f;
    }

    public float getHeatPerShot() {
        if(hasHeatData()) return gunIndex.getGunData().getHeatData().getHeatPerShot();
        return 0f;
    }

    public boolean isOverheatLocked() {
        return abstractGunItem.isOverheatLocked(itemStack);
    }

    public void setOverheatLocked(boolean locked) {
        abstractGunItem.setOverheatLocked(itemStack, locked);
    }

    public long getOverheatTime() {
        if(hasHeatData()) return gunIndex.getGunData().getHeatData().getOverHeatTime();
        return 0;
    }

    public long getCoolingDelay() {
        if(hasHeatData()) return gunIndex.getGunData().getHeatData().getCoolingDelay();
        return 0;
    }

    public float calcHeatReduction(long heatTimestamp) {
        GunHeatData heatData = gunIndex.getGunData().getHeatData();
        if (heatData != null) {
            return ((float)(System.currentTimeMillis() - heatTimestamp) / 10000f)
                    * heatData.getCoolingMultiplier();
        }
        return 0f;
    }

    // TODO: 豬玖ｯ墓｣譟･ enum 蛟ｼ譏ｯ蜷ｦ蜿ｯ莉･逶ｴ謗･蝨ｨ lua 荳ｭ隹・畑・御ｻ･邂蛹冶ｿ吩ｸｪ蜉溯・荳ｺ荳矩擇驍｣荳ｪ譁ｹ豕・
    public int getBoltByInt() {
        Bolt bolt = gunIndex.getGunData().getBolt();
        if (bolt == Bolt.MANUAL_ACTION) {
            return 1;
        }
        if (bolt == Bolt.CLOSED_BOLT) {
            return 2;
        }
        if (bolt == Bolt.OPEN_BOLT) {
            return 3;
        }
        return 0;
    }

    public Bolt getBolt() {
        return gunIndex.getGunData().getBolt();
    }

    public void setDataHolder(ShooterDataHolder dataHolder) {
        this.dataHolder = dataHolder;
    }

    public boolean useInventoryAmmo() {
        return abstractGunItem.useInventoryAmmo(itemStack);
    }

    ShooterDataHolder getDataHolder() {
        return this.dataHolder;
    }

    private void initGunItem() {
        if (itemStack == null || !(itemStack.getItem() instanceof AbstractGunItem gunItem)) {
            gunIndex = null;
            abstractGunItem = null;
            return;
        }
        gunId = gunItem.getGunId(itemStack);
        gunDisplayId = gunItem.getGunDisplayId(itemStack);
        Optional<CommonGunIndex> gunIndexOptional = TimelessAPI.getCommonGunIndex(gunId);
        gunIndex = gunIndexOptional.orElse(null);
        abstractGunItem = gunItem;
        var tag = NbtCompat.getTag(itemStack);
        if (tag != null) {
            nbtUtil = new LuaNbtAccessor(tag);
        }
    }


    private LuaFunction checkFunction(LuaValue luaValue) {
        if (luaValue.isfunction()) {
            return (LuaFunction) luaValue;
        } else if (luaValue.isnil()) {
            return null;
        } else {
            throw new LuaError("bad argument: function or nil expected, got " + luaValue.typename());
        }
    }
}
