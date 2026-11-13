package com.exiledradio.trickytrialsbackport.palegarden.wood.client;

import com.exiledradio.trickytrialsbackport.TrickyTrialsBackport;
import com.exiledradio.trickytrialsbackport.palegarden.wood.WoodKind;
import com.exiledradio.trickytrialsbackport.palegarden.wood.WoodSign;

import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.gui.GuiUtilRenderComponents;
import net.minecraft.client.model.ModelSign;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.tileentity.TileEntitySpecialRenderer;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.text.ITextComponent;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Draws the signs of this mod's woods: vanilla's sign model with the wood's 1.20 texture
 * ({@code textures/entity/signs/<wood>.png}, the same 64x32 layout as 1.12.2's sign).
 *
 * <p>Vanilla's renderer cannot be reused: it tells standing from wall signs by comparing the block with
 * {@code Blocks.STANDING_SIGN} and binds its one texture. Text color: a sign dyed through Village &
 * Pillage's sign dyeing keeps its color in the tile's Forge data under {@code villagerbackport:Color};
 * this reads that key, so dyeing shows on these signs whether or not that mod is installed now.
 */
public class WoodSignRenderer extends TileEntitySpecialRenderer<WoodSign.Tile> {

    private static final String DYE_KEY = "villagerbackport:Color";

    /** 1.14's sign text colors by dye name; drawn at 40%, as 1.14 draws them. */
    private static final Map<String, Integer> TEXT_COLORS = new HashMap<String, Integer>();

    static {
        String[] names = {"white", "orange", "magenta", "light_blue", "yellow", "lime", "pink", "gray", "light_gray",
                "cyan", "purple", "blue", "brown", "green", "red", "black"};
        int[] colors = {0xFFFFFF, 0xFF681F, 0xFF00FF, 0x9AC0CD, 0xFFFF00, 0xBFFF00, 0xFF69B4, 0x808080, 0xD3D3D3,
                0x00FFFF, 0xA020F0, 0x0000FF, 0x8B4513, 0x00FF00, 0xFF0000, 0x000000};
        for (int i = 0; i < names.length; i++) {
            TEXT_COLORS.put(names[i], colors[i]);
        }
    }

    private final ModelSign model = new ModelSign();
    private final Map<String, ResourceLocation> textures = new HashMap<String, ResourceLocation>();

    @Override
    public void render(WoodSign.Tile te, double x, double y, double z, float partialTicks, int destroyStage, float alpha) {
        WoodKind kind = WoodSign.kindOf(te.getBlockType());
        if (kind == null) {
            return;
        }
        boolean standing = te.getBlockType() instanceof WoodSign.Standing;
        GlStateManager.pushMatrix();
        GlStateManager.translate((float) x + 0.5F, (float) y + 0.5F, (float) z + 0.5F);
        if (standing) {
            GlStateManager.rotate(-(te.getBlockMetadata() * 360) / 16.0F, 0.0F, 1.0F, 0.0F);
            this.model.signStick.showModel = true;
        } else {
            int facing = te.getBlockMetadata();
            float angle = facing == 2 ? 180.0F : facing == 4 ? 90.0F : facing == 5 ? -90.0F : 0.0F;
            GlStateManager.rotate(-angle, 0.0F, 1.0F, 0.0F);
            GlStateManager.translate(0.0F, -0.3125F, -0.4375F);
            this.model.signStick.showModel = false;
        }

        if (destroyStage >= 0) {
            bindTexture(DESTROY_STAGES[destroyStage]);
            GlStateManager.matrixMode(5890);
            GlStateManager.pushMatrix();
            GlStateManager.scale(4.0F, 2.0F, 1.0F);
            GlStateManager.translate(0.0625F, 0.0625F, 0.0625F);
            GlStateManager.matrixMode(5888);
        } else {
            bindTexture(texture(kind));
        }

        GlStateManager.enableRescaleNormal();
        GlStateManager.pushMatrix();
        GlStateManager.scale(0.6666667F, -0.6666667F, -0.6666667F);
        this.model.renderSign();
        GlStateManager.popMatrix();

        FontRenderer font = getFontRenderer();
        GlStateManager.translate(0.0F, 0.33333334F, 0.046666667F);
        GlStateManager.scale(0.010416667F, -0.010416667F, 0.010416667F);
        GlStateManager.glNormal3f(0.0F, 0.0F, -0.010416667F);
        GlStateManager.depthMask(false);
        if (destroyStage < 0) {
            int color = textColor(te);
            for (int line = 0; line < te.signText.length; line++) {
                if (te.signText[line] == null) {
                    continue;
                }
                List<ITextComponent> split = GuiUtilRenderComponents.splitText(te.signText[line], 90, font, false, true);
                String s = split != null && !split.isEmpty() ? split.get(0).getFormattedText() : "";
                if (line == te.lineBeingEdited) {
                    s = "> " + s + " <";
                }
                font.drawString(s, -font.getStringWidth(s) / 2, line * 10 - te.signText.length * 5, color);
            }
        }
        GlStateManager.depthMask(true);
        GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
        GlStateManager.popMatrix();

        if (destroyStage >= 0) {
            GlStateManager.matrixMode(5890);
            GlStateManager.popMatrix();
            GlStateManager.matrixMode(5888);
        }
    }

    private ResourceLocation texture(WoodKind kind) {
        ResourceLocation t = this.textures.get(kind.name);
        if (t == null) {
            t = new ResourceLocation(TrickyTrialsBackport.MOD_ID, "textures/entity/signs/" + kind.name + ".png");
            this.textures.put(kind.name, t);
        }
        return t;
    }

    private static int textColor(WoodSign.Tile te) {
        Integer full = TEXT_COLORS.get(te.getTileData().getString(DYE_KEY));
        if (full == null || full == 0) {
            return 0;
        }
        int r = (int) (((full >> 16) & 255) * 0.4D);
        int g = (int) (((full >> 8) & 255) * 0.4D);
        int b = (int) ((full & 255) * 0.4D);
        return r << 16 | g << 8 | b;
    }
}
