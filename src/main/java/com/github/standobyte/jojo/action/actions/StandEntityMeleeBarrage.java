package com.github.standobyte.jojo.action.actions;

import com.github.standobyte.jojo.action.ActionConditionResult;
import com.github.standobyte.jojo.action.ActionTarget;
import com.github.standobyte.jojo.entity.stand.StandEntity;
import com.github.standobyte.jojo.entity.stand.task.MeleeAttackTask;
import com.github.standobyte.jojo.power.stand.IStandPower;
import com.github.standobyte.jojo.power.stand.type.EntityStandType;

import net.minecraft.entity.LivingEntity;
import net.minecraft.util.math.MathHelper;
import net.minecraft.world.World;

public class StandEntityMeleeBarrage extends StandEntityAction {

    public StandEntityMeleeBarrage(StandEntityAction.Builder builder) {
        super(builder);
        this.doNotAutoSummonStand = true;
    }
    
    @Override
    protected ActionConditionResult checkSpecificConditions(LivingEntity user, LivingEntity performer, IStandPower power, ActionTarget target) {
        return performer instanceof StandEntity && !((StandEntity) performer).canAttackMelee() ? 
                ActionConditionResult.NEGATIVE
                : super.checkSpecificConditions(user, performer, power, target);
    }
    
    @Override
    public void startedHolding(World world, LivingEntity user, IStandPower power, ActionTarget target, boolean requirementsFulfilled) {
        if (!world.isClientSide() && requirementsFulfilled) {
            StandEntity stand;
            if (power.isActive()) {
                stand = (StandEntity) getPerformer(user, power);
            }
            else {
                ((EntityStandType<?>) power.getType()).summon(user, power, entity -> {
                    entity.setArmsOnlyMode();
                }, true);
                stand = (StandEntity) power.getStandManifestation();
            }
            stand.setTask(new MeleeAttackTask(stand, true));
        }
    }
    
    @Override
    public void stoppedHolding(World world, LivingEntity user, IStandPower power, int ticksHeld) {
        if (!world.isClientSide() && power.isActive()) {
            ((StandEntity) power.getStandManifestation()).clearTask();
        }
    }

    @Override
    public int getCooldown(IStandPower power, int ticksHeld) {
        return MathHelper.floor((float) (getCooldownValue() * ticksHeld) / (float) getHoldDurationMax() + 0.5F);
    }
}
