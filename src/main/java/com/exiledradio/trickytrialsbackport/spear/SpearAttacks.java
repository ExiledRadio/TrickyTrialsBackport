package com.exiledradio.trickytrialsbackport.spear;

import com.exiledradio.trickytrialsbackport.common.Content;
import com.exiledradio.trickytrialsbackport.network.NetworkHandler;
import com.google.common.base.Predicate;

import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.EnumCreatureAttribute;
import net.minecraft.entity.MultiPartEntityPart;
import net.minecraft.entity.SharedMonsterAttributes;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.item.ItemStack;
import net.minecraft.stats.StatList;
import net.minecraft.util.DamageSource;
import net.minecraft.util.EntityDamageSource;
import net.minecraft.util.EnumHand;
import net.minecraft.util.EnumParticleTypes;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import net.minecraft.world.WorldServer;
import net.minecraftforge.common.ForgeHooks;
import net.minecraftforge.event.ForgeEventFactory;
import net.minecraftforge.fml.common.network.NetworkRegistry;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;

/**
 * The spear's two attacks, as 1.21.11's {@code PiercingWeapon} (the jab) and {@code KineticWeapon} (the charge) do
 * them, and Lunge. Everything here is public so that other code (stream 24's mounted mobs) can use spears the same
 * way: {@link #jab}, {@link #chargeTick}, {@link #hitsAlong}, {@link #motion}, {@link #lunge}.
 *
 * <h2>Speed</h2>
 * 1.21.11 measures how fast an entity moved over its last tick ({@code getKnownSpeed}, blocks a second). 1.12.2 resets
 * its own previous positions before a mob's item-use tick runs, so the move is sampled here at the start of each
 * living entity's update ({@link SpearEvents}) and kept per entity; anything not sampled (a boat, a minecart) uses
 * its position against last tick's.
 */
public final class SpearAttacks {

    /** 1.21.11's {@code damage_type/spear}: "was speared by". */
    public static final String DAMAGE_TYPE = "spear";

    /** entity -> {x, y, z at the last sample, dx, dy, dz over the tick before it} */
    private static final Map<Entity, double[]> MOTION = new WeakHashMap<Entity, double[]>();
    /** wielder -> targets hit in the current charge, with the world time of their last hit */
    private static final Map<EntityLivingBase, Map<Entity, Long>> STABBED = new WeakHashMap<EntityLivingBase, Map<Entity, Long>>();
    /** wielder -> world time of the last charge-hit feedback (1.21.11's {@code lastKineticHitFeedbackTime}) */
    private static final Map<EntityLivingBase, Long> FEEDBACK = new WeakHashMap<EntityLivingBase, Long>();
    /** Vehicles whose charge is faster or slower (1.21.11's {@code Mob.chargeSpeedModifier}), by class. */
    private static final Map<Class<?>, Float> CHARGE_SPEED = new HashMap<Class<?>, Float>();

    private SpearAttacks() {
    }

    // ------------------------------------------------------------------------------------------
    // Speed
    // ------------------------------------------------------------------------------------------

    /** Records an entity's position for this tick (called at the start of every living update). */
    public static void sample(Entity e) {
        synchronized (MOTION) {
            double[] m = MOTION.get(e);
            if (m == null) {
                MOTION.put(e, new double[] {e.posX, e.posY, e.posZ, 0, 0, 0});
                return;
            }
            m[3] = e.posX - m[0];
            m[4] = e.posY - m[1];
            m[5] = e.posZ - m[2];
            m[0] = e.posX;
            m[1] = e.posY;
            m[2] = e.posZ;
        }
    }

    /** Sets an entity's last move outright (blocks per tick), as if it had just moved so. */
    public static void setMotion(Entity e, double dx, double dy, double dz) {
        synchronized (MOTION) {
            MOTION.put(e, new double[] {e.posX, e.posY, e.posZ, dx, dy, dz});
        }
    }

    /** @return the entity's last move in blocks per tick (1.21.11's {@code getKnownMovement}) */
    public static Vec3d movement(Entity e) {
        synchronized (MOTION) {
            double[] m = MOTION.get(e);
            if (m != null) {
                return new Vec3d(m[3], m[4], m[5]);
            }
        }
        return new Vec3d(e.posX - e.lastTickPosX, e.posY - e.lastTickPosY, e.posZ - e.lastTickPosZ);
    }

