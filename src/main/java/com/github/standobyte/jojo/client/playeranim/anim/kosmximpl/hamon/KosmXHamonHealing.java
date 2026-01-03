package com.github.standobyte.jojo.client.playeranim.anim.kosmximpl.hamon;

import com.github.standobyte.jojo.JojoMod;
import com.github.standobyte.jojo.action.ActionTarget.TargetType;
import com.github.standobyte.jojo.client.playeranim.PlayerAnimationHandler;
import com.github.standobyte.jojo.client.playeranim.anim.interfaces.BasicToggleAnim;
import com.github.standobyte.jojo.client.playeranim.kosmx.KosmXPlayerAnimatorInstalled.AnimLayerHandler;
import com.github.standobyte.jojo.client.playeranim.kosmx.anim.modifier.KosmXFixedFadeModifier;
import com.github.standobyte.jojo.client.playeranim.kosmx.anim.modifier.KosmXHeadRotationModifier;
import com.github.standobyte.jojo.init.power.non_stand.ModPowers;
import com.github.standobyte.jojo.init.power.non_stand.hamon.ModHamonActions;
import com.github.standobyte.jojo.init.power.non_stand.hamon.ModHamonSkills;
import com.github.standobyte.jojo.power.impl.nonstand.INonStandPower;
import com.github.standobyte.jojo.power.impl.nonstand.type.hamon.HamonData;
import com.github.standobyte.jojo.util.general.GeneralUtil;
import com.github.standobyte.jojo.util.mc.MCUtil;

import dev.kosmx.playerAnim.api.layered.IAnimation;
import dev.kosmx.playerAnim.api.layered.ModifierLayer;
import dev.kosmx.playerAnim.api.layered.modifier.SpeedModifier;
import dev.kosmx.playerAnim.core.util.Ease;
import net.minecraft.client.entity.player.AbstractClientPlayerEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.Hand;
import net.minecraft.util.HandSide;
import net.minecraft.util.ResourceLocation;

public class KosmXHamonHealing extends AnimLayerHandler<ModifierLayer<IAnimation>> implements BasicToggleAnim {
	//private static final float SPEED = 1.4F;

    public KosmXHamonHealing(ResourceLocation id) {
        super(id);
    }

    @Override
    protected ModifierLayer<IAnimation> createAnimLayer(AbstractClientPlayerEntity player) {
        return new ModifierLayer<>(null, new KosmXHeadRotationModifier());
    }
    
    
    @Override
    public boolean setAnimEnabled(PlayerEntity player, boolean enabled) {
        enabled &= PlayerAnimationHandler.canAnimate(player);
        if (enabled) {
            return fadeOutAnim((AbstractClientPlayerEntity) player, KosmXFixedFadeModifier.standardFadeIn(4, Ease.OUTCUBIC), 
                    getAnimFromName(getAnimPath(player)));
        }
        else {
            return fadeOutAnim((AbstractClientPlayerEntity) player, KosmXFixedFadeModifier.standardFadeIn(10, Ease.OUTCUBIC), null);
        }
    }
    
    private static final ResourceLocation HEAL = new ResourceLocation(JojoMod.MOD_ID, "hamon_healing");
    private static final ResourceLocation PLANT = new ResourceLocation(JojoMod.MOD_ID, "hamon_plant_growth");
    private static final ResourceLocation HEAL_TOUCH = new ResourceLocation(JojoMod.MOD_ID, "hamon_healing_touch");
    //private static final ResourceLocation ITEMS_BOTH = new ResourceLocation(JojoMod.MOD_ID, "sendo_wave_kick_lr");
    private static final ResourceLocation[] ANIMS = new ResourceLocation[] {
    		HEAL,
    		PLANT,
    		HEAL_TOUCH
            //ITEMS_BOTH
    };
    private ResourceLocation getAnimPath(PlayerEntity player) {
        int index = 0;
        HamonData hamon = INonStandPower.getPlayerNonStandPower(player).getTypeSpecificData(ModPowers.HAMON.get()).get();
        if (INonStandPower.getNonStandPowerOptional(player).map(power -> power.getHeldAction(true) == ModHamonActions.HAMON_HEALING.get()).orElse(false)) {
        	if(INonStandPower.getNonStandPowerOptional(player).map(power -> power.getMouseTarget().getType() == TargetType.BLOCK).orElse(false) 
        			&& hamon.isSkillLearned(ModHamonSkills.PLANTS_GROWTH.get())) {
        		index = 1;
        	} else if (INonStandPower.getNonStandPowerOptional(player).map(power -> power.getMouseTarget().getType() == TargetType.ENTITY).orElse(false) 
        			&& player.isShiftKeyDown()
        			&& hamon.isSkillLearned(ModHamonSkills.HEALING_TOUCH.get())) {
        		index = 2;
        	}
        	
        }
        return ANIMS[index];
    }
    
}
