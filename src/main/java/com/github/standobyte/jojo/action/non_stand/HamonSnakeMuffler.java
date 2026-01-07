package com.github.standobyte.jojo.action.non_stand;

import java.util.HashSet;
import java.util.List;
import java.util.OptionalInt;
import java.util.Set;
import java.util.UUID;

import com.github.standobyte.jojo.action.ActionConditionResult;
import com.github.standobyte.jojo.action.ActionTarget;
import com.github.standobyte.jojo.action.player.ContinuousActionInstance;
import com.github.standobyte.jojo.action.player.IPlayerAction;
import com.github.standobyte.jojo.capability.entity.EntityUtilCapProvider;
import com.github.standobyte.jojo.capability.entity.PlayerUtilCap;
import com.github.standobyte.jojo.client.ClientUtil;
import com.github.standobyte.jojo.client.playeranim.anim.ModPlayerAnimations;
import com.github.standobyte.jojo.client.sound.ClientTickingSoundsHelper;
import com.github.standobyte.jojo.client.sound.HamonSparksLoopSound;
import com.github.standobyte.jojo.entity.damaging.projectile.ownerbound.SnakeMufflerEntity;
import com.github.standobyte.jojo.init.ModItems;
import com.github.standobyte.jojo.init.ModSounds;
import com.github.standobyte.jojo.init.power.non_stand.ModPowers;
import com.github.standobyte.jojo.power.impl.nonstand.INonStandPower;
import com.github.standobyte.jojo.power.impl.nonstand.type.hamon.HamonData;
import com.github.standobyte.jojo.power.impl.nonstand.type.hamon.HamonPowerType;
import com.github.standobyte.jojo.power.impl.nonstand.type.hamon.skill.BaseHamonSkill.HamonStat;
import com.github.standobyte.jojo.power.impl.stand.StandUtil;
import com.github.standobyte.jojo.util.general.MathUtil;
import com.github.standobyte.jojo.util.mc.MCUtil;
import com.github.standobyte.jojo.util.mc.damage.DamageUtil;

import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.ai.attributes.Attributes;
import net.minecraft.entity.item.ArmorStandEntity;
import net.minecraft.entity.passive.GolemEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.projectile.ProjectileEntity;
import net.minecraft.inventory.EquipmentSlotType;
import net.minecraft.util.DamageSource;
import net.minecraft.util.EntityDamageSource;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.vector.Vector3d;
import net.minecraft.world.World;

public class HamonSnakeMuffler extends HamonAction implements IPlayerAction<HamonSnakeMuffler.Instance, INonStandPower> {

    public HamonSnakeMuffler(HamonAction.Builder builder) {
        super(builder);
    }
    
    @Override
    protected ActionConditionResult checkSpecificConditions(LivingEntity user, INonStandPower power, ActionTarget target) {
            if (user.getItemBySlot(EquipmentSlotType.HEAD).getItem() != ModItems.SATIPOROJA_SCARF.get()) {
                return conditionMessage("scarf");
            }
        return ActionConditionResult.noMessage(user.isOnGround());
    }

    private static final OptionalInt COLOR = OptionalInt.of(HamonPowerType.COLOR);
    @Override
    protected void perform(World world, LivingEntity user, INonStandPower power, ActionTarget target) {
        if (user instanceof PlayerEntity) {
        	HamonData hamon = power.getTypeSpecificData(ModPowers.HAMON.get()).get();
        	double controlRatio = (double) hamon.getHamonControlLevel() / (double) HamonData.MAX_STAT_LEVEL * hamon.getActionEfficiency(this.getEnergyCost(power, target), false, getUnlockingSkill());
            double radius = (double) 40 * (controlRatio * 0.8D + 0.2D);
            double maxRadius = 8D + controlRatio * 24D;
        	List<LivingEntity> entitiesAround = MCUtil.entitiesAround(LivingEntity.class, 
                    user, Math.min(radius, maxRadius), false, 
                    entity -> StandUtil.getStandUser(entity) != user &&
                    !(entity instanceof GolemEntity || entity instanceof ArmorStandEntity));
            if (!user.level.isClientSide()) {
                SnakeMufflerEntity snakeMuffler = new SnakeMufflerEntity(user.level, user);
                user.level.addFreshEntity(snakeMuffler);
                snakeMuffler.attachToBlockPos(user.blockPosition());
                user.setOnGround(false);
                setPlayerAction(user, power);
            }
            else {
            	if (user == ClientUtil.getClientPlayer()) {
                    entitiesAround.forEach(entity -> entity.getCapability(EntityUtilCapProvider.CAPABILITY).ifPresent(
                            cap -> cap.setClGlowingColor(COLOR, 100)));
                }
                user.setOnGround(false);
                user.hasImpulse = true;
                Vector3d leap = Vector3d.directionFromRotation(MathHelper.clamp(user.xRot, -45F, -18F), user.yRot)
                        .scale(1 + user.getAttributeValue(Attributes.MOVEMENT_SPEED));
                user.setDeltaMovement(leap.x/5, leap.y + 0.5, leap.z/5);
                world.playSound(ClientUtil.getClientPlayer(), user.getX(), user.getY(), user.getZ(), ModSounds.HAMON_SPARK.get(), 
                        SoundCategory.AMBIENT, 0.9F, 1.0F);
                world.playSound(ClientUtil.getClientPlayer(), user.getX(), user.getY(), user.getZ(), ModSounds.HAMON_CONCENTRATION.get(), 
                        SoundCategory.AMBIENT, 0.9F, 1.0F);
                ClientTickingSoundsHelper.playStoppableEntitySound(user, ModSounds.HAMON_SYO_CHARGE.get(), 
                        1.1F, 1.0F, false, entity -> ContinuousActionInstance.getCurrentAction(user).map(
                                playerAction -> playerAction.isStopped()).orElse(false), Instance.USUAL_SNAKE_MUFFLER_DURATION);
            }
        }
    }
    
