package com.exiledradio.trickytrialsbackport.selftest;

import com.exiledradio.backportcore.locate.StructureLocate;
import com.exiledradio.backportcore.structure.ScatteredPlacement;
import com.exiledradio.backportcore.structure.ScatteredStructure;
import com.exiledradio.backportcore.structure.Structures;
import com.exiledradio.backportcore.worldgen.FlatWorlds;
import com.exiledradio.trickytrialsbackport.TrickyTrialsBackport;
import com.exiledradio.trickytrialsbackport.chambers.ChamberLayout;
import com.exiledradio.trickytrialsbackport.chambers.ChamberSettings;
import com.exiledradio.trickytrialsbackport.chambers.ChambersContent;
import com.exiledradio.trickytrialsbackport.chambers.TrialChambersStructure;
import com.exiledradio.trickytrialsbackport.common.Owners;

import net.minecraft.block.Block;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityList;
import net.minecraft.init.Blocks;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.world.World;
import net.minecraft.world.WorldServer;
import net.minecraft.world.WorldType;
import net.minecraft.world.chunk.Chunk;
import net.minecraft.world.chunk.storage.ExtendedBlockStorage;
import net.minecraft.world.storage.WorldInfo;
import net.minecraftforge.fml.common.IWorldGenerator;
import net.minecraftforge.fml.common.registry.GameRegistry;
import net.minecraftforge.fml.relauncher.ReflectionHelper;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.TreeMap;
import java.util.function.Function;

/**
 * Superflat worlds generate only what vanilla 1.12 does for their preset (Backport Core's {@code FlatWorlds}): no trial
 * chambers ever, and this mod's ores (copper, tuff) only with {@code decoration}.
 *
 * <p>In the suite's own (Default) world it checks the gates by making the world info say superflat for a moment: the
 * registered trial chambers factory gives no instance, and this mod's world generators draw nothing from their random.
 * In a real superflat world ({@code tools/flat/selftest.sh}) it populates the chunks around spawn and around the chunk
 * where trial chambers would start in any other world, then scans them for this mod's blocks and entities.
 */
public final class FlatSelfTest implements SelfTest {

    private static final String DEFAULT = "3;minecraft:bedrock,2*minecraft:dirt,minecraft:grass;1;village";
    private static final String STONE = "3;minecraft:bedrock,59*minecraft:stone,3*minecraft:dirt,minecraft:grass;1;village";
    private static final String DECORATED = "3;minecraft:bedrock,59*minecraft:stone,3*minecraft:dirt,minecraft:grass;1;village,decoration";

    @Override
    public String name() {
        return "flat";
    }

    @Override
    public void run(Check c) throws Exception {
        if (FlatWorlds.isFlat(c.world)) {
            flatWorld(c);
        } else {
            rules(c);
        }
    }

    // ---- the gates, in the suite's world

    private void rules(Check c) throws Exception {
        WorldServer world = c.world;
        c.ok(FlatWorlds.decoration(world), "a Default world decorates");
        if (ChambersContent.generates()) {
            c.ok(ScatteredStructure.of(world, Structures.TRIAL_CHAMBERS) instanceof TrialChambersStructure,
                    "a Default world has trial chambers");
        }
        Function<World, ? extends ScatteredStructure> factory = chamberFactory();
        List<IWorldGenerator> ours = ourGenerators();
        // The generators are the copies' (copper ore, tuff): with Caves & Cliffs installed it owns both, generates
        // them itself, and this mod registers none.
        boolean copies = Owners.copy("copper_ore") || Owners.copy("tuff");
        if (copies) {
            c.ok(!ours.isEmpty(), "this mod registers world generators (copper, tuff): " + ours.size());
            c.eq(-1, draws(world, ours), "outside superflat the generators run (draws, or -1 for some)");
        } else {
            c.eq(0, ours.size(), "copper and tuff are the owner's, so this mod registers no world generators");
        }

        WorldInfo info = world.getWorldInfo();
        Field type = ReflectionHelper.findField(WorldInfo.class, "terrainType", "field_76098_b");
        Field options = ReflectionHelper.findField(WorldInfo.class, "generatorOptions", "field_82576_c");
        Object oldType = type.get(info);
        Object oldOptions = options.get(info);
        try {
            type.set(info, WorldType.FLAT);
            for (String preset : new String[]{DEFAULT, STONE, DECORATED}) {
                options.set(info, preset);
                FlatWorlds.forget(world);
                boolean decorated = preset.endsWith("decoration");
                c.ok(FlatWorlds.isFlat(world) && FlatWorlds.decoration(world) == decorated, "superflat " + preset);
                if (factory != null) {
                    c.ok(factory.apply(world) == null, "the trial chambers factory gives no instance in superflat " + preset);
                }
                c.ok(Structures.nearest(world, Structures.TRIAL_CHAMBERS, BlockPos.ORIGIN, 64) == null,
                        "no trial chambers near in superflat " + preset);
                c.ok(StructureLocate.nearest(world, BlockPos.ORIGIN, "minecraft:trial_chambers") == null,
                        "/locate finds no trial chambers in superflat " + preset);
                int drew = draws(world, ours);
                if (!copies) {
                    continue;
                }
                if (decorated) {
                    c.eq(-1, drew, "with decoration the generators run in superflat");
                } else {
                    c.eq(0, drew, "without decoration the generators draw nothing in superflat " + preset);
                }
            }
        } finally {
            type.set(info, oldType);
            options.set(info, oldOptions);
            FlatWorlds.forget(world);
        }
        c.ok(!FlatWorlds.isFlat(world) && FlatWorlds.decoration(world), "the world is itself again");
    }

