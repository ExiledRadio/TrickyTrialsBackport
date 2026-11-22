package com.exiledradio.trickytrialsbackport.decor;

import com.exiledradio.trickytrialsbackport.TrickyTrialsBackport;
import com.exiledradio.trickytrialsbackport.common.Owners;

import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityHanging;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.entity.item.EntityPainting;
import net.minecraft.init.Items;
import net.minecraft.item.ItemStack;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraftforge.event.entity.EntityJoinWorldEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.EventPriority;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

import javax.annotation.Nullable;

/**
 * Named paintings.
 *
 * <ul>
 *   <li><b>Placing</b> (1.20.5, so always here): a renamed painting item hangs a painting with that custom name
 *       ({@code HangingEntityItem.useOn} applies {@code EntityType.createDefaultStackConfig}, which copies
 *       {@code custom_name}). 1.12.2's {@code ItemHangingEntity} drops the name; {@link DecorEvents} puts it on.</li>
 *   <li><b>Breaking</b> (26.3): the painting drops a painting item with the painting's custom name
 *       ({@code Painting.dropItem} sets {@code custom_name}; 1.21's dropped a plain one).</li>
 *   <li><b>Showing</b> (26.3, "Paintings with custom names now also show those names when the player looks at
 *       them"): 26.3's {@code PaintingRenderer.submit} now ends in {@code EntityRenderer.submit}, whose
 *       {@code shouldShowName} is a custom name and the entity under the crosshair (or a name always shown); the
 *       nameplate is the generic one, within 64 blocks ({@code client.PaintingNameplate}).</li>
 * </ul>
 * The 26.3 parts come with 26.3, that is, with Game Drops Backport installed.
 */
@Mod.EventBusSubscriber(modid = TrickyTrialsBackport.MOD_ID)
public final class PaintingNames {

    /** For the self-tests: 26.3's parts without Game Drops Backport installed. */
    public static volatile boolean forceSince263;

    private PaintingNames() {
    }

    /** @return true when 26.3's named paintings apply: Game Drops Backport (26.x) is installed */
    public static boolean since263() {
        return DecorContent.enabled() && (forceSince263 || Owners.present(Owners.GAME_DROPS));
    }

    /** Placing: the item's custom name goes on the painting it hangs (1.20.5's createDefaultStackConfig). */
    public static void name(@Nullable Entity painting, ItemStack stack) {
        if (painting != null && stack.hasDisplayName()) {
            painting.setCustomNameTag(stack.getDisplayName());
        }
    }

    /** @return the item a painting breaks into: a painting, with the painting's custom name from 26.3 */
    public static ItemStack drop(EntityHanging painting) {
        ItemStack stack = new ItemStack(Items.PAINTING);
        if (since263() && painting.hasCustomName()) {
            stack.setStackDisplayName(painting.getCustomNameTag());
        }
        return stack;
    }

    /**
     * 1.12.2's {@code EntityPainting.onBroken} drops {@code new ItemStack(Items.PAINTING)} right after the painting is
     * set dead, through {@code EntityHanging.entityDropItem}: 0.15 blocks out from the wall, at the painting's height.
     * That item joining the world, exactly there, gets the dead painting's name.
     */
    @SubscribeEvent(priority = EventPriority.HIGH)
    public static void itemJoin(EntityJoinWorldEvent event) {
        if (event.getWorld().isRemote || !(event.getEntity() instanceof EntityItem) || !since263()) {
            return;
        }
        EntityItem item = (EntityItem) event.getEntity();
        ItemStack stack = item.getItem();
        if (stack.getItem() != Items.PAINTING || stack.getCount() != 1 || stack.hasTagCompound()) {
            return;
        }
        AxisAlignedBB at = new AxisAlignedBB(item.posX, item.posY, item.posZ, item.posX, item.posY, item.posZ).grow(0.5D);
        for (EntityPainting painting : event.getWorld().getEntitiesWithinAABB(EntityPainting.class, at)) {
            if (painting.isDead && painting.hasCustomName() && painting.facingDirection != null && droppedBy(painting, item)) {
                ItemStack named = stack.copy();
                named.setStackDisplayName(painting.getCustomNameTag());
                item.setItem(named);
                return;
            }
        }
    }

    /** @return true when the item stands where {@code EntityHanging.entityDropItem(stack, 0)} puts the painting's drop */
    private static boolean droppedBy(EntityHanging painting, EntityItem item) {
        return item.posX == painting.posX + (double) ((float) painting.facingDirection.getXOffset() * 0.15F)
                && item.posY == painting.posY
                && item.posZ == painting.posZ + (double) ((float) painting.facingDirection.getZOffset() * 0.15F);
    }
}
