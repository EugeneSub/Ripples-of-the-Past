package com.github.standobyte.jojo.action.non_stand;

import java.util.UUID;

import com.github.standobyte.jojo.action.ActionConditionResult;
import com.github.standobyte.jojo.action.ActionTarget;
import com.github.standobyte.jojo.action.player.ContinuousActionInstance;
import com.github.standobyte.jojo.action.player.IPlayerAction;
import com.github.standobyte.jojo.capability.entity.PlayerUtilCap;
import com.github.standobyte.jojo.client.ClientUtil;
import com.github.standobyte.jojo.client.playeranim.anim.ModPlayerAnimations;
import com.github.standobyte.jojo.client.sound.ClientTickingSoundsHelper;
import com.github.standobyte.jojo.init.ModParticles;
import com.github.standobyte.jojo.init.ModSounds;
import com.github.standobyte.jojo.init.power.non_stand.ModPowers;
import com.github.standobyte.jojo.power.impl.nonstand.INonStandPower;
import com.github.standobyte.jojo.power.impl.nonstand.type.hamon.HamonData;
import com.github.standobyte.jojo.power.impl.nonstand.type.hamon.skill.BaseHamonSkill.HamonStat;
import com.github.standobyte.jojo.util.mc.damage.DamageUtil;

import it.unimi.dsi.fastutil.objects.Object2FloatArrayMap;
import it.unimi.dsi.fastutil.objects.Object2FloatMap;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.Hand;
import net.minecraft.world.World;

public class HamonSunlightYellowOverdrive extends HamonAction implements IPlayerAction<HamonSunlightYellowOverdrive.Instance, INonStandPower> {

    public HamonSunlightYellowOverdrive(HamonAction.Builder builder) {
        super(builder.needsFreeMainHand());
    }
    
    @Override
    public ActionConditionResult checkSpecificConditions(LivingEntity user, INonStandPower power, ActionTarget target) {
        if (power.getHeldAction() != this) {
            if (power.getEnergy() <= 0) {
                return conditionMessage("some_energy");
            }
        }
        return ActionConditionResult.POSITIVE;
    }
    
    @Override
    public boolean holdOnly(INonStandPower power) {
        return false;
    }
    
    @Override
    public int getHoldDurationMax(INonStandPower power) {
        return Integer.MAX_VALUE;
    }
    
    private int getMaxPowerTicks(INonStandPower power) {
        return super.getHoldDurationMax(power);
    }
    
    
    private Object2FloatMap<UUID> playerSpentEnergy = new Object2FloatArrayMap<>();
    @Override
    public float getHeldTickEnergyCost(INonStandPower power) {
        return Math.min(power.getMaxEnergy() / Math.max(getMaxPowerTicks(power), 1), power.getEnergy());
    }
    
    @Override
    public void onHoldTick(World world, LivingEntity user, INonStandPower power, int ticksHeld, ActionTarget target, boolean requirementsFulfilled) {
        float spentEnergy = power.getEnergy();
        super.onHoldTick(world, user, power, ticksHeld, target, requirementsFulfilled);
        if (power.isUserCreative()) {
            spentEnergy = getHeldTickEnergyCost(power);
        }
        else {
            spentEnergy -= power.getEnergy();
        }
        if (spentEnergy > 0) {
            playerSpentEnergy.put(user.getUUID(), getSpentEnergy(power) + spentEnergy);
        }
    }
    
    public float getSpentEnergy(INonStandPower power) {
        float spentEnergy = playerSpentEnergy.getOrDefault(power.getUser().getUUID(), 0);
        return spentEnergy;
    }
    
    
    @Override
    public void startedHolding(World world, LivingEntity user, INonStandPower power, ActionTarget target, boolean requirementsFulfilled) {
        playerSpentEnergy.removeFloat(user.getUUID());
        if (requirementsFulfilled && world.isClientSide()) {
            ClientTickingSoundsHelper.playStoppableEntitySound(user, ModSounds.HAMON_SYO_CHARGE.get(), 
                    1.0F, 1.0F, false, entity -> power.getHeldAction() == this);
        }
    }
    
    @Override
    public boolean clHeldStartAnim(PlayerEntity user) {
        return ModPlayerAnimations.sunlightYellowOverdrive.setWindupAnim(user);
    }
    
    @Override
    protected void perform(World world, LivingEntity user, INonStandPower power, ActionTarget target) {
        if (!user.level.isClientSide()) {
            setPlayerAction(user, power);
            playerSpentEnergy.removeFloat(user.getUUID());
        }
    }
    
