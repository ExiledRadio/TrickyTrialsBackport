package com.exiledradio.trickytrialsbackport.effects.client;

import com.exiledradio.trickytrialsbackport.common.Content;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.inventory.GuiContainer;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.renderer.texture.TextureMap;
import net.minecraft.entity.Entity;
import net.minecraft.init.Blocks;
import net.minecraft.init.Items;
import net.minecraft.item.Item;
import net.minecraft.util.ResourceLocation;
import net.minecraft.world.World;
import net.minecraftforge.client.event.TextureStitchEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.world.WorldEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Random;

/**
 * Client side of the effects stream: the icons, and each effect's own particle on the entities that have it.
 *
 * <p>1.21's {@code LivingEntity.tickEffects}: each tick, one chance in 4 (15 when invisible, five times less
 * when every effect is ambient) of one particle at a random point of the body, picked from the particles of
 * all its visible effects. Draws that land on another effect are left to 1.12.2's own swirl.
 * <ul>
 *   <li>Wind Charged: 1.21's {@code small_gust}, the small gust frames at 0.15 size.</li>
 *   <li>Weaving: 1.21's {@code item_cobweb}, a cobweb item crumb.</li>
 *   <li>Oozing: 1.21's {@code item_slime}, which is 1.12.2's own {@code slime} particle.</li>
 *   <li>Infested: 1.21's {@code infested}, a rising spell particle with its own texture.</li>
 * </ul>
 */
public final class EffectsClient {

    private static final String[] ICONS = {"wind_charged", "weaving", "oozing", "infested"};

    static final TextureAtlasSprite[] SMALL_GUST = new TextureAtlasSprite[7];
    static TextureAtlasSprite infested;

    /** entity id -> particle state, client thread only */
    private static final Map<Integer, Integer> STATES = new HashMap<Integer, Integer>();

    private EffectsClient() {
    }

    public static void init() {
        MinecraftForge.EVENT_BUS.register(new EffectsClient());
    }

    @SubscribeEvent
    public void stitch(TextureStitchEvent.Pre event) {
        TextureMap map = event.getMap();
        // The same frames the breeze stream's gusts use; the atlas keeps one sprite per name.
        for (int i = 0; i < SMALL_GUST.length; i++) {
            SMALL_GUST[i] = map.registerSprite(Content.id("particle/small_gust_" + i));
        }
        infested = map.registerSprite(Content.id("particle/infested"));
    }

    /** Draws an effect's 18x18 icon at the given screen point, then puts the inventory sheet back. */
    public static void drawIcon(String name, int x, int y, float alpha) {
        Minecraft mc = Minecraft.getMinecraft();
        GlStateManager.enableBlend();
        GlStateManager.color(1.0F, 1.0F, 1.0F, alpha);
        mc.getTextureManager().bindTexture(new ResourceLocation(Content.MOD_ID, "textures/mob_effect/" + name + ".png"));
        Gui.drawModalRectWithCustomSizedTexture(x, y, 0.0F, 0.0F, 18, 18, 18.0F, 18.0F);
        // Vanilla draws the next effects' cells from the inventory sheet without binding it again.
        mc.getTextureManager().bindTexture(GuiContainer.INVENTORY_BACKGROUND);
    }

    /** From the network thread. */
    public static void schedule(final int entityId, final int state) {
        Minecraft.getMinecraft().addScheduledTask(new Runnable() {
            @Override
            public void run() {
                if (state == 0) {
                    STATES.remove(entityId);
                } else {
                    STATES.put(entityId, state);
                }
            }
        });
    }

    @SubscribeEvent
    public void unload(WorldEvent.Unload event) {
        if (event.getWorld().isRemote) {
            STATES.clear();
        }
    }

    @SubscribeEvent
    public void tick(TickEvent.ClientTickEvent event) {
        Minecraft mc = Minecraft.getMinecraft();
        if (event.phase != TickEvent.Phase.END || mc.world == null || mc.isGamePaused() || STATES.isEmpty()) {
            return;
        }
        World world = mc.world;
        Random random = world.rand;
        for (Iterator<Map.Entry<Integer, Integer>> it = STATES.entrySet().iterator(); it.hasNext(); ) {
            Map.Entry<Integer, Integer> entry = it.next();
            Entity entity = world.getEntityByID(entry.getKey());
            if (entity == null) {
                // No longer tracked here; a client that sees it again is sent its state again.
                it.remove();
                continue;
            }
            int state = entry.getValue();
            int ours = state & 15;
            boolean ambient = (state & 16) != 0;
            int others = state >> 8 & 255;
            int rate = entity.isInvisible() ? 15 : 4;
            if (random.nextInt(rate * (ambient ? 5 : 1)) != 0) {
                continue;
            }
            int pick = random.nextInt(Integer.bitCount(ours) + others);
            if (pick >= Integer.bitCount(ours)) {
                continue;
            }
            int index = -1;
            for (int bit = 0; bit < 4; bit++) {
                if ((ours & 1 << bit) != 0 && pick-- == 0) {
                    index = bit;
                    break;
                }
            }
            double x = entity.posX + entity.width * (2.0 * random.nextDouble() - 1.0) * 0.5;
            double y = entity.posY + entity.height * random.nextDouble();
            double z = entity.posZ + entity.width * (2.0 * random.nextDouble() - 1.0) * 0.5;
            spawn(mc, world, index, x, y, z);
        }
    }

    private static void spawn(Minecraft mc, World world, int index, double x, double y, double z) {
        if (!shown(mc, world, x, y, z)) {
            return;
        }
        Particle particle;
        switch (index) {
            case 0:
                if (SMALL_GUST[0] == null) {
                    return;
                }
                particle = new ParticleSmallGust(world, x, y, z, SMALL_GUST);
                break;
            case 1:
                particle = new ParticleItemCrumb(world, x, y, z, Item.getItemFromBlock(Blocks.WEB));
                break;
            case 2:
                particle = new ParticleItemCrumb(world, x, y, z, Items.SLIME_BALL);
                break;
            default:
                if (infested == null) {
                    return;
                }
                particle = new ParticleInfested(world, x, y, z, infested);
                break;
        }
        mc.effectRenderer.addEffect(particle);
    }

    /** 1.12.2's {@code RenderGlobal} rule for a plain particle: within 32 blocks, and the particle setting. */
    private static boolean shown(Minecraft mc, World world, double x, double y, double z) {
        Entity view = mc.getRenderViewEntity();
        if (view == null || view.getDistanceSq(x, y, z) > 1024.0) {
            return false;
        }
        int level = mc.gameSettings.particleSetting;
        if (level == 1 && world.rand.nextInt(3) == 0) {
            level = 2;
        }
        return level <= 1;
    }
}