    /**
     * @return 1.21.11's {@code KineticWeapon.getMotion}: blocks a second, a mob riding something taking its root
     * vehicle's
     */
    public static Vec3d motion(Entity e) {
        Entity of = e;
        if (!(e instanceof EntityPlayer) && e.isRiding()) {
            of = e.getLowestRidingEntity();
        }
        return movement(of).scale(20.0);
    }

    /** Lets a vehicle class charge faster or slower for the mobs riding it (1.21.11's {@code Mob.chargeSpeedModifier}). */
    public static synchronized void setChargeSpeed(Class<? extends Entity> vehicle, float modifier) {
        CHARGE_SPEED.put(vehicle, modifier);
    }

    /** @return the charge speed modifier of the vehicle a mob rides, 1 on foot */
    public static synchronized float chargeSpeed(Entity mob) {
        Entity root = mob.getLowestRidingEntity();
        for (Class<?> c = root.getClass(); root != mob && c != null; c = c.getSuperclass()) {
            Float f = CHARGE_SPEED.get(c);
            if (f != null) {
                return f;
            }
        }
        return 1.0F;
    }

    // ------------------------------------------------------------------------------------------
    // Reach
    // ------------------------------------------------------------------------------------------

    public static float minReach(Entity e) {
        if (e instanceof EntityPlayer) {
            EntityPlayer p = (EntityPlayer) e;
            return p.isSpectator() ? 0.0F : p.isCreative() ? SpearTier.MIN_CREATIVE_REACH : SpearTier.MIN_REACH;
        }
        return SpearTier.MIN_REACH * SpearTier.MOB_REACH_FACTOR;
    }

    public static float maxReach(Entity e) {
        if (e instanceof EntityPlayer) {
            return ((EntityPlayer) e).isCreative() ? SpearTier.MAX_CREATIVE_REACH : SpearTier.MAX_REACH;
        }
        return SpearTier.MAX_REACH * SpearTier.MOB_REACH_FACTOR;
    }

    /** 1.21.11's {@code PiercingWeapon.canHitEntity}. */
    public static boolean canHit(Entity attacker, Entity target) {
        if (target == attacker || target.getIsInvulnerable() || !target.isEntityAlive() || !target.canBeCollidedWith()) {
            return false;
        }
        if (target instanceof EntityPlayer) {
            if (((EntityPlayer) target).isSpectator()) {
                return false;
            }
            if (attacker instanceof EntityPlayer && !((EntityPlayer) attacker).canAttackPlayer((EntityPlayer) target)) {
                return false;
            }
        }
        return !attacker.isRidingSameEntity(target);
    }

    /**
     * 1.21.11's {@code ProjectileUtil.getHitEntitiesAlong}: every entity whose box (grown by the hitbox margin) the
     * look crosses between the minimum and maximum reach - the maximum pushed out by the wielder's move along their
     * look. A block in the way cuts the line short, and a block nearer than the minimum reach stops it all.
     */
    public static List<Entity> hitsAlong(final EntityLivingBase attacker) {
        List<Entity> hits = new ArrayList<Entity>();
        Vec3d look = attacker.getLookVec();
        Vec3d eye = attacker.getPositionEyes(1.0F);
        Vec3d from = eye.add(look.scale(minReach(attacker)));
        double ahead = movement(attacker).dotProduct(look);
        Vec3d to = eye.add(look.scale(maxReach(attacker) + Math.max(0.0, ahead)));
        World world = attacker.world;
        RayTraceResult block = world.rayTraceBlocks(eye, to, false, true, false);
        if (block != null && block.typeOfHit == RayTraceResult.Type.BLOCK) {
            to = block.hitVec;
            if (eye.squareDistanceTo(to) < eye.squareDistanceTo(from)) {
                return hits;
            }
        }
        double m = SpearTier.HITBOX_MARGIN;
        AxisAlignedBB around = new AxisAlignedBB(from.x - m / 2, from.y - m / 2, from.z - m / 2, from.x + m / 2, from.y + m / 2, from.z + m / 2)
                .expand(to.x - from.x, to.y - from.y, to.z - from.z).grow(1.0);
        for (Entity e : world.getEntitiesInAABBexcluding(attacker, around, new Predicate<Entity>() {
            @Override
            public boolean apply(@Nullable Entity e) {
                return e != null && canHit(attacker, e);
            }
        })) {
            AxisAlignedBB box = e.getEntityBoundingBox().grow(m);
            if (box.contains(from) || box.calculateIntercept(from, to) != null) {
                Entity hit = e instanceof MultiPartEntityPart && ((MultiPartEntityPart) e).parent instanceof Entity
                        ? (Entity) ((MultiPartEntityPart) e).parent : e;
                if (!hits.contains(hit)) {
                    hits.add(hit);
                }
            }
        }
        return hits;
    }

