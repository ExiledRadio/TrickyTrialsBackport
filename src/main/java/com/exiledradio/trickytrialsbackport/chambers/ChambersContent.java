package com.exiledradio.trickytrialsbackport.chambers;

import com.exiledradio.backportcore.locate.StructureLocate;
import com.exiledradio.backportcore.structure.JigsawPools;
import com.exiledradio.backportcore.structure.ModernPalette;
import com.exiledradio.backportcore.structure.ScatteredStructure;
import com.exiledradio.backportcore.structure.Structures;
import com.exiledradio.backportcore.structure.TerrainAdaptation;
import com.exiledradio.backportcore.worldgen.FlatWorlds;
import com.exiledradio.trickytrialsbackport.ModConfig;
import com.exiledradio.trickytrialsbackport.TrickyTrialsBackport;
import com.exiledradio.trickytrialsbackport.common.Content;
import com.exiledradio.trickytrialsbackport.common.ItemTrigger;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.item.ItemStack;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraft.world.gen.structure.MapGenStructureIO;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.world.WorldEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;

/**
 * Stream 10: 1.21's trial chambers.
 *
 * <ul>
 *   <li>The structure ({@link TrialChambersStructure}): 1.21.11's structure set, structure, pools, processor list and
 *       templates, unchanged ({@code tools/chambers/generate.py}), laid out by 1.21's own jigsaw rules
 *       ({@link ChamberLayout}), placed by Backport Core's templates and scattered driver.</li>
 *   <li>Inside: the trials stream's trial spawners (their 1.21 configs, picked per chamber by the pool aliases), vaults
 *       and ominous vaults; chests, barrels, dispensers and pots roll 1.21's tables ({@link ChamberLoot}); breezes and
 *       bogged come from their streams' spawner configs.</li>
 *   <li>No natural spawns inside a piece (1.21's {@code spawn_overrides}, every category empty, by piece).</li>
 *   <li>{@code /locate structure minecraft:trial_chambers} (Backport Core), and the terrain adapts where a world's
 *       generator can ({@code terrain_adaptation: encapsulate}).</li>
 *   <li>The advancement <i>Minecraft: Trial(s) Edition</i>, parent of the trials stream's <i>Under Lock and Key</i>.</li>
 *   <li>The trial explorer map, sold by cartographers at journeyman ({@link ExplorerMap}).</li>
 * </ul>
 */
public final class ChambersContent {

    public static final ItemTrigger ENTERED = new ItemTrigger(Content.id("entered_trial_chambers"));

    /**
     * Blocks of the chambers' templates that other family mods own: Village &amp; Pillage's barrel (its loot moves to a
     * chest), Trails &amp; Tales' decorated pot, Caves &amp; Cliffs' candles, deepslate, moss, dripstone and powder snow,
     * Nether Update's chain, Wild Update's mangrove and mud.
     */
    static final String[][] FALLBACKS = {
            {"minecraft:barrel", "minecraft:chest"},
            {"minecraft:decorated_pot", "minecraft:air"},
            {"minecraft:candle", "minecraft:air"},
            {"minecraft:red_candle", "minecraft:air"},
            {"minecraft:cobbled_deepslate", "minecraft:cobblestone"},
            {"minecraft:moss_block", "minecraft:mossy_cobblestone"},
            {"minecraft:moss_carpet", "minecraft:air"},
            {"minecraft:pointed_dripstone", "minecraft:air"},
            {"minecraft:powder_snow", "minecraft:snow_block"},
            {"minecraft:iron_chain", "minecraft:air"},
            {"minecraft:mangrove_leaves", "minecraft:oak_leaves"},
            {"minecraft:mangrove_log", "minecraft:oak_log"},
            {"minecraft:mangrove_wood", "minecraft:oak_wood"},
            {"minecraft:mangrove_roots", "minecraft:air"},
            {"minecraft:muddy_mangrove_roots", "minecraft:dirt"},
            {"minecraft:mud", "minecraft:dirt"},
    };

