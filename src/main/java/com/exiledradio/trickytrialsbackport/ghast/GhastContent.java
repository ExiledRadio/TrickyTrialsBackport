package com.exiledradio.trickytrialsbackport.ghast;

import com.exiledradio.backportcore.waterlog.Waterlogging;
import com.exiledradio.trickytrialsbackport.ModConfig;
import com.exiledradio.trickytrialsbackport.TrickyTrialsBackport;
import com.exiledradio.trickytrialsbackport.common.Content;
import com.exiledradio.trickytrialsbackport.common.ItemTrigger;

import net.minecraft.block.Block;
import net.minecraft.block.SoundType;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.init.Blocks;
import net.minecraft.init.Items;
import net.minecraft.item.EnumDyeColor;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.crafting.IRecipe;
import net.minecraft.item.crafting.Ingredient;
import net.minecraft.network.NetHandlerPlayServer;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.SoundEvent;
import net.minecraft.world.storage.loot.LootTableList;
import net.minecraftforge.event.RegistryEvent;
import net.minecraftforge.event.world.BlockEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import net.minecraftforge.fml.common.registry.EntityEntry;
import net.minecraftforge.fml.common.registry.EntityEntryBuilder;
import net.minecraftforge.fml.relauncher.ReflectionHelper;
import net.minecraftforge.oredict.OreIngredient;
import net.minecraftforge.oredict.ShapedOreRecipe;
import net.minecraftforge.oredict.ShapelessOreRecipe;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Stream 17: the happy ghast (1.21.6). The dried ghast block, the happy ghast and its ghastling ({@code happy_ghast},
 * entity id 45; 46-49 unused), the sixteen harnesses with their crafting and dyeing, the sounds, and the advancement
 * <i>Stay Hydrated!</i>. No packets (85-89 unused): a rider's client steers, as 1.12.2's horses and boats do.
 */
@Mod.EventBusSubscriber(modid = TrickyTrialsBackport.MOD_ID)
public final class GhastContent {

    public static final int HAPPY_GHAST_ID = 45;
    /** Spawn egg colors, the two commonest of 1.21.6's egg texture (its white body and gray face). */
    public static final int EGG_PRIMARY = 0xF0F0F0;
    public static final int EGG_SECONDARY = 0x736D6D;

    public static final SoundEvent AMBIENT = Content.sound("entity.happy_ghast.ambient");
    public static final SoundEvent DEATH = Content.sound("entity.happy_ghast.death");
    public static final SoundEvent HURT = Content.sound("entity.happy_ghast.hurt");
    public static final SoundEvent RIDING = Content.sound("entity.happy_ghast.riding");
    public static final SoundEvent HARNESS_EQUIP = Content.sound("entity.happy_ghast.equip");
    public static final SoundEvent HARNESS_UNEQUIP = Content.sound("entity.happy_ghast.unequip");
    public static final SoundEvent GOGGLES_DOWN = Content.sound("entity.happy_ghast.harness_goggles_down");
    public static final SoundEvent GOGGLES_UP = Content.sound("entity.happy_ghast.harness_goggles_up");
    public static final SoundEvent GHASTLING_AMBIENT = Content.sound("entity.ghastling.ambient");
    public static final SoundEvent GHASTLING_DEATH = Content.sound("entity.ghastling.death");
    public static final SoundEvent GHASTLING_HURT = Content.sound("entity.ghastling.hurt");
    public static final SoundEvent GHASTLING_SPAWN = Content.sound("entity.ghastling.spawn");
    public static final SoundEvent DRIED_AMBIENT = Content.sound("block.dried_ghast.ambient");
    public static final SoundEvent DRIED_AMBIENT_WATER = Content.sound("block.dried_ghast.ambient_water");
    public static final SoundEvent DRIED_PLACE = Content.sound("block.dried_ghast.place");
    public static final SoundEvent DRIED_PLACE_IN_WATER = Content.sound("block.dried_ghast.place_in_water");
    public static final SoundEvent DRIED_TRANSITION = Content.sound("block.dried_ghast.transition");

    /**
     * 1.21's {@code SoundType.DRIED_GHAST}: its break, step and fall; no place or hit sound (the block plays its own
     * place sound, dry or in water).
     */
    static final SoundType DRIED_GHAST_SOUND = new SoundType(1.0F, 1.0F, Content.sound("block.dried_ghast.break"),
            Content.sound("block.dried_ghast.step"), Content.sound("intentionally_empty"), Content.sound("intentionally_empty"),
            Content.sound("block.dried_ghast.fall"));

    /** 1.21.6's <i>Stay Hydrated!</i>: a dried ghast placed into water. */
    public static final ItemTrigger PLACED_IN_WATER = new ItemTrigger(Content.id("dried_ghast_placed_in_water"));

    public static BlockDriedGhast driedGhast;
    /** color name (white, orange, ...) -> harness */
    public static final Map<String, ItemHarness> HARNESSES = new LinkedHashMap<String, ItemHarness>();

    private static final Field VEHICLE_FLOATING = ReflectionHelper.findField(NetHandlerPlayServer.class, "vehicleFloating", "field_184345_D");
    private static final Field VEHICLE_FLOATING_TICKS = ReflectionHelper.findField(NetHandlerPlayServer.class, "vehicleFloatingTickCount", "field_184346_E");

    private GhastContent() {
    }

    public static boolean enabled() {
        return ModConfig.happyGhast.enabled;
    }

    /** From the mod's pre-init. */
    public static void preInit() {
        if (enabled()) {
            PLACED_IN_WATER.register();
        }
    }

