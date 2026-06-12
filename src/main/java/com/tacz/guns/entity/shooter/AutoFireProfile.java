package com.tacz.guns.entity.shooter;

import com.tacz.guns.api.item.IGun;
import com.tacz.guns.api.item.gun.FireMode;
import com.tacz.guns.resource.index.CommonGunIndex;
import com.tacz.guns.resource.modifier.AttachmentCacheProperty;
import com.tacz.guns.resource.modifier.custom.RpmModifier;
import com.tacz.guns.resource.pojo.data.gun.GunData;
import com.tacz.guns.resource.pojo.data.gun.GunHeatData;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemStack;

import java.util.Random;

public final class AutoFireProfile {
    private static final int HIGH_RPM_THRESHOLD = 1200;
    private static final int SAMPLE_COUNT = 128;
    private static final int SAMPLE_MASK = SAMPLE_COUNT - 1;

    private final boolean highRpm;
    private final float[] spreadUnitXY;

    private AutoFireProfile(boolean highRpm, float[] spreadUnitXY) {
        this.highRpm = highRpm;
        this.spreadUnitXY = spreadUnitXY;
    }

    public static AutoFireProfile create(ItemStack gunItem, IGun gun, CommonGunIndex index, AttachmentCacheProperty cacheProperty) {
        GunData gunData = index.getGunData();
        FireMode fireMode = gun.getFireMode(gunItem);
        int maxPossibleRpm = gunData.getRoundsPerMinute(fireMode);
        if (cacheProperty != null) {
            Integer cachedRpm = cacheProperty.getCache(RpmModifier.ID);
            if (cachedRpm != null) {
                maxPossibleRpm = Math.max(maxPossibleRpm, cachedRpm);
            }
        }
        GunHeatData heatData = gunData.getHeatData();
        if (heatData != null) {
            maxPossibleRpm = (int) Math.ceil(maxPossibleRpm * heatData.getMaxRpmMod());
        }
        boolean highRpm = maxPossibleRpm >= HIGH_RPM_THRESHOLD;
        long seed = 31L * gun.getGunId(gunItem).hashCode() + System.nanoTime();
        return new AutoFireProfile(highRpm, createSpreadSamples(seed));
    }

    public boolean highRpm() {
        return highRpm;
    }

    public double spreadX(int shotIndex, float inaccuracy) {
        return spreadUnitXY[(shotIndex & SAMPLE_MASK) * 2] * inaccuracy;
    }

    public double spreadY(int shotIndex, float inaccuracy) {
        return spreadUnitXY[(shotIndex & SAMPLE_MASK) * 2 + 1] * inaccuracy;
    }

    private static float[] createSpreadSamples(long seed) {
        float[] samples = new float[SAMPLE_COUNT * 2];
        Random random = new Random(seed);
        for (int i = 0; i < SAMPLE_COUNT; i++) {
            double angle = random.nextDouble() * Math.PI * 2.0;
            double radius = Math.sqrt(random.nextDouble());
            samples[i * 2] = (float) (Mth.cos((float) angle) * radius);
            samples[i * 2 + 1] = (float) (Mth.sin((float) angle) * radius);
        }
        return samples;
    }
}
