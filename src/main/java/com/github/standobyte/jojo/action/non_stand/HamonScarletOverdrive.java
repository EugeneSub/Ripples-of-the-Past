package com.github.standobyte.jojo.action.non_stand;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import javax.annotation.Nullable;

import com.github.standobyte.jojo.action.ActionTarget;
import com.github.standobyte.jojo.action.ActionTarget.TargetType;
import com.github.standobyte.jojo.capability.entity.PlayerUtilCap;
import com.github.standobyte.jojo.client.ClientUtil;
import com.github.standobyte.jojo.client.particle.custom.CustomParticlesHelper;
import com.github.standobyte.jojo.init.ModParticles;
import com.github.standobyte.jojo.init.ModSounds;
import com.github.standobyte.jojo.init.power.non_stand.hamon.ModHamonSkills;
import com.github.standobyte.jojo.power.impl.nonstand.INonStandPower;
import com.github.standobyte.jojo.power.impl.nonstand.type.hamon.HamonData;
import com.github.standobyte.jojo.power.impl.nonstand.type.hamon.skill.BaseHamonSkill.HamonStat;
import com.github.standobyte.jojo.util.mc.damage.DamageUtil;
import com.github.standobyte.jojo.util.mc.damage.KnockbackCollisionImpact;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;

import net.minecraft.block.AbstractFireBlock;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.CampfireBlock;
import net.minecraft.block.TripWireBlock;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.particles.ParticleTypes;
import net.minecraft.state.BooleanProperty;
import net.minecraft.state.Property;
import net.minecraft.state.properties.BlockStateProperties;
import net.minecraft.util.Direction;
import net.minecraft.util.Hand;
import net.minecraft.util.SoundEvents;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.BlockRayTraceResult;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.util.math.vector.Vector3d;
import net.minecraft.world.World;

public class HamonScarletOverdrive extends HamonSunlightYellowOverdrive {
    
    public HamonScarletOverdrive(HamonAction.Builder builder) {
        super(builder);
    }
    
    /*@Override
    protected WindupAttackAnim getPlayerAnim() {
        return ModPlayerAnimations.scarletOverdrive;
    }*/
    
    @Override
    public Instance createContinuousActionInstance(
            LivingEntity user, PlayerUtilCap userCap, INonStandPower power) {
        if (user.level.isClientSide() && user instanceof PlayerEntity) {
            getPlayerAnim().setAttackAnim((PlayerEntity) user);
        }
        return new HamonScarletOverdrive.Instance(user, userCap, power, this, getSpentEnergy(power));
    } 
    
    public static class Instance extends HamonSunlightYellowOverdrive.Instance {

        public Instance(LivingEntity user, PlayerUtilCap userCap, INonStandPower playerPower,
                HamonSunlightYellowOverdrive action, float spentEnergy) {
            super(user, userCap, playerPower, action, spentEnergy);
        }
        
        @Override
        public void playerTick() {
            switch (getTick()) {
            case 1:
                if (user.level.isClientSide()) {
                    user.level.playSound(ClientUtil.getClientPlayer(), user.getX(), user.getEyeY(), user.getZ(), 
                            ModSounds.HAMON_SYO_SWING.get(), user.getSoundSource(), 1.0f, 1.0f);
                    user.swing(Hand.MAIN_HAND, true);
                }
                break;
            case 4:
            	ActionTarget target = playerPower.getMouseTarget();
                if (!user.level.isClientSide()) {
                    if (target.getEntity() instanceof LivingEntity) {
                        performPunch((LivingEntity) target.getEntity());
                        playerPower.consumeEnergy(250F);
                    }
                } else {
                	RayTraceResult mouseTarget = Minecraft.getInstance().hitResult;
                	user.level.playSound(ClientUtil.getClientPlayer(), user.getX(), user.getEyeY(), user.getZ(), 
                    		SoundEvents.BLAZE_SHOOT, user.getSoundSource(), 2.0f, 1.0f);
                	for (int i = 0; i < 30; i++) {
                    	Vector3d particlePos = mouseTarget.getLocation().add(
                    	    	(Math.random() - 0.5) * user.getBbWidth(), 
                    	         Math.random(), 
                    	        (Math.random() - 0.5) * user.getBbWidth());
                    	user.level.addParticle(ParticleTypes.FLAME, particlePos.x, particlePos.y, particlePos.z, 
                    	    	(Math.random() - 0.5) * 0.4, Math.random() * 0.4, (Math.random() - 0.5) * 0.4);
                    	}
                }
                //if (target.getType() == TargetType.BLOCK) {
            		makeLit(user.level, user, playerPower);
                //}
                
                break;
            case 9:
                stopAction();
                break;
            }
        }
        
