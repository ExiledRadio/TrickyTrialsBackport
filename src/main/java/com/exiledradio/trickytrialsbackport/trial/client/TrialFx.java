package com.exiledradio.trickytrialsbackport.trial.client;

import com.exiledradio.trickytrialsbackport.common.Content;
import com.exiledradio.trickytrialsbackport.trial.BlockVault;
import com.exiledradio.trickytrialsbackport.trial.EntityOminousItemSpawner;
import com.exiledradio.trickytrialsbackport.trial.TileEntityTrialSpawner;
import com.exiledradio.trickytrialsbackport.trial.TileEntityVault;
import com.exiledradio.trickytrialsbackport.trial.TrialMessage;
import com.exiledradio.trickytrialsbackport.trial.TrialSpawnerPhase;

import net.minecraft.block.state.IBlockState;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.renderer.texture.TextureMap;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumParticleTypes;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import net.minecraftforge.client.event.TextureStitchEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.client.registry.ClientRegistry;
import net.minecraftforge.fml.client.registry.RenderingRegistry;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

import java.util.Random;
import java.util.UUID;

/**
 * The trials' client side: 1.21's level events 3011-3021 (sounds and particles), the spawner's and vault's
 * idle effects, the ominous item spawner's streaks, effect icons, and the renderers. Only a client loads
 * this class; common code reaches it with plain values.
 */
public final class TrialFx {

    static TextureAtlasSprite[] flame = new TextureAtlasSprite[1];
    static TextureAtlasSprite[] soulFireFlame = new TextureAtlasSprite[1];
    static TextureAtlasSprite[] trialOmen = new TextureAtlasSprite[1];
    static TextureAtlasSprite[] ominousSpawning = new TextureAtlasSprite[1];
    static TextureAtlasSprite[] vaultConnection = new TextureAtlasSprite[1];
    static TextureAtlasSprite[] detection = new TextureAtlasSprite[5];
    static TextureAtlasSprite[] detectionOminous = new TextureAtlasSprite[5];

    private TrialFx() {
    }

    public static void init() {
        MinecraftForge.EVENT_BUS.register(new TrialFx());
        ClientRegistry.bindTileEntitySpecialRenderer(TileEntityTrialSpawner.class, new RenderTrialSpawner());
        ClientRegistry.bindTileEntitySpecialRenderer(TileEntityVault.class, new RenderVault());
        RenderingRegistry.registerEntityRenderingHandler(EntityOminousItemSpawner.class, RenderOminousItemSpawner::new);
    }

    @SubscribeEvent
    public void stitch(TextureStitchEvent.Pre event) {
        TextureMap map = event.getMap();
        flame[0] = map.registerSprite(Content.id("particle/flame"));
        soulFireFlame[0] = map.registerSprite(Content.id("particle/soul_fire_flame"));
        trialOmen[0] = map.registerSprite(Content.id("particle/trial_omen"));
        ominousSpawning[0] = map.registerSprite(Content.id("particle/ominous_spawning"));
        vaultConnection[0] = map.registerSprite(Content.id("particle/vault_connection"));
        for (int i = 0; i < 5; i++) {
            detection[i] = map.registerSprite(Content.id("particle/trial_spawner_detection_" + i));
            detectionOminous[i] = map.registerSprite(Content.id("particle/trial_spawner_detection_ominous_" + i));
        }
    }

    // ------------------------------------------------------------------------------------------
    // Level events
    // ------------------------------------------------------------------------------------------

    /** From the network thread: run on the client thread. */
    public static void schedule(final int event, final long pos, final int data) {
        final Minecraft mc = Minecraft.getMinecraft();
        mc.addScheduledTask(new Runnable() {
            @Override
            public void run() {
                if (mc.world != null && flame[0] != null) {
                    levelEvent(mc.world, event, BlockPos.fromLong(pos), data);
                }
            }
        });
    }

