package com.exiledradio.trickytrialsbackport.nautilus.client;

import com.exiledradio.trickytrialsbackport.common.Content;
import com.exiledradio.trickytrialsbackport.common.Guis;
import com.exiledradio.trickytrialsbackport.nautilus.EntityAbstractNautilus;
import com.exiledradio.trickytrialsbackport.nautilus.EntityNautilus;
import com.exiledradio.trickytrialsbackport.nautilus.EntityZombieNautilus;
import com.exiledradio.trickytrialsbackport.nautilus.NautilusContent;
import com.exiledradio.trickytrialsbackport.nautilus.NautilusOpenMessage;
import com.exiledradio.trickytrialsbackport.network.NetworkHandler;

import net.minecraft.client.Minecraft;
import net.minecraft.client.audio.ISound;
import net.minecraft.client.audio.MovingSound;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.inventory.GuiContainer;
import net.minecraft.client.gui.inventory.GuiContainerCreative;
import net.minecraft.client.gui.inventory.GuiInventory;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.entity.Entity;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.MathHelper;
import net.minecraftforge.client.event.GuiOpenEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.client.registry.RenderingRegistry;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;

/**
 * The nautilus stream's client side, from the mod's pre-init on a client only: the renderers, the inventory screen,
 * the inventory key while riding (packet 115), the riding sound and Breath of the Nautilus's icon.
 */
public final class NautilusClient {

    static final ResourceLocation BREATH_ICON = Content.id("textures/mob_effect/breath_of_the_nautilus.png");

    private static Entity soundVehicle;

    private NautilusClient() {
    }

    public static void init() {
        if (!NautilusContent.enabled()) {
            return;
        }
        RenderingRegistry.registerEntityRenderingHandler(EntityNautilus.class, RenderNautilus::new);
        RenderingRegistry.registerEntityRenderingHandler(EntityZombieNautilus.class, RenderNautilus::new);
        Guis.screen(NautilusContent.GUI_ID, (player, world, pos) -> {
            Entity e = world.getEntityByID(pos.getX());
            return e instanceof EntityAbstractNautilus ? new GuiNautilus(player.inventory, (EntityAbstractNautilus) e) : null;
        });
        MinecraftForge.EVENT_BUS.register(new Events());
    }

    /** Breath of the Nautilus's 18 x 18 icon, then the inventory sheet bound again for the effects after it. */
    public static void drawIcon(int x, int y, float alpha) {
        Minecraft mc = Minecraft.getMinecraft();
        GlStateManager.enableBlend();
        GlStateManager.color(1.0F, 1.0F, 1.0F, alpha);
        mc.getTextureManager().bindTexture(BREATH_ICON);
        Gui.drawModalRectWithCustomSizedTexture(x, y, 0.0F, 0.0F, 18, 18, 18.0F, 18.0F);
        mc.getTextureManager().bindTexture(GuiContainer.INVENTORY_BACKGROUND);
    }

    public static final class Events {
        @SubscribeEvent
        public void tick(TickEvent.ClientTickEvent event) {
            if (event.phase != TickEvent.Phase.END) {
                return;
            }
            Minecraft mc = Minecraft.getMinecraft();
            EntityPlayerSP player = mc.player;
            Entity vehicle = player == null ? null : player.getRidingEntity();
            if (vehicle instanceof EntityAbstractNautilus && vehicle != soundVehicle) {
                soundVehicle = vehicle;
                mc.getSoundHandler().playSound(new RidingSound(player, (EntityAbstractNautilus) vehicle));
            } else if (!(vehicle instanceof EntityAbstractNautilus)) {
                soundVehicle = null;
            }
        }

        /** 1.21.11: the inventory key while riding a tamed nautilus opens its inventory instead. */
        @SubscribeEvent
        public void openGui(GuiOpenEvent event) {
            if (!(event.getGui() instanceof GuiInventory) && !(event.getGui() instanceof GuiContainerCreative)) {
                return;
            }
            EntityPlayerSP player = Minecraft.getMinecraft().player;
            Entity vehicle = player == null ? null : player.getRidingEntity();
            if (vehicle instanceof EntityAbstractNautilus && ((EntityAbstractNautilus) vehicle).isTamed()) {
                event.setCanceled(true);
                NetworkHandler.channel.sendToServer(new NautilusOpenMessage());
            }
        }
    }

    /**
     * 1.21.11's {@code RidingEntitySoundInstance} for {@code entity.nautilus.riding}: looping, everywhere at once,
     * heard only under water, 5 times the nautilus's speed up to full.
     */
    static final class RidingSound extends MovingSound {
        private final EntityPlayerSP player;
        private final EntityAbstractNautilus nautilus;

        RidingSound(EntityPlayerSP player, EntityAbstractNautilus nautilus) {
            super(NautilusContent.RIDING, nautilus.getSoundCategory());
            this.player = player;
            this.nautilus = nautilus;
            this.attenuationType = ISound.AttenuationType.NONE;
            this.repeat = true;
            this.repeatDelay = 0;
            this.volume = 0.0F;
        }

        @Override
        public void update() {
            if (this.nautilus.isDead || !this.player.isRiding() || this.player.getRidingEntity() != this.nautilus) {
                this.donePlaying = true;
            } else if (this.nautilus.isSilent() || !this.nautilus.isInsideOfMaterial(net.minecraft.block.material.Material.WATER)) {
                this.volume = 0.0F;
            } else {
                float speed = MathHelper.sqrt(this.nautilus.motionX * this.nautilus.motionX + this.nautilus.motionY * this.nautilus.motionY
                        + this.nautilus.motionZ * this.nautilus.motionZ);
                this.volume = speed >= 0.01F ? 5.0F * MathHelper.clamp(speed, 0.0F, 1.0F) : 0.0F;
            }
        }
    }
}
