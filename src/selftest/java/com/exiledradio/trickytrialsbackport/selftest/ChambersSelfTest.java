package com.exiledradio.trickytrialsbackport.selftest;

import com.exiledradio.backportcore.locate.StructureLocate;
import com.exiledradio.backportcore.structure.JigsawElement;
import com.exiledradio.backportcore.structure.JigsawPools;
import com.exiledradio.backportcore.structure.ModernTemplate;
import com.exiledradio.backportcore.structure.ScatteredPlacement;
import com.exiledradio.backportcore.structure.ScatteredStructure;
import com.exiledradio.backportcore.structure.StructureRandoms;
import com.exiledradio.backportcore.structure.Structures;
import com.exiledradio.backportcore.terrain.WorldHeights;
import com.exiledradio.trickytrialsbackport.chambers.ChamberLayout;
import com.exiledradio.trickytrialsbackport.chambers.ChamberPiece;
import com.exiledradio.trickytrialsbackport.chambers.ChamberSettings;
import com.exiledradio.trickytrialsbackport.chambers.ChambersContent;
import com.exiledradio.trickytrialsbackport.chambers.TrialChambersStructure;
import com.exiledradio.trickytrialsbackport.common.Content;
import com.exiledradio.trickytrialsbackport.trial.BlockVault;
import com.exiledradio.trickytrialsbackport.trial.TileEntityTrialSpawner;
import com.exiledradio.trickytrialsbackport.trial.TileEntityVault;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.minecraft.advancements.Advancement;
import net.minecraft.block.Block;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.EnumCreatureType;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.tileentity.TileEntityLockableLoot;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.world.WorldServer;
import net.minecraft.world.biome.Biome;
import net.minecraft.world.gen.structure.StructureBoundingBox;
import net.minecraft.world.gen.structure.StructureComponent;
import net.minecraft.world.gen.structure.StructureStart;
import net.minecraftforge.event.ForgeEventFactory;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.TreeSet;

/**
 * Stream 10: trial chambers against vanilla 1.21.11 ({@code tools/chambers/oracle121.txt}: a real 1.21.11 server's saved
 * starts at every candidate chunk of sixteen regions, three seeds), the data files, and chambers built in this world:
 * the spawners, vaults and containers inside, the heights, spawns, /locate and the advancement.
 */
public final class ChambersSelfTest implements SelfTest {

    @Override
    public String name() {
        return "chambers";
    }

    @Override
    public void run(Check c) throws Exception {
        settings(c);
        templates(c);
        vanilla(c);
        world(c);
    }

    // ---- vanilla's files

    private void settings(Check c) {
        ChamberSettings s = ChamberSettings.get();
        c.eq(34, s.grid.spacing(), "structure set spacing (vanilla's file)");
        c.eq(12, s.grid.separation(), "structure set separation");
        c.eq(94251327, s.grid.salt(), "structure set salt");
        c.eq(20, s.maxDepth, "jigsaw size");
        c.eq(116, s.maxDistanceHorizontal, "max distance from center");
        c.eq(-40, s.startMinY, "start height min");
        c.eq(-20, s.startMaxY, "start height max");
        c.eq(10, s.paddingBottom, "dimension padding");
        c.eq(3, s.aliases.size(), "pool aliases: the ranged group, melee and small melee");
        c.ok(!s.biomes.contains("minecraft:deep_dark") && s.biomes.contains("minecraft:plains") && s.biomes.contains("minecraft:lush_caves"),
                "the biome tag: every overworld biome but the deep dark");
        c.ok(ChambersContent.enabled(), "the stream is on");
    }

