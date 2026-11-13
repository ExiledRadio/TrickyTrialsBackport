package com.exiledradio.trickytrialsbackport.lookfeel.client;

import com.exiledradio.trickytrialsbackport.TrickyTrialsBackport;
import com.exiledradio.trickytrialsbackport.lookfeel.LookAndFeel;
import com.exiledradio.trickytrialsbackport.lookfeel.Rules;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiChat;
import net.minecraft.client.gui.GuiDownloadTerrain;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.GuiTextField;
import net.minecraftforge.client.event.GuiOpenEvent;
import net.minecraftforge.client.event.GuiScreenEvent;
import net.minecraftforge.fml.common.ObfuscationReflectionHelper;
import net.minecraftforge.fml.common.eventhandler.EventPriority;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import org.lwjgl.input.Keyboard;

import javax.annotation.Nullable;
import java.lang.ref.WeakReference;

/**
 * 1.21.9's chat changes ({@code ChatScreen}, {@code ChatComponent}, {@code LocalPlayer.handlePortalTransitionEffect}):
 *
 * <ul>
 *   <li>Chat drafts: an unsent message is kept when another screen takes the chat's place, and, with Save Unsent
 *       Chats on, when the chat is closed with Escape. The next time chat opens (plainly, or with "/" for a draft
 *       that is a command) the draft is back, gray until edited; Backspace on an untouched draft clears it. Sending
 *       a message, or closing an empty chat, drops it.</li>
 *   <li>Standing in a nether portal no longer closes the chat.</li>
 *   <li>A chat open when the player changes dimension opens again, with its text, once the new world is shown.</li>
 * </ul>
 * Only 1.12.2's own chat screen ({@code GuiChat} itself, not subclasses such as the multiplayer bed screen).
 */
@SideOnly(Side.CLIENT)
public final class ChatDrafts {

    /** {@code ChatFormatting.GRAY}: an untouched draft's color. */
    private static final int DRAFT_COLOR = 0xAAAAAA;
    /** {@code GuiTextField}'s default text color. */
    private static final int TEXT_COLOR = 0xE0E0E0;

    @Nullable
    private String draft;
    /** Set by the key that is closing the chat: Escape or Enter; null means another screen interrupted it. */
    @Nullable
    private Rules.ChatExit closing;
    /** The chat screen showing a restored, untouched draft. */
    private WeakReference<GuiChat> draftScreen = new WeakReference<GuiChat>(null);
    /** The text of a chat a dimension change interrupted, to open again after it. */
    @Nullable
    private String preserved;

    @Nullable
    static GuiTextField input(GuiChat chat) {
        try {
            return ObfuscationReflectionHelper.getPrivateValue(GuiChat.class, chat, "field_146415_a");
        } catch (RuntimeException e) {
            TrickyTrialsBackport.LOGGER.debug("Look and feel: no chat input field", e);
            return null;
        }
    }

    @Nullable
    static String opening(GuiChat chat) {
        try {
            return ObfuscationReflectionHelper.getPrivateValue(GuiChat.class, chat, "field_146409_v");
        } catch (RuntimeException e) {
            return null;
        }
    }

    /** @return true while 1.12.2's player is standing in a nether portal ({@code Entity.inPortal}, protected) */
    private static boolean inPortal(Minecraft mc) {
        if (mc.player == null) {
            return false;
        }
        try {
            Boolean in = ObfuscationReflectionHelper.getPrivateValue(net.minecraft.entity.Entity.class, mc.player, "field_71087_bX");
            return in != null && in;
        } catch (RuntimeException e) {
            return false;
        }
    }

    private static boolean plainChat(@Nullable GuiScreen screen) {
        return screen != null && screen.getClass() == GuiChat.class;
    }

    @SubscribeEvent
    public void key(GuiScreenEvent.KeyboardInputEvent.Pre event) {
        if (!plainChat(event.getGui()) || !Keyboard.getEventKeyState()) {
            return;
        }
        GuiChat chat = (GuiChat) event.getGui();
        int key = Keyboard.getEventKey();
        if (key == Keyboard.KEY_ESCAPE) {
            this.closing = Rules.ChatExit.INTENTIONAL;
        } else if (key == Keyboard.KEY_RETURN || key == Keyboard.KEY_NUMPADENTER) {
            this.closing = Rules.ChatExit.DONE;
        } else if (key == Keyboard.KEY_BACK && this.draftScreen.get() == chat) {
            GuiTextField field = input(chat);
            if (field != null) {
                field.setText("");
                undraft(chat);
                event.setCanceled(true);
            }
        }
    }

    @SubscribeEvent
    public void keyAfter(GuiScreenEvent.KeyboardInputEvent.Post event) {
        GuiScreen gui = event.getGui();
        if (plainChat(gui) && this.draftScreen.get() == gui) {
            GuiTextField field = input((GuiChat) gui);
            if (field != null && !field.getText().equals(this.draft)) {
                undraft((GuiChat) gui);
            }
        }
    }

    private void undraft(GuiChat chat) {
        GuiTextField field = input(chat);
        if (field != null) {
            field.setTextColor(TEXT_COLOR);
        }
        this.draftScreen = new WeakReference<GuiChat>(null);
    }

    @SubscribeEvent
    public void init(GuiScreenEvent.InitGuiEvent.Post event) {
        if (plainChat(event.getGui()) && this.draftScreen.get() == event.getGui()) {
            GuiTextField field = input((GuiChat) event.getGui());
            if (field != null) {
                field.setTextColor(DRAFT_COLOR);
            }
        }
    }

    @SubscribeEvent(priority = EventPriority.LOW)
    public void open(GuiOpenEvent event) {
        Minecraft mc = Minecraft.getMinecraft();
        GuiScreen current = mc.currentScreen;
        GuiScreen next = event.getGui();
        if (plainChat(current) && next != current) {
            Rules.ChatExit exit = this.closing != null ? this.closing : Rules.ChatExit.INTERRUPTED;
            this.closing = null;
            // 1.21.9: chat is allowed in a portal; 1.12.2's player closes every screen that does not pause the game.
            if (next == null && exit == Rules.ChatExit.INTERRUPTED && inPortal(mc)) {
                event.setCanceled(true);
                return;
            }
            GuiTextField field = input((GuiChat) current);
            String text = field != null ? field.getText() : "";
            if (!Rules.keepsDraft(exit, LookAndFeel.SAVE_CHAT_DRAFTS.get(), text)) {
                this.draft = null;
            } else if (this.draftScreen.get() != current) {
                this.draft = text;
            }
            this.draftScreen = new WeakReference<GuiChat>(null);
            if (next instanceof GuiDownloadTerrain && exit == Rules.ChatExit.INTERRUPTED) {
                this.preserved = text;
            }
            return;
        }
        if (next == null && current instanceof GuiDownloadTerrain && this.preserved != null && mc.world != null) {
            // Minecraft.setScreen(null) restores the chat screen a level change preserved.
            event.setGui(new GuiChat(this.preserved));
            this.preserved = null;
            return;
        }
        if (plainChat(next)) {
            this.closing = null;
            String opening = opening((GuiChat) next);
            if (this.draft != null && Rules.restoresDraft(opening == null ? "" : opening, this.draft)) {
                GuiChat restored = new GuiChat(this.draft);
                event.setGui(restored);
                this.draftScreen = new WeakReference<GuiChat>(restored);
            }
        }
    }
}
