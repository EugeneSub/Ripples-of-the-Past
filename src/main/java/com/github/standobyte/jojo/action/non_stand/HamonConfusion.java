package com.github.standobyte.jojo.action.non_stand;

import com.github.standobyte.jojo.action.ActionTarget;
import com.github.standobyte.jojo.action.player.ContinuousActionInstance;
import com.github.standobyte.jojo.action.player.IPlayerAction;
import com.github.standobyte.jojo.capability.entity.PlayerUtilCap;
import com.github.standobyte.jojo.client.ClientUtil;
import com.github.standobyte.jojo.client.playeranim.anim.ModPlayerAnimations;
import com.github.standobyte.jojo.init.ModSounds;
import com.github.standobyte.jojo.init.ModStatusEffects;
import com.github.standobyte.jojo.init.power.non_stand.ModPowers;
import com.github.standobyte.jojo.power.impl.nonstand.INonStandPower;
import com.github.standobyte.jojo.power.impl.nonstand.type.hamon.HamonData;
import com.github.standobyte.jojo.power.impl.nonstand.type.hamon.skill.BaseHamonSkill.HamonStat;
import com.github.standobyte.jojo.util.mc.damage.DamageUtil;

import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.potion.EffectInstance;
import net.minecraft.util.Hand;
import net.minecraft.world.World;

public class HamonConfusion extends HamonAction implements IPlayerAction<HamonConfusion.Instance, INonStandPower> {

    public HamonConfusion(HamonAction.Builder builder) {
        super(builder);
    }
    
    
    @Override
    protected void perform(World world, LivingEntity user, INonStandPower power, ActionTarget target) {
        if (!world.isClientSide()) {
            setPlayerAction(user, power);
            world.playSound(null, user.getX(), user.getEyeY(), user.getZ(), ModSounds.HAMON_EFFECT.get(), user.getSoundSource(), 1F, 1F);
        }
    }
    
    @Override
    public HamonConfusion.Instance createContinuousActionInstance(
            LivingEntity user, PlayerUtilCap userCap, INonStandPower power) {
        if (user.level.isClientSide() && user instanceof PlayerEntity) {
            ModPlayerAnimations.hamonConfusion.setAnimEnabled((PlayerEntity) user, true);
        }
        return new Instance(user, userCap, power, this);
    }
    
    
    @Override
    public void setCooldownOnUse(INonStandPower power) {} // cooldown is set inside the continuous action instance
    
    @Override
    protected void consumeEnergy(World world, LivingEntity user, INonStandPower power, ActionTarget target) {} // and so is energy consumption
    
    
    public static class Instance extends ContinuousActionInstance<HamonConfusion, INonStandPower> {
        private HamonData userHamon;
        
        public Instance(LivingEntity user, PlayerUtilCap userCap, 
                INonStandPower playerPower, HamonConfusion action) {
            super(user, userCap, playerPower, action);
            
            userHamon = playerPower.getTypeSpecificData(ModPowers.HAMON.get()).get();
        }
        
        @Override
        public void playerTick() {
            switch (getTick()) {
            case 5:
                if (user.level.isClientSide()) {
                    user.level.playSound(ClientUtil.getClientPlayer(), user.getX(), user.getEyeY(), user.getZ(), 
                            ModSounds.HAMON_SYO_SWING.get(), user.getSoundSource(), 1.0f, 1.0f);
                    user.swing(Hand.MAIN_HAND, true);
                }
                break;
            case 10:
                if (!user.level.isClientSide()) {
                    ActionTarget target = playerPower.getMouseTarget();
                    if (target.getEntity() instanceof LivingEntity) {
                        punch((LivingEntity) target.getEntity());
                    }
                }
                break;
            case 15:
                stopAction();
                break;
            }
        }
        
        private void punch(LivingEntity target) {
            World world = user.level;
            if (!world.isClientSide()) {
            	HamonConfusion hamonAction = getAction();
                if (hamonAction.checkHeldItems(user, playerPower).isPositive()) {
                	HamonData hamon = playerPower.getTypeSpecificData(ModPowers.HAMON.get()).get();
                    float damage = 1.5f;
                    float cost = hamonAction.getEnergyCost(playerPower, new ActionTarget(target));
                    float efficiency = userHamon.getActionEfficiency(cost, true, hamonAction.getUnlockingSkill());
                    float strengthLvl = hamon.getHamonStrengthLevelRatio();
                    float controlLvl = hamon.getHamonControlLevelRatio();
                    float energyRatio = hamon.getEnergyRatio();
                    int duration = (int) (200 + (80 * controlLvl + 60 * energyRatio) * efficiency);
                    int amplifier = (int) (strengthLvl * 0.05F * efficiency);
                    
                    if (DamageUtil.dealHamonDamage(target, damage * efficiency, user, null)) {
                        world.playSound(null, target.getX(), target.getEyeY(), target.getZ(), ModSounds.HAMON_SYO_PUNCH.get(), target.getSoundSource(), 1F, 1.5F);
                        target.knockback(1.85F, user.getX() - target.getX(), user.getZ() - target.getZ());
                        target.addEffect(new EffectInstance(
                                ModStatusEffects.HAMON_CONFUSION.get(), duration, amplifier, false, false, true));
                        addPointsForAction(playerPower, userHamon, HamonStat.STRENGTH, cost, efficiency);
                        playerPower.consumeEnergy(cost);
                    }
                }
                
                HamonSunlightYellowOverdrive.doMeleeAttack(user, target);
            }
            
            if (user instanceof PlayerEntity) {
                ((PlayerEntity) user).resetAttackStrengthTicker();
            }
        }
        
        @Override
        public boolean updateTarget() {
            return true;
        }
        
        @Override
        public float getWalkSpeed() {
            return getAction().getHeldWalkSpeed();
        }
        
        @Override
        public void onStop() {
            super.onStop();
            if (user.level.isClientSide() && user instanceof PlayerEntity) {
                ModPlayerAnimations.hamonConfusion.setAnimEnabled((PlayerEntity) user, false);
            }
        }
        
    }
}
