package com.exiledradio.trickytrialsbackport.flora.client;

import com.exiledradio.trickytrialsbackport.TrickyTrialsBackport;
import com.exiledradio.trickytrialsbackport.flora.FloraContent;

import net.minecraft.client.Minecraft;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.renderer.texture.TextureUtil;
import net.minecraft.client.resources.IReloadableResourceManager;
import net.minecraft.client.resources.IResourceManager;
import net.minecraft.client.resources.IResourceManagerReloadListener;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Biomes;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.world.ColorizerGrass;
import net.minecraft.world.EnumSkyBlock;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;
import net.minecraft.world.biome.Biome;
import net.minecraft.world.biome.BiomeColorHelper;
import net.minecraftforge.client.event.ColorHandlerEvent;
import net.minecraftforge.client.event.TextureStitchEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

import java.io.IOException;
import java.util.Random;

/**
 * Client parts of the spring plants: the bush's and wildflower stems' grass tint, leaf litter's dry foliage tint
 * (1.21.5's third color map), the firefly particle and the bushes' and dry grass's ambient sounds.
 */
public final class FloraClient {

    private static TextureAtlasSprite firefly;
    /** {@code DryFoliageColor}'s map; {@code FOLIAGE_DRY_DEFAULT} until it is loaded. */
    private static int[] dryFoliage = new int[0];
    private static final int DRY_DEFAULT = -10732494;

    private FloraClient() {
    }

    public static void init() {
        if (!FloraContent.enabled()) {
            return;
        }
        MinecraftForge.EVENT_BUS.register(new Events());
        ((IReloadableResourceManager) Minecraft.getMinecraft().getResourceManager()).registerReloadListener(new ColorMap());
    }

    // ===========================================================================================
    // Firefly bush and dry grass (their randomDisplayTick)
    // ===========================================================================================

    /**
     * {@code FireflyBushBlock.animateTick}: one time in thirty at night (day time 12600 to 23400, as 1.21.11's day
     * timeline sets {@code firefly_bush_sounds}) under open sky, the buzz; seven times in ten where the light is 13 or
     * lower, a firefly somewhere within 5 blocks across and 5 up.
     */
    public static void fireflyBush(World world, BlockPos pos, Random rand) {
        if (rand.nextInt(30) == 0 && night(world) && openAbove(world, pos)) {
            world.playSound(pos.getX() + 0.5D, pos.getY() + 0.5D, pos.getZ() + 0.5D, FloraContent.FIREFLY_BUSH_IDLE,
                    SoundCategory.AMBIENT, 1.0F, 1.0F, false);
        }
        int sky = world.getLightFor(EnumSkyBlock.SKY, pos) - world.calculateSkylightSubtracted(1.0F);
        int brightness = Math.max(sky, world.getLightFor(EnumSkyBlock.BLOCK, pos));
        if (brightness <= 13 && rand.nextDouble() <= 0.7D) {
            double x = pos.getX() + rand.nextDouble() * 10.0D - 5.0D;
            double y = pos.getY() + rand.nextDouble() * 5.0D;
            double z = pos.getZ() + rand.nextDouble() * 10.0D - 5.0D;
            if (firefly != null) {
                Minecraft.getMinecraft().effectRenderer.addEffect(new Firefly(world, x, y, z, rand));
            }
        }
    }

    private static boolean night(World world) {
        if (!world.provider.isSurfaceWorld()) {
            return false;
        }
        long time = world.getWorldTime() % 24000L;
        return time >= 12600L && time < 23401L;
    }

    /** MOTION_BLOCKING_NO_LEAVES at the column is at or below the bush: nothing but leaves over it. */
    private static boolean openAbove(World world, BlockPos pos) {
        int top = world.getPrecipitationHeight(pos).getY();
        BlockPos.MutableBlockPos p = new BlockPos.MutableBlockPos();
        for (int y = top - 1; y > pos.getY(); y--) {
            p.setPos(pos.getX(), y, pos.getZ());
            net.minecraft.block.state.IBlockState s = world.getBlockState(p);
            if ((s.getMaterial().blocksMovement() || s.getMaterial().isLiquid()) && !s.getBlock().isLeaves(s, world, p)) {
                return false;
            }
        }
        return true;
    }

