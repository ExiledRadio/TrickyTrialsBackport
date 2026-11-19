package com.exiledradio.trickytrialsbackport.coppergolem.client;

import com.exiledradio.trickytrialsbackport.TrickyTrialsBackport;
import com.exiledradio.trickytrialsbackport.coppergolem.CopperGolemContent;
import com.exiledradio.trickytrialsbackport.coppergolem.EntityCopperGolem;

import net.minecraft.block.state.IBlockState;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.OpenGlHelper;
import net.minecraft.client.renderer.block.model.ItemCameraTransforms;
import net.minecraft.client.renderer.entity.RenderLiving;
import net.minecraft.client.renderer.entity.RenderManager;
import net.minecraft.client.renderer.entity.layers.LayerRenderer;
import net.minecraft.client.renderer.texture.TextureMap;
import net.minecraft.init.Blocks;
import net.minecraft.item.Item;
import net.minecraft.item.ItemBlock;
import net.minecraft.item.ItemStack;
import net.minecraft.util.EnumHandSide;
import net.minecraft.util.ResourceLocation;

/**
 * 1.21.11's {@code CopperGolemRenderer}: the texture of its stage, its glowing eyes, what it carries, and the block on
 * its antenna (the poppy an iron golem gave it). Shadow 0.5.
 */
public final class RenderCopperGolem extends RenderLiving<EntityCopperGolem> {

    static final ResourceLocation[] TEXTURES = new ResourceLocation[4];
    static final ResourceLocation[] EYES = new ResourceLocation[4];

    static {
        for (int stage = 0; stage < 4; stage++) {
            String name = CopperGolemContent.prefix(stage) + "copper_golem";
            TEXTURES[stage] = new ResourceLocation(TrickyTrialsBackport.MOD_ID, "textures/entity/copper_golem/" + name + ".png");
            EYES[stage] = new ResourceLocation(TrickyTrialsBackport.MOD_ID, "textures/entity/copper_golem/" + name + "_eyes.png");
        }
    }

    public RenderCopperGolem(RenderManager manager) {
        super(manager, new ModelCopperGolem(), 0.5F);
        addLayer(new Eyes(this));
        addLayer(new HeldItem(this));
        addLayer(new Antenna(this));
    }

    @Override
    protected ResourceLocation getEntityTexture(EntityCopperGolem entity) {
        return TEXTURES[entity.getWeatherState()];
    }

    ModelCopperGolem model() {
        return (ModelCopperGolem) getMainModel();
    }

    /** 1.21's emissive eyes layer: the eyes texture, full bright and added on top. */
    static final class Eyes implements LayerRenderer<EntityCopperGolem> {
        private final RenderCopperGolem renderer;

        Eyes(RenderCopperGolem renderer) {
            this.renderer = renderer;
        }

        @Override
        public void doRenderLayer(EntityCopperGolem golem, float limbSwing, float limbSwingAmount, float partialTicks, float ageInTicks,
                                  float netHeadYaw, float headPitch, float scale) {
            this.renderer.bindTexture(EYES[golem.getWeatherState()]);
            GlStateManager.enableBlend();
            GlStateManager.disableAlpha();
            GlStateManager.blendFunc(GlStateManager.SourceFactor.ONE, GlStateManager.DestFactor.ONE);
            GlStateManager.depthMask(!golem.isInvisible());
            OpenGlHelper.setLightmapTextureCoords(OpenGlHelper.lightmapTexUnit, 61680.0F, 0.0F);
            GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
            Minecraft.getMinecraft().entityRenderer.setupFogColor(true);
            this.renderer.model().renderAgain(scale);
            Minecraft.getMinecraft().entityRenderer.setupFogColor(false);
            int light = golem.getBrightnessForRender();
            OpenGlHelper.setLightmapTextureCoords(OpenGlHelper.lightmapTexUnit, light % 65536, light / 65536);
            this.renderer.setLightmap(golem);
            GlStateManager.depthMask(true);
            GlStateManager.disableBlend();
            GlStateManager.enableAlpha();
        }

        @Override
        public boolean shouldCombineTextures() {
            return false;
        }
    }

    /** 1.21's {@code ItemInHandLayer} on the copper golem's hands. */
    static final class HeldItem implements LayerRenderer<EntityCopperGolem> {
        private final RenderCopperGolem renderer;

        HeldItem(RenderCopperGolem renderer) {
            this.renderer = renderer;
        }

        @Override
        public void doRenderLayer(EntityCopperGolem golem, float limbSwing, float limbSwingAmount, float partialTicks, float ageInTicks,
                                  float netHeadYaw, float headPitch, float scale) {
            boolean rightMain = golem.getPrimaryHand() == EnumHandSide.RIGHT;
            ItemStack right = rightMain ? golem.getHeldItemMainhand() : golem.getHeldItemOffhand();
            ItemStack left = rightMain ? golem.getHeldItemOffhand() : golem.getHeldItemMainhand();
            render(golem, right, true, scale);
            render(golem, left, false, scale);
        }

        private void render(EntityCopperGolem golem, ItemStack stack, boolean right, float scale) {
            if (stack.isEmpty()) {
                return;
            }
            GlStateManager.pushMatrix();
            this.renderer.model().translateToHand(golem, right, scale);
            GlStateManager.rotate(-90.0F, 1.0F, 0.0F, 0.0F);
            GlStateManager.rotate(180.0F, 0.0F, 1.0F, 0.0F);
            GlStateManager.translate((right ? 1 : -1) / 16.0F, 0.125F, -0.625F);
            Minecraft.getMinecraft().getItemRenderer().renderItemSide(golem, stack, right
                    ? ItemCameraTransforms.TransformType.THIRD_PERSON_RIGHT_HAND
                    : ItemCameraTransforms.TransformType.THIRD_PERSON_LEFT_HAND, !right);
            GlStateManager.popMatrix();
        }

        @Override
        public boolean shouldCombineTextures() {
            return false;
        }
    }

    /** 1.21's {@code BlockDecorationLayer}: the block on its antenna, upright, centered on the antenna's top. */
    static final class Antenna implements LayerRenderer<EntityCopperGolem> {
        private final RenderCopperGolem renderer;

        Antenna(RenderCopperGolem renderer) {
            this.renderer = renderer;
        }

        @Override
        public void doRenderLayer(EntityCopperGolem golem, float limbSwing, float limbSwingAmount, float partialTicks, float ageInTicks,
                                  float netHeadYaw, float headPitch, float scale) {
            ItemStack stack = golem.getAntenna();
            if (stack.isEmpty() || !(stack.getItem() instanceof ItemBlock)) {
                return;
            }
            @SuppressWarnings("deprecation")
            IBlockState state = ((ItemBlock) stack.getItem()).getBlock().getStateFromMeta(stack.getMetadata());
            if (stack.getItem() == Item.getItemFromBlock(Blocks.RED_FLOWER)) {
                state = Blocks.RED_FLOWER.getStateFromMeta(stack.getMetadata());
            }
            GlStateManager.pushMatrix();
            this.renderer.model().applyBlockOnAntennaTransform(scale);
            GlStateManager.rotate(180.0F, 0.0F, 0.0F, 1.0F);
            GlStateManager.translate(-0.5F, -1.5F, -0.5F);
            this.renderer.bindTexture(TextureMap.LOCATION_BLOCKS_TEXTURE);
            Minecraft.getMinecraft().getBlockRendererDispatcher().renderBlockBrightness(state, 1.0F);
            GlStateManager.popMatrix();
        }

        @Override
        public boolean shouldCombineTextures() {
            return false;
        }
    }
}