        @Override
        protected void doHamonAttack(LivingEntity target) {
            float efficiency = userHamon.getActionEfficiency(0, true, getAction().getUnlockingSkill());
            float damage = 2.5F + 5F * energySpentRatio;
            damage *= efficiency;
            int fireSeconds = MathHelper.floor(2 + 8F * (float) userHamon.getHamonStrengthLevel() / (float) HamonData.MAX_STAT_LEVEL * efficiency);
            float hamonDamage = damage;
            if (DamageUtil.dealDamageAndSetOnFire(target, 
                    entity -> DamageUtil.dealHamonDamage(entity, hamonDamage, user, null, attack -> attack.hamonParticle(ModParticles.HAMON_SPARK_RED.get())), 
                    fireSeconds, false)) {
                target.level.playSound(null, target.getX(), target.getEyeY(), target.getZ(), ModSounds.HAMON_SYO_PUNCH.get(), target.getSoundSource(), energySpentRatio, 1.0F);
                userHamon.hamonPointsFromAction(HamonStat.STRENGTH, getActualMaxEnergy(playerPower) * energySpentRatio * efficiency);
                DamageUtil.knockback3d(target, 2f, -5, user.yRot);
                boolean hamonSpread = userHamon.isSkillLearned(ModHamonSkills.HAMON_SPREAD.get());
                float punchDamage = damage;
                KnockbackCollisionImpact.getHandler(target).ifPresent(cap -> {
                    cap.onPunchSetKnockbackImpact(target.getDeltaMovement(), user);
                    if (hamonSpread) {
                        cap.hamonDamage(punchDamage, Math.max(20 * fireSeconds / 2, 20), ModParticles.HAMON_SPARK_RED.get());
                    }
                });
            }
        }
        
        
        protected void makeLit(World world, LivingEntity user, INonStandPower power) {
        	ActionTarget mouseTarget = power.getMouseTarget();
        	if (world.isClientSide()) {
        		mouseTarget = ActionTarget.fromRayTraceResult(Minecraft.getInstance().hitResult);
        	}
        	if(mouseTarget.getType() == TargetType.BLOCK) {
	        	BlockPos blockpos = mouseTarget.getBlockPos();
	        	BlockState blockstate = world.getBlockState(blockpos);
	        	BlockPos blockpos1 = blockpos.relative(mouseTarget.getFace());
	            
	        	if (CampfireBlock.canLight(blockstate)) {
	                world.setBlock(blockpos, blockstate.setValue(BlockStateProperties.LIT, Boolean.valueOf(true)), 11);
	        	} else {
	    	        if (AbstractFireBlock.canBePlacedAt(world, blockpos1, user == null ? Direction.NORTH : user.getDirection())) {
	    	           BlockState blockstate1 = AbstractFireBlock.getState(world, blockpos1);
	    	           if(!user.level.isClientSide()) {
	    	        	   world.setBlock(blockpos1, blockstate1, 11);
	    	           }
	    	        }
	        	}
	        	Block blockType = blockstate.getBlock();
	        	if (blockstate.getBlock() == Blocks.TRIPWIRE || FLAMEABLE_BLOCKS.contains(blockType)) {
	        		createScorchedCobweb(user, blockpos, blockstate, world, (int) (32 * userHamon.getHamonStrengthLevelRatio()), null);
	        	}
        	}
        }
        
        private static final Map<BooleanProperty, Direction> PROPERTY_TO_DIRECTION = ImmutableMap.of(
                TripWireBlock.EAST, Direction.EAST, 
                TripWireBlock.SOUTH, Direction.SOUTH, 
                TripWireBlock.WEST, Direction.WEST, 
                TripWireBlock.NORTH, Direction.NORTH);
        
        private static List<Block> FLAMEABLE_BLOCKS = ImmutableList.of(Blocks.COBWEB, Blocks.GRASS, 
        		Blocks.HAY_BLOCK, Blocks.TALL_GRASS, Blocks.DEAD_BUSH);
        
        private static void createScorchedCobweb(LivingEntity user, BlockPos pos, BlockState blockState, World world, 
                int range, @Nullable Direction from) {
        		Block blockType = blockState.getBlock();
	            if (range > 0 && blockState.getBlock() == Blocks.TRIPWIRE) {
	                Map<Property<?>, Comparable<?>> values = blockState.getValues();
	                List<Direction> directions = new ArrayList<Direction>();
	                for (Map.Entry<BooleanProperty, Direction> entry: PROPERTY_TO_DIRECTION.entrySet()) {
	                    if (entry.getValue() != from && (Boolean) values.get(entry.getKey())) {
	                        directions.add(entry.getValue());
	                    }
	                }
	                //if (!user.level.isClientSide()) {
	                	world.setBlock(pos, Blocks.FIRE.defaultBlockState(), 11);
	                //}
	                for (Direction direction : directions) {
	                    BlockPos nextPos = pos.relative(direction);
	                    createScorchedCobweb(user, nextPos, world.getBlockState(nextPos), world, 
	                            range - 1, direction.getOpposite());
	                }
	            } else if (range > 0 && FLAMEABLE_BLOCKS.contains(blockType)) {
	            	//if (!user.level.isClientSide()) {
                        world.setBlock(pos, Blocks.FIRE.defaultBlockState(), 11);
                    //}
	            	for (Direction direction : Direction.values()) {
	                    BlockPos nextPos = pos.relative(direction);
	                    BlockState nextState = world.getBlockState(pos.relative(direction));
	                    if (FLAMEABLE_BLOCKS.contains(nextState.getBlock())) {
	                        createScorchedCobweb(user, nextPos, nextState, world, --range, null);
	                    }
	                }
	            }
        	
            if (user.level.isClientSide()) {
            	for (int i = 0; i < 5; i++) {
            	    Vector3d particlePos = new Vector3d(pos.getX(), pos.getY(), pos.getZ()).add(
            	    		(Math.random() - 0.5) * user.getBbWidth(), 
            	             Math.random(), 
            	            (Math.random() - 0.5) * user.getBbWidth());
            	    user.level.addParticle(ParticleTypes.FLAME, particlePos.x, particlePos.y, particlePos.z, 
            	    		(Math.random() - 0.5) * 0.4, Math.random() * 0.4, (Math.random() - 0.5) * 0.4);
            	    }
            }
        }
    }
    
}
