package com.exiledradio.trickytrialsbackport.locator.client;

import com.exiledradio.trickytrialsbackport.ModConfig;
import com.exiledradio.trickytrialsbackport.TrickyTrialsBackport;
import com.exiledradio.trickytrialsbackport.locator.BarMath;
import com.exiledradio.trickytrialsbackport.locator.Waypoints;

import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.renderer.EntityRenderer;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.client.event.RenderGameOverlayEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.common.ObfuscationReflectionHelper;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

/**
 * 1.21.6's locator bar ({@code LocatorBarRenderer} and {@code Gui.nextContextualInfoState}), drawn in place of the
 * experience bar (or the horse's jump bar) while the server has sent other players' waypoints:
 *
 * <ul>
 *   <li>the bar's background, with the experience level above it as on the experience bar;</li>
 *   <li>a dot per player within 60 degrees of the view, in the team's color or one from the player's UUID, farthest
 *       first; the dot fades with distance (four sprites, 128 to 332 blocks);</li>
 *   <li>a blinking arrow above or below a dot whose player is off the top or bottom of the screen.</li>
 * </ul>
 * The experience bar comes back for five seconds after experience changes, and the jump bar while charging a jump.
 */
@SideOnly(Side.CLIENT)
public final class LocatorBarHud {

    private static final String SPRITES = "textures/gui/sprites/hud/";
    private static final ResourceLocation BACKGROUND = new ResourceLocation(TrickyTrialsBackport.MOD_ID, SPRITES + "locator_bar_background.png");
    private static final ResourceLocation ARROW_UP = new ResourceLocation(TrickyTrialsBackport.MOD_ID, SPRITES + "locator_bar_arrow_up.png");
    private static final ResourceLocation ARROW_DOWN = new ResourceLocation(TrickyTrialsBackport.MOD_ID, SPRITES + "locator_bar_arrow_down.png");
    private static final ResourceLocation[] DOTS = new ResourceLocation[BarMath.SPRITES];

    static {
        for (int i = 0; i < DOTS.length; i++) {
            DOTS[i] = new ResourceLocation(TrickyTrialsBackport.MOD_ID, SPRITES + "locator_bar_dot/default_" + i + ".png");
        }
    }

    /** {@code LocalPlayer.experienceDisplayStartTick}'s two waiting values. */
    private static final int UNREADY = Integer.MIN_VALUE;

    /** For a look in game: frames the bar was drawn. */
    public static volatile int frames;

    private EntityPlayerSP player;
    private int level;
    private float progress;
    private int displayStart = UNREADY;
    private Method fovMethod;

    /**
     * {@code LocalPlayer.setExperienceValues}: a change shows the experience bar for 100 ticks. The values the server
     * sends as a player joins or respawns (its first 20 ticks) do not, as vanilla skips the first packet.
     */
    @SubscribeEvent
    public void tick(TickEvent.ClientTickEvent event) {
        EntityPlayerSP p = Minecraft.getMinecraft().player;
        if (event.phase != TickEvent.Phase.END || p == null) {
            return;
        }
        if (p != this.player) {
            this.player = p;
            this.displayStart = UNREADY;
        } else if (p.experienceLevel != this.level || p.experience != this.progress) {
            this.displayStart = p.ticksExisted < 20 ? UNREADY : p.ticksExisted;
        }
        this.level = p.experienceLevel;
        this.progress = p.experience;
    }

    private boolean showLocator(EntityPlayerSP p) {
        if (!ModConfig.locatorBar.enabled || !ClientWaypoints.hasWaypoints()) {
            return false;
        }
        if (p.isRidingHorse() && p.getHorseJumpPower() > 0.0F) {
            return false;
        }
        boolean experience = Minecraft.getMinecraft().playerController.gameIsSurvivalOrAdventure();
        return !(experience && this.displayStart != UNREADY && this.displayStart + 100 > p.ticksExisted);
    }

