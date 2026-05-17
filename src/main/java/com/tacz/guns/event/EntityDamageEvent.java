package com.tacz.guns.event;

import com.tacz.guns.init.ModAttributes;
import com.tacz.guns.init.ModDamageTypes;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.eventbus.api.listener.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber
public class EntityDamageEvent {

    @SubscribeEvent(priority = -64)
    public static void onLivingHurt(LivingHurtEvent event){
        if (event.getSource().is(ModDamageTypes.BULLETS_TAG)) {
            LivingEntity living = event.getEntity();
            var attributeHolder = ModAttributes.BULLET_RESISTANCE.getHolder().orElse(null);
            if (attributeHolder == null) {
                return;
            }
            AttributeInstance resistance = living.getAttribute(attributeHolder);
            if (resistance != null) {
                float modifiedDamage = event.getAmount() * (float) (1 - resistance.getValue());
                event.setAmount(modifiedDamage);
            }
        }
    }
}