    private static boolean dealPhysicalDamage(LivingEntity user, Entity target) {
        return target.hurt(new EntityDamageSource(user instanceof PlayerEntity ? "player" : "mob", user), 
                DamageUtil.getDamageWithoutHeldItem(user));
    }
    
    public static AxisAlignedBB kickHitbox(LivingEntity user) {
        float xzAngle = -user.yRot * MathUtil.DEG_TO_RAD;
        Vector3d lookVec = new Vector3d(Math.sin(xzAngle), 0, Math.cos(xzAngle));
        Vector3d hitboxXZCenter = user.position().add(lookVec.scale(user.getBbWidth() * 0.75F));
        return new AxisAlignedBB(hitboxXZCenter, hitboxXZCenter)
                .inflate(user.getBbWidth() * 1.25F, 0.125, user.getBbWidth() * 1.25F)
                .expandTowards(0, user.getBbHeight() / 2, 0);
    }
    
    @Override
    public Instance createContinuousActionInstance(
            LivingEntity user, PlayerUtilCap userCap, INonStandPower power) {
        if (user.level.isClientSide() && user instanceof PlayerEntity) {
            ModPlayerAnimations.snakeMuffler.setAnimEnabled((PlayerEntity) user, true);
        }
        Instance snakeMuffler = new Instance(user, userCap, power, this);
        
        float energyCost = Math.min(getEnergyCost(power, ActionTarget.EMPTY), power.getEnergy());
        float efficiency = power.getTypeSpecificData(ModPowers.HAMON.get()).get().getActionEfficiency(energyCost, true, getUnlockingSkill());
        snakeMuffler.setEnergySpent(energyCost * efficiency);
        
        return snakeMuffler;
    }
    
    
    
    public static class Instance extends ContinuousActionInstance<HamonSnakeMuffler, INonStandPower> {
        private int positionWaitingTimer = 0;
        private boolean gavePoints = false;
        private float energySpent;
        private final float initialYRot;
        private Set<UUID> damagedEntities = new HashSet<>();

        public Instance(LivingEntity user, PlayerUtilCap userCap, 
                INonStandPower playerPower, HamonSnakeMuffler action) {
            super(user, userCap, playerPower, action);
            this.initialYRot = user.yRot;
        }
        
        public void setEnergySpent(float energy) {
            this.energySpent = energy;
        }
        
        public float getInitialYRot() {
            return initialYRot;
        }
        
        @Override
        public boolean cancelIncomingDamage(DamageSource dmgSource, float dmgAmount) {
        	return true;
        }

