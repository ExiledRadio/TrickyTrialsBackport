package com.exiledradio.trickytrialsbackport.ghast.client;

import com.exiledradio.trickytrialsbackport.common.Content;
import com.exiledradio.trickytrialsbackport.ghast.EntityHappyGhast;
import com.exiledradio.trickytrialsbackport.ghast.GhastContent;
import com.exiledradio.trickytrialsbackport.ghast.ItemHarness;

import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.culling.ICamera;
import net.minecraft.client.renderer.entity.RenderLiving;
import net.minecraft.client.renderer.entity.RenderManager;
import net.minecraft.client.renderer.entity.layers.LayerRenderer;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.AxisAlignedBB;

import java.util.HashMap;
import java.util.Map;

/**
 * 1.21.11's {@code HappyGhastRenderer}: the model 4 times larger (the ghastling 0.95), shadow 2 (scaled with the
 * ghastling), the harness layer, and culled by a box reaching half its height below its feet (the tentacles).
 */
public final class RenderHappyGhast extends RenderLiving<EntityHappyGhast> {

    static final ResourceLocation TEXTURE = Content.id("textures/entity/ghast/happy_ghast.png");
    static final ResourceLocation BABY_TEXTURE = Content.id("textures/entity/ghast/happy_ghast_baby.png");
    private static final float SCALE = 4.0F;
    private static final float SHADOW = 2.0F;

    public RenderHappyGhast(RenderManager manager) {
        super(manager, new ModelHappyGhast(), SHADOW);
        this.addLayer(new HarnessLayer());
    }

    @Override
    protected ResourceLocation getEntityTexture(EntityHappyGhast entity) {
        return entity.isChild() ? BABY_TEXTURE : TEXTURE;
    }

    @Override
    protected void preRenderCallback(EntityHappyGhast entity, float partialTicks) {
        float s = SCALE * (entity.isChild() ? EntityHappyGhast.BABY_SCALE : 1.0F);
        GlStateManager.scale(s, s, s);
    }

    @Override
    public void doRender(EntityHappyGhast entity, double x, double y, double z, float yaw, float partialTicks) {
        this.shadowSize = SHADOW * (entity.isChild() ? EntityHappyGhast.BABY_SCALE : 1.0F);
        super.doRender(entity, x, y, z, yaw, partialTicks);
    }

    @Override
    public boolean shouldRender(EntityHappyGhast entity, ICamera camera, double camX, double camY, double camZ) {
        if (super.shouldRender(entity, camera, camX, camY, camZ)) {
            return true;
        }
        AxisAlignedBB box = entity.getRenderBoundingBox();
        return camera.isBoundingBoxInFrustum(new AxisAlignedBB(box.minX, box.minY - entity.height / 2.0F, box.minZ, box.maxX, box.maxY, box.maxZ));
    }

    /** 1.21's {@code SimpleEquipmentLayer} for the happy ghast's body: the harness of the color worn. */
    private final class HarnessLayer implements LayerRenderer<EntityHappyGhast> {
        private final ModelHappyGhastHarness model = new ModelHappyGhastHarness();
        private final Map<String, ResourceLocation> textures = new HashMap<String, ResourceLocation>();

        @Override
        public void doRenderLayer(EntityHappyGhast entity, float limbSwing, float limbSwingAmount, float partialTicks, float ageInTicks,
                                  float netHeadYaw, float headPitch, float scale) {
            ItemStack harness = entity.getHarness();
            if (!(harness.getItem() instanceof ItemHarness) || entity.isChild()) {
                return;
            }
            String color = GhastContent.colorName(((ItemHarness) harness.getItem()).color());
            ResourceLocation texture = this.textures.computeIfAbsent(color,
                    c -> Content.id("textures/entity/equipment/happy_ghast_body/" + c + "_harness.png"));
            bindTexture(texture);
            GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
            this.model.render(entity, limbSwing, limbSwingAmount, ageInTicks, netHeadYaw, headPitch, scale);
        }

        @Override
        public boolean shouldCombineTextures() {
            return false;
        }
    }
}