    @Override
    public Instance createContinuousActionInstance(
            LivingEntity user, PlayerUtilCap userCap, INonStandPower power) {
        if (user.level.isClientSide() && user instanceof PlayerEntity) {
            ModPlayerAnimations.sunlightYellowOverdrive.setAttackAnim((PlayerEntity) user);
        }
        return new Instance(user, userCap, power, this, getSpentEnergy(power));
    }
    
    @Override
    public void stoppedHolding(World world, LivingEntity user, INonStandPower power, int ticksHeld, boolean willFire) {
        if (!willFire) {
            if (!world.isClientSide()) {
                if (!power.isUserCreative()) {
                    float energySpent = getSpentEnergy(power);
                    power.setEnergy(Math.min(power.getMaxEnergy(), power.getEnergy() + energySpent));
                }
            }
            // FIXME always triggers for other players in MP
            // FIXME sometimes wrong on the client side of the user too
            else if (user instanceof PlayerEntity) {
                ModPlayerAnimations.sunlightYellowOverdrive.stopAnim((PlayerEntity) user);
            }
        }
    }
    
    
    
    public static class Instance extends ContinuousActionInstance<Instance, INonStandPower> {
        private float energySpentRatio;
        private HamonData userHamon;
        private final HamonSunlightYellowOverdrive hamonAction;
        
        public Instance(LivingEntity user, PlayerUtilCap userCap, INonStandPower playerPower,
                IPlayerAction<Instance, INonStandPower> action, float spentEnergy) {
            super(user, userCap, playerPower, action);
            energySpentRatio = playerPower == null ? 0 : Math.min(spentEnergy / playerPower.getEnergy(), 1);
            
            userHamon = playerPower.getTypeSpecificData(ModPowers.HAMON.get()).get();
            hamonAction = (HamonSunlightYellowOverdrive) action;
        }
        
        @Override
        public void playerTick() {
            switch (getTick()) {
            case 0:
                if (user.level.isClientSide()) {
                    user.level.playSound(ClientUtil.getClientPlayer(), user.getX(), user.getEyeY(), user.getZ(), 
                            ModSounds.HAMON_SYO_SWING.get(), user.getSoundSource(), 1.0f, 1.0f);
                    user.swing(Hand.MAIN_HAND, true);
                }
                break;
            case 4:
                if (!user.level.isClientSide()) {
                    ActionTarget target = playerPower.getMouseTarget();
                    if (target.getEntity() instanceof LivingEntity) {
                        punch((LivingEntity) target.getEntity());
                    }
                }
                break;
            case 11:
                stopAction();
                break;
            }
        }
        
        private void punch(LivingEntity target) {
            World world = user.level;
            
            if (!world.isClientSide()) {
                if (hamonAction.checkHeldItems(user, playerPower).isPositive()) {
                    float efficiency = userHamon.getActionEfficiency(0, true, hamonAction.getUnlockingSkill());
                    float damage = 3.25F + 6.75F * energySpentRatio;
                    damage *= efficiency;
                    
                    if (DamageUtil.dealHamonDamage(target, damage, user, null, attack -> attack.hamonParticle(ModParticles.HAMON_SPARK_YELLOW.get()))) {
                        world.playSound(null, target.getX(), target.getEyeY(), target.getZ(), ModSounds.HAMON_SYO_PUNCH.get(), target.getSoundSource(), energySpentRatio, 1.0F);
                        userHamon.hamonPointsFromAction(HamonStat.STRENGTH, playerPower.getMaxEnergy() * energySpentRatio * efficiency);
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
            return hamonAction.getHeldWalkSpeed();
        }
        
        @Override
        public void onStop() {
            super.onStop();
            if (user.level.isClientSide() && user instanceof PlayerEntity) {
                ModPlayerAnimations.sunlightYellowOverdrive.stopAnim((PlayerEntity) user);
            }
        }
        
        @Override
        protected Instance getThis() {
            return this;
        }
        
    }
    
    
    public static void doMeleeAttack(LivingEntity attacker, LivingEntity targetEntity) {
        if (attacker instanceof PlayerEntity) {
            PlayerEntity player = (PlayerEntity) attacker;
            player.attack(targetEntity);
        }
        else if (!attacker.level.isClientSide()) {
            attacker.doHurtTarget(targetEntity);
        }
    }
    
}
