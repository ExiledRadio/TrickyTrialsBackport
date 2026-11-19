package com.exiledradio.trickytrialsbackport.locator;

import net.minecraft.block.Block;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.ai.attributes.AttributeModifier;
import net.minecraft.entity.ai.attributes.IAttribute;
import net.minecraft.entity.ai.attributes.IAttributeInstance;
import net.minecraft.entity.ai.attributes.RangedAttribute;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.init.Blocks;
import net.minecraft.init.MobEffects;
import net.minecraft.inventory.EntityEquipmentSlot;
import net.minecraft.item.Item;
import net.minecraft.item.ItemSkull;
import net.minecraft.item.ItemStack;
import net.minecraft.potion.PotionEffect;
import net.minecraft.scoreboard.ScorePlayerTeam;
import net.minecraft.scoreboard.Team;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.text.TextFormatting;

import java.awt.Color;
import java.util.UUID;

/**
 * 1.21.6's waypoint rules as plain functions ({@code WaypointTransmitter}, {@code Waypoint}, {@code LivingEntity}'s
 * waypoint methods and {@code ServerPlayer}'s crouch modifier), shared by the server code and the self-test.
 */
public final class Waypoints {

    /** {@code Waypoint.MAX_RANGE}. */
    public static final double MAX_RANGE = 6.0E7D;
    /** {@code WaypointTransmitter.REALLY_FAR_DISTANCE}: beyond this a waypoint is only a direction. */
    public static final float REALLY_FAR_DISTANCE = 332.0F;
    /** {@code EntityAzimuthConnection.update}: half a degree, in radians. */
    public static final float AZIMUTH_STEP = 0.008726646F;

    /**
     * {@code Attributes.WAYPOINT_TRANSMIT_RANGE}: how far an entity shows on others' locator bars. 0 by default,
     * 6e7 for players ({@code Player.createAttributes}). Its tooltip key is vanilla's.
     */
    public static final IAttribute TRANSMIT_RANGE = new RangedAttribute(null, "waypoint_transmit_range", 0.0D, 0.0D, MAX_RANGE)
            .setDescription("Waypoint Transmit Range");
    /** {@code Attributes.WAYPOINT_RECEIVE_RANGE}: how far a player sees others on its locator bar. */
    public static final IAttribute RECEIVE_RANGE = new RangedAttribute(null, "waypoint_receive_range", 0.0D, 0.0D, MAX_RANGE)
            .setDescription("Waypoint Receive Range");

    /** {@code Waypoint.WAYPOINT_TRANSMIT_RANGE_HIDE_MODIFIER}: -1, multiply total (1.12.2's operation 2). */
    public static final UUID HIDE_ID = UUID.fromString("b9f5f3a6-6a0e-4c8b-9a71-2f0f3c1d6e01");
    /** {@code ServerPlayer.WAYPOINT_TRANSMIT_RANGE_CROUCH_MODIFIER}. */
    public static final UUID CROUCH_ID = UUID.fromString("b9f5f3a6-6a0e-4c8b-9a71-2f0f3c1d6e02");
    /** The invisibility effect's modifier ({@code MobEffects.INVISIBILITY}, {@code effect.waypoint_transmit_range_hide}). */
    public static final UUID INVISIBILITY_ID = UUID.fromString("b9f5f3a6-6a0e-4c8b-9a71-2f0f3c1d6e03");

    static final AttributeModifier HIDE = new AttributeModifier(HIDE_ID, "waypoint_transmit_range_hide", -1.0D, 2).setSaved(false);
    static final AttributeModifier CROUCH = new AttributeModifier(CROUCH_ID, "waypoint_transmit_range_crouch", -1.0D, 2).setSaved(false);

    /** The four kinds of {@code TrackedWaypoint}, in vanilla's order. */
    public static final byte EMPTY = 0;
    public static final byte BLOCK = 1;
    public static final byte CHUNK = 2;
    public static final byte AZIMUTH = 3;

    private Waypoints() {
    }

