package com.exiledradio.trickytrialsbackport.bundle.client;

import com.exiledradio.trickytrialsbackport.TrickyTrialsBackport;
import com.exiledradio.trickytrialsbackport.bundle.BundleContents;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.RenderHelper;
import net.minecraft.client.renderer.RenderItem;
import net.minecraft.client.resources.I18n;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.text.TextFormatting;
import net.minecraftforge.fml.client.config.GuiUtils;
import org.apache.commons.lang3.math.Fraction;

import java.util.List;

/**
 * The bundle's tooltip image: 1.21.11's {@code ClientBundleTooltip}, every size and position its own.
 *
 * <p>1.12.2's tooltips are text lines only, so the bundle's tooltip gets blank placeholder lines right after
 * its name (where vanilla puts the image), as many as the image is tall, each wide enough for the 96-pixel
 * grid; when the tooltip is drawn the image is drawn over them.
 */
final class BundleTooltip {

    /** Starts every placeholder line: two formatting codes that draw nothing and take no width. */
    static final String MARKER = "§z§z";
    /** 24 spaces: 96 pixels in the default font. */
    private static final String PLACEHOLDER = MARKER + "                        ";

    private static final int SLOT_SIZE = 24;
    private static final int GRID_WIDTH = 96;
    private static final int PROGRESSBAR_HEIGHT = 13;
    private static final int PROGRESSBAR_FILL_MAX = 94;
    private static final int PROGRESSBAR_MARGIN_Y = 4;
    private static final int TEXT_LINE = 10;

    private static final ResourceLocation BORDER = sprite("bundle_progressbar_border");
    private static final ResourceLocation FILL = sprite("bundle_progressbar_fill");
    private static final ResourceLocation FULL = sprite("bundle_progressbar_full");
    private static final ResourceLocation HIGHLIGHT_BACK = sprite("slot_highlight_back");
    private static final ResourceLocation HIGHLIGHT_FRONT = sprite("slot_highlight_front");
    private static final ResourceLocation SLOT_BACKGROUND = sprite("slot_background");

    private BundleTooltip() {
    }

    private static ResourceLocation sprite(String name) {
        return new ResourceLocation(TrickyTrialsBackport.MOD_ID, "textures/gui/sprites/container/bundle/" + name + ".png");
    }

    private static String key(String suffix) {
        return "item." + TrickyTrialsBackport.MOD_ID + ".bundle." + suffix;
    }

    /** Adds the placeholder lines after the name. */
    static void addPlaceholders(ItemStack bundle, List<String> lines, FontRenderer font) {
        int height = height(BundleContents.items(bundle), font);
        int count = (height + TEXT_LINE - 1) / TEXT_LINE;
        int at = Math.min(1, lines.size());
        for (int i = 0; i < count; i++) {
            lines.add(at, PLACEHOLDER);
        }
    }

    /** Vanilla's {@code getHeight}. */
    static int height(List<ItemStack> items, FontRenderer font) {
        if (items.isEmpty()) {
            return descriptionLines(font).size() * 9 + PROGRESSBAR_HEIGHT + 8;
        }
        return gridRows(items.size()) * SLOT_SIZE + PROGRESSBAR_HEIGHT + 8;
    }

    private static List<String> descriptionLines(FontRenderer font) {
        return font.listFormattedStringToWidth(I18n.format(key("empty.description")), GRID_WIDTH);
    }

    static int gridRows(int size) {
        return (Math.min(12, size) + 3) / 4;
    }

