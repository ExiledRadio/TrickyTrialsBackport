package com.exiledradio.trickytrialsbackport.decor.client;

import com.exiledradio.trickytrialsbackport.TrickyTrialsBackport;
import com.exiledradio.trickytrialsbackport.decor.EntityModernPainting;
import com.exiledradio.trickytrialsbackport.decor.PaintingNames;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.EntityRenderer;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.OpenGlHelper;
import net.minecraft.client.renderer.entity.RenderManager;
import net.minecraft.entity.Entity;
import net.minecraft.entity.item.EntityPainting;
import net.minecraft.util.math.RayTraceResult;
import net.minecraftforge.client.event.RenderWorldLastEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.relauncher.Side;

import javax.annotation.Nullable;

/**
 * 26.3's named paintings on screen ({@link PaintingNames}): the painting under the crosshair shows its custom name,
 * as 26.3's {@code EntityRenderer.shouldShowName} decides for an entity that is not a mob (a custom name and the
 * crosshair's entity), within its 64 blocks ({@code extractNameTags}).
 *
 * <p>The nameplate is 1.12.2's own ({@code Render.renderLivingLabel}: {@code EntityRenderer.drawNameplate} at the
 * entity's height plus half a block, at the entity's light), drawn after the world because 1.12.2's
 * {@code RenderPainting} is vanilla's and not ours to change. A painting whose name is always shown
 * ({@code CustomNameVisible}) already has 1.12.2's nameplate, so it is not drawn twice.
 */
@Mod.EventBusSubscriber(modid = TrickyTrialsBackport.MOD_ID, value = Side.CLIENT)
public final class PaintingNameplate {

    /** 26.3's {@code extractNameTags} distance for an entity: 64 blocks. */
    public static final double DISTANCE = 64.0D;

    /** For the client self-test: the name last drawn, and on which frame count. */
    public static volatile String lastDrawn;
    public static volatile int drawnCount;

    private PaintingNameplate() {
    }

    /** @return the painting whose name 26.3 shows now, or null */
    @Nullable
    public static Entity shown(Minecraft mc) {
        if (!PaintingNames.since263() || mc.world == null) {
            return null;
        }
        RayTraceResult hit = mc.objectMouseOver;
        Entity viewer = mc.getRenderViewEntity();
        if (hit == null || hit.typeOfHit != RayTraceResult.Type.ENTITY || viewer == null) {
            return null;
        }
        return shows(hit.entityHit, viewer) ? hit.entityHit : null;
    }

    /** 26.3's rule for a painting under the crosshair, seen by the viewer. */
    public static boolean shows(@Nullable Entity entity, Entity viewer) {
        return (entity instanceof EntityPainting || entity instanceof EntityModernPainting) && !entity.isDead
                && entity.hasCustomName() && !entity.getAlwaysRenderNameTagForRender()
                && entity.getDistanceSq(viewer) < DISTANCE * DISTANCE;
    }

    @SubscribeEvent
    public static void render(RenderWorldLastEvent event) {
        Minecraft mc = Minecraft.getMinecraft();
        Entity painting = shown(mc);
        if (painting == null) {
            return;
        }
        RenderManager rm = mc.getRenderManager();
        if (rm.options == null) {
            return;
        }
        String name = painting.getDisplayName().getFormattedText();
        double x = painting.posX - rm.viewerPosX;
        double y = painting.posY - rm.viewerPosY;
        double z = painting.posZ - rm.viewerPosZ;
        // RenderManager.renderEntity lights an entity with its own brightness, and so its nameplate.
        mc.entityRenderer.enableLightmap();
        int light = painting.getBrightnessForRender();
        OpenGlHelper.setLightmapTextureCoords(OpenGlHelper.lightmapTexUnit, light % 65536, light / 65536);
        GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
        EntityRenderer.drawNameplate(mc.fontRenderer, name, (float) x, (float) y + painting.height + 0.5F, (float) z, 0,
                rm.playerViewY, rm.playerViewX, rm.options.thirdPersonView == 2, false);
        mc.entityRenderer.disableLightmap();
        lastDrawn = name;
        drawnCount++;
    }
}