    /** Gives a player the two attributes with {@code Player.createAttributes}' values (once per entity). */
    public static void addAttributes(EntityPlayer player) {
        if (player.getAttributeMap().getAttributeInstanceByName(TRANSMIT_RANGE.getName()) == null) {
            player.getAttributeMap().registerAttribute(TRANSMIT_RANGE).setBaseValue(MAX_RANGE);
        }
        if (player.getAttributeMap().getAttributeInstanceByName(RECEIVE_RANGE.getName()) == null) {
            player.getAttributeMap().registerAttribute(RECEIVE_RANGE).setBaseValue(MAX_RANGE);
        }
    }

    public static double value(EntityLivingBase entity, IAttribute attribute) {
        IAttributeInstance instance = entity.getAttributeMap().getAttributeInstanceByName(attribute.getName());
        return instance == null ? 0.0D : instance.getAttributeValue();
    }

    /** {@code LivingEntity.isTransmittingWaypoint}. */
    public static boolean transmits(EntityLivingBase entity) {
        return value(entity, TRANSMIT_RANGE) > 0.0D;
    }

    /** {@code ServerPlayer.isReceivingWaypoints}. */
    public static boolean receives(EntityPlayer player) {
        return value(player, RECEIVE_RANGE) > 0.0D;
    }

    /**
     * 1.21.6's {@code Waypoint.addHideAttribute} items worn on the head: the carved pumpkin (1.12.2's pumpkin) and every
     * mob head (1.12.2's skull item, and another mod's {@code carved_pumpkin} or {@code piglin_head}).
     */
    public static boolean hidesWearer(ItemStack head) {
        if (head.isEmpty()) {
            return false;
        }
        Item item = head.getItem();
        if (item instanceof ItemSkull || item == Item.getItemFromBlock(Blocks.PUMPKIN)) {
            return true;
        }
        Block block = Block.getBlockFromItem(item);
        if (block == Blocks.PUMPKIN || block == Blocks.SKULL) {
            return true;
        }
        ResourceLocation name = item.getRegistryName();
        return name != null && ("carved_pumpkin".equals(name.getPath()) || "piglin_head".equals(name.getPath()));
    }

    /** {@code Entity.isCrouching}: 1.12.2's sneak, except while flying, gliding or sleeping (no crouching pose then). */
    public static boolean crouching(EntityPlayer player) {
        return player.isSneaking() && !player.capabilities.isFlying && !player.isElytraFlying() && !player.isPlayerSleeping();
    }

    /**
     * Applies the transient hide modifiers vanilla applies: the crouch one ({@code ServerPlayer.updatePlayerAttributes})
     * and the head item's ({@code ItemAttributeModifiers} on the head slot). Invisibility's comes from the effect.
     */
    public static void updateModifiers(EntityPlayer player) {
        IAttributeInstance range = player.getAttributeMap().getAttributeInstanceByName(TRANSMIT_RANGE.getName());
        if (range == null) {
            return;
        }
        set(range, CROUCH, crouching(player));
        set(range, HIDE, hidesWearer(player.getItemStackFromSlot(EntityEquipmentSlot.HEAD)));
        // The effect applies its own modifier; this keeps it right where the effect was loaded from a save before the
        // attribute existed on the player (1.12.2 restores saved modifiers only for attributes already registered).
        PotionEffect invisibility = player.getActivePotionEffect(MobEffects.INVISIBILITY);
        boolean has = range.getModifier(INVISIBILITY_ID) != null;
        if (invisibility != null && !has) {
            range.applyModifier(new AttributeModifier(INVISIBILITY_ID, "effect.waypoint_transmit_range_hide",
                    -1.0D * (invisibility.getAmplifier() + 1), 2).setSaved(false));
        } else if (invisibility == null && has) {
            range.removeModifier(INVISIBILITY_ID);
        }
    }

    private static void set(IAttributeInstance range, AttributeModifier modifier, boolean on) {
        boolean has = range.getModifier(modifier.getID()) != null;
        if (on && !has) {
            range.applyModifier(modifier);
        } else if (!on && has) {
            range.removeModifier(modifier.getID());
        }
    }

    /**
     * {@code WaypointTransmitter.doesSourceIgnoreReceiver}: a spectator receiver sees everyone; a spectator source, a
     * source carrying the receiver, or one at least {@code min(transmit, receive)} away is not shown.
     */
    public static boolean ignores(EntityLivingBase source, EntityPlayerMP receiver) {
        if (receiver.isSpectator()) {
            return false;
        }
        if (source instanceof EntityPlayer && ((EntityPlayer) source).isSpectator()
                || source.getRecursivePassengers().contains(receiver)) {
            return true;
        }
        double range = Math.min(value(source, TRANSMIT_RANGE), value(receiver, RECEIVE_RANGE));
        return source.getDistance(receiver) >= range;
    }

    /** {@code WaypointTransmitter.isReallyFar}. */
    public static boolean reallyFar(EntityLivingBase source, EntityPlayerMP receiver) {
        return source.getDistance(receiver) > REALLY_FAR_DISTANCE;
    }

    /**
     * {@code WaypointTransmitter.isChunkVisible}: the chunk is within the receiver's view distance (1.12.2 sends a
     * square of chunks around the player's chunk).
     */
    public static boolean chunkVisible(int chunkX, int chunkZ, EntityPlayerMP receiver, int viewDistance) {
        return Math.abs(chunkX - receiver.chunkCoordX) <= viewDistance && Math.abs(chunkZ - receiver.chunkCoordZ) <= viewDistance;
    }

    /**
     * {@code LivingEntity.makeWaypointConnectionWith}'s choice.
     *
     * @return {@link #AZIMUTH}, {@link #CHUNK} or {@link #BLOCK}, or -1 for no connection
     */
    public static int connectionType(EntityLivingBase source, EntityPlayerMP receiver, int viewDistance) {
        if (source == receiver || source.ticksExisted == 0 || ignores(source, receiver)) {
            return -1;
        }
        if (reallyFar(source, receiver)) {
            return AZIMUTH;
        }
        return chunkVisible(MathHelper.floor(source.posX) >> 4, MathHelper.floor(source.posZ) >> 4, receiver, viewDistance) ? BLOCK : CHUNK;
    }

    /** {@code EntityAzimuthConnection}'s angle, in radians: receiver minus source, turned 90 degrees clockwise. */
    public static float azimuth(EntityLivingBase source, EntityPlayerMP receiver) {
        double dx = receiver.posX - source.posX;
        double dz = receiver.posZ - source.posZ;
        // Vec3.rotateClockwise90 is (-z, y, x); atan2 of its z over its x.
        return (float) MathHelper.atan2(dx, -dz);
    }

    /**
     * {@code Waypoint.Icon.cloneAndAssignStyle}'s color: the team's color, black shown as dark gray, none without a
     * team or a team with no color.
     *
     * @return the RGB color, or -1 for none
     */
    public static int teamColor(EntityLivingBase source) {
        Team team = source.getTeam();
        if (!(team instanceof ScorePlayerTeam)) {
            return -1;
        }
        TextFormatting color = ((ScorePlayerTeam) team).getColor();
        int rgb = rgb(color);
        if (rgb < 0) {
            return -1;
        }
        return rgb == 0 ? 0x303030 : rgb;
    }

    /** {@code ChatFormatting}'s colors, the same table 1.12.2's font renderer builds; -1 for a non-color. */
    public static int rgb(TextFormatting color) {
        if (color == null || !color.isColor()) {
            return -1;
        }
        int i = color.getColorIndex();
        int j = (i >> 3 & 1) * 85;
        int r = (i >> 2 & 1) * 170 + j;
        int g = (i >> 1 & 1) * 170 + j;
        int b = (i & 1) * 170 + j;
        if (i == 6) {
            r += 85;
        }
        return r << 16 | g << 8 | b;
    }

    /** The color a waypoint without one is drawn in: {@code ARGB.setBrightness(ARGB.color(255, uuid.hashCode()), 0.9F)}. */
    public static int idColor(UUID id) {
        int rgb = id.hashCode() & 0xFFFFFF;
        float[] hsb = Color.RGBtoHSB(rgb >> 16 & 255, rgb >> 8 & 255, rgb & 255, null);
        return 0xFF000000 | Color.HSBtoRGB(hsb[0], hsb[1], 0.9F) & 0xFFFFFF;
    }
}
