package com.exiledradio.trickytrialsbackport.spear.client;

import com.exiledradio.trickytrialsbackport.TrickyTrialsBackport;
import com.exiledradio.trickytrialsbackport.common.Content;
import com.exiledradio.trickytrialsbackport.network.NetworkHandler;
import com.exiledradio.trickytrialsbackport.spear.ItemSpear;
import com.exiledradio.trickytrialsbackport.spear.SpearAttacks;
import com.exiledradio.trickytrialsbackport.spear.SpearContent;
import com.exiledradio.trickytrialsbackport.spear.SpearJabMessage;
import com.exiledradio.trickytrialsbackport.spear.SpearTier;

import net.minecraft.block.state.IBlockState;
import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.block.model.IBakedModel;
import net.minecraft.client.renderer.block.model.ItemCameraTransforms;
import net.minecraft.client.renderer.block.model.ItemOverrideList;
import net.minecraft.client.renderer.block.model.ModelResourceLocation;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.settings.KeyBinding;
import net.minecraft.entity.Entity;
import net.minecraft.init.MobEffects;
import net.minecraft.item.ItemStack;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.EnumHandSide;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.registry.IRegistry;
import net.minecraftforge.client.event.InputUpdateEvent;
import net.minecraftforge.client.event.ModelBakeEvent;
import net.minecraftforge.client.event.ModelRegistryEvent;
import net.minecraftforge.client.event.MouseEvent;
import net.minecraftforge.client.event.RenderSpecificHandEvent;
import net.minecraftforge.client.model.ModelLoader;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.common.eventhandler.EventPriority;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import org.apache.commons.lang3.tuple.Pair;

import javax.annotation.Nullable;
import javax.vecmath.Matrix4f;
import java.util.List;

/**
 * The spear on a client.
 *
 * <ul>
 *   <li><b>The attack button jabs</b> (1.21.11's {@code Minecraft.startAttack}): with a spear in the main hand a click
 *       never mines or hits the usual way; at full attack charge it asks the server to jab (packet 110), restarts the
 *       timer, plays the jab and starts the thrust. Held down, it does not keep mining either.</li>
 *   <li><b>Charging does not slow you</b> (1.21.11's {@code UseEffects(canSprint, speed 1)}): 1.12.2 cuts the walk to a
 *       fifth while an item is in use, so the input is raised to undo that, and sprinting can start mid-charge.</li>
 *   <li><b>Models</b>: 1.21.11's {@code items/<spear>.json} shows the flat icon in the GUI, on the ground and in item
 *       frames, and the long {@code _in_hand} model everywhere else; the item's baked model picks by perspective.</li>
 *   <li><b>First person</b> (1.21.11's {@code ItemInHandRenderer} spear branch): raising and swaying the spear while
 *       charging, the jolt of a charge hit, and the jab's thrust over the tier's swing time.</li>
 * </ul>
 */
public final class SpearClient {

    private static long jabStart = Long.MIN_VALUE / 2;
    private static int jabTicks = 1;
    private static long hitFeedback = Long.MIN_VALUE / 2;

    private SpearClient() {
    }

    public static void init() {
        if (SpearContent.enabled()) {
            MinecraftForge.EVENT_BUS.register(new SpearClient());
        }
    }

    /** Packet 111: a charge hit; the sound at the wielder, and the wielder's own spear jolts. */
    public static void kineticHit(final int entityId, final boolean wood) {
        final Minecraft mc = Minecraft.getMinecraft();
        mc.addScheduledTask(() -> {
            if (mc.world == null) {
                return;
            }
            Entity e = mc.world.getEntityByID(entityId);
            if (e == null) {
                return;
            }
            mc.world.playSound(e.posX, e.posY, e.posZ, Content.sound(wood ? "item.spear_wood.hit" : "item.spear.hit"), e.getSoundCategory(),
                    1.0F, 1.0F, false);
            if (e == mc.player) {
                hitFeedback = mc.world.getTotalWorldTime();
            }
        });
    }

    // ------------------------------------------------------------------------------------------
    // Input
    // ------------------------------------------------------------------------------------------

