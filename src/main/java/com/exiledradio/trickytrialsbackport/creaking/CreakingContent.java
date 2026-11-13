package com.exiledradio.trickytrialsbackport.creaking;

import com.exiledradio.trickytrialsbackport.ModConfig;
import com.exiledradio.trickytrialsbackport.TrickyTrialsBackport;
import com.exiledradio.trickytrialsbackport.common.Content;
import com.exiledradio.trickytrialsbackport.common.ItemTrigger;
import com.exiledradio.trickytrialsbackport.palegarden.BlockCreakingHeart;
import com.exiledradio.trickytrialsbackport.palegarden.PaleGardenContent;

import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.SoundEvent;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraft.world.storage.loot.LootTableList;
import net.minecraftforge.event.RegistryEvent;
import net.minecraftforge.event.world.BlockEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.registry.EntityEntry;
import net.minecraftforge.fml.common.registry.EntityEntryBuilder;

/**
 * Stream 13: the creaking ({@code creaking}, entity id 35), its spawn egg, sounds, loot table and the
 * advancement <i>Heart Transplanter</i>. The heart it belongs to is stream 12's; the creaking plugs into it
 * through {@link CreakingHeartHook}. Packet 65 ({@link CreakingFxMessage}) carries its particles.
 */
@Mod.EventBusSubscriber(modid = TrickyTrialsBackport.MOD_ID)
public final class CreakingContent {

    public static final int CREAKING_ID = 35;
    /** 1.21.4's spawn egg colors (the creaking's gray and orange). */
    public static final int EGG_PRIMARY = EntityCreaking.CREAKING_GRAY;
    public static final int EGG_SECONDARY = EntityCreaking.CREAKING_ORANGE;

    public static final SoundEvent ACTIVATE = Content.sound("entity.creaking.activate");
    public static final SoundEvent AMBIENT = Content.sound("entity.creaking.ambient");
    public static final SoundEvent ATTACK = Content.sound("entity.creaking.attack");
    public static final SoundEvent DEACTIVATE = Content.sound("entity.creaking.deactivate");
    public static final SoundEvent DEATH = Content.sound("entity.creaking.death");
    public static final SoundEvent FREEZE = Content.sound("entity.creaking.freeze");
    public static final SoundEvent SPAWN = Content.sound("entity.creaking.spawn");
    public static final SoundEvent STEP = Content.sound("entity.creaking.step");
    public static final SoundEvent SWAY = Content.sound("entity.creaking.sway");
    public static final SoundEvent TWITCH = Content.sound("entity.creaking.twitch");
    public static final SoundEvent UNFREEZE = Content.sound("entity.creaking.unfreeze");
    public static final SoundEvent HEART_HURT = Content.sound("block.creaking_heart.hurt");
    public static final SoundEvent HEART_SPAWN = Content.sound("block.creaking_heart.spawn");

    /** 1.21.6's <i>Heart Transplanter</i>: placing a heart that wakes or sleeps between its logs, or the log that completes one. */
    public static final ItemTrigger HEART_TRANSPLANTED = new ItemTrigger(Content.id("heart_transplanted"));

    private CreakingContent() {
    }

    public static boolean enabled() {
        return ModConfig.creaking.enabled;
    }

    /** From the mod's pre-init. */
    public static void preInit() {
        if (enabled()) {
            HEART_TRANSPLANTED.register();
        }
    }

    @SubscribeEvent
    public static void registerItems(RegistryEvent.Register<Item> event) {
        if (enabled()) {
            LootTableList.register(EntityCreaking.LOOT);
        }
    }

    @SubscribeEvent
    public static void registerEntities(RegistryEvent.Register<EntityEntry> event) {
        if (!enabled()) {
            return;
        }
        event.getRegistry().register(EntityEntryBuilder.create()
                .entity(EntityCreaking.class)
                .id(Content.id("creaking"), CREAKING_ID)
                .name(TrickyTrialsBackport.MOD_ID + ".creaking")
                .tracker(80, 3, true)
                .egg(EGG_PRIMARY, EGG_SECONDARY)
                .build());
    }

    @SubscribeEvent
    public static void onPlace(BlockEvent.PlaceEvent event) {
        if (!enabled() || !(event.getPlayer() instanceof EntityPlayerMP) || PaleGardenContent.creakingHeart == null) {
            return;
        }
        if (transplants(event.getWorld(), event.getPos(), event.getPlacedBlock())) {
            HEART_TRANSPLANTED.trigger((EntityPlayerMP) event.getPlayer(), new ItemStack(PaleGardenContent.creakingHeart));
        }
    }

    /**
     * The advancement's three criteria: a heart placed awake or dormant, or a pale oak log placed in line with a
     * heart of the same axis that has another such log on its far side.
     */
    public static boolean transplants(World world, BlockPos pos, IBlockState placed) {
        if (placed.getBlock() instanceof BlockCreakingHeart) {
            return placed.getValue(BlockCreakingHeart.STATE) != BlockCreakingHeart.State.UPROOTED;
        }
        if (!BlockCreakingHeart.isPaleOakLog(placed) || !placed.getPropertyKeys().contains(BlockCreakingHeart.AXIS)) {
            return false;
        }
        EnumFacing.Axis axis = placed.getValue(BlockCreakingHeart.AXIS);
        for (EnumFacing.AxisDirection dir : EnumFacing.AxisDirection.values()) {
            EnumFacing face = EnumFacing.getFacingFromAxis(dir, axis);
            IBlockState heart = world.getBlockState(pos.offset(face));
            IBlockState far = world.getBlockState(pos.offset(face, 2));
            if (heart.getBlock() instanceof BlockCreakingHeart && heart.getValue(BlockCreakingHeart.AXIS) == axis
                    && BlockCreakingHeart.isPaleOakLog(far) && far.getPropertyKeys().contains(BlockCreakingHeart.AXIS)
                    && far.getValue(BlockCreakingHeart.AXIS) == axis) {
                return true;
            }
        }
        return false;
    }
}
