package com.github.standobyte.jojo.action.non_stand;

import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import javax.annotation.Nullable;

import com.github.standobyte.jojo.action.ActionConditionResult;
import com.github.standobyte.jojo.action.ActionTarget;
import com.github.standobyte.jojo.action.ActionTarget.TargetType;
import com.github.standobyte.jojo.action.non_stand.HamonSnakeMuffler.Instance;
import com.github.standobyte.jojo.action.player.ContinuousActionInstance;
import com.github.standobyte.jojo.action.player.IPlayerAction;
import com.github.standobyte.jojo.action.stand.StandEntityAction.Phase;
import com.github.standobyte.jojo.capability.entity.PlayerUtilCap;
import com.github.standobyte.jojo.client.playeranim.anim.ModPlayerAnimations;
import com.github.standobyte.jojo.client.sound.ClientTickingSoundsHelper;
import com.github.standobyte.jojo.client.sound.HamonSparksLoopSound;
import com.github.standobyte.jojo.init.ModEntityTypes;
import com.github.standobyte.jojo.init.ModParticles;
import com.github.standobyte.jojo.init.ModSounds;
import com.github.standobyte.jojo.init.ModStatusEffects;
import com.github.standobyte.jojo.init.power.non_stand.ModPowers;
import com.github.standobyte.jojo.init.power.non_stand.hamon.ModHamonActions;
import com.github.standobyte.jojo.init.power.non_stand.hamon.ModHamonSkills;
import com.github.standobyte.jojo.power.impl.nonstand.INonStandPower;
import com.github.standobyte.jojo.power.impl.nonstand.type.hamon.HamonData;
import com.github.standobyte.jojo.power.impl.nonstand.type.hamon.HamonUtil;
import com.github.standobyte.jojo.power.impl.nonstand.type.hamon.skill.BaseHamonSkill.HamonStat;
import com.github.standobyte.jojo.util.general.LazySupplier;
import com.github.standobyte.jojo.util.general.MathUtil;
import com.github.standobyte.jojo.util.mc.MCUtil;
import com.github.standobyte.jojo.util.mc.damage.DamageUtil;
import com.github.standobyte.jojo.util.mc.damage.KnockbackCollisionImpact;
import com.github.standobyte.jojo.util.mod.JojoModUtil;

import net.minecraft.block.BlockState;
import net.minecraft.block.SoundType;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.ai.attributes.Attributes;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.potion.EffectInstance;
import net.minecraft.util.DamageSource;
import net.minecraft.util.EntityDamageSource;
import net.minecraft.util.Hand;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.vector.Vector3d;
import net.minecraft.world.World;
import net.minecraft.world.server.ServerWorld;
import net.minecraftforge.common.ForgeHooks;

public class HamonGrip extends HamonAction implements IPlayerAction<HamonGrip.Instance, INonStandPower> {

    public HamonGrip(HamonAction.Builder builder) {
        super(builder);
    }
    
    @Override
    public ActionConditionResult checkTarget(ActionTarget target, LivingEntity user, INonStandPower power) {
        Entity entity = target.getEntity();
        if (entity == null) {
            return ActionConditionResult.NEGATIVE;
        }
        return super.checkTarget(target, user, power);
    }
    
