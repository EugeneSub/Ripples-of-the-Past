package com.github.standobyte.jojo.potion;

import java.util.Random;
import java.util.Map.Entry;

import com.github.standobyte.jojo.client.particle.custom.CustomParticlesHelper;
import com.github.standobyte.jojo.client.sound.HamonSparksLoopSound;
import com.github.standobyte.jojo.entity.stand.StandEntity;
import com.github.standobyte.jojo.modcompat.OptionalDependencyHelper;
import com.github.standobyte.jojo.util.mc.MCUtil;
import com.github.standobyte.jojo.util.mod.JojoModUtil;

import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.MobEntity;
import net.minecraft.entity.ai.attributes.Attribute;
import net.minecraft.entity.ai.attributes.AttributeModifier;
import net.minecraft.entity.ai.attributes.AttributeModifierManager;
import net.minecraft.entity.ai.attributes.Attributes;
import net.minecraft.entity.ai.attributes.ModifiableAttributeInstance;
import net.minecraft.potion.EffectType;

public class HamonConfusionEffect extends StatusEffect {

    public HamonConfusionEffect(EffectType type, int liquidColor) {
    	super(type, liquidColor);
    	addAttributeModifier(Attributes.MOVEMENT_SPEED, "e30ee41c-6ea2-468c-99ab-fd0a7d6be8c3", -0.10, AttributeModifier.Operation.MULTIPLY_TOTAL);
    }

    @Override
    public void applyEffectTick(LivingEntity entity, int amplifier) {
    	int tick = 0;
        super.applyEffectTick(entity, amplifier);
        if (entity instanceof MobEntity && tick % 10 == 0) {
        	for (LivingEntity target : MCUtil.entitiesAround(
                    LivingEntity.class, entity, 9, false, entity2 -> !(entity instanceof StandEntity && entity2.is(((StandEntity) entity).getUser())))) {
        		((MobEntity) entity).setTarget(null);
        		((MobEntity) entity).setTarget(target);
        		break;
        	}
        	tick++;
        }
        if (MCUtil.isControlledThisSide(entity)) {
            Random random = entity.getRandom();
            entity.yRot     += (random.nextFloat() - 0.5F) * (amplifier + 1) * 6F;
            entity.yHeadRot += (random.nextFloat() - 0.5F) * (amplifier + 1) * 6F;
            entity.xRot     += (random.nextFloat() - 0.5F) * (amplifier + 1) * 6F;
        }
        
        if (entity.level.isClientSide()) {
            HamonSparksLoopSound.playSparkSound(entity, entity.getBoundingBox().getCenter(), 1.0F, true);
            CustomParticlesHelper.createHamonSparkParticles(entity, entity.getRandomX(0.5), entity.getY(Math.random()), entity.getRandomZ(0.5), 1);
        }
    }
    
    @Override
    public boolean isDurationEffectTick(int duration, int amplifier) {
        return true;
    }
    
    @Override
    public void removeAttributeModifiers(LivingEntity pLivingEntity, AttributeModifierManager pAttributeMap, int pAmplifier) {
    	super.removeAttributeModifiers(pLivingEntity, pAttributeMap, pAmplifier);
    	if (pLivingEntity instanceof MobEntity) {
        	((MobEntity) pLivingEntity).setTarget(null);
    	}
     }
}