    private static void levelEvent(World world, int event, BlockPos pos, int data) {
        Random r = world.rand;
        switch (event) {
            case TrialMessage.SPAWN_PARTICLES:
                addSpawnParticles(world, pos, r, data == 1 ? soulFireFlame : flame);
                break;
            case TrialMessage.SPAWN_MOB:
                play(world, pos, "block.trial_spawner.spawn_mob", 1.0F, (r.nextFloat() - r.nextFloat()) * 0.2F + 1.0F);
                addSpawnParticles(world, pos, r, data == 1 ? soulFireFlame : flame);
                break;
            case TrialMessage.DETECT_PLAYER:
                play(world, pos, "block.trial_spawner.detect_player", 1.0F, (r.nextFloat() - r.nextFloat()) * 0.2F + 1.0F);
                addDetectPlayerParticles(world, pos, r, data, detection);
                break;
            case TrialMessage.EJECT_ITEM:
                play(world, pos, "block.trial_spawner.eject_item", 1.0F, (r.nextFloat() - r.nextFloat()) * 0.2F + 1.0F);
                addEjectItemParticles(world, pos, r);
                break;
            case TrialMessage.VAULT_ACTIVATE:
                if (world.getTileEntity(pos) instanceof TileEntityVault) {
                    TileEntityVault vault = (TileEntityVault) world.getTileEntity(pos);
                    emitConnectionParticles(world, pos, vault);
                    for (int i = 0; i < 20; i++) {
                        Vec3d v = randomPosInsideCage(pos, r);
                        world.spawnParticle(EnumParticleTypes.SMOKE_NORMAL, v.x, v.y, v.z, 0.0, 0.0, 0.0);
                        flameParticle(world, data == 0 ? flame : soulFireFlame, data == 0 ? 0.5F : 1.0F, v.x, v.y, v.z, 0.0, 0.0, 0.0);
                    }
                    play(world, pos, "block.vault.activate", 1.0F, (r.nextFloat() - r.nextFloat()) * 0.2F + 1.0F);
                }
                break;
            case TrialMessage.VAULT_DEACTIVATE:
                for (int i = 0; i < 20; i++) {
                    double x = pos.getX() + 0.4 + r.nextDouble() * 0.2;
                    double y = pos.getY() + 0.4 + r.nextDouble() * 0.2;
                    double z = pos.getZ() + 0.4 + r.nextDouble() * 0.2;
                    flameParticle(world, data == 0 ? flame : soulFireFlame, data == 0 ? 0.5F : 1.0F, x, y, z,
                            r.nextGaussian() * 0.02, r.nextGaussian() * 0.02, r.nextGaussian() * 0.02);
                }
                play(world, pos, "block.vault.deactivate", 1.0F, (r.nextFloat() - r.nextFloat()) * 0.2F + 1.0F);
                break;
            case TrialMessage.VAULT_EJECT_ITEM:
                addEjectItemParticles(world, pos, r);
                break;
            case TrialMessage.DETECT_PLAYER_OMINOUS:
                play(world, pos, "block.trial_spawner.detect_player", 1.0F, (r.nextFloat() - r.nextFloat()) * 0.2F + 1.0F);
                addDetectPlayerParticles(world, pos, r, data, detectionOminous);
                break;
            case TrialMessage.BECOME_OMINOUS:
                play(world, pos, "block.trial_spawner.ominous_activate", data == 0 ? 0.3F : 1.0F, (r.nextFloat() - r.nextFloat()) * 0.2F + 1.0F);
                addDetectPlayerParticles(world, pos, r, 0, detectionOminous);
                for (int i = 0; i < 20; i++) {
                    double x = pos.getX() + 0.5 + (r.nextDouble() - 0.5) * 2.0;
                    double y = pos.getY() + 0.5 + (r.nextDouble() - 0.5) * 2.0;
                    double z = pos.getZ() + 0.5 + (r.nextDouble() - 0.5) * 2.0;
                    double vx = r.nextGaussian() * 0.02;
                    double vy = r.nextGaussian() * 0.02;
                    double vz = r.nextGaussian() * 0.02;
                    add(new ParticleTrial(world, ParticleTrial.Kind.SPELL, trialOmen, x, y, z, vx, vy, vz, 1.0F));
                    flameParticle(world, soulFireFlame, 1.0F, x, y, z, vx, vy, vz);
                }
                break;
            case TrialMessage.SPAWN_ITEM:
                play(world, pos, "block.trial_spawner.spawn_item", 1.0F, (r.nextFloat() - r.nextFloat()) * 0.2F + 1.0F);
                addSpawnParticles(world, pos, r, data == 1 ? soulFireFlame : flame);
                break;
            default:
                break;
        }
    }

    private static void play(World world, BlockPos pos, String event, float volume, float pitch) {
        world.playSound(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, Content.sound(event), SoundCategory.BLOCKS, volume, pitch, true);
    }

    private static void add(ParticleTrial p) {
        Minecraft.getMinecraft().effectRenderer.addEffect(p);
    }

    private static void flameParticle(World world, TextureAtlasSprite[] sprite, float scale, double x, double y, double z, double vx, double vy, double vz) {
        add(new ParticleTrial(world, ParticleTrial.Kind.FLAME, sprite, x, y, z, vx, vy, vz, scale));
    }