    /** {@code playAmbientDryGrassSounds}: one time in 200 on sand or terracotta two deep, the wind, at the player. */
    public static void dryGrass(World world, BlockPos pos, Random rand) {
        if (rand.nextInt(200) == 0 && dryVegetationSounds(world, pos.down())) {
            EntityPlayer player = Minecraft.getMinecraft().player;
            if (player != null) {
                world.playSound(player.posX, player.posY, player.posZ, FloraContent.DRY_GRASS, SoundCategory.AMBIENT, 1.0F, 1.0F, false);
            }
        }
    }

    private static boolean dryVegetationSounds(World world, BlockPos pos) {
        return dryTrigger(world, pos) && dryTrigger(world, pos.down());
    }

    private static boolean dryTrigger(World world, BlockPos pos) {
        net.minecraft.block.Block b = world.getBlockState(pos).getBlock();
        return b == net.minecraft.init.Blocks.SAND || b == net.minecraft.init.Blocks.HARDENED_CLAY
                || b == net.minecraft.init.Blocks.STAINED_HARDENED_CLAY;
    }

    // ===========================================================================================
    // Colors
    // ===========================================================================================

    /** {@code DryFoliageColor.get}: the color map at the biome's temperature and downfall, as foliage's. */
    static int dryFoliage(Biome biome) {
        if (biome == Biomes.ROOFED_FOREST || biome == Biomes.MUTATED_ROOFED_FOREST || biome == Biomes.SWAMPLAND
                || biome == Biomes.MUTATED_SWAMPLAND || named(biome, "mangrove_swamp")) {
            return 0x7B5334;
        }
        if (named(biome, "pale_garden")) {
            return 0xA0A69C;
        }
        if (named(biome, "dappled_forest")) {
            // 26.3's dry_foliage_color for Game Drops Backport's dappled forest.
            return 0x8C3A04;
        }
        double t = MathHelper.clamp(biome.getDefaultTemperature(), 0.0F, 1.0F);
        double d = MathHelper.clamp(biome.getRainfall(), 0.0F, 1.0F);
        d *= t;
        int i = (int) ((1.0D - t) * 255.0D);
        int j = (int) ((1.0D - d) * 255.0D);
        int k = j << 8 | i;
        return k >= dryFoliage.length ? DRY_DEFAULT : dryFoliage[k];
    }

    private static boolean named(Biome biome, String path) {
        return biome.getRegistryName() != null && path.equals(biome.getRegistryName().getPath());
    }

    /** {@code BiomeColors.getAverageDryFoliageColor}, over the 3x3 around the block as 1.12.2 averages grass. */
    static int averageDryFoliage(IBlockAccess world, BlockPos pos) {
        int r = 0, g = 0, b = 0, n = 0;
        for (BlockPos.MutableBlockPos p : BlockPos.getAllInBoxMutable(pos.add(-1, 0, -1), pos.add(1, 0, 1))) {
            int c = dryFoliage(world.getBiome(p));
            r += (c & 0xFF0000) >> 16;
            g += (c & 0xFF00) >> 8;
            b += c & 0xFF;
            n++;
        }
        return (r / n & 255) << 16 | (g / n & 255) << 8 | b / n & 255;
    }

    static final class ColorMap implements IResourceManagerReloadListener {
        @Override
        public void onResourceManagerReload(IResourceManager manager) {
            try {
                dryFoliage = TextureUtil.readImageData(manager,
                        new ResourceLocation(TrickyTrialsBackport.MOD_ID, "textures/colormap/dry_foliage.png"));
            } catch (IOException e) {
                TrickyTrialsBackport.LOGGER.warn("Could not load the dry foliage color map; leaf litter keeps its default color.", e);
                dryFoliage = new int[0];
            }
        }
    }

    public static final class Events {
        @SubscribeEvent
        public void stitch(TextureStitchEvent.Pre event) {
            firefly = event.getMap().registerSprite(new ResourceLocation(TrickyTrialsBackport.MOD_ID, "particle/firefly"));
        }

