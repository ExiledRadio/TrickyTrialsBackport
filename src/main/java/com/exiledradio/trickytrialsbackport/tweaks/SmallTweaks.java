package com.exiledradio.trickytrialsbackport.tweaks;

import com.exiledradio.trickytrialsbackport.ModConfig;
import com.exiledradio.trickytrialsbackport.TrickyTrialsBackport;
import net.minecraft.block.Block;
import net.minecraft.block.BlockSkull;
import net.minecraft.block.material.Material;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.EntityCreature;
import net.minecraft.entity.EntityLiving;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.ai.EntityAIBase;
import net.minecraft.entity.ai.EntityAIPanic;
import net.minecraft.entity.ai.EntityAITasks;
import net.minecraft.entity.monster.EntityPolarBear;
import net.minecraft.entity.passive.AbstractHorse;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.entity.projectile.EntityPotion;
import net.minecraft.init.Blocks;
import net.minecraft.init.MobEffects;
import net.minecraft.item.ItemAxe;
import net.minecraft.item.ItemStack;
import net.minecraft.potion.Potion;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.DamageSource;
import net.minecraft.util.EnumActionResult;
import net.minecraft.util.EnumHand;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraftforge.event.entity.EntityJoinWorldEvent;
import net.minecraftforge.event.entity.ProjectileImpactEvent;
import net.minecraftforge.event.entity.living.LivingEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.event.world.BlockEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.EventPriority;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Stream 25's smaller changes that need no patch: axe and shield (1.21), panic (1.21, 1.21.11), no spawn
 * invulnerability and Water Breathing refilling air (1.21.4), elytra and climbables (1.21.5, 1.21.11), tool-free drops
 * (1.21.4), head names (1.21.9), and the splash point P12 reads.
 */
@Mod.EventBusSubscriber(modid = TrickyTrialsBackport.MOD_ID)
public final class SmallTweaks {

    private static final Field RESPAWN_INVULNERABILITY = Tweaks.field(EntityPlayerMP.class, "respawnInvulnerabilityTicks", "field_147101_bU");
    private static final Field PANIC_SPEED = Tweaks.field(EntityAIPanic.class, "speed", "field_75265_b");

    /** Forge tile data key for a placed head's custom name. */
    static final String HEAD_NAME = TrickyTrialsBackport.MOD_ID + ":custom_name";

    private SmallTweaks() {
    }

    // ------------------------------------------------------------------------------------------
    // Axe and shield (1.21)
    // ------------------------------------------------------------------------------------------

    /**
     * 1.21's {@code AxeItem.useOn} passes when the off hand holds a shield and the player is not sneaking, so the use
     * raises the shield. 1.12.2's axe has no use of its own; stripping and scraping are the family's events (Update
     * Aquatic's logs, Caves &amp; Cliffs' and this mod's copper, this mod's pale oak), all after this one.
     */
    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onRightClickBlock(PlayerInteractEvent.RightClickBlock event) {
        if (event.getHand() != EnumHand.MAIN_HAND || !Tweaks.on(ModConfig.tweaks.axeShield)) {
            return;
        }
        if (raisesShield(event.getEntityPlayer().getHeldItemMainhand(), event.getEntityPlayer().getHeldItemOffhand(),
                event.getEntityPlayer().isSneaking(), event.getEntityPlayer())) {
            event.setCanceled(true);
            event.setCancellationResult(EnumActionResult.PASS);
        }
    }

    public static boolean raisesShield(ItemStack main, ItemStack off, boolean sneaking, EntityLivingBase holder) {
        boolean axe = !main.isEmpty() && (main.getItem() instanceof ItemAxe || main.getItem().getToolClasses(main).contains("axe"));
        return axe && !sneaking && !off.isEmpty() && off.getItem().isShield(off, holder);
    }

    // ------------------------------------------------------------------------------------------
    // Panic (1.21, 1.21.11)
    // ------------------------------------------------------------------------------------------

    /** 1.21's {@code #panic_environmental_causes}, as 1.12.2's (and the family's) damage type names. */
    static final Set<String> ENVIRONMENTAL = new HashSet<String>(Arrays.asList(
            "cactus", "freeze", "hotFloor", "inFire", "lava", "lightningBolt", "onFire"));
    /** The rest of 1.21's {@code #panic_causes}. */
    static final Set<String> CAUSES = new HashSet<String>(Arrays.asList(
            "arrow", "dragonBreath", "explosion", "explosion.player", "fireball", "fireworks", "indirectMagic", "magic",
            "mob", "player", "sonic_boom", "sonicBoom", "sting", "thrown", "trident", "windCharge", "wind_charge",
            "wither", "witherSkull", "mobProjectile"));

    /** @return whether a damage source makes an animal panic under 1.21's tags */
    public static boolean panicCause(DamageSource source, boolean environmentalOnly) {
        String type = source.getDamageType();
        return ENVIRONMENTAL.contains(type) || !environmentalOnly && CAUSES.contains(type);
    }

    /** Swaps 1.12.2's panic goals for {@link Panic}; it behaves as 1.12.2's while the setting is off. */
    @SubscribeEvent
    public static void onJoin(EntityJoinWorldEvent event) {
        if (event.getWorld().isRemote) {
            return;
        }
        if (event.getEntity() instanceof EntityPlayerMP && Tweaks.on(ModConfig.tweaks.noSpawnInvulnerability)) {
            Tweaks.set(RESPAWN_INVULNERABILITY, event.getEntity(), 0);
        }
        if (!(event.getEntity() instanceof EntityCreature)) {
            return;
        }
        EntityCreature creature = (EntityCreature) event.getEntity();
        List<EntityAITasks.EntityAITaskEntry> swap = new ArrayList<EntityAITasks.EntityAITaskEntry>();
        for (EntityAITasks.EntityAITaskEntry entry : creature.tasks.taskEntries) {
            Class<?> c = entry.action.getClass();
            if (c == EntityAIPanic.class || c.getName().equals("net.minecraft.entity.monster.EntityPolarBear$AIPanic")) {
                swap.add(entry);
            }
        }
        for (EntityAITasks.EntityAITaskEntry entry : swap) {
            Object speed = Tweaks.get(PANIC_SPEED, entry.action);
            creature.tasks.removeTask(entry.action);
            creature.tasks.addTask(entry.priority, new Panic(creature, speed instanceof Double ? (Double) speed : 1.25D, entry.action));
        }
    }

    /**
     * 1.21's {@code PanicGoal}: panics while the last damage (kept 2 seconds, 1.12.2's {@code getLastDamageSource} too)
     * is one of the panic causes, environmental ones only for an adult polar bear; never while a mob rides and steers
     * a mount (1.21.11). 1.12.2's panicked for as long as it had a revenge target, from anything with an attacker.
     */
    public static final class Panic extends EntityAIPanic {
        private final EntityAIBase old;

        public Panic(EntityCreature creature, double speed, EntityAIBase old) {
            super(creature, speed);
            this.old = old;
        }

        @Override
        public boolean shouldExecute() {
            if (!Tweaks.on(ModConfig.tweaks.panic)) {
                return this.old.shouldExecute();
            }
            if (!this.shouldPanic()) {
                return false;
            }
            if (this.creature.isBurning()) {
                BlockPos water = water(this.creature, 5, 4);
                if (water != null) {
                    this.randPosX = water.getX();
                    this.randPosY = water.getY();
                    this.randPosZ = water.getZ();
                    return true;
                }
            }
            return this.findRandomPosition();
        }

        /** 1.21's {@code PanicGoal.shouldPanic}, with 1.21.11's mob-steered mounts. */
        public boolean shouldPanic() {
            if (this.creature instanceof AbstractHorse && this.creature.getControllingPassenger() instanceof EntityLiving) {
                return false;
            }
            DamageSource last = this.creature.getLastDamageSource();
            boolean environmentalOnly = this.creature instanceof EntityPolarBear && !this.creature.isChild();
            return last != null && panicCause(last, environmentalOnly);
        }

        /** 1.12.2's (private) {@code EntityAIPanic.getRandPos}: the nearest water within the range. */
        private static BlockPos water(EntityCreature e, int range, int vertical) {
            BlockPos at = new BlockPos(e);
            double best = range * range * vertical * 2;
            BlockPos found = null;
            BlockPos.MutableBlockPos p = new BlockPos.MutableBlockPos();
            for (int x = at.getX() - range; x <= at.getX() + range; x++) {
                for (int y = at.getY() - vertical; y <= at.getY() + vertical; y++) {
                    for (int z = at.getZ() - range; z <= at.getZ() + range; z++) {
                        p.setPos(x, y, z);
                        if (e.world.getBlockState(p).getMaterial() == Material.WATER) {
                            double d = (x - at.getX()) * (x - at.getX()) + (y - at.getY()) * (y - at.getY()) + (z - at.getZ()) * (z - at.getZ());
                            if (d < best) {
                                best = d;
                                found = p.toImmutable();
                            }
                        }
                    }
                }
            }
            return found;
        }
    }

    // ------------------------------------------------------------------------------------------
    // Players (1.21.4, 1.21.5, 1.21.11)
    // ------------------------------------------------------------------------------------------

    private static Potion conduitPower;
    private static boolean conduitLooked;