    @SubscribeEvent
    public void overlay(RenderGameOverlayEvent.Pre event) {
        RenderGameOverlayEvent.ElementType type = event.getType();
        if (type != RenderGameOverlayEvent.ElementType.EXPERIENCE && type != RenderGameOverlayEvent.ElementType.JUMPBAR) {
            return;
        }
        Minecraft mc = Minecraft.getMinecraft();
        EntityPlayerSP p = mc.player;
        if (p == null || !showLocator(p)) {
            return;
        }
        event.setCanceled(true);
        int width = event.getResolution().getScaledWidth();
        int height = event.getResolution().getScaledHeight();
        int left = (width - 182) / 2;
        int top = height - 24 - 5;
        GlStateManager.enableBlend();
        GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
        background(mc, left, top);
        if (mc.playerController.gameIsSurvivalOrAdventure() && p.experienceLevel > 0) {
            level(mc.fontRenderer, width, height, p.experienceLevel);
        }
        GlStateManager.enableBlend();
        dots(mc, width, top, event.getPartialTicks());
        GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
        GlStateManager.disableBlend();
        mc.getTextureManager().bindTexture(Gui.ICONS);
        frames++;
        MinecraftForge.EVENT_BUS.post(new RenderGameOverlayEvent.Post(event, type));
    }

    /** {@code hud/locator_bar_background}: a 12 x 5 nine-slice sprite (5 pixels each end, the middle tiled) at 182 x 5. */
    private static void background(Minecraft mc, int left, int top) {
        mc.getTextureManager().bindTexture(BACKGROUND);
        Gui.drawModalRectWithCustomSizedTexture(left, top, 0.0F, 0.0F, 5, 5, 12.0F, 5.0F);
        for (int x = 5; x < 177; x += 2) {
            Gui.drawModalRectWithCustomSizedTexture(left + x, top, 5.0F, 0.0F, Math.min(2, 177 - x), 5, 12.0F, 5.0F);
        }
        Gui.drawModalRectWithCustomSizedTexture(left + 177, top, 7.0F, 0.0F, 5, 5, 12.0F, 5.0F);
    }

    /** {@code ContextualBarRenderer.renderExperienceLevel}: as 1.12.2's level text. */
    private static void level(FontRenderer font, int width, int height, int level) {
        String text = Integer.toString(level);
        int x = (width - font.getStringWidth(text)) / 2;
        int y = height - 24 - 9 - 2;
        font.drawString(text, x + 1, y, 0);
        font.drawString(text, x - 1, y, 0);
        font.drawString(text, x, y + 1, 0);
        font.drawString(text, x, y - 1, 0);
        font.drawString(text, x, y, 0x80FF20);
    }

