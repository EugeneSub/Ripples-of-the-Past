package com.github.standobyte.jojo.client.render.entity.renderer;

import com.github.standobyte.jojo.entity.damaging.projectile.HamonSendoOverdriveEntity2;
import com.mojang.blaze3d.matrix.MatrixStack;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.IRenderTypeBuffer;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererManager;
import net.minecraft.util.ResourceLocation;

public class SendoHamonOverdriveRenderer2 extends EntityRenderer<HamonSendoOverdriveEntity2> {

    public SendoHamonOverdriveRenderer2(EntityRendererManager renderManager) {
        super(renderManager);
    }

    @Override
    public ResourceLocation getTextureLocation(HamonSendoOverdriveEntity2 p_110775_1_) {
        return null;
    }

    @Override
    public void render(HamonSendoOverdriveEntity2 entity, float yRotation, float partialTick, 
            MatrixStack matrixStack, IRenderTypeBuffer buffer, int packedLight) {
        if (shouldRender(entity)) {
            super.render(entity, yRotation, partialTick, matrixStack, buffer, packedLight);
        }
    }
    
    protected boolean shouldRender(HamonSendoOverdriveEntity2 entity) {
        return !entity.isInvisible() || !entity.isInvisibleTo(Minecraft.getInstance().player);
    }
    
}