    @SuppressWarnings("unchecked")
    private static Function<World, ? extends ScatteredStructure> chamberFactory() {
        Map<String, Function<World, ? extends ScatteredStructure>> factories =
                ReflectionHelper.getPrivateValue(ScatteredStructure.class, null, "FACTORIES");
        return factories.get(Structures.TRIAL_CHAMBERS);
    }

    /** This mod's registered {@link IWorldGenerator}s. */
    @SuppressWarnings("unchecked")
    private static List<IWorldGenerator> ourGenerators() {
        Set<IWorldGenerator> all = ReflectionHelper.getPrivateValue(GameRegistry.class, null, "worldGenerators");
        List<IWorldGenerator> ours = new ArrayList<IWorldGenerator>();
        for (IWorldGenerator g : all) {
            if (g.getClass().getName().startsWith("com.exiledradio.trickytrialsbackport.")) {
                ours.add(g);
            }
        }
        return ours;
    }

    /**
     * Asks every generator for a far chunk with a random that stops at its first draw (so nothing is placed).
     * @return 0 when no generator drew, -1 when every one did, else how many did
     */
    private static int draws(WorldServer world, List<IWorldGenerator> generators) {
        int drew = 0;
        for (IWorldGenerator g : generators) {
            Stop random = new Stop();
            try {
                g.generate(random, 40000, 40000, world, null, world.getChunkProvider());
            } catch (Stop.Drawn e) {
                drew++;
            }
        }
        return drew == generators.size() && drew > 0 ? -1 : drew;
    }

    /** A random that throws on its first draw. */
    private static final class Stop extends Random {
        @Override
        protected int next(int bits) {
            throw new Drawn();
        }

        static final class Drawn extends RuntimeException {
        }
    }

    // ---- a real superflat world