    @SubscribeEvent
    public static void onLivingUpdate(LivingEvent.LivingUpdateEvent event) {
        EntityLivingBase living = event.getEntityLiving();
        if (living.world.isRemote || !ModConfig.tweaks.enabled) {
            return;
        }
        if (ModConfig.tweaks.waterBreathingRefillsAir && living.getAir() < 300 && living.isInsideOfMaterial(Material.WATER)
                && (living.isPotionActive(MobEffects.WATER_BREATHING) || conduit() != null && living.isPotionActive(conduit()))) {
            // 1.21.4's increaseAirSupply: 4 a tick, up to full.
            living.setAir(Math.min(300, living.getAir() + 4));
        }
        if (ModConfig.tweaks.elytraClimbables && living instanceof EntityPlayerMP && living.isElytraFlying()
                && living.isOnLadder() && !glidesThrough(living)) {
            ((EntityPlayerMP) living).clearElytraFlying();
        }
    }

    private static Potion conduit() {
        if (!conduitLooked) {
            conduitLooked = true;
            conduitPower = Potion.REGISTRY.getObject(new ResourceLocation("updateaquaticbackport", "conduit_power"));
        }
        return conduitPower;
    }

    /** 1.21.11's {@code #can_glide_through}: vines, twisting and weeping vines, cave vines. */
    static boolean glidesThrough(EntityLivingBase living) {
        BlockPos pos = new BlockPos(MathHelper.floor(living.posX), MathHelper.floor(living.getEntityBoundingBox().minY), MathHelper.floor(living.posZ));
        ResourceLocation id = living.world.getBlockState(pos).getBlock().getRegistryName();
        return id != null && id.getPath().contains("vines") || living.world.getBlockState(pos).getBlock() == Blocks.VINE;
    }

    // ------------------------------------------------------------------------------------------
    // Tool-free drops (1.21.4)
    // ------------------------------------------------------------------------------------------

    /** Other mods' blocks of the 1.21.4 list, by registry path. */
    static final Set<String> TOOL_FREE_PATHS = new HashSet<String>(Arrays.asList(
            "bell", "lantern", "soul_lantern", "polished_blackstone_pressure_plate", "copper_door", "exposed_copper_door",
            "weathered_copper_door", "oxidized_copper_door", "waxed_copper_door", "waxed_exposed_copper_door",
            "waxed_weathered_copper_door", "waxed_oxidized_copper_door"));

    public static boolean toolFree(Block block) {
        if (block == Blocks.IRON_DOOR || block == Blocks.HEAVY_WEIGHTED_PRESSURE_PLATE || block == Blocks.LIGHT_WEIGHTED_PRESSURE_PLATE
                || block == Blocks.STONE_PRESSURE_PLATE || block == Blocks.BREWING_STAND || block == Blocks.ENDER_CHEST) {
            return true;
        }
        ResourceLocation id = block.getRegistryName();
        return id != null && !"minecraft".equals(id.getNamespace()) && TOOL_FREE_PATHS.contains(id.getPath());
    }

    @SubscribeEvent
    public static void onHarvestCheck(PlayerEvent.HarvestCheck event) {
        if (!event.canHarvest() && Tweaks.on(ModConfig.tweaks.toolFreeDrops) && toolFree(event.getTargetBlock().getBlock())) {
            event.setCanHarvest(true);
        }
    }

    // ------------------------------------------------------------------------------------------
    // Head names (1.21.9)
    // ------------------------------------------------------------------------------------------

    @SubscribeEvent
    public static void onPlace(BlockEvent.PlaceEvent event) {
        if (event.getWorld().isRemote || !(event.getPlacedBlock().getBlock() instanceof BlockSkull) || !Tweaks.on(ModConfig.tweaks.headNames)) {
            return;
        }
        ItemStack stack = event.getPlayer().getHeldItem(event.getHand());
        TileEntity te = event.getWorld().getTileEntity(event.getPos());
        if (te != null && stack.hasDisplayName()) {
            te.getTileData().setString(HEAD_NAME, stack.getDisplayName());
        }
    }

    @SubscribeEvent
    public static void onDrops(BlockEvent.HarvestDropsEvent event) {
        IBlockState state = event.getState();
        if (event.getWorld().isRemote || !(state.getBlock() instanceof BlockSkull) || !Tweaks.on(ModConfig.tweaks.headNames)) {
            return;
        }
        TileEntity te = event.getWorld().getTileEntity(event.getPos());
        if (te == null || !te.getTileData().hasKey(HEAD_NAME)) {
            return;
        }
        String name = te.getTileData().getString(HEAD_NAME);
        for (ItemStack drop : event.getDrops()) {
            if (drop.getItem() == net.minecraft.init.Items.SKULL) {
                drop.setStackDisplayName(name);
            }
        }
    }

    // ------------------------------------------------------------------------------------------
    // P12's splash point
    // ------------------------------------------------------------------------------------------

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onPotionImpact(ProjectileImpactEvent.Throwable event) {
        if (event.getThrowable() instanceof EntityPotion && !event.isCanceled() && event.getRayTraceResult().hitVec != null) {
            MechanicsHooks.splashedAt((EntityPotion) event.getThrowable(), event.getRayTraceResult().hitVec);
        }
    }
}
