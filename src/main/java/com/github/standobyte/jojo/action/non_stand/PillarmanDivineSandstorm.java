package com.github.standobyte.jojo.action.non_stand;

import com.github.standobyte.jojo.action.ActionConditionResult;
import com.github.standobyte.jojo.action.ActionTarget;
import com.github.standobyte.jojo.entity.damaging.projectile.HamonTurquoiseBlueOverdriveEntity;
import com.github.standobyte.jojo.entity.damaging.projectile.PillarmanDivineSandstormEntity;
import com.github.standobyte.jojo.init.ModParticles;
import com.github.standobyte.jojo.init.power.non_stand.ModPowers;
import com.github.standobyte.jojo.power.impl.nonstand.INonStandPower;
import com.github.standobyte.jojo.power.impl.nonstand.type.hamon.HamonData;
import com.github.standobyte.jojo.power.impl.nonstand.type.pillarman.PillarmanData.Mode;

import net.minecraft.entity.LivingEntity;
import net.minecraft.util.math.vector.Vector3d;
import net.minecraft.world.World;

public class PillarmanDivineSandstorm extends PillarmanAction {

    public PillarmanDivineSandstorm(PillarmanAction.Builder builder) {
        super(builder.holdType());
        mode = Mode.WIND;
    }
    
    @Override
    public ActionConditionResult checkSpecificConditions(LivingEntity user, INonStandPower power, ActionTarget target) {
        if (power.getHeldAction() == this) {
            if (power.getEnergy() == 0F) {
                return conditionMessage("no_energy_pillarman");
            }
        }
        return ActionConditionResult.POSITIVE;
    }
    
    @Override
    public float getHeldTickEnergyCost(INonStandPower power) {
    	int maxTicks = Math.max(getHoldDurationToFire(power), 1);
        int ticksHeld = Math.min(power.getHeldActionTicks(), maxTicks);
    	if(ticksHeld >= maxTicks) {
    		return 5.0F;
    	}
        	return 0;
    }
    
    @Override
    public void onHoldTickClientEffect(LivingEntity user, INonStandPower power, int ticksHeld, boolean requirementsFulfilled, boolean stateRefreshed) {
        if (requirementsFulfilled) {
        	for (int i = 0; i < 12; i++) {
            Vector3d particlePos = user.position().add(
                    (Math.random() - 0.5) * (user.getBbWidth() + 0.5), 
                    Math.random() * (user.getBbHeight()), 
                    (Math.random() - 0.5) * (user.getBbWidth() + 0.5));
            user.level.addParticle(ModParticles.HAMON_AURA_GREEN.get(), particlePos.x, particlePos.y, particlePos.z, 0, 0, 0);
        	}
        }
    }
    
    @Override
    protected void holdTick(World world, LivingEntity user, INonStandPower power, int ticksHeld, ActionTarget target, boolean requirementsFulfilled) {
        if (!world.isClientSide()) {
        	int maxTicks = Math.max(getHoldDurationToFire(power), 1);
        	if(ticksHeld >= maxTicks && power.getEnergy() > 0) {
        		PillarmanDivineSandstormEntity sanstormWave = new PillarmanDivineSandstormEntity(world, user)
                        .setRadius(1.75F)
                        .setDamage(2F)
                        .setDuration(20);
                sanstormWave.shootFromRotation(user, 0.5F, 0.5F);
                world.addFreshEntity(sanstormWave);
        	}
        }
    }

}
