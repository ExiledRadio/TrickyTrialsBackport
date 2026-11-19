package com.exiledradio.trickytrialsbackport.nautilus.client;

import com.exiledradio.trickytrialsbackport.common.Content;
import com.exiledradio.trickytrialsbackport.nautilus.ContainerNautilus;
import com.exiledradio.trickytrialsbackport.nautilus.EntityAbstractNautilus;

import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.inventory.GuiContainer;
import net.minecraft.client.gui.inventory.GuiInventory;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.util.ResourceLocation;

/**
 * 1.21.11's {@code NautilusInventoryScreen}: its background, the slot frames and empty-slot icons for the saddle and
 * the armor (only for a tamed adult), and the nautilus following the mouse.
 */
public class GuiNautilus extends GuiContainer {

    static final ResourceLocation BACKGROUND = Content.id("textures/gui/container/nautilus.png");
    static final ResourceLocation SLOT = Content.id("textures/gui/sprites/container/slot.png");
    static final ResourceLocation SADDLE_ICON = Content.id("textures/gui/sprites/container/slot/saddle.png");
    static final ResourceLocation ARMOR_ICON = Content.id("textures/gui/sprites/container/slot/nautilus_armor_inventory.png");

    private final InventoryPlayer playerInventory;
    private final EntityAbstractNautilus nautilus;
    private float mouseX;
    private float mouseY;

    public GuiNautilus(InventoryPlayer playerInventory, EntityAbstractNautilus nautilus) {
        super(new ContainerNautilus(playerInventory, nautilus));
        this.playerInventory = playerInventory;
        this.nautilus = nautilus;
        this.xSize = 176;
        this.ySize = 166;
    }

    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        this.mouseX = mouseX;
        this.mouseY = mouseY;
        this.drawDefaultBackground();
        super.drawScreen(mouseX, mouseY, partialTicks);
        this.renderHoveredToolTip(mouseX, mouseY);
    }

    @Override
    protected void drawGuiContainerForegroundLayer(int mouseX, int mouseY) {
        this.fontRenderer.drawString(this.nautilus.getDisplayName().getFormattedText(), 8, 6, 0x404040);
        this.fontRenderer.drawString(this.playerInventory.getDisplayName().getFormattedText(), 8, this.ySize - 96 + 2, 0x404040);
    }

    @Override
    protected void drawGuiContainerBackgroundLayer(float partialTicks, int mouseX, int mouseY) {
        GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
        int x = (this.width - this.xSize) / 2;
        int y = (this.height - this.ySize) / 2;
        this.mc.getTextureManager().bindTexture(BACKGROUND);
        this.drawTexturedModalRect(x, y, 0, 0, this.xSize, this.ySize);
        if (this.nautilus.canUseEquipment()) {
            GlStateManager.enableBlend();
            this.mc.getTextureManager().bindTexture(SLOT);
            Gui.drawModalRectWithCustomSizedTexture(x + 7, y + 17, 0, 0, 18, 18, 18, 18);
            Gui.drawModalRectWithCustomSizedTexture(x + 7, y + 35, 0, 0, 18, 18, 18, 18);
            if (this.nautilus.getSaddle().isEmpty()) {
                this.mc.getTextureManager().bindTexture(SADDLE_ICON);
                Gui.drawModalRectWithCustomSizedTexture(x + 8, y + 18, 0, 0, 16, 16, 16, 16);
            }
            if (this.nautilus.inventory.getStackInSlot(EntityAbstractNautilus.SLOT_ARMOR).isEmpty()) {
                this.mc.getTextureManager().bindTexture(ARMOR_ICON);
                Gui.drawModalRectWithCustomSizedTexture(x + 8, y + 36, 0, 0, 16, 16, 16, 16);
            }
        }
        GuiInventory.drawEntityOnScreen(x + 51, y + 60, 17, x + 51 - this.mouseX, y + 75 - 50 - this.mouseY, this.nautilus);
    }
}
