package com.exiledradio.trickytrialsbackport.selftest;

import com.exiledradio.trickytrialsbackport.TrickyTrialsBackport;
import com.exiledradio.trickytrialsbackport.bundle.BundleContent;
import com.exiledradio.trickytrialsbackport.bundle.BundleContents;
import com.exiledradio.trickytrialsbackport.bundle.BundleTooltipLines;
import com.exiledradio.trickytrialsbackport.common.Guis;
import com.exiledradio.trickytrialsbackport.coppergear.CopperGearContent;
import com.exiledradio.trickytrialsbackport.crafter.CrafterContent;
import com.exiledradio.trickytrialsbackport.crafter.client.GuiCrafter;
import com.exiledradio.trickytrialsbackport.creaking.EntityCreaking;
import com.exiledradio.trickytrialsbackport.creaking.client.ModelCreaking;
import com.exiledradio.trickytrialsbackport.ghast.EntityHappyGhast;
import com.exiledradio.trickytrialsbackport.ghast.GhastContent;
import com.exiledradio.trickytrialsbackport.nautilus.EntityNautilus;
import com.exiledradio.trickytrialsbackport.nautilus.client.GuiNautilus;
import com.exiledradio.trickytrialsbackport.shelf.BlockShelf;
import com.exiledradio.trickytrialsbackport.shelf.ShelfContent;
import com.exiledradio.trickytrialsbackport.shelf.ShelfWood;
import com.exiledradio.trickytrialsbackport.shelf.TileEntityShelf;
import com.exiledradio.trickytrialsbackport.shelf.client.RenderShelf;
import com.exiledradio.trickytrialsbackport.spear.ItemSpear;
import com.exiledradio.trickytrialsbackport.spear.SpearContent;
import com.exiledradio.trickytrialsbackport.spear.SpearPoseMath;
import com.exiledradio.trickytrialsbackport.spear.SpearTier;
import net.minecraft.block.BlockTorch;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.inventory.GuiInventory;
import net.minecraft.client.model.ModelBase;
import net.minecraft.client.model.ModelBiped;
import net.minecraft.client.model.ModelRenderer;
import net.minecraft.client.renderer.block.model.IBakedModel;
import net.minecraft.client.renderer.block.model.ItemCameraTransforms;
import net.minecraft.client.renderer.entity.Render;
import net.minecraft.client.renderer.entity.RenderLivingBase;
import net.minecraft.client.renderer.tileentity.TileEntityRendererDispatcher;
import net.minecraft.client.settings.KeyBinding;
import net.minecraft.client.util.ITooltipFlag;
import com.exiledradio.trickytrialsbackport.decor.PaintingItems;
import net.minecraft.entity.item.EntityPainting;
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.enchantment.EnchantmentData;
import net.minecraft.entity.Entity;
import net.minecraft.entity.monster.EntityZombie;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.init.Blocks;
import net.minecraft.init.Items;
import net.minecraft.init.MobEffects;
import net.minecraft.item.ItemEnchantedBook;
import net.minecraft.item.ItemStack;
import net.minecraft.potion.PotionEffect;
import net.minecraft.server.integrated.IntegratedServer;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.MovementInput;
import net.minecraft.util.ScreenShotHelper;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.world.EnumDifficulty;
import net.minecraft.world.GameType;
import net.minecraft.world.WorldServer;
import net.minecraftforge.client.event.GuiScreenEvent;
import net.minecraftforge.client.event.RenderTooltipEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.client.FMLClientHandler;
import net.minecraftforge.fml.client.IModGuiFactory;
import net.minecraftforge.fml.client.config.GuiUtils;
import net.minecraftforge.fml.common.Loader;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

import java.io.File;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.TimeUnit;
import java.util.function.BiConsumer;

/**
 * {@link ClientSelfTest}'s scenes, each built on the integrated server in code and checked on the client's own copies
 * of the entities, models and screens, with photos and no input:
 * <ul>
 *   <li>the spear: its held model by perspective, 1.21.11's mob arm pose through patch P21 on the real client model,
 *       a player's plain pose until a charge and the charge's pose;</li>
 *   <li>steering: a harnessed happy ghast and a saddled nautilus (under water), each ridden by the client player with
 *       forward held as its own keys give it, moving on the client and taken by the server; the nautilus's
 *       inventory screen;</li>
 *   <li>the creaking: frozen while looked at, its walk animation (limbSwing) over a chase, frozen again mid-chase;</li>
 *   <li>a shelf's three items on the client and its renderer;</li>
 *   <li>screens: the crafter, the bundle's tooltip, the Lunge book's name and this mod's settings screen.</li>
 * </ul>
 */
final class ClientScenes {

    private static final float EPS = 1.0E-3F;

    private final Minecraft mc;
    private final File shots;
    private final BiConsumer<Boolean, String> ok;
    private final List<Step> steps = new ArrayList<Step>();
    private int step;
    private int wait;
    private BlockPos base;

    /** One scene step: runs once, then the given number of ticks pass before the next. */
    private interface Step {
        int run() throws Exception;
    }

