package com.github.standobyte.jojo.action.non_stand;

import com.github.standobyte.jojo.action.ActionTarget;
import com.github.standobyte.jojo.capability.entity.LivingUtilCapProvider;
import com.github.standobyte.jojo.client.playeranim.anim.ModPlayerAnimations;
import com.github.standobyte.jojo.client.sound.ClientTickingSoundsHelper;
import com.github.standobyte.jojo.init.ModSounds;
import com.github.standobyte.jojo.power.impl.nonstand.INonStandPower;

import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.world.World;

public class PillarmanEvasion extends PillarmanAction {

    public PillarmanEvasion(PillarmanAction.Builder builder) {
        super(builder);
        stage = 2;
    }

    @Override
    public void onHoldTickClientEffect(LivingEntity user, INonStandPower power, int ticksHeld, boolean requirementsFulfilled, boolean stateRefreshed) {
        if (stateRefreshed && requirementsFulfilled) {
            ClientTickingSoundsHelper.playHeldActionSound(ModSounds.HAMON_SYO_SWING.get(), 1.0F, 1.25F, true, user, power, this); // TODO separate sound event
        }
    }
    
    @Override
    public void startedHolding(World world, LivingEntity user, INonStandPower power, ActionTarget target, boolean requirementsFulfilled) {
        if (!world.isClientSide() && requirementsFulfilled) {
        	/*user.getCapability(LivingUtilCapProvider.CAPABILITY).ifPresent(cap -> {
	            cap.addAfterimages(4, 40);
	        });*/
        }
    }
    
    @Override
    public int getCooldownAdditional(INonStandPower power, int ticksHeld) {
        return cooldownFromHoldDuration(super.getCooldownAdditional(power, ticksHeld), power, ticksHeld);
    }
    
    @Override
    public boolean clHeldStartAnim(PlayerEntity user) {
        return ModPlayerAnimations.pillarmanEvasion.setAnimEnabled(user, true);
    }
    
    @Override
    public void clHeldStopAnim(PlayerEntity user) {
        ModPlayerAnimations.pillarmanEvasion.setAnimEnabled(user, false);
    }
}