    // ------------------------------------------------------------------------------------------
    // Jab
    // ------------------------------------------------------------------------------------------

    /** 1.21.11's {@code Player.cannotAttackWithItem}: a spear jabs only at full charge, the server allowing 5 ticks. */
    public static boolean charged(EntityPlayer player, float leeway) {
        return player.getCooledAttackStrength(leeway) >= 1.0F;
    }

    /**
     * The jab: everything along the look ({@link #hitsAlong}) for the wielder's attack damage, with knockback, then
     * Lunge. A player's attack timer starts again.
     *
     * @return true if anything was hit
     */
    public static boolean jab(EntityLivingBase attacker) {
        if (attacker.world.isRemote) {
            return false;
        }
        ItemStack stack = attacker.getHeldItemMainhand();
        if (!(stack.getItem() instanceof ItemSpear)) {
            return false;
        }
        SpearTier tier = ((ItemSpear) stack.getItem()).getTier();
        float damage = (float) attacker.getEntityAttribute(SharedMonsterAttributes.ATTACK_DAMAGE).getAttributeValue();
        boolean any = false;
        float strength = attacker instanceof EntityPlayer ? ((EntityPlayer) attacker).getCooledAttackStrength(0.5F) : 1.0F;
        for (Entity target : hitsAlong(attacker)) {
            any |= stab(attacker, EnumHand.MAIN_HAND, target, damage, strength, true, true, false);
        }
        if (attacker instanceof EntityPlayer) {
            ((EntityPlayer) attacker).resetCooldown();
        }
        lunge(attacker);
        World world = attacker.world;
        if (any) {
            world.playSound(null, attacker.posX, attacker.posY, attacker.posZ, Content.sound(tier == SpearTier.WOOD ? "item.spear_wood.hit" : "item.spear.hit"),
                    attacker.getSoundCategory(), 1.0F, 1.0F);
        }
        jabSound(attacker, tier);
        return any;
    }

    /** The jab's whoosh, for everyone but the wielder (whose own game plays it). */
    public static void jabSound(EntityLivingBase attacker, SpearTier tier) {
        attacker.world.playSound(attacker instanceof EntityPlayer ? (EntityPlayer) attacker : null, attacker.posX, attacker.posY, attacker.posZ,
                Content.sound(tier == SpearTier.WOOD ? "item.spear_wood.attack" : "item.spear.attack"), attacker.getSoundCategory(), 1.0F, 1.0F);
    }

    /** The charge's start. */
    public static void useSound(EntityLivingBase user, SpearTier tier) {
        user.world.playSound(user instanceof EntityPlayer ? (EntityPlayer) user : null, user.posX, user.posY, user.posZ,
                Content.sound(tier == SpearTier.WOOD ? "item.spear_wood.use" : "item.spear.use"), user.getSoundCategory(), 1.0F, 1.0F);
    }

    /** A spear hit's damage source: 1.21.11's {@code spear} damage type. */
    public static DamageSource source(EntityLivingBase attacker) {
        return new EntityDamageSource(DAMAGE_TYPE, attacker);
    }

