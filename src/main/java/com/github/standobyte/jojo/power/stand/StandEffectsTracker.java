package com.github.standobyte.jojo.power.stand;

import java.util.Collection;
import java.util.Collections;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import javax.annotation.Nullable;

import com.github.standobyte.jojo.action.stand.effect.StandEffectInstance;
import com.github.standobyte.jojo.network.PacketManager;
import com.github.standobyte.jojo.network.packets.fromserver.TrStandEffectPacket;
import com.github.standobyte.jojo.util.utils.JojoModUtil;

import it.unimi.dsi.fastutil.ints.Int2ObjectLinkedOpenHashMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap.Entry;
import it.unimi.dsi.fastutil.objects.ObjectIterator;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.nbt.CompoundNBT;

public class StandEffectsTracker {
    private static final AtomicInteger EFFECTS_COUNTER = new AtomicInteger();
    private final IStandPower standPower;
    private final Int2ObjectMap<StandEffectInstance> effects = new Int2ObjectLinkedOpenHashMap<>();
    
    public StandEffectsTracker(IStandPower standPower) {
        this.standPower = standPower;
    }
    
    public void addEffect(StandEffectInstance instance) {
        putEffectInstance(instance);
        LivingEntity user = standPower.getUser();
        if (!user.level.isClientSide()) {
            PacketManager.sendToClientsTrackingAndSelf(TrStandEffectPacket.add(
                    instance.withId(EFFECTS_COUNTER.incrementAndGet())), user);
        }
    }
    
    private void putEffectInstance(StandEffectInstance instance) {
        instance.withStand(standPower);
        effects.put(instance.getId(), instance);
        instance.onStart();
    }
    
    public void removeEffect(StandEffectInstance instance) {
        if (instance != null) {
            onEffectRemoved(instance);
            effects.remove(instance.getId());
            LivingEntity user = standPower.getUser();
            if (!user.level.isClientSide()) {
                PacketManager.sendToClientsTrackingAndSelf(TrStandEffectPacket.remove(
                        instance.withId(EFFECTS_COUNTER.incrementAndGet())), user);
            }
        }
    }
    
    private void onEffectRemoved(StandEffectInstance instance) {
        instance.onStop();
    }

    public void tick() {
        if (effects.isEmpty()) {
            return;
        }
        
        ObjectIterator<Entry<StandEffectInstance>> it = effects.int2ObjectEntrySet().iterator();
        while (it.hasNext()) {
            StandEffectInstance effect = it.next().getValue();
            effect.tickCount++;
            effect.onTick();
            if (effect.toBeRemoved()) {
                onEffectRemoved(effect);
                it.remove();
            }
        }
    }

    public void onUserStandRemoved(LivingEntity user) {
        effects.values().forEach(effect -> effect.onStop());
        effects.clear();
        if (!user.level.isClientSide()) {
            PacketManager.sendToClientsTrackingAndSelf(TrStandEffectPacket.removeAll(user), user);
        }
    }
    
    public StandEffectInstance getById(int id) {
        return effects.get(id);
    }
    
    public Collection<StandEffectInstance> getEffects(@Nullable Predicate<StandEffectInstance> filter) {
        if (filter == null) {
            return Collections.unmodifiableCollection(effects.values());
        }
        return effects.values().stream().filter(filter).collect(Collectors.toList());
    }
    
    public void syncWithTrackingOrUser(ServerPlayerEntity player) {
        effects.values().forEach(effect -> {
            effect.updateTargets(player.getLevel());
            PacketManager.sendToClientsTrackingAndSelf(TrStandEffectPacket.add(effect), player);
        });
    }
    
    public CompoundNBT toNBT() {
        CompoundNBT nbt = new CompoundNBT();
        effects.forEach((id, effect) -> {
            if (!effect.toBeRemoved()) {
                nbt.put(String.valueOf(id), effect.toNBT());
            }
        });
        return nbt;
    }
    
    public void fromNBT(CompoundNBT nbt) {
        nbt.getAllKeys().forEach(key -> {
            if (nbt.contains(key, JojoModUtil.getNbtId(CompoundNBT.class))) {
                StandEffectInstance effect = StandEffectInstance.fromNBT(nbt.getCompound(key));
                if (effect != null) {
                    putEffectInstance(effect);
                }
            }
        });
    }
}
