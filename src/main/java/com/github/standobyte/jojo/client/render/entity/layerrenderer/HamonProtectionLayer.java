package com.github.standobyte.jojo.client.render.entity.layerrenderer;

import com.github.standobyte.jojo.JojoMod;
import com.github.standobyte.jojo.client.ClientUtil;
import com.github.standobyte.jojo.client.playeranim.PlayerAnimationHandler;
import com.github.standobyte.jojo.init.power.non_stand.ModPowers;
import com.github.standobyte.jojo.power.impl.nonstand.INonStandPower;
import com.github.standobyte.jojo.power.impl.nonstand.type.hamon.HamonData;
import com.mojang.blaze3d.matrix.MatrixStack;
import com.mojang.blaze3d.vertex.IVertexBuilder;

import net.minecraft.client.entity.player.AbstractClientPlayerEntity;
import net.minecraft.client.renderer.IRenderTypeBuffer;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.IEntityRenderer;
import net.minecraft.client.renderer.entity.PlayerRenderer;
import net.minecraft.client.renderer.entity.layers.LayerRenderer;
import net.minecraft.client.renderer.entity.model.PlayerModel;
import net.minecraft.client.renderer.model.ModelRenderer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.HandSide;
import net.minecraft.util.ResourceLocation;

//@OnlyIn(Dist.CLIENT)
public class HamonProtectionLayer<T extends LivingEntity, M extends PlayerModel<T>> extends LayerRenderer<T, M> implements IFirstPersonHandLayer {
    //public static final ResourceLocation TEXTURE = new ResourceLocation(JojoMod.MOD_ID, "textures/entity/biped/hamon_protection.png");
    private final PlayerModel<T> model = new PlayerModel<>(0F, false);
    
    public HamonProtectionLayer(IEntityRenderer<T, M> renderer) {
        super(renderer);
        PlayerAnimationHandler.getPlayerAnimator().onArmorLayerInit(this);
    }
    

    protected ResourceLocation getTexture(AbstractClientPlayerEntity player){
    	return player.getSkinTextureLocation();
    }
    
    @Override
    public void render(MatrixStack pMatrixStack, IRenderTypeBuffer pBuffer, int pPackedLight, 
    		T pLivingEntity, float pLimbSwing, float pLimbSwingAmount, float pPartialTicks, float pAgeInTicks, float pNetHeadYaw, float pHeadPitch) {
    	if (INonStandPower.getNonStandPowerOptional(pLivingEntity).resolve().flatMap(
                        power -> power.getTypeSpecificData(ModPowers.HAMON.get())
                        .map(hamon -> power.getEnergy() > 0.5 * power.getMaxEnergy())).orElse(false) && INonStandPower.getNonStandPowerOptional(pLivingEntity).resolve().flatMap(
                                power -> power.getTypeSpecificData(ModPowers.HAMON.get())
                                .map(hamon -> hamon.getBreathStability() == hamon.getMaxBreathStability())).orElse(false)) {
	        PlayerModel<T> entitymodel = model;
	        entitymodel.prepareMobModel(pLivingEntity, pLimbSwing, pLimbSwingAmount, pPartialTicks);
	        this.getParentModel().copyPropertiesTo(entitymodel);
	        IVertexBuilder vertexBuilder = pBuffer.getBuffer(RenderType.entityTranslucent(getTexture((AbstractClientPlayerEntity) pLivingEntity)));
	        entitymodel.setupAnim(pLivingEntity, pLimbSwing, pLimbSwingAmount, pAgeInTicks, pNetHeadYaw, pHeadPitch);
	        entitymodel.renderToBuffer(pMatrixStack, vertexBuilder, ClientUtil.MAX_MODEL_LIGHT, OverlayTexture.NO_OVERLAY, 1.0F, 1.0F, 1.0F, 1.0F);
    	}
    }
    
    @Override
    public void renderHandFirstPerson(HandSide side, MatrixStack matrixStack, 
            IRenderTypeBuffer buffer, int light, AbstractClientPlayerEntity player, 
            PlayerRenderer playerRenderer) {
        if (!player.isSpectator() && INonStandPower.getNonStandPowerOptional(player).resolve().flatMap(
                power -> power.getTypeSpecificData(ModPowers.HAMON.get())
                .map(hamon -> power.getEnergy() > 0.5 * power.getMaxEnergy())).orElse(false) && INonStandPower.getNonStandPowerOptional(player).resolve().flatMap(
                        power -> power.getTypeSpecificData(ModPowers.HAMON.get())
                        .map(hamon -> hamon.getBreathStability() == hamon.getMaxBreathStability())).orElse(false)) {
            float partialTick = ClientUtil.getPartialTick();
            float f = (float)player.tickCount + partialTick;
            PlayerModel<AbstractClientPlayerEntity> model = playerRenderer.getModel();
            ClientUtil.setupForFirstPersonRender(model, player);
            IVertexBuilder vertexBuilder = buffer.getBuffer(RenderType.entityTranslucent(getTexture(player)));
            ModelRenderer arm = ClientUtil.getArm(model, side);
            ModelRenderer armOuter = ClientUtil.getArmOuter(model, side);
            arm.xRot = 0.0F;
            arm.render(matrixStack, vertexBuilder, ClientUtil.MAX_MODEL_LIGHT, OverlayTexture.NO_OVERLAY, 1.0F, 1.0F, 1.0F, 1.0F);
            armOuter.xRot = 0.0F;
            armOuter.render(matrixStack, vertexBuilder, ClientUtil.MAX_MODEL_LIGHT, OverlayTexture.NO_OVERLAY, 1.0F, 1.0F, 1.0F, 1.0F);
        }
    }
    
    protected float xOffset(float p_225634_1_) {
        return p_225634_1_ * 0.01F;
     }
}

