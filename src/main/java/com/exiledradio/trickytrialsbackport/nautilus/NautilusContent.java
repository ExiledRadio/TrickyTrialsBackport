package com.exiledradio.trickytrialsbackport.nautilus;

import com.exiledradio.backportcore.smithing.SmithingUpgrades;
import com.exiledradio.trickytrialsbackport.ModConfig;
import com.exiledradio.trickytrialsbackport.TrickyTrialsBackport;
import com.exiledradio.trickytrialsbackport.common.Content;
import com.exiledradio.trickytrialsbackport.common.Guis;
import com.exiledradio.trickytrialsbackport.common.Owners;

import net.minecraft.block.material.Material;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityList;
import net.minecraft.entity.EntityLiving;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.EnumCreatureType;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.init.MobEffects;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.potion.Potion;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.SoundEvent;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraft.world.biome.Biome;
import net.minecraft.world.storage.loot.LootEntry;
import net.minecraft.world.storage.loot.LootEntryEmpty;
import net.minecraft.world.storage.loot.LootEntryItem;
import net.minecraft.world.storage.loot.LootPool;
import net.minecraft.world.storage.loot.LootTableList;
import net.minecraft.world.storage.loot.RandomValueRange;
import net.minecraft.world.storage.loot.conditions.KilledByPlayer;
import net.minecraft.world.storage.loot.conditions.LootCondition;
import net.minecraft.world.storage.loot.conditions.RandomChanceWithLooting;
import net.minecraft.world.storage.loot.functions.LootFunction;
import net.minecraft.entity.EntitySpawnPlacementRegistry;
import net.minecraftforge.common.BiomeDictionary;
import net.minecraftforge.event.LootTableLoadEvent;
import net.minecraftforge.event.RegistryEvent;
import net.minecraftforge.event.entity.living.LivingEvent;
import net.minecraftforge.event.entity.living.LivingSpawnEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import net.minecraftforge.fml.common.registry.EntityEntry;
import net.minecraftforge.fml.common.registry.EntityEntryBuilder;
import net.minecraftforge.fml.common.registry.EntityRegistry;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.WeakHashMap;

/**
 * Stream 23: the nautilus (1.21.11). The nautilus (entity id 60) and zombie nautilus (61; 62-64 unused), the five
 * nautilus armors, Breath of the Nautilus, the sounds, the loot, ocean spawning, Update Aquatic's drowned riding zombie
 * nautiluses and its chests holding nautilus armor. Packet 115 opens a ridden nautilus's inventory (116-119 unused).
 *
 * <p>The nautilus shell, the fish buckets, the drowned, the trident, the pufferfish mob and the ocean structures are
 * Update Aquatic's; each is looked up by name, so the stream works with or without it.
 */
@Mod.EventBusSubscriber(modid = TrickyTrialsBackport.MOD_ID)
public final class NautilusContent {

    public static final int NAUTILUS_ID = 60;
    public static final int ZOMBIE_NAUTILUS_ID = 61;
    /** The GUI id is the stream's row. */
    public static final int GUI_ID = 23;
    /** Spawn egg colors, the two commonest of each 1.21.11 egg texture. */
    public static final int EGG_PRIMARY = 0xDB5A41;
    public static final int EGG_SECONDARY = 0x66452E;
    public static final int ZOMBIE_EGG_PRIMARY = 0x776C49;
    public static final int ZOMBIE_EGG_SECONDARY = 0x615633;
    /** Weights in 1.21.11's ocean spawn lists: 5 in temperate and warm oceans, 2 in cold and frozen ones. */
    public static final int WEIGHT = 5;
    public static final int COLD_WEIGHT = 2;
    /** {@code Drowned.ZOMBIE_NAUTILUS_JOCKEY_CHANCE}. */
    public static final float JOCKEY_CHANCE = 0.5F;
    public static final ResourceLocation DROWNED = new ResourceLocation(Owners.UPDATE_AQUATIC, "drowned");
    /** Update Aquatic's chests that 1.21.11 gives nautilus armor. */
    public static final String[] ARMOR_CHESTS = {"buried_treasure", "shipwreck_map", "shipwreck_supply", "shipwreck_treasure",
            "underwater_ruin_big", "underwater_ruin_small"};

    public static final SoundEvent AMBIENT = Content.sound("entity.nautilus.ambient");
    public static final SoundEvent AMBIENT_LAND = Content.sound("entity.nautilus.ambient_land");
    public static final SoundEvent DASH = Content.sound("entity.nautilus.dash");
    public static final SoundEvent DASH_LAND = Content.sound("entity.nautilus.dash_land");
    public static final SoundEvent DASH_READY = Content.sound("entity.nautilus.dash_ready");
    public static final SoundEvent DASH_READY_LAND = Content.sound("entity.nautilus.dash_ready_land");
    public static final SoundEvent DEATH = Content.sound("entity.nautilus.death");
    public static final SoundEvent DEATH_LAND = Content.sound("entity.nautilus.death_land");
    public static final SoundEvent EAT = Content.sound("entity.nautilus.eat");
    public static final SoundEvent HURT = Content.sound("entity.nautilus.hurt");
    public static final SoundEvent HURT_LAND = Content.sound("entity.nautilus.hurt_land");
    public static final SoundEvent RIDING = Content.sound("entity.nautilus.riding");
    public static final SoundEvent SWIM = Content.sound("entity.nautilus.swim");
    public static final SoundEvent BABY_AMBIENT = Content.sound("entity.baby_nautilus.ambient");
    public static final SoundEvent BABY_AMBIENT_LAND = Content.sound("entity.baby_nautilus.ambient_land");
    public static final SoundEvent BABY_DEATH = Content.sound("entity.baby_nautilus.death");
    public static final SoundEvent BABY_DEATH_LAND = Content.sound("entity.baby_nautilus.death_land");
    public static final SoundEvent BABY_EAT = Content.sound("entity.baby_nautilus.eat");
    public static final SoundEvent BABY_HURT = Content.sound("entity.baby_nautilus.hurt");
    public static final SoundEvent BABY_HURT_LAND = Content.sound("entity.baby_nautilus.hurt_land");
    public static final SoundEvent BABY_SWIM = Content.sound("entity.baby_nautilus.swim");
    public static final SoundEvent ZOMBIE_AMBIENT = Content.sound("entity.zombie_nautilus.ambient");
    public static final SoundEvent ZOMBIE_AMBIENT_LAND = Content.sound("entity.zombie_nautilus.ambient_land");
    public static final SoundEvent ZOMBIE_DASH = Content.sound("entity.zombie_nautilus.dash");
    public static final SoundEvent ZOMBIE_DASH_LAND = Content.sound("entity.zombie_nautilus.dash_land");
    public static final SoundEvent ZOMBIE_DASH_READY = Content.sound("entity.zombie_nautilus.dash_ready");
    public static final SoundEvent ZOMBIE_DASH_READY_LAND = Content.sound("entity.zombie_nautilus.dash_ready_land");
    public static final SoundEvent ZOMBIE_DEATH = Content.sound("entity.zombie_nautilus.death");
    public static final SoundEvent ZOMBIE_DEATH_LAND = Content.sound("entity.zombie_nautilus.death_land");
    public static final SoundEvent ZOMBIE_EAT = Content.sound("entity.zombie_nautilus.eat");
    public static final SoundEvent ZOMBIE_HURT = Content.sound("entity.zombie_nautilus.hurt");
    public static final SoundEvent ZOMBIE_HURT_LAND = Content.sound("entity.zombie_nautilus.hurt_land");
    public static final SoundEvent ZOMBIE_SWIM = Content.sound("entity.zombie_nautilus.swim");
    public static final SoundEvent ARMOR_EQUIP = Content.sound("item.armor.equip_nautilus");
    public static final SoundEvent ARMOR_UNEQUIP = Content.sound("item.armor.unequip_nautilus");
    public static final SoundEvent SADDLE_EQUIP = Content.sound("item.nautilus_saddle_equip");
    public static final SoundEvent SADDLE_UNDERWATER_EQUIP = Content.sound("item.nautilus_saddle_underwater_equip");

    public static final Map<NautilusArmorTier, ItemNautilusArmor> ARMOR = new EnumMap<NautilusArmorTier, ItemNautilusArmor>(NautilusArmorTier.class);
    @Nullable
    public static PotionBreath breath;

    /** Natural drowned waiting for their first tick, when they may get a zombie nautilus. */
    static final Set<Entity> PENDING_RIDERS = Collections.newSetFromMap(new WeakHashMap<Entity, Boolean>());
    /** Air held by Breath of the Nautilus: a player's at the start of its tick; another mob's from its last tick. */
    private static final Map<Entity, Integer> HELD_AIR = Collections.synchronizedMap(new WeakHashMap<Entity, Integer>());

    private NautilusContent() {
    }

    public static boolean enabled() {
        return ModConfig.nautilus.enabled;
    }

    @SubscribeEvent
    public static void registerItems(RegistryEvent.Register<Item> event) {
        if (!enabled()) {
            return;
        }
        for (NautilusArmorTier tier : NautilusArmorTier.values()) {
            ARMOR.put(tier, Content.item(event.getRegistry(), tier.itemName, new ItemNautilusArmor(tier)));
        }
        LootTableList.register(EntityNautilus.LOOT);
        LootTableList.register(EntityZombieNautilus.LOOT);
    }

    @SubscribeEvent
    public static void registerEffects(RegistryEvent.Register<Potion> event) {
        if (!enabled()) {
            return;
        }
        breath = new PotionBreath();
        breath.setRegistryName(Content.id("breath_of_the_nautilus"));
        event.getRegistry().register(breath);
    }

    @SubscribeEvent
    public static void registerEntities(RegistryEvent.Register<EntityEntry> event) {
        if (!enabled()) {
            return;
        }
        event.getRegistry().register(EntityEntryBuilder.create()
                .entity(EntityNautilus.class)
                .id(Content.id("nautilus"), NAUTILUS_ID)
                .name(TrickyTrialsBackport.MOD_ID + ".nautilus")
                .tracker(160, 3, true)
                .egg(EGG_PRIMARY, EGG_SECONDARY)
                .build());
        event.getRegistry().register(EntityEntryBuilder.create()
                .entity(EntityZombieNautilus.class)
                .id(Content.id("zombie_nautilus"), ZOMBIE_NAUTILUS_ID)
                .name(TrickyTrialsBackport.MOD_ID + ".zombie_nautilus")
                .tracker(160, 3, true)
                .egg(ZOMBIE_EGG_PRIMARY, ZOMBIE_EGG_SECONDARY)
                .build());
        EntitySpawnPlacementRegistry.setPlacementType(EntityNautilus.class, EntityLiving.SpawnPlacementType.IN_WATER);
    }

    /** From the mod's init. */
    public static void init() {
        if (!enabled()) {
            return;
        }
        Guis.container(GUI_ID, (player, world, pos) -> {
            Entity e = world.getEntityByID(pos.getX());
            return e instanceof EntityAbstractNautilus ? new ContainerNautilus(player.inventory, (EntityAbstractNautilus) e) : null;
        });
    }

    /** From post-init, once every mod's biomes exist. */
    public static void postInit() {
        if (!enabled()) {
            return;
        }
        if (ModConfig.nautilus.naturalSpawn) {
            for (Biome biome : oceans()) {
                EntityRegistry.addSpawn(EntityNautilus.class, weight(biome), 1, 1, EnumCreatureType.WATER_CREATURE, biome);
            }
        }
        ItemNautilusArmor diamond = ARMOR.get(NautilusArmorTier.DIAMOND);
        ItemNautilusArmor netherite = ARMOR.get(NautilusArmorTier.NETHERITE);
        if (diamond != null && netherite != null) {
            SmithingUpgrades.add(diamond, null, new ItemStack(netherite), TrickyTrialsBackport.MOD_ID);
        }
    }

    /** Every ocean (1.12.2's three and Update Aquatic's warm, lukewarm and cold ones). */
    static List<Biome> oceans() {
        List<Biome> out = new ArrayList<Biome>(BiomeDictionary.getBiomes(BiomeDictionary.Type.OCEAN));
        return out;
    }

    /** 2 in cold and frozen oceans, 5 in the others. */
    static int weight(Biome biome) {
        return BiomeDictionary.hasType(biome, BiomeDictionary.Type.COLD) || BiomeDictionary.hasType(biome, BiomeDictionary.Type.SNOWY)
                ? COLD_WEIGHT : WEIGHT;
    }

    // ------------------------------------------------------------------------------------------
    // Loot
    // ------------------------------------------------------------------------------------------

    @SubscribeEvent
    public static void onLootLoad(LootTableLoadEvent event) {
        if (!enabled()) {
            return;
        }
        if (EntityNautilus.LOOT.equals(event.getName())) {
            Item shell = shell();
            if (shell != null) {
                event.getTable().addPool(shellPool(shell));
            }
            return;
        }
        if (!ModConfig.nautilus.chestLoot || !Owners.UPDATE_AQUATIC.equals(event.getName().getNamespace())) {
            return;
        }
        for (String chest : ARMOR_CHESTS) {
            if (("chests/" + chest).equals(event.getName().getPath())) {
                event.getTable().addPool(armorPool());
                return;
            }
        }
    }

    /** Update Aquatic's nautilus shell, or null without it. */
    @Nullable
    public static Item shell() {
        return NautilusItems.uaItem.apply("nautilus_shell");
    }

    /** 1.21.11's {@code entities/nautilus}: a shell for a player kill, 5% (6% with Looting I, +1% each level more). */
    static LootPool shellPool(Item shell) {
        LootCondition[] conditions = {new KilledByPlayer(false), new RandomChanceWithLooting(0.05F, 0.01F)};
        LootEntry entry = new LootEntryItem(shell, 1, 0, new LootFunction[0], new LootCondition[0], "nautilus_shell");
        return new LootPool(new LootEntry[] {entry}, conditions, new RandomValueRange(1), new RandomValueRange(0), "nautilus_shell");
    }

    /** 1.21.11's pool in six ocean chests: nothing 148, copper 20, iron 10, golden 5, diamond 2. */
    static LootPool armorPool() {
        List<LootEntry> entries = new ArrayList<LootEntry>();
        entries.add(new LootEntryEmpty(148, 0, new LootCondition[0], "empty"));
        int[] weights = {20, 10, 5, 2};
        NautilusArmorTier[] tiers = {NautilusArmorTier.COPPER, NautilusArmorTier.IRON, NautilusArmorTier.GOLDEN, NautilusArmorTier.DIAMOND};
        for (int i = 0; i < tiers.length; i++) {
            entries.add(new LootEntryItem(ARMOR.get(tiers[i]), weights[i], 0, new LootFunction[0], new LootCondition[0], tiers[i].itemName));
        }
        return new LootPool(entries.toArray(new LootEntry[0]), new LootCondition[0], new RandomValueRange(1), new RandomValueRange(0),
                "nautilus_armor");
    }

    // ------------------------------------------------------------------------------------------
    // Inventory while riding
    // ------------------------------------------------------------------------------------------

    /** Packet 115's work: the inventory of the nautilus the player rides. */
    static void openRidden(EntityPlayerMP player) {
        Entity vehicle = player.getRidingEntity();
        if (enabled() && vehicle instanceof EntityAbstractNautilus) {
            ((EntityAbstractNautilus) vehicle).openInventory(player);
        }
    }

    // ------------------------------------------------------------------------------------------
    // Breath of the Nautilus
    // ------------------------------------------------------------------------------------------

    /** Under water with the effect and no other way to breathe, the air is held where it was. */
    public static boolean holdsAir(EntityLivingBase e) {
        if (breath == null || !e.isPotionActive(breath) || !e.isInsideOfMaterial(Material.WATER)) {
            return false;
        }
        if (e.canBreatheUnderwater() || e.isPotionActive(MobEffects.WATER_BREATHING)) {
            return false;
        }
        Potion conduit = Owners.present(Owners.UPDATE_AQUATIC) ? Owners.effect(Owners.UPDATE_AQUATIC, "conduit_power") : null;
        return conduit == null || !e.isPotionActive(conduit);
    }

    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (!enabled()) {
            return;
        }
        EntityPlayer player = event.player;
        if (event.phase == TickEvent.Phase.START) {
            if (holdsAir(player)) {
                HELD_AIR.put(player, player.getAir());
            } else {
                HELD_AIR.remove(player);
            }
            return;
        }
        Integer held = HELD_AIR.remove(player);
        if (held != null && holdsAir(player) && player.getAir() < held) {
            player.setAir(held);
        }
    }

    @SubscribeEvent
    public static void onLivingUpdate(LivingEvent.LivingUpdateEvent event) {
        if (!enabled()) {
            return;
        }
        EntityLivingBase e = event.getEntityLiving();
        if (!(e instanceof EntityPlayer)) {
            if (holdsAir(e)) {
                Integer held = HELD_AIR.get(e);
                if (held != null && e.getAir() < held) {
                    e.setAir(held);
                }
                HELD_AIR.put(e, e.getAir());
            } else if (!HELD_AIR.isEmpty()) {
                HELD_AIR.remove(e);
            }
        }
        if (!e.world.isRemote && !PENDING_RIDERS.isEmpty() && PENDING_RIDERS.remove(e) && e instanceof EntityLiving) {
            maybeMount((EntityLiving) e);
        }
    }

    // ------------------------------------------------------------------------------------------
    // Drowned riders
    // ------------------------------------------------------------------------------------------

    public static boolean isDrowned(Entity e) {
        return DROWNED.equals(EntityList.getKey(e));
    }

    /** A drowned spawned naturally (not by a spawner) is marked; it may get its mount on its first tick. */
    @SubscribeEvent
    public static void onSpecialSpawn(LivingSpawnEvent.SpecialSpawn event) {
        if (enabled() && ModConfig.nautilus.drownedRiders && event.getSpawner() == null && !event.getWorld().isRemote
                && isDrowned(event.getEntityLiving())) {
            PENDING_RIDERS.add(event.getEntityLiving());
        }
    }

    /**
     * 1.21.11's {@code Drowned.finalizeSpawn}: a natural adult drowned holding a trident, outside rivers, rides a new
     * zombie nautilus half the time.
     *
     * @return the zombie nautilus, or null
     */
    @Nullable
    public static EntityZombieNautilus maybeMount(EntityLiving drowned) {
        if (drowned.isRiding() || drowned.isChild() || !drowned.isEntityAlive()) {
            return null;
        }
        ItemStack held = drowned.getHeldItemMainhand();
        Item trident = NautilusItems.uaItem.apply("trident");
        if (held.isEmpty() || trident == null || held.getItem() != trident) {
            return null;
        }
        if (drowned.getRNG().nextFloat() >= JOCKEY_CHANCE) {
            return null;
        }
        World world = drowned.world;
        BlockPos pos = new BlockPos(drowned);
        if (BiomeDictionary.hasType(world.getBiome(pos), BiomeDictionary.Type.RIVER)) {
            return null;
        }
        return mount(drowned);
    }

    /** Puts a rider on a fresh zombie nautilus where it stands. */
    public static EntityZombieNautilus mount(EntityLiving rider) {
        EntityZombieNautilus mount = new EntityZombieNautilus(rider.world);
        mount.setLocationAndAngles(rider.posX, rider.posY, rider.posZ, rider.rotationYaw, 0.0F);
        mount.onInitialSpawn(rider.world.getDifficultyForLocation(new BlockPos(mount)), null);
        rider.world.spawnEntity(mount);
        rider.startRiding(mount, true);
        return mount;
    }
}