    private void flatWorld(Check c) throws Exception {
        WorldServer world = c.world;
        String options = world.getWorldInfo().getGeneratorOptions();
        boolean decorated = FlatWorlds.decoration(world);
        TrickyTrialsBackport.LOGGER.info("SELFTEST flat world: preset {}", options);
        c.eq(options.contains("decoration"), decorated, "decoration as the preset says (" + options + ")");

        BlockPos spawn = world.getSpawnPoint();
        ChunkPos home = new ChunkPos(spawn);
        // 13 x 13 chunks populated around spawn (a chunk is populated once its east, south and south-east neighbors exist).
        populate(world, home.x, home.z, 6);

        // The chunk where trial chambers start in any other world with this seed: the nearest candidate chunk around
        // spawn whose 1.21 start rule (the stub) gives a start. Its start piece is always within two chunks of it.
        ScatteredPlacement grid = ChamberSettings.get().grid;
        int s = grid.spacing();
        ChunkPos start = null;
        for (int gx = Math.floorDiv(home.x, s) - 2; gx <= Math.floorDiv(home.x, s) + 2; gx++) {
            for (int gz = Math.floorDiv(home.z, s) - 2; gz <= Math.floorDiv(home.z, s) + 2; gz++) {
                ChunkPos cand = grid.candidate(world.getSeed(), gx * s, gz * s);
                ChamberLayout.Stub stub = TrialChambersStructure.stub(world, cand.x, cand.z);
                if (stub != null && (start == null || dist(cand, home) < dist(start, home))) {
                    start = cand;
                }
            }
        }
        c.ok(start != null, "a chunk where trial chambers would start, by their own start rule");
        if (start != null) {
            TrickyTrialsBackport.LOGGER.info("SELFTEST flat world: spawn {}, chambers would start in chunk {}, {}", spawn, start.x, start.z);
            populate(world, start.x, start.z, 3);
        }

        // Every loaded chunk: this mod's blocks.
        Map<String, Integer> counts = new TreeMap<String, Integer>();
        int stone = 0;
        int chunks = 0;
        for (Chunk chunk : world.getChunkProvider().getLoadedChunks()) {
            if (!chunk.isTerrainPopulated()) {
                continue;
            }
            chunks++;
            for (ExtendedBlockStorage storage : chunk.getBlockStorageArray()) {
                if (storage == null) {
                    continue;
                }
                for (int x = 0; x < 16; x++) {
                    for (int y = 0; y < 16; y++) {
                        for (int z = 0; z < 16; z++) {
                            IBlockState state = storage.get(x, y, z);
                            Block block = state.getBlock();
                            if (block == Blocks.STONE) {
                                stone++;
                            }
                            ResourceLocation id = block.getRegistryName();
                            if (id != null && TrickyTrialsBackport.MOD_ID.equals(id.getNamespace())) {
                                Integer n = counts.get(id.getPath());
                                counts.put(id.getPath(), n == null ? 1 : n + 1);
                            }
                        }
                    }
                }
            }
        }
        TrickyTrialsBackport.LOGGER.info("SELFTEST flat world: {} populated chunks, {} stone, this mod's blocks {}", chunks, stone, counts);
        c.ok(chunks >= 13 * 13, "populated chunks: " + chunks);

        if (!decorated) {
            c.ok(counts.isEmpty(), "no blocks of this mod without decoration: " + counts);
        } else {
            Map<String, Integer> other = new TreeMap<String, Integer>(counts);
            other.remove("copper_ore");
            other.remove("tuff");
            for (String path : new ArrayList<String>(other.keySet())) {
                Block block = Block.REGISTRY.getObject(new ResourceLocation(TrickyTrialsBackport.MOD_ID, path));
                if (block.getClass().getName().contains(".flora.")) {
                    other.remove(path); // spring's plants, by the biome decorator (DecorateBiomeEvent.Post)
                }
            }
            c.ok(other.isEmpty(), "with decoration only ores and plants of this mod (no chambers): " + other);
            if (stone > 0) {
                c.ok(counts.containsKey("copper_ore"), "with decoration and stone, copper ore: " + counts.get("copper_ore"));
                c.ok(counts.containsKey("tuff"), "with decoration and stone, tuff: " + counts.get("tuff"));
            }
        }

        // No structure, and no populate-time mobs of any mod (1.12's superflat generator spawns none).
        c.ok(ScatteredStructure.of(world, Structures.TRIAL_CHAMBERS) == null, "trial chambers have no instance");
        c.ok(Structures.nearest(world, Structures.TRIAL_CHAMBERS, spawn, 64) == null, "the locator finds no trial chambers");
        c.ok(StructureLocate.nearest(world, spawn, "minecraft:trial_chambers") == null, "/locate finds no trial chambers");
        c.ok(world.getPerWorldStorage().getOrLoadData(net.minecraft.world.gen.structure.MapGenStructureData.class,
                TrialChambersStructure.SAVE_NAME) == null, "no saved trial chambers starts");
        List<String> mobs = new ArrayList<String>();
        for (Entity e : world.loadedEntityList) {
            ResourceLocation id = EntityList.getKey(e);
            if (id != null && TrickyTrialsBackport.MOD_ID.equals(id.getNamespace())) {
                mobs.add(id + "@" + e.getPosition());
            }
        }
        c.ok(mobs.isEmpty(), "no entities of this mod: " + mobs);
    }

    private static void populate(WorldServer world, int cx, int cz, int radius) {
        for (int x = cx - radius; x <= cx + radius + 1; x++) {
            for (int z = cz - radius; z <= cz + radius + 1; z++) {
                world.getChunkProvider().provideChunk(x, z);
            }
        }
    }

    private static long dist(ChunkPos a, ChunkPos b) {
        long dx = a.x - b.x;
        long dz = a.z - b.z;
        return dx * dx + dz * dz;
    }
}
