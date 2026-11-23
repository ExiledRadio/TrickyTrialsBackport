package com.exiledradio.trickytrialsbackport.selftest;

import com.exiledradio.backportcore.structure.ScatteredStructure;
import com.exiledradio.backportcore.structure.Structures;
import com.exiledradio.backportcore.terrain.TerrainHeights;
import com.exiledradio.trickytrialsbackport.chambers.ChamberLayout;
import com.exiledradio.trickytrialsbackport.chambers.ChamberPiece;
import com.exiledradio.trickytrialsbackport.chambers.ChamberShell;
import com.exiledradio.trickytrialsbackport.chambers.TrialChambersStructure;
import net.minecraft.block.material.Material;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.world.WorldServer;
import net.minecraft.world.gen.structure.StructureBoundingBox;
import net.minecraft.world.gen.structure.StructureComponent;

import java.util.ArrayList;
import java.util.List;

/**
 * 1.21's {@code encapsulate} ({@link ChamberShell}): the sum against 1.21.11's Beardifier, and a chamber that rises
 * out of the sea bed (1.21's layouts are up to 80 blocks tall, more than 1.12.2's deep sea beds leave under them) is
 * wrapped in rock, not left bare in the water. Found from the bare terrain (the nearest chambers rising 4 or more
 * blocks out of a sea bed), so it works in any seed with sea.
 */
public final class ChamberShellSelfTest implements SelfTest {

    @Override
    public String name() {
        return "chambershell";
    }

    @Override
    public void run(Check c) throws Exception {
        sums(c);
        world(c);
    }

    /** 1.21.11's Beardifier, ENCAPSULATE: getBuryContribution(dx / 2, dy / 2, dz / 2) * 0.8 per rigid piece. */
    private void sums(Check c) {
        StructureComponent p = new StructureComponent(0) {
            {
                this.boundingBox = new StructureBoundingBox(0, 0, 0, 9, 9, 9);
            }

            @Override
            protected void writeStructureToNBT(net.minecraft.nbt.NBTTagCompound tag) {
            }

            @Override
            protected void readStructureFromNBT(net.minecraft.nbt.NBTTagCompound tag, net.minecraft.world.gen.structure.template.TemplateManager m) {
            }

            @Override
            public boolean addComponentParts(net.minecraft.world.World w, java.util.Random rand, StructureBoundingBox box) {
                return true;
            }
        };
        List<StructureComponent> one = new ArrayList<StructureComponent>();
        one.add(p);
        c.ok(Math.abs(ChamberShell.density(one, 5, 5, 5) - 0.8) < 1e-9, "inside a piece: 0.8");
        c.ok(Math.abs(ChamberShell.density(one, 11, 5, 5) - (1 - 1.0 / 6) * 0.8) < 1e-9, "2 blocks out: (1 - 1/6) * 0.8");
        c.ok(Math.abs(ChamberShell.density(one, 13, 13, 5) - (1 - Math.sqrt(8) / 6) * 0.8) < 1e-9, "4 out and 4 up: (1 - sqrt(8)/6) * 0.8");
        c.ok(ChamberShell.density(one, 21, 5, 5) == 0.0, "12 blocks out: nothing");
    }

    private void world(Check c) throws Exception {
        WorldServer world = c.world;
        ScatteredStructure s = ScatteredStructure.of(world, Structures.TRIAL_CHAMBERS);
        if (!(s instanceof TrialChambersStructure)) {
            c.fail("no trial chambers in the overworld");
            return;
        }
        int sp = s.placement().spacing();
        int r = 2000 / 16 / sp + 1;
        int starts = 0, poking = 0;
        ChunkPos pick = null;
        int pickOver = 0;
        long pickDist = Long.MAX_VALUE;
        for (int gx = -r; gx <= r; gx++) {
            for (int gz = -r; gz <= r; gz++) {
                ChunkPos cp = s.placement().candidate(world.getSeed(), gx * sp, gz * sp);
                if (!s.startsAt(world, cp.x, cp.z)) {
                    continue;
                }
                List<ChamberPiece> layout = TrialChambersStructure.layout(world, cp.x, cp.z);
                TerrainHeights th = TerrainHeights.forWorld(world);
                if (th == null || layout.isEmpty()) {
                    continue;
                }
                starts++;
                int over = Integer.MIN_VALUE;
                boolean water = false;
                for (ChamberPiece p : layout) {
                    StructureBoundingBox b = p.getBoundingBox();
                    for (int x = b.minX; x <= b.maxX; x += 2) {
                        for (int z = b.minZ; z <= b.maxZ; z += 2) {
                            int o = b.maxY - th.floor(x, z) + 1;
                            if (o > over) {
                                over = o;
                                water = th.isWater(x, z);
                            }
                        }
                    }
                }
                if (over > 0) {
                    poking++;
                }
                long d = (long) cp.x * cp.x + (long) cp.z * cp.z;
                if (water && over >= 4 && d < pickDist) {
                    pick = cp;
                    pickOver = over;
                    pickDist = d;
                }
            }
        }
        ChambersSelfTest.TrickyLog.info("chambershell: " + starts + " starts within 2000 blocks, " + poking + " rise above the bare terrain");
        if (pick == null) {
            ChambersSelfTest.TrickyLog.info("chambershell: no chamber here rises out of a sea bed; the wrapped-in-rock check is skipped");
            return;
        }
        // Seed 1718 (Default), before chambers were buried (bury, 2026-11-23): the chambers at block 288,-288, y 18-66, had
        // their roof at the sea's surface and 110 water blocks against them; with the shell, none. Buried, no start
        // there rises out of a sea bed (Default or Caves & Cliffs' extended world), and this is skipped.
        wrapped(c, world, pick, pickOver);
        // Seed 1718, Default: the chambers seen with their roof at the sea's surface, about x -330..-300, z -250..-220.
    }

