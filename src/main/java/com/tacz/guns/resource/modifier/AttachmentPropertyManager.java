package com.tacz.guns.resource.modifier;

import com.google.common.collect.Maps;
import com.tacz.guns.GunMod;
import com.tacz.guns.api.GunProperties;
import com.tacz.guns.api.TimelessAPI;
import com.tacz.guns.api.entity.IGunOperator;
import com.tacz.guns.api.event.common.AttachmentPropertyEvent;
import com.tacz.guns.api.item.IGun;
import com.tacz.guns.api.modifier.IAttachmentModifier;
import com.tacz.guns.entity.shooter.ShooterDataHolder;
import com.tacz.guns.event.ChangeGunPropertyEvent;
import com.tacz.guns.resource.modifier.custom.*;
import com.tacz.guns.resource.pojo.data.attachment.Modifier;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.common.MinecraftForge;
import org.apache.commons.lang3.StringUtils;
import org.luaj.vm2.script.LuaScriptEngineFactory;

import javax.script.ScriptEngine;
import javax.script.ScriptException;
import java.util.*;

public class AttachmentPropertyManager {
    private static final ScriptEngine LUAJ_ENGINE = new LuaScriptEngineFactory().getScriptEngine();
    private static final Map<String, IAttachmentModifier<?, ?>> MODIFIERS = Maps.newLinkedHashMap();

    public static void registerModifier() {
        MODIFIERS.put(AdsModifier.ID, new AdsModifier());
        MODIFIERS.put(AmmoSpeedModifier.ID, new AmmoSpeedModifier());
        MODIFIERS.put(ArmorIgnoreModifier.ID, new ArmorIgnoreModifier());
        MODIFIERS.put(DamageModifier.ID, new DamageModifier());
        MODIFIERS.put(EffectiveRangeModifier.ID, new EffectiveRangeModifier());
        MODIFIERS.put(ExplosionModifier.ID, new ExplosionModifier());
        MODIFIERS.put(HeadShotModifier.ID, new HeadShotModifier());
        MODIFIERS.put(IgniteModifier.ID, new IgniteModifier());
        MODIFIERS.put(InaccuracyModifier.ID, new InaccuracyModifier());
        MODIFIERS.put(KnockbackModifier.ID, new KnockbackModifier());
        MODIFIERS.put(PierceModifier.ID, new PierceModifier());
        MODIFIERS.put(RecoilModifier.ID, new RecoilModifier());
        MODIFIERS.put(RpmModifier.ID, new RpmModifier());
        MODIFIERS.put(SilenceModifier.ID, new SilenceModifier());
        MODIFIERS.put(WeightModifier.ID, new WeightModifier());
        MODIFIERS.put(ExtraMovementModifier.ID, new ExtraMovementModifier());
    }

    public static Map<String, IAttachmentModifier<?, ?>> getModifiers() {
        return MODIFIERS;
    }

    public static void postChangeEvent(LivingEntity shooter, ItemStack gunItem) {
        if (!(gunItem.getItem() instanceof IGun iGun)) {
            return;
        }
        Identifier gunId = iGun.getGunId(gunItem);
        TimelessAPI.getCommonGunIndex(gunId).ifPresent(index -> {
            AttachmentCacheProperty cacheProperty = new AttachmentCacheProperty();
            // 陷ｿ螟ｧ・ｸ繝ｻ・ｺ蛟ｶ・ｻ・ｶ
            AttachmentPropertyEvent event = new AttachmentPropertyEvent(gunItem, cacheProperty);
            ChangeGunPropertyEvent.internalOnAttachmentPropertyEvent(event);
            event.postEventToKubeJS(event);
        com.tacz.guns.util.ForgeEventCompat.post(event);
            // 髫ｶ・ｩ髢ｼ螢ｽ謔ｽ隴厄ｽｴ隴・ｽｰ驛帷§・ｭ繝ｻ
            IGunOperator operator = IGunOperator.fromLivingEntity(shooter);
            ShooterDataHolder dataHolder = operator.getDataHolder();
            GunProperties.allCacheModifiableByScript().forEach((id, property) -> {
                // noinspection rawtypes,unchecked
                iGun.modifyProperty(dataHolder, gunItem, shooter, "modify_cached_property", property.name(), (Class) property.type(), cacheProperty.getCache(property));
            });
            // 隴厄ｽｴ隴・ｽｰ陞ｳ讓費ｽｽ骰句飭驛帷§・ｭ莨懶ｽｯ・ｹ髮趣ｽ｡
            operator.updateCacheProperty(cacheProperty);
        });
    }

    public static double eval(Modifier modifier, double defaultValue) {
        return eval(Collections.singletonList(modifier), defaultValue);
    }

    public static double eval(List<Modifier> modifiers, double defaultValue) {
        double addend = defaultValue;
        double percent = 1;
        double multiplier = 1;
        for (Modifier modifier : modifiers) {
            addend += modifier.getAddend();
            percent += modifier.getPercent();
            multiplier *= Math.max(modifier.getMultiplier(), 0f);
        }
        percent = Math.max(percent, 0f);
        double value = addend * percent * multiplier;
        for (Modifier modifier : modifiers) {
            String function = modifier.getFunction();
            if (StringUtils.isEmpty(function)) {
                continue;
            }
            value = functionEval(value, defaultValue, function);
        }
        return value;
    }

    public static boolean eval(List<Boolean> modified, boolean defaultValue) {
        if (defaultValue) {
            // 陞ｯ繧域｣｡魄溷ｩ・ｮ・､陋滂ｽｼ闕ｳ・ｺ true繝ｻ遒√≦闕ｵ莠･螳ｵ髫補扱諤剰叉闕ｳ・ｪ false 陝・ｽｱ髴第ｳ悟ｱ・false
            return modified.stream().allMatch(s -> s);
        } else {
            // 陞ｯ繧域｣｡魄溷ｩ・ｮ・､陋滂ｽｼ闕ｳ・ｺ false繝ｻ遒√≦闕ｵ莠･螳ｵ髫補扱諤剰叉闕ｳ・ｪ true 陝・ｽｱ髴第ｳ悟ｱ・true
            return modified.stream().anyMatch(s -> s);
        }
    }

    public static double functionEval(double value, double defaultValue, String script) {
        script = script.toLowerCase(Locale.ENGLISH);
        LUAJ_ENGINE.put("x", value);
        LUAJ_ENGINE.put("r", defaultValue);
        try {
            LUAJ_ENGINE.eval(script);
        } catch (ScriptException e) {
            GunMod.LOGGER.catching(e);
        }
        if (LUAJ_ENGINE.get("y") instanceof Number number) {
            return number.doubleValue();
        }
        return value;
    }
}