    /** Every template of every pool reachable from the start loads, and what its blocks become here. */
    private void templates(Check c) {
        TreeSet<String> pools = new TreeSet<String>();
        List<String> todo = new ArrayList<String>();
        todo.add(ChamberSettings.get().startPool.toString());
        // Alias targets are reached only through the aliases, which the pool files name.
        for (String alias : aliasTargets()) {
            todo.add(alias);
        }
        TreeSet<String> templates = new TreeSet<String>();
        int empty = 0;
        while (!todo.isEmpty()) {
            String id = todo.remove(todo.size() - 1);
            if (!pools.add(id) || id.equals("minecraft:empty")) {
                continue;
            }
            JigsawPools.Pool pool = JigsawPools.pool(new ResourceLocation(id));
            if (pool == null) {
                if (!aliasNames().contains(id)) {
                    c.fail("pool " + id + " is missing");
                }
                continue;
            }
            todo.add(pool.fallback().toString());
            for (JigsawElement e : pool.elements()) {
                if (e.template() == null) {
                    empty++;
                    continue;
                }
                if (templates.add(e.template().toString())) {
                    for (JigsawElement.Connector j : e.connectors(BlockPos.ORIGIN, net.minecraft.util.Rotation.NONE)) {
                        todo.add(full(j.pool));
                    }
                }
            }
        }
        c.ok(pools.size() >= 40, "pools reached from the start: " + pools.size());
        c.ok(templates.size() > 150, "templates the pools name: " + templates.size() + " (" + empty + " empty elements)");
        TreeMap<String, Integer> unresolved = new TreeMap<String, Integer>();
        int loaded = 0;
        for (String t : templates) {
            ModernTemplate m = ModernTemplate.load(JigsawPools.templateId(new ResourceLocation(t)));
            if (m.isEmpty()) {
                c.fail("template " + t + " is empty or missing");
                continue;
            }
            loaded++;
            for (String u : m.unresolvedBlocks()) {
                unresolved.merge(u, 1, Integer::sum);
            }
        }
        c.eq(templates.size(), loaded, "every template loads");
        TrickyLog.info("chambers: " + pools.size() + " pools, " + templates.size() + " templates");
        // With no other family mod installed, their blocks get Backport Core's stand-ins.
        TrickyLog.info("chambers: blocks nothing installed provides: " + unresolved);
        for (String ours : new String[] {"minecraft:tuff_bricks", "minecraft:chiseled_tuff", "minecraft:polished_tuff", "minecraft:waxed_copper_bulb",
                "minecraft:waxed_copper_grate", "minecraft:waxed_oxidized_cut_copper_stairs", "minecraft:trial_spawner", "minecraft:vault",
                "minecraft:waxed_copper_door", "minecraft:waxed_oxidized_copper_trapdoor", "minecraft:waxed_chiseled_copper"}) {
            c.ok(!unresolved.containsKey(ours), ours + " is this mod's block");
        }
    }

    private static List<String> aliasTargets() {
        List<String> out = new ArrayList<String>();
        JsonObject structure = readJson("/assets/trickytrialsbackport/worldgen/structure/trial_chambers.json");
        collect(structure.get("pool_aliases"), out, "target", "data");
        return out;
    }

    private static List<String> aliasNames() {
        List<String> out = new ArrayList<String>();
        collect(readJson("/assets/trickytrialsbackport/worldgen/structure/trial_chambers.json").get("pool_aliases"), out, "alias");
        return out;
    }

    private static void collect(JsonElement e, List<String> out, String... keys) {
        if (e == null) {
            return;
        }
        if (e.isJsonArray()) {
            for (JsonElement x : e.getAsJsonArray()) {
                collect(x, out, keys);
            }
        } else if (e.isJsonObject()) {
            for (Map.Entry<String, JsonElement> m : e.getAsJsonObject().entrySet()) {
                boolean wanted = false;
                for (String k : keys) {
                    wanted |= k.equals(m.getKey());
                }
                if (wanted && m.getValue().isJsonPrimitive()) {
                    out.add(full(m.getValue().getAsString()));
                } else {
                    collect(m.getValue(), out, keys);
                }
            }
        }
    }

    private static JsonObject readJson(String path) {
        try (InputStream in = ChambersSelfTest.class.getResourceAsStream(path)) {
            return new JsonParser().parse(new InputStreamReader(in, StandardCharsets.UTF_8)).getAsJsonObject();
        } catch (Exception e) {
            throw new IllegalStateException(path, e);
        }
    }

