package com.exiledradio.trickytrialsbackport.animals.client;

import com.exiledradio.trickytrialsbackport.ModConfig;
import com.exiledradio.trickytrialsbackport.animals.AnimalsContent;
import com.exiledradio.trickytrialsbackport.animals.EntityColoredEgg;
import com.exiledradio.trickytrialsbackport.animals.FarmAnimals;
import com.exiledradio.trickytrialsbackport.animals.FarmVariant;

import net.minecraft.client.Minecraft;
import net.minecraft.entity.Entity;
import net.minecraft.entity.passive.EntityChicken;
import net.minecraft.entity.passive.EntityCow;
import net.minecraft.entity.passive.EntityPig;
import net.minecraftforge.fml.client.registry.RenderingRegistry;

/** The animals stream's renderers, called from the mod's pre-init on a client only, and the variant packet's end. */
public final class AnimalsClient {

    private AnimalsClient() {
    }

    public static void init() {
        if (!AnimalsContent.enabled()) {
            return;
        }
        // Registered after vanilla's, so these replace RenderCow, RenderPig and RenderChicken.
        if (ModConfig.animals.cowVariants) {
            RenderingRegistry.registerEntityRenderingHandler(EntityCow.class, FarmRenderers.Cow::new);
        }
        if (ModConfig.animals.pigVariants) {
            RenderingRegistry.registerEntityRenderingHandler(EntityPig.class, FarmRenderers.Pig::new);
        }
        if (ModConfig.animals.chickenVariants) {
            RenderingRegistry.registerEntityRenderingHandler(EntityChicken.class, FarmRenderers.Chicken::new);
        }
        if (AnimalsContent.eggs()) {
            RenderingRegistry.registerEntityRenderingHandler(EntityColoredEgg.class,
                    manager -> new FarmRenderers.Egg(manager, Minecraft.getMinecraft().getRenderItem()));
        }
    }

    /** From packet 80, on the network thread: sets the variant on the client's entity on the main thread. */
    public static void schedule(final int entityId, final byte variant) {
        final Minecraft mc = Minecraft.getMinecraft();
        mc.addScheduledTask(() -> {
            if (mc.world == null) {
                return;
            }
            Entity e = mc.world.getEntityByID(entityId);
            if (e != null) {
                FarmAnimals.setVariant(e, FarmVariant.byOrdinal(variant));
            }
        });
    }
}
