package com.exiledradio.trickytrialsbackport.mounts;

import com.exiledradio.backportcore.smithing.SmithingUpgrades;
import com.exiledradio.trickytrialsbackport.ModConfig;
import com.exiledradio.trickytrialsbackport.TrickyTrialsBackport;
import com.exiledradio.trickytrialsbackport.common.Content;
import com.exiledradio.trickytrialsbackport.common.Owners;

import net.minecraft.entity.EnumCreatureType;
import net.minecraft.entity.monster.EntitySkeleton;
import net.minecraft.init.Biomes;
import net.minecraft.init.Items;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.biome.Biome;
import net.minecraft.world.storage.loot.LootTableList;
import net.minecraftforge.event.RegistryEvent;
import net.minecraftforge.event.entity.living.LivingSpawnEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.Event;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.registry.EntityEntry;
import net.minecraftforge.fml.common.registry.EntityEntryBuilder;
import net.minecraftforge.fml.common.registry.EntityRegistry;

/**
 * Stream 24, mounts (1.21.11, "Mounts of Mayhem"): the parched ({@code parched}, entity id 65), netherite horse armor,
 * 1.21.11's zombie horse ({@link ZombieHorses}, patch P24a), floating while ridden and mounted reach ({@link Riding}),
 * and, with Trails &amp; Tales' camels, the camel husk (id 66).
 *
 * <p>Stream 22's spears are looked up by name ({@code trickytrialsbackport:iron_spear}); without them the riders keep
 * the weapon their own spawn gave them.
 */
@Mod.EventBusSubscriber(modid = TrickyTrialsBackport.MOD_ID)
public final class MountsContent {

    public static final int PARCHED_ID = 65;
    public static final int CAMEL_HUSK_ID = 66;
    /** 1.21.11's spawn egg is a texture; its two commonest colors. */
    public static final int PARCHED_EGG_PRIMARY = 0x746E66;
    public static final int PARCHED_EGG_SECONDARY = 0xAC986B;

    static final int PARCHED_WEIGHT = 50;
    static final int DESERT_SKELETON_WEIGHT = 50;

    static final String[] SOUNDS = {"entity.parched.ambient", "entity.parched.death", "entity.parched.hurt", "entity.parched.step"};

    public static Item netheriteHorseArmor;

    /** How other streams' items are found: {@link Owners#ours}. The self-test swaps it to stand in for stream 22. */
    public static java.util.function.Function<String, Item> lookup = Owners::ours;

    private MountsContent() {
    }

    public static boolean enabled() {
        return ModConfig.mounts.enabled;
    }

    /** @return a new iron spear from stream 22, or empty when that stream is absent or off */
    public static ItemStack ironSpear() {
        Item spear = lookup.apply("iron_spear");
        return spear == null ? ItemStack.EMPTY : new ItemStack(spear);
    }

    @SubscribeEvent
    public static void registerItems(RegistryEvent.Register<Item> event) {
        if (!enabled()) {
            return;
        }
        for (String sound : SOUNDS) {
            Content.sound(sound);
        }
        LootTableList.register(EntityParched.LOOT);
        if (ModConfig.mounts.netheriteHorseArmor) {
            netheriteHorseArmor = Content.item(event.getRegistry(), "netherite_horse_armor", HorseArmor.create());
        }
    }

    @SubscribeEvent
    public static void registerEntities(RegistryEvent.Register<EntityEntry> event) {
        if (!enabled()) {
            return;
        }
        event.getRegistry().register(EntityEntryBuilder.create()
                .entity(EntityParched.class)
                .id(Content.id("parched"), PARCHED_ID)
                .name(TrickyTrialsBackport.MOD_ID + ".parched")
                .tracker(80, 3, true)
                .egg(PARCHED_EGG_PRIMARY, PARCHED_EGG_SECONDARY)
                .build());
    }

    /** From post-init, once every mod's biomes and items exist. */
    public static void postInit() {
        if (!enabled()) {
            return;
        }
        if (ModConfig.mounts.parchedSpawn) {
            Biome[] deserts = deserts();
            EntityRegistry.addSpawn(EntityParched.class, PARCHED_WEIGHT, 4, 4, EnumCreatureType.MONSTER, deserts);
            for (Biome biome : deserts) {
                for (Biome.SpawnListEntry entry : biome.getSpawnableList(EnumCreatureType.MONSTER)) {
                    if (entry.entityClass == EntitySkeleton.class && entry.itemWeight > DESERT_SKELETON_WEIGHT) {
                        entry.itemWeight = DESERT_SKELETON_WEIGHT;
                    }
                }
            }
        }
        ZombieHorses.postInit();
        if (netheriteHorseArmor != null) {
            SmithingUpgrades.add(Items.DIAMOND_HORSE_ARMOR, null, new ItemStack(netheriteHorseArmor), TrickyTrialsBackport.MOD_ID);
        }
    }

    /** 1.21.11's desert, in 1.12.2's three: desert, desert hills and the desert M. */
    static Biome[] deserts() {
        return new Biome[] {Biomes.DESERT, Biomes.DESERT_HILLS, Biomes.MUTATED_DESERT};
    }

    /** 1.21.11's {@code Monster.checkSurfaceMonstersSpawnRules}: a natural parched needs the sky. */
    @SubscribeEvent
    public static void onCheckSpawn(LivingSpawnEvent.CheckSpawn event) {
        if (event.getEntityLiving() instanceof EntityParched && !event.isSpawner() && enabled()
                && !event.getWorld().canSeeSky(new BlockPos(event.getX(), event.getY(), event.getZ()))) {
            event.setResult(Event.Result.DENY);
        }
    }
}