    private static boolean holdsSpear(EntityPlayerSP player) {
        return player != null && player.getHeldItemMainhand().getItem() instanceof ItemSpear;
    }

    @SubscribeEvent(priority = EventPriority.HIGH)
    public void onMouse(MouseEvent event) {
        Minecraft mc = Minecraft.getMinecraft();
        EntityPlayerSP player = mc.player;
        if (!event.isButtonstate() || mc.currentScreen != null || !holdsSpear(player) || player.isSpectator()
                || event.getButton() - 100 != mc.gameSettings.keyBindAttack.getKeyCode()) {
            return;
        }
        event.setCanceled(true);
        if (player.isHandActive() || !SpearAttacks.charged(player, 0.0F)) {
            return;
        }
        SpearTier tier = ((ItemSpear) player.getHeldItemMainhand().getItem()).getTier();
        NetworkHandler.channel.sendToServer(new SpearJabMessage());
        player.resetCooldown();
        player.world.playSound(player, player.posX, player.posY, player.posZ,
                Content.sound(tier == SpearTier.WOOD ? "item.spear_wood.attack" : "item.spear.attack"), player.getSoundCategory(), 1.0F, 1.0F);
        player.swingArm(EnumHand.MAIN_HAND);
        jabStart = player.world.getTotalWorldTime();
        jabTicks = Math.max(1, tier.swingTicks);
    }

    @SubscribeEvent
    public void onClientTick(TickEvent.ClientTickEvent event) {
        Minecraft mc = Minecraft.getMinecraft();
        if (event.phase != TickEvent.Phase.START || mc.currentScreen != null || !holdsSpear(mc.player)) {
            return;
        }
        KeyBinding attack = mc.gameSettings.keyBindAttack;
        if (attack.isKeyDown()) {
            // A spear never mines: a button held from before the spear was taken out lets go.
            KeyBinding.setKeyBindState(attack.getKeyCode(), false);
        }
    }

    private static boolean charging(EntityPlayerSP player) {
        return player.isHandActive() && player.getActiveItemStack().getItem() instanceof ItemSpear && !player.isRiding();
    }

    @SubscribeEvent
    public void onInput(InputUpdateEvent event) {
        if (event.getEntityPlayer() instanceof EntityPlayerSP && charging((EntityPlayerSP) event.getEntityPlayer())) {
            // 1.12.2 multiplies both by 0.2 right after this event while an item is in use.
            event.getMovementInput().moveForward *= 5.0F;
            event.getMovementInput().moveStrafe *= 5.0F;
        }
    }

    @SubscribeEvent
    public void onPlayerTick(TickEvent.PlayerTickEvent event) {
        Minecraft mc = Minecraft.getMinecraft();
        if (event.phase != TickEvent.Phase.START || event.player != mc.player || !charging(mc.player)) {
            return;
        }
        EntityPlayerSP p = mc.player;
        if (!p.isSprinting() && mc.gameSettings.keyBindSprint.isKeyDown() && p.movementInput.moveForward >= 0.8F
                && (p.getFoodStats().getFoodLevel() > 6 || p.capabilities.allowFlying) && !p.collidedHorizontally
                && !p.isPotionActive(MobEffects.BLINDNESS)) {
            p.setSprinting(true);
        }
    }

    // ------------------------------------------------------------------------------------------
    // First person
    // ------------------------------------------------------------------------------------------

