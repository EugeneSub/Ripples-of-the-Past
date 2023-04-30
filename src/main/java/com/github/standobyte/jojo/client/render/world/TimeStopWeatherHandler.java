package com.github.standobyte.jojo.client.render.world;

import java.util.Random;

import com.mojang.blaze3d.systems.RenderSystem;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.ActiveRenderInfo;
import net.minecraft.client.renderer.BufferBuilder;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.WorldRenderer;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.world.biome.Biome;
import net.minecraft.world.gen.Heightmap;
import net.minecraftforge.client.IWeatherParticleRenderHandler;
import net.minecraftforge.client.IWeatherRenderHandler;

public class TimeStopWeatherHandler implements IWeatherRenderHandler, IWeatherParticleRenderHandler {
    private static final ResourceLocation RAIN_LOCATION = new ResourceLocation("textures/environment/rain.png");
    private static final ResourceLocation SNOW_LOCATION = new ResourceLocation("textures/environment/snow.png");
    private final float[] rainSizeX = new float[1024];
    private final float[] rainSizeZ = new float[1024];
    private boolean ticksUpdated = false;
    private int ticks;
    private float partialTick;

    public TimeStopWeatherHandler() {
        for(int i = 0; i < 32; ++i) {
            for(int j = 0; j < 32; ++j) {
                float f = (float)(j - 16);
                float f1 = (float)(i - 16);
                float f2 = MathHelper.sqrt(f * f + f1 * f1);
                this.rainSizeX[i << 5 | j] = -f1 / f2;
                this.rainSizeZ[i << 5 | j] = f / f2;
            }
        }
    }
    
    public void onTimeStopEnd() {
        ticksUpdated = false;
    }
    
    private void updateTicks(int ticks, float partialTick) {
        if (!ticksUpdated) {
            this.ticks = ticks;
            this.partialTick = partialTick;
            ticksUpdated = true;
        }
    }

