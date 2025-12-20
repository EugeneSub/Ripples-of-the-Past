package com.github.standobyte.jojo.action.non_stand;

import java.util.List;
import java.util.Random;

import javax.annotation.Nullable;

import com.github.standobyte.jojo.action.Action;
import com.github.standobyte.jojo.action.ActionTarget;
import com.github.standobyte.jojo.action.ActionTarget.TargetType;
import com.github.standobyte.jojo.client.ClientUtil;
import com.github.standobyte.jojo.client.particle.custom.CustomParticlesHelper;
import com.github.standobyte.jojo.client.sound.ClientTickingSoundsHelper;
import com.github.standobyte.jojo.entity.HamonSendoOverdriveEntity;
import com.github.standobyte.jojo.entity.damaging.projectile.HamonSendoOverdriveEntity2;
import com.github.standobyte.jojo.entity.damaging.projectile.HamonTurquoiseBlueOverdriveEntity;
import com.github.standobyte.jojo.init.ModParticles;
import com.github.standobyte.jojo.init.ModSounds;
import com.github.standobyte.jojo.init.power.non_stand.ModPowers;
import com.github.standobyte.jojo.init.power.non_stand.hamon.ModHamonActions;
import com.github.standobyte.jojo.init.power.non_stand.hamon.ModHamonSkills;
import com.github.standobyte.jojo.power.impl.nonstand.INonStandPower;
import com.github.standobyte.jojo.power.impl.nonstand.type.hamon.HamonData;
import com.github.standobyte.jojo.power.impl.nonstand.type.hamon.HamonPowerType;
import com.github.standobyte.jojo.power.impl.nonstand.type.hamon.skill.BaseHamonSkill.HamonStat;
import com.github.standobyte.jojo.util.general.GeneralUtil;
import com.github.standobyte.jojo.util.general.LazySupplier;
import com.github.standobyte.jojo.util.general.ObjectWrapper;
import com.github.standobyte.jojo.util.mc.damage.DamageUtil;

import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.MoverType;
import net.minecraft.util.Direction;
import net.minecraft.util.EntityPredicates;
import net.minecraft.util.Hand;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.BlockRayTraceResult;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.RayTraceContext;
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.util.math.vector.Vector3d;
import net.minecraft.world.World;

public class HamonSendoOverdrive extends HamonAction {

    public HamonSendoOverdrive(HamonAction.Builder builder) {
        super(builder);
    }
    
    @Override
    protected Action<INonStandPower> replaceAction(INonStandPower power, ActionTarget target) {
        if (GeneralUtil.orElseFalse(power.getTypeSpecificData(ModPowers.HAMON.get()), hamon -> {
            return hamon.isSkillLearned(ModHamonSkills.TURQUOISE_BLUE_OVERDRIVE.get());
        })) {
            if (power.getUser().isInWaterOrBubble()) {
                return ModHamonActions.HAMON_TURQUOISE_BLUE_OVERDRIVE.get();
            }
        }
        return super.replaceAction(power, target);
    }
    
    @Override
    public void startedHolding(World world, LivingEntity user, INonStandPower power, ActionTarget target, boolean requirementsFulfilled) {
        if (requirementsFulfilled && world.isClientSide()) {
        	ClientTickingSoundsHelper.playStoppableEntitySound(user, ModSounds.HAMON_CHANNEL.get(), 
                    1.0F, 1.0F, false, entity -> power.getHeldAction() == this);
        }
    }
    
    @Override
    public void onHoldTickClientEffect(LivingEntity user, INonStandPower power, int ticksHeld, boolean reqFulfilled, boolean reqStateChanged) {
        if (reqFulfilled) {
        	if (user.level.isClientSide()) {
                boolean isUserTheCameraEntity = user == ClientUtil.getCameraEntity();
                if (isUserTheCameraEntity) {
                    CustomParticlesHelper.summonHamonAuraParticlesFirstPerson(ModParticles.HAMON_AURA.get(), user, 6 / 5);
                }
            }
        }
    }
    
