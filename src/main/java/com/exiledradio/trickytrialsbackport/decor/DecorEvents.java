package com.exiledradio.trickytrialsbackport.decor;

import com.exiledradio.trickytrialsbackport.TrickyTrialsBackport;

import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityList;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.entity.item.EntityPainting;
import net.minecraft.entity.monster.EntityGhast;
import net.minecraft.entity.passive.EntityChicken;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.projectile.EntityLargeFireball;
import net.minecraft.init.Items;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.DamageSource;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraftforge.event.entity.EntityJoinWorldEvent;
import net.minecraftforge.event.entity.living.LivingDropsEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.ObfuscationReflectionHelper;
import net.minecraftforge.fml.common.eventhandler.EventPriority;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * Placing paintings with 1.21's choice, and the Tears and Lava Chicken drops.
 *
 * <h2>Paintings</h2>
 * 1.12.2's painting item already picks as 1.21 does: every painting that fits the wall, then the largest of those,
 * then one at random. 1.21's list is 1.12.2's 26 and our 21, so when the item places a vanilla painting (a right
 * click on a wall made it, this tick) the choice is made again over all 47 and, when one of ours wins, ours hangs
 * instead. Nothing else that spawns a painting (a summon, a loaded chunk) is touched.
 *
 * <h2>Discs</h2>
 * 1.21.6's ghast table: Tears when the killing blow is a ghast fireball (a projectile) and a player hit it last, which
 * in 1.12.2 is a fireball the player sent back. 1.21.7's zombie table: Lava Chicken from a baby zombie (zombies only,
 * not husks or zombie villagers) riding a chicken, killed by a player.
 */
@Mod.EventBusSubscriber(modid = TrickyTrialsBackport.MOD_ID)
public final class DecorEvents {

    private static final ResourceLocation ZOMBIE = new ResourceLocation("zombie");

    /** Where the painting item is being used this tick on the server: the block the painting will hang from. */
    @Nullable
    private static BlockPos placing;
    @Nullable
    private static World placingWorld;

    private DecorEvents() {
    }

