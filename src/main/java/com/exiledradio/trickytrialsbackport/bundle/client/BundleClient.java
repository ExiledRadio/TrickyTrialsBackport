package com.exiledradio.trickytrialsbackport.bundle.client;

import com.exiledradio.trickytrialsbackport.TrickyTrialsBackport;
import com.exiledradio.trickytrialsbackport.bundle.BundleClickMessage;
import com.exiledradio.trickytrialsbackport.bundle.BundleClicks;
import com.exiledradio.trickytrialsbackport.bundle.BundleContent;
import com.exiledradio.trickytrialsbackport.bundle.BundleContents;
import com.exiledradio.trickytrialsbackport.bundle.BundleSelectMessage;
import com.exiledradio.trickytrialsbackport.bundle.ItemBundle;
import com.exiledradio.trickytrialsbackport.network.NetworkHandler;

import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.inventory.GuiContainer;
import net.minecraft.client.gui.inventory.GuiContainerCreative;
import net.minecraft.client.settings.KeyBinding;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.inventory.Slot;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.client.event.GuiOpenEvent;
import net.minecraftforge.client.event.GuiScreenEvent;
import net.minecraftforge.client.event.ModelBakeEvent;
import net.minecraftforge.client.event.ModelRegistryEvent;
import net.minecraftforge.client.event.RenderTooltipEvent;
import net.minecraftforge.client.model.ModelLoader;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.entity.player.ItemTooltipEvent;
import net.minecraftforge.fml.client.config.GuiUtils;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import org.lwjgl.input.Keyboard;
import org.lwjgl.input.Mouse;

import java.util.List;
import java.util.Map;

/**
 * The client side of bundles, all through Forge events (no patching).
 *
 * <ul>
 *   <li><b>Clicks</b> (vanilla's {@code AbstractContainerMenu.tryItemClickBehaviourOverride}): a left or right
 *       press on a slot that {@link BundleClicks#decide} calls a bundle click never reaches the screen, nor
 *       its release; the click is played out here and sent to the server. In the creative screen it is
 *       played out here alone, on the player's own inventory slots only, and the inventory sends its
 *       changed slots as every creative click does.</li>
 *   <li><b>The wheel</b> over a bundle picks a stack (vanilla's {@code BundleMouseActions} and
 *       {@code ScrollWheelHandler}); moving off the bundle, shift-clicking it, a hotbar key over it, a plain
 *       click on it and closing the screen clear the pick.</li>
 *   <li><b>The tooltip</b>: {@link BundleTooltip}, also shown while carrying something, as vanilla does.</li>
 *   <li><b>The open look</b>: {@link BundleModels}.</li>
 * </ul>
 */
public final class BundleClient {

    /** Releases to swallow, per button, after a press played out as a bundle click. */
    private final boolean[] swallowRelease = new boolean[2];
    private double accumulatedScroll;
    private GuiContainer lastGui;
    private Slot lastSlot;

    private BundleClient() {
    }

    public static void init() {
        MinecraftForge.EVENT_BUS.register(new BundleClient());
    }

    // ------------------------------------------------------------------------------------------
    // Models
    // ------------------------------------------------------------------------------------------

    @SubscribeEvent
    public void registerModels(ModelRegistryEvent event) {
        for (Map.Entry<String, ItemBundle> entry : BundleContent.BUNDLES.entrySet()) {
            String name = entry.getKey();
            ModelLoader.registerItemVariants(entry.getValue(), new ResourceLocation(TrickyTrialsBackport.MOD_ID, name),
                    new ResourceLocation(TrickyTrialsBackport.MOD_ID, name + "_open_back"),
                    new ResourceLocation(TrickyTrialsBackport.MOD_ID, name + "_open_front"));
            entry.getValue().setTileEntityItemStackRenderer(BundleModels.Renderer.INSTANCE);
        }
    }

    @SubscribeEvent
    public void bakeModels(ModelBakeEvent event) {
        for (Map.Entry<String, ItemBundle> entry : BundleContent.BUNDLES.entrySet()) {
            BundleModels.wrap(event.getModelRegistry(), entry.getValue(), entry.getKey());
        }
    }

    // ------------------------------------------------------------------------------------------
    // Clicks and the wheel
    // ------------------------------------------------------------------------------------------