    static String full(String name) {
        return name.indexOf(':') < 0 ? "minecraft:" + name : name;
    }

    // ---- against vanilla 1.21.11

    /** One start of the oracle. */
    static final class Vanilla {
        final long seed;
        final int cx, cz;
        boolean valid;
        final List<String> pieces = new ArrayList<String>();

        Vanilla(long seed, int cx, int cz) {
            this.seed = seed;
            this.cx = cx;
            this.cz = cz;
        }
    }

    static List<Vanilla> oracle() throws Exception {
        List<Vanilla> out = new ArrayList<Vanilla>();
        File file = new File("../tools/chambers/oracle121.txt");
        long seed = 0;
        Vanilla cur = null;
        try (BufferedReader r = new BufferedReader(new InputStreamReader(new FileInputStream(file), StandardCharsets.UTF_8))) {
            String line;
            while ((line = r.readLine()) != null) {
                String[] a = line.trim().split(" ");
                if (a[0].equals("seed")) {
                    seed = Long.parseLong(a[1]);
                } else if (a[0].equals("start")) {
                    cur = new Vanilla(seed, Integer.parseInt(a[1]), Integer.parseInt(a[2]));
                    cur.valid = a[3].equals("valid");
                    out.add(cur);
                } else if (a[0].equals("p") && cur != null) {
                    cur.pieces.add("minecraft:trial_chambers/" + line.trim().substring(2));
                }
            }
        }
        return out;
    }

    static String describe(ChamberPiece p, int dy) {
        BlockPos pos = p.position();
        return p.template() + " " + pos.getX() + " " + (pos.getY() - dy) + " " + pos.getZ() + " " + p.rotation().name();
    }

    private void vanilla(Check c) throws Exception {
        List<Vanilla> starts = oracle();
        c.ok(starts.size() >= 48, "the oracle has " + starts.size() + " candidate chunks");
        ScatteredPlacement grid = ChamberSettings.get().grid;
        int gridSame = 0, stubs = 0, valid = 0, same = 0, pieces = 0, piecesSame = 0;
        for (Vanilla v : starts) {
            ChunkPos ours = grid.candidate(v.seed, v.cx, v.cz);
            if (ours.x == v.cx && ours.z == v.cz && grid.isCandidate(v.seed, v.cx, v.cz)) {
                gridSame++;
            } else {
                c.fail("seed " + v.seed + ": vanilla's candidate " + v.cx + "," + v.cz + ", ours " + ours);
            }
            if (!v.valid) {
                continue;
            }
            valid++;
            ChamberLayout.Stub stub = ChamberLayout.stub(v.seed, v.cx, v.cz, StructureRandoms.start(v.seed, v.cx, v.cz));
            if (stub != null) {
                stubs++;
            }
            List<ChamberPiece> layout = ChamberLayout.assemble(v.seed, v.cx, v.cz, StructureRandoms.start(v.seed, v.cx, v.cz));
            pieces += v.pieces.size();
            int match = 0;
            for (int i = 0; i < Math.min(layout.size(), v.pieces.size()); i++) {
                if (describe(layout.get(i), 0).equals(v.pieces.get(i))) {
                    match++;
                } else if (match == i) {
                    c.fail("seed " + v.seed + " start " + v.cx + "," + v.cz + " piece " + i + ": vanilla " + v.pieces.get(i) + ", ours "
                            + describe(layout.get(i), 0));
                }
            }
            piecesSame += match;
            if (match == v.pieces.size() && layout.size() == v.pieces.size()) {
                same++;
            } else {
                c.fail("seed " + v.seed + " start " + v.cx + "," + v.cz + ": " + match + " of " + v.pieces.size() + " pieces, ours " + layout.size());
            }
        }
        TrickyLog.info("chambers: vanilla 1.21.11, " + starts.size() + " candidate chunks (" + gridSame + " same), " + valid + " valid starts, "
                + same + " laid out piece for piece, " + piecesSame + "/" + pieces + " pieces");
        c.eq(starts.size(), gridSame, "every candidate chunk is vanilla's (" + gridSame + "/" + starts.size() + ")");
        c.eq(valid, stubs, "every vanilla start has a stub");
        c.eq(valid, same, "every start lays out piece for piece as 1.21.11's (" + same + "/" + valid + ", " + piecesSame + "/" + pieces + " pieces)");
    }