    /**
     * 1.21.11's {@code stabAttack}, for players and mobs.
     *
     * @param damage    the hit before enchantments
     * @param strength  a player's attack charge (1 for a charge, which is not scaled)
     * @return true if it hurt, knocked back or dismounted the target
     */
    public static boolean stab(EntityLivingBase attacker, EnumHand hand, Entity target, float damage, float strength, boolean dealDamage,
                               boolean knockback, boolean dismount) {
        ItemStack stack = attacker.getHeldItem(hand);
        EntityPlayer player = attacker instanceof EntityPlayer ? (EntityPlayer) attacker : null;
        if (player != null && (!ForgeHooks.onPlayerAttackTarget(player, target) || !target.canBeAttackedWithItem() || target.hitByEntity(player))) {
            return false;
        }
        EnumCreatureAttribute kind = target instanceof EntityLivingBase ? ((EntityLivingBase) target).getCreatureAttribute() : EnumCreatureAttribute.UNDEFINED;
        float bonus = EnchantmentHelper.getModifierForCreature(stack, kind);
        boolean using = attacker.isHandActive() && attacker.getActiveHand() == hand;
        if (player != null && !using) {
            bonus *= strength;
            damage *= 0.2F + strength * strength * 0.8F;
        }
        float total = dealDamage ? damage + bonus : 0.0F;
        float healthBefore = target instanceof EntityLivingBase ? ((EntityLivingBase) target).getHealth() : 0.0F;

        int fire = EnchantmentHelper.getFireAspectModifier(attacker);
        boolean lit = false;
        if (dealDamage && fire > 0 && target instanceof EntityLivingBase && !target.isBurning()) {
            lit = true;
            target.setFire(1);
        }
        double tmx = target.motionX;
        double tmy = target.motionY;
        double tmz = target.motionZ;
        boolean hurt = dealDamage && total > 0.0F && target.attackEntityFrom(source(attacker), total);
        if (dealDamage && !hurt && lit) {
            target.extinguish();
        }
        if (knockback) {
            float kb = 0.4F + EnchantmentHelper.getKnockbackModifier(attacker) * 0.5F;
            float sin = MathHelper.sin(attacker.rotationYaw * 0.017453292F);
            float cos = MathHelper.cos(attacker.rotationYaw * 0.017453292F);
            if (target instanceof EntityLivingBase) {
                ((EntityLivingBase) target).knockBack(attacker, kb, sin, -cos);
            } else {
                target.addVelocity(-sin * kb, 0.1, cos * kb);
            }
            attacker.motionX *= 0.6;
            attacker.motionZ *= 0.6;
            target.velocityChanged = true;
        }
        boolean dismounted = false;
        if (dismount && target.isRiding()) {
            target.dismountRidingEntity();
            dismounted = true;
        }
        if (target instanceof EntityPlayerMP && target.velocityChanged) {
            // As 1.12.2's own attack: the player is told its new speed, and the server keeps the old one.
            ((EntityPlayerMP) target).connection.sendPacket(new net.minecraft.network.play.server.SPacketEntityVelocity(target));
            target.velocityChanged = false;
            target.motionX = tmx;
            target.motionY = tmy;
            target.motionZ = tmz;
        }
        if (!hurt && !knockback && !dismounted) {
            return false;
        }
        attacker.setLastAttackedEntity(target);
        if (hurt) {
            if (target instanceof EntityLivingBase) {
                EnchantmentHelper.applyThornEnchantments((EntityLivingBase) target, attacker);
            }
            EnchantmentHelper.applyArthropodEnchantments(attacker, target);
            if (fire > 0) {
                target.setFire(fire * 4);
            }
        }
        if (target instanceof EntityLivingBase) {
            if (player != null) {
                ItemStack before = stack.copy();
                stack.hitEntity((EntityLivingBase) target, player);
                if (stack.isEmpty()) {
                    ForgeEventFactory.onPlayerDestroyItem(player, before, hand);
                    player.setHeldItem(hand, ItemStack.EMPTY);
                }
            } else {
                stack.getItem().hitEntity(stack, (EntityLivingBase) target, attacker);
            }
        }
        if (player != null) {
            if (bonus > 0.0F) {
                player.onEnchantmentCritical(target);
            }
            if (target instanceof EntityLivingBase) {
                float dealt = healthBefore - ((EntityLivingBase) target).getHealth();
                player.addStat(StatList.DAMAGE_DEALT, Math.round(dealt * 10.0F));
                if (player.world instanceof WorldServer && dealt > 2.0F) {
                    ((WorldServer) player.world).spawnParticle(EnumParticleTypes.DAMAGE_INDICATOR, target.posX,
                            target.posY + target.height * 0.5F, target.posZ, (int) (dealt * 0.5), 0.1, 0.0, 0.1, 0.2);
                }
            }
            player.addExhaustion(0.1F);
        }
        return true;
    }