    @SubscribeEvent
    public void mouse(GuiScreenEvent.MouseInputEvent.Pre event) {
        if (!(event.getGui() instanceof GuiContainer) || BundleContent.BUNDLES.isEmpty()) {
            return;
        }
        GuiContainer gui = (GuiContainer) event.getGui();
        Minecraft mc = Minecraft.getMinecraft();
        EntityPlayerSP player = mc.player;
        if (player == null || mc.getConnection() == null) {
            return;
        }
        int wheel = Mouse.getEventDWheel();
        if (wheel != 0) {
            if (this.scroll(gui, wheel)) {
                event.setCanceled(true);
            }
            return;
        }
        int button = Mouse.getEventButton();
        if (button != 0 && button != 1) {
            return;
        }
        if (!Mouse.getEventButtonState()) {
            if (this.swallowRelease[button]) {
                this.swallowRelease[button] = false;
                event.setCanceled(true);
            }
            return;
        }
        this.swallowRelease[button] = false;
        Slot slot = gui.getSlotUnderMouse();
        if (slot == null || mc.gameSettings.touchscreen || !takesBundleClicks(gui, slot)) {
            return;
        }
        if (GuiScreen.isShiftKeyDown()) {
            // A quick move: vanilla's onSlotClicked clears the pick.
            this.unselect(gui, slot);
            return;
        }
        boolean primary = button == 0;
        BundleClicks.Outcome outcome = BundleClicks.decide(player.inventory.getItemStack(), slot.getStack(), primary);
        if (outcome == BundleClicks.Outcome.UNSELECT) {
            this.unselect(gui, slot);
        } else if (outcome == BundleClicks.Outcome.BUNDLE) {
            BundleClicks.click(slot, primary, player);
            if (gui instanceof GuiContainerCreative) {
                player.inventoryContainer.detectAndSendChanges();
            } else {
                NetworkHandler.channel.sendToServer(new BundleClickMessage(gui.inventorySlots.windowId, slot.slotNumber, primary));
            }
            this.swallowRelease[button] = true;
            event.setCanceled(true);
        }
    }

    /** Every slot of a server-backed screen; in the creative screen, only the player's own inventory. */
    private static boolean takesBundleClicks(GuiContainer gui, Slot slot) {
        return !(gui instanceof GuiContainerCreative) || slot.inventory instanceof InventoryPlayer;
    }

    @SubscribeEvent
    public void keyboard(GuiScreenEvent.KeyboardInputEvent.Pre event) {
        if (!(event.getGui() instanceof GuiContainer) || !Keyboard.getEventKeyState()) {
            return;
        }
        GuiContainer gui = (GuiContainer) event.getGui();
        Slot slot = gui.getSlotUnderMouse();
        if (slot == null) {
            return;
        }
        int key = Keyboard.getEventKey();
        for (KeyBinding binding : Minecraft.getMinecraft().gameSettings.keyBindsHotbar) {
            if (binding.isActiveAndMatches(key)) {
                // A hotbar swap: vanilla's onSlotClicked clears the pick.
                this.unselect(gui, slot);
                return;
            }
        }
    }

    /** Vanilla's {@code BundleMouseActions.onMouseScrolled}. @return whether the wheel was the bundle's */
    private boolean scroll(GuiContainer gui, int wheel) {
        Slot slot = gui.getSlotUnderMouse();
        if (slot == null || !slot.getHasStack() || !BundleContents.isBundle(slot.getStack())) {
            return false;
        }
        ItemStack bundle = slot.getStack();
        int shown = BundleContents.numberOfItemsToShow(BundleContents.items(bundle).size());
        if (shown == 0) {
            return false;
        }
        // One notch is 120 in LWJGL 2 and 1 in some replacements; vanilla counts notches.
        double notches = Math.abs(wheel) >= 120 ? wheel / 120.0D : Math.signum(wheel);
        int amount = this.onMouseScroll(notches);
        if (amount != 0) {
            int current = BundleContents.selectedItem(bundle);
            int next = BundleContents.nextScrollWheelSelection(amount, current, shown);
            if (current != next) {
                this.select(gui, slot, next);
            }
        }
        return true;
    }

