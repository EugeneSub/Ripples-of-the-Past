package com.github.standobyte.jojo.entity.damaging.projectile;

import javax.annotation.Nullable;

import com.github.standobyte.jojo.action.ActionTarget.TargetType;
import com.github.standobyte.jojo.client.ClientUtil;
import com.github.standobyte.jojo.init.ModEntityTypes;
import com.github.standobyte.jojo.init.ModParticles;
import com.github.standobyte.jojo.init.ModSounds;
import com.github.standobyte.jojo.init.power.non_stand.ModPowers;
import com.github.standobyte.jojo.power.impl.nonstand.INonStandPower;
import com.github.standobyte.jojo.power.impl.nonstand.type.hamon.skill.BaseHamonSkill.HamonStat;
import com.github.standobyte.jojo.util.mc.damage.DamageUtil;

import net.minecraft.block.BlockState;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntitySize;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.Pose;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.network.PacketBuffer;

import net.minecraft.util.SoundCategory;

import net.minecraft.util.Direction.Axis;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.BlockRayTraceResult;
import net.minecraft.util.math.EntityRayTraceResult;
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.util.math.vector.Vector3d;
import net.minecraft.world.World;

public class HamonSendoOverdriveEntity2 extends ModdedProjectileEntity {
    private float radius;
    private float damage;
    private float points;
    private float sparksCount;
    private boolean gaveHamonPoints;
    private int duration;
    private Vector3d original;
    private boolean isCeiling = false;
    private boolean isVertical = false;
    private boolean isGoingUp = false;
    private boolean isGoingDown = false;
    
    

    public HamonSendoOverdriveEntity2(World world, LivingEntity entity) {
        super(ModEntityTypes.SENDO_HAMON_OVERDRIVE2.get(), entity, world);
    }
    
    public HamonSendoOverdriveEntity2 setRadius(float radius) {
        this.radius = radius;
        this.sparksCount = radius * radius * 9;
        Vector3d pos = getBoundingBox().getCenter();
        refreshDimensions();
        setBoundingBox(new AxisAlignedBB(pos, pos).inflate(radius));
        return this;
    }
    
    public HamonSendoOverdriveEntity2 setDamage(float damage) {
        this.damage = damage;
        return this;
    }
    
    public HamonSendoOverdriveEntity2 setPoints(float points) {
        this.points = points;
        return this;
    }
    
    public HamonSendoOverdriveEntity2 setDuration(int ticks) {
        this.duration = ticks;
        return this;
    }

    public HamonSendoOverdriveEntity2(EntityType<? extends HamonSendoOverdriveEntity2> entityType, World world) {
        super(entityType, world);
    }
    
    @Override
    public void shoot(double x, double y, double z, float velocity, float inaccuracy) {
        setPos(getX(), getY() - radius, getZ());
        super.shoot(x, y, z, velocity, inaccuracy);
    }
    