    ClientScenes(Minecraft mc, File shots, BiConsumer<Boolean, String> ok) {
        this.mc = mc;
        this.shots = shots;
        this.ok = ok;
        this.steps.add(this::platform);
        copperTorch();
        spear();
        ghast();
        nautilus();
        creaking();
        shelf();
        paintings();
        screens();
    }

    /** @return true when done */
    boolean tick() throws Exception {
        if (this.mc.isGamePaused() && this.mc.currentScreen == null) {
            return false;
        }
        if (this.wait > 0) {
            this.wait--;
            return false;
        }
        if (this.step >= this.steps.size()) {
            return true;
        }
        this.wait = this.steps.get(this.step++).run();
        return false;
    }

    private void ok(boolean condition, String what) {
        this.ok.accept(condition, what);
    }

    private void shot(String name) {
        ScreenShotHelper.saveScreenshot(this.shots, name, this.mc.displayWidth, this.mc.displayHeight, this.mc.getFramebuffer());
        File f = new File(new File(this.shots, "screenshots"), name);
        ok(f.isFile() && f.length() > 1000, "saved " + name);
    }

    // ---- the server side ---------------------------------------------------------------------------------------

    private IntegratedServer server() {
        return this.mc.getIntegratedServer();
    }

    private WorldServer serverWorld() {
        return server().getWorld(0);
    }

    private EntityPlayerMP serverPlayer() {
        return server().getPlayerList().getPlayerByUUID(this.mc.player.getUniqueID());
    }

    /** Runs on the server thread and waits for it. */
    private <T> T onServer(Callable<T> task) throws Exception {
        return server().callFromMainThread(task).get(30, TimeUnit.SECONDS);
    }

    private Entity serverEntity(int id) {
        return serverWorld().getEntityByID(id);
    }

    private int look(BlockPos feet, float yaw, float pitch) throws Exception {
        onServer(() -> {
            EntityPlayerMP p = serverPlayer();
            p.dismountRidingEntity();
            p.connection.setPlayerLocation(feet.getX() + 0.5D, feet.getY(), feet.getZ() + 0.5D, yaw, pitch);
            return null;
        });
        return 15;
    }

    private void kill(int id) throws Exception {
        onServer(() -> {
            Entity e = serverEntity(id);
            if (e != null) {
                e.setDead();
            }
            return null;
        });
    }

    private int platform() throws Exception {
        this.mc.gameSettings.thirdPersonView = 0;
        this.mc.gameSettings.pauseOnLostFocus = false;
        onServer(() -> {
            WorldServer w = serverWorld();
            BlockPos spawn = w.getSpawnPoint();
            this.base = new BlockPos(spawn.getX(), w.getHeight(spawn).getY(), spawn.getZ());
            w.getGameRules().setOrCreateGameRule("doDaylightCycle", "false");
            w.getGameRules().setOrCreateGameRule("doMobSpawning", "false");
            w.getGameRules().setOrCreateGameRule("doWeatherCycle", "false");
            w.setWorldTime(6000L);
            w.getWorldInfo().setRaining(false);
            w.getWorldInfo().setThundering(false);
            server().setDifficultyForAllWorlds(EnumDifficulty.NORMAL);
            serverPlayer().capabilities.isFlying = false;
            serverPlayer().sendPlayerAbilities();
            return null;
        });
        ok(this.base.getY() > 0, "the superflat ground is at y " + this.base.getY());
        look(this.base, 0.0F, 10.0F);
        return 60;
    }

    // ---- riding: forward held, as a player's own keys give it ----------------------------------------------------

    static final class Forward extends MovementInput {
        @Override
        public void updatePlayerMoveState() {
            this.moveStrafe = 0.0F;
            this.moveForward = 1.0F;
            this.forwardKeyDown = true;
            this.backKeyDown = false;
            this.leftKeyDown = false;
            this.rightKeyDown = false;
            this.jump = false;
            this.sneak = false;
        }
    }

    private MovementInput keys;
    private Entity mount;
    private double startX;
    private double startZ;
    private double serverStartZ;

    private void steer(String name, int id) throws Exception {
        this.mount = this.mc.world.getEntityByID(id);
        ok(this.mount != null && this.mc.player.getRidingEntity() == this.mount, name + ": the client player rides it (" + this.mc.player.getRidingEntity() + ")");
        if (this.mount != null) {
            ok(this.mount.getControllingPassenger() == this.mc.player, name + ": the rider steers (" + this.mount.getControllingPassenger() + ")");
            ok(this.mount.canPassengerSteer(), name + ": the rider's client moves it");
            this.startX = this.mount.posX;
            this.startZ = this.mount.posZ;
        }
        this.serverStartZ = onServer(() -> serverEntity(id).posZ);
        this.mc.player.rotationYaw = 0.0F;
        this.mc.player.rotationPitch = 0.0F;
        this.keys = this.mc.player.movementInput;
        this.mc.player.movementInput = new Forward();
        this.mc.gameSettings.thirdPersonView = 1;
    }

