package com.exiledradio.trickytrialsbackport.bundle.client;

import com.exiledradio.trickytrialsbackport.TrickyTrialsBackport;
import com.exiledradio.trickytrialsbackport.bundle.BundleContents;

import net.minecraft.block.state.IBlockState;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.RenderItem;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.block.model.IBakedModel;
import net.minecraft.client.renderer.block.model.ItemCameraTransforms;
import net.minecraft.client.renderer.block.model.ItemOverrideList;
import net.minecraft.client.renderer.block.model.ModelResourceLocation;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.renderer.texture.TextureMap;
import net.minecraft.client.renderer.tileentity.TileEntityItemStackRenderer;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.registry.IRegistry;
import net.minecraft.world.World;
import net.minecraftforge.client.ForgeHooksClient;
import org.apache.commons.lang3.tuple.Pair;

import javax.annotation.Nullable;
import javax.vecmath.Matrix4f;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * The open bundle: 1.21.11's {@code items/<bundle>.json}. In a screen, while a stack is picked, the bundle is
 * drawn as its open back, the picked item, and its open front ({@code bundle_open_back} sits 16 behind,
 * {@code bundle_open_front} 16 in front, as their models say); anywhere else, and with nothing picked, the
 * closed bundle.
 *
 * <p>1.12.2's item models cannot draw another item, so the bundle's baked model is wrapped: its overrides
 * pick an open model while something is picked, the open model is drawn by {@link Renderer} in the GUI
 * and is the closed model everywhere else.
 */
final class BundleModels {

    /** item -> its open model, filled when models bake */
    private static final Map<Item, Open> OPEN = new HashMap<Item, Open>();

    private BundleModels() {
    }

    static ModelResourceLocation inventory(String name) {
        return new ModelResourceLocation(TrickyTrialsBackport.MOD_ID + ":" + name, "inventory");
    }

    /** Wraps one bundle's baked model; leaves it alone if any of its three models is missing. */
    static void wrap(IRegistry<ModelResourceLocation, IBakedModel> registry, Item item, String name) {
        IBakedModel closed = registry.getObject(inventory(name));
        IBakedModel back = registry.getObject(inventory(name + "_open_back"));
        IBakedModel front = registry.getObject(inventory(name + "_open_front"));
        if (closed == null || back == null || front == null) {
            TrickyTrialsBackport.LOGGER.warn("Bundle model {} is incomplete; it will not open.", name);
            return;
        }
        Open open = new Open(closed, back, front);
        OPEN.put(item, open);
        registry.putObject(inventory(name), new Closed(closed, open));
    }

    /** Delegates everything to a model. */
    private abstract static class Wrapper implements IBakedModel {
        final IBakedModel closed;

        Wrapper(IBakedModel closed) {
            this.closed = closed;
        }

        @Override
        public List<BakedQuad> getQuads(@Nullable IBlockState state, @Nullable EnumFacing side, long rand) {
            return this.closed.getQuads(state, side, rand);
        }

        @Override
        public boolean isAmbientOcclusion() {
            return this.closed.isAmbientOcclusion();
        }

        @Override
        public boolean isGui3d() {
            return this.closed.isGui3d();
        }

        @Override
        public TextureAtlasSprite getParticleTexture() {
            return this.closed.getParticleTexture();
        }

        @SuppressWarnings("deprecation")
        @Override
        public ItemCameraTransforms getItemCameraTransforms() {
            return this.closed.getItemCameraTransforms();
        }
    }

    /** The closed bundle, whose overrides open it while a stack is picked. */
    static final class Closed extends Wrapper {
        private final ItemOverrideList overrides;

        Closed(final IBakedModel closed, final Open open) {
            super(closed);
            this.overrides = new ItemOverrideList(Collections.emptyList()) {
                @Override
                public IBakedModel handleItemState(IBakedModel original, ItemStack stack, @Nullable World world,
                                                   @Nullable EntityLivingBase entity) {
                    if (!BundleContents.selectedStack(stack).isEmpty()) {
                        return open;
                    }
                    return closed.getOverrides().handleItemState(closed, stack, world, entity);
                }
            };
        }

        @Override
        public boolean isBuiltInRenderer() {
            return false;
        }

        @Override
        public ItemOverrideList getOverrides() {
            return this.overrides;
        }

        @Override
        public Pair<? extends IBakedModel, Matrix4f> handlePerspective(ItemCameraTransforms.TransformType type) {
            return this.closed.handlePerspective(type);
        }
    }

    /** The open bundle: drawn by {@link Renderer} in the GUI, the closed bundle anywhere else. */
    static final class Open extends Wrapper {
        final IBakedModel back;
        final IBakedModel front;

        Open(IBakedModel closed, IBakedModel back, IBakedModel front) {
            super(closed);
            this.back = back;
            this.front = front;
        }

        @Override
        public boolean isGui3d() {
            return false;
        }

        @Override
        public boolean isBuiltInRenderer() {
            return true;
        }

        @Override
        public ItemOverrideList getOverrides() {
            return ItemOverrideList.NONE;
        }

        @Override
        public Pair<? extends IBakedModel, Matrix4f> handlePerspective(ItemCameraTransforms.TransformType type) {
            if (type == ItemCameraTransforms.TransformType.GUI) {
                return Pair.of(this, null);
            }
            return this.closed.handlePerspective(type);
        }
    }

    /** Draws an open bundle in the GUI: back, the picked item, front. */
    static final class Renderer extends TileEntityItemStackRenderer {
        static final Renderer INSTANCE = new Renderer();

        @Override
        public void renderByItem(ItemStack stack) {
            Open open = OPEN.get(stack.getItem());
            if (open == null) {
                return;
            }
            RenderItem items = Minecraft.getMinecraft().getRenderItem();
            GlStateManager.pushMatrix();
            // RenderItem moved to the model's corner before calling here; the layers move there themselves.
            GlStateManager.translate(0.5F, 0.5F, 0.5F);
            layer(items, stack, open.back);
            ItemStack picked = BundleContents.selectedStack(stack);
            if (!picked.isEmpty()) {
                layer(items, picked, items.getItemModelWithOverrides(picked, null, null));
            }
            layer(items, stack, open.front);
            GlStateManager.popMatrix();
            GlStateManager.disableLighting();
        }

        private static void layer(RenderItem items, ItemStack stack, IBakedModel model) {
            GlStateManager.pushMatrix();
            Minecraft.getMinecraft().getTextureManager().bindTexture(TextureMap.LOCATION_BLOCKS_TEXTURE);
            IBakedModel transformed = ForgeHooksClient.handleCameraTransforms(model, ItemCameraTransforms.TransformType.GUI, false);
            if (transformed.isGui3d()) {
                GlStateManager.enableLighting();
            } else {
                GlStateManager.disableLighting();
            }
            items.renderItem(stack, transformed);
            GlStateManager.popMatrix();
        }
    }
}
