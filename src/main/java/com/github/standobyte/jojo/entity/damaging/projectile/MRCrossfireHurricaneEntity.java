package com.github.standobyte.jojo.entity.damaging.projectile;

import java.util.List;

import javax.annotation.Nullable;

import com.github.standobyte.jojo.init.ModActions;
import com.github.standobyte.jojo.init.ModEntityTypes;
import com.github.standobyte.jojo.power.stand.IStandPower;
import com.github.standobyte.jojo.util.JojoModUtil;
import com.github.standobyte.jojo.util.damage.IndirectStandEntityDamageSource;
import com.github.standobyte.jojo.util.damage.DamageUtil;
import com.github.standobyte.jojo.util.damage.StandEntityDamageSource;

import net.minecraft.block.AbstractFireBlock;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityClassification;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.network.PacketBuffer;
import net.minecraft.util.DamageSource;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.BlockRayTraceResult;
import net.minecraft.util.math.EntityRayTraceResult;
import net.minecraft.util.math.vector.Vector3d;
import net.minecraft.world.Explosion;
import net.minecraft.world.World;
import net.minecraft.world.server.ServerWorld;
import net.minecraftforge.event.ForgeEventFactory;

public class MRCrossfireHurricaneEntity extends ModdedProjectileEntity {
    private boolean small;
    private Vector3d targetPos;
    @Nullable
    private IStandPower userStandPower;
    
    public MRCrossfireHurricaneEntity(boolean small, LivingEntity shooter, World world, IStandPower standPower) {
        super(small ? ModEntityTypes.MR_CROSSFIRE_HURRICANE_SPECIAL.get() : ModEntityTypes.MR_CROSSFIRE_HURRICANE.get(), shooter, world);
        this.small = small;
        userStandPower = standPower;
    }

    public MRCrossfireHurricaneEntity(EntityType<? extends MRCrossfireHurricaneEntity> type, World world) {
        super(type, world);
    }
    
    public void setSpecial(Vector3d targetPos) {
        this.targetPos = targetPos;
    }

    @Override
    protected void moveProjectile() {
        super.moveProjectile();
        if (targetPos != null) {
            double velocitySqr = getDeltaMovement().lengthSqr();
            if (velocitySqr > 0) {
                Vector3d targetVec = targetPos.subtract(position());
                double targetDistSqr = targetVec.lengthSqr();
                if (velocitySqr < targetDistSqr) {
                    Vector3d vec = getDeltaMovement().scale(targetDistSqr / velocitySqr);
                    setDeltaMovement(vec.add(targetVec).normalize().scale(Math.sqrt(velocitySqr)));
                }
                else if (!level.isClientSide()) {
                    explode();
                }
            }
        }
    }
    
    @Override
    public boolean standDamage() {
        return true;
    }

    @Override
    public void tick() {
        if (isInWaterOrRain()) {
            clearFire();
        }
        else {
            super.tick();
        }
    }
    
    @Override
    public void clearFire() {
        super.clearFire();
        if (!level.isClientSide()) {
            JojoModUtil.extinguishFieryStandEntity(this, (ServerWorld) level);
        }
    }
    
    @Override
    public float getBaseDamage() {
        return small ? 2.0F : 6.0F;
    }
    
    @Override
    protected float getMaxHardnessBreakable() {
        return 5.0F;
    }
    
    @Override
    protected int ticksLifespan() {
        return 100;
    }
    
    @Override
    protected boolean canHitOwner() {
        return true;
    }
    
    @Override
    protected DamageSource getDamageSource(LivingEntity owner) {
        return super.getDamageSource(owner).setIsFire();
    }
    
    @Override
    public boolean isInvulnerableTo(DamageSource dmgSource) {
        return dmgSource.isExplosion() || super.isInvulnerableTo(dmgSource);
    }
    
    @Override
    protected void afterBlockHit(BlockRayTraceResult blockRayTraceResult, boolean brokenBlock) {
        explode();
    }
    
    @Override
    protected void afterEntityHit(EntityRayTraceResult entityRayTraceResult, boolean entityHurt) {
        explode();
    }
    
    private void explode() {
        if (!level.isClientSide) {
            StandEntityDamageSource dmgSource = new IndirectStandEntityDamageSource("explosion.stand", this, getOwner());
            if (small) {
                dmgSource.setBypassInvulTicksInEvent();
            }
            level.explode(this, dmgSource.setExplosion(), null, getX(), getY(), getZ(), small ? 0.5F : 3.0F, false, Explosion.Mode.NONE);
        }
    }
    
    public void onExplode(List<Entity> affectedEntities, List<BlockPos> affectedBlocks) {
        LivingEntity magiciansRed = getOwner();
        for (Entity entity : affectedEntities) {
            if (!entity.is(magiciansRed)) {
                DamageUtil.setOnFire(entity, 10, true);
                if (!level.isClientSide() && userStandPower != null && 
                        (entity.getClassification(false) == EntityClassification.MONSTER || entity.getType() == EntityType.PLAYER)) {
                    userStandPower.addLearningProgressPoints(ModActions.MAGICIANS_RED_CROSSFIRE_HURRICANE.get(), 0.0625F);
                }
            }
        }
        if (magiciansRed != null && ForgeEventFactory.getMobGriefingEvent(level, magiciansRed)) {
            for (BlockPos pos : affectedBlocks) {
                if (level.isEmptyBlock(pos)) {
                    level.setBlockAndUpdate(pos, AbstractFireBlock.getState(level, pos));
                }
            }
        }
    }

    @Override
    public void writeSpawnData(PacketBuffer buffer) {
        super.writeSpawnData(buffer);
        buffer.writeBoolean(targetPos != null);
        if (targetPos != null) {
            buffer.writeDouble(targetPos.x);
            buffer.writeDouble(targetPos.y);
            buffer.writeDouble(targetPos.z);
        }
    }

    @Override
    public void readSpawnData(PacketBuffer additionalData) {
        super.readSpawnData(additionalData);
        if (additionalData.readBoolean()) {
            targetPos = new Vector3d(additionalData.readDouble(), additionalData.readDouble(), additionalData.readDouble());
        }
    }
}