    private static void addSpawnParticles(World world, BlockPos pos, Random r, TextureAtlasSprite[] sprite) {
        for (int i = 0; i < 20; i++) {
            double x = pos.getX() + 0.5 + (r.nextDouble() - 0.5) * 2.0;
            double y = pos.getY() + 0.5 + (r.nextDouble() - 0.5) * 2.0;
            double z = pos.getZ() + 0.5 + (r.nextDouble() - 0.5) * 2.0;
            world.spawnParticle(EnumParticleTypes.SMOKE_NORMAL, x, y, z, 0.0, 0.0, 0.0);
            flameParticle(world, sprite, 1.0F, x, y, z, 0.0, 0.0, 0.0);
        }
    }

    private static void addDetectPlayerParticles(World world, BlockPos pos, Random r, int players, TextureAtlasSprite[] sprites) {
        for (int i = 0; i < 30 + Math.min(players, 10) * 5; i++) {
            double ox = (2.0F * r.nextFloat() - 1.0F) * 0.65;
            double oz = (2.0F * r.nextFloat() - 1.0F) * 0.65;
            add(new ParticleTrial(world, ParticleTrial.Kind.DETECTION, sprites, pos.getX() + 0.5 + ox, pos.getY() + 0.1 + r.nextFloat() * 0.8,
                    pos.getZ() + 0.5 + oz, 0.0, 0.0, 0.0, 1.0F));
        }
    }

    private static void addEjectItemParticles(World world, BlockPos pos, Random r) {
        for (int i = 0; i < 20; i++) {
            double x = pos.getX() + 0.4 + r.nextDouble() * 0.2;
            double y = pos.getY() + 0.4 + r.nextDouble() * 0.2;
            double z = pos.getZ() + 0.4 + r.nextDouble() * 0.2;
            double vx = r.nextGaussian() * 0.02;
            double vy = r.nextGaussian() * 0.02;
            double vz = r.nextGaussian() * 0.02;
            flameParticle(world, flame, 0.5F, x, y, z, vx, vy, vz * 0.25);
            world.spawnParticle(EnumParticleTypes.SMOKE_NORMAL, x, y, z, vx, vy, vz);
        }
    }

    // ------------------------------------------------------------------------------------------
    // Ticks
    // ------------------------------------------------------------------------------------------

    /** 1.21's {@code TrialSpawner.tickClient}: the phase's particles, the mob's spin, the crackle. */
    public static void tickSpawner(TileEntityTrialSpawner te, boolean ominous) {
        World world = te.getWorld();
        if (flame[0] == null) {
            return;
        }
        TrialSpawnerPhase phase = te.getPhase();
        BlockPos pos = te.getPos();
        Random r = world.rand;
        Vec3d center = new Vec3d(pos).add(0.5, 0.5, 0.5);
        switch (phase.particles) {
            case SMALL_FLAMES:
                if (r.nextInt(2) == 0) {
                    Vec3d v = offsetRandom(center, r, 0.9F);
                    flameParticle(world, ominous ? soulFireFlame : flame, ominous ? 1.0F : 0.5F, v.x, v.y, v.z, 0.0, 0.0, 0.0);
                }
                break;
            case FLAMES_AND_SMOKE: {
                Vec3d v = offsetRandom(center, r, 1.0F);
                world.spawnParticle(EnumParticleTypes.SMOKE_NORMAL, v.x, v.y, v.z, 0.0, 0.0, 0.0);
                flameParticle(world, ominous ? soulFireFlame : flame, 1.0F, v.x, v.y, v.z, 0.0, 0.0, 0.0);
                break;
            }
            case SMOKE_INSIDE_AND_TOP_FACE: {
                Vec3d v = offsetRandom(center, r, 0.9F);
                if (r.nextInt(3) == 0) {
                    world.spawnParticle(EnumParticleTypes.SMOKE_NORMAL, v.x, v.y, v.z, 0.0, 0.0, 0.0);
                }
                if (world.getTotalWorldTime() % 20L == 0L) {
                    int n = r.nextInt(4) + 20;
                    for (int i = 0; i < n; i++) {
                        world.spawnParticle(EnumParticleTypes.SMOKE_NORMAL, center.x, center.y + 0.5, center.z, 0.0, 0.0, 0.0);
                    }
                }
                break;
            }
            default:
                break;
        }
        if (phase.hasSpinningMob()) {
            double left = Math.max(0L, te.nextMobSpawnsAt() - world.getTotalWorldTime());
            te.oSpin = te.spin;
            te.spin = (te.spin + phase.spinningMobSpeed / (left + 200.0)) % 360.0;
        }
        if (phase.capableOfSpawning && r.nextFloat() <= 0.02F) {
            play(world, pos, ominous ? "block.trial_spawner.ambient_ominous" : "block.trial_spawner.ambient", r.nextFloat() * 0.25F + 0.75F,
                    r.nextFloat() + 0.5F);
        }
    }

