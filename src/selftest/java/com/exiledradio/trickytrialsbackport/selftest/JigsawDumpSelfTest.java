package com.exiledradio.trickytrialsbackport.selftest;

import com.exiledradio.backportcore.locate.StructureLocate;
import com.exiledradio.backportcore.structure.JigsawPiece;
import com.exiledradio.backportcore.structure.StructureRandoms;
import com.exiledradio.backportcore.structure.TerrainAdaptation;
import com.exiledradio.trickytrialsbackport.TrickyTrialsBackport;
import com.exiledradio.trickytrialsbackport.chambers.ChamberLayout;
import com.exiledradio.trickytrialsbackport.chambers.ChamberPiece;
import com.exiledradio.trickytrialsbackport.chambers.TrialChambersStructure;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityList;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.WorldServer;
import net.minecraft.world.gen.structure.StructureBoundingBox;

import java.io.File;
import java.io.PrintWriter;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;
import java.util.function.IntBinaryOperator;

/**
 * The trial chambers' layout, piece for piece, on several seeds (in 1.21's heights), and the blocks of chambers
 * built in this world, written to {@code jigsawdump-chambers.txt} with a digest of each, so a change to the placer
 * can be checked against the layout and the blocks before it ({@code -Pselftest=jigsawdump}, a fresh world each
 * time; not part of {@code all}). With a file {@code jigsawdump-full} beside the world, every block is written too.
 */
public final class JigsawDumpSelfTest implements SelfTest {

    static final long[] SEEDS = {1013L, 0L, 42L, -5872306475386390532L, 123456789L, 7L};

    @Override
    public String name() {
        return "jigsawdump";
    }

    @Override
    public void run(Check check) throws Exception {
        WorldServer world = check.world;
        File file = world.getMinecraftServer().getFile("jigsawdump-chambers.txt");
        PrintWriter out = new PrintWriter(file, "UTF-8");
        MessageDigest layoutDigest = MessageDigest.getInstance("SHA-1");
        int layouts = 0, pieces = 0;
        for (long seed : SEEDS) {
            for (int i = -4; i <= 4; i++) {
                for (int j = -4; j <= 4; j++) {
                    int cx = i * 37 + 3, cz = j * 41 - 5;
                    List<ChamberPiece> laid = ChamberLayout.assemble(seed, cx, cz, StructureRandoms.start(seed, cx, cz));
                    line(out, layoutDigest, "L " + seed + " " + cx + "," + cz + " " + laid.size());
                    layouts += laid.isEmpty() ? 0 : 1;
                    for (ChamberPiece p : laid) {
                        pieces++;
                        line(out, layoutDigest, "  " + describe(p));
                    }
                }
            }
        }
        String layoutHash = hex(layoutDigest.digest());
        TrickyTrialsBackport.LOGGER.info("SELFTEST jigsawdump chambers layout: {} layouts, {} pieces, digest {}", layouts, pieces, layoutHash);
        check.ok(layouts > 400, "chamber layouts dumped: " + layouts);

        // This world's trial chambers, built: the nearest to spawn and to two points further out.
        MessageDigest blockDigest = MessageDigest.getInstance("SHA-1");
        int blocks = 0, built = 0;
        for (BlockPos from : new BlockPos[] {BlockPos.ORIGIN, new BlockPos(3000, 0, -3000), new BlockPos(-4000, 0, 2500)}) {
            StructureLocate.Found f = StructureLocate.nearest(world, from, "minecraft:trial_chambers");
            if (f == null) {
                line(out, blockDigest, "W from " + from + " none");
                continue;
            }
            int cx = f.chunk.x, cz = f.chunk.z;
            built++;
            blocks += built(world, out, blockDigest, "W " + cx + "," + cz, new ArrayList<JigsawPiece>(TrialChambersStructure.layout(world, cx, cz)));
        }
        String blockHash = hex(blockDigest.digest());
        TrickyTrialsBackport.LOGGER.info("SELFTEST jigsawdump chambers blocks: {} built, {} blocks, digest {}", built, blocks, blockHash);
        out.println("layout " + layoutHash);
        out.println("blocks " + blockHash);
        out.close();
        check.ok(built >= 2, "trial chambers built: " + built);
    }

