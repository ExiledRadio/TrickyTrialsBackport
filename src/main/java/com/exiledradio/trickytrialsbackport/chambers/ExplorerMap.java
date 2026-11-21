package com.exiledradio.trickytrialsbackport.chambers;

import com.exiledradio.backportcore.locate.StructureLocate;
import com.exiledradio.backportcore.map.MapIcons;
import com.exiledradio.backportcore.trade.TradeRegistry;
import com.exiledradio.trickytrialsbackport.ModConfig;
import com.exiledradio.trickytrialsbackport.TrickyTrialsBackport;
import com.exiledradio.trickytrialsbackport.common.Owners;
import net.minecraft.entity.IMerchant;
import net.minecraft.entity.passive.EntityVillager;
import net.minecraft.init.Items;
import net.minecraft.item.ItemMap;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.BlockPos;
import net.minecraft.village.MerchantRecipe;
import net.minecraft.village.MerchantRecipeList;
import net.minecraft.world.World;
import net.minecraft.world.WorldServer;
import net.minecraft.world.storage.MapData;
import net.minecraft.world.storage.MapDecoration;
import net.minecraftforge.fml.common.Loader;
import net.minecraftforge.fml.common.registry.ForgeRegistries;
import net.minecraftforge.fml.common.registry.VillagerRegistry;

import javax.annotation.Nullable;
import java.lang.reflect.Field;
import java.util.Random;

/**
 * The trial explorer map (1.21): 1.21.11's {@code VillagerTrades} cartographer, journeyman level,
 * {@code TreasureMapForEmeralds(12, #on_trial_chambers_maps, "filled_map.trial_chambers", TRIAL_CHAMBERS, 12, 10)}.
 *
 * <ul>
 *   <li>The map: a zoom-2 map centered on the nearest trial chambers ({@code #on_trial_chambers_maps} is
 *       {@code minecraft:trial_chambers} alone), with the biome preview, a {@code "+"} decoration of today's trial
 *       chambers type (Backport Core's {@link MapIcons}: its sprite and the map item's color, 12741452) and the name
 *       {@code filled_map.trial_chambers}. The search is Backport Core's {@code /locate}, today's 100 regions out from
 *       the villager.</li>
 *   <li>The trade: 12 emeralds and a compass, 12 uses. With Village &amp; Pillage Backport, its current-Java
 *       cartographer table ({@code trades.modernCartographer}) already sells this map at journeyman; with its 1.14
 *       table, this mod adds the map to that table's journeyman pool through Backport Core's {@link TradeRegistry}.
 *       Without it, 1.12.2's cartographer career gets it at its fourth level, beside the monument map, which is where
 *       1.21.11 has it too.</li>
 *   <li>No vanilla 1.21.11 chest holds one (its only {@code exploration_map} loot is the buried treasure map).</li>
 * </ul>
 */
public final class ExplorerMap {

    /** Today's sprite name, shared with every backport that asks Backport Core for it. */
    public static final String ICON = "trial_chambers";
    public static final String STRUCTURE = "minecraft:trial_chambers";
    /** The map's item name (today's {@code ITEM_NAME}), vanilla's key. */
    public static final String NAME = "filled_map.trial_chambers";
    public static final int EMERALDS = 12;
    public static final int MAX_USES = 12;
    /** 1.14 and later's journeyman, in Village &amp; Pillage's tables. */
    public static final int JOURNEYMAN = 3;
    /** 1.12.2's cartographer level that sells its explorer maps. */
    public static final int CAREER_LEVEL = 4;
    public static final String CAREER = "minecraft:librarian/cartographer";

    private static MapDecoration.Type type;
    private static Field vpTrades;
    private static Field vpModern;
    private static boolean vpRead;

    private ExplorerMap() {
    }

    /** Pre-init, both sides, whatever the settings: the decoration type is numbered the same on servers and clients. */
    static void preInit() {
        type = MapIcons.get(ICON, MapIcons.TRIAL_CHAMBERS_COLOR);
    }

    /** Init, after the chambers' {@code /locate} is registered. */
    static void init() {
        if (Loader.isModLoaded(Owners.VILLAGE_AND_PILLAGE)) {
            TradeRegistry.addVillagerTrade(CAREER, JOURNEYMAN, new TradeRegistry.VillagerTrade() {
                @Override
                public boolean available() {
                    return offered() && !villagePillageSellsIt();
                }

                @Nullable
                @Override
                public MerchantRecipe offer(@Nullable EntityVillager villager, Random random) {
                    return villager == null ? null : trade(villager.world, new BlockPos(villager));
                }
            });
            return;
        }
        VillagerRegistry.VillagerCareer career = cartographer();
        if (career == null) {
            TrickyTrialsBackport.LOGGER.warn("No cartographer career; the trial explorer map is not sold.");
            return;
        }
        career.addTrade(CAREER_LEVEL, new EntityVillager.ITradeList() {
            @Override
            public void addMerchantRecipe(IMerchant merchant, MerchantRecipeList list, Random random) {
                MerchantRecipe recipe = offered() ? trade(merchant.getWorld(), merchant.getPos()) : null;
                if (recipe != null) {
                    list.add(recipe);
                }
            }
        });
    }

    /** @return the decoration type of today's trial chambers icon */
    public static MapDecoration.Type type() {
        return type;
    }

    /** @return true while the setting is on and trial chambers can be located */
    public static boolean offered() {
        return ModConfig.chambers.enabled && ModConfig.chambers.explorerMap && StructureLocate.known(STRUCTURE);
    }

    /** @return 1.12.2's cartographer career, or null if another mod took it away */
    @Nullable
    public static VillagerRegistry.VillagerCareer cartographer() {
        VillagerRegistry.VillagerProfession librarian = ForgeRegistries.VILLAGER_PROFESSIONS.getValue(new ResourceLocation("minecraft:librarian"));
        if (librarian == null) {
            return null;
        }
        VillagerRegistry.VillagerCareer career = librarian.getCareer(1);
        return career != null && "cartographer".equals(career.getName()) ? career : null;
    }

    /**
     * Today's {@code TreasureMapForEmeralds.getOffer}.
     *
     * @return the trade, or null where the world has no trial chambers within today's 100 regions
     */
    @Nullable
    public static MerchantRecipe trade(World world, BlockPos from) {
        ItemStack map = create(world, from);
        return map.isEmpty() ? null
                : new MerchantRecipe(new ItemStack(Items.EMERALD, EMERALDS), new ItemStack(Items.COMPASS), map, 0, MAX_USES);
    }

    /** @return a trial explorer map to the trial chambers nearest {@code from}, or empty if there are none */
    public static ItemStack create(World world, BlockPos from) {
        if (!(world instanceof WorldServer)) {
            return ItemStack.EMPTY;
        }
        BlockPos target;
        try {
            StructureLocate.Found found = StructureLocate.nearest((WorldServer) world, from, STRUCTURE);
            target = found == null ? null : found.pos;
        } catch (StructureLocate.Unavailable e) {
            target = null;
        }
        if (target == null) {
            return ItemStack.EMPTY;
        }
        ItemStack map = ItemMap.setupNewMap(world, target.getX(), target.getZ(), (byte) 2, true, true);
        ItemMap.renderBiomePreviewMap(world, map);
        MapData.addTargetDecoration(map, target, "+", type);
        map.setTranslatableName(NAME);
        return map;
    }

    /**
     * @return true when Village &amp; Pillage Backport's current-Java cartographer table is on: it sells this map at
     * journeyman itself. An older version without that setting has only 1.14's table.
     */
    public static synchronized boolean villagePillageSellsIt() {
        if (!vpRead) {
            vpRead = true;
            try {
                Class<?> config = Class.forName("com.exiledradio.villagerbackport.ModConfig");
                vpTrades = config.getField("trades");
                vpModern = vpTrades.getType().getField("modernCartographer");
            } catch (ReflectiveOperationException | RuntimeException | LinkageError e) {
                vpTrades = null;
                vpModern = null;
            }
        }
        if (vpModern == null) {
            return false;
        }
        try {
            return vpModern.getBoolean(vpTrades.get(null));
        } catch (ReflectiveOperationException | RuntimeException e) {
            return false;
        }
    }
}
