package com.github.standobyte.jojo.action.stand.effect;

import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;
import java.util.UUID;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.apache.commons.lang3.mutable.MutableInt;

import com.github.standobyte.jojo.init.ModStandEffects;
import com.github.standobyte.jojo.network.PacketManager;
import com.github.standobyte.jojo.network.packets.fromserver.TrStandEffectPacket;
import com.github.standobyte.jojo.power.stand.IStandPower;
import com.github.standobyte.jojo.util.utils.JojoModUtil;

import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.network.PacketBuffer;
import net.minecraft.util.ResourceLocation;
import net.minecraft.world.server.ServerWorld;

public abstract class StandEffectInstance {
    @Nonnull public final StandEffectType<?> effectType;
    protected LivingEntity user;
    protected IStandPower userPower;
    // FIXME !!!!!!!!!!!!!!!!!!!!!!!! map<UUID, LivingEntity>
    protected final Set<LivingEntity> targets = new HashSet<LivingEntity>();
    @Nullable private Set<UUID> targetsLoaded;
    private int id;
    public int tickCount = 0;
    private boolean toBeRemoved = false;
    
    public StandEffectInstance(@Nonnull StandEffectType<?> effectType) {
        this.effectType = effectType;
    }
    
    public StandEffectInstance withUser(LivingEntity user) {
        this.user = user;
        this.userPower = IStandPower.getStandPowerOptional(user).orElse(null);
        return this;
    }
    
    public StandEffectInstance withStand(IStandPower stand) {
        this.user = stand.getUser();
        this.userPower = stand;
        return this;
    }
    
    public StandEffectInstance withId(int id) {
        this.id = id;
        return this;
    }
    
    public StandEffectInstance addTarget(LivingEntity target) {
        this.targets.add(target);
        return this;
    }
    
    public LivingEntity getStandUser() {
        return user;
    }
    
    public Collection<LivingEntity> getTargets() {
        return Collections.unmodifiableCollection(targets);
    }
    
    public void onStart() {
        start();
    }
    
    public void onTick() {
        tickCount++;
        
        Iterator<LivingEntity> it = targets.iterator();
        while (it.hasNext()) {
            LivingEntity target = it.next();
            if (target.isDeadOrDying()) {
                if (removeDeadTarget(target)) {
                    it.remove();
                }
            }
            else {
                tickTarget(target);
            }
        }
        tick();
    }
    
    public void onStop() {
        stop();
    }
    
    protected abstract void start();
    protected abstract void tickTarget(LivingEntity target);
    protected abstract void tick();
    protected abstract void stop();
    
    public boolean removeOnUserDeath() {
        return true;
    }
    
    public boolean removeOnUserLogout() {
        return true;
    }
    
    public boolean removeOnStandChanged() {
        return true;
    }
    
    protected boolean removeDeadTarget(LivingEntity target) {
        return true;
    }
    
    public int getId() {
        return id;
    }
    
    public void remove() {
        toBeRemoved = true;
    }
    
    public boolean toBeRemoved() {
        return toBeRemoved;
    }
    
    public void syncWithUserOnly(ServerPlayerEntity user) {}
    
    public void syncWithTrackingAndUser() {
        PacketManager.sendToClientsTrackingAndSelf(TrStandEffectPacket.add(this), user);
    }
    
    public void syncWithTrackingOrUser(ServerPlayerEntity player) {
        PacketManager.sendToClient(TrStandEffectPacket.add(this), player);
    }

    public CompoundNBT toNBT() {
        CompoundNBT nbt = new CompoundNBT();
        nbt.putString("Type", effectType.getRegistryName().toString());
        nbt.putInt("TickCount", tickCount);
        
        CompoundNBT targetsNBT = new CompoundNBT();
        MutableInt i = new MutableInt(0);
        targets.forEach(target -> targetsNBT.putUUID(String.valueOf(i.incrementAndGet()), target.getUUID()));
        nbt.put("Targets", targetsNBT);
        
        writeAdditionalSaveData(nbt);
        return nbt;
    }
    
    public static StandEffectInstance fromNBT(CompoundNBT nbt) {
        StandEffectType<?> effectType = ModStandEffects.Registry.getRegistry().getValue(new ResourceLocation(nbt.getString("Type")));
        if (effectType == null) return null;
        StandEffectInstance effect = effectType.create();
        effect.tickCount = nbt.getInt("TickCount");
        
        if (nbt.contains("Targets", JojoModUtil.getNbtId(CompoundNBT.class))) {
            CompoundNBT targetsNBT = nbt.getCompound("Targets");
            effect.targetsLoaded = new HashSet<>();
            targetsNBT.getAllKeys().forEach(key -> {
                if (targetsNBT.hasUUID(key)) {
                    effect.targetsLoaded.add(targetsNBT.getUUID(key));
                }
            });
        }
        
        effect.readAdditionalSaveData(nbt);
        return effect;
    }
    
    public void updateTargets(ServerWorld world) {
        if (targetsLoaded != null) {
            targetsLoaded.forEach(uuid -> {
                Entity entity = world.getEntity(uuid);
                if (entity instanceof LivingEntity) {
                    addTarget((LivingEntity) entity);
                }
            });
            targetsLoaded = null;
        }
    }
    
    public void writeAdditionalPacketData(PacketBuffer buf) {}
    
    public void readAdditionalPacketData(PacketBuffer buf) {}

    protected void writeAdditionalSaveData(CompoundNBT nbt) {}

    protected void readAdditionalSaveData(CompoundNBT nbt) {}
}