    @Override
    public void overrideVanillaMouseTarget(ObjectWrapper<ActionTarget> targetContainer, World world, LivingEntity user, INonStandPower power) {
        ActionTarget target = targetContainer.get();
        if (target.getType() == TargetType.BLOCK) {
            BlockPos blockPos = target.getBlockPos();
            BlockState blockState = world.getBlockState(blockPos);
            if (blockState.getCollisionShape(world, blockPos).isEmpty()) {
                Vector3d pos1 = user.getEyePosition(1.0F);
                Vector3d pos2 = pos1.add(user.getViewVector(1.0F).scale(Math.sqrt(getMaxRangeSqBlockTarget())));
                RayTraceResult targetCollisionBlocks = user.level.clip(new RayTraceContext(
                        pos1, pos2, RayTraceContext.BlockMode.COLLIDER, RayTraceContext.FluidMode.NONE, user)); // to not target plant blocks like grass
                targetContainer.set(ActionTarget.fromRayTraceResult(targetCollisionBlocks));
            }
        }
    }
    
    private final LazySupplier<ResourceLocation> altTex = 
            new LazySupplier<>(() -> makeIconVariant(this, "_alt"));
    
    private final LazySupplier<ResourceLocation> silverTex = 
            new LazySupplier<>(() -> makeIconVariant(this, "_silver"));
    
 
    @Override
    public ResourceLocation getIconTexturePath(@Nullable INonStandPower power) {
    	
    	RayTraceResult mouseTarget = Minecraft.getInstance().hitResult;
    	if(mouseTarget != null && mouseTarget.getType() == RayTraceResult.Type.BLOCK) {
    		BlockPos target = ((BlockRayTraceResult) mouseTarget).getBlockPos();
    		BlockState blockState = power.getUser().level.getBlockState(target).getBlockState();
	        if (power.getUser().isShiftKeyDown()) {
	            return altTex.get();
	        } else if (power.getTypeSpecificData(ModPowers.HAMON.get()).get().isSkillLearned(ModHamonSkills.METAL_SILVER_OVERDRIVE.get())
	        		&& (blockState.is(Blocks.IRON_BLOCK)
	        		|| blockState.is(Blocks.IRON_BARS)
	        		|| blockState.is(Blocks.IRON_DOOR)
	        		|| blockState.is(Blocks.IRON_TRAPDOOR)
	        		|| blockState.is(Blocks.GOLD_BLOCK)
	        		|| blockState.is(Blocks.NETHERITE_BLOCK))) {
	        	return silverTex.get();
	        }
	        else {
	            return super.getIconTexturePath(power);
	        }
    	}
    	return super.getIconTexturePath(power);
    }
    
    
    