    private void steered(String name, int id, double minDistance) throws Exception {
        this.mc.player.movementInput = this.keys;
        this.mc.gameSettings.thirdPersonView = 0;
        if (this.mount == null) {
            return;
        }
        double moved = this.mount.posZ - this.startZ;
        double serverMoved = onServer(() -> {
            Entity e = serverEntity(id);
            return e == null ? Double.NaN : e.posZ;
        }) - this.serverStartZ;
        ok(moved >= minDistance, name + ": forward moves it on the client: " + String.format("%.2f", moved) + " blocks in 2 s");
        ok(serverMoved >= minDistance, name + ": and the server takes the move: " + String.format("%.2f", serverMoved));
        double side = Math.abs(this.mount.posX - this.startX);
        ok(side < 0.5D + 0.1D * Math.abs(moved), name + ": it went the rider's way: " + String.format("%.2f", side) + " sideways");
    }

    // ---- the spear ---------------------------------------------------------------------------------------------

    private int zombie;
    private float idleArm;

    private ModelBase model(Entity e) {
        Render<Entity> r = this.mc.getRenderManager().getEntityRenderObject(e);
        return r instanceof RenderLivingBase ? ((RenderLivingBase<?>) r).getMainModel() : null;
    }

    private void spear() {
        this.steps.add(() -> {
            BlockPos at = this.base.add(0, 0, 3);
            this.zombie = onServer(() -> {
                WorldServer w = serverWorld();
                EntityZombie z = new EntityZombie(w);
                z.setLocationAndAngles(at.getX() + 0.5D, at.getY(), at.getZ() + 0.5D, 180.0F, 0.0F);
                z.setNoAI(true);
                z.setChild(false);
                z.setLeftHanded(false);
                z.setHeldItem(EnumHand.MAIN_HAND, new ItemStack(SpearContent.spear(SpearTier.IRON)));
                w.spawnEntity(z);
                serverPlayer().setHeldItem(EnumHand.MAIN_HAND, new ItemStack(SpearContent.spear(SpearTier.IRON)));
                return z.getEntityId();
            });
            look(this.base, 0.0F, 10.0F);
            return 30;
        });
        this.steps.add(() -> {
            ItemStack held = this.mc.player.getHeldItemMainhand();
            ok(held.getItem() instanceof ItemSpear, "spear: the client player holds an iron spear: " + held);
            IBakedModel item = this.mc.getRenderItem().getItemModelWithOverrides(held, this.mc.world, this.mc.player);
            IBakedModel gui = item.handlePerspective(ItemCameraTransforms.TransformType.GUI).getLeft();
            IBakedModel hand = item.handlePerspective(ItemCameraTransforms.TransformType.FIRST_PERSON_RIGHT_HAND).getLeft();
            IBakedModel third = item.handlePerspective(ItemCameraTransforms.TransformType.THIRD_PERSON_RIGHT_HAND).getLeft();
            IBakedModel ground = item.handlePerspective(ItemCameraTransforms.TransformType.GROUND).getLeft();
            ok(gui != hand, "spear: the GUI shows the flat icon, the hand the long model (" + gui.getClass().getSimpleName() + ", "
                    + hand.getClass().getSimpleName() + ")");
            ok(hand.getClass() == third.getClass() && ground.getClass() != hand.getClass(), "spear: third person holds the long model too ("
                    + third.getClass().getSimpleName() + "); the ground shows the icon (" + ground.getClass().getSimpleName() + ")");
            ok(!gui.getQuads(null, null, 0L).isEmpty() && !hand.getQuads(null, null, 0L).isEmpty(), "spear: both models have quads");
            ok(hand.getParticleTexture() != this.mc.getTextureMapBlocks().getMissingSprite(), "spear: the held model's texture is found: "
                    + hand.getParticleTexture().getIconName());

            // 1.21.11's mob pose through P21, on the client's own zombie and its real (patched) model.
            Entity z = this.mc.world.getEntityByID(this.zombie);
            ok(z instanceof EntityZombie && ((EntityZombie) z).getHeldItemMainhand().getItem() instanceof ItemSpear,
                    "spear: the client's zombie holds it");
            ModelBase m = z == null ? null : model(z);
            ok(m instanceof ModelBiped, "spear: the zombie's model is a biped: " + m);
            if (m instanceof ModelBiped) {
                ModelBiped b = (ModelBiped) m;
                b.swingProgress = 0.0F;
                b.setRotationAngles(0.0F, 0.0F, z.ticksExisted, 0.0F, 0.0F, 0.0625F, z);
                float want = SpearPoseMath.handUse(0.0F, 0.0F, 0.0F, true, SpearTier.IRON, 0.0F, false)[0];
                ok(Math.abs(b.bipedRightArm.rotateAngleX - want) < EPS, "spear: an idle zombie holds the spear upright, arm at "
                        + b.bipedRightArm.rotateAngleX + " (1.21.11: " + want + ")");
                ok(Math.abs(b.bipedLeftArm.rotateAngleX - want) < EPS, "spear: and its other arm takes the pose too: " + b.bipedLeftArm.rotateAngleX);
                ok(Math.abs(b.bipedRightArm.rotateAngleX + (float) Math.PI / 2.0F) > 0.5F, "spear: not 1.12.2's zombie arms out");
            }
            ModelBase pm = model(this.mc.player);
            if (pm instanceof ModelBiped) {
                ModelBiped b = (ModelBiped) pm;
                b.swingProgress = 0.0F;
                b.setRotationAngles(0.0F, 0.0F, this.mc.player.ticksExisted, 0.0F, 0.0F, 0.0625F, this.mc.player);
                this.idleArm = b.bipedRightArm.rotateAngleX;
                float mob = SpearPoseMath.handUse(0.0F, 0.0F, 0.0F, true, SpearTier.IRON, 0.0F, false)[0];
                ok(Math.abs(this.idleArm - mob) > 0.2F, "spear: an idle player keeps the plain item pose: " + this.idleArm);
            } else {
                ok(false, "spear: the player's model is a biped: " + pm);
            }
            return 1;
        });
        this.steps.add(() -> {
            shot("spear-first-person.png");
            this.mc.gameSettings.thirdPersonView = 2;
            return 10;
        });
        this.steps.add(() -> {
            shot("spear-third-person.png");
            this.mc.gameSettings.thirdPersonView = 0;
            // A charge, as the use key starts it (the server is told too). 1.12.2 ends a use once the use key is up,
            // so the game's own key binding is held down meanwhile (its state only: no key is pressed).
            KeyBinding.setKeyBindState(this.mc.gameSettings.keyBindUseItem.getKeyCode(), true);
            this.mc.playerController.processRightClick(this.mc.player, this.mc.world, EnumHand.MAIN_HAND);
            return 12;
        });
        this.steps.add(() -> {
            ok(this.mc.player.isHandActive() && this.mc.player.getActiveItemStack().getItem() instanceof ItemSpear,
                    "spear: the use key charges it");
            ModelBase pm = model(this.mc.player);
            if (pm instanceof ModelBiped) {
                ModelBiped b = (ModelBiped) pm;
                b.swingProgress = 0.0F;
                b.setRotationAngles(0.0F, 0.0F, this.mc.player.ticksExisted, 0.0F, 0.0F, 0.0625F, this.mc.player);
                float want = SpearPoseMath.handUse(b.bipedHead.rotateAngleX, b.bipedHead.rotateAngleY, 0.0F, true, SpearTier.IRON,
                        this.mc.player.getItemInUseMaxCount(), false)[0];
                ok(Math.abs(b.bipedRightArm.rotateAngleX - want) < EPS, "spear: charging, the player's arm takes 1.21.11's spear pose: "
                        + b.bipedRightArm.rotateAngleX + " (" + want + ")");
                ok(Math.abs(b.bipedRightArm.rotateAngleX - this.idleArm) > 0.2F, "spear: unlike the idle pose");
            }
            shot("spear-charging.png");
            this.mc.gameSettings.thirdPersonView = 2;
            return 5;
        });
        this.steps.add(() -> {
            shot("spear-charging-third-person.png");
            this.mc.gameSettings.thirdPersonView = 0;
            KeyBinding.setKeyBindState(this.mc.gameSettings.keyBindUseItem.getKeyCode(), false);
            this.mc.playerController.onStoppedUsingItem(this.mc.player);
            kill(this.zombie);
            onServer(() -> {
                serverPlayer().setHeldItem(EnumHand.MAIN_HAND, ItemStack.EMPTY);
                return null;
            });
            return 10;
        });
    }