    @SuppressWarnings("deprecation")
    @Override
    public void render(int ticks, float partialTick, ClientWorld world, Minecraft mc, LightTexture lightmap,
            double x, double y, double z) {
        updateTicks(ticks, partialTick);
        float f = world.getRainLevel(this.partialTick);
        if (!(f <= 0.0F)) {
            int i = MathHelper.floor(x);
            int j = MathHelper.floor(y);
            int k = MathHelper.floor(z);
            Tessellator tessellator = Tessellator.getInstance();
            BufferBuilder bufferbuilder = tessellator.getBuilder();
            RenderSystem.enableAlphaTest();
            RenderSystem.disableCull();
            RenderSystem.normal3f(0.0F, 1.0F, 0.0F);
            RenderSystem.enableBlend();
            RenderSystem.defaultBlendFunc();
            RenderSystem.defaultAlphaFunc();
            RenderSystem.enableDepthTest();
            int l = 5;
            if (Minecraft.useFancyGraphics()) {
                l = 10;
            }

            RenderSystem.depthMask(Minecraft.useShaderTransparency());
            int i1 = -1;
            float f1 = (float)this.ticks + this.partialTick;
            RenderSystem.color4f(1.0F, 1.0F, 1.0F, 1.0F);
            BlockPos.Mutable blockpos$mutable = new BlockPos.Mutable();

            for(int j1 = k - l; j1 <= k + l; ++j1) {
                for(int k1 = i - l; k1 <= i + l; ++k1) {
                    int l1 = (j1 - k + 16) * 32 + k1 - i + 16;
                    double d0 = (double)this.rainSizeX[l1] * 0.5D;
                    double d1 = (double)this.rainSizeZ[l1] * 0.5D;
                    blockpos$mutable.set(k1, 0, j1);
                    Biome biome = world.getBiome(blockpos$mutable);
                    if (biome.getPrecipitation() != Biome.RainType.NONE) {
                        int i2 = world.getHeightmapPos(Heightmap.Type.MOTION_BLOCKING, blockpos$mutable).getY();
                        int j2 = j - l;
                        int k2 = j + l;
                        if (j2 < i2) {
                            j2 = i2;
                        }

                        if (k2 < i2) {
                            k2 = i2;
                        }

                        int l2 = i2;
                        if (i2 < j) {
                            l2 = j;
                        }

                        if (j2 != k2) {
                            Random random = new Random((long)(k1 * k1 * 3121 + k1 * 45238971 ^ j1 * j1 * 418711 + j1 * 13761));
                            blockpos$mutable.set(k1, j2, j1);
                            float f2 = biome.getTemperature(blockpos$mutable);
                            if (f2 >= 0.15F) {
                                if (i1 != 0) {
                                    if (i1 >= 0) {
                                        tessellator.end();
                                    }

                                    i1 = 0;
                                    mc.getTextureManager().bind(RAIN_LOCATION);
                                    bufferbuilder.begin(7, DefaultVertexFormats.PARTICLE);
                                }

                                int i3 = this.ticks + k1 * k1 * 3121 + k1 * 45238971 + j1 * j1 * 418711 + j1 * 13761 & 31;
                                float f3 = -((float)i3 + this.partialTick) / 32.0F * (3.0F + random.nextFloat());
                                double d2 = (double)((float)k1 + 0.5F) - x;
                                double d4 = (double)((float)j1 + 0.5F) - z;
                                float f4 = MathHelper.sqrt(d2 * d2 + d4 * d4) / (float)l;
                                float f5 = ((1.0F - f4 * f4) * 0.5F + 0.5F) * f;
                                blockpos$mutable.set(k1, l2, j1);
                                int j3 = WorldRenderer.getLightColor(world, blockpos$mutable);
                                bufferbuilder.vertex((double)k1 - x - d0 + 0.5D, (double)k2 - y, (double)j1 - z - d1 + 0.5D).uv(0.0F, (float)j2 * 0.25F + f3).color(1.0F, 1.0F, 1.0F, f5).uv2(j3).endVertex();
                                bufferbuilder.vertex((double)k1 - x + d0 + 0.5D, (double)k2 - y, (double)j1 - z + d1 + 0.5D).uv(1.0F, (float)j2 * 0.25F + f3).color(1.0F, 1.0F, 1.0F, f5).uv2(j3).endVertex();
                                bufferbuilder.vertex((double)k1 - x + d0 + 0.5D, (double)j2 - y, (double)j1 - z + d1 + 0.5D).uv(1.0F, (float)k2 * 0.25F + f3).color(1.0F, 1.0F, 1.0F, f5).uv2(j3).endVertex();
                                bufferbuilder.vertex((double)k1 - x - d0 + 0.5D, (double)j2 - y, (double)j1 - z - d1 + 0.5D).uv(0.0F, (float)k2 * 0.25F + f3).color(1.0F, 1.0F, 1.0F, f5).uv2(j3).endVertex();
                            } else {
                                if (i1 != 1) {
                                    if (i1 >= 0) {
                                        tessellator.end();
                                    }

                                    i1 = 1;
                                    mc.getTextureManager().bind(SNOW_LOCATION);
                                    bufferbuilder.begin(7, DefaultVertexFormats.PARTICLE);
                                }

                                float f6 = -((float)(this.ticks & 511) + this.partialTick) / 512.0F;
                                float f7 = (float)(random.nextDouble() + (double)f1 * 0.01D * (double)((float)random.nextGaussian()));
                                float f8 = (float)(random.nextDouble() + (double)(f1 * (float)random.nextGaussian()) * 0.001D);
                                double d3 = (double)((float)k1 + 0.5F) - x;
                                double d5 = (double)((float)j1 + 0.5F) - z;
                                float f9 = MathHelper.sqrt(d3 * d3 + d5 * d5) / (float)l;
                                float f10 = ((1.0F - f9 * f9) * 0.3F + 0.5F) * f;
                                blockpos$mutable.set(k1, l2, j1);
                                int k3 = WorldRenderer.getLightColor(world, blockpos$mutable);
                                int l3 = k3 >> 16 & '\uffff';
                                int i4 = (k3 & '\uffff') * 3;
                                int j4 = (l3 * 3 + 240) / 4;
                                int k4 = (i4 * 3 + 240) / 4;
                                bufferbuilder.vertex((double)k1 - x - d0 + 0.5D, (double)k2 - y, (double)j1 - z - d1 + 0.5D).uv(0.0F + f7, (float)j2 * 0.25F + f6 + f8).color(1.0F, 1.0F, 1.0F, f10).uv2(k4, j4).endVertex();
                                bufferbuilder.vertex((double)k1 - x + d0 + 0.5D, (double)k2 - y, (double)j1 - z + d1 + 0.5D).uv(1.0F + f7, (float)j2 * 0.25F + f6 + f8).color(1.0F, 1.0F, 1.0F, f10).uv2(k4, j4).endVertex();
                                bufferbuilder.vertex((double)k1 - x + d0 + 0.5D, (double)j2 - y, (double)j1 - z + d1 + 0.5D).uv(1.0F + f7, (float)k2 * 0.25F + f6 + f8).color(1.0F, 1.0F, 1.0F, f10).uv2(k4, j4).endVertex();
                                bufferbuilder.vertex((double)k1 - x - d0 + 0.5D, (double)j2 - y, (double)j1 - z - d1 + 0.5D).uv(0.0F + f7, (float)k2 * 0.25F + f6 + f8).color(1.0F, 1.0F, 1.0F, f10).uv2(k4, j4).endVertex();
                            }
                        }
                    }
                }
            }

            if (i1 >= 0) {
                tessellator.end();
            }

            RenderSystem.enableCull();
            RenderSystem.disableBlend();
            RenderSystem.defaultAlphaFunc();
            RenderSystem.disableAlphaTest();
            lightmap.turnOffLightLayer();
        }
    }

    @Override
    public void render(int ticks, ClientWorld world, Minecraft mc, ActiveRenderInfo activeRenderInfoIn) {
    }

}
