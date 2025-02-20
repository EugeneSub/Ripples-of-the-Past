package com.github.standobyte.jojo.network.packets.fromserver;

import java.util.function.Supplier;

import com.github.standobyte.jojo.client.ClientUtil;
import com.github.standobyte.jojo.init.power.non_stand.ModPowers;
import com.github.standobyte.jojo.network.packets.IModPacketHandler;
import com.github.standobyte.jojo.power.impl.nonstand.INonStandPower;
import com.github.standobyte.jojo.power.impl.nonstand.type.hamon.HamonData;

import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.network.PacketBuffer;
import net.minecraftforge.fml.network.NetworkEvent;

public class TrHamonProtectionPacket {
    private final int entityId;
    private final boolean protectionEnabled;
    
    public TrHamonProtectionPacket(int entityId, HamonData hamonData) {
        this(entityId, hamonData.isProtectionEnabled());
    }
    
    public TrHamonProtectionPacket(int entityId, boolean protectionEnabled) {
        this.entityId = entityId;
        this.protectionEnabled = protectionEnabled;
    }
    
    
    
    public static class Handler implements IModPacketHandler<TrHamonProtectionPacket> {

        @Override
        public void encode(TrHamonProtectionPacket msg, PacketBuffer buf) {
            buf.writeInt(msg.entityId);
            buf.writeBoolean(msg.protectionEnabled);
        }

        @Override
        public TrHamonProtectionPacket decode(PacketBuffer buf) {
            return new TrHamonProtectionPacket(buf.readInt(), buf.readBoolean());
        }

        @Override
        public void handle(TrHamonProtectionPacket msg, Supplier<NetworkEvent.Context> ctx) {
            Entity entity = ClientUtil.getEntityById(msg.entityId);
            if (entity instanceof LivingEntity) {
                INonStandPower.getNonStandPowerOptional((LivingEntity) entity).ifPresent(power -> {
                    power.getTypeSpecificData(ModPowers.HAMON.get()).ifPresent(hamon -> {
                        hamon.setHamonProtection(msg.protectionEnabled);
                    });
                });
            }
        }

        @Override
        public Class<TrHamonProtectionPacket> getPacketClass() {
            return TrHamonProtectionPacket.class;
        }
    }
}