    // ---- this world

    private void world(Check c) throws Exception {
        WorldServer world = c.world;
        ScatteredStructure s = ScatteredStructure.of(world, Structures.TRIAL_CHAMBERS);
        c.ok(s instanceof TrialChambersStructure, "trial chambers are in the overworld");
        if (!(s instanceof TrialChambersStructure)) {
            return;
        }
        boolean extended = WorldHeights.minY(world) < 0;
        c.eq(extended ? 0 : 56, TrialChambersStructure.shift(world, -40), "1.21's lowest start height moves to y " + (extended ? -40 : 16));
        c.eq(extended ? 0 : 49, TrialChambersStructure.shift(world, -20), "1.21's highest start height moves to y " + (extended ? -20 : 29));

        BlockPos found;
        try {
            found = StructureLocate.nearest(world, BlockPos.ORIGIN, "minecraft:trial_chambers").pos;
        } catch (Exception e) {
            c.fail("/locate structure minecraft:trial_chambers: " + e);
            return;
        }
        c.ok(found != null, "/locate finds trial chambers: " + found);
        if (found == null) {
            return;
        }
        List<String> said = com.exiledradio.backportcore.locate.LocateChecks.run(world.getMinecraftServer(), world, BlockPos.ORIGIN,
                "locate structure minecraft:trial_chambers");
        c.ok(!said.isEmpty() && said.get(0).contains(String.valueOf(found.getX())), "/locate structure minecraft:trial_chambers says " + said);
        int cx = found.getX() >> 4, cz = found.getZ() >> 4;
        c.ok(s.startsAt(world, cx, cz), "chambers start in the located chunk");
        String ring = com.exiledradio.backportcore.locate.LocateChecks.checkRingOrder(world, BlockPos.ORIGIN, "minecraft:trial_chambers",
                StructureLocate.nearest(world, BlockPos.ORIGIN, "minecraft:trial_chambers"));
        c.ok(ring == null, "the located chambers are today's search result" + (ring == null ? "" : ": " + ring));

        // This world's seed is in the oracle: the layout here is vanilla's, moved to this world's heights.
        ChamberLayout.Stub stub = TrialChambersStructure.stub(world, cx, cz);
        int dy = TrialChambersStructure.shift(world, stub.startY);
        List<ChamberPiece> layout = TrialChambersStructure.layout(world, cx, cz);
        for (Vanilla v : oracle()) {
            if (v.seed == world.getSeed() && v.cx == cx && v.cz == cz) {
                int match = 0;
                for (int i = 0; i < Math.min(layout.size(), v.pieces.size()); i++) {
                    match += describe(layout.get(i), dy).equals(v.pieces.get(i)) ? 1 : 0;
                }
                c.eq(v.pieces.size(), match, "this world's chambers are vanilla's, moved " + dy + " up (" + match + "/" + v.pieces.size() + ")");
            }
        }
        StructureBoundingBox all = new StructureBoundingBox(layout.get(0).getBoundingBox());
        for (ChamberPiece p : layout) {
            all.expandTo(p.getBoundingBox());
        }
        int startY = stub.startY + dy;
        c.ok(extended ? startY >= -40 && startY <= -20 : startY >= 16 && startY <= 29, "the start height is y " + startY);
        c.ok(all.minY >= WorldHeights.minY(world) + 1 && all.maxY < WorldHeights.maxYExclusive(world), "the chambers sit in y " + all.minY + "-" + all.maxY);

        // Build it: every chunk it touches, and a ring around so each is populated.
        for (int x = (all.minX >> 4) - 1; x <= (all.maxX >> 4) + 2; x++) {
            for (int z = (all.minZ >> 4) - 1; z <= (all.maxZ >> 4) + 2; z++) {
                world.getChunk(x, z);
            }
        }
        StructureStart saved = com.exiledradio.backportcore.locate.LocateChecks.savedStart(s, cx, cz);
        c.ok(saved instanceof TrialChambersStructure.Start && saved.isSizeableStructure(), "the start is laid out and saved");
        if (saved != null) {
            int n = 0;
            for (StructureComponent comp : saved.getComponents()) {
                n++;
            }
            c.eq(layout.size(), n, "the saved start has the layout's pieces");
        }
        contents(c, world, all, layout);
    }

