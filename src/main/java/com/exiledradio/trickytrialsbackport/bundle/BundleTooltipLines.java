package com.exiledradio.trickytrialsbackport.bundle;

import java.util.List;

/**
 * The placeholder lines a bundle's tooltip carries where its image goes (the client draws the image over
 * them). No client types, so the self-test can check them.
 *
 * <p>Screens do not show a tooltip's lines as the item gave them. 1.12.2's {@code GuiScreen.getItemToolTip}
 * colors the name and puts gray ({@code §7}) before every other line, and other mods add their own codes.
 * So a placeholder is found by the marker anywhere in the line, never by how the line starts.
 */
public final class BundleTooltipLines {

    /** In every placeholder line: two formatting codes no font draws or measures. */
    public static final String MARKER = "§z§z";
    /** The marker and 24 spaces: 96 pixels in the default font, the grid's width. */
    public static final String PLACEHOLDER = MARKER + "                        ";

    private BundleTooltipLines() {
    }

    public static boolean isPlaceholder(String line) {
        return line != null && line.contains(MARKER);
    }

    /** @return the index of the first placeholder line, or -1 */
    public static int firstPlaceholder(List<String> lines) {
        for (int i = 0; i < lines.size(); i++) {
            if (isPlaceholder(lines.get(i))) {
                return i;
            }
        }
        return -1;
    }
}
