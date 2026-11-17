package com.exiledradio.trickytrialsbackport.tweaks;

import com.exiledradio.trickytrialsbackport.ModConfig;
import net.minecraft.block.Block;
import net.minecraft.block.BlockDoor;
import net.minecraft.block.state.IBlockState;
import net.minecraft.enchantment.Enchantment;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.IProjectile;
import net.minecraft.entity.item.EntityFireworkRocket;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.projectile.EntityFireball;
import net.minecraft.entity.projectile.EntityFishHook;
import net.minecraft.entity.projectile.EntityPotion;
import net.minecraft.entity.projectile.EntityShulkerBullet;
import net.minecraft.init.Blocks;
import net.minecraft.init.Enchantments;
import net.minecraft.inventory.EntityEquipmentSlot;
import net.minecraft.item.ItemStack;
import net.minecraft.util.DamageSource;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import net.minecraft.world.storage.MapData;

import java.util.Map;
import java.util.Random;
import java.util.WeakHashMap;

/**
 * The calls the coremod's mechanics patches (P5-P14, docs/PATCHES.md) make. Each returns 1.12.2's value while its
 * setting (or the stream) is off.
 */
public final class MechanicsHooks {

    private MechanicsHooks() {
    }

    // P6 ----------------------------------------------------------------------------------------

    /**
     * P6, in {@code EntityLivingBase.attackEntityFrom} just before 1.12.2's {@code if (attacker != null) knockBack(...)}:
     * 1.21's {@code LivingEntity.hurtServer} knocks back from a projectile's own direction of travel (potions and
     * fireworks: away from themselves), whoever shot it, and also when nothing did (a dispenser).
     *
     * @return true when this knocked back, so 1.12.2's shooter-based knockback is skipped
     */
    public static boolean projectileKnockback(EntityLivingBase target, DamageSource source, Entity attacker) {
        Entity direct = source.getImmediateSource();
        if (direct == null || !isProjectile(direct) || !Tweaks.on(ModConfig.tweaks.projectileKnockback)) {
            return false;
        }
        double x;
        double z;
        if (direct instanceof EntityPotion || direct instanceof EntityFireworkRocket) {
            x = direct.posX - target.posX;
            z = direct.posZ - target.posZ;
        } else {
            x = -direct.motionX;
            z = -direct.motionZ;
        }
        while (x * x + z * z < 1.0E-4D) {
            x = (Math.random() - Math.random()) * 0.01D;
            z = (Math.random() - Math.random()) * 0.01D;
        }
        target.attackedAtYaw = (float) (MathHelper.atan2(z, x) * (180D / Math.PI) - (double) target.rotationYaw);
        target.knockBack(attacker != null ? attacker : direct, 0.4F, x, z);
        return true;
    }

    /** 1.21's {@code Projectile}s that exist in 1.12.2. */
    static boolean isProjectile(Entity e) {
        return e instanceof IProjectile || e instanceof EntityFireball || e instanceof EntityShulkerBullet
                || e instanceof EntityFishHook || e instanceof EntityFireworkRocket;
    }

    // P8 ----------------------------------------------------------------------------------------

    /**
     * P8, {@code ItemDoor.placeDoor}'s {@code getBlock() == door} tests for a neighboring door: 1.21's
     * {@code DoorBlock.getHinge} counts a door of any material, so a new door pairs with it.
     */
    public static Block doorMatch(Block found, Block door) {
        return found instanceof BlockDoor && Tweaks.on(ModConfig.tweaks.mixedDoubleDoors) ? door : found;
    }

    // P9 ----------------------------------------------------------------------------------------

    /**
     * P9, the head of {@code BlockFire.updateTick}: 1.21.5's fire neither burns nor spreads unless a player is within
     * 128 blocks ({@code ServerLevel.canSpreadFireAround}); it only schedules its next tick.
     *
     * @return true to skip this tick
     */
    public static boolean fireIdle(Block fire, World world, BlockPos pos, Random rand) {
        if (world.isRemote || !Tweaks.on(ModConfig.tweaks.fireNearPlayers) || GameRuleTweaks.fireAwayFromPlayers(world)) {
            return false;
        }
        double x = pos.getX() + 0.5D, y = pos.getY() + 0.5D, z = pos.getZ() + 0.5D;
        for (EntityPlayer p : world.playerEntities) {
            if (!p.isSpectator() && p.getDistanceSq(x, y, z) < 128.0D * 128.0D) {
                return false;
            }
        }
        world.scheduleUpdate(pos, fire, fire.tickRate(world) + rand.nextInt(10));
        return true;
    }

    // P10 ---------------------------------------------------------------------------------------

    /** P10: whether {@code EnchantmentProtection}'s two helpers use the additive rules below. */
    public static boolean protectionStacks() {
        return Tweaks.on(ModConfig.tweaks.protectionStacks);
    }

    /** The sum of an enchantment's levels over the armor worn (1.21 adds an attribute modifier per piece). */
    static int armorLevels(Enchantment ench, EntityLivingBase living) {
        int sum = 0;
        for (EntityEquipmentSlot slot : EntityEquipmentSlot.values()) {
            if (slot.getSlotType() == EntityEquipmentSlot.Type.ARMOR) {
                sum += EnchantmentHelper.getEnchantmentLevel(ench, living.getItemStackFromSlot(slot));
            }
        }
        return sum;
    }

