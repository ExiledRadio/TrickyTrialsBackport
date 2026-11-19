package com.exiledradio.trickytrialsbackport.nautilus.client;

import com.exiledradio.trickytrialsbackport.common.Content;
import com.exiledradio.trickytrialsbackport.nautilus.EntityAbstractNautilus;
import com.exiledradio.trickytrialsbackport.nautilus.EntityZombieNautilus;
import com.exiledradio.trickytrialsbackport.nautilus.ItemNautilusArmor;

import net.minecraft.client.renderer.entity.RenderLiving;
import net.minecraft.client.renderer.entity.RenderManager;
import net.minecraft.client.renderer.entity.layers.LayerRenderer;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ResourceLocation;

/**
 * 1.21.11's {@code NautilusRenderer} and {@code ZombieNautilusRenderer}: the adult or baby model (the zombie's normal
 * or coral one by variant), shadow 0.7, and the armor and saddle layers on an adult.
 */
public class RenderNautilus extends RenderLiving<EntityAbstractNautilus> {

    static final ResourceLocation NAUTILUS = Content.id("textures/entity/nautilus/nautilus.png");
    static final ResourceLocation BABY = Content.id("textures/entity/nautilus/nautilus_baby.png");
    static final ResourceLocation ZOMBIE = Content.id("textures/entity/nautilus/zombie_nautilus.png");
    static final ResourceLocation ZOMBIE_CORAL = Content.id("textures/entity/nautilus/zombie_nautilus_coral.png");
    static final ResourceLocation SADDLE = Content.id("textures/entity/equipment/nautilus_saddle/saddle.png");
    static final float SHADOW = 0.7F;

    private final ModelNautilus adult = new ModelNautilus(ModelNautilus.Mesh.ADULT);
    private final ModelNautilus baby = new ModelNautilus(ModelNautilus.Mesh.BABY);
    private final ModelNautilus coral = new ModelNautilus(ModelNautilus.Mesh.CORAL);

    public RenderNautilus(RenderManager manager) {
        super(manager, new ModelNautilus(ModelNautilus.Mesh.ADULT), SHADOW);
        this.mainModel = this.adult;
        this.addLayer(new Equipment(this, new ModelNautilus(ModelNautilus.Mesh.ARMOR), true));
        this.addLayer(new Equipment(this, new ModelNautilus(ModelNautilus.Mesh.SADDLE), false));
    }

    @Override
    public void doRender(EntityAbstractNautilus entity, double x, double y, double z, float yaw, float partialTicks) {
        if (entity.isChild()) {
            this.mainModel = this.baby;
            this.shadowSize = SHADOW * 0.5F;
        } else {
            boolean warm = entity instanceof EntityZombieNautilus && ((EntityZombieNautilus) entity).getVariant() == EntityZombieNautilus.WARM;
            this.mainModel = warm ? this.coral : this.adult;
            this.shadowSize = SHADOW;
            this.coral.corals.showModel = entity.getBodyArmor().isEmpty();
        }
        super.doRender(entity, x, y, z, yaw, partialTicks);
    }

    @Override
    protected ResourceLocation getEntityTexture(EntityAbstractNautilus entity) {
        if (entity instanceof EntityZombieNautilus) {
            return ((EntityZombieNautilus) entity).getVariant() == EntityZombieNautilus.WARM ? ZOMBIE_CORAL : ZOMBIE;
        }
        return entity.isChild() ? BABY : NAUTILUS;
    }

    static ResourceLocation armorTexture(ItemStack stack) {
        return Content.id("textures/entity/equipment/nautilus_body/" + ((ItemNautilusArmor) stack.getItem()).tier.asset + ".png");
    }

    /** 1.21's {@code SimpleEquipmentLayer} for the body armor or the saddle. */
    static final class Equipment implements LayerRenderer<EntityAbstractNautilus> {
        private final RenderNautilus renderer;
        private final ModelNautilus model;
        private final boolean armor;

        Equipment(RenderNautilus renderer, ModelNautilus model, boolean armor) {
            this.renderer = renderer;
            this.model = model;
            this.armor = armor;
        }

        @Override
        public void doRenderLayer(EntityAbstractNautilus entity, float limbSwing, float limbSwingAmount, float partialTicks, float ageInTicks,
                float netHeadYaw, float headPitch, float scale) {
            if (entity.isChild()) {
                return;
            }
            ResourceLocation texture;
            if (this.armor) {
                ItemStack stack = entity.getBodyArmor();
                if (!(stack.getItem() instanceof ItemNautilusArmor)) {
                    return;
                }
                texture = armorTexture(stack);
            } else {
                if (!entity.isSaddled()) {
                    return;
                }
                texture = SADDLE;
            }
            this.renderer.bindTexture(texture);
            this.model.setModelAttributes(this.renderer.getMainModel());
            this.model.setLivingAnimations(entity, limbSwing, limbSwingAmount, partialTicks);
            this.model.render(entity, limbSwing, limbSwingAmount, ageInTicks, netHeadYaw, headPitch, scale);
        }

        @Override
        public boolean shouldCombineTextures() {
            return false;
        }
    }
}