    // ---- the happy ghast ---------------------------------------------------------------------------------------

    private int ghastId;

    private void ghast() {
        this.steps.add(() -> {
            BlockPos at = this.base.add(-30, 3, 0);
            this.ghastId = onServer(() -> {
                WorldServer w = serverWorld();
                EntityHappyGhast g = new EntityHappyGhast(w);
                g.setLocationAndAngles(at.getX() + 0.5D, at.getY(), at.getZ() + 0.5D, 0.0F, 0.0F);
                w.spawnEntity(g);
                g.setHarness(new ItemStack(GhastContent.HARNESSES.get("red")));
                EntityPlayerMP p = serverPlayer();
                p.connection.setPlayerLocation(at.getX() + 0.5D, at.getY() + 5.0D, at.getZ() + 0.5D, 0.0F, 0.0F);
                p.startRiding(g, true);
                return g.getEntityId();
            });
            return 40;
        });
        this.steps.add(() -> {
            Entity g = this.mc.world.getEntityByID(this.ghastId);
            ok(g instanceof EntityHappyGhast && ((EntityHappyGhast) g).isWearingHarness(), "happy ghast: the client sees the harness");
            steer("happy ghast", this.ghastId);
            return 40;
        });
        this.steps.add(() -> {
            shot("happy-ghast-ride.png");
            steered("happy ghast", this.ghastId, 1.0D);
            look(this.base, 0.0F, 10.0F);
            kill(this.ghastId);
            return 10;
        });
    }

