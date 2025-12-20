package com.github.standobyte.jojo.action.non_stand;

import com.github.standobyte.jojo.action.ActionConditionResult;
import com.github.standobyte.jojo.action.ActionTarget;
import com.github.standobyte.jojo.client.ClientUtil;
import com.github.standobyte.jojo.client.particle.custom.CustomParticlesHelper;
import com.github.standobyte.jojo.client.sound.ClientTickingSoundsHelper;
import com.github.standobyte.jojo.entity.damaging.projectile.HamonTurquoiseBlueOverdriveEntity;
import com.github.standobyte.jojo.init.ModParticles;
import com.github.standobyte.jojo.init.ModSounds;
import com.github.standobyte.jojo.init.power.non_stand.ModPowers;
import com.github.standobyte.jojo.power.impl.nonstand.INonStandPower;
import com.github.standobyte.jojo.power.impl.nonstand.type.hamon.HamonData;

import net.minecraft.entity.LivingEntity;
import net.minecraft.util.Direction.Axis;
import net.minecraft.world.World;

public class HamonTurquoiseBlueOverdrive extends HamonAction {

    public HamonTurquoiseBlueOverdrive(HamonAction.Builder builder) {
        super(builder.needsFreeMainHand());
    }
    
    @Override
    protected ActionConditionResult checkSpecificConditions(LivingEntity user, INonStandPower power, ActionTarget target) {
        if (!user.isInWaterOrBubble()) {
            return conditionMessage("underwater");
        }
        return ActionConditionResult.POSITIVE;
    }
    
    @Override
    public void startedHolding(World world, LivingEntity user, INonStandPower power, ActionTarget target, boolean requirementsFulfilled) {
        if (requirementsFulfilled && world.isClientSide()) {
        	ClientTickingSoundsHelper.playStoppableEntitySound(user, ModSounds.HAMON_CHANNEL.get(), 
                    0.9F, 0.9F, false, entity -> power.getHeldAction() == this);
        }
    }
    
    @Override
    public void onHoldTickClientEffect(LivingEntity user, INonStandPower power, int ticksHeld, boolean reqFulfilled, boolean reqStateChanged) {
        if (reqFulfilled) {
        	if (user.level.isClientSide()) {
                boolean isUserTheCameraEntity = user == ClientUtil.getCameraEntity();
                if (isUserTheCameraEntity) {
                    CustomParticlesHelper.summonHamonAuraParticlesFirstPerson(ModParticles.HAMON_AURA_BLUE.get(), user, 6 / 5);
                }
            }
        }
    }
    
    @Override
    protected void perform(World world, LivingEntity user, INonStandPower power, ActionTarget target) {
        if (!world.isClientSide()) {
            HamonData hamon = power.getTypeSpecificData(ModPowers.HAMON.get()).get();
            float energyCost = getEnergyCost(power, target);
            float hamonEfficiency = hamon.getActionEfficiency(energyCost, true, getUnlockingSkill());
            float hamonControl = hamon.getHamonControlLevelRatio();
            
            HamonTurquoiseBlueOverdriveEntity overdriveWave = new HamonTurquoiseBlueOverdriveEntity(world, user)
                    .setRadius(2F + (float) (2.5F * hamonControl * hamonEfficiency))
                    .setDamage(1.25F * hamonEfficiency)
                    .setPoints(Math.min(energyCost, power.getEnergy()) * hamonEfficiency)
                    .setDuration(30 + (int) (70 * hamonControl));
            overdriveWave.shootFromRotation(user, 1.5F, 0);
            world.addFreshEntity(overdriveWave);
        }
    }
}
