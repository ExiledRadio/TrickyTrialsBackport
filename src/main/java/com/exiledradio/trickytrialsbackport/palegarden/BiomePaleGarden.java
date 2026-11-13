package com.exiledradio.trickytrialsbackport.palegarden;

import net.minecraft.entity.passive.EntityBat;
import net.minecraft.init.Blocks;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraft.world.biome.Biome;
import net.minecraft.world.biome.BiomeDecorator;
import net.minecraft.world.gen.feature.WorldGenLiquids;
import net.minecraftforge.common.BiomeDictionary;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.terraingen.DecorateBiomeEvent;
import net.minecraftforge.event.terraingen.TerrainGen;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import java.util.Random;

/**
 * The pale garden, 1.21.4 ({@code worldgen/biome/pale_garden} of 1.21.11).
 *
 * <ul>
 *   <li>Temperature 0.7, downfall 0.8, rain; grass {@code #778272}, foliage {@code #878D76}, water
 *       {@code #76889D}, sky {@code #B9B9B9}; fog {@code #817770} (client, {@code client.PaleGardenClient}).
 *       The roofed forest's height and variation (0.1, 0.2), so it joins a roofed forest without a seam.</li>
 *   <li>Spawns: no animals; vanilla's usual monsters (spider, zombie, zombie villager, skeleton, creeper,
 *       slime, enderman, witch at 1.21's weights, which are 1.12.2's); bats.</li>
 *   <li>Decoration: ores, sand, clay and gravel disks as any 1.12.2 biome, then 1.21.11's vegetal step
 *       ({@link PaleGardenFeatures#vegetation}), then springs. No mushrooms, flowers or 1.12.2 grass.</li>
 * </ul>
 * Not here: the glow lichen of its caves (Caves &amp; Cliffs' block), the firefly bushes 1.21.5 added by water
 * (stream 15), the silence (1.21.4's {@code music_volume 0}).
 */
public class BiomePaleGarden extends Biome {

    public static final int GRASS = 0x778272;
    public static final int FOLIAGE = 0x878D76;
    public static final int SKY = 0xB9B9B9;
    public static final int FOG = 0x817770;
    public static final int WATER = 0x76889D;

    public BiomePaleGarden() {
        super(new BiomeProperties("Pale Garden").setBaseHeight(0.1F).setHeightVariation(0.2F)
                .setTemperature(0.7F).setRainfall(0.8F).setWaterColor(WATER));
        this.spawnableCreatureList.clear();
        this.spawnableWaterCreatureList.clear();
        this.spawnableCaveCreatureList.clear();
        this.spawnableCaveCreatureList.add(new SpawnListEntry(EntityBat.class, 10, 8, 8));
        this.topBlock = Blocks.GRASS.getDefaultState();
        this.fillerBlock = Blocks.DIRT.getDefaultState();
    }

    static void addTypes(Biome biome) {
        BiomeDictionary.addTypes(biome, BiomeDictionary.Type.FOREST, BiomeDictionary.Type.DENSE, BiomeDictionary.Type.SPOOKY);
    }

    @Override
    public BiomeDecorator createBiomeDecorator() {
        return getModdedBiomeDecorator(new Decorator());
    }

    @Override
    @SideOnly(Side.CLIENT)
    public int getSkyColorByTemp(float temperature) {
        return SKY;
    }

    @Override
    @SideOnly(Side.CLIENT)
    public int getGrassColorAtPos(BlockPos pos) {
        return getModdedBiomeGrassColor(GRASS);
    }

    @Override
    @SideOnly(Side.CLIENT)
    public int getFoliageColorAtPos(BlockPos pos) {
        return getModdedBiomeFoliageColor(FOLIAGE);
    }

    /** 1.12.2's decoration with everything vegetal replaced by 1.21.11's pale garden step. */
    public static class Decorator extends BiomeDecorator {

        @Override
        protected void genDecorations(Biome biome, World world, Random random) {
            net.minecraft.util.math.ChunkPos chunk = new net.minecraft.util.math.ChunkPos(this.chunkPos);
            MinecraftForge.EVENT_BUS.post(new DecorateBiomeEvent.Pre(world, random, chunk));
            generateOres(world, random);
            if (TerrainGen.decorate(world, random, chunk, DecorateBiomeEvent.Decorate.EventType.SAND)) {
                for (int i = 0; i < this.sandPatchesPerChunk; i++) {
                    int x = random.nextInt(16) + 8;
                    int z = random.nextInt(16) + 8;
                    this.sandGen.generate(world, random, world.getTopSolidOrLiquidBlock(this.chunkPos.add(x, 0, z)));
                }
            }
            if (TerrainGen.decorate(world, random, chunk, DecorateBiomeEvent.Decorate.EventType.CLAY)) {
                for (int i = 0; i < this.clayPerChunk; i++) {
                    int x = random.nextInt(16) + 8;
                    int z = random.nextInt(16) + 8;
                    this.clayGen.generate(world, random, world.getTopSolidOrLiquidBlock(this.chunkPos.add(x, 0, z)));
                }
            }
            if (TerrainGen.decorate(world, random, chunk, DecorateBiomeEvent.Decorate.EventType.SAND_PASS2)) {
                for (int i = 0; i < this.gravelPatchesPerChunk; i++) {
                    int x = random.nextInt(16) + 8;
                    int z = random.nextInt(16) + 8;
                    this.gravelGen.generate(world, random, world.getTopSolidOrLiquidBlock(this.chunkPos.add(x, 0, z)));
                }
            }
            if (TerrainGen.decorate(world, random, chunk, DecorateBiomeEvent.Decorate.EventType.TREE)) {
                PaleGardenFeatures.vegetation(world, random, this.chunkPos, biome);
            }
            if (this.generateFalls) {
                if (TerrainGen.decorate(world, random, chunk, DecorateBiomeEvent.Decorate.EventType.LAKE_WATER)) {
                    for (int i = 0; i < 50; i++) {
                        int x = random.nextInt(16) + 8;
                        int z = random.nextInt(16) + 8;
                        int y = random.nextInt(random.nextInt(248) + 8);
                        new WorldGenLiquids(Blocks.FLOWING_WATER).generate(world, random, this.chunkPos.add(x, y, z));
                    }
                }
                if (TerrainGen.decorate(world, random, chunk, DecorateBiomeEvent.Decorate.EventType.LAKE_LAVA)) {
                    for (int i = 0; i < 20; i++) {
                        int x = random.nextInt(16) + 8;
                        int z = random.nextInt(16) + 8;
                        int y = random.nextInt(random.nextInt(random.nextInt(240) + 8) + 8);
                        new WorldGenLiquids(Blocks.FLOWING_LAVA).generate(world, random, this.chunkPos.add(x, y, z));
                    }
                }
            }
            MinecraftForge.EVENT_BUS.post(new DecorateBiomeEvent.Post(world, random, chunk));
        }
    }
}