    // ---- the nautilus ------------------------------------------------------------------------------------------

    private int nautilusId;

    private void nautilus() {
        this.steps.add(() -> {
            BlockPos pool = this.base.add(40, 0, 0);
            this.nautilusId = onServer(() -> {
                WorldServer w = serverWorld();
                for (int x = -5; x <= 5; x++) {
                    for (int z = -5; z <= 25; z++) {
                        for (int y = 0; y <= 6; y++) {
                            boolean wall = x == -5 || x == 5 || z == -5 || z == 25;
                            w.setBlockState(pool.add(x, y, z), (wall ? Blocks.GLASS : y == 6 ? Blocks.AIR : Blocks.WATER).getDefaultState(), 2);
                        }
                    }
                }
                EntityNautilus n = new EntityNautilus(w);
                n.setLocationAndAngles(pool.getX() + 0.5D, pool.getY() + 2.0D, pool.getZ() + 0.5D, 0.0F, 0.0F);
                w.spawnEntity(n);
                EntityPlayerMP p = serverPlayer();
                n.setTamedBy(p);
                n.setSaddle(new ItemStack(Items.SADDLE));
                p.connection.setPlayerLocation(pool.getX() + 0.5D, pool.getY() + 2.0D, pool.getZ() + 0.5D, 0.0F, 0.0F);
                p.startRiding(n, true);
                return n.getEntityId();
            });
            return 40;
        });
        this.steps.add(() -> {
            Entity n = this.mc.world.getEntityByID(this.nautilusId);
            ok(n instanceof EntityNautilus && ((EntityNautilus) n).isSaddled() && n.isInWater(), "nautilus: saddled, in the water, on the client");
            steer("nautilus", this.nautilusId);
            return 40;
        });
        this.steps.add(() -> {
            shot("nautilus-ride.png");
            steered("nautilus", this.nautilusId, 1.5D);
            onServer(() -> {
                Entity n = serverEntity(this.nautilusId);
                if (n instanceof EntityNautilus) {
                    ((EntityNautilus) n).openInventory(serverPlayer());
                }
                return null;
            });
            return 15;
        });
        this.steps.add(() -> {
            ok(this.mc.currentScreen instanceof GuiNautilus, "nautilus: its inventory screen opens: " + this.mc.currentScreen);
            shot("nautilus-inventory.png");
            onServer(() -> {
                serverPlayer().closeScreen();
                return null;
            });
            look(this.base, 0.0F, 10.0F);
            kill(this.nautilusId);
            return 10;
        });
    }

    // ---- the creaking ------------------------------------------------------------------------------------------

    private int creakingId;
    private EntityCreaking creaking;
    private ModelRenderer leg;
    private float restLeg;
    private double z0;
    private float maxAmount;
    private float maxLegTurn;
    private boolean movedSometime;

    private float legAngle() {
        ModelCreaking m = (ModelCreaking) model(this.creaking);
        EntityCreaking c = this.creaking;
        m.setRotationAngles(c.limbSwing, c.limbSwingAmount, c.ticksExisted, 0.0F, 0.0F, 0.0625F, c);
        return this.leg.rotateAngleX;
    }