    // ------------------------------------------------------------------------------------------
    // Charge
    // ------------------------------------------------------------------------------------------

    /** A new charge forgets the targets of the last (1.21.11's {@code startUsingItem}). */
    public static void startCharge(EntityLivingBase user) {
        synchronized (STABBED) {
            STABBED.put(user, new HashMap<Entity, Long>());
        }
    }

    /**
     * 1.21.11's {@code LivingEntity.onKineticHit}: the hit sound and the view's jolt come at most once every 11 ticks a
     * wielder ({@code gameTime - last > 10}), however many targets or hits.
     *
     * @return true if this hit gets its feedback (and records it)
     */
    public static boolean feedback(EntityLivingBase user, long now) {
        synchronized (FEEDBACK) {
            Long last = FEEDBACK.get(user);
            if (last != null && now - last <= 10L && now >= last) {
                return false;
            }
            FEEDBACK.put(user, now);
            return true;
        }
    }

    /** @return the world time of the wielder's last charge-hit feedback, or -1 */
    public static long lastFeedback(EntityLivingBase user) {
        synchronized (FEEDBACK) {
            Long last = FEEDBACK.get(user);
            return last == null ? -1L : last;
        }
    }

    /** @return the world time this charge last hit a target, or -1 */
    public static long lastStab(EntityLivingBase user, Entity target) {
        synchronized (STABBED) {
            Map<Entity, Long> m = STABBED.get(user);
            Long t = m == null ? null : m.get(target);
            return t == null ? -1L : t;
        }
    }

    /** @return how many living things this charge has hit (for <i>Mob Kabob</i>) */
    public static int stabbedLiving(EntityLivingBase user) {
        synchronized (STABBED) {
            Map<Entity, Long> m = STABBED.get(user);
            if (m == null) {
                return 0;
            }
            int n = 0;
            for (Entity e : m.keySet()) {
                if (e instanceof EntityLivingBase) {
                    n++;
                }
            }
            return n;
        }
    }

    /** The charge's damage before enchantments: the wielder's base attack damage and the closing speed's share. */
    public static float chargeDamage(SpearTier tier, double baseDamage, double relativeSpeed) {
        return (float) baseDamage + (float) MathHelper.floor(relativeSpeed * (double) tier.damageMultiplier);
    }

