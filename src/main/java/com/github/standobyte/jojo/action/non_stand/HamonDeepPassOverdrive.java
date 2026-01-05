package com.github.standobyte.jojo.action.non_stand;

import com.github.standobyte.jojo.action.ActionConditionResult;
import com.github.standobyte.jojo.action.ActionTarget;
import com.github.standobyte.jojo.action.ActionTarget.TargetType;
import com.github.standobyte.jojo.advancements.ModCriteriaTriggers;
import com.github.standobyte.jojo.capability.entity.LivingUtilCap;
import com.github.standobyte.jojo.capability.entity.LivingUtilCap.HypnosisTargetCheck;
import com.github.standobyte.jojo.capability.entity.LivingUtilCapProvider;
import com.github.standobyte.jojo.client.ClientUtil;
import com.github.standobyte.jojo.client.particle.custom.CustomParticlesHelper;
import com.github.standobyte.jojo.client.playeranim.anim.ModPlayerAnimations;
import com.github.standobyte.jojo.client.sound.ClientTickingSoundsHelper;
import com.github.standobyte.jojo.client.sound.HamonSparksLoopSound;
import com.github.standobyte.jojo.init.ModParticles;
import com.github.standobyte.jojo.init.ModSounds;
import com.github.standobyte.jojo.init.power.non_stand.ModPowers;
import com.github.standobyte.jojo.init.power.non_stand.hamon.ModHamonSkills;
import com.github.standobyte.jojo.potion.HypnosisEffect;
import com.github.standobyte.jojo.power.impl.nonstand.INonStandPower;
import com.github.standobyte.jojo.power.impl.nonstand.type.hamon.HamonData;
import com.github.standobyte.jojo.power.impl.nonstand.type.hamon.HamonUtil;
import com.github.standobyte.jojo.power.impl.nonstand.type.hamon.skill.BaseHamonSkill.HamonStat;
import com.github.standobyte.jojo.util.mod.JojoModUtil;

import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.util.DamageSource;
import net.minecraft.util.math.vector.Vector3d;
import net.minecraft.world.World;

public class HamonDeepPassOverdrive extends HamonAction {
    
    public HamonDeepPassOverdrive(HamonAction.Builder builder) {
        super(builder);
    }
    
    @Override
    public TargetRequirement getTargetRequirement() {
        return TargetRequirement.ENTITY;
    }
    
    @Override
    protected ActionConditionResult checkSpecificConditions(LivingEntity user, INonStandPower power, ActionTarget target) {
        if (!(user instanceof PlayerEntity)) {
                return ActionConditionResult.NEGATIVE;
            }
        return ActionConditionResult.POSITIVE;
    }

    @Override
    public void startedHolding(World world, LivingEntity user, INonStandPower power, ActionTarget target, boolean requirementsFulfilled) {
        if (requirementsFulfilled && world.isClientSide()) {
        	ClientTickingSoundsHelper.playStoppableEntitySound(user, ModSounds.HAMON_OVERDRIVE.get(), 
                    1.75F, 0.9F, false, entity -> power.getHeldAction() == this);
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
        if (!world.isClientSide() && target.getType() == TargetType.ENTITY) {
        	if (target != null && target.getEntity() instanceof PlayerEntity) {
        		HamonData hamon = power.getTypeSpecificData(ModPowers.HAMON.get()).get();
                HamonData receiverHamon = INonStandPower.getPlayerNonStandPower((PlayerEntity)target.getEntity()).getTypeSpecificData(ModPowers.HAMON.get()).get();
                HamonData giverHamon = INonStandPower.getPlayerNonStandPower((PlayerEntity)user.getEntity()).getTypeSpecificData(ModPowers.HAMON.get()).get();
                PlayerEntity receiver = (PlayerEntity) target.getEntity();
                if (receiverHamon.characterIs(ModHamonSkills.CHARACTER_JONATHAN.get())) {
                    JojoModUtil.sayVoiceLine(receiver, ModSounds.JONATHAN_DEEP_PASS_REACTION.get());
                }
                receiverHamon.setHamonStatPoints(HamonStat.STRENGTH, 
                        receiverHamon.getHamonStrengthPoints() + hamon.getHamonStrengthPoints(), true, false);
                receiverHamon.setHamonStatPoints(HamonStat.CONTROL, 
                        receiverHamon.getHamonControlPoints() + hamon.getHamonControlPoints(), true, false);
                giverHamon.setHamonStatPoints(HamonStat.STRENGTH, 0, true, true);
                giverHamon.setHamonStatPoints(HamonStat.CONTROL, 0, true, true);
                receiver.setHealth(receiver.getMaxHealth());
                if (receiver instanceof ServerPlayerEntity) {
                    ModCriteriaTriggers.LAST_HAMON.get().trigger((ServerPlayerEntity) receiver, user);
                }
                HamonUtil.createHamonSparkParticlesEmitter(receiver, 1.0F);
            }
        }
    }
    
    @Override
    public boolean cancelHeldOnGettingAttacked(INonStandPower power, DamageSource dmgSource, float dmgAmount) {
        return true;
    }
    
    @Override
    public boolean clHeldStartAnim(PlayerEntity user) {
        return ModPlayerAnimations.hamonHealing.setAnimEnabled(user, true);
    }
    
    @Override
    public void clHeldStopAnim(PlayerEntity user) {
        ModPlayerAnimations.hamonHealing.setAnimEnabled(user, false);
    }
}
