package com.exiledradio.trickytrialsbackport.bogged;

import com.exiledradio.trickytrialsbackport.ModConfig;
import com.exiledradio.trickytrialsbackport.TrickyTrialsBackport;
import com.exiledradio.trickytrialsbackport.common.Content;
import com.exiledradio.trickytrialsbackport.common.Owners;

import net.minecraft.entity.EnumCreatureType;
import net.minecraft.entity.monster.EntitySkeleton;
import net.minecraft.init.Biomes;
import net.minecraft.item.Item;
import net.minecraft.util.ResourceLocation;
import net.minecraft.world.biome.Biome;
import net.minecraft.world.storage.loot.LootTableList;
import net.minecraftforge.event.RegistryEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.registry.EntityEntry;
import net.minecraftforge.fml.common.registry.EntityEntryBuilder;
import net.minecraftforge.fml.common.registry.EntityRegistry;
import net.minecraftforge.fml.common.registry.ForgeRegistries;

import java.util.ArrayList;
import java.util.List;

/**
 * Stream 4: the bogged ({@code bogged}, entity id 10), with its spawn egg and loot table.
 *
 * <p>Spawning (1.21's swamp and mangrove swamp lists): bogged in groups of 4 at weight 30, and the
 * skeleton's weight there lowered from 100 to 70. Vanilla's two swamps, and Wild Update's mangrove swamp
 * when it is installed. Behind {@code bogged.spawn}.
 */
@Mod.EventBusSubscriber(modid = TrickyTrialsBackport.MOD_ID)
public final class BoggedContent {

    public static final int BOGGED_ID = 10;
    /** 1.21's spawn egg colors. */
    public static final int EGG_PRIMARY = 0x8A9C72;
    public static final int EGG_SECONDARY = 0x314D1B;

    static final int WEIGHT = 30;
    static final int SKELETON_WEIGHT = 70;

    static final String[] SOUNDS = {"entity.bogged.ambient", "entity.bogged.death", "entity.bogged.hurt", "entity.bogged.shear",
            "entity.bogged.step"};

    private BoggedContent() {
    }

    public static boolean enabled() {
        return ModConfig.bogged.enabled;
    }

    @SubscribeEvent
    public static void registerItems(RegistryEvent.Register<Item> event) {
        if (!enabled()) {
            return;
        }
        // Before the sound registry event, so every event this stream plays is registered.
        for (String sound : SOUNDS) {
            Content.sound(sound);
        }
        LootTableList.register(EntityBogged.LOOT);
    }

    @SubscribeEvent
    public static void registerEntities(RegistryEvent.Register<EntityEntry> event) {
        if (!enabled()) {
            return;
        }
        event.getRegistry().register(EntityEntryBuilder.create()
                .entity(EntityBogged.class)
                .id(Content.id("bogged"), BOGGED_ID)
                .name(TrickyTrialsBackport.MOD_ID + ".bogged")
                .tracker(80, 3, true)
                .egg(EGG_PRIMARY, EGG_SECONDARY)
                .build());
    }

    /** From post-init, once every mod's biomes exist. */
    public static void postInit() {
        if (!enabled() || !ModConfig.bogged.spawn) {
            return;
        }
        Biome[] swamps = swamps().toArray(new Biome[0]);
        EntityRegistry.addSpawn(EntityBogged.class, WEIGHT, 4, 4, EnumCreatureType.MONSTER, swamps);
        for (Biome biome : swamps) {
            for (Biome.SpawnListEntry entry : biome.getSpawnableList(EnumCreatureType.MONSTER)) {
                if (entry.entityClass == EntitySkeleton.class && entry.itemWeight > SKELETON_WEIGHT) {
                    entry.itemWeight = SKELETON_WEIGHT;
                }
            }
        }
    }

    /** Vanilla's swamp and swamp hills, and Wild Update's mangrove swamp when it is installed. */
    public static List<Biome> swamps() {
        List<Biome> out = new ArrayList<Biome>();
        out.add(Biomes.SWAMPLAND);
        out.add(Biomes.MUTATED_SWAMPLAND);
        if (Owners.present(Owners.WILD_UPDATE)) {
            Biome mangrove = ForgeRegistries.BIOMES.getValue(new ResourceLocation(Owners.WILD_UPDATE, "mangrove_swamp"));
            if (mangrove != null) {
                out.add(mangrove);
            }
        }
        return out;
    }
}