    /**
     * One tick of a held charge (1.21.11's {@code KineticWeapon.damageEntities}): after the tier's delay, each target
     * along the look not hit in the last 10 ticks is dismounted, knocked back and damaged as the wielder's speed and
     * closing speed allow, for the wielder's base attack damage plus {@code floor(closing speed x multiplier)}.
     *
     * @param remaining the use's remaining ticks, as {@code onUsingTick} gets them
     * @return true if anything was hit
     */
    public static boolean chargeTick(EntityLivingBase user, ItemStack stack, int remaining) {
        if (user.world.isRemote || !(stack.getItem() instanceof ItemSpear)) {
            return false;
        }
        SpearTier tier = ((ItemSpear) stack.getItem()).getTier();
        int ticks = stack.getMaxItemUseDuration() - remaining;
        if (ticks < tier.delayTicks) {
            return false;
        }
        ticks -= tier.delayTicks;
        Vec3d look = user.getLookVec();
        double speed = look.dotProduct(motion(user));
        double factor = user instanceof EntityPlayer ? 1.0 : (double) SpearTier.MOB_SPEED_FACTOR;
        double base = user.getEntityAttribute(SharedMonsterAttributes.ATTACK_DAMAGE).getBaseValue();
        EnumHand hand = user.getActiveHand() == null ? EnumHand.MAIN_HAND : user.getActiveHand();
        long now = user.world.getTotalWorldTime();
        Map<Entity, Long> stabbed;
        synchronized (STABBED) {
            stabbed = STABBED.get(user);
            if (stabbed == null) {
                stabbed = new HashMap<Entity, Long>();
                STABBED.put(user, stabbed);
            }
        }
        boolean any = false;
        for (Entity target : hitsAlong(user)) {
            Long last = stabbed.get(target);
            if (last != null && now - last < SpearTier.CONTACT_COOLDOWN) {
                continue;
            }
            stabbed.put(target, now);
            double relative = Math.max(0.0, speed - look.dotProduct(motion(target)));
            boolean dismount = tier.dismount.test(ticks, speed, relative, factor);
            boolean knock = tier.knockback.test(ticks, speed, relative, factor);
            boolean hurt = tier.damage.test(ticks, speed, relative, factor);
            if (dismount || knock || hurt) {
                any |= stab(user, hand, target, chargeDamage(tier, base, relative), 1.0F, hurt, knock, dismount);
            }
        }
        if (any) {
            if (feedback(user, now)) {
                NetworkHandler.channel.sendToAllAround(new SpearHitMessage(user.getEntityId(), tier == SpearTier.WOOD),
                        new NetworkRegistry.TargetPoint(user.dimension, user.posX, user.posY, user.posZ, 64.0));
            }
            if (user instanceof EntityPlayerMP) {
                SpearContent.MOB_KABOB.trigger((EntityPlayerMP) user, stabbedLiving(user));
            }
        }
        return any;
    }

    // ------------------------------------------------------------------------------------------
    // Lunge
    // ------------------------------------------------------------------------------------------

    /**
     * Lunge after a jab (1.21.11's {@code post_piercing_attack}): not riding, gliding or in water, and a player needs
     * more than 6 food points (or creative flight). Wears the spear by one, tires by 4 a level, pushes the wielder along their
     * look's level part by 0.458 a level, and plays the level's lunge sound.
     *
     * @return the level that lunged, 0 if none
     */
    public static int lunge(EntityLivingBase user) {
        if (user.world.isRemote || SpearContent.lunge == null) {
            return 0;
        }
        ItemStack stack = user.getHeldItemMainhand();
        int level = EnchantmentHelper.getEnchantmentLevel(SpearContent.lunge, stack);
        if (level <= 0 || user.isRiding() || user.isElytraFlying() || user.isInWater()) {
            return 0;
        }
        if (user instanceof EntityPlayer) {
            EntityPlayer p = (EntityPlayer) user;
            if (!(p.getFoodStats().getFoodLevel() > 6 || p.capabilities.allowFlying)) {
                return 0;
            }
            ItemStack before = stack.copy();
            stack.damageItem(1, p);
            if (stack.isEmpty()) {
                ForgeEventFactory.onPlayerDestroyItem(p, before, EnumHand.MAIN_HAND);
                p.setHeldItem(EnumHand.MAIN_HAND, ItemStack.EMPTY);
            }
            p.addExhaustion(4.0F * level);
        } else {
            stack.damageItem(1, user);
        }
        Vec3d look = user.getLookVec();
        float magnitude = 0.458F * level;
        user.addVelocity(look.x * magnitude, 0.0, look.z * magnitude);
        user.velocityChanged = true;
        if (user instanceof EntityPlayerMP) {
            ((EntityPlayerMP) user).connection.sendPacket(new net.minecraft.network.play.server.SPacketEntityVelocity(user));
        }
        if (!user.isSilent()) {
            user.world.playSound(null, user.posX, user.posY, user.posZ, Content.sound("item.spear.lunge_" + Math.min(level, 3)),
                    user.getSoundCategory(), 1.0F, 1.0F);
        }
        return level;
    }

    /** @return the spear in an entity's main hand, or empty */
    public static ItemStack spear(EntityLivingBase e) {
        ItemStack s = e.getHeldItemMainhand();
        return s.getItem() instanceof ItemSpear ? s : ItemStack.EMPTY;
    }
}
