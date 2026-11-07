package com.exiledradio.trickytrialsbackport.crafter.client;

import com.exiledradio.trickytrialsbackport.common.Content;
import com.exiledradio.trickytrialsbackport.crafter.ContainerCrafter;
import com.exiledradio.trickytrialsbackport.crafter.TileEntityCrafter;

import net.minecraft.client.gui.inventory.GuiContainer;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.RenderHelper;
import net.minecraft.client.resources.I18n;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.inventory.InventoryCrafting;
import net.minecraft.item.ItemStack;
import net.minecraft.item.crafting.CraftingManager;
import net.minecraft.item.crafting.IRecipe;
import net.minecraft.util.ResourceLocation;

/**
 * 1.21's crafter screen: its background, a red mark on switched-off slots, the redstone dot lit while
 * powered, and the result the grid would make, shown at (134, 35) but never taken.
 */
public final class GuiCrafter extends GuiContainer {

    static final ResourceLocation BACKGROUND = Content.id("textures/gui/container/crafter.png");
    static final ResourceLocation DISABLED = Content.id("textures/gui/sprites/container/crafter/disabled_slot.png");
    static final ResourceLocation POWERED = Content.id("textures/gui/sprites/container/crafter/powered_redstone.png");
    static final ResourceLocation UNPOWERED = Content.id("textures/gui/sprites/container/crafter/unpowered_redstone.png");

    private final ContainerCrafter container;
    private final InventoryPlayer player;

    public GuiCrafter(InventoryPlayer player, ContainerCrafter container) {
        super(container);
        this.container = container;
        this.player = player;
        this.xSize = 176;
        this.ySize = 166;
    }

    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        drawDefaultBackground();
        super.drawScreen(mouseX, mouseY, partialTicks);
        renderHoveredToolTip(mouseX, mouseY);
    }

    @Override
    protected void drawGuiContainerForegroundLayer(int mouseX, int mouseY) {
        String title = I18n.format("container.trickytrialsbackport.crafter");
        this.fontRenderer.drawString(title, (this.xSize - this.fontRenderer.getStringWidth(title)) / 2, 6, 0x404040);
        this.fontRenderer.drawString(this.player.getDisplayName().getUnformattedText(), 8, this.ySize - 94, 0x404040);
        ItemStack result = preview();
        if (!result.isEmpty()) {
            RenderHelper.enableGUIStandardItemLighting();
            this.itemRender.renderItemAndEffectIntoGUI(result, 134, 35);
            this.itemRender.renderItemOverlayIntoGUI(this.fontRenderer, result, 134, 35, null);
            RenderHelper.disableStandardItemLighting();
        }
    }

    /** What the grid as shown would make. */
    private ItemStack preview() {
        InventoryCrafting inv = new InventoryCrafting(this.container, 3, 3);
        for (int i = 0; i < TileEntityCrafter.SLOTS; i++) {
            inv.setInventorySlotContents(i, this.container.getSlot(i).getStack().copy());
        }
        IRecipe recipe = CraftingManager.findMatchingRecipe(inv, this.mc.world);
        return recipe == null ? ItemStack.EMPTY : recipe.getCraftingResult(inv);
    }

    @Override
    protected void drawGuiContainerBackgroundLayer(float partialTicks, int mouseX, int mouseY) {
        GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
        this.mc.getTextureManager().bindTexture(BACKGROUND);
        drawTexturedModalRect(this.guiLeft, this.guiTop, 0, 0, this.xSize, this.ySize);
        for (int i = 0; i < TileEntityCrafter.SLOTS; i++) {
            if (this.container.disabled(i)) {
                this.mc.getTextureManager().bindTexture(DISABLED);
                drawModalRectWithCustomSizedTexture(this.guiLeft + 25 + (i % 3) * 18, this.guiTop + 16 + (i / 3) * 18, 0, 0, 18, 18, 18, 18);
            }
        }
        this.mc.getTextureManager().bindTexture(this.container.powered() ? POWERED : UNPOWERED);
        drawModalRectWithCustomSizedTexture(this.guiLeft + 97, this.guiTop + 35, 0, 0, 16, 16, 16, 16);
    }
}