    private void creaking() {
        BlockPos[] at = new BlockPos[1];
        this.steps.add(() -> {
            at[0] = this.base.add(-60, 0, 0);
            this.creakingId = onServer(() -> {
                WorldServer w = serverWorld();
                EntityPlayerMP p = serverPlayer();
                p.setGameType(GameType.SURVIVAL);
                p.addPotionEffect(new PotionEffect(MobEffects.RESISTANCE, 2400, 4, false, false));
                EntityCreaking c = new EntityCreaking(w);
                c.setLocationAndAngles(at[0].getX() + 0.5D, at[0].getY(), at[0].getZ() + 10.5D, 180.0F, 0.0F);
                w.spawnEntity(c);
                return c.getEntityId();
            });
            // Looking at it: it wakes and freezes.
            look(at[0], 0.0F, 0.0F);
            return 40;
        });
        this.steps.add(() -> {
            Entity e = this.mc.world.getEntityByID(this.creakingId);
            ok(e instanceof EntityCreaking, "creaking: on the client: " + e);
            if (!(e instanceof EntityCreaking)) {
                return 1;
            }
            this.creaking = (EntityCreaking) e;
            ModelBase m = model(this.creaking);
            ok(m instanceof ModelCreaking, "creaking: drawn with its 1.21.11 model: " + m);
            Field f = ModelCreaking.class.getDeclaredField("leftLeg");
            f.setAccessible(true);
            this.leg = (ModelRenderer) f.get(m);
            ok(!this.creaking.canMove(), "creaking: frozen while looked at (client)");
            ok(onServer(() -> ((EntityCreaking) serverEntity(this.creakingId)).isActive()), "creaking: awake on the server");
            this.z0 = this.creaking.posZ;
            return 20;
        });
        this.steps.add(() -> {
            if (this.creaking == null) {
                return 1;
            }
            ok(Math.abs(this.creaking.posZ - this.z0) < 0.05D, "creaking: frozen, it stays: " + (this.creaking.posZ - this.z0));
            ok(this.creaking.limbSwingAmount < 0.05F, "creaking: frozen, its legs rest: limbSwingAmount " + this.creaking.limbSwingAmount);
            this.restLeg = legAngle();
            shot("creaking-frozen.png");
            // Turning away: it chases.
            look(at[0], 180.0F, 0.0F);
            this.mc.gameSettings.thirdPersonView = 2;
            this.z0 = this.creaking.posZ;
            this.maxAmount = 0.0F;
            this.maxLegTurn = 0.0F;
            this.movedSometime = false;
            return 1;
        });
        for (int i = 0; i < 30; i++) {
            this.steps.add(() -> {
                if (this.creaking != null) {
                    this.movedSometime |= this.creaking.canMove();
                    this.maxAmount = Math.max(this.maxAmount, this.creaking.limbSwingAmount);
                    this.maxLegTurn = Math.max(this.maxLegTurn, Math.abs(legAngle() - this.restLeg));
                }
                return 0;
            });
        }
        this.steps.add(() -> {
            if (this.creaking == null) {
                return 1;
            }
            shot("creaking-chase.png");
            ok(this.movedSometime, "creaking: free to move once no one looks");
            ok(this.z0 - this.creaking.posZ > 1.5D, "creaking: it chased: " + String.format("%.2f", this.z0 - this.creaking.posZ) + " blocks");
            ok(this.maxAmount > 0.5F, "creaking: its walk swings its limbs on the client: limbSwingAmount up to " + this.maxAmount);
            ok(this.maxLegTurn > 0.1F, "creaking: the walk animation moves its legs: up to " + this.maxLegTurn + " rad");
            // Looking back: it freezes mid-chase.
            this.mc.gameSettings.thirdPersonView = 0;
            BlockPos here = new BlockPos(this.mc.player);
            look(here, 0.0F, 0.0F);
            return 25;
        });
        this.steps.add(() -> {
            if (this.creaking != null) {
                this.z0 = this.creaking.posZ;
            }
            return 10;
        });
        this.steps.add(() -> {
            if (this.creaking != null) {
                ok(!this.creaking.canMove(), "creaking: looked at again, it freezes");
                ok(Math.abs(this.creaking.posZ - this.z0) < 0.05D, "creaking: and stays: " + (this.creaking.posZ - this.z0));
                ok(this.creaking.limbSwingAmount < 0.05F, "creaking: its limbs stop: " + this.creaking.limbSwingAmount);
                ok(Math.abs(legAngle() - this.restLeg) < 1.0E-4F, "creaking: no walk animation while frozen");
                shot("creaking-frozen-again.png");
            }
            kill(this.creakingId);
            onServer(() -> {
                EntityPlayerMP p = serverPlayer();
                p.clearActivePotions();
                p.setGameType(GameType.CREATIVE);
                return null;
            });
            look(this.base, 0.0F, 10.0F);
            return 10;
        });
    }

    // ---- copper torch ------------------------------------------------------------------------------------------

    private BlockPos torchPillar;
    private int torchParticles;

    /** Every facing of the copper torch spawns its green flame (2026-11-21: the flame's layer override crashed). */
    private void copperTorch() {
        EnumFacing[] facings = {EnumFacing.UP, EnumFacing.NORTH, EnumFacing.EAST, EnumFacing.WEST, EnumFacing.SOUTH};
        this.steps.add(() -> {
            BlockPos feet = this.base.add(-15, 0, 0);
            this.torchPillar = feet.add(0, 1, 2);
            onServer(() -> {
                WorldServer w = serverWorld();
                w.setBlockState(this.torchPillar.down(), Blocks.STONE.getDefaultState(), 2);
                w.setBlockState(this.torchPillar, Blocks.STONE.getDefaultState(), 2);
                for (EnumFacing f : facings) {
                    w.setBlockState(this.torchPillar.offset(f), CopperGearContent.torch.getDefaultState().withProperty(BlockTorch.FACING, f), 3);
                }
                return null;
            });
            look(feet, 0.0F, 10.0F);
            return 30;
        });
        this.steps.add(() -> {
            int before = particles();
            for (EnumFacing f : facings) {
                BlockPos at = this.torchPillar.offset(f);
                IBlockState state = this.mc.world.getBlockState(at);
                ok(state.getBlock() == CopperGearContent.torch && state.getValue(BlockTorch.FACING) == f,
                        "copper torch: the client has the " + f + " torch: " + state);
                for (int i = 0; i < 4; i++) {
                    state.getBlock().randomDisplayTick(state, this.mc.world, at, this.mc.world.rand);
                }
            }
            this.torchParticles = before;
            return 2;
        });
        this.steps.add(() -> {
            // A smoke and a flame per call, for every facing: the flame is built (no setMiscTex throw) and added. The
            // particle manager takes new particles in on its next tick, so they are counted now.
            int after = particles();
            ok(after - this.torchParticles >= facings.length * 4 * 2,
                    "copper torch: flames and smoke spawned: " + this.torchParticles + " -> " + after);
            return 40;
        });
        this.steps.add(() -> {
            shot("copper_torch.png");
            return 1;
        });
    }

