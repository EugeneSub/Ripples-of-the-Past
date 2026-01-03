package com.github.standobyte.jojo.action.non_stand;

import com.github.standobyte.jojo.action.ActionConditionResult;
import com.github.standobyte.jojo.action.ActionTarget;
import com.github.standobyte.jojo.action.player.ContinuousActionInstance;
import com.github.standobyte.jojo.action.player.IPlayerAction;
import com.github.standobyte.jojo.capability.entity.LivingUtilCapProvider;
import com.github.standobyte.jojo.capability.entity.PlayerUtilCap;
import com.github.standobyte.jojo.client.ClientUtil;
import com.github.standobyte.jojo.client.playeranim.anim.ModPlayerAnimations;
import com.github.standobyte.jojo.init.ModSounds;
import com.github.standobyte.jojo.init.power.non_stand.ModPowers;
import com.github.standobyte.jojo.power.impl.nonstand.INonStandPower;
import com.github.standobyte.jojo.power.impl.nonstand.type.hamon.HamonData;
import com.github.standobyte.jojo.power.impl.nonstand.type.hamon.skill.BaseHamonSkill.HamonStat;
import com.github.standobyte.jojo.util.mc.damage.DamageUtil;

import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.ai.attributes.Attributes;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.DamageSource;
import net.minecraft.util.Hand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.vector.Vector3d;
import net.minecraft.world.World;

public class HamonAfterimage extends HamonAction implements IPlayerAction<HamonAfterimage.Instance, INonStandPower> {

    public HamonAfterimage(HamonAction.Builder builder) {
        super(builder);
    }
    
    @Override
    protected ActionConditionResult checkSpecificConditions(LivingEntity user, INonStandPower power, ActionTarget target) {
        return ActionConditionResult.noMessage(user.isOnGround());
    }
    
    @Override
    protected void perform(World world, LivingEntity user, INonStandPower power, ActionTarget target) {
        if (!world.isClientSide()) {
            setPlayerAction(user, power);
        }
    }
    
    @Override
    public HamonAfterimage.Instance createContinuousActionInstance(
            LivingEntity user, PlayerUtilCap userCap, INonStandPower power) {
        return new Instance(user, userCap, power, this);
    }
    
    
    @Override
    public void setCooldownOnUse(INonStandPower power) {} // cooldown is set inside the continuous action instance
    
    
    
    public static class Instance extends ContinuousActionInstance<HamonAfterimage, INonStandPower> {
    	
    	BlockPos originalPos;
    	boolean isShift = false;
        private HamonData userHamon;
        
        public Instance(LivingEntity user, PlayerUtilCap userCap, 
                INonStandPower playerPower, HamonAfterimage action) {
            super(user, userCap, playerPower, action);
            
            userHamon = playerPower.getTypeSpecificData(ModPowers.HAMON.get()).get();
        }
        
        @Override
        public void playerTick() {
            switch (getTick()) {
            case 1:
            	if(user.isShiftKeyDown()) {
            		originalPos = user.blockPosition();
            		isShift = true;
            	}
            	user.getCapability(LivingUtilCapProvider.CAPABILITY).ifPresent(cap -> {
	                cap.addAfterimages(6, 10);
	            });
            	Vector3d move = Vector3d.directionFromRotation(user.xRot, user.yRot)
                        .scale(1 + user.getAttributeValue(Attributes.MOVEMENT_SPEED) * 5);
                user.setDeltaMovement(move.x * 3, move.y * 0.1, move.z * 3);
                break;
            case 5:
                if (!user.level.isClientSide()) {
                    
                }
                break;
            case 12:
            	if(isShift) {
            		user.teleportTo(originalPos.getX(), originalPos.getY(), originalPos.getZ());	
            	}
            	float cost = action.getEnergyCost(playerPower, new ActionTarget(user));
                float efficiency = userHamon.getActionEfficiency(cost, true, action.getUnlockingSkill());
            	
            	playerPower.setCooldownTimer(getAction(), 20);
            	addPointsForAction(playerPower, userHamon, HamonStat.CONTROL, cost, efficiency);
                stopAction();
                break;
            }
        }
        
        @Override
        public boolean cancelIncomingDamage(DamageSource dmgSource, float dmgAmount) {
            return true;
        }  
        
    }
}