    @Override
    protected void perform(World world, LivingEntity user, INonStandPower power, ActionTarget target) {
        if (target.getType() == TargetType.BLOCK) {
        	BlockState blockState = world.getBlockState(target.getBlockPos());
            if (!world.isClientSide()) {
                BlockPos blockPos = target.getBlockPos();
                Direction face = target.getFace();
            	HamonData hamon = power.getTypeSpecificData(ModPowers.HAMON.get()).get();
                float energyCost = getEnergyCost(power, target);
                float hamonEfficiency = hamon.getActionEfficiency(energyCost, false, getUnlockingSkill());
                float hamonControl = hamon.getHamonControlLevelRatio();
                
                if(user.isShiftKeyDown()) {
                    double diameter = 2 + (double) (hamon.getHamonStrengthLevel() * 4.5) / (double) HamonData.MAX_STAT_LEVEL
                            * hamon.getBloodstreamEfficiency();
                    double radiusMinus1 = (diameter - 1) / 2;
                    AxisAlignedBB aabb = new AxisAlignedBB(blockPos).inflate(radiusMinus1).expandTowards(Vector3d.atLowerCornerOf(face.getNormal()))
                            .move(Vector3d.atLowerCornerOf(face.getNormal()).scale(-radiusMinus1));
                    Random random = user.getRandom();
                    int sparksCount = Math.max(MathHelper.floor(diameter * diameter * diameter / 8), 1);
                    for (int i = 0; i < sparksCount; i++) {
                    	CustomParticlesHelper.createHamonSparkParticles(null, 
                                aabb.minX + random.nextDouble() * (aabb.maxX - aabb.minX), 
                                aabb.minY + random.nextDouble() * (aabb.maxY - aabb.minY), 
                                aabb.minZ + random.nextDouble() * (aabb.maxZ - aabb.minZ), 
                                20);
                    }
                    world.playSound(ClientUtil.getClientPlayer(), user.getX(), user.getY(), user.getZ(), ModSounds.HAMON_SPARKS_LONG.get(), 
                            SoundCategory.AMBIENT, 0.75F, 1.0F);
                    List<Entity> entities = world.getEntitiesOfClass(LivingEntity.class, aabb, EntityPredicates.NO_CREATIVE_OR_SPECTATOR);
                    boolean givePoints = false;
                    for (Entity entity : entities) {
                        if (!entity.is(user) && DamageUtil.dealHamonDamage(entity, 2.0F, user, null)) {
                            givePoints = true;
                        }
                    }
                    if (givePoints) {
                        hamon.hamonPointsFromAction(HamonStat.STRENGTH, getEnergyCost(power, target));
                    }
                } else if ((blockState.is(Blocks.IRON_BLOCK) 
                		|| blockState.is(Blocks.IRON_BARS)
                		|| blockState.is(Blocks.IRON_DOOR)
                		|| blockState.is(Blocks.IRON_TRAPDOOR)
                		|| blockState.is(Blocks.GOLD_BLOCK)
                		|| blockState.is(Blocks.CHAIN)
                		|| blockState.is(Blocks.NETHERITE_BLOCK)) && hamon.isSkillLearned(ModHamonSkills.METAL_SILVER_OVERDRIVE.get())) {
                	
                	HamonSendoOverdriveEntity sendoOverdrive = new HamonSendoOverdriveEntity(world, user, face.getAxis());
                	float heldRatio = MathHelper.clamp((float) (power.getHeldActionTicks() - 1) / this.getHoldDurationToFire(power), 0, 1);
                	sendoOverdrive.yRot = user.yRot;
                	sendoOverdrive.xRot = user.xRot;
                	sendoOverdrive.sparksAngle = (float) Math.PI / 4 + heldRatio * (float) Math.PI / 4 * 7;
                	sendoOverdrive.radius = (3 + hamon.getHamonControlLevelRatio() * 3) * hamonEfficiency;
                	sendoOverdrive.damage = 0.9F * hamonEfficiency;
                	sendoOverdrive.setWavesCount(2 + (int) ((2 + Math.min(hamon.getHamonControlLevelRatio() * 3, 2)) * hamonEfficiency));
                	sendoOverdrive.setStatPoints(Math.min(energyCost, power.getEnergy()) * hamonEfficiency);
                	        
                	sendoOverdrive.moveTo(Vector3d.atCenterOf(blockPos).subtract(0, sendoOverdrive.getDimensions(null).height * 0.5, 0));
                	sendoOverdrive.setBlockTarget(target.getBlockPos(), target.getFace());
                	world.addFreshEntity(sendoOverdrive);
                }
                else {
	                HamonSendoOverdriveEntity2 overdriveWave = new HamonSendoOverdriveEntity2(world, user)
	                        .setRadius(0.6F + (float) (0.5F * hamonControl * hamonEfficiency))
	                        .setDamage(2F * hamonEfficiency)
	                        .setPoints(Math.min(energyCost, power.getEnergy()) * hamonEfficiency)
	                        .setDuration(30 + (int) (20 * hamonControl));
	                overdriveWave.shootFromRotation(user, 0.5F, 0);
	                world.addFreshEntity(overdriveWave);
	                
	                power.consumeEnergy(energyCost);
                }
	                
            }
            user.swing(Hand.MAIN_HAND, false);
        }
    }
}