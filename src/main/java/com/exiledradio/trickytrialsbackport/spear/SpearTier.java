package com.exiledradio.trickytrialsbackport.spear;

/**
 * The seven spears' numbers, from 1.21.11's {@code Items} ({@code Item.Properties.spear(material, ...)}) and its
 * {@code ToolMaterial}s.
 *
 * <p>Every time is kept as 1.21.11 turns it into ticks: {@code (int) (seconds * 20.0F)} in float arithmetic, so a
 * rounding vanilla makes is made here too.
 */
public enum SpearTier {
    //            name         uses  bonus ench swing  mult    delay dismount     knockback     damage
    WOOD("wooden_spear", 59, 0.0F, 15, 0.65F, 0.7F, 0.75F, 5.0F, 14.0F, 10.0F, 5.1F, 15.0F, 4.6F),
    STONE("stone_spear", 131, 1.0F, 5, 0.75F, 0.82F, 0.7F, 4.5F, 10.0F, 9.0F, 5.1F, 13.75F, 4.6F),
    COPPER("copper_spear", 190, 1.0F, 13, 0.85F, 0.82F, 0.65F, 4.0F, 9.0F, 8.25F, 5.1F, 12.5F, 4.6F),
    IRON("iron_spear", 250, 2.0F, 14, 0.95F, 0.95F, 0.6F, 2.5F, 8.0F, 6.75F, 5.1F, 11.25F, 4.6F),
    GOLD("golden_spear", 32, 0.0F, 22, 0.95F, 0.7F, 0.7F, 3.5F, 10.0F, 8.5F, 5.1F, 13.75F, 4.6F),
    DIAMOND("diamond_spear", 1561, 3.0F, 10, 1.05F, 1.075F, 0.5F, 3.0F, 7.5F, 6.5F, 5.1F, 10.0F, 4.6F),
    NETHERITE("netherite_spear", 2031, 4.0F, 15, 1.15F, 1.2F, 0.4F, 2.5F, 7.0F, 5.5F, 5.1F, 8.75F, 4.6F);

    /** 1.21.11's {@code KineticWeapon}: a target can be hit again after this many ticks of one charge. */
    public static final int CONTACT_COOLDOWN = 10;
    /** 1.21.11's spear {@code AttackRange}: reach 2 to 4.5 (creative 2 to 6.5), hitbox margin 0.125, mobs at half. */
    public static final float MIN_REACH = 2.0F;
    public static final float MAX_REACH = 4.5F;
    public static final float MIN_CREATIVE_REACH = 2.0F;
    public static final float MAX_CREATIVE_REACH = 6.5F;
    public static final float HITBOX_MARGIN = 0.125F;
    public static final float MOB_REACH_FACTOR = 0.5F;
    /** A mob's speeds count five times less against the charge's thresholds (1.21.11: 0.2 for non-players). */
    public static final float MOB_SPEED_FACTOR = 0.2F;
    /** {@code KineticWeapon.forwardMovement}, which only the drawn pose uses. */
    public static final float FORWARD_MOVEMENT = 0.38F;

    public final String itemName;
    public final int durability;
    /** The material's attack damage bonus: a spear hits for 1 + this (a player's base 1). */
    public final float damageBonus;
    public final int enchantability;
    /** Seconds per jab: attack speed is 1 / this. */
    public final float swingSeconds;
    /** The charge's damage per block a second of closing speed. */
    public final float damageMultiplier;
    public final int delayTicks;
    public final Condition dismount;
    public final Condition knockback;
    public final Condition damage;
    /** The jab's swing animation length, {@code (int) (swing * 20)}. */
    public final int swingTicks;

    SpearTier(String itemName, int durability, float damageBonus, int enchantability, float swing, float multiplier, float delay,
              float dismountSeconds, float dismountSpeed, float knockbackSeconds, float knockbackSpeed, float damageSeconds,
              float damageRelativeSpeed) {
        this.itemName = itemName;
        this.durability = durability;
        this.damageBonus = damageBonus;
        this.enchantability = enchantability;
        this.swingSeconds = swing;
        this.damageMultiplier = multiplier;
        this.delayTicks = (int) (delay * 20.0F);
        this.dismount = new Condition((int) (dismountSeconds * 20.0F), dismountSpeed, 0.0F);
        this.knockback = new Condition((int) (knockbackSeconds * 20.0F), knockbackSpeed, 0.0F);
        this.damage = new Condition((int) (damageSeconds * 20.0F), 0.0F, damageRelativeSpeed);
        this.swingTicks = (int) (swing * 20.0F);
    }

    /** A spear's attack damage, a player's base 1 included. */
    public float attackDamage() {
        return 1.0F + this.damageBonus;
    }

    /** Attacks a second: 1.21.11's modifier is {@code 1 / swing - 4} over a player's base 4. */
    public double attackSpeed() {
        return (double) (1.0F / this.swingSeconds);
    }

    /** {@code KineticWeapon.computeDamageUseDuration}: the delay and the damage window, how long a mob holds a charge. */
    public int damageUseDuration() {
        return this.delayTicks + this.damage.maxDurationTicks;
    }

    /** 1.21.11's {@code KineticWeapon.Condition}: until a time, at least a speed and a closing speed (blocks a second). */
    public static final class Condition {
        public final int maxDurationTicks;
        public final float minSpeed;
        public final float minRelativeSpeed;

        Condition(int maxDurationTicks, float minSpeed, float minRelativeSpeed) {
            this.maxDurationTicks = maxDurationTicks;
            this.minSpeed = minSpeed;
            this.minRelativeSpeed = minRelativeSpeed;
        }

        /**
         * @param ticks    ticks of the charge after its delay
         * @param speed    the wielder's speed along their look, blocks a second
         * @param relative how much faster than the target along that look, at least 0
         * @param factor   1 for a player, 0.2 for a mob
         */
        public boolean test(int ticks, double speed, double relative, double factor) {
            return ticks <= this.maxDurationTicks && speed >= this.minSpeed * factor && relative >= this.minRelativeSpeed * factor;
        }
    }
}