    @Override
    public void tick() {
        super.tick();
        
        if(this.tickCount < 2) {
        	original = new Vector3d(this.getDeltaMovement().x, 0, this.getDeltaMovement().z);
        } else
        
        if (level.isClientSide()) {
            Vector3d center = getBoundingBox().getCenter();
            int sparksCount = Math.max((int) (this.sparksCount * damageWearOffMultiplier()), 1);
            for (int i = 0; i < sparksCount; i++) {
                Vector3d sparkVec = center.add(new Vector3d(
                        (random.nextDouble() - 0.5), 
                        (random.nextDouble() - 0.5),
                        (random.nextDouble() - 0.5))
                        .normalize().scale(random.nextDouble() * radius));
                
                level.addParticle(ModParticles.HAMON_SPARK.get(), false, sparkVec.x, sparkVec.y, sparkVec.z, 0, 0, 0);
                
            }
            level.playSound(ClientUtil.getClientPlayer(), center.x, center.y, center.z, ModSounds.HAMON_SPARK.get(), 
                    SoundCategory.AMBIENT, Math.min(0.1F + radius * 0.15F, 0.75F), 1.0F + (random.nextFloat() - 0.5F) * 0.15F);
        }
        if(level.isEmptyBlock(this.blockPosition().offset(0, 0, 0))
        		&& level.isEmptyBlock(this.blockPosition().offset(0, 1, 0))
        		&& level.isEmptyBlock(this.blockPosition().offset(0, -1, 0))
        		&& level.isEmptyBlock(this.blockPosition().offset(1, 0, 0))
        		&& level.isEmptyBlock(this.blockPosition().offset(-1, 0, 0))
        		&& level.isEmptyBlock(this.blockPosition().offset(0, 0, 1))
        		&& level.isEmptyBlock(this.blockPosition().offset(0, 0, -1))) {
        	//this.remove();
        	
        	Vector3d sendoEyes = this.getEyePosition(1.0F);
    		Vector3d lookVec = this.getViewVector(1.0F);
    		BlockPos target = new BlockPos(sendoEyes.add(lookVec.scale(1.0)));
        	
        	if(isVertical == true) {
        		if(isGoingUp) {
        			if(!level.isEmptyBlock(target.offset(0, -1, 0))) {
            			moveForth();
            			isCeiling = false;
            		} else {
            			moveBack();
            			isCeiling = false;
            		}
        		} else if (isGoingDown) {
        			if(!level.isEmptyBlock(target.offset(0, 1, 0))) {
            			moveForth();
            		} else {
            			moveBack();
            		}
        		}
        	} else {
        		if(this.getDeltaMovement().y == 0) {
		        	if(isCeiling == true) {
		        		moveUp();
		        	} else if (isCeiling == false) {
		        		moveDown();
		        	}
        		}
        	}
        }
    }


    
    @Override
    protected boolean hurtTarget(Entity target, LivingEntity owner) {
        return DamageUtil.dealHamonDamage(target, getDamageAmount(), 
                this, owner, attack -> attack.hamonParticle(ModParticles.HAMON_SPARK.get()));
    }

    @Override
    protected void afterEntityHit(EntityRayTraceResult entityRayTraceResult, boolean entityHurt) {
        if (entityHurt) {
            Entity target = entityRayTraceResult.getEntity();
            if (target instanceof LivingEntity) {
                DamageUtil.knockback3d((LivingEntity) target, radius * 0.1F, xRot, yRot);
            }
            if (!gaveHamonPoints) {
                INonStandPower.getNonStandPowerOptional(getOwner()).ifPresent(power -> {
                    power.getTypeSpecificData(ModPowers.HAMON.get()).ifPresent(hamon -> {
                        gaveHamonPoints = true;
                        hamon.hamonPointsFromAction(HamonStat.STRENGTH, points);
                    });
                });
            }
        }
    }
    
    @Override
    protected void onHitBlock(BlockRayTraceResult result) {
    	
        if (result.getDirection().getAxis() == Axis.Y || result.getDirection().getAxis() == Axis.X || result.getDirection().getAxis() == Axis.Z) {
        	Vector3d movementVec = getDeltaMovement();
        	
        	if (result.getDirection().getAxis() == Axis.Y) {
        		if(this.getDeltaMovement().y > 0) {
        			isCeiling = true;
        		} else {
        			isCeiling = false;
        		}
	            moveForthStart();
	        }
        	if(result.getDirection().getAxis() == Axis.Y && movementVec.x == 0 && movementVec.z == 0) {
        		
        		Vector3d sendoEyes = this.getEyePosition(1.0F);
        		Vector3d lookVec = this.getViewVector(1.0F);
        		BlockPos target = new BlockPos(sendoEyes.add(lookVec.scale(1.0)));
        		
        		if(!level.isEmptyBlock(target)) {
        			moveBack();
        		} else {
        			moveForth();
        		}
        		
        		if(isGoingUp) {
        			isCeiling = true;
        		} else if (isGoingDown) {
        			isCeiling = false;
        		}
        	}
        	if (result.getDirection().getAxis() == Axis.X || result.getDirection().getAxis() == Axis.Z) {
        		
        		BlockPos pos = this.getOnPos();
    			BlockState state = level.getBlockState(pos);
    			
        		if (this.getDeltaMovement().y == 0) {
        			if(level.isEmptyBlock(this.getOnPos().above())) {
	        			moveUp();
	        		} else {
	        			moveDown();
	        		}
        		} 
        		if(this.getDeltaMovement().y > 0) {
        			moveUp();
        			this.xRot = 0;
        			this.xRotO = 0;
        		} else {
        			moveDown();
        			this.xRot = 0;
        			this.xRotO = 0;
        		}
	        }
        }
    }
    