    private void contents(Check c, WorldServer world, StructureBoundingBox all, List<ChamberPiece> layout) {
        Block spawnerBlock = Block.getBlockFromName(Content.MOD_ID + ":trial_spawner");
        Block vaultBlock = Block.getBlockFromName(Content.MOD_ID + ":vault");
        Map<String, Integer> configs = new TreeMap<String, Integer>();
        Map<String, Integer> tables = new TreeMap<String, Integer>();
        List<String> vaults = new ArrayList<String>();
        int spawners = 0, spawnersConfigured = 0, bulbs = 0, tuff = 0;
        BlockPos inside = null;
        TileEntityLockableLoot chest = null;
        for (BlockPos p : BlockPos.getAllInBoxMutable(new BlockPos(all.minX, all.minY, all.minZ), new BlockPos(all.maxX, all.maxY, all.maxZ))) {
            IBlockState st = world.getBlockState(p);
            Block b = st.getBlock();
            String name = String.valueOf(b.getRegistryName());
            if (name.endsWith("copper_bulb")) {
                bulbs++;
            } else if (name.endsWith("tuff_bricks")) {
                tuff++;
            }
            if (b == spawnerBlock) {
                spawners++;
                TileEntity te = world.getTileEntity(p);
                if (te instanceof TileEntityTrialSpawner) {
                    NBTTagCompound tag = te.writeToNBT(new NBTTagCompound());
                    String normal = tag.getString("normal_config");
                    String ominous = tag.getString("ominous_config");
                    if (normal.startsWith("minecraft:trial_chamber/") && ominous.equals(normal.replace("/normal", "/ominous"))) {
                        spawnersConfigured++;
                    }
                    configs.merge(normal, 1, Integer::sum);
                }
            } else if (b == vaultBlock) {
                TileEntity te = world.getTileEntity(p);
                if (te instanceof TileEntityVault) {
                    NBTTagCompound cfg = te.writeToNBT(new NBTTagCompound()).getCompoundTag("config");
                    vaults.add((st.getValue(BlockVault.OMINOUS) ? "ominous " : "normal ") + cfg.getString("loot_table") + " key "
                            + cfg.getCompoundTag("key_item").getString("id"));
                }
            } else {
                TileEntity te = world.getTileEntity(p);
                if (te instanceof TileEntityLockableLoot) {
                    String t = te.writeToNBT(new NBTTagCompound()).getString("LootTable");
                    tables.merge(name + " " + t, 1, Integer::sum);
                    if (chest == null && t.startsWith(Content.MOD_ID + ":chambers/chests/")) {
                        chest = (TileEntityLockableLoot) te;
                    }
                }
            }
            if (inside == null && st.getMaterial() == net.minecraft.block.material.Material.AIR && ChambersContent.insidePiece(world, p)) {
                inside = p.toImmutable();
            }
        }
        TrickyLog.info("chambers: spawner configs " + configs + "; containers " + tables + "; vaults " + vaults);
        c.ok(tuff > 1000, "tuff bricks: " + tuff);
        c.ok(bulbs > 10, "copper bulbs: " + bulbs);
        c.ok(spawners >= 4, "trial spawners: " + spawners);
        c.eq(spawners, spawnersConfigured, "every trial spawner has its 1.21 normal and ominous configs");
        // The pool aliases pick one mob per kind for the whole structure.
        Map<String, String> kinds = new TreeMap<String, String>();
        boolean oneEach = true;
        for (String cfg : configs.keySet()) {
            String[] parts = cfg.split("/");
            if (parts.length >= 4) {
                String kind = parts[1];
                String mob = parts[2];
                String had = kinds.put(kind, mob);
                oneEach &= had == null || had.equals(mob);
            }
        }
        TrickyLog.info("chambers: " + layout.size() + " pieces, y " + all.minY + "-" + all.maxY + ", " + spawners + " trial spawners, "
                + vaults.size() + " vaults, " + bulbs + " copper bulbs");
        c.ok(oneEach, "one mob per spawner kind, as the aliases chose: " + kinds);
        String ranged = kinds.get("ranged"), slow = kinds.get("slow_ranged");
        c.ok(ranged == null || slow == null || ranged.equals(slow), "ranged and slow ranged spawners are one group: " + ranged + "/" + slow);
        boolean vaultsOk = !vaults.isEmpty();
        for (String v : vaults) {
            vaultsOk &= v.startsWith("ominous") ? v.contains("reward_ominous") && v.endsWith(":ominous_trial_key")
                    : v.contains("chests/trial_chambers/reward") && !v.contains("ominous") && v.endsWith(":trial_key");
        }
        c.ok(vaultsOk, "vaults and ominous vaults have 1.21's keys and tables: " + vaults);
        boolean remapped = true;
        for (String t : tables.keySet()) {
            remapped &= !t.contains("minecraft:chests/trial_chambers") && !t.contains("minecraft:dispensers/trial_chambers");
        }
        c.ok(!tables.isEmpty() && remapped, "containers name our tables for 1.21's: " + tables);
        if (chest != null) {
            chest.fillWithLoot(null);
            int stacks = 0;
            for (int i = 0; i < chest.getSizeInventory(); i++) {
                stacks += chest.getStackInSlot(i).isEmpty() ? 0 : 1;
            }
            c.ok(stacks > 0, "a chamber chest fills from 1.21's table: " + stacks + " stacks");
        } else {
            c.fail("no chamber chest found");
        }
        List<EntityItem> drops = world.getEntitiesWithinAABB(EntityItem.class,
                new AxisAlignedBB(all.minX, all.minY, all.minZ, all.maxX + 1, all.maxY + 1, all.maxZ + 1));
        c.eq(0, drops.size(), "nothing popped off while the chambers were built");
        c.ok(inside != null, "a place inside a piece: " + inside);
        if (inside != null) {
            List<Biome.SpawnListEntry> list = new ArrayList<Biome.SpawnListEntry>(world.getBiome(inside).getSpawnableList(EnumCreatureType.MONSTER));
            boolean had = !list.isEmpty();
            List<Biome.SpawnListEntry> after = ForgeEventFactory.getPotentialSpawns(world, EnumCreatureType.MONSTER, inside, list);
            c.ok(had && (after == null || after.isEmpty()), "no natural monsters inside a piece");
            BlockPos outside = new BlockPos(all.maxX + 40, 70, all.maxZ + 40);
            List<Biome.SpawnListEntry> out = ForgeEventFactory.getPotentialSpawns(world, EnumCreatureType.MONSTER, outside,
                    new ArrayList<Biome.SpawnListEntry>(world.getBiome(outside).getSpawnableList(EnumCreatureType.MONSTER)));
            c.ok(out == null || !out.isEmpty() || world.getBiome(outside).getSpawnableList(EnumCreatureType.MONSTER).isEmpty(),
                    "natural monsters outside it as before");
        }
        Advancement adv = world.getMinecraftServer().getAdvancementManager().getAdvancement(Content.id("adventure/minecraft_trials_edition"));
        Advancement lock = world.getMinecraftServer().getAdvancementManager().getAdvancement(Content.id("adventure/under_lock_and_key"));
        c.ok(adv != null && lock != null && lock.getParent() == adv, "Minecraft: Trial(s) Edition is Under Lock and Key's parent");
    }

    /** The self-test log line, through the mod's logger. */
    static final class TrickyLog {
        static void info(String s) {
            com.exiledradio.trickytrialsbackport.TrickyTrialsBackport.LOGGER.info("SELFTEST " + s);
        }
    }
}