    private void wrapped(Check c, WorldServer world, ChunkPos pick, int pickOver) {
        List<ChamberPiece> layout = TrialChambersStructure.layout(world, pick.x, pick.z);
        ChamberLayout.Stub stub = TrialChambersStructure.stub(world, pick.x, pick.z);
        StructureBoundingBox all = new StructureBoundingBox(layout.get(0).getBoundingBox());
        for (ChamberPiece p : layout) {
            all.expandTo(p.getBoundingBox());
        }
        ChambersSelfTest.TrickyLog.info("chambershell: chambers at block " + (pick.x << 4) + "," + (pick.z << 4) + ", start y "
                + (stub.startY + TrialChambersStructure.shift(world, stub.startY)) + ", y " + all.minY + "-" + all.maxY
                + ", " + pickOver + " blocks above the sea bed");
        for (int x = (all.minX >> 4) - 1; x <= (all.maxX >> 4) + 2; x++) {
            for (int z = (all.minZ >> 4) - 1; z <= (all.maxZ >> 4) + 2; z++) {
                world.getChunk(x, z);
            }
        }
        // Every block just outside a piece (and in none): never the sea. 1.12's carvers never cut next to water, so a cave
        // there is open air, as 1.21's carvers cut its shell too; a lake decoration may still sit in one.
        int wet = 0, faces = 0;
        String first = null;
        BlockPos.MutableBlockPos q = new BlockPos.MutableBlockPos();
        for (ChamberPiece p : layout) {
            StructureBoundingBox b = p.getBoundingBox();
            for (int x = b.minX - 1; x <= b.maxX + 1; x++) {
                for (int y = b.minY - 1; y <= b.maxY + 1; y++) {
                    for (int z = b.minZ - 1; z <= b.maxZ + 1; z++) {
                        if (b.isVecInside(q.setPos(x, y, z)) || inAny(layout, q) || y < 1 || y > 255) {
                            continue;
                        }
                        faces++;
                        if (sea(world, layout, x, y, z)) {
                            wet++;
                            if (first == null) {
                                first = p.template() + " at " + x + "," + y + "," + z;
                            }
                        }
                    }
                }
            }
        }
        c.eq(0, wet, "no sea against the chambers at " + (pick.x << 4) + "," + (pick.z << 4) + " (" + wet + " of " + faces + " blocks around them"
                + (first == null ? "" : ", first by " + first) + ")");
    }

    /** @return true if the block is water whose column reaches the sea's surface through water and pieces */
    private static boolean sea(WorldServer world, List<ChamberPiece> layout, int x, int y, int z) {
        BlockPos.MutableBlockPos p = new BlockPos.MutableBlockPos();
        if (world.getBlockState(p.setPos(x, y, z)).getMaterial() != Material.WATER) {
            return false;
        }
        for (int h = y + 1; h < world.getSeaLevel(); h++) {
            p.setPos(x, h, z);
            if (world.getBlockState(p).getMaterial() != Material.WATER && !inAny(layout, p)) {
                return false;
            }
        }
        return true;
    }

    private static boolean inAny(List<ChamberPiece> layout, BlockPos pos) {
        for (ChamberPiece p : layout) {
            if (p.getBoundingBox().isVecInside(pos)) {
                return true;
            }
        }
        return false;
    }
}