    /** Vanilla's {@code ScrollWheelHandler.onMouseScroll}, vertical only. */
    private int onMouseScroll(double amount) {
        if (this.accumulatedScroll != 0.0D && Math.signum(amount) != Math.signum(this.accumulatedScroll)) {
            this.accumulatedScroll = 0.0D;
        }
        this.accumulatedScroll += amount;
        int whole = (int) this.accumulatedScroll;
        this.accumulatedScroll -= whole;
        return whole;
    }

    /** Vanilla's {@code toggleSelectedBundleItem}: picks here and tells the server. */
    private void select(GuiContainer gui, Slot slot, int index) {
        ItemStack bundle = slot.getStack();
        if (index >= BundleContents.numberOfItemsToShow(BundleContents.items(bundle).size())) {
            return;
        }
        BundleContents.toggleSelectedItem(bundle, index);
        this.sendPick(gui, slot, index);
    }

    /** Clears a pick, if there is one. */
    private void unselect(GuiContainer gui, Slot slot) {
        ItemStack bundle = slot.getStack();
        if (BundleContents.isBundle(bundle) && BundleContents.selectedItem(bundle) >= 0) {
            BundleContents.toggleSelectedItem(bundle, -1);
            this.sendPick(gui, slot, -1);
        }
    }

    private void sendPick(GuiContainer gui, Slot slot, int index) {
        if (gui instanceof GuiContainerCreative) {
            if (slot.inventory instanceof InventoryPlayer && Minecraft.getMinecraft().player != null) {
                Minecraft.getMinecraft().player.inventoryContainer.detectAndSendChanges();
            }
        } else if (Minecraft.getMinecraft().getConnection() != null) {
            NetworkHandler.channel.sendToServer(new BundleSelectMessage(gui.inventorySlots.windowId, slot.slotNumber, index));
        }
    }

    // ------------------------------------------------------------------------------------------
    // Hovering and the tooltip
    // ------------------------------------------------------------------------------------------

    @SubscribeEvent
    public void drawn(GuiScreenEvent.DrawScreenEvent.Post event) {
        if (!(event.getGui() instanceof GuiContainer)) {
            return;
        }
        GuiContainer gui = (GuiContainer) event.getGui();
        Slot now = gui.getSlotUnderMouse();
        if (this.lastSlot != null && this.lastSlot != now && this.lastGui == gui) {
            this.unselect(gui, this.lastSlot);
        }
        this.lastGui = gui;
        this.lastSlot = now;
        // Vanilla's showTooltipWithItemInHand: a bundle's tooltip shows while carrying something.
        EntityPlayerSP player = Minecraft.getMinecraft().player;
        if (now != null && now.getHasStack() && BundleContents.isBundle(now.getStack()) && player != null
                && !player.inventory.getItemStack().isEmpty()) {
            ItemStack bundle = now.getStack();
            GuiUtils.preItemToolTip(bundle);
            gui.drawHoveringText(gui.getItemToolTip(bundle), event.getMouseX(), event.getMouseY());
            GuiUtils.postItemToolTip();
        }
    }

    @SubscribeEvent
    public void opened(GuiOpenEvent event) {
        if (this.lastGui != null && this.lastSlot != null && event.getGui() != this.lastGui) {
            this.unselect(this.lastGui, this.lastSlot);
        }
        if (event.getGui() != this.lastGui) {
            this.lastGui = null;
            this.lastSlot = null;
        }
    }

    @SubscribeEvent
    public void tooltip(ItemTooltipEvent event) {
        if (BundleContents.isBundle(event.getItemStack())) {
            BundleTooltip.addPlaceholders(event.getItemStack(), event.getToolTip(), Minecraft.getMinecraft().fontRenderer);
        }
    }

    @SubscribeEvent
    public void tooltipDrawn(RenderTooltipEvent.PostText event) {
        if (!BundleContents.isBundle(event.getStack())) {
            return;
        }
        List<String> lines = event.getLines();
        for (int i = 0; i < lines.size(); i++) {
            if (lines.get(i).startsWith(BundleTooltip.MARKER)) {
                // GuiUtils.drawHoveringText: 10 pixels a line, 2 more after the first.
                int top = event.getY() + i * 10 + (i >= 1 ? 2 : 0);
                BundleTooltip.render(event.getStack(), event.getX(), top, event.getWidth(), event.getFontRenderer());
                return;
            }
        }
    }
}