    /** 1.21's color names (light_blue, light_gray), not 1.12.2's (lightBlue, silver). */
    public static String colorName(EnumDyeColor color) {
        switch (color) {
            case LIGHT_BLUE:
                return "light_blue";
            case SILVER:
                return "light_gray";
            default:
                return color.getName().toLowerCase(Locale.ROOT);
        }
    }

    static String dyeOre(EnumDyeColor color) {
        StringBuilder b = new StringBuilder("dye");
        for (String part : colorName(color).split("_")) {
            b.append(Character.toUpperCase(part.charAt(0))).append(part.substring(1));
        }
        return b.toString();
    }

    @SubscribeEvent
    public static void registerBlocks(RegistryEvent.Register<Block> event) {
        if (enabled()) {
            driedGhast = Content.block(event.getRegistry(), "dried_ghast", new BlockDriedGhast(DRIED_GHAST_SOUND), null);
        }
    }

    @SubscribeEvent
    public static void registerItems(RegistryEvent.Register<Item> event) {
        if (!enabled()) {
            return;
        }
        Content.registerBlockItem(event.getRegistry(), driedGhast);
        for (EnumDyeColor color : EnumDyeColor.values()) {
            String name = colorName(color) + "_harness";
            HARNESSES.put(colorName(color), Content.item(event.getRegistry(), name, new ItemHarness(color)));
        }
        LootTableList.register(EntityHappyGhast.LOOT);
    }

    @SubscribeEvent
    public static void registerRecipes(RegistryEvent.Register<IRecipe> event) {
        if (!enabled()) {
            return;
        }
        ShapedOreRecipe dried = new ShapedOreRecipe(Content.id("dry_ghast"), new ItemStack(driedGhast), "###", "#X#", "###",
                '#', Items.GHAST_TEAR, 'X', Blocks.SOUL_SAND);
        dried.setRegistryName(Content.id("dried_ghast"));
        event.getRegistry().register(dried);
        ResourceLocation group = Content.id("harness");
        ResourceLocation dyeGroup = Content.id("harness_dye");
        for (EnumDyeColor color : EnumDyeColor.values()) {
            ItemHarness harness = HARNESSES.get(colorName(color));
            String name = colorName(color) + "_harness";
            ShapedOreRecipe craft = new ShapedOreRecipe(group, new ItemStack(harness), "LLL", "G#G", 'L', Items.LEATHER,
                    'G', Blocks.GLASS, '#', new ItemStack(Blocks.WOOL, 1, color.getMetadata()));
            craft.setRegistryName(Content.id(name));
            event.getRegistry().register(craft);
            // 1.21.6's dye_<color>_harness: a dye and any harness of another color.
            List<Item> others = new ArrayList<Item>();
            for (ItemHarness h : HARNESSES.values()) {
                if (h != harness) {
                    others.add(h);
                }
            }
            ShapelessOreRecipe dye = new ShapelessOreRecipe(dyeGroup, new ItemStack(harness), new OreIngredient(dyeOre(color)),
                    Ingredient.fromItems(others.toArray(new Item[0])));
            dye.setRegistryName(Content.id("dye_" + name));
            event.getRegistry().register(dye);
        }
    }

    @SubscribeEvent
    public static void registerEntities(RegistryEvent.Register<EntityEntry> event) {
        if (!enabled()) {
            return;
        }
        event.getRegistry().register(EntityEntryBuilder.create()
                .entity(EntityHappyGhast.class)
                .id(Content.id("happy_ghast"), HAPPY_GHAST_ID)
                .name(TrickyTrialsBackport.MOD_ID + ".happy_ghast")
                .tracker(160, 3, true)
                .egg(EGG_PRIMARY, EGG_SECONDARY)
                .build());
    }

    /** <i>Stay Hydrated!</i>: a player places a dried ghast that holds water. */
    @SubscribeEvent
    public static void onPlace(BlockEvent.PlaceEvent event) {
        if (!enabled() || !(event.getPlayer() instanceof EntityPlayerMP) || event.getPlacedBlock().getBlock() != driedGhast) {
            return;
        }
        if (Waterlogging.hasWater(event.getWorld(), event.getPos())) {
            PLACED_IN_WATER.trigger((EntityPlayerMP) event.getPlayer(), new ItemStack(driedGhast));
        }
    }

    /**
     * 1.12.2 kicks a player whose vehicle floats for 80 ticks while flight is off; 1.21 exempts flying vehicles
     * ({@code isFlyingVehicle}), the adult happy ghast among them. The count is cleared each tick for its riders,
     * after their movement packets and before the network tick that would kick them.
     */
    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END || !(event.player instanceof EntityPlayerMP) || !enabled()) {
            return;
        }
        EntityPlayerMP player = (EntityPlayerMP) event.player;
        Entity root = player.getLowestRidingEntity();
        if (root instanceof EntityHappyGhast && !((EntityHappyGhast) root).isChild() && player.connection != null) {
            clearFloating(player.connection);
        }
    }

    static void clearFloating(NetHandlerPlayServer connection) {
        try {
            VEHICLE_FLOATING.setBoolean(connection, false);
            VEHICLE_FLOATING_TICKS.setInt(connection, 0);
        } catch (IllegalAccessException e) {
            TrickyTrialsBackport.LOGGER.error("vehicleFloating", e);
        }
    }

    static int floatingTicks(NetHandlerPlayServer connection) throws IllegalAccessException {
        return VEHICLE_FLOATING_TICKS.getInt(connection);
    }

    static void setFloating(NetHandlerPlayServer connection, boolean floating, int ticks) throws IllegalAccessException {
        VEHICLE_FLOATING.setBoolean(connection, floating);
        VEHICLE_FLOATING_TICKS.setInt(connection, ticks);
    }
}