    /**
     * Draws the image over its placeholder lines.
     *
     * @param left the tooltip's text left, {@param top} the first placeholder line's top, {@param width} the
     *             tooltip's text width
     */
    static void render(ItemStack bundle, int left, int top, int width, FontRenderer font) {
        List<ItemStack> items = BundleContents.items(bundle);
        Fraction weight = BundleContents.weight(bundle);
        int x = left + (width - GRID_WIDTH) / 2;
        GlStateManager.disableDepth();
        GlStateManager.enableBlend();
        GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
        if (items.isEmpty()) {
            List<String> description = descriptionLines(font);
            for (int i = 0; i < description.size(); i++) {
                font.drawString(description.get(i), x, top + i * 9, 0xFFAAAAAA);
            }
            drawProgressbar(x, top + description.size() * 9 + PROGRESSBAR_MARGIN_Y, items, weight, font);
            return;
        }
        boolean overflowing = items.size() > 12;
        int shownCount = BundleContents.numberOfItemsToShow(items.size());
        List<ItemStack> shown = items.subList(0, Math.min(items.size(), shownCount));
        int rows = gridRows(items.size());
        int right = x + GRID_WIDTH;
        int bottom = top + rows * SLOT_SIZE;
        int selected = BundleContents.selectedItem(bundle);
        int next = 1;
        for (int row = 1; row <= rows; row++) {
            for (int column = 1; column <= 4; column++) {
                int sx = right - column * SLOT_SIZE;
                int sy = bottom - row * SLOT_SIZE;
                if (overflowing && column * row == 1) {
                    int hidden = 0;
                    for (int i = shown.size(); i < items.size(); i++) {
                        hidden += items.get(i).getCount();
                    }
                    drawCentered(font, "+" + hidden, sx + 12, sy + 10, 0xFFFFFFFF);
                } else if (shown.size() >= next) {
                    renderSlot(next, sx, sy, shown, selected, font);
                    next++;
                }
            }
        }
        drawSelectedItemName(bundle, items, selected, left, top, width, font);
        drawProgressbar(x, top + rows * SLOT_SIZE + PROGRESSBAR_MARGIN_Y, items, weight, font);
    }

    private static void renderSlot(int number, int x, int y, List<ItemStack> shown, int selectedIndex, FontRenderer font) {
        int index = shown.size() - number;
        boolean selected = index == selectedIndex;
        ItemStack stack = shown.get(index);
        blit(selected ? HIGHLIGHT_BACK : SLOT_BACKGROUND, x, y, SLOT_SIZE, SLOT_SIZE);
        RenderItem items = Minecraft.getMinecraft().getRenderItem();
        float z = items.zLevel;
        items.zLevel = 400.0F;
        RenderHelper.enableGUIStandardItemLighting();
        GlStateManager.enableDepth();
        items.renderItemAndEffectIntoGUI(stack, x + 4, y + 4);
        items.renderItemOverlayIntoGUI(font, stack, x + 4, y + 4, null);
        RenderHelper.disableStandardItemLighting();
        GlStateManager.disableLighting();
        GlStateManager.disableDepth();
        GlStateManager.enableBlend();
        items.zLevel = z;
        if (selected) {
            blit(HIGHLIGHT_FRONT, x, y, SLOT_SIZE, SLOT_SIZE);
        }
    }

