package com.exiledradio.trickytrialsbackport.lookfeel.client;

import com.exiledradio.trickytrialsbackport.lookfeel.LookAndFeel;

import net.minecraft.client.Minecraft;
import net.minecraft.util.MouseHelper;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

/**
 * 1.21.9's Invert Mouse X ({@code MouseHandler.turnPlayer} turns by -dx when it is on).
 *
 * <p>1.12.2 turns the player by its {@code MouseHelper}'s delta each frame; the game's helper is wrapped once by one
 * that reads the helper it replaced (vanilla's, or another mod's) and negates the horizontal delta while the option
 * is on. No patch.
 */
@SideOnly(Side.CLIENT)
public final class InvertMouseX {

    @SubscribeEvent
    public void tick(TickEvent.ClientTickEvent event) {
        Minecraft mc = Minecraft.getMinecraft();
        if (event.phase == TickEvent.Phase.START && mc.mouseHelper != null && !(mc.mouseHelper instanceof Helper)) {
            mc.mouseHelper = new Helper(mc.mouseHelper);
        }
    }

    /** The game's mouse helper, with the horizontal delta negated while Invert Mouse X is on. */
    public static final class Helper extends MouseHelper {
        private final MouseHelper wrapped;

        Helper(MouseHelper wrapped) {
            this.wrapped = wrapped;
        }

        @Override
        public void grabMouseCursor() {
            this.wrapped.grabMouseCursor();
        }

        @Override
        public void ungrabMouseCursor() {
            this.wrapped.ungrabMouseCursor();
        }

        @Override
        public void mouseXYChange() {
            this.wrapped.mouseXYChange();
            this.deltaX = LookAndFeel.INVERT_MOUSE_X.get() ? -this.wrapped.deltaX : this.wrapped.deltaX;
            this.deltaY = this.wrapped.deltaY;
        }
    }
}
