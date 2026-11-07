package com.exiledradio.trickytrialsbackport.breeze;

import com.exiledradio.trickytrialsbackport.ModConfig;
import com.exiledradio.trickytrialsbackport.TrickyTrialsBackport;
import com.exiledradio.trickytrialsbackport.common.Content;

import net.minecraft.block.BlockDispenser;
import net.minecraft.dispenser.BehaviorProjectileDispense;
import net.minecraft.dispenser.IPosition;
import net.minecraft.entity.IProjectile;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.crafting.IRecipe;
import net.minecraft.world.World;
import net.minecraft.world.storage.loot.LootTableList;
import net.minecraftforge.event.RegistryEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.registry.EntityEntry;
import net.minecraftforge.fml.common.registry.EntityEntryBuilder;
import net.minecraftforge.oredict.ShapelessOreRecipe;

/**
 * Stream 3: the breeze, the breeze rod and wind charges.
 *
 * <ul>
 *   <li>{@link EntityBreeze} ({@code breeze}, entity id 1), with a spawn egg; it spawns only where something
 *       places it (trial spawners, stream 7).</li>
 *   <li>{@link EntityWindCharge} ({@code wind_charge}, 2) and {@link EntityBreezeWindCharge}
 *       ({@code breeze_wind_charge}, 3); the burst itself is {@code common/WindBurst}.</li>
 *   <li>{@code breeze_rod} (four wind charges each; the mace's handle and repair item and a brewing
 *       ingredient in later streams, found by name) and {@link ItemWindCharge}, which dispensers shoot.</li>
 *   <li>Advancements <i>Blowback</i> and <i>Who Needs Rockets?</i> (JSON).</li>
 * </ul>
 * Off ({@code breeze.enabled}), nothing is registered.
 */
@Mod.EventBusSubscriber(modid = TrickyTrialsBackport.MOD_ID)
public final class BreezeContent {

    public static final int BREEZE_ID = 1;
    public static final int WIND_CHARGE_ID = 2;
    public static final int BREEZE_WIND_CHARGE_ID = 3;

    /** 1.21's spawn egg colors. */
    public static final int EGG_PRIMARY = 0xAF94DF;
    public static final int EGG_SECONDARY = 0x9166DF;

    /** Every sound this stream plays; made before the sound registry event so all are registered. */
    static final String[] SOUNDS = {
            "entity.breeze.charge", "entity.breeze.death", "entity.breeze.deflect", "entity.breeze.hurt",
            "entity.breeze.idle_air", "entity.breeze.idle_ground", "entity.breeze.inhale", "entity.breeze.jump",
            "entity.breeze.land", "entity.breeze.shoot", "entity.breeze.slide", "entity.breeze.whirl",
            "entity.breeze.wind_burst", "entity.wind_charge.throw", "entity.wind_charge.wind_burst"};

    public static Item breezeRod;
    public static ItemWindCharge windCharge;

    private BreezeContent() {
    }

    public static boolean enabled() {
        return ModConfig.breeze.enabled;
    }

    @SubscribeEvent
    public static void registerItems(RegistryEvent.Register<Item> event) {
        if (!enabled()) {
            return;
        }
        for (String sound : SOUNDS) {
            Content.sound(sound);
        }
        breezeRod = Content.item(event.getRegistry(), "breeze_rod", new Item());
        windCharge = Content.item(event.getRegistry(), "wind_charge", new ItemWindCharge());
        LootTableList.register(EntityBreeze.LOOT);
        BlockDispenser.DISPENSE_BEHAVIOR_REGISTRY.putObject(windCharge, new BehaviorProjectileDispense() {
            @Override
            protected IProjectile getProjectileEntity(World world, IPosition position, ItemStack stack) {
                return new EntityWindCharge(world, position.getX(), position.getY(), position.getZ());
            }

            @Override
            protected float getProjectileInaccuracy() {
                return 6.6666665F;
            }

            @Override
            protected float getProjectileVelocity() {
                return 1.0F;
            }
        });
    }

    @SubscribeEvent
    public static void registerEntities(RegistryEvent.Register<EntityEntry> event) {
        if (!enabled()) {
            return;
        }
        event.getRegistry().register(EntityEntryBuilder.create()
                .entity(EntityBreeze.class)
                .id(Content.id("breeze"), BREEZE_ID)
                .name(TrickyTrialsBackport.MOD_ID + ".breeze")
                .tracker(80, 3, true)
                .egg(EGG_PRIMARY, EGG_SECONDARY)
                .build());
        event.getRegistry().register(EntityEntryBuilder.create()
                .entity(EntityWindCharge.class)
                .id(Content.id("wind_charge"), WIND_CHARGE_ID)
                .name(TrickyTrialsBackport.MOD_ID + ".wind_charge")
                .tracker(64, 10, true)
                .build());
        event.getRegistry().register(EntityEntryBuilder.create()
                .entity(EntityBreezeWindCharge.class)
                .id(Content.id("breeze_wind_charge"), BREEZE_WIND_CHARGE_ID)
                .name(TrickyTrialsBackport.MOD_ID + ".breeze_wind_charge")
                .tracker(64, 10, true)
                .build());
    }

    @SubscribeEvent
    public static void registerRecipes(RegistryEvent.Register<IRecipe> event) {
        if (!enabled()) {
            return;
        }
        ShapelessOreRecipe r = new ShapelessOreRecipe(null, new ItemStack(windCharge, 4), breezeRod);
        r.setRegistryName(Content.id("wind_charge"));
        event.getRegistry().register(r);
    }
}
