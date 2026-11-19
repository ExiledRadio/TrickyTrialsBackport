package com.exiledradio.trickytrialsbackport.mounts.client;

import com.exiledradio.trickytrialsbackport.common.Content;
import com.exiledradio.trickytrialsbackport.mounts.EntityParched;
import com.exiledradio.trickytrialsbackport.mounts.MountsContent;

import net.minecraft.client.Minecraft;
import net.minecraft.client.model.ModelSkeleton;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.entity.Render;
import net.minecraft.client.renderer.entity.RenderBiped;
import net.minecraft.client.renderer.entity.RenderLivingBase;
import net.minecraft.client.renderer.entity.RenderManager;
import net.minecraft.client.renderer.entity.layers.LayerBipedArmor;
import net.minecraft.client.renderer.entity.layers.LayerRenderer;
import net.minecraft.entity.passive.EntityZombieHorse;
import net.minecraft.entity.passive.HorseArmorType;
import net.minecraft.inventory.EntityEquipmentSlot;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.client.registry.RenderingRegistry;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;

/** The mounts stream's renderers; called from the mod's pre-init on a client only. */
public final class MountsClient {

    private MountsClient() {
    }

    public static void init() {
        if (!MountsContent.enabled()) {
            return;
        }
        RenderingRegistry.registerEntityRenderingHandler(EntityParched.class, RenderParched::new);
        MinecraftForge.EVENT_BUS.register(new ArmorLayerInstaller());
    }

    /** The parched: 1.21.11's {@code ParchedRenderer}, a skeleton's armor and held items, its own model and texture. */
    static final class RenderParched extends RenderBiped<EntityParched> {
        static final ResourceLocation TEXTURE = Content.id("textures/entity/skeleton/parched.png");

        RenderParched(RenderManager manager) {
            super(manager, new ModelParched(), 0.5F);
            addLayer(new LayerBipedArmor(this) {
                @Override
                protected void initArmor() {
                    this.modelLeggings = new ModelSkeleton(0.5F, true);
                    this.modelArmor = new ModelSkeleton(1.0F, true);
                }
            });
        }

        @Override
        protected ResourceLocation getEntityTexture(EntityParched entity) {
            return TEXTURE;
        }

        @Override
        public void transformHeldFull3DItemLayer() {
            GlStateManager.translate(0.09375F, 0.1875F, 0.0F);
        }
    }

    /**
     * Puts the armor layer on 1.12.2's zombie horse renderer once the render manager exists (it is built after
     * pre-init).
     */
    public static final class ArmorLayerInstaller {
        private boolean done;

        @SubscribeEvent
        @SuppressWarnings({"unchecked", "rawtypes"})
        public void tick(TickEvent.ClientTickEvent event) {
            if (this.done || event.phase != TickEvent.Phase.END) {
                return;
            }
            RenderManager manager = Minecraft.getMinecraft().getRenderManager();
            if (manager == null) {
                return;
            }
            this.done = true;
            Render<?> render = manager.entityRenderMap.get(EntityZombieHorse.class);
            if (render instanceof RenderLivingBase) {
                ((RenderLivingBase) render).addLayer(new ZombieHorseArmor((RenderLivingBase<EntityZombieHorse>) render));
            }
        }
    }

    /**
     * The zombie horse's horse armor: 1.12.2 draws a horse's armor as one more texture over the same model (its layered
     * texture), so this draws the main model again in the armor's texture. The armor reaches the client as the horse's
     * chest equipment ({@code ZombieHorses.syncArmor}).
     */
    static final class ZombieHorseArmor implements LayerRenderer<EntityZombieHorse> {
        private final RenderLivingBase<EntityZombieHorse> renderer;

        ZombieHorseArmor(RenderLivingBase<EntityZombieHorse> renderer) {
            this.renderer = renderer;
        }

        @Override
        public void doRenderLayer(EntityZombieHorse horse, float limbSwing, float limbSwingAmount, float partialTicks, float ageInTicks,
                                  float netHeadYaw, float headPitch, float scale) {
            ItemStack armor = horse.getItemStackFromSlot(EntityEquipmentSlot.CHEST);
            if (armor.isEmpty() || HorseArmorType.getByItemStack(armor) == HorseArmorType.NONE) {
                return;
            }
            String texture = armor.getItem().getHorseArmorTexture(horse, armor);
            if (texture == null) {
                return;
            }
            GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
            this.renderer.bindTexture(new ResourceLocation(texture));
            this.renderer.getMainModel().render(horse, limbSwing, limbSwingAmount, ageInTicks, netHeadYaw, headPitch, scale);
        }

        @Override
        public boolean shouldCombineTextures() {
            return true;
        }
    }
}
