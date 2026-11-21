package com.exiledradio.trickytrialsbackport.selftest;

import com.exiledradio.backportcore.locate.StructureLocate;
import com.exiledradio.backportcore.map.MapIcons;
import com.exiledradio.backportcore.structure.ScatteredStructure;
import com.exiledradio.backportcore.structure.Structures;
import com.exiledradio.backportcore.trade.TradeRegistry;
import com.exiledradio.trickytrialsbackport.ModConfig;
import com.exiledradio.trickytrialsbackport.chambers.ExplorerMap;
import com.exiledradio.trickytrialsbackport.chambers.TrialChambersStructure;
import com.exiledradio.trickytrialsbackport.common.Owners;
import net.minecraft.entity.passive.EntityVillager;
import net.minecraft.init.Items;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.BlockPos;
import net.minecraft.village.MerchantRecipe;
import net.minecraft.village.MerchantRecipeList;
import net.minecraft.world.WorldServer;
import net.minecraft.world.storage.MapData;
import net.minecraft.world.storage.MapDecoration;
import net.minecraftforge.fml.common.Loader;
import net.minecraftforge.fml.common.registry.VillagerRegistry;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Random;

/**
 * The trial explorer map (stream 10): today's trial chambers icon, the cartographer's journeyman trade (1.12.2's
 * career without Village &amp; Pillage, its table's pool through Backport Core with it), and a bought map that points
 * at a real trial chamber, against 1.21.11's {@code TreasureMapForEmeralds}.
 */
public final class ExplorerMapSelfTest implements SelfTest {

    @Override
    public String name() {
        return "explorermap";
    }

    @Override
    public void run(Check c) throws Exception {
        icon(c);
        lang(c);
        MerchantRecipe recipe = trade(c);
        if (recipe != null) {
            map(c, recipe.getItemToSell(), c.world);
        }
        setting(c);
    }

    private void icon(Check c) {
        MapDecoration.Type type = ExplorerMap.type();
        c.ok(type != null, "the trial chambers decoration type exists");
        if (type == null) {
            return;
        }
        c.ok(type == MapIcons.find(ExplorerMap.ICON), "it is Backport Core's shared trial_chambers type");
        c.eq(new ResourceLocation("backportcore", "textures/map/decorations/trial_chambers.png"), MapIcons.sprite(type),
                "drawn from today's sprite");
        c.ok(MapDecoration.class.getResource("/assets/backportcore/textures/map/decorations/trial_chambers.png") != null,
                "the sprite is in Backport Core's jar");
        c.ok(type.hasMapColor(), "the type colors the map item");
        c.eq(12741452, type.getMapColor(), "1.21.11's trial chambers map color");
        c.eq(MapIcons.TRIAL_CHAMBERS_COLOR, type.getMapColor(), "Core's TRIAL_CHAMBERS_COLOR");
        c.ok(renderedOnFrame(type), "shown on item frames (1.21.11's showOnItemFrame)");
        c.eq(type, MapDecoration.Type.byIcon(type.getIcon()), "saved and synced by its ordinal " + type.getIcon());
    }

    /** Forge's {@code addTrade} counts levels from 1, its {@code getTrades} from 0. */
    private static List<EntityVillager.ITradeList> careerTrades(VillagerRegistry.VillagerCareer career) {
        List<EntityVillager.ITradeList> trades = career.getTrades(ExplorerMap.CAREER_LEVEL - 1);
        return trades == null ? java.util.Collections.<EntityVillager.ITradeList>emptyList() : trades;
    }

    /** {@code isRenderedOnFrame()} is client-only; its field is on both sides. */
    private static boolean renderedOnFrame(MapDecoration.Type type) {
        try {
            java.lang.reflect.Field f = MapDecoration.Type.class.getDeclaredField("renderedOnFrame");
            f.setAccessible(true);
            return f.getBoolean(type);
        } catch (ReflectiveOperationException e) {
            return false;
        }
    }