    /** 1.21's {@code VaultBlockEntity.Client.tick}. */
    public static void tickVault(TileEntityVault te, boolean ominous) {
        World world = te.getWorld();
        if (flame[0] == null) {
            return;
        }
        BlockPos pos = te.getPos();
        Random r = world.rand;
        if (world.getTotalWorldTime() % 20L == 0L) {
            emitConnectionParticles(world, pos, te);
        }
        boolean active = !te.getDisplayItem().isEmpty();
        if (r.nextFloat() <= 0.5F) {
            Vec3d v = randomPosInsideCage(pos, r);
            world.spawnParticle(EnumParticleTypes.SMOKE_NORMAL, v.x, v.y, v.z, 0.0, 0.0, 0.0);
            if (active) {
                flameParticle(world, ominous ? soulFireFlame : flame, ominous ? 1.0F : 0.5F, v.x, v.y, v.z, 0.0, 0.0, 0.0);
            }
        }
        if (active && r.nextFloat() <= 0.02F) {
            play(world, pos, "block.vault.ambient", r.nextFloat() * 0.25F + 0.75F, r.nextFloat() + 0.5F);
        }
    }

    private static void emitConnectionParticles(World world, BlockPos pos, TileEntityVault te) {
        if (te.getConnectedPlayers().isEmpty()) {
            return;
        }
        IBlockState state = world.getBlockState(pos);
        EnumFacing facing = state.getBlock() instanceof BlockVault ? state.getValue(BlockVault.FACING) : EnumFacing.NORTH;
        Vec3d keyhole = new Vec3d(pos.getX() + 0.5 + facing.getXOffset() * 0.5, pos.getY() + 1.75, pos.getZ() + 0.5 + facing.getZOffset() * 0.5);
        double range = te.connectedParticlesRange();
        Random r = world.rand;
        for (UUID id : te.getConnectedPlayers()) {
            EntityPlayer p = world.getPlayerEntityByUUID(id);
            if (p == null || p.getPosition().distanceSq(pos) > range * range) {
                continue;
            }
            Vec3d to = new Vec3d(p.posX - keyhole.x, p.posY + p.height / 2.0F - keyhole.y, p.posZ - keyhole.z);
            int n = 2 + r.nextInt(4);
            for (int i = 0; i < n; i++) {
                Vec3d d = offsetRandom(to, r, 1.0F);
                add(new ParticleTrial(world, ParticleTrial.Kind.FLY_TOWARD, vaultConnection, keyhole.x, keyhole.y, keyhole.z, d.x, d.y, d.z, 1.5F));
            }
        }
    }

    /** 1.21's {@code OminousItemSpawner.addParticles}, every 5 ticks. */
    public static void ominousSpawning(EntityOminousItemSpawner e) {
        if (flame[0] == null) {
            return;
        }
        Random r = e.world.rand;
        int n = 1 + r.nextInt(3);
        for (int i = 0; i < n; i++) {
            double dx = 0.4 * (r.nextGaussian() - r.nextGaussian());
            double dy = 0.4 * (r.nextGaussian() - r.nextGaussian());
            double dz = 0.4 * (r.nextGaussian() - r.nextGaussian());
            float scale = 3.0F + r.nextFloat() * 2.0F;
            add(new ParticleTrial(e.world, ParticleTrial.Kind.FLY_STRAIGHT, ominousSpawning, e.posX, e.posY, e.posZ, dx, dy, dz, scale)
                    .colors(0xFF45AEFE, 0xFFFFFFFF));
        }
    }

    private static Vec3d offsetRandom(Vec3d v, Random r, float f) {
        return v.add((r.nextFloat() - 0.5F) * f, (r.nextFloat() - 0.5F) * f, (r.nextFloat() - 0.5F) * f);
    }

    private static Vec3d randomPosInsideCage(BlockPos pos, Random r) {
        return new Vec3d(pos.getX() + 0.1 + r.nextDouble() * 0.8, pos.getY() + 0.25 + r.nextDouble() * 0.5, pos.getZ() + 0.1 + r.nextDouble() * 0.8);
    }

    // ------------------------------------------------------------------------------------------
    // Effect icons
    // ------------------------------------------------------------------------------------------

    public static void drawIcon(ResourceLocation icon, int x, int y, float alpha) {
        Minecraft.getMinecraft().getTextureManager().bindTexture(icon);
        GlStateManager.enableBlend();
        GlStateManager.color(1.0F, 1.0F, 1.0F, alpha);
        Gui.drawModalRectWithCustomSizedTexture(x, y, 0.0F, 0.0F, 18, 18, 18.0F, 18.0F);
        GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
    }
}
