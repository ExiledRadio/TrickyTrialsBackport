package com.exiledradio.trickytrialsbackport.trial.client;

import com.exiledradio.trickytrialsbackport.trial.TileEntityTrialSpawner;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.tileentity.TileEntitySpecialRenderer;
import net.minecraft.entity.Entity;

/** The mob spinning inside a trial spawner: 1.21's {@code TrialSpawnerRenderer}, as the mob spawner draws it. */
final class RenderTrialSpawner extends TileEntitySpecialRenderer<TileEntityTrialSpawner> {

    @Override
    public void render(TileEntityTrialSpawner te, double x, double y, double z, float partialTicks, int destroyStage, float alpha) {
        if (te.getWorld() == null) {
            return;
        }
        Entity entity = te.getOrCreateDisplayEntity(te.getPhase());
        if (entity == null) {
            return;
        }
        GlStateManager.pushMatrix();
        GlStateManager.translate((float) x + 0.5F, (float) y, (float) z + 0.5F);
        float scale = 0.53125F;
        float size = Math.max(entity.width, entity.height);
        if (size > 1.0F) {
            scale /= size;
        }
        GlStateManager.translate(0.0F, 0.4F, 0.0F);
        GlStateManager.rotate((float) (te.oSpin + (te.spin - te.oSpin) * partialTicks) * 10.0F, 0.0F, 1.0F, 0.0F);
        GlStateManager.translate(0.0F, -0.2F, 0.0F);
        GlStateManager.rotate(-30.0F, 1.0F, 0.0F, 0.0F);
        GlStateManager.scale(scale, scale, scale);
        entity.setLocationAndAngles(x, y, z, 0.0F, 0.0F);
        Minecraft.getMinecraft().getRenderManager().renderEntity(entity, 0.0, 0.0, 0.0, 0.0F, partialTicks, false);
        GlStateManager.popMatrix();
    }
}
