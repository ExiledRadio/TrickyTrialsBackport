package com.exiledradio.trickytrialsbackport.lookfeel.client;

import com.exiledradio.backportcore.presentation.LoadingStatus;
import com.exiledradio.backportcore.presentation.SpawnProgress;
import com.exiledradio.backportcore.presentation.client.LoadingScreens;
import com.exiledradio.backportcore.presentation.client.PanoramaRenderer;
import com.exiledradio.trickytrialsbackport.TrickyTrialsBackport;
import com.exiledradio.trickytrialsbackport.lookfeel.Rules;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.renderer.BufferBuilder;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.client.resources.I18n;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import javax.annotation.Nullable;

/**
 * 1.21.9's world-loading screen ({@code LevelLoadingScreen}), Backport Core's {@code SPAWN_VIEW_1_21_9} revision:
 * the panorama with 1.20.5's dark menu tile over it, "Loading terrain...", a 200 by 2 progress bar under it (black,
 * filled green), and the chunks being prepared as a 33 by 33 grid of 2 by 2 cells below, in 1.21.9's status colors.
 *
 * <p>1.12.2 prepares the area around the world spawn before the player joins, so the grid is centered there (Core's
 * {@code SpawnProgress}); the progress is 1.12.2's own count of that area, eased as 1.21.9 eases it (a fifth of the
 * way each tick). The panorama is drawn sharp: 1.20.5's blur is not here.
 */
@SideOnly(Side.CLIENT)
public final class LoadingScreen1219 implements LoadingScreens.Drawer {

    private static final ResourceLocation MENU_BACKGROUND = new ResourceLocation(TrickyTrialsBackport.MOD_ID, "textures/gui/menu_background.png");

    /** For a look in game: frames drawn. */
    public static volatile int frames;

    private float smoothed;
    private long last = -1L;

    @Override
    public void draw(Minecraft mc, int width, int height, LoadingScreens.Frame frame) {
        // 1.12.2 draws this from launchIntegratedServer's wait loop, not as a screen; the screen (or none) only keys
        // the panorama's clock.
        PanoramaRenderer.render(mc.currentScreen);
        drawMenuBackground(mc, width, height);
        long now = Minecraft.getSystemTime();
        float ticks = this.last < 0 ? 0.0F : Math.min(20.0F, (now - this.last) / 50.0F);
        this.last = now;
        float target = Math.max(0, Math.min(100, frame.percent)) / 100.0F;
        this.smoothed += (target - this.smoothed) * (1.0F - (float) Math.pow(0.8, ticks));

        int x = width / 2;
        int y = height / 2;
        boolean grid = frame.preparingSpawn;
        if (grid) {
            drawGrid(x, y);
        }
        int textY = Rules.loadingTextY(height, grid);
        String text = I18n.format("multiplayer.downloadingTerrain");
        mc.fontRenderer.drawStringWithShadow(text, x - mc.fontRenderer.getStringWidth(text) / 2.0F, textY, 0xFFFFFF);
        if (grid) {
            int barX = x - 100;
            int barY = textY + 9 + 3;
            Gui.drawRect(barX, barY, barX + 200, barY + 2, 0xFF000000);
            Gui.drawRect(barX, barY, barX + Math.round(this.smoothed * 200), barY + 2, 0xFF00FF00);
        } else {
            this.smoothed = 0.0F;
        }
        frames++;
    }

    /** 1.20.5's {@code Screen.renderMenuBackground}: the 32 by 32 dark tile over the whole screen. */
    private static void drawMenuBackground(Minecraft mc, int width, int height) {
        mc.getTextureManager().bindTexture(MENU_BACKGROUND);
        GlStateManager.enableBlend();
        GlStateManager.tryBlendFuncSeparate(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA,
                GlStateManager.SourceFactor.ONE, GlStateManager.DestFactor.ZERO);
        GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
        Gui.drawModalRectWithCustomSizedTexture(0, 0, 0.0F, 0.0F, width, height, 32.0F, 32.0F);
        GlStateManager.disableBlend();
    }

    /** {@code LevelLoadingScreen.renderChunks} with scale 2 and spacing 0. */
    private static void drawGrid(int centerX, int centerY) {
        int radius = Rules.LOADING_VIEW_RADIUS;
        int side = radius * 2 + 1;
        int x0 = centerX - side * 2 / 2;
        int y0 = centerY - side * 2 / 2;
        GlStateManager.disableTexture2D();
        GlStateManager.disableBlend();
        Tessellator tessellator = Tessellator.getInstance();
        BufferBuilder buffer = tessellator.getBuffer();
        buffer.begin(7, DefaultVertexFormats.POSITION_COLOR);
        for (int gx = 0; gx < side; gx++) {
            for (int gz = 0; gz < side; gz++) {
                int rgb = color(SpawnProgress.status(gx - radius, gz - radius));
                int r = rgb >> 16 & 255;
                int g = rgb >> 8 & 255;
                int b = rgb & 255;
                int left = x0 + gx * 2;
                int top = y0 + gz * 2;
                buffer.pos(left, top + 2, 0.0D).color(r, g, b, 255).endVertex();
                buffer.pos(left + 2, top + 2, 0.0D).color(r, g, b, 255).endVertex();
                buffer.pos(left + 2, top, 0.0D).color(r, g, b, 255).endVertex();
                buffer.pos(left, top, 0.0D).color(r, g, b, 255).endVertex();
            }
        }
        tessellator.draw();
        GlStateManager.enableTexture2D();
    }

    /**
     * 1.21.9's {@code LevelLoadingScreen.COLORS} for Core's statuses. 1.12.2's chunk steps map to generated (1.21's
     * carvers color), populated (features), ready to light (light) and full; a chunk not yet loaded is black.
     */
    static int color(@Nullable LoadingStatus status) {
        if (status == null) {
            return 0x000000;
        }
        switch (status) {
            case EMPTY:
                return 0x545454;
            case STRUCTURE_STARTS:
                return 0x999999;
            case STRUCTURE_REFERENCES:
                return 0x5F6191;
            case BIOMES:
                return 0x80B252;
            case NOISE:
                return 0xD1D1D1;
            case SURFACE:
                return 0x726809;
            case CARVERS:
            case LIQUID_CARVERS:
                return 0x303572;
            case FEATURES:
                return 0x21C600;
            case LIGHT:
                return 0xFFE0A0;
            case SPAWN:
                return 0xF26060;
            default:
                return 0xFFFFFF;
        }
    }
}