        private static final int USUAL_SNAKE_MUFFLER_DURATION = 40;
        @Override
        public void playerTick() {
            LivingEntity user = getUser();
            if(getTick() > 10) {
	            if (!user.level.isClientSide()) {
	                if (positionWaitingTimer >= 0) {
	                    // FIXME ! (hamon 2) check if the client sent position
	                    boolean clientSentPosition = true;
	                    if (clientSentPosition) {
	                        positionWaitingTimer = -1;
	                    }
	                    else {
	                        positionWaitingTimer++;
	                    }
	                }
	                if (positionWaitingTimer < 0 && (user.isOnGround() || !user.level.getFluidState(user.blockPosition()).isEmpty())
	                        || positionWaitingTimer >= USUAL_SNAKE_MUFFLER_DURATION) {
	                    stopAction();
	                    playerPower.setCooldownTimer(getAction(), 60);
	                    return;
	                }
	                
	                List<LivingEntity> targets = user.level.getEntitiesOfClass(LivingEntity.class, kickHitbox(user), 
	                        entity -> !entity.is(user) && user.canAttack(entity));
	                boolean points = false;
	                for (LivingEntity target : targets) {
	                    if (damagedEntities.add(target.getUUID())) {
	                        boolean kickDamage = dealPhysicalDamage(user, target);
	                        boolean hamonDamage = DamageUtil.dealHamonDamage(target, 3.0F, user, null);
	                        if (kickDamage || hamonDamage) {
	                            Vector3d vecToTarget = target.position().subtract(user.position());
	                            boolean left = MathHelper.wrapDegrees(
	                                    user.yBodyRot - MathUtil.yRotDegFromVec(vecToTarget))
	                                    < 0;
	                            float knockbackYRot = (60F + user.getRandom().nextFloat() * 30F) * (left ? 1 : -1);
	                            knockbackYRot += (float) -MathHelper.atan2(vecToTarget.x, vecToTarget.z) * MathUtil.RAD_TO_DEG;
	                            DamageUtil.knockback((LivingEntity) target, 0.75F, knockbackYRot);
	                            
	                            if (hamonDamage) {
	                                points = true;
	                            }
	                        }
	                    }
	                }
	
	                if (!gavePoints && points) {
	                    INonStandPower.getNonStandPowerOptional(user).ifPresent(power -> {
	                        power.getTypeSpecificData(ModPowers.HAMON.get()).ifPresent(hamon -> {
	                            hamon.hamonPointsFromAction(HamonStat.STRENGTH, energySpent); 
	                        });
	                    });
	                    gavePoints = true;
	                }
	            }
	            
	            else {
	                HamonSparksLoopSound.playSparkSound(user, new Vector3d(user.getX(), user.getY(0.25), user.getZ()), 1.0F, true);
	            }
            }
	            user.fallDistance = 0;
	        }
        
        @Override
        public void onStop() {
            super.onStop();
            if (user.level.isClientSide() && user instanceof PlayerEntity) {
                ModPlayerAnimations.snakeMuffler.setAnimEnabled((PlayerEntity) user, false);
            }
        }
        
    }
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
    /*public static boolean snakeMuffler(LivingEntity target, DamageSource dmgSource, float dmgAmount) {
        if (!target.level.isClientSide() && target.canUpdate() && target.isOnGround()) {
            Entity attacker = dmgSource.getEntity();
            if (attacker != null && dmgSource.getDirectEntity() == attacker && attacker instanceof LivingEntity
                    && target instanceof PlayerEntity && target.getItemBySlot(EquipmentSlotType.HEAD).getItem() == ModItems.SATIPOROJA_SCARF.get()) {
                LivingEntity livingAttacker = (LivingEntity) attacker;
                PlayerEntity playerTarget = (PlayerEntity) target;
                if (!playerTarget.getCooldowns().isOnCooldown(ModItems.SATIPOROJA_SCARF.get())) {
                    INonStandPower power = INonStandPower.getPlayerNonStandPower(playerTarget);
                    float energyCost = 500F;
                    if (power.hasEnergy(energyCost)) {
                        if (power.getTypeSpecificData(ModPowers.HAMON.get()).map(hamon -> {
                            if (hamon.isSkillLearned(ModHamonSkills.SNAKE_MUFFLER.get())) {
                                playerTarget.getCooldowns().addCooldown(ModItems.SATIPOROJA_SCARF.get(), 80);
                                float efficiency = hamon.getActionEfficiency(energyCost, false, ModHamonSkills.SNAKE_MUFFLER.get());
                                if (efficiency == 1 || efficiency >= dmgAmount / target.getMaxHealth()) {
                                    JojoModUtil.sayVoiceLine(target, ModSounds.LISA_LISA_SNAKE_MUFFLER.get());
                                    power.consumeEnergy(energyCost);
                                    DamageUtil.dealHamonDamage(attacker, 0.75F, target, null);
                                    livingAttacker.addEffect(new EffectInstance(Effects.GLOWING, 200));
                                    SnakeMufflerEntity snakeMuffler = new SnakeMufflerEntity(target.level, target);
                                    snakeMuffler.setEntityToJumpOver(attacker);
                                    target.level.addFreshEntity(snakeMuffler);
                                    snakeMuffler.attachToBlockPos(target.blockPosition());
                                    return true;
                                }
                            }
                            return false;
                        }).orElse(false)) return true;
                    }
                }
            }
        }
        return false;
    }*/
}
