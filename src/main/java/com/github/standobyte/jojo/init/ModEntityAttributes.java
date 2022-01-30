package com.github.standobyte.jojo.init;

import com.github.standobyte.jojo.JojoMod;

import net.minecraft.entity.ai.attributes.Attribute;
import net.minecraft.entity.ai.attributes.RangedAttribute;
import net.minecraftforge.event.RegistryEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.RegistryObject;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;

@EventBusSubscriber(modid = JojoMod.MOD_ID, bus = EventBusSubscriber.Bus.MOD)
public class ModEntityAttributes {
    public static final DeferredRegister<Attribute> ATTRIBUTES = DeferredRegister.create(ForgeRegistries.ATTRIBUTES, JojoMod.MOD_ID);

    public static final RegistryObject<Attribute> STAND_DURABILITY = ATTRIBUTES.register("stand_durability", 
            () -> new RangedAttribute("attribute.name.generic.max_health", 0.0, 0.0, 1024.0).setSyncable(true));

    public static final RegistryObject<Attribute> STAND_PRECISION = ATTRIBUTES.register("stand_precision", 
            () -> new RangedAttribute("attribute.name.generic.max_health", 0.0, 0.0, 10.0).setSyncable(true));
    
    
    
    @SubscribeEvent(priority = EventPriority.LOW)
    public static final void afterAttributesRegister(RegistryEvent.Register<Attribute> event) {
        ModItems.TOMMY_GUN.get().initAttributeModifiers();
    }
}