    public void moveForthStart() {
    	Vector3d movementVec = getDeltaMovement();
    	Vector3d newVec = new Vector3d(movementVec.x, 0, movementVec.z);
        this.setDeltaMovement(newVec);
        isVertical = false;
        this.rotateTowardsMovement(1.0F);
    }
    
    public void moveForth() {
    	this.setDeltaMovement(original);
    	isVertical = false;
    	isGoingUp = false;
    	isGoingDown = false;
        
    }
    
    public void moveBack() {
    	//Vector3d newVec = new Vector3d(original.x, 0, original.z);
		this.setDeltaMovement(original.reverse());
		isVertical = false;
		isGoingUp = false;
    	isGoingDown = false;
    }
    
    public void moveUp() {
    	Vector3d movementVec = getDeltaMovement();
    	Vector3d newVec = new Vector3d(0, movementVec.length(), 0);
		this.setDeltaMovement(newVec);
		isGoingUp = true;
		isVertical = true;
    }
    
    public void moveDown() {
    	Vector3d movementVec = getDeltaMovement();
    	Vector3d newVec = new Vector3d(0, -movementVec.length(), 0);
		this.setDeltaMovement(newVec);
		isGoingDown = true;
		isVertical = true;
    }
    
    @Override
    protected Vector3d getOwnerRelativeOffset() {
        return new Vector3d((float) 0, 0.6F, 0);
    }
    
    @Override
    protected void breakProjectile(TargetType targetType, RayTraceResult hitTarget) {
        if (targetType != TargetType.ENTITY) {
            super.breakProjectile(targetType, hitTarget);
        }
    }
    
    @Override
    public EntitySize getDimensions(Pose pose) {
        EntitySize defaultSize = super.getDimensions(pose);
        return new EntitySize(radius * 2, radius / 2, defaultSize.fixed);
    }
    
    @Override
    public boolean isPickable() {
        return false;
    }

    @Override
    public int ticksLifespan() {
        return duration;
    }

    @Override
    protected float getBaseDamage() {
        return damage;
    }
    
    @Override
    protected float getDamageAmount() {
        return damage * damageWearOffMultiplier();
    }
    
    private float damageWearOffMultiplier() {
        float ageRatio = (float) tickCount / (float) duration;
        return Math.min(2 - ageRatio * 2, 1);
    }

    @Override
    protected float getMaxHardnessBreakable() {
        return 0;
    }

    @Override
    public boolean standDamage() {
        return false;
    }
    
    @Override
    public boolean canBeDeflected(@Nullable Entity context) {
        return false;
    }
    
    @Override
    public boolean canBeEvaded(@Nullable Entity context) {
        return false;
    }

    @Override
    protected void addAdditionalSaveData(CompoundNBT nbt) {
        super.addAdditionalSaveData(nbt);
        nbt.putFloat("Radius", radius);
        nbt.putBoolean("PointsGiven", gaveHamonPoints);
        nbt.putFloat("Damage", damage);
        nbt.putFloat("Points", points);
        nbt.putInt("Duration", duration);
    }

    @Override
    protected void readAdditionalSaveData(CompoundNBT nbt) {
        super.readAdditionalSaveData(nbt);
        setRadius(nbt.getFloat("Radius"));
        gaveHamonPoints = nbt.getBoolean("PointsGiven");
        damage = nbt.getFloat("Damage");
        points = nbt.getFloat("Points");
        duration = nbt.getInt("Duration");
    }
    
    @Override
    public void writeSpawnData(PacketBuffer buffer) {
        super.writeSpawnData(buffer);
        buffer.writeFloat(radius);
        buffer.writeVarInt(duration);
    }

    @Override
    public void readSpawnData(PacketBuffer additionalData) {
        super.readSpawnData(additionalData);
        setRadius(additionalData.readFloat());
        setDuration(additionalData.readVarInt());
    }
}