    @Override
    protected ActionConditionResult checkHeldItems(LivingEntity user, INonStandPower power) {
        if (!MCUtil.areHandsFree(user, Hand.MAIN_HAND, Hand.OFF_HAND)) {
            return conditionMessage("hands");
        }
        return ActionConditionResult.POSITIVE;
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
    protected void perform(World world, LivingEntity user, INonStandPower power, ActionTarget target) {
    	Optional<HamonGrip.Instance> curGrip = getCurGrip(user);
    	if (curGrip.isPresent()) {
            HamonGrip.Instance grip = curGrip.get();
            if (!world.isClientSide()) {
                if (grip.canThrow()) {
                    grip.doThrow();
                }
            }
        } else if (user instanceof PlayerEntity) {
            	if (!world.isClientSide()) {
            		setPlayerAction(user, power);
            	} else {
            		ClientTickingSoundsHelper.playStoppableEntitySound(user, ModSounds.HAMON_SYO_CHARGE.get(), 
            				1.1F, 1.0F, false, entity -> ContinuousActionInstance.getCurrentAction(user).map(
            						playerAction -> playerAction.isStopped()).orElse(false), Instance.USUAL_ACTION_DURATION);
            	}
        }
    }
    
    private static boolean dealPhysicalDamage(LivingEntity user, Entity target) {
        return target.hurt(new EntityDamageSource(user instanceof PlayerEntity ? "player" : "mob", user), 
                DamageUtil.getDamageWithoutHeldItem(user));
    }

    
    @Override
    public Instance createContinuousActionInstance(
            LivingEntity user, PlayerUtilCap userCap, INonStandPower power) {
        if (user.level.isClientSide() && user instanceof PlayerEntity) {
            ModPlayerAnimations.hamon_grip.setAnimEnabled((PlayerEntity) user, true);
        }
        Instance hamonGrip = new Instance(user, userCap, power, this);
        
        float energyCost = Math.min(getEnergyCost(power, ActionTarget.EMPTY), power.getEnergy());
        float efficiency = power.getTypeSpecificData(ModPowers.HAMON.get()).get().getActionEfficiency(energyCost, true, getUnlockingSkill());
        hamonGrip.setEnergySpent(energyCost * efficiency);
        
        return hamonGrip;
    }
    
    @Override
    protected boolean canBeUsedDuringPlayerAction(ContinuousActionInstance<?, ?> curPlayerAction) {
        return curPlayerAction.getAction() == this && ((HamonGrip.Instance) curPlayerAction).canThrow();
    }
    
    private final LazySupplier<ResourceLocation> throwTex = 
            new LazySupplier<>(() -> makeIconVariant(this, "_throw"));
    @Override
    public ResourceLocation getIconTexturePath(@Nullable INonStandPower power) {
        if (power != null && getCurGrip(power.getUser()).map(throwing -> throwing.canThrow()).orElse(false)) {
            return throwTex.get();
        }
        else {
            return super.getIconTexturePath(power);
        }
    }
    
    @Override
    public boolean greenSelection(INonStandPower power, ActionConditionResult conditionCheck) {
        return getCurGrip(power.getUser()).isPresent();
    }
    
    public static Optional<HamonGrip.Instance> getCurGrip(LivingEntity user) {
        return ContinuousActionInstance.getCurrentAction(user)
                .filter(action -> action.getAction() == ModHamonActions.WARRIOR_HAMON_GRIP.get())
                .map(action -> (HamonGrip.Instance) action);
    }
    
    public static class Instance extends ContinuousActionInstance<HamonGrip, INonStandPower> {
        private float energySpent;
        HamonData userHamon;

        public Instance(LivingEntity user, PlayerUtilCap userCap, 
                INonStandPower playerPower, HamonGrip action) {
            super(user, userCap, playerPower, action);
            userHamon = playerPower.getTypeSpecificData(ModPowers.HAMON.get()).get();
        }
        
        @Override
        public void onStart() {
            super.onStart();
            setPhase(Phase.WINDUP);
        }
        
        public void setEnergySpent(float energy) {
            this.energySpent = energy;
        }
        
        private void dealHamonDamage() {
            ActionTarget target = getPower().getMouseTarget();
            LivingEntity targetEntity = null;
                targetEntity = (LivingEntity) target.getEntity();
                targetEntity.addEffect(new EffectInstance(ModStatusEffects.STUN.get(), 10, 0, false, false, false));
                if (user instanceof PlayerEntity) {
                    //int invulTicks = targetEntity.invulnerableTime;
                    //((PlayerEntity) user).attack(targetEntity);
                    //targetEntity.invulnerableTime = invulTicks;
                }
                if (!user.level.isClientSide()) {
                    DamageUtil.dealHamonDamage(targetEntity, 0.3F, user, null, 
                            attack -> attack.hamonParticle(ModParticles.HAMON_SPARK_YELLOW.get()));
                    playerPower.consumeEnergy(90F);
                }
                INonStandPower.getNonStandPowerOptional(user).ifPresent(power -> {
                    power.getTypeSpecificData(ModPowers.HAMON.get()).ifPresent(hamon -> {
                        hamon.hamonPointsFromAction(HamonStat.STRENGTH, energySpent); 
                    });
                });
        }

        private static final int USUAL_ACTION_DURATION = 60;
        private static final int WINDUP_TICKS = 18;
        private static final int PERFORM_TICKS = 62;
        private static final int RECOVERY_TICKS = 140;
        @Override
        public void playerTick() {
            LivingEntity user = getUser();
            if(!user.level.isClientSide()) {
            	if(playerPower.getEnergy() <= 0) {
            		stopAction();
            	}
            }
            if(user.level.isClientSide()) {
        		HamonSparksLoopSound.playSparkSound(user, new Vector3d(user.getX(), user.getY(0.25), user.getZ()), 1.0F, true);
            } 
            switch (getPhase()) {
            case WINDUP:
                if (getTick() >= WINDUP_TICKS) {
                    setPhase(Phase.PERFORM);
                }
                break;
            case PERFORM:
                if (getTick() >= PERFORM_TICKS) {
                    stopAction();
                }
                break;
            case RECOVERY:
                if (getTick() >= RECOVERY_TICKS) {
                    stopAction();
                }
                break;
            default:
                throw new IllegalStateException();
            }
            
            if (getPhase() == Phase.PERFORM) {
            	if (!user.level.isClientSide()) {
                    ActionTarget target = playerPower.getMouseTarget();
                    ActionConditionResult result = getAction().checkTarget(target, user, playerPower);
                    if (!result.isPositive()) {
                        stopAction();
                    } else if (target.getType() == TargetType.ENTITY && target.getEntity() instanceof LivingEntity) {
                    	//dealPhysicalDamage(user, target.getEntity());
                        dealHamonDamage();
                    }
                }
            }
        }
        
        @Override
        public boolean cancelIncomingDamage(DamageSource dmgSource, float dmgAmount) {
        	if (getTick() > 10 && getTick() < 50) {
            	return DamageUtil.isMeleeAttack(dmgSource);
        	}
        	return false;
        }
        
        @Override
        public boolean updateTarget() {
            return true;
        }
        
        @Override
        public float getWalkSpeed() {
            return 0;
        }
        
        public boolean canThrow() {
            return getPhase() == Phase.PERFORM;
        }
        
        public void doThrow() {
            if (getPhase() == Phase.PERFORM) {
            	setPhase(Phase.RECOVERY);
            	tick = 100;
            	if (!user.level.isClientSide()) {
                    ActionTarget target = playerPower.getMouseTarget();
                    ActionConditionResult result = getAction().checkTarget(target, user, playerPower);
                    if (!result.isPositive()) {
                        stopAction();
                    }
                    else if (target.getType() == TargetType.ENTITY && target.getEntity() instanceof LivingEntity) {
                    	LivingEntity targetEntity = (LivingEntity) target.getEntity();
                    	targetEntity.removeEffect(ModStatusEffects.STUN.get());
                        DamageUtil.knockback(targetEntity, 3F, user.yRot + 180F);
                        boolean hamonSpread = userHamon.isSkillLearned(ModHamonSkills.HAMON_SPREAD.get());
                        KnockbackCollisionImpact.getHandler((LivingEntity) target.getEntity()).ifPresent(cap -> {
                            cap.onPunchSetKnockbackImpact(target.getEntity().getDeltaMovement(), user);
                            if (hamonSpread) {
                            	targetEntity.addEffect(new EffectInstance(ModStatusEffects.HAMON_SPREAD.get(), 200, 3, false, false));
                                cap.hamonDamage(DamageUtil.getDamageWithoutHeldItem(user), 0, ModParticles.HAMON_SPARK_YELLOW.get());
                            }
                        });
                    }
                }
            	//stopAction();
            }
        }
        
        @Override
        protected void onPhaseSet(Phase oldPhase, Phase nextPhase) {
            if (user.level.isClientSide() && user instanceof PlayerEntity) {
                setAnim((PlayerEntity) user, nextPhase);
            }
        }
        
        private void setAnim(PlayerEntity abstrClientPlayer, Phase phase) {
            switch (phase) {
            case RECOVERY:
            	ModPlayerAnimations.hamon_grip.setAnimEnabled((PlayerEntity) user, false);
                ModPlayerAnimations.hamon_grip_throw.setAnimEnabled((PlayerEntity) user, true);
                break;
            default:
                break;
            }
        }
        
        @Override
        public void onStop() {
            super.onStop();
            if (user.level.isClientSide() && user instanceof PlayerEntity) {
                ModPlayerAnimations.hamon_grip.setAnimEnabled((PlayerEntity) user, false);
                ModPlayerAnimations.hamon_grip_throw.setAnimEnabled((PlayerEntity) user, false);
            }
        }
    }
}

