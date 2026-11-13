package com.exiledradio.trickytrialsbackport.lookfeel.client;

import com.exiledradio.backportcore.presentation.LoadingRevision;
import com.exiledradio.backportcore.presentation.client.DebugText;
import com.exiledradio.backportcore.presentation.client.FogPipeline;
import com.exiledradio.backportcore.presentation.client.LoadingScreens;

import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import java.util.regex.Pattern;

/**
 * Stream Lf's client side: what 1.21.x changed on screen that is drawn or handled on the client.
 *
 * <ul>
 *   <li>1.21.2: {@link FramerateLimiter}, {@link HighContrastOutline}, {@link AirBubbles},
 *       {@link ItemRarityTooltips}, and F3's entity line without its unused "B" value;</li>
 *   <li>1.21.6: fog thickens in rain ({@link Fog#RAIN});</li>
 *   <li>1.21.9: {@link InvertMouseX}, {@link SprintWindow}, {@link ChatDrafts}, and the loading screen
 *       ({@link LoadingScreen1219});</li>
 *   <li>1.21.11: nether fog from 10 to 96 blocks ({@link Fog#NETHER}).</li>
 * </ul>
 */
@SideOnly(Side.CLIENT)
public final class LookAndFeelClient {

    /** 1.12.2's {@code RenderGlobal.getDebugInfoEntities}: "E: 3/93, B: 0, I: 0". */
    private static final Pattern ENTITY_B = Pattern.compile(", B: \\d+");

    private LookAndFeelClient() {
    }

    public static void init() {
        MinecraftForge.EVENT_BUS.register(new FramerateLimiter());
        MinecraftForge.EVENT_BUS.register(new HighContrastOutline());
        MinecraftForge.EVENT_BUS.register(new AirBubbles());
        MinecraftForge.EVENT_BUS.register(new ItemRarityTooltips());
        MinecraftForge.EVENT_BUS.register(new InvertMouseX());
        MinecraftForge.EVENT_BUS.register(new SprintWindow());
        MinecraftForge.EVENT_BUS.register(new ChatDrafts());
        LoadingScreens.register(LoadingRevision.SPAWN_VIEW_1_21_9, new LoadingScreen1219());
        FogPipeline.add("1.21.6", "fog thickens in rain", Fog.RAIN);
        FogPipeline.add("1.21.11", "nether fog from 10 to 96 blocks", Fog.NETHER);
        DebugText.add("1.21.2", "no B on the entity line", lines -> {
            for (int i = 0; i < lines.left.size(); i++) {
                String line = lines.left.get(i);
                if (line != null && line.startsWith("E: ")) {
                    lines.left.set(i, ENTITY_B.matcher(line).replaceFirst(""));
                }
            }
        });
    }
}
