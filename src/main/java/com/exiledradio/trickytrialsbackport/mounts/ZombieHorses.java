package com.exiledradio.trickytrialsbackport.mounts;

import com.exiledradio.trickytrialsbackport.ModConfig;
import com.exiledradio.trickytrialsbackport.TrickyTrialsBackport;

import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLiving;
import net.minecraft.entity.EnumCreatureType;
import net.minecraft.entity.SharedMonsterAttributes;
import net.minecraft.entity.ai.EntityAIRunAroundLikeCrazy;
import net.minecraft.entity.ai.EntityAITasks;
import net.minecraft.entity.ai.EntityAITempt;
import net.minecraft.entity.ai.attributes.IAttributeInstance;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.entity.monster.EntityZombie;
import net.minecraft.entity.passive.AbstractHorse;
import net.minecraft.entity.passive.EntityChicken;
import net.minecraft.entity.passive.EntityZombieHorse;
import net.minecraft.init.Biomes;
import net.minecraft.init.Blocks;
import net.minecraft.init.Items;
import net.minecraft.inventory.ContainerHorseChest;
import net.minecraft.inventory.EntityEquipmentSlot;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.EnumDifficulty;
import net.minecraft.world.EnumSkyBlock;
import net.minecraft.world.World;
import net.minecraft.world.biome.Biome;
import net.minecraft.world.storage.loot.LootEntry;
import net.minecraft.world.storage.loot.LootEntryItem;
import net.minecraft.world.storage.loot.LootPool;
import net.minecraft.world.storage.loot.LootTable;
import net.minecraft.world.storage.loot.LootTableList;
import net.minecraft.world.storage.loot.RandomValueRange;
import net.minecraft.world.storage.loot.conditions.LootCondition;
import net.minecraft.world.storage.loot.functions.LootFunction;
import net.minecraft.world.storage.loot.functions.LootingEnchantBonus;
import net.minecraft.world.storage.loot.functions.SetCount;
import net.minecraftforge.event.LootTableLoadEvent;
import net.minecraftforge.event.entity.EntityJoinWorldEvent;
import net.minecraftforge.event.entity.living.LivingDropsEvent;
import net.minecraftforge.event.entity.living.LivingEvent;
import net.minecraftforge.event.entity.living.LivingSpawnEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.Event;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.registry.EntityRegistry;
import net.minecraftforge.fml.relauncher.ReflectionHelper;

import javax.annotation.Nullable;
import java.lang.reflect.Field;
import java.util.function.Supplier;

/**
 * 1.21.11's zombie horse, on 1.12.2's own entity ({@code mounts.zombieHorses}), and its natural spawning
 * ({@code mounts.zombieHorseSpawn}).
 *
 * <ul>
 * <li>25 health; speed {@code (9 + 3 x random) / 42.16} and jump strength {@code 0.5 + 3 x random / 15}, rolled once
 * (a zombie horse from before keeps neither: 1.12.2 gave every one the same 0.2 speed).</li>
 * <li>Red mushrooms tempt it (1.25 x speed) and heal it (3 health, 3 temper); tamed by riding, as a horse.</li>
 * <li>Takes a saddle and horse armor (patch P24a opens the armor slot); the armor is shown, counts, and keeps the sun
 * off. It burns in daylight as a zombie does, without armor.</li>
 * <li>Untamed, it despawns as a monster does (P24a); a player's use makes it persistent.</li>
 * <li>A mob rider controls it: it never bucks one off (1.21.11's {@code RunAroundLikeCrazyGoal} skips a mob-controlled
 * horse).</li>
 * <li>Drops 2-3 rotten flesh, Looting +0-1; a zombie riding one drops 0-1 red mushroom, Looting +0-1.</li>
 * <li>Spawning: weight 5, alone, in plains and savannas (1.21.11's six; 1.12.2's plains, sunflower plains, savanna,
 * savanna plateau and both shattered savannas), in the monster pass and under monster rules (dark, not Peaceful).
 * A natural spawn always carries a zombie, holding an iron spear when stream 22's spear exists.</li>
 * </ul>
 */
@Mod.EventBusSubscriber(modid = TrickyTrialsBackport.MOD_ID)
public final class ZombieHorses {

    static final double HEALTH = 25.0D;
    static final double OLD_HEALTH = 15.0D;
    static final double OLD_SPEED = 0.20000000298023224D;
    static final int WEIGHT = 5;

    /** Forge entity data: the 1.21.11 attributes were rolled. */
    static final String ROLLED = TrickyTrialsBackport.MOD_ID + ":rolled";
    /** Forge entity data: a natural spawn still owes its zombie rider. */
    static final String RIDER = TrickyTrialsBackport.MOD_ID + ":rider";

    static final Supplier<Item> RED_MUSHROOM = () -> Item.getItemFromBlock(Blocks.RED_MUSHROOM);

    private static final Field CHEST = ReflectionHelper.findField(AbstractHorse.class, "horseChest", "field_110296_bG");

    private ZombieHorses() {
    }

    static boolean on() {
        return MountsHooks.zombieHorses();
    }

    static boolean spawnOn() {
        return MountsContent.enabled() && ModConfig.mounts.zombieHorseSpawn;
    }

    /** 1.21.11's plains and savannas, in 1.12.2's names. */
    static Biome[] biomes() {
        return new Biome[] {Biomes.PLAINS, Biomes.MUTATED_PLAINS, Biomes.SAVANNA, Biomes.SAVANNA_PLATEAU, Biomes.MUTATED_SAVANNA,
                Biomes.MUTATED_SAVANNA_ROCK};
    }

    static void postInit() {
        if (spawnOn()) {
            EntityRegistry.addSpawn(EntityZombieHorse.class, WEIGHT, 1, 1, EnumCreatureType.MONSTER, biomes());
        }
    }

    @Nullable
    static ContainerHorseChest chest(AbstractHorse horse) {
        try {
            return (ContainerHorseChest) CHEST.get(horse);
        } catch (IllegalAccessException e) {
            return null;
        }
    }

    /** @return the zombie horse's armor (its chest's slot 1), or empty */
    static ItemStack armor(AbstractHorse horse) {
        ContainerHorseChest chest = chest(horse);
        return chest == null || chest.getSizeInventory() < 2 ? ItemStack.EMPTY : chest.getStackInSlot(1);
    }

    /** @return true if the first passenger is a mob (1.21.11's {@code isMobControlled}) */
    static boolean mobControlled(Entity mount) {
        return mount.getControllingPassenger() instanceof EntityLiving;
    }

    // ------------------------------------------------------------------ joining: attributes and goals

    @SubscribeEvent
    public static void onJoin(EntityJoinWorldEvent event) {
        if (!(event.getEntity() instanceof EntityZombieHorse) || event.getWorld().isRemote || !on()) {
            return;
        }
        EntityZombieHorse horse = (EntityZombieHorse) event.getEntity();
        if (!horse.getEntityData().getBoolean(ROLLED)) {
            horse.getEntityData().setBoolean(ROLLED, true);
            roll(horse);
        }
        goals(horse);
        horse.setDropChance(EntityEquipmentSlot.CHEST, 0.0F);
    }

    /** 1.21.11's attributes: 25 health (a full horse stays full) and the rolled speed and jump. */
    static void roll(EntityZombieHorse horse) {
        IAttributeInstance health = horse.getEntityAttribute(SharedMonsterAttributes.MAX_HEALTH);
        if (health.getBaseValue() == OLD_HEALTH) {
            boolean full = horse.getHealth() >= horse.getMaxHealth();
            health.setBaseValue(HEALTH);
            if (full) {
                horse.setHealth(horse.getMaxHealth());
            }
        }
        IAttributeInstance speed = horse.getEntityAttribute(SharedMonsterAttributes.MOVEMENT_SPEED);
        if (speed.getBaseValue() == OLD_SPEED) {
            java.util.Random r = horse.getRNG();
            speed.setBaseValue((9.0D + r.nextDouble() + r.nextDouble() + r.nextDouble()) / 42.16F);
            horse.getEntityAttribute(jumpStrength()).setBaseValue(0.5D + (r.nextDouble() + r.nextDouble() + r.nextDouble()) / 15.0D);
        }
    }

    static net.minecraft.entity.ai.attributes.IAttribute jumpStrength() {
        return ReflectionHelper.getPrivateValue(AbstractHorse.class, null, "JUMP_STRENGTH", "field_110271_bv");
    }

    /** 1.21.11's goals: AbstractHorse's, the crazy run skipped while a mob rides, and the red mushroom tempt at 3. */
    static void goals(EntityZombieHorse horse) {
        EntityAITasks.EntityAITaskEntry crazy = null;
        boolean tempt = false;
        for (EntityAITasks.EntityAITaskEntry entry : horse.tasks.taskEntries) {
            if (entry.action.getClass() == EntityAIRunAroundLikeCrazy.class) {
                crazy = entry;
            } else if (entry.action instanceof Tempt) {
                tempt = true;
            }
        }
        if (crazy != null) {
            horse.tasks.removeTask(crazy.action);
            horse.tasks.addTask(crazy.priority, new RunAroundUnlessMobRides(horse));
        }
        if (!tempt) {
            horse.tasks.addTask(3, new Tempt(horse));
        }
    }

    /** 1.21.11's {@code RunAroundLikeCrazyGoal.canUse}: not while a mob rides. */
    static final class RunAroundUnlessMobRides extends EntityAIRunAroundLikeCrazy {
        private final AbstractHorse horse;

        RunAroundUnlessMobRides(AbstractHorse horse) {
            super(horse, 1.2D);
            this.horse = horse;
        }

        @Override
        public boolean shouldExecute() {
            return !mobControlled(this.horse) && super.shouldExecute();
        }
    }

    static final class Tempt extends EntityAITempt {
        Tempt(EntityZombieHorse horse) {
            super(horse, 1.25D, RED_MUSHROOM.get(), false);
        }
    }

    // ------------------------------------------------------------------ ticking: armor, sun, rider

    @SubscribeEvent
    public static void onUpdate(LivingEvent.LivingUpdateEvent event) {
        if (!(event.getEntityLiving() instanceof EntityZombieHorse) || event.getEntityLiving().world.isRemote || !on()) {
            return;
        }
        EntityZombieHorse horse = (EntityZombieHorse) event.getEntityLiving();
        if (horse.getEntityData().getBoolean(RIDER)) {
            horse.getEntityData().removeTag(RIDER);
            spawnRider(horse);
        }
        syncArmor(horse);
        burn(horse);
    }

    /**
     * Shows the armor in the chest's slot 1 as the horse's chest equipment (synced to clients by 1.12.2's own
     * equipment packets; never dropped, as the chest drops it), and gives its armor value as a horse's armor does,
     * with netherite's toughness and knockback resistance.
     */
    static void syncArmor(EntityZombieHorse horse) {
        ItemStack armor = armor(horse);
        if (!ItemStack.areItemStacksEqual(armor, horse.getItemStackFromSlot(EntityEquipmentSlot.CHEST))) {
            horse.setItemStackToSlot(EntityEquipmentSlot.CHEST, armor.copy());
        }
        HorseArmor.apply(horse, armor);
    }

    /** 1.21.11's {@code Mob.burnUndead} with the body slot as the sun protection. */
    static void burn(EntityZombieHorse horse) {
        World world = horse.world;
        if (!horse.isEntityAlive() || !world.isDaytime() || horse.isWet()) {
            return;
        }
        float light = horse.getBrightness();
        BlockPos eyes = new BlockPos(horse.posX, horse.posY + horse.getEyeHeight(), horse.posZ);
        if (light > 0.5F && horse.getRNG().nextFloat() * 30.0F < (light - 0.4F) * 2.0F && world.canSeeSky(eyes) && armor(horse).isEmpty()) {
            horse.setFire(8);
        }
    }

    /** 1.21.11's {@code ZombieHorse.finalizeSpawn} for a natural spawn: a zombie rides it, with an iron spear. */
    static EntityZombie spawnRider(EntityZombieHorse horse) {
        World world = horse.world;
        EntityZombie zombie = new EntityZombie(world);
        zombie.setLocationAndAngles(horse.posX, horse.posY, horse.posZ, horse.rotationYaw, 0.0F);
        zombie.onInitialSpawn(world.getDifficultyForLocation(new BlockPos(zombie)), null);
        Entity vehicle = zombie.getRidingEntity();
        if (vehicle != null) {
            // 1.21.11 makes the rider as a jockey, which never becomes a chicken jockey itself.
            zombie.dismountRidingEntity();
            if (vehicle instanceof EntityChicken) {
                if (vehicle.ticksExisted == 0) {
                    vehicle.setDead();
                } else {
                    ((EntityChicken) vehicle).setChickenJockey(false);
                }
            }
        }
        ItemStack spear = MountsContent.ironSpear();
        if (!spear.isEmpty()) {
            zombie.setItemStackToSlot(EntityEquipmentSlot.MAINHAND, spear);
        }
        world.spawnEntity(zombie);
        zombie.startRiding(horse, true);
        return zombie;
    }

