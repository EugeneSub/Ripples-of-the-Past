package com.github.standobyte.jojo.action.non_stand;

import com.github.standobyte.jojo.action.ActionConditionResult;
import com.github.standobyte.jojo.action.ActionTarget;
import com.github.standobyte.jojo.action.ActionTarget.TargetType;
import com.github.standobyte.jojo.client.ClientUtil;
import com.github.standobyte.jojo.client.particle.custom.CustomParticlesHelper;
import com.github.standobyte.jojo.client.playeranim.anim.ModPlayerAnimations;
import com.github.standobyte.jojo.client.sound.ClientTickingSoundsHelper;
import com.github.standobyte.jojo.entity.CrimsonBubbleEntity;
import com.github.standobyte.jojo.init.ModParticles;
import com.github.standobyte.jojo.init.ModSounds;
import com.github.standobyte.jojo.init.power.non_stand.ModPowers;
import com.github.standobyte.jojo.power.impl.nonstand.INonStandPower;
import com.github.standobyte.jojo.power.impl.nonstand.type.hamon.HamonData;
import com.github.standobyte.jojo.power.impl.nonstand.type.hamon.skill.BaseHamonSkill.HamonStat;

import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.item.ItemEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.util.DamageSource;
import net.minecraft.util.Hand;
import net.minecraft.world.World;

public class HamonCrimsonBubble extends HamonAction {
    
    public HamonCrimsonBubble(HamonAction.Builder builder) {
        super(builder);
    }
      
    @Override
    protected ActionConditionResult checkSpecificConditions(LivingEntity user, INonStandPower power, ActionTarget target) {
    	if (user.getHealth() >= user.getMaxHealth()
                && !(user instanceof PlayerEntity && ((PlayerEntity) user).abilities.invulnerable)) {
            return conditionMessage("full_health");
        } else {
        	return ActionConditionResult.POSITIVE;
        }
    }

    @Override
    public void startedHolding(World world, LivingEntity user, INonStandPower power, ActionTarget target, boolean requirementsFulfilled) {
        if (requirementsFulfilled && world.isClientSide()) {
        	ClientTickingSoundsHelper.playStoppableEntitySound(user, ModSounds.HAMON_SYO_CHARGE.get(), 
                    1.75F, 1.0F, false, entity -> power.getHeldAction() == this);
        }
    }
    
    @Override
    public void onHoldTickClientEffect(LivingEntity user, INonStandPower power, int ticksHeld, boolean reqFulfilled, boolean reqStateChanged) {
        if (reqFulfilled) {
        	if (user.level.isClientSide()) {
                boolean isUserTheCameraEntity = user == ClientUtil.getCameraEntity();
                for (int i = 0; i < 18; i++) {
                    CustomParticlesHelper.createHamonAuraParticle(ModParticles.HAMON_AURA.get(), user, 
                            user.getX() + (Math.random() - 0.5) * (user.getBbWidth() + 0.5F), 
                            user.getY() + Math.random() * (user.getBbHeight() * 0.5F), 
                            user.getZ() + (Math.random() - 0.5) * (user.getBbWidth() + 0.5F));
                }
                if (isUserTheCameraEntity) {
                    CustomParticlesHelper.summonHamonAuraParticlesFirstPerson(ModParticles.HAMON_AURA.get(), user, 6 / 5);
                }
            }
        }
    }
    
    @Override
    protected void perform(World world, LivingEntity user, INonStandPower power, ActionTarget target) {
        if (!world.isClientSide()) {
    		HamonData hamon = power.getTypeSpecificData(ModPowers.HAMON.get()).get();
        	CrimsonBubbleEntity bubble = new CrimsonBubbleEntity(user.level);
            ItemStack heldItem = user.getMainHandItem();
            if (!heldItem.isEmpty()) {
            	user.setItemInHand(Hand.MAIN_HAND, ItemStack.EMPTY);
                ItemEntity item = new ItemEntity(user.level, user.getX(), user.getEyeY() - 0.3D, user.getZ(), heldItem);
                item.setPickUpDelay(2);
                user.level.addFreshEntity(item);
                bubble.putItem(item);
            }
            bubble.moveTo(user.getX(), user.getEyeY() + 1, user.getZ(), user.yRot, user.xRot);
            bubble.setHamonPoints(hamon.getHamonStrengthPoints(), hamon.getHamonControlPoints());
            user.level.addFreshEntity(bubble);
            HamonData giverHamon = INonStandPower.getPlayerNonStandPower((PlayerEntity)user.getEntity()).getTypeSpecificData(ModPowers.HAMON.get()).get();
            giverHamon.setHamonStatPoints(HamonStat.STRENGTH, 0, true, true);
            giverHamon.setHamonStatPoints(HamonStat.CONTROL, 0, true, true);
        }
    }
    
    @Override
    public boolean cancelHeldOnGettingAttacked(INonStandPower power, DamageSource dmgSource, float dmgAmount) {
        return true;
    }
    
    @Override
    public boolean clHeldStartAnim(PlayerEntity user) {
        return ModPlayerAnimations.caesarFinalHamon.setAnimEnabled(user, true);
    }
    
    @Override
    public void clHeldStopAnim(PlayerEntity user) {
        ModPlayerAnimations.caesarFinalHamon.setAnimEnabled(user, false);
    }
}