    private void lang(Check c) throws Exception {
        String name = null;
        InputStream in = ExplorerMap.class.getResourceAsStream("/assets/trickytrialsbackport/lang/en_us.lang");
        c.ok(in != null, "en_us.lang is in the jar");
        if (in == null) {
            return;
        }
        try (BufferedReader r = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8))) {
            for (String line; (line = r.readLine()) != null; ) {
                if (line.startsWith(ExplorerMap.NAME + "=")) {
                    name = line.substring(ExplorerMap.NAME.length() + 1);
                }
            }
        }
        c.eq("Trial Explorer Map", name, "the map's name is 1.21.11's");
    }

    /** @return a bought trial explorer map's trade, or null */
    private MerchantRecipe trade(Check c) {
        WorldServer world = c.world;
        BlockPos at = c.area();
        EntityVillager villager = new EntityVillager(world, 1);
        villager.setPosition(at.getX() + 0.5, at.getY(), at.getZ() + 0.5);
        boolean vp = Loader.isModLoaded(Owners.VILLAGE_AND_PILLAGE);
        c.ok(StructureLocate.known(ExplorerMap.STRUCTURE), "trial chambers can be located");
        c.ok(ExplorerMap.offered(), "the trade is offered with the setting on");

        int registered = 0;
        TradeRegistry.VillagerTrade added = null;
        for (TradeRegistry.VillagerTrade t : TradeRegistry.villagerTrades(ExplorerMap.CAREER, ExplorerMap.JOURNEYMAN)) {
            if (t.getClass().getName().startsWith(ExplorerMap.class.getName())) {
                registered++;
                added = t;
            }
        }
        VillagerRegistry.VillagerCareer career = ExplorerMap.cartographer();
        c.ok(career != null, "1.12.2's cartographer career exists");
        int inCareer = 0;
        EntityVillager.ITradeList list = null;
        if (career != null) {
            for (EntityVillager.ITradeList t : careerTrades(career)) {
                if (t.getClass().getName().startsWith(ExplorerMap.class.getName())) {
                    inCareer++;
                    list = t;
                }
            }
            boolean monument = false;
            for (EntityVillager.ITradeList t : careerTrades(career)) {
                monument |= t.getClass().getName().endsWith("TreasureMapForEmeralds") && destination(t) == MapDecoration.Type.MONUMENT;
            }
            c.ok(monument, "1.12.2's career level " + ExplorerMap.CAREER_LEVEL + " is the one with the monument map");
        }

        MerchantRecipe recipe;
        if (vp) {
            c.eq(1, registered, "with Village & Pillage, one journeyman trade in its cartographer pool");
            c.eq(0, inCareer, "and none in 1.12.2's career, which it replaces");
            c.eq(!ExplorerMap.villagePillageSellsIt(), added != null && added.available(),
                    "offered only beside its 1.14 table (its current table sells the map itself)");
            recipe = added == null ? null : added.offer(villager, new Random(1));
            c.ok(added == null || added.offer(null, new Random(1)) == null, "no map without a villager");
            vpDraw(c, villager);
        } else {
            c.eq(0, registered, "without Village & Pillage, nothing in Core's trade registry (nobody reads it)");
            c.eq(1, inCareer, "one trade at 1.12.2's cartographer level " + ExplorerMap.CAREER_LEVEL);
            MerchantRecipeList out = new MerchantRecipeList();
            if (list != null) {
                list.addMerchantRecipe(villager, out, new Random(1));
            }
            c.eq(1, out.size(), "the career's trade gives one offer");
            recipe = out.isEmpty() ? null : out.get(0);
        }
        if (recipe == null) {
            recipe = ExplorerMap.trade(world, new BlockPos(villager));
        }
        c.ok(recipe != null, "a villager here is offered a trial explorer map");
        if (recipe == null) {
            return null;
        }
        c.ok(recipe.getItemToBuy().getItem() == Items.EMERALD && recipe.getItemToBuy().getCount() == 12, "for 12 emeralds: " + recipe.getItemToBuy());
        c.ok(recipe.getSecondItemToBuy().getItem() == Items.COMPASS && recipe.getSecondItemToBuy().getCount() == 1, "and a compass: " + recipe.getSecondItemToBuy());
        c.eq(12, recipe.getMaxTradeUses(), "12 uses");
        c.eq(0, recipe.getToolUses(), "none used");

        // Where the map points: today's /locate from the villager.
        BlockPos expected = null;
        try {
            StructureLocate.Found f = StructureLocate.nearest(world, new BlockPos(villager), ExplorerMap.STRUCTURE);
            expected = f == null ? null : f.pos;
        } catch (StructureLocate.Unavailable e) {
            c.fail("locate: " + e.getMessage());
        }
        NBTTagCompound tag = recipe.getItemToSell().getTagCompound();
        NBTTagList decorations = tag == null ? new NBTTagList() : tag.getTagList("Decorations", 10);
        c.eq(1, decorations.tagCount(), "one target decoration");
        if (decorations.tagCount() == 1 && expected != null) {
            NBTTagCompound d = decorations.getCompoundTagAt(0);
            c.eq(expected.getX(), (int) d.getDouble("x"), "it marks the nearest chambers' x");
            c.eq(expected.getZ(), (int) d.getDouble("z"), "and z");
        }
        return recipe;
    }

    /**
     * Village &amp; Pillage's own journeyman draw (its {@code ModernTrades.addTier}, by name), with whichever cartographer
     * table is on: a trial explorer map turns up, with this mod's decoration type, and never twice in one draw.
     */
    private void vpDraw(Check c, EntityVillager villager) {
        java.lang.reflect.Method addTier;
        try {
            addTier = Class.forName("com.exiledradio.villagerbackport.trade.ModernTrades")
                    .getMethod("addTier", String.class, int.class, EntityVillager.class, MerchantRecipeList.class, Random.class);
        } catch (ReflectiveOperationException e) {
            c.fail("Village & Pillage's ModernTrades.addTier: " + e);
            return;
        }
        int maps = 0, twice = 0, wrongType = 0;
        for (int seed = 0; seed < 40; seed++) {
            MerchantRecipeList out = new MerchantRecipeList();
            try {
                addTier.invoke(null, ExplorerMap.CAREER, ExplorerMap.JOURNEYMAN, villager, out, new Random(seed));
            } catch (ReflectiveOperationException e) {
                c.fail("addTier: " + e);
                return;
            }
            int here = 0;
            for (MerchantRecipe r : out) {
                ItemStack sell = r.getItemToSell();
                NBTTagCompound tag = sell.getTagCompound();
                if (tag != null && ExplorerMap.NAME.equals(tag.getCompoundTag("display").getString("LocName"))) {
                    here++;
                    wrongType += tag.getTagList("Decorations", 10).getCompoundTagAt(0).getByte("type") == ExplorerMap.type().getIcon() ? 0 : 1;
                }
            }
            maps += here;
            twice += here > 1 ? 1 : 0;
        }
        c.ok(maps > 0, "Village & Pillage's journeyman cartographer sells the trial explorer map (" + maps + " in 40 draws, "
                + (ExplorerMap.villagePillageSellsIt() ? "its current table" : "its 1.14 table with ours") + ")");
        c.eq(0, twice, "never two in one draw");
        c.eq(0, wrongType, "with the trial chambers decoration");
    }

    /** 1.12.2's {@code TreasureMapForEmeralds} is package-private; its public field is read by reflection. */
    private static Object destination(Object treasureMap) {
        try {
            java.lang.reflect.Field f = treasureMap.getClass().getDeclaredField("destinationType");
            f.setAccessible(true);
            return f.get(treasureMap);
        } catch (ReflectiveOperationException e) {
            return null;
        }
    }

    private void map(Check c, ItemStack map, WorldServer world) {
        c.ok(map.getItem() == Items.FILLED_MAP, "a filled map: " + map);
        NBTTagCompound tag = map.getTagCompound();
        c.ok(tag != null, "with its tags");
        if (tag == null) {
            return;
        }
        NBTTagCompound display = tag.getCompoundTag("display");
        c.eq(ExplorerMap.NAME, display.getString("LocName"), "named filled_map.trial_chambers");
        c.eq(12741452, display.getInteger("MapColor"), "colored 12741452");
        NBTTagCompound d = tag.getTagList("Decorations", 10).getCompoundTagAt(0);
        c.eq((int) ExplorerMap.type().getIcon(), (int) d.getByte("type"), "the decoration is the trial chambers type");
        c.eq("+", d.getString("id"), "id +");
        c.eq(180.0D, d.getDouble("rot"), "rotation 180");
        BlockPos target = new BlockPos(d.getDouble("x"), 0, d.getDouble("z"));

        MapData data = Items.FILLED_MAP.getMapData(map, world);
        c.ok(data != null, "the map's data exists");
        if (data != null) {
            c.eq((byte) 2, data.scale, "zoom 2");
            c.ok(data.trackingPosition && data.unlimitedTracking, "tracking, unlimited");
            int size = 128 << 2;
            c.ok(Math.abs(data.xCenter - target.getX()) <= size / 2 && Math.abs(data.zCenter - target.getZ()) <= size / 2,
                    "centered on the chambers' grid square (" + data.xCenter + ", " + data.zCenter + ")");
            int colored = 0;
            for (byte b : data.colors) {
                colored += b != 0 ? 1 : 0;
            }
            c.ok(colored > 0, "the biome preview is drawn (" + colored + " pixels)");
        }

        // A real chamber: the target chunk starts one, laid out with pieces.
        ScatteredStructure s = ScatteredStructure.of(world, Structures.TRIAL_CHAMBERS);
        int cx = target.getX() >> 4, cz = target.getZ() >> 4;
        c.ok(s != null && s.startsAt(world, cx, cz), "trial chambers start in the marked chunk " + cx + ", " + cz);
        List<?> layout = TrialChambersStructure.layout(world, cx, cz);
        c.ok(layout != null && layout.size() > 1, "and lay out (" + (layout == null ? 0 : layout.size()) + " pieces)");
    }

    private void setting(Check c) {
        boolean was = ModConfig.chambers.explorerMap;
        try {
            ModConfig.chambers.explorerMap = false;
            c.ok(!ExplorerMap.offered(), "setting off: not offered");
            VillagerRegistry.VillagerCareer career = ExplorerMap.cartographer();
            if (career != null && !Loader.isModLoaded(Owners.VILLAGE_AND_PILLAGE)) {
                EntityVillager villager = new EntityVillager(c.world, 1);
                BlockPos at = c.area();
                villager.setPosition(at.getX(), at.getY(), at.getZ());
                MerchantRecipeList out = new MerchantRecipeList();
                for (EntityVillager.ITradeList t : careerTrades(career)) {
                    if (t.getClass().getName().startsWith(ExplorerMap.class.getName())) {
                        t.addMerchantRecipe(villager, out, new Random(1));
                    }
                }
                c.eq(0, out.size(), "setting off: the career's trade gives nothing");
            }
            for (TradeRegistry.VillagerTrade t : TradeRegistry.villagerTrades(ExplorerMap.CAREER, ExplorerMap.JOURNEYMAN)) {
                if (t.getClass().getName().startsWith(ExplorerMap.class.getName())) {
                    c.ok(!t.available(), "setting off: Core's pool entry is unavailable");
                }
            }
        } finally {
            ModConfig.chambers.explorerMap = was;
        }
        c.ok(ExplorerMap.offered() == was, "setting restored");
    }
}