    /** The picked stack's name in its own small tooltip, centered above (vanilla's {@code drawSelectedItemTooltip}). */
    private static void drawSelectedItemName(ItemStack bundle, List<ItemStack> items, int selected, int left, int top, int width,
                                             FontRenderer font) {
        if (selected < 0 || selected >= items.size()) {
            return;
        }
        ItemStack stack = items.get(selected);
        String name = stack.getRarity().color + (stack.hasDisplayName() ? TextFormatting.ITALIC.toString() : "") + stack.getDisplayName();
        int textWidth = font.getStringWidth(name);
        // Vanilla passes (center - 12 - width / 2, top - 15) to the mouse tooltip positioner, which adds (12, -12).
        int tx = left + width / 2 - textWidth / 2;
        int ty = Math.max(4, top - 27);
        int h = 8;
        int bg = 0xF0100010;
        int borderStart = 0x505000FF;
        int borderEnd = (borderStart & 0xFEFEFE) >> 1 | borderStart & 0xFF000000;
        int zLevel = 400;
        GuiUtils.drawGradientRect(zLevel, tx - 3, ty - 4, tx + textWidth + 3, ty - 3, bg, bg);
        GuiUtils.drawGradientRect(zLevel, tx - 3, ty + h + 3, tx + textWidth + 3, ty + h + 4, bg, bg);
        GuiUtils.drawGradientRect(zLevel, tx - 3, ty - 3, tx + textWidth + 3, ty + h + 3, bg, bg);
        GuiUtils.drawGradientRect(zLevel, tx - 4, ty - 3, tx - 3, ty + h + 3, bg, bg);
        GuiUtils.drawGradientRect(zLevel, tx + textWidth + 3, ty - 3, tx + textWidth + 4, ty + h + 3, bg, bg);
        GuiUtils.drawGradientRect(zLevel, tx - 3, ty - 3 + 1, tx - 3 + 1, ty + h + 3 - 1, borderStart, borderEnd);
        GuiUtils.drawGradientRect(zLevel, tx + textWidth + 2, ty - 3 + 1, tx + textWidth + 3, ty + h + 3 - 1, borderStart, borderEnd);
        GuiUtils.drawGradientRect(zLevel, tx - 3, ty - 3, tx + textWidth + 3, ty - 3 + 1, borderStart, borderStart);
        GuiUtils.drawGradientRect(zLevel, tx - 3, ty + h + 2, tx + textWidth + 3, ty + h + 3, borderEnd, borderEnd);
        font.drawStringWithShadow(name, tx, ty, 0xFFFFFFFF);
        GlStateManager.enableBlend();
        GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
    }

    private static void drawProgressbar(int x, int y, List<ItemStack> items, Fraction weight, FontRenderer font) {
        boolean full = weight.compareTo(Fraction.ONE) >= 0;
        int fill = Math.max(0, Math.min(PROGRESSBAR_FILL_MAX, BundleContents.mulAndTruncate(weight, PROGRESSBAR_FILL_MAX)));
        nineSlice(full ? FULL : FILL, 6, 2, x + 1, y, fill, PROGRESSBAR_HEIGHT);
        nineSlice(BORDER, 12, 2, x, y, GRID_WIDTH, PROGRESSBAR_HEIGHT);
        String text = items.isEmpty() ? I18n.format(key("empty")) : full ? I18n.format(key("full")) : null;
        if (text != null) {
            drawCentered(font, text, x + 48, y + 3, 0xFFFFFFFF);
        }
    }

    private static void drawCentered(FontRenderer font, String text, int centerX, int y, int color) {
        font.drawStringWithShadow(text, centerX - font.getStringWidth(text) / 2, y, color);
        GlStateManager.enableBlend();
        GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
    }

    private static void blit(ResourceLocation texture, int x, int y, int w, int h) {
        Minecraft.getMinecraft().getTextureManager().bindTexture(texture);
        GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
        Gui.drawModalRectWithCustomSizedTexture(x, y, 0, 0, w, h, w, h);
    }

    /** A square sprite drawn as 1.21's {@code nine_slice} scaling does, corners kept, edges and middle stretched. */
    private static void nineSlice(ResourceLocation texture, int size, int border, int x, int y, int w, int h) {
        if (w <= 0 || h <= 0) {
            return;
        }
        Minecraft.getMinecraft().getTextureManager().bindTexture(texture);
        GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
        int bx = Math.min(border, w / 2);
        int by = Math.min(border, h / 2);
        int[] dx = {x, x + bx, x + w - bx};
        int[] dw = {bx, w - 2 * bx, bx};
        int[] dy = {y, y + by, y + h - by};
        int[] dh = {by, h - 2 * by, by};
        int[] u = {0, border, size - border};
        int[] uw = {border, size - 2 * border, border};
        for (int i = 0; i < 3; i++) {
            for (int j = 0; j < 3; j++) {
                if (dw[i] > 0 && dh[j] > 0) {
                    Gui.drawScaledCustomSizeModalRect(dx[i], dy[j], u[i], u[j], uw[i], uw[j], dw[i], dh[j], size, size);
                }
            }
        }
    }
}
