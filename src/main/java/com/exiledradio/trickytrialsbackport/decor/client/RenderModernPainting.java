package com.exiledradio.trickytrialsbackport.decor.client;

import com.exiledradio.trickytrialsbackport.TrickyTrialsBackport;
import com.exiledradio.trickytrialsbackport.decor.EntityModernPainting;
import com.exiledradio.trickytrialsbackport.decor.Paintings;

import net.minecraft.client.renderer.BufferBuilder;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.OpenGlHelper;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.entity.Render;
import net.minecraft.client.renderer.entity.RenderManager;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;

import javax.annotation.Nullable;

/**
 * Draws a painting of ours as 1.12.2's {@code RenderPainting} draws one, block by block with each block's light,
 * but from 1.21's textures: the picture from {@code textures/painting/<name>.png}, the back and the edges from
 * {@code textures/painting/back.png}.
 */
public class RenderModernPainting extends Render<EntityModernPainting> {

    static final ResourceLocation BACK = new ResourceLocation(TrickyTrialsBackport.MOD_ID, "textures/painting/back.png");
    private static final ResourceLocation[] FRONTS = new ResourceLocation[Paintings.all().size()];

    static {
        for (Paintings.Variant v : Paintings.all()) {
            FRONTS[v.index] = new ResourceLocation(TrickyTrialsBackport.MOD_ID, "textures/painting/" + v.name + ".png");
        }
    }

    public RenderModernPainting(RenderManager manager) {
        super(manager);
    }

    @Override
    public void doRender(EntityModernPainting entity, double x, double y, double z, float entityYaw, float partialTicks) {
        GlStateManager.pushMatrix();
        GlStateManager.translate(x, y, z);
        GlStateManager.rotate(180.0F - entityYaw, 0.0F, 1.0F, 0.0F);
        GlStateManager.enableRescaleNormal();
        GlStateManager.scale(0.0625F, 0.0625F, 0.0625F);
        if (this.renderOutlines) {
            GlStateManager.enableColorMaterial();
            GlStateManager.enableOutlineMode(this.getTeamColor(entity));
        }
        Paintings.Variant v = entity.getVariant();
        int width = v.width * 16;
        int height = v.height * 16;
        this.bindTexture(FRONTS[v.index]);
        draw(entity, width, height, true);
        this.bindTexture(BACK);
        draw(entity, width, height, false);
        if (this.renderOutlines) {
            GlStateManager.disableOutlineMode();
            GlStateManager.disableColorMaterial();
        }
        GlStateManager.disableRescaleNormal();
        GlStateManager.popMatrix();
        super.doRender(entity, x, y, z, entityYaw, partialTicks);
    }

    @Nullable
    @Override
    protected ResourceLocation getEntityTexture(EntityModernPainting entity) {
        return FRONTS[entity.getVariant().index];
    }