    /** 1.21's {@code burning_time}: 15% less per level of Fire Protection, summed over every piece. */
    public static int fireTime(EntityLivingBase living, int ticks) {
        double factor = Math.max(0.0D, 1.0D - 0.15D * armorLevels(Enchantments.FIRE_PROTECTION, living));
        return MathHelper.floor(ticks * factor);
    }

    /** 1.21's {@code explosion_knockback_resistance}: 15% per level of Blast Protection, summed, at most all of it. */
    public static double blastKnockback(EntityLivingBase living, double knockback) {
        double resist = Math.min(1.0D, 0.15D * armorLevels(Enchantments.BLAST_PROTECTION, living));
        return knockback * (1.0D - resist);
    }

    // P11 ---------------------------------------------------------------------------------------

    /** P11, the head of {@code EntityEnderCrystal.attackEntityFrom}: 1.21's crystal is fire immune. */
    public static boolean crystalIgnores(DamageSource source) {
        return source.isFireDamage() && Tweaks.on(ModConfig.tweaks.crystalsFireImmune);
    }

    // P12 ---------------------------------------------------------------------------------------

    private static final Map<EntityPotion, Vec3d> SPLASH_AT = new WeakHashMap<EntityPotion, Vec3d>();

    /** Where a splash potion hit, recorded from its impact event, which comes just before {@code applySplash}. */
    static void splashedAt(EntityPotion potion, Vec3d at) {
        SPLASH_AT.put(potion, at);
    }

    /**
     * P12, {@code EntityPotion.applySplash}'s {@code getDistanceSq(entity)}: 1.21.2's {@code ThrownSplashPotion} measures
     * from its box, moved to the hit, to the mob's box grown by the projectile margin; 1.12.2 from its center to the
     * mob's feet. Both keep 1.12.2's "under 16, strength 1 - distance / 4".
     */
    public static double splashDistanceSq(EntityPotion potion, Entity entity) {
        if (!Tweaks.on(ModConfig.tweaks.splashDistance)) {
            return potion.getDistanceSq(entity);
        }
        Vec3d at = SPLASH_AT.get(potion);
        AxisAlignedBB box = potion.getEntityBoundingBox();
        if (at != null) {
            box = box.offset(at.x - potion.posX, at.y - potion.posY, at.z - potion.posZ);
        }
        double margin = Math.max(0.0D, Math.min(0.3D, (potion.ticksExisted - 2) / 20.0D));
        return boxDistanceSq(box, entity.getEntityBoundingBox().grow(margin));
    }

    static double boxDistanceSq(AxisAlignedBB a, AxisAlignedBB b) {
        double dx = Math.max(0.0D, Math.max(a.minX - b.maxX, b.minX - a.maxX));
        double dy = Math.max(0.0D, Math.max(a.minY - b.maxY, b.minY - a.maxY));
        double dz = Math.max(0.0D, Math.max(a.minZ - b.maxZ, b.minZ - a.maxZ));
        return dx * dx + dy * dy + dz * dz;
    }

    // P13 ---------------------------------------------------------------------------------------

    /**
     * P13, at the end of {@code MapData.updateVisiblePlayers}: 1.21.2's {@code MapItemSavedData.tickCarriedBy} removes
     * the marker of every other player wearing a carved pumpkin (1.12.2's pumpkin, the same block).
     */
    public static void hideMapMarkers(MapData data, EntityPlayer holder) {
        if (!Tweaks.on(ModConfig.tweaks.pumpkinHidesMapMarker)) {
            return;
        }
        for (MapData.MapInfo info : data.playersArrayList) {
            if (info.player != holder && wearsPumpkin(info.player)) {
                data.mapDecorations.remove(info.player.getName());
            }
        }
    }

    static boolean wearsPumpkin(EntityPlayer player) {
        ItemStack head = player.getItemStackFromSlot(EntityEquipmentSlot.HEAD);
        if (head.isEmpty()) {
            return false;
        }
        if (head.getItem() == net.minecraft.item.Item.getItemFromBlock(Blocks.PUMPKIN)) {
            return true;
        }
        ResourceLocation id = head.getItem().getRegistryName();
        return id != null && "carved_pumpkin".equals(id.getPath());
    }

    // P14 ---------------------------------------------------------------------------------------

    /**
     * P14, {@code Teleporter.placeInPortal}'s {@code setBlockState} calls building the End's obsidian platform: 1.21's
     * {@code EndPlatformFeature} breaks what was there as if mined, dropping it, before placing obsidian or air.
     */
    public static boolean platformBlock(World world, BlockPos pos, IBlockState state) {
        if (Tweaks.on(ModConfig.tweaks.portalsDropBlocks)) {
            IBlockState there = world.getBlockState(pos);
            if (there.getBlock() != state.getBlock() && !there.getBlock().isAir(there, world, pos)) {
                world.destroyBlock(pos, true);
            }
        }
        return world.setBlockState(pos, state);
    }
}
