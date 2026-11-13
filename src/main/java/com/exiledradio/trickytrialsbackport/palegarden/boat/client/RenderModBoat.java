package com.exiledradio.trickytrialsbackport.palegarden.boat.client;

import com.exiledradio.trickytrialsbackport.TrickyTrialsBackport;
import com.exiledradio.trickytrialsbackport.palegarden.boat.BoatWood;
import com.exiledradio.trickytrialsbackport.palegarden.boat.EntityModBoat;

import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.entity.Render;
import net.minecraft.client.renderer.entity.RenderManager;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.MathHelper;
import net.minecraftforge.fml.client.registry.RenderingRegistry;

/**
 * Draws this mod's boats as vanilla's {@code RenderBoat} draws its own (same translation, rocking when hit,
 * the water mask inside a boat's hull), with the wood's 1.20 texture and model.
 */
public class RenderModBoat extends Render<EntityModBoat> {

    private final ModelWoodBoat boat = new ModelWoodBoat(false, false);

    public RenderModBoat(RenderManager manager) {
        super(manager);
        this.shadowSize = 0.5F;
    }

    public static void register() {
        RenderingRegistry.registerEntityRenderingHandler(EntityModBoat.class, RenderModBoat::new);
    }

    private ModelWoodBoat model(EntityModBoat entity) {
        return this.boat;
    }

    @Override
    public void doRender(EntityModBoat entity, double x, double y, double z, float entityYaw, float partialTicks) {
        GlStateManager.pushMatrix();
        setup(entity, x, y, z, entityYaw, partialTicks);
        bindEntityTexture(entity);
        if (this.renderOutlines) {
            GlStateManager.enableColorMaterial();
            GlStateManager.enableOutlineMode(getTeamColor(entity));
        }
        model(entity).render(entity, partialTicks, 0.0625F);
        if (this.renderOutlines) {
            GlStateManager.disableOutlineMode();
            GlStateManager.disableColorMaterial();
        }
        GlStateManager.popMatrix();
        super.doRender(entity, x, y, z, entityYaw, partialTicks);
    }

    private static void setup(EntityModBoat entity, double x, double y, double z, float entityYaw, float partialTicks) {
        GlStateManager.translate((float) x, (float) y + 0.375F, (float) z);
        GlStateManager.rotate(180.0F - entityYaw, 0.0F, 1.0F, 0.0F);
        float hit = entity.getTimeSinceHit() - partialTicks;
        float damage = Math.max(0.0F, entity.getDamageTaken() - partialTicks);
        if (hit > 0.0F) {
            GlStateManager.rotate(MathHelper.sin(hit) * hit * damage / 10.0F * entity.getForwardDirection(), 1.0F, 0.0F, 0.0F);
        }
        GlStateManager.scale(-1.0F, -1.0F, 1.0F);
    }

    @Override
    public boolean isMultipass() {
        return true;
    }

    /** The invisible patch that keeps water from being drawn inside a boat's hull. */
    @Override
    public void renderMultipass(EntityModBoat entity, double x, double y, double z, float entityYaw, float partialTicks) {
        ModelWoodBoat m = model(entity);
        if (m.noWater == null) {
            return;
        }
        GlStateManager.pushMatrix();
        setup(entity, x, y, z, entityYaw, partialTicks);
        bindEntityTexture(entity);
        GlStateManager.rotate(90.0F, 0.0F, 1.0F, 0.0F);
        GlStateManager.colorMask(false, false, false, false);
        m.noWater.render(0.0625F);
        GlStateManager.colorMask(true, true, true, true);
        GlStateManager.popMatrix();
    }

    @Override
    protected ResourceLocation getEntityTexture(EntityModBoat entity) {
        BoatWood wood = entity.wood();
        return new ResourceLocation(TrickyTrialsBackport.MOD_ID, "textures/entity/boat/" + wood.name + ".png");
    }
}
