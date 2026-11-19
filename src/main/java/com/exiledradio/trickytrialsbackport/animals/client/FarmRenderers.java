package com.exiledradio.trickytrialsbackport.animals.client;

import com.exiledradio.trickytrialsbackport.TrickyTrialsBackport;
import com.exiledradio.trickytrialsbackport.animals.EntityColoredEgg;
import com.exiledradio.trickytrialsbackport.animals.FarmAnimals;
import com.exiledradio.trickytrialsbackport.animals.FarmVariant;

import net.minecraft.client.model.ModelBase;
import net.minecraft.client.model.ModelChicken;
import net.minecraft.client.renderer.RenderItem;
import net.minecraft.client.renderer.entity.RenderChicken;
import net.minecraft.client.renderer.entity.RenderCow;
import net.minecraft.client.renderer.entity.RenderManager;
import net.minecraft.client.renderer.entity.RenderPig;
import net.minecraft.client.renderer.entity.RenderSnowball;
import net.minecraft.entity.passive.EntityChicken;
import net.minecraft.entity.passive.EntityCow;
import net.minecraft.entity.passive.EntityPig;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ResourceLocation;

/**
 * 1.21.5's {@code CowRenderer}, {@code PigRenderer} and {@code ChickenRenderer}: each picks its model and texture by
 * the animal's variant. They extend 1.12.2's renderers, so the pig keeps its saddle layer and the chicken its wing
 * flapping.
 */
final class FarmRenderers {

    private FarmRenderers() {
    }

    static ResourceLocation texture(String mob, FarmVariant v) {
        return new ResourceLocation(TrickyTrialsBackport.MOD_ID, "textures/entity/" + mob + "/" + v.name + "_" + mob + ".png");
    }

    private static ResourceLocation[] textures(String mob) {
        ResourceLocation[] out = new ResourceLocation[FarmVariant.values().length];
        for (FarmVariant v : FarmVariant.values()) {
            out[v.ordinal()] = texture(mob, v);
        }
        return out;
    }

    static final class Cow extends RenderCow {
        private final ModelBase[] models = new ModelBase[FarmVariant.values().length];
        private final ResourceLocation[] textures = textures("cow");

        Cow(RenderManager manager) {
            super(manager);
            for (FarmVariant v : FarmVariant.values()) {
                this.models[v.ordinal()] = new ModelFarmCow(v);
            }
            this.mainModel = this.models[0];
        }

        @Override
        public void doRender(EntityCow entity, double x, double y, double z, float yaw, float partialTicks) {
            this.mainModel = this.models[FarmAnimals.variant(entity).ordinal()];
            super.doRender(entity, x, y, z, yaw, partialTicks);
        }

        @Override
        protected ResourceLocation getEntityTexture(EntityCow entity) {
            return this.textures[FarmAnimals.variant(entity).ordinal()];
        }
    }

    static final class Pig extends RenderPig {
        private final ModelBase[] models = new ModelBase[FarmVariant.values().length];
        private final ResourceLocation[] textures = textures("pig");

        Pig(RenderManager manager) {
            super(manager);
            ModelBase plain = new ModelFarmPig(FarmVariant.TEMPERATE);
            this.models[FarmVariant.TEMPERATE.ordinal()] = plain;
            this.models[FarmVariant.WARM.ordinal()] = plain;
            this.models[FarmVariant.COLD.ordinal()] = new ModelFarmPig(FarmVariant.COLD);
            this.mainModel = plain;
        }

        @Override
        public void doRender(EntityPig entity, double x, double y, double z, float yaw, float partialTicks) {
            this.mainModel = this.models[FarmAnimals.variant(entity).ordinal()];
            super.doRender(entity, x, y, z, yaw, partialTicks);
        }

        @Override
        protected ResourceLocation getEntityTexture(EntityPig entity) {
            return this.textures[FarmAnimals.variant(entity).ordinal()];
        }
    }

    static final class Chicken extends RenderChicken {
        private final ModelBase[] models = new ModelBase[FarmVariant.values().length];
        private final ResourceLocation[] textures = textures("chicken");

        Chicken(RenderManager manager) {
            super(manager);
            ModelBase plain = new ModelChicken();
            this.models[FarmVariant.TEMPERATE.ordinal()] = plain;
            this.models[FarmVariant.WARM.ordinal()] = plain;
            this.models[FarmVariant.COLD.ordinal()] = new ModelColdChicken();
            this.mainModel = plain;
        }

        @Override
        public void doRender(EntityChicken entity, double x, double y, double z, float yaw, float partialTicks) {
            this.mainModel = this.models[FarmAnimals.variant(entity).ordinal()];
            super.doRender(entity, x, y, z, yaw, partialTicks);
        }

        @Override
        protected ResourceLocation getEntityTexture(EntityChicken entity) {
            return this.textures[FarmAnimals.variant(entity).ordinal()];
        }
    }

    /** The thrown blue or brown egg, drawn as its item, as 1.12.2 draws its egg. */
    static final class Egg extends RenderSnowball<EntityColoredEgg> {
        Egg(RenderManager manager, RenderItem items) {
            super(manager, null, items);
        }

        @Override
        public ItemStack getStackToRender(EntityColoredEgg entity) {
            return entity.item() == null ? ItemStack.EMPTY : new ItemStack(entity.item());
        }
    }
}