    @SubscribeEvent
    public void onRenderHand(RenderSpecificHandEvent event) {
        ItemStack stack = event.getItemStack();
        if (!(stack.getItem() instanceof ItemSpear)) {
            return;
        }
        Minecraft mc = Minecraft.getMinecraft();
        EntityPlayerSP player = mc.player;
        if (player == null) {
            return;
        }
        event.setCanceled(true);
        SpearTier tier = ((ItemSpear) stack.getItem()).getTier();
        EnumHand hand = event.getHand();
        EnumHandSide side = hand == EnumHand.MAIN_HAND ? player.getPrimaryHand() : player.getPrimaryHand().opposite();
        boolean right = side == EnumHandSide.RIGHT;
        int i = right ? 1 : -1;
        float partial = event.getPartialTicks();
        long now = player.world.getTotalWorldTime();

        GlStateManager.pushMatrix();
        if (player.isHandActive() && player.getActiveHand() == hand) {
            GlStateManager.translate(i * 0.56F, -0.52F, -0.72F);
            float used = stack.getMaxItemUseDuration() - (player.getItemInUseCount() - partial + 1.0F);
            SpearPoses.firstPersonUse(now - hitFeedback + partial, used, i, tier);
        } else {
            GlStateManager.translate(i * 0.56F, -0.52F + event.getEquipProgress() * -0.6F, -0.72F);
            if (hand == EnumHand.MAIN_HAND) {
                float p = (now - jabStart + partial) / jabTicks;
                if (p >= 0.0F && p < 1.0F) {
                    SpearPoses.firstPersonAttack(p, i);
                }
            }
        }
        mc.getItemRenderer().renderItemSide(player, stack,
                right ? ItemCameraTransforms.TransformType.FIRST_PERSON_RIGHT_HAND : ItemCameraTransforms.TransformType.FIRST_PERSON_LEFT_HAND,
                !right);
        GlStateManager.popMatrix();
    }

    // ------------------------------------------------------------------------------------------
    // Models
    // ------------------------------------------------------------------------------------------

    private static ModelResourceLocation inventory(String name) {
        return new ModelResourceLocation(TrickyTrialsBackport.MOD_ID + ":" + name, "inventory");
    }

    @SubscribeEvent
    public void registerModels(ModelRegistryEvent event) {
        for (ItemSpear spear : SpearContent.spears().values()) {
            String name = spear.getTier().itemName;
            ModelLoader.registerItemVariants(spear, new ResourceLocation(TrickyTrialsBackport.MOD_ID, name),
                    new ResourceLocation(TrickyTrialsBackport.MOD_ID, name + "_in_hand"));
        }
    }

    @SubscribeEvent
    public void bakeModels(ModelBakeEvent event) {
        IRegistry<ModelResourceLocation, IBakedModel> registry = event.getModelRegistry();
        for (ItemSpear spear : SpearContent.spears().values()) {
            String name = spear.getTier().itemName;
            IBakedModel icon = registry.getObject(inventory(name));
            IBakedModel held = registry.getObject(inventory(name + "_in_hand"));
            if (icon == null || held == null) {
                TrickyTrialsBackport.LOGGER.warn("Spear model {} is incomplete; it is held as its icon.", name);
                continue;
            }
            registry.putObject(inventory(name), new ByPerspective(icon, new SeamlessHeldModel(held)));
        }
    }

    /** The icon in the GUI, on the ground and in frames; the in-hand model held, worn and in first person. */
    static final class ByPerspective implements IBakedModel {
        final IBakedModel icon;
        final IBakedModel held;

        ByPerspective(IBakedModel icon, IBakedModel held) {
            this.icon = icon;
            this.held = held;
        }

        static boolean flat(ItemCameraTransforms.TransformType type) {
            return type == ItemCameraTransforms.TransformType.GUI || type == ItemCameraTransforms.TransformType.GROUND
                    || type == ItemCameraTransforms.TransformType.FIXED;
        }

        @Override
        public Pair<? extends IBakedModel, Matrix4f> handlePerspective(ItemCameraTransforms.TransformType type) {
            return (flat(type) ? this.icon : this.held).handlePerspective(type);
        }

        @Override
        public List<BakedQuad> getQuads(@Nullable IBlockState state, @Nullable EnumFacing side, long rand) {
            return this.icon.getQuads(state, side, rand);
        }

        @Override
        public boolean isAmbientOcclusion() {
            return this.icon.isAmbientOcclusion();
        }

        @Override
        public boolean isGui3d() {
            return this.icon.isGui3d();
        }

        @Override
        public boolean isBuiltInRenderer() {
            return false;
        }

        @Override
        public TextureAtlasSprite getParticleTexture() {
            return this.icon.getParticleTexture();
        }

        @SuppressWarnings("deprecation")
        @Override
        public ItemCameraTransforms getItemCameraTransforms() {
            return this.icon.getItemCameraTransforms();
        }

        @Override
        public ItemOverrideList getOverrides() {
            return this.icon.getOverrides();
        }
    }
}
