package com.exiledradio.trickytrialsbackport.tweaks;

import com.exiledradio.trickytrialsbackport.ModConfig;
import com.exiledradio.trickytrialsbackport.TrickyTrialsBackport;
import com.exiledradio.trickytrialsbackport.common.Content;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityAgeable;
import net.minecraft.entity.EntityLiving;
import net.minecraft.entity.monster.IMob;
import net.minecraft.entity.passive.EntityAmbientCreature;
import net.minecraft.entity.passive.EntityWaterMob;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.util.EnumActionResult;
import net.minecraft.util.EnumHand;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.Vec3d;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.EventPriority;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

import java.util.ArrayList;
import java.util.List;

/**
 * 1.21.6's leads: sneak-using a mob ties the mobs you lead to it; shears cut every lead on a mob (its own and those it
 * holds); a lead snaps at 12 blocks between the two mobs' centers instead of 10 (patch P7 for the creature's own check).
 */
@Mod.EventBusSubscriber(modid = TrickyTrialsBackport.MOD_ID)
public final class Leads {

    /** 1.21.6's {@code Leashable.LEASH_TOO_FAR_DIST}. */
    public static final float SNAP = 12.0F;

    private Leads() {
    }

    /** {@code EntityCreature.updateLeashedState}'s two 10.0F (patch P7). */
    public static float snapDistance() {
        return Tweaks.on(ModConfig.tweaks.leads) ? SNAP : 10.0F;
    }

    @SubscribeEvent(priority = EventPriority.HIGH)
    public static void onInteract(PlayerInteractEvent.EntityInteract event) {
        EntityPlayer player = event.getEntityPlayer();
        if (!player.isSneaking() || !(event.getTarget() instanceof EntityLiving) || !Tweaks.on(ModConfig.tweaks.leads)
                || event.getHand() != EnumHand.MAIN_HAND) {
            return;
        }
        EntityLiving holder = (EntityLiving) event.getTarget();
        if (!canHold(holder)) {
            return;
        }
        boolean tied = false;
        for (EntityLiving led : ledBy(holder, player)) {
            if (led != holder && distance(led, holder) <= SNAP) {
                if (!holder.world.isRemote) {
                    led.setLeashHolder(holder, true);
                }
                tied = true;
            }
        }
        if (tied) {
            if (!holder.world.isRemote) {
                holder.playSound(Content.sound("item.lead.tied"), 1.0F, 1.0F);
            }
            event.setCanceled(true);
            event.setCancellationResult(EnumActionResult.SUCCESS);
        }
    }

    /** 1.21.6's {@code Leashable.canBeLeashed} for 1.12.2's mobs: no monsters, water or ambient mobs, no babies. */
    static boolean canHold(EntityLiving mob) {
        return mob.isEntityAlive() && !(mob instanceof IMob) && !(mob instanceof EntityWaterMob)
                && !(mob instanceof EntityAmbientCreature) && !(mob instanceof EntityAgeable && ((EntityAgeable) mob).isChild());
    }

    /** The leashed mobs within 1.21.6's 32-block box whose holder is {@code holder}. */
    static List<EntityLiving> ledBy(Entity around, Entity holder) {
        Vec3d c = center(around);
        AxisAlignedBB box = new AxisAlignedBB(c.x - 16, c.y - 16, c.z - 16, c.x + 16, c.y + 16, c.z + 16);
        List<EntityLiving> out = new ArrayList<EntityLiving>();
        for (EntityLiving mob : around.world.getEntitiesWithinAABB(EntityLiving.class, box)) {
            if (mob.getLeashed() && mob.getLeashHolder() == holder) {
                out.add(mob);
            }
        }
        return out;
    }

    /** 1.21.6 measures leads between the centers of the two boxes. */
    static double distance(Entity a, Entity b) {
        return center(a).distanceTo(center(b));
    }

    private static Vec3d center(Entity e) {
        AxisAlignedBB b = e.getEntityBoundingBox();
        return new Vec3d((b.minX + b.maxX) / 2, (b.minY + b.maxY) / 2, (b.minZ + b.maxZ) / 2);
    }

    /** Shears cut a mob's own lead and the leads of every mob it holds (1.21.6's {@code shearOffAllLeashConnections}). */
    static boolean snip(EntityLiving mob, EntityPlayer player, ItemStack shears, EnumHand hand) {
        List<EntityLiving> held = ledBy(mob, mob);
        boolean any = mob.getLeashed() || !held.isEmpty();
        if (!any) {
            return false;
        }
        if (!mob.world.isRemote) {
            if (mob.getLeashed()) {
                mob.clearLeashed(true, true);
            }
            for (EntityLiving other : held) {
                other.clearLeashed(true, true);
            }
            mob.world.playSound(null, mob.posX, mob.posY, mob.posZ, Content.sound("item.shears.snip"), player.getSoundCategory(), 1.0F, 1.0F);
            shears.damageItem(1, player);
        }
        return true;
    }
}