    // ------------------------------------------------------------------ spawning

    /** Monster rules for a natural zombie horse (1.21.11's {@code Monster.checkMonsterSpawnRules}, in 1.12.2's terms). */
    @SubscribeEvent
    public static void onCheckSpawn(LivingSpawnEvent.CheckSpawn event) {
        if (!(event.getEntityLiving() instanceof EntityZombieHorse) || event.isSpawner() || !spawnOn()) {
            return;
        }
        EntityLiving horse = (EntityLiving) event.getEntityLiving();
        event.setResult(monsterRules(horse) && horse.isNotColliding() ? Event.Result.ALLOW : Event.Result.DENY);
    }

    /** 1.12.2's {@code EntityMob.getCanSpawnHere}: not Peaceful, dark enough, and a block it may spawn on. */
    static boolean monsterRules(EntityLiving mob) {
        World world = mob.world;
        if (world.getDifficulty() == EnumDifficulty.PEACEFUL) {
            return false;
        }
        BlockPos pos = new BlockPos(mob.posX, mob.getEntityBoundingBox().minY, mob.posZ);
        if (world.getLightFor(EnumSkyBlock.SKY, pos) > mob.getRNG().nextInt(32)) {
            return false;
        }
        int light = world.getLightFromNeighbors(pos);
        if (world.isThundering()) {
            int skylight = world.getSkylightSubtracted();
            world.setSkylightSubtracted(10);
            light = world.getLightFromNeighbors(pos);
            world.setSkylightSubtracted(skylight);
        }
        return light <= mob.getRNG().nextInt(8) && world.getBlockState(pos.down()).canEntitySpawn(mob);
    }

    /** A natural spawn owes a rider, spawned on its first tick (when it is in the world for sure). */
    @SubscribeEvent
    public static void onSpecialSpawn(LivingSpawnEvent.SpecialSpawn event) {
        if (event.getEntityLiving() instanceof EntityZombieHorse && event.getSpawner() == null && spawnOn()) {
            event.getEntityLiving().getEntityData().setBoolean(RIDER, true);
        }
    }

    // ------------------------------------------------------------------ drops

    @SubscribeEvent
    public static void onLootLoad(LootTableLoadEvent event) {
        if (!on() || !LootTableList.ENTITIES_ZOMBIE_HORSE.equals(event.getName())) {
            return;
        }
        LootCondition[] none = new LootCondition[0];
        LootEntry flesh = new LootEntryItem(Items.ROTTEN_FLESH, 1, 0, new LootFunction[] {
                new SetCount(none, new RandomValueRange(2.0F, 3.0F)),
                new LootingEnchantBonus(none, new RandomValueRange(0.0F, 1.0F), 0)}, none, "minecraft:rotten_flesh");
        event.setTable(new LootTable(new LootPool[] {
                new LootPool(new LootEntry[] {flesh}, none, new RandomValueRange(1.0F), new RandomValueRange(0.0F), "main")}));
    }

    /** 1.21.11's {@code entities/zombie}: 0-1 red mushroom (Looting +0-1) for a zombie riding a zombie horse. */
    @SubscribeEvent
    public static void onDrops(LivingDropsEvent event) {
        Entity dead = event.getEntityLiving();
        if (dead.getClass() != EntityZombie.class || !(dead.getRidingEntity() instanceof EntityZombieHorse) || !on()
                || !dead.world.getGameRules().getBoolean("doMobLoot")) {
            return;
        }
        java.util.Random r = event.getEntityLiving().getRNG();
        int count = r.nextInt(2) + Math.round(event.getLootingLevel() * r.nextFloat());
        if (count > 0) {
            EntityItem item = new EntityItem(dead.world, dead.posX, dead.posY, dead.posZ, new ItemStack(Blocks.RED_MUSHROOM, count));
            item.setDefaultPickupDelay();
            event.getDrops().add(item);
        }
    }
}