    /** The picture (front) or the back and four edges, one block-sized cell at a time. */
    private void draw(EntityModernPainting painting, int width, int height, boolean front) {
        float left = -width / 2.0F;
        float bottom = -height / 2.0F;
        float edge = 1.0F / 16.0F;
        for (int i = 0; i < width / 16; ++i) {
            for (int j = 0; j < height / 16; ++j) {
                float x1 = left + (i + 1) * 16;
                float x0 = left + i * 16;
                float y1 = bottom + (j + 1) * 16;
                float y0 = bottom + j * 16;
                this.setLightmap(painting, (x1 + x0) / 2.0F, (y1 + y0) / 2.0F);
                Tessellator tessellator = Tessellator.getInstance();
                BufferBuilder b = tessellator.getBuffer();
                b.begin(7, DefaultVertexFormats.POSITION_TEX_NORMAL);
                if (front) {
                    double u0 = (double) (width - i * 16) / width;
                    double u1 = (double) (width - (i + 1) * 16) / width;
                    double v0 = (double) (height - j * 16) / height;
                    double v1 = (double) (height - (j + 1) * 16) / height;
                    b.pos(x1, y0, -0.5D).tex(u1, v0).normal(0.0F, 0.0F, -1.0F).endVertex();
                    b.pos(x0, y0, -0.5D).tex(u0, v0).normal(0.0F, 0.0F, -1.0F).endVertex();
                    b.pos(x0, y1, -0.5D).tex(u0, v1).normal(0.0F, 0.0F, -1.0F).endVertex();
                    b.pos(x1, y1, -0.5D).tex(u1, v1).normal(0.0F, 0.0F, -1.0F).endVertex();
                } else {
                    b.pos(x1, y1, 0.5D).tex(0.0D, 0.0D).normal(0.0F, 0.0F, 1.0F).endVertex();
                    b.pos(x0, y1, 0.5D).tex(1.0D, 0.0D).normal(0.0F, 0.0F, 1.0F).endVertex();
                    b.pos(x0, y0, 0.5D).tex(1.0D, 1.0D).normal(0.0F, 0.0F, 1.0F).endVertex();
                    b.pos(x1, y0, 0.5D).tex(0.0D, 1.0D).normal(0.0F, 0.0F, 1.0F).endVertex();
                    // Top and bottom: a one-pixel row of the back; the sides: a one-pixel column.
                    b.pos(x1, y1, -0.5D).tex(0.0D, 0.0D).normal(0.0F, 1.0F, 0.0F).endVertex();
                    b.pos(x0, y1, -0.5D).tex(1.0D, 0.0D).normal(0.0F, 1.0F, 0.0F).endVertex();
                    b.pos(x0, y1, 0.5D).tex(1.0D, edge).normal(0.0F, 1.0F, 0.0F).endVertex();
                    b.pos(x1, y1, 0.5D).tex(0.0D, edge).normal(0.0F, 1.0F, 0.0F).endVertex();
                    b.pos(x1, y0, 0.5D).tex(0.0D, 0.0D).normal(0.0F, -1.0F, 0.0F).endVertex();
                    b.pos(x0, y0, 0.5D).tex(1.0D, 0.0D).normal(0.0F, -1.0F, 0.0F).endVertex();
                    b.pos(x0, y0, -0.5D).tex(1.0D, edge).normal(0.0F, -1.0F, 0.0F).endVertex();
                    b.pos(x1, y0, -0.5D).tex(0.0D, edge).normal(0.0F, -1.0F, 0.0F).endVertex();
                    b.pos(x1, y1, 0.5D).tex(edge, 0.0D).normal(-1.0F, 0.0F, 0.0F).endVertex();
                    b.pos(x1, y0, 0.5D).tex(edge, 1.0D).normal(-1.0F, 0.0F, 0.0F).endVertex();
                    b.pos(x1, y0, -0.5D).tex(0.0D, 1.0D).normal(-1.0F, 0.0F, 0.0F).endVertex();
                    b.pos(x1, y1, -0.5D).tex(0.0D, 0.0D).normal(-1.0F, 0.0F, 0.0F).endVertex();
                    b.pos(x0, y1, -0.5D).tex(0.0D, 0.0D).normal(1.0F, 0.0F, 0.0F).endVertex();
                    b.pos(x0, y0, -0.5D).tex(0.0D, 1.0D).normal(1.0F, 0.0F, 0.0F).endVertex();
                    b.pos(x0, y0, 0.5D).tex(edge, 1.0D).normal(1.0F, 0.0F, 0.0F).endVertex();
                    b.pos(x0, y1, 0.5D).tex(edge, 0.0D).normal(1.0F, 0.0F, 0.0F).endVertex();
                }
                tessellator.draw();
            }
        }
    }

    /** 1.12.2's {@code RenderPainting.setLightmap}: the light of the block behind this cell. */
    private void setLightmap(EntityModernPainting painting, float cx, float cy) {
        int i = MathHelper.floor(painting.posX);
        int j = MathHelper.floor(painting.posY + cy / 16.0F);
        int k = MathHelper.floor(painting.posZ);
        EnumFacing facing = painting.facingDirection;
        if (facing == EnumFacing.NORTH) {
            i = MathHelper.floor(painting.posX + cx / 16.0F);
        }
        if (facing == EnumFacing.WEST) {
            k = MathHelper.floor(painting.posZ - cx / 16.0F);
        }
        if (facing == EnumFacing.SOUTH) {
            i = MathHelper.floor(painting.posX - cx / 16.0F);
        }
        if (facing == EnumFacing.EAST) {
            k = MathHelper.floor(painting.posZ + cx / 16.0F);
        }
        int light = this.renderManager.world.getCombinedLight(new BlockPos(i, j, k), 0);
        OpenGlHelper.setLightmapTextureCoords(OpenGlHelper.lightmapTexUnit, light % 65536, light / 65536);
        GlStateManager.color(1.0F, 1.0F, 1.0F);
    }
}
