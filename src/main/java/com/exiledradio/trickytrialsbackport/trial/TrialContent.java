package com.exiledradio.trickytrialsbackport.trial;

import com.exiledradio.backportcore.ownership.ItemOwnership;
import com.exiledradio.trickytrialsbackport.ModConfig;
import com.exiledradio.trickytrialsbackport.TrickyTrialsBackport;
import com.exiledradio.trickytrialsbackport.common.Content;
import com.exiledradio.trickytrialsbackport.common.ItemTrigger;
import com.exiledradio.trickytrialsbackport.common.Owners;

import net.minecraft.block.Block;
import net.minecraft.item.Item;
import net.minecraft.potion.Potion;
import net.minecraft.util.SoundEvent;
import net.minecraftforge.event.RegistryEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.registry.EntityEntry;
import net.minecraftforge.fml.common.registry.EntityEntryBuilder;
import net.minecraftforge.fml.common.registry.GameRegistry;

/**
 * Stream 7: trial spawners, vaults, trial keys, Trial Omen and ominous item spawners (1.21).
 *
 * <ul>
 *   <li>{@code trial_spawner} ({@link BlockTrialSpawner}, {@link TileEntityTrialSpawner}) and {@code vault}
 *       ({@link BlockVault}, {@link TileEntityVault}), with their items;</li>
 *   <li>{@code trial_key} and {@code ominous_trial_key};</li>
 *   <li>the effect {@code trial_omen};</li>
 *   <li>{@code ominous_item_spawner} ({@link EntityOminousItemSpawner}, entity id 15);</li>
 *   <li>{@code ominous_bottle} (metadata 0-4 = Bad Omen I-V), 1.21's, which this mod owns since 1.0.2
 *       (Village &amp; Pillage 2.7.4 drops it from captains when it is here, and renames its older bottles to it);</li>
 *   <li>without Village &amp; Pillage, a copy of its {@code bad_omen} effect, which it remaps when added to the world.</li>
 * </ul>
 * Packet id 35 ({@link TrialMessage}). Spawner configs and loot tables are vanilla's own files
 * ({@link TrialData}).
 */
@Mod.EventBusSubscriber(modid = TrickyTrialsBackport.MOD_ID)
public final class TrialContent {

    public static final int OMINOUS_ITEM_SPAWNER_ID = 15;

    /** 1.21's Trial Omen and Bad Omen colors. */
    private static final int TRIAL_OMEN_COLOR = 0x16A6A6;
    private static final int BAD_OMEN_COLOR = 0x0B6138;

    /** A key used on an active vault, normal or ominous (1.21's {@code item_used_on_block} there). */
    public static final ItemTrigger VAULT_USED = new ItemTrigger(Content.id("vault_used"));
    public static final ItemTrigger OMINOUS_VAULT_USED = new ItemTrigger(Content.id("ominous_vault_used"));

    public static Block trialSpawner;
    public static Block vault;
    public static Item trialKey;
    public static Item ominousTrialKey;
    public static Potion trialOmen;
    /** Our copy of Bad Omen, null when Village &amp; Pillage provides it. */
    public static Potion badOmen;
    /** 1.21's ominous bottle, ours whenever trials are on. */
    public static Item ominousBottle;

    private TrialContent() {
    }

    public static boolean enabled() {
        return ModConfig.trials.enabled;
    }

    /** A copy of Village &amp; Pillage's Bad Omen, only without it. */
    public static boolean copiesOmens() {
        return !Owners.present(Owners.VILLAGE_AND_PILLAGE);
    }

    static SoundEvent sound(String name) {
        return Content.sound(name);
    }

    @SubscribeEvent
    public static void registerBlocks(RegistryEvent.Register<Block> event) {
        if (!enabled()) {
            return;
        }
        trialSpawner = Content.block(event.getRegistry(), "trial_spawner", new BlockTrialSpawner(Content.soundType("trial_spawner", 1.0F, 1.0F)), "pickaxe");
        vault = Content.block(event.getRegistry(), "vault", new BlockVault(Content.soundType("vault", 1.0F, 1.0F)), "pickaxe");
        GameRegistry.registerTileEntity(TileEntityTrialSpawner.class, Content.id("trial_spawner"));
        GameRegistry.registerTileEntity(TileEntityVault.class, Content.id("vault"));
    }

    @SubscribeEvent
    public static void registerItems(RegistryEvent.Register<Item> event) {
        // Backport Core 1.4.5 lists the ominous bottle as ours; others ask whether we register it.
        ItemOwnership.provides(TrialContent::enabled, "ominous_bottle");
        if (!enabled()) {
            return;
        }
        Content.registerBlockItem(event.getRegistry(), trialSpawner);
        Content.registerBlockItem(event.getRegistry(), vault);
        trialKey = Content.item(event.getRegistry(), "trial_key", new Item());
        ominousTrialKey = Content.item(event.getRegistry(), "ominous_trial_key", new Item());
        sound("event.mob_effect.trial_omen");
        ominousBottle = Content.item(event.getRegistry(), "ominous_bottle", new ItemOminousBottle());
        for (int level = 1; level < ItemOminousBottle.LEVELS; level++) {
            Content.model(ominousBottle, level, "ominous_bottle");
        }
        sound("item.ominous_bottle.dispose");
        if (copiesOmens()) {
            sound("event.mob_effect.bad_omen");
        }
        for (String e : new String[] {"about_to_spawn_item", "ambient", "ambient_ominous", "close_shutter", "detect_player", "eject_item",
                "ominous_activate", "open_shutter", "spawn_item", "spawn_item_begin", "spawn_mob"}) {
            sound("block.trial_spawner." + e);
        }
        for (String e : new String[] {"activate", "ambient", "close_shutter", "deactivate", "eject_item", "insert_item", "insert_item_fail",
                "open_shutter", "reject_rewarded_player"}) {
            sound("block.vault." + e);
        }
        VAULT_USED.register();
        OMINOUS_VAULT_USED.register();
    }

    @SubscribeEvent
    public static void registerPotions(RegistryEvent.Register<Potion> event) {
        if (!enabled()) {
            return;
        }
        trialOmen = new TrialEffect("trial_omen", TRIAL_OMEN_COLOR);
        event.getRegistry().register(trialOmen);
        if (copiesOmens()) {
            badOmen = new TrialEffect("bad_omen", BAD_OMEN_COLOR);
            event.getRegistry().register(badOmen);
        }
    }

    @SubscribeEvent
    public static void registerEntities(RegistryEvent.Register<EntityEntry> event) {
        if (!enabled()) {
            return;
        }
        event.getRegistry().register(EntityEntryBuilder.create()
                .entity(EntityOminousItemSpawner.class)
                .id(Content.id("ominous_item_spawner"), OMINOUS_ITEM_SPAWNER_ID)
                .name(TrickyTrialsBackport.MOD_ID + ".ominous_item_spawner")
                .tracker(128, 20, false)
                .build());
    }
}