    private ChambersContent() {
    }

    public static boolean enabled() {
        return ModConfig.chambers.enabled;
    }

    /** @return true when new chunks get trial chambers */
    public static boolean generates() {
        return enabled();
    }

    /** Pre-init: this jar holds vanilla's files under their minecraft: names; the loot tables' renames. */
    public static void preInit() {
        ExplorerMap.preInit();
        if (!enabled()) {
            return;
        }
        JigsawPools.source(TrickyTrialsBackport.MOD_ID);
        ModernPalette.source(TrickyTrialsBackport.MOD_ID);
        ChamberLoot.preInit();
        // Stand-ins only where nothing installed provides the block (its owner's is always used when present):
        // otherwise the terrain's block would stay where the template has one of these.
        for (String[] f : FALLBACKS) {
            ModernPalette.fallback(f[0], f[1]);
        }
        // Floor and ceiling buttons and levers in every facing (Core's table knows them facing north only).
        FaceAttached.register();
        ENTERED.register();
    }

    /** From init. */
    public static void init() {
        if (!enabled()) {
            return;
        }
        MapGenStructureIO.registerStructure(TrialChambersStructure.Start.class, TrialChambersStructure.SAVE_NAME);
        MapGenStructureIO.registerStructureComponent(ChamberPiece.class, "TTB_TrialChambersPiece");
        ScatteredStructure.register(Structures.TRIAL_CHAMBERS,
                // Never in superflat (Core gates it too; a 1.12 preset has no option for trial chambers).
                world -> generates() && world.provider.getDimension() == 0 && FlatWorlds.structure(world, Structures.TRIAL_CHAMBERS)
                        ? new TrialChambersStructure() : null);
        StructureLocate.scattered("minecraft:trial_chambers", Structures.TRIAL_CHAMBERS, null, "minecraft:trial_chambers");
        ExplorerMap.init();
        // 1.21's terrain_adaptation: encapsulate. A terrain that adapts to structures does it in its own noise.
        TerrainAdaptation.register(Structures.TRIAL_CHAMBERS, TerrainAdaptation.Kind.ENCAPSULATE, (world, cx, cz, out) -> {
            ScatteredStructure s = ScatteredStructure.of(world, Structures.TRIAL_CHAMBERS);
            if (s instanceof TrialChambersStructure) {
                ((TrialChambersStructure) s).startsNear(world, cx, cz, out);
            }
        });
        MinecraftForge.EVENT_BUS.register(new ChamberLoot());
        MinecraftForge.EVENT_BUS.register(new Events());
    }

    /** @return true if the block is inside a piece of a laid-out trial chamber (1.21's piece-wise structure checks) */
    public static boolean insidePiece(World world, BlockPos pos) {
        ScatteredStructure s = ScatteredStructure.of(world, Structures.TRIAL_CHAMBERS);
        return s != null && s.isInsideStructure(pos);
    }

    public static final class Events {
        /** 1.21's spawn_overrides: every category empty, by piece. */
        @SubscribeEvent
        public void potentialSpawns(WorldEvent.PotentialSpawns event) {
            World world = event.getWorld();
            if (!world.isRemote && world.provider.getDimension() == 0 && !event.getList().isEmpty() && insidePiece(world, event.getPos())) {
                event.getList().clear();
            }
        }

        /** Minecraft: Trial(s) Edition: 1.21's location trigger checks once a second. */
        @SubscribeEvent
        public void playerTick(TickEvent.PlayerTickEvent event) {
            EntityPlayer player = event.player;
            if (event.phase != TickEvent.Phase.END || !(player instanceof EntityPlayerMP) || player.ticksExisted % 20 != 0
                    || player.world.provider.getDimension() != 0) {
                return;
            }
            if (insidePiece(player.world, new BlockPos(player))) {
                ENTERED.trigger((EntityPlayerMP) player, ItemStack.EMPTY);
            }
        }
    }
}