    private int particles() {
        return Integer.parseInt(this.mc.effectRenderer.getStatistics().trim());
    }

    // ---- shelves -----------------------------------------------------------------------------------------------

    private BlockPos shelfPos;

    private void shelf() {
        ItemStack[] items = {new ItemStack(Items.DIAMOND), new ItemStack(Items.APPLE), new ItemStack(SpearContent.spear(SpearTier.IRON))};
        this.steps.add(() -> {
            BlockPos feet = this.base.add(15, 0, 0);
            this.shelfPos = feet.add(0, 1, 2);
            onServer(() -> {
                WorldServer w = serverWorld();
                BlockShelf shelf = ShelfContent.shelves().get(ShelfWood.OAK);
                w.setBlockState(this.shelfPos.down(), Blocks.STONE.getDefaultState(), 2);
                w.setBlockState(this.shelfPos, shelf.getDefaultState().withProperty(BlockShelf.FACING, EnumFacing.NORTH), 3);
                TileEntity te = w.getTileEntity(this.shelfPos);
                if (te instanceof TileEntityShelf) {
                    for (int i = 0; i < items.length; i++) {
                        ((TileEntityShelf) te).setInventorySlotContents(i, items[i].copy());
                    }
                    te.markDirty();
                }
                return null;
            });
            look(feet, 0.0F, 15.0F);
            return 30;
        });
        this.steps.add(() -> {
            TileEntity te = this.mc.world.getTileEntity(this.shelfPos);
            ok(te instanceof TileEntityShelf, "shelf: the client has its tile entity: " + te);
            if (te instanceof TileEntityShelf) {
                boolean same = true;
                for (int i = 0; i < items.length; i++) {
                    same &= ((TileEntityShelf) te).items().get(i).getItem() == items[i].getItem();
                }
                ok(same, "shelf: the client shows its three items: " + ((TileEntityShelf) te).items());
                Object r = TileEntityRendererDispatcher.instance.getRenderer(te);
                ok(r instanceof RenderShelf, "shelf: drawn by its renderer: " + r);
            }
            shot("shelf.png");
            return 1;
        });
    }

    // ---- screens -----------------------------------------------------------------------------------------------

    private BlockPos crafterPos;
    private GuiScreen settings;

    /** Draws a bundle's tooltip over the open screen, as hovering it does. */
    public static final class BundleTip {
        private final Minecraft mc = Minecraft.getMinecraft();
        ItemStack stack;
        boolean drawn;

        @SubscribeEvent
        public void draw(GuiScreenEvent.DrawScreenEvent.Post event) {
            if (this.stack != null) {
                GuiScreen s = event.getGui();
                List<String> lines = this.stack.getTooltip(this.mc.player, ITooltipFlag.TooltipFlags.NORMAL);
                GuiUtils.drawHoveringText(this.stack, lines, s.width / 2 - 40, s.height / 2 - 30, s.width, s.height, -1,
                        this.mc.fontRenderer);
            }
        }

        @SubscribeEvent
        public void post(RenderTooltipEvent.PostText event) {
            if (this.stack != null && event.getStack() == this.stack) {
                this.drawn = true;
            }
        }
    }

    /** Painting items with their variant: the tooltip, two hung paintings, and pick-block on one. */
    private void paintings() {
        this.steps.add(() -> {
            List<String> kebab = PaintingItems.byName("kebab").stack().getTooltip(this.mc.player, ITooltipFlag.TooltipFlags.NORMAL);
            ok(kebab.size() >= 4 && (TextFormatting.YELLOW + "Kebab med tre pepperoni").equals(kebab.get(1))
                    && (TextFormatting.GRAY + "Kristoffer Zetterstrand").equals(kebab.get(2)) && "1x1".equals(kebab.get(3)),
                    "painting: Kebab's tooltip gives title, author and size: " + kebab);
            List<String> random = new ItemStack(Items.PAINTING).getTooltip(this.mc.player, ITooltipFlag.TooltipFlags.NORMAL);
            ok(random.size() >= 2 && (TextFormatting.GRAY + "Random variant").equals(random.get(1)),
                    "painting: the random one says so in creative: " + random);
            BlockPos feet = this.base.add(40, 0, 0);
            BlockPos wall = feet.add(-5, 0, 3);
            onServer(() -> {
                WorldServer w = serverWorld();
                for (int dx = 0; dx < 10; dx++) {
                    for (int dy = 0; dy < 4; dy++) {
                        w.setBlockState(wall.add(dx, dy, 0), Blocks.STONE.getDefaultState(), 2);
                    }
                }
                // Facing north, toward the player: Skull and Roses straight ahead, Baroque to its side.
                w.spawnEntity(PaintingItems.byName("skull_and_roses").create(w, feet.add(0, 1, 2), EnumFacing.NORTH));
                w.spawnEntity(PaintingItems.byName("baroque").create(w, feet.add(-3, 1, 2), EnumFacing.NORTH));
                w.spawnEntity(PaintingItems.byName("kebab").create(w, feet.add(3, 1, 2), EnumFacing.NORTH));
                return null;
            });
            look(feet, 0.0F, 0.0F);
            return 40;
        });
        this.steps.add(() -> {
            shot("paintings.png");
            RayTraceResult hit = this.mc.objectMouseOver;
            ok(hit != null && hit.entityHit instanceof EntityPainting, "painting: the crosshair is on Skull and Roses: " + hit);
            if (hit != null && hit.entityHit != null) {
                this.mc.player.inventory.currentItem = 0;
                this.mc.player.inventory.setInventorySlotContents(0, ItemStack.EMPTY);
                net.minecraftforge.common.ForgeHooks.onPickBlock(hit, this.mc.player, this.mc.world);
                PaintingItems.Entry picked = PaintingItems.of(this.mc.player.getHeldItemMainhand());
                ok(picked != null && "skull_and_roses".equals(picked.name), "painting: pick-block gives Skull and Roses: "
                        + this.mc.player.getHeldItemMainhand() + " " + this.mc.player.getHeldItemMainhand().getTagCompound());
            }
            return 1;
        });
    }