        @SubscribeEvent
        public void blockColors(ColorHandlerEvent.Block event) {
            event.getBlockColors().registerBlockColorHandler((state, world, pos, tint) -> world != null && pos != null
                    ? BiomeColorHelper.getGrassColorAtPos(world, pos) : ColorizerGrass.getGrassColor(0.5D, 1.0D), FloraContent.bush);
            event.getBlockColors().registerBlockColorHandler((state, world, pos, tint) -> tint == 0 ? -1
                    : world != null && pos != null ? BiomeColorHelper.getGrassColorAtPos(world, pos)
                    : ColorizerGrass.getGrassColor(0.5D, 1.0D), FloraContent.wildflowers);
            event.getBlockColors().registerBlockColorHandler((state, world, pos, tint) -> world != null && pos != null
                    ? averageDryFoliage(world, pos) : DRY_DEFAULT, FloraContent.leafLitter);
        }

        @SubscribeEvent
        public void itemColors(ColorHandlerEvent.Item event) {
            // items/bush.json: tinted as grass at temperature 0.5, downfall 1.0.
            event.getItemColors().registerItemColorHandler((stack, tint) -> ColorizerGrass.getGrassColor(0.5D, 1.0D), FloraContent.bush);
        }
    }

    // ===========================================================================================
    // FireflyParticle
    // ===========================================================================================

    /**
     * 1.21.11's {@code FireflyParticle}: lives 200-300 ticks, fades in over its first 30% and out over its last half,
     * glows (its light rises over the first 10% and falls over the last 30%), wanders off in a new direction one tick
     * in twenty, speeds up sideways when it bumps into something above or below, and goes when it enters a block.
     */
    static final class Firefly extends Particle {

        Firefly(World world, double x, double y, double z, Random rand) {
            super(world, x, y, z, 0.5D - rand.nextDouble(), rand.nextBoolean() ? 0.0D : -0.0D, 0.5D - rand.nextDouble());
            setParticleTexture(firefly);
            this.particleScale *= 0.75F;
            this.motionX *= 0.8D;
            this.motionY *= 0.8D;
            this.motionZ *= 0.8D;
            this.particleMaxAge = 200 + rand.nextInt(101);
            // scale(1.5F)
            setSize(0.2F * 1.5F, 0.2F * 1.5F);
            this.particleScale *= 1.5F;
            this.particleAlpha = 0.0F;
        }

        @Override
        public int getFXLayer() {
            return 1;
        }

        private float progress(float age) {
            return MathHelper.clamp(age / this.particleMaxAge, 0.0F, 1.0F);
        }

        private static float fade(float t, float fadeOut, float fadeIn) {
            if (t >= 1.0F - fadeOut) {
                return (1.0F - t) / fadeOut;
            }
            return t <= fadeIn ? t / fadeIn : 1.0F;
        }

        @Override
        public int getBrightnessForRender(float partialTicks) {
            return (int) (255.0F * fade(progress(this.particleAge + partialTicks), 0.1F, 0.3F));
        }

        @Override
        public void onUpdate() {
            this.prevPosX = this.posX;
            this.prevPosY = this.posY;
            this.prevPosZ = this.posZ;
            if (this.particleAge++ >= this.particleMaxAge) {
                setExpired();
                return;
            }
            double oldY = this.posY;
            move(this.motionX, this.motionY, this.motionZ);
            if (this.posY == oldY) {
                // speedUpWhenYMotionIsBlocked
                this.motionX *= 1.1D;
                this.motionZ *= 1.1D;
            }
            this.motionX *= 0.96D;
            this.motionY *= 0.96D;
            this.motionZ *= 0.96D;
            if (this.onGround) {
                this.motionX *= 0.7D;
                this.motionZ *= 0.7D;
            }
            BlockPos at = new BlockPos(this.posX, this.posY, this.posZ);
            if (!this.world.isAirBlock(at)) {
                setExpired();
                return;
            }
            this.particleAlpha = fade(progress(this.particleAge), 0.3F, 0.5F);
            if (this.rand.nextFloat() > 0.95F || this.particleAge == 1) {
                this.motionX = -0.05F + 0.1F * this.rand.nextFloat();
                this.motionY = -0.05F + 0.1F * this.rand.nextFloat();
                this.motionZ = -0.05F + 0.1F * this.rand.nextFloat();
            }
        }
    }
}