    private void dots(Minecraft mc, int width, int top, float partial) {
        Entity camera = mc.getRenderViewEntity();
        if (camera == null || mc.world == null) {
            return;
        }
        boolean front = mc.gameSettings.thirdPersonView == 2;
        float yaw = camera.prevRotationYaw + (camera.rotationYaw - camera.prevRotationYaw) * partial + (front ? 180.0F : 0.0F);
        float pitch = camera.prevRotationPitch + (camera.rotationPitch - camera.prevRotationPitch) * partial;
        if (front) {
            pitch = -pitch;
        }
        double cx = camera.prevPosX + (camera.posX - camera.prevPosX) * partial;
        double cy = camera.prevPosY + (camera.posY - camera.prevPosY) * partial + camera.getEyeHeight();
        double cz = camera.prevPosZ + (camera.posZ - camera.prevPosZ) * partial;
        float fov = fov(mc, partial);
        // ClientWaypointManager.forEachWaypoint: farthest first, so the nearest is drawn on top.
        List<ClientWaypoints.Tracked> list = new ArrayList<>(ClientWaypoints.WAYPOINTS.values());
        list.sort((a, b) -> Double.compare(distanceSq(b, camera), distanceSq(a, camera)));
        int arrowFrame = mc.ingameGUI.getUpdateCounter() % 14 < 10 ? 0 : 1;
        for (ClientWaypoints.Tracked w : list) {
            if (w.id.equals(camera.getUniqueID())) {
                continue;
            }
            double angle;
            int arrow;
            if (w.type == Waypoints.AZIMUTH) {
                angle = BarMath.yawToAzimuth(yaw, w.angle);
                arrow = BarMath.pitchFromHorizon(BarMath.horizon(pitch, fov));
            } else if (w.type == Waypoints.CHUNK) {
                double px = (w.x << 4) + 8.5D;
                double pz = (w.z << 4) + 8.5D;
                angle = BarMath.yawToPoint(cx, cz, yaw, px, pz);
                arrow = BarMath.pitchFromHorizon(BarMath.horizon(pitch, fov));
            } else if (w.type == Waypoints.BLOCK) {
                double[] at = blockTarget(mc, w, partial);
                angle = BarMath.yawToPoint(cx, cz, yaw, at[0], at[2]);
                arrow = BarMath.pitchToPoint(at[0] - cx, at[1] - cy, at[2] - cz, yaw, pitch, fov);
            } else {
                continue;
            }
            if (!BarMath.visible(angle)) {
                continue;
            }
            int x = BarMath.dotX(width, angle);
            int color = w.color >= 0 ? w.color : Waypoints.idColor(w.id);
            float distance = MathHelper.sqrt(distanceSq(w, camera));
            GlStateManager.color((color >> 16 & 255) / 255.0F, (color >> 8 & 255) / 255.0F, (color & 255) / 255.0F, 1.0F);
            mc.getTextureManager().bindTexture(DOTS[BarMath.sprite(distance)]);
            Gui.drawModalRectWithCustomSizedTexture(x, top - 2, 0.0F, 0.0F, 9, 9, 9.0F, 9.0F);
            GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
            if (arrow != BarMath.NONE) {
                mc.getTextureManager().bindTexture(arrow == BarMath.DOWN ? ARROW_DOWN : ARROW_UP);
                // The arrows are animated (two 7 x 5 frames, 10 and 4 ticks).
                Gui.drawModalRectWithCustomSizedTexture(x + 1, top + (arrow == BarMath.DOWN ? 6 : -6), 0.0F, arrowFrame * 5.0F, 7, 5, 7.0F, 10.0F);
            }
        }
    }

    /**
     * {@code Vec3iWaypoint.position}: the player's eyes while it is loaded within 3 blocks (Manhattan) of the sent
     * position, else the block's center.
     */
    private static double[] blockTarget(Minecraft mc, ClientWaypoints.Tracked w, float partial) {
        EntityPlayer target = mc.world.getPlayerEntityByUUID(w.id);
        if (target != null) {
            int bx = MathHelper.floor(target.posX);
            int by = MathHelper.floor(target.posY);
            int bz = MathHelper.floor(target.posZ);
            if (Math.abs(bx - w.x) + Math.abs(by - w.y) + Math.abs(bz - w.z) <= 3) {
                return new double[]{target.prevPosX + (target.posX - target.prevPosX) * partial,
                        target.prevPosY + (target.posY - target.prevPosY) * partial + target.getEyeHeight(),
                        target.prevPosZ + (target.posZ - target.prevPosZ) * partial};
            }
        }
        return new double[]{w.x + 0.5D, w.y + 0.5D, w.z + 0.5D};
    }

    /** {@code TrackedWaypoint.distanceSquared}: infinite for an angle; a chunk's middle at the entity's height. */
    private static double distanceSq(ClientWaypoints.Tracked w, Entity e) {
        if (w.type == Waypoints.BLOCK) {
            return e.getDistanceSq(w.x + 0.5D, w.y + 0.5D, w.z + 0.5D);
        }
        if (w.type == Waypoints.CHUNK) {
            return e.getDistanceSq((w.x << 4) + 8.5D, MathHelper.floor(e.posY) + 0.5D, (w.z << 4) + 8.5D);
        }
        return Double.POSITIVE_INFINITY;
    }

    /** {@code GameRenderer.getFov(camera, partial, true)}: 1.12.2's {@code EntityRenderer.getFOVModifier}. */
    private float fov(Minecraft mc, float partial) {
        try {
            if (this.fovMethod == null) {
                this.fovMethod = ObfuscationReflectionHelper.findMethod(EntityRenderer.class, "func_78481_a", float.class, float.class, boolean.class);
            }
            return (Float) this.fovMethod.invoke(mc.entityRenderer, partial, true);
        } catch (Exception e) {
            return mc.gameSettings.fovSetting;
        }
    }
}