    private void screens() {
        BundleTip tip = new BundleTip();
        this.steps.add(() -> {
            BlockPos feet = this.base.add(25, 0, 0);
            this.crafterPos = feet.add(0, 0, 2);
            onServer(() -> {
                WorldServer w = serverWorld();
                w.setBlockState(this.crafterPos, CrafterContent.crafter.getDefaultState(), 3);
                return null;
            });
            look(feet, 0.0F, 20.0F);
            return 20;
        });
        this.steps.add(() -> {
            onServer(() -> {
                Guis.open(CrafterContent.GUI, serverPlayer(), serverWorld(), this.crafterPos);
                return null;
            });
            return 15;
        });
        this.steps.add(() -> {
            ok(this.mc.currentScreen instanceof GuiCrafter, "crafter: its screen opens: " + this.mc.currentScreen);
            shot("crafter.png");
            onServer(() -> {
                serverPlayer().closeScreen();
                return null;
            });
            return 10;
        });
        // The bundle: its tooltip lines and its drawn tooltip.
        this.steps.add(() -> {
            ItemStack bundle = new ItemStack(BundleContent.BUNDLES.get("bundle"));
            BundleContents.Mutable m = new BundleContents.Mutable(bundle);
            m.tryInsert(new ItemStack(Items.DIAMOND, 3));
            m.tryInsert(new ItemStack(Items.APPLE, 5));
            m.tryInsert(new ItemStack(Items.ARROW, 16));
            m.save(bundle);
            List<String> lines = bundle.getTooltip(this.mc.player, ITooltipFlag.TooltipFlags.NORMAL);
            ok(BundleTooltipLines.firstPlaceholder(lines) >= 0, "bundle: its tooltip makes room for the contents: " + lines);
            ok(BundleContents.items(bundle).size() == 3, "bundle: holds three stacks");
            tip.stack = bundle;
            MinecraftForge.EVENT_BUS.register(tip);
            this.mc.displayGuiScreen(new GuiInventory(this.mc.player));
            return 10;
        });
        this.steps.add(() -> {
            ok(tip.drawn, "bundle: its tooltip is drawn over a screen");
            shot("bundle-tooltip.png");
            MinecraftForge.EVENT_BUS.unregister(tip);
            tip.stack = null;
            this.mc.displayGuiScreen(null);
            return 5;
        });
        // The Lunge book.
        this.steps.add(() -> {
            ItemStack book = ItemEnchantedBook.getEnchantedItemStack(new EnchantmentData(SpearContent.lunge, 2));
            List<String> lines = book.getTooltip(this.mc.player, ITooltipFlag.TooltipFlags.NORMAL);
            boolean named = false;
            for (String l : lines) {
                named |= "Lunge II".equals(TextFormatting.getTextWithoutFormattingCodes(l));
            }
            ok(named, "Lunge book: its tooltip names Lunge II: " + lines);
            return 1;
        });
        // This mod's settings screen.
        this.steps.add(() -> {
            IModGuiFactory factory = FMLClientHandler.instance().getGuiFactoryFor(
                    Loader.instance().getIndexedModList().get(TrickyTrialsBackport.MOD_ID));
            ok(factory != null && factory.hasConfigGui(), "settings: the mod has a settings screen");
            this.settings = factory == null ? null : factory.createConfigGui(null);
            ok(this.settings != null, "settings: it opens: " + this.settings);
            if (this.settings != null) {
                this.mc.displayGuiScreen(this.settings);
            }
            return 20;
        });
        this.steps.add(() -> {
            if (this.settings != null) {
                ok(this.mc.currentScreen == this.settings, "settings: it stays open and draws: " + this.mc.currentScreen);
                shot("settings.png");
            }
            this.mc.displayGuiScreen(null);
            return 5;
        });
    }
}
