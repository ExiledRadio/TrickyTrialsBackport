package com.exiledradio.trickytrialsbackport.lookfeel;

/**
 * The numbers and decisions of 1.21.x's look and feel, as plain functions, so the headless self-test checks the
 * same code the client runs. Each method names the 1.21.11 code it follows.
 */
public final class Rules {

    private Rules() {
    }

    // ---- 1.21.2: Reduce FPS when (FramerateLimitTracker) ----

    /** After a minute without input, the AFK limit applies. */
    public static final long AFK_THRESHOLD_MS = 60000L;
    /** After ten minutes without input, the long AFK limit applies. */
    public static final long LONG_AFK_THRESHOLD_MS = 600000L;

    /**
     * {@code FramerateLimitTracker.getFramerateLimit}, without its out-of-level menu limit (1.12.2 holds menus
     * without a world at 30 itself).
     *
     * @param afk       true when "Reduce FPS when" is AFK, false when it is Minimized
     * @param idleMs    milliseconds since the last key, click or mouse movement
     * @param iconified true while the window is minimized
     * @param limit     Max Framerate (260 or more is unlimited)
     * @return the frame rate to hold the game to, or 0 for no throttle of ours
     */
    public static int framerateLimit(boolean afk, long idleMs, boolean iconified, int limit) {
        if (iconified) {
            return 10;
        }
        if (afk) {
            if (idleMs > LONG_AFK_THRESHOLD_MS) {
                return 10;
            }
            if (idleMs > AFK_THRESHOLD_MS) {
                return Math.min(limit, 30);
            }
        }
        return 0;
    }

    // ---- 1.21.2: air bubbles (Gui.renderAirBubbles) ----

    /** {@code Gui.getCurrentAirSupplyBubble}: ceil((air + offset) * 10 / max). */
    public static int airBubble(int air, int max, int offset) {
        return (int) Math.ceil((air + offset) * 10 / (float) max);
    }

    /** {@code Gui.getEmptyBubbleDelayDuration}. */
    public static int emptyBubbleDelay(int air, boolean underWater) {
        return air != 0 && underWater ? 1 : 0;
    }

    /** {@code Gui.playAirBubblePoppedSound}'s volume, with {@code empty} bubbles already empty. */
    public static float bubblePopVolume(int empty) {
        return 0.5F + 0.1F * Math.max(0, empty - 3 + 1);
    }

    /** {@code Gui.playAirBubblePoppedSound}'s pitch. */
    public static float bubblePopPitch(int empty) {
        return 1.0F + 0.1F * Math.max(0, empty - 5 + 1);
    }

    /** What one of the ten bubble slots shows. */
    public enum Bubble { NONE, FULL, BURSTING, EMPTY }

    /**
     * {@code Gui.renderAirBubbles}' choice for slot {@code i} (1 to 10, right to left).
     *
     * @return what the slot shows
     */
    public static Bubble bubble(int i, int air, int max, boolean underWater) {
        int full = airBubble(air, max, -2);
        int current = airBubble(air, max, 0);
        int empty = 10 - airBubble(air, max, emptyBubbleDelay(air, underWater));
        if (i <= full) {
            return Bubble.FULL;
        }
        if (full != current && i == current && underWater) {
            return Bubble.BURSTING;
        }
        if (i > 10 - empty) {
            return Bubble.EMPTY;
        }
        return Bubble.NONE;
    }

    // ---- 1.21.6: rain fog (AtmosphericFogEnvironment) ----

    /** @return the rain fog's target strength: rain level, scaled by sky light above 8 and halved without rain */
    public static float rainFogTarget(float rainLevel, int skyLight, boolean precipitation) {
        float light = Math.max(0.0F, Math.min(1.0F, (skyLight - 8.0F) / 7.0F));
        return rainLevel * light * (precipitation ? 1.0F : 0.5F);
    }

    /** @return the fog start with rain fog {@code m} (0 to 1) */
    public static float rainFogStart(float start, float m) {
        return start + -160.0F * m;
    }

    /** @return the fog end with rain fog {@code m}: pulled in by up to 256 blocks, never nearer than 96 */
    public static float rainFogEnd(float end, float m) {
        return Math.max(Math.min(96.0F, end), end + -256.0F * m);
    }

    // ---- 1.21.11: nether fog (the_nether's fog_start_distance and fog_end_distance) ----

    public static final float NETHER_FOG_START = 10.0F;
    public static final float NETHER_FOG_END = 96.0F;

    // ---- 1.21.9: chat drafts (ChatScreen.removed, ChatComponent.createScreen) ----

    /** Why the chat screen closed. */
    public enum ChatExit { INTERRUPTED, INTENTIONAL, DONE }

    /**
     * {@code ChatScreen.removed}: a closed chat's text is kept as a draft when the screen was interrupted (another
     * screen took its place), or closed with Escape while Save Unsent Chats is on; never when sent or blank.
     */
    public static boolean keepsDraft(ChatExit exit, boolean saveDrafts, String text) {
        if (text == null || text.trim().isEmpty()) {
            return false;
        }
        boolean discard = exit != ChatExit.INTERRUPTED && (exit != ChatExit.INTENTIONAL || !saveDrafts);
        return !discard;
    }

    /**
     * {@code ChatComponent.ChatMethod.isDraftRestorable}: chat opened plainly restores any draft; chat opened with
     * "/" restores only a command. Chat opened with other text (a mod's) is left as it is.
     */
    public static boolean restoresDraft(String opening, String draft) {
        if (draft == null) {
            return false;
        }
        if ("".equals(opening)) {
            return true;
        }
        return "/".equals(opening) && draft.startsWith("/");
    }

    // ---- 1.21.9: the loading screen (LevelLoadingScreen) ----

    /** {@code MinecraftServer.createChunkLoadStatusView}'s radius: max(5, 3) + RADIUS_AROUND_FULL_CHUNK (10) + 1. */
    public static final int LOADING_VIEW_RADIUS = 16;

    /** @return the y of "Loading terrain..." for a screen of this height, with the grid shown or not */
    public static int loadingTextY(int height, boolean grid) {
        int middle = height / 2;
        return grid ? middle - LOADING_VIEW_RADIUS * 2 - 9 * 3 : middle - 50;
    }
}