    static String describe(JigsawPiece p) {
        int[] j = p instanceof TerrainAdaptation.AdaptingPiece ? ((TerrainAdaptation.AdaptingPiece) p).junctions() : new int[0];
        return p.getClass().getSimpleName() + " " + p.element() + " pos=" + p.position() + " rot=" + p.rotation() + " gld=" + p.groundLevelDelta()
                + " box=" + p.getBoundingBox() + " pivot=" + p.pivot() + " j=" + Arrays.toString(j);
    }

    /** Builds the chunks under the pieces (and a ring for population), then writes every block, tile and entity in them. */
    static int built(WorldServer world, PrintWriter out, MessageDigest all, String head, List<JigsawPiece> laid) throws Exception {
        if (laid.isEmpty()) {
            line(out, all, head + " no pieces");
            return 0;
        }
        StructureBoundingBox box = new StructureBoundingBox(laid.get(0).getBoundingBox());
        for (JigsawPiece p : laid) {
            box.expandTo(p.getBoundingBox());
        }
        for (int x = (box.minX >> 4) - 1; x <= (box.maxX >> 4) + 2; x++) {
            for (int z = (box.minZ >> 4) - 1; z <= (box.maxZ >> 4) + 2; z++) {
                world.getChunk(x, z);
            }
        }
        line(out, all, head + " pieces " + laid.size() + " box " + box);
        for (JigsawPiece p : laid) {
            line(out, all, "  " + describe(p));
        }
        int n = 0;
        // With a file jigsawdump-full next to the world, every block is written too (jigsawdump-blocks.txt).
        PrintWriter full = world.getMinecraftServer().getFile("jigsawdump-full").exists()
                ? new PrintWriter(new java.io.FileOutputStream(world.getMinecraftServer().getFile("jigsawdump-blocks.txt"), true)) : null;
        BlockPos.MutableBlockPos at = new BlockPos.MutableBlockPos();
        for (int cx = box.minX >> 4; cx <= box.maxX >> 4; cx++) {
            for (int cz = box.minZ >> 4; cz <= box.maxZ >> 4; cz++) {
                MessageDigest column = MessageDigest.getInstance("SHA-1");
                int x0 = Math.max(box.minX, cx << 4), x1 = Math.min(box.maxX, (cx << 4) + 15);
                int z0 = Math.max(box.minZ, cz << 4), z1 = Math.min(box.maxZ, (cz << 4) + 15);
                for (int y = box.minY; y <= box.maxY; y++) {
                    for (int x = x0; x <= x1; x++) {
                        for (int z = z0; z <= z1; z++) {
                            at.setPos(x, y, z);
                            IBlockState state = world.getBlockState(at);
                            StringBuilder b = new StringBuilder(state.toString());
                            TileEntity te = world.getTileEntity(at);
                            if (te != null) {
                                b.append(' ').append(te.writeToNBT(new NBTTagCompound()));
                            }
                            column.update(b.toString().getBytes("UTF-8"));
                            if (full != null) {
                                full.println(x + " " + y + " " + z + " " + b);
                            }
                            column.update((byte) '\n');
                            n++;
                        }
                    }
                }
                List<String> entities = new ArrayList<String>();
                for (Entity e : world.getEntitiesWithinAABB(Entity.class, new AxisAlignedBB(x0, box.minY, z0, x1 + 1, box.maxY + 1, z1 + 1))) {
                    entities.add(EntityList.getKey(e) + String.format(" %.2f %.2f %.2f", e.posX, e.posY, e.posZ));
                }
                java.util.Collections.sort(entities);
                for (String e : entities) {
                    column.update(e.getBytes("UTF-8"));
                }
                line(out, all, "  chunk " + cx + "," + cz + " " + hex(column.digest()) + " entities " + entities);
            }
        }
        if (full != null) {
            full.close();
        }
        return n;
    }

    static void line(PrintWriter out, MessageDigest digest, String s) throws Exception {
        out.println(s);
        digest.update(s.getBytes("UTF-8"));
        digest.update((byte) '\n');
    }

    static String hex(byte[] b) {
        StringBuilder s = new StringBuilder();
        for (byte x : b) {
            s.append(String.format("%02x", x));
        }
        return s.toString();
    }
}