    /**
     * A painting that carries its variant ({@link PaintingItems}) hangs that picture, as 1.19.4's
     * {@code HangingEntityItem.useOn} does: on a side face the player may edit, and only where it fits (otherwise the
     * item stays in hand). Runs on both sides, like the item's own use, which it replaces.
     */
    @SubscribeEvent
    public static void placeVariant(PlayerInteractEvent.RightClickBlock event) {
        if (!DecorContent.enabled() || event.isCanceled()) {
            return;
        }
        ItemStack stack = event.getItemStack();
        PaintingItems.Entry entry = PaintingItems.of(stack);
        if (entry == null) {
            return;
        }
        event.setCanceled(true);
        EnumFacing face = event.getFace();
        EntityPlayer player = event.getEntityPlayer();
        BlockPos at = face == null ? null : event.getPos().offset(face);
        if (at == null || !face.getAxis().isHorizontal() || !player.canPlayerEdit(at, face, stack)) {
            event.setCancellationResult(net.minecraft.util.EnumActionResult.FAIL);
            return;
        }
        event.setCancellationResult(net.minecraft.util.EnumActionResult.SUCCESS);
        World world = event.getWorld();
        net.minecraft.entity.EntityHanging painting = entry.create(world, at, face);
        if (painting.onValidSurface()) {
            if (!world.isRemote) {
                painting.playPlaceSound();
                world.spawnEntity(painting);
            }
            // The item's own use is skipped, and with it the creative count restore around it.
            if (!player.capabilities.isCreativeMode) {
                stack.shrink(1);
            }
        }
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void rightClickBlock(PlayerInteractEvent.RightClickBlock event) {
        if (!DecorContent.enabled() || event.getWorld().isRemote || event.isCanceled()
                || event.getItemStack().getItem() != Items.PAINTING || event.getFace() == null
                || !event.getFace().getAxis().isHorizontal()) {
            return;
        }
        placing = event.getPos().offset(event.getFace());
        placingWorld = event.getWorld();
    }

    @SubscribeEvent
    public static void serverTick(TickEvent.ServerTickEvent event) {
        if (event.phase == TickEvent.Phase.END) {
            placing = null;
            placingWorld = null;
        }
    }

    @SubscribeEvent(priority = EventPriority.HIGH)
    public static void placePainting(EntityJoinWorldEvent event) {
        Entity entity = event.getEntity();
        if (placing == null || event.getWorld() != placingWorld || entity.getClass() != EntityPainting.class
                || !placing.equals(((EntityPainting) entity).getHangingPosition())) {
            return;
        }
        placing = null;
        placingWorld = null;
        EntityPainting painting = (EntityPainting) entity;
        if (painting.facingDirection == null) {
            return;
        }
        Entity chosen = choose(event.getWorld(), painting.getHangingPosition(), painting.facingDirection, painting, event.getWorld().rand);
        if (chosen instanceof EntityModernPainting) {
            event.setCanceled(true);
            event.getWorld().spawnEntity(chosen);
        }
    }

    /**
     * 1.21's {@code Painting.create} over 1.12.2's paintings and ours: the largest that fit, one at random.
     *
     * @param vanilla a vanilla painting at this spot, set to the chosen art when a vanilla one wins
     * @return {@code vanilla}, a new painting of ours, or null when nothing fits
     */
    @Nullable
    public static Entity choose(World world, BlockPos pos, EnumFacing facing, EntityPainting vanilla, Random random) {
        EntityPainting.EnumArt kept = vanilla.art;
        List<Object> fits = new ArrayList<Object>();
        int best = 0;
        for (EntityPainting.EnumArt art : EntityPainting.EnumArt.values()) {
            vanilla.art = art;
            setFacing(vanilla, facing);
            if (vanilla.onValidSurface()) {
                int area = art.sizeX * art.sizeY / 256;
                if (area > best) {
                    best = area;
                    fits.clear();
                }
                if (area == best) {
                    fits.add(art);
                }
            }
        }
        for (Paintings.Variant variant : Paintings.all()) {
            EntityModernPainting ours = new EntityModernPainting(world, pos, facing, variant);
            if (ours.onValidSurface()) {
                int area = variant.area();
                if (area > best) {
                    best = area;
                    fits.clear();
                }
                if (area == best) {
                    fits.add(ours);
                }
            }
        }
        if (fits.isEmpty()) {
            vanilla.art = kept;
            setFacing(vanilla, facing);
            return null;
        }
        Object pick = fits.get(random.nextInt(fits.size()));
        if (pick instanceof EntityModernPainting) {
            vanilla.art = kept;
            setFacing(vanilla, facing);
            return (Entity) pick;
        }
        vanilla.art = (EntityPainting.EnumArt) pick;
        setFacing(vanilla, facing);
        return vanilla;
    }

    @Nullable
    private static java.lang.reflect.Method updateFacing;

    /** {@code updateFacingWithBoundingBox} is protected; it sizes the box from the current art. */
    static void setFacing(EntityPainting painting, EnumFacing facing) {
        try {
            if (updateFacing == null) {
                updateFacing = ObfuscationReflectionHelper.findMethod(net.minecraft.entity.EntityHanging.class, "func_174859_a",
                        void.class, EnumFacing.class);
            }
            updateFacing.invoke(painting, facing);
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException(e);
        }
    }

    @SubscribeEvent
    public static void drops(LivingDropsEvent event) {
        if (!DecorContent.enabled()) {
            return;
        }
        EntityLivingBase dead = event.getEntityLiving();
        if (dead.world.isRemote || !killedByPlayer(dead)) {
            return;
        }
        DamageSource source = event.getSource();
        Item disc = null;
        if (dead instanceof EntityGhast && source.isProjectile() && source.getImmediateSource() instanceof EntityLargeFireball) {
            disc = DecorContent.DISC_ITEMS.get("tears");
        } else if (ZOMBIE.equals(EntityList.getKey(dead)) && dead.isChild() && dead.getRidingEntity() instanceof EntityChicken) {
            disc = DecorContent.DISC_ITEMS.get("lava_chicken");
        }
        if (disc != null) {
            EntityItem drop = new EntityItem(dead.world, dead.posX, dead.posY, dead.posZ, new ItemStack(disc));
            drop.setDefaultPickupDelay();
            event.getDrops().add(drop);
        }
    }

    /** 1.21's {@code killed_by_player}: a player hurt it within the last 100 ticks. */
    static boolean killedByPlayer(EntityLivingBase entity) {
        EntityPlayer player = ObfuscationReflectionHelper.getPrivateValue(EntityLivingBase.class, entity, "field_70717_bb");
        return player != null;
    }
}
