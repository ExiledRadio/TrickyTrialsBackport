package com.exiledradio.trickytrialsbackport.breeze.client;

import com.exiledradio.trickytrialsbackport.breeze.EntityAbstractWindCharge;
import com.exiledradio.trickytrialsbackport.common.Content;

import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.entity.Render;
import net.minecraft.client.renderer.entity.RenderManager;
import net.minecraft.util.ResourceLocation;

/** Both wind charges: the spinning model, translucent, its texture drifting as it flies. */
public final class RenderWindCharge extends Render<EntityAbstractWindCharge> {

    static final ResourceLocation TEXTURE = Content.id("textures/entity/projectiles/wind_charge.png");

    private final ModelWindCharge model = new ModelWindCharge();

    public RenderWindCharge(RenderManager manager) {
        super(manager);
    }

    @Override
    public void doRender(EntityAbstractWindCharge entity, double x, double y, double z, float yaw, float partialTicks) {
        float age = entity.ticksExisted + partialTicks;
        GlStateManager.pushMatrix();
        GlStateManager.translate((float) x, (float) y + 0.15F, (float) z);
        GlStateManager.scale(-1.0F, -1.0F, 1.0F);
        bindEntityTexture(entity);
        GlStateManager.matrixMode(5890);
        GlStateManager.loadIdentity();
        GlStateManager.translate(age * 0.03F, 0.0F, 0.0F);
        GlStateManager.matrixMode(5888);
        GlStateManager.enableBlend();
        GlStateManager.blendFunc(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA);
        this.model.render(entity, 0.0F, 0.0F, age, 0.0F, 0.0F, 0.0625F);
        GlStateManager.disableBlend();
        GlStateManager.matrixMode(5890);
        GlStateManager.loadIdentity();
        GlStateManager.matrixMode(5888);
        GlStateManager.popMatrix();
        super.doRender(entity, x, y, z, yaw, partialTicks);
    }

    @Override
    protected ResourceLocation getEntityTexture(EntityAbstractWindCharge entity) {
        return TEXTURE;
    }
}
