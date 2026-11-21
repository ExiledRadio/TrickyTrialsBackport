package com.exiledradio.trickytrialsbackport.selftest;

import com.exiledradio.trickytrialsbackport.common.Content;
import com.exiledradio.trickytrialsbackport.common.Owners;
import com.exiledradio.trickytrialsbackport.decor.DecorContent;
import com.exiledradio.trickytrialsbackport.decor.DecorEvents;
import com.exiledradio.trickytrialsbackport.decor.EntityModernPainting;
import com.exiledradio.trickytrialsbackport.decor.ItemDecorDisc;
import com.exiledradio.trickytrialsbackport.decor.ItemTrimTemplate;
import com.exiledradio.trickytrialsbackport.decor.Paintings;
import com.exiledradio.trickytrialsbackport.decor.PaintingItems;
import com.exiledradio.trickytrialsbackport.asm.PaintingItemTransformer;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.util.NonNullList;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;

import net.minecraft.block.Block;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityHanging;
import net.minecraft.entity.item.EntityPainting;
import net.minecraft.entity.monster.EntityGhast;
import net.minecraft.entity.monster.EntityHusk;
import net.minecraft.entity.monster.EntityZombie;
import net.minecraft.entity.passive.EntityChicken;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.entity.projectile.EntityArrow;
import net.minecraft.entity.projectile.EntityLargeFireball;
import net.minecraft.entity.projectile.EntityTippedArrow;
import net.minecraft.init.Blocks;
import net.minecraft.init.Items;
import net.minecraft.inventory.InventoryCrafting;
import net.minecraft.item.EnumDyeColor;
import net.minecraft.item.EnumRarity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemBanner;
import net.minecraft.item.ItemRecord;
import net.minecraft.item.ItemStack;
import net.minecraft.item.crafting.CraftingManager;
import net.minecraft.item.crafting.IRecipe;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.tileentity.BannerPattern;
import net.minecraft.util.DamageSource;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.NonNullList;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraftforge.common.ForgeHooks;
import net.minecraftforge.fml.common.ObfuscationReflectionHelper;
import net.minecraftforge.oredict.OreDictionary;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;

/** Stream 9: paintings, banner patterns, music discs, sherds and templates. */
public final class DecorSelfTest implements SelfTest {

    @Override
    public String name() {
        return "decor";
    }

    @Override
    public void run(Check c) {
        items(c);
        recipes(c);
        patterns(c);
        if (Owners.present(Owners.VILLAGE_AND_PILLAGE)) {
            loom(c);
        } else {
            grid(c);
        }
        BlockPos area = c.area();
        EntityPlayerMP player = Players.real(c.world, "decor");
        player.setPosition(area.getX() + 16.5, area.getY(), area.getZ() + 2.5);
        c.world.spawnEntity(player);
        try {
            paintings(c, player, area);
            variantItems(c, player, area);
            tears(c, player, area);
            lavaChicken(c, player, area);
        } finally {
            c.world.removeEntityDangerously(player);
        }
    }

    private static Item item(String name) {
        return Owners.ours(name);
    }

    private void items(Check c) {
        int[] signals = {12, 11, 13, 10, 9};
        EnumRarity[] rarity = {EnumRarity.RARE, EnumRarity.UNCOMMON, EnumRarity.UNCOMMON, EnumRarity.UNCOMMON, EnumRarity.RARE};
        String[] discs = {"creator", "creator_music_box", "precipice", "tears", "lava_chicken"};
        for (int i = 0; i < discs.length; i++) {
            Item disc = item("music_disc_" + discs[i]);
            if (!c.ok(disc instanceof ItemDecorDisc, "disc " + discs[i])) {
                continue;
            }
            ItemDecorDisc d = (ItemDecorDisc) disc;
            c.eq(signals[i], d.comparatorSignal(), discs[i] + " comparator signal");
            c.eq(rarity[i], new ItemStack(d).getRarity(), discs[i] + " rarity");
            c.eq(1, new ItemStack(d).getMaxStackSize(), discs[i] + " one to a stack");
            c.eq(Content.id("music_disc." + discs[i]), d.music().getRegistryName(), discs[i] + " sound event");
            c.ok(d instanceof ItemRecord, discs[i] + " is a record the jukebox takes");
        }
        for (String s : DecorContent.SHERDS) {
            Item sherd = item(s + "_pottery_sherd");
            if (c.ok(sherd != null, s + " sherd")) {
                c.eq(EnumRarity.UNCOMMON, new ItemStack(sherd).getRarity(), s + " sherd uncommon");
                c.eq(64, new ItemStack(sherd).getMaxStackSize(), s + " sherd stacks to 64");
                c.ok(hasOre(sherd, "potterySherd"), s + " sherd in potterySherd (Trails & Tales' pot)");
            }
        }
        for (String t : DecorContent.TRIMS) {
            Item template = item(t + "_armor_trim_smithing_template");
            if (c.ok(template instanceof ItemTrimTemplate, t + " template")) {
                c.eq(t, ((ItemTrimTemplate) template).pattern(), t + " template pattern");
                c.eq(EnumRarity.UNCOMMON, new ItemStack(template).getRarity(), t + " template uncommon");
                c.ok(hasOre(template, "armorTrimTemplate"), t + " template in armorTrimTemplate");
            }
        }
        String[] patterns = {"flow", "guster", "field_masoned", "bordure_indented"};
        EnumRarity[] patternRarity = {EnumRarity.RARE, EnumRarity.RARE, EnumRarity.COMMON, EnumRarity.COMMON};
        for (int i = 0; i < patterns.length; i++) {
            Item p = item(patterns[i] + "_banner_pattern");
            if (c.ok(p != null, patterns[i] + " banner pattern")) {
                c.eq(1, new ItemStack(p).getMaxStackSize(), patterns[i] + " pattern one to a stack");
                if (!Owners.present(Owners.VILLAGE_AND_PILLAGE)) {
                    c.eq(patternRarity[i], new ItemStack(p).getRarity(), patterns[i] + " pattern rarity");
                }
            }
        }
    }

    private static boolean hasOre(Item item, String ore) {
        for (int id : OreDictionary.getOreIDs(new ItemStack(item))) {
            if (ore.equals(OreDictionary.getOreName(id))) {
                return true;
            }
        }
        return false;
    }

    private void recipes(Check c) {
        Crafting.expect(c, Crafting.grid(2, 1, new ItemStack(Items.PAPER), new ItemStack(Blocks.BRICK_BLOCK)),
                new ItemStack(DecorContent.fieldMasonedBannerPattern), "paper and bricks make Field Masoned");
        Crafting.expect(c, Crafting.grid(2, 1, new ItemStack(Blocks.VINE), new ItemStack(Items.PAPER)),
                new ItemStack(DecorContent.bordureIndentedBannerPattern), "paper and vines make Bordure Indented");
        ItemStack d = new ItemStack(Items.DIAMOND);
        Item flow = item("flow_armor_trim_smithing_template");
        Item bolt = item("bolt_armor_trim_smithing_template");
        Item rod = item("breeze_rod");
        Crafting.expect(c, Crafting.grid(3, 3, d, new ItemStack(flow), d, d, new ItemStack(rod), d, d, d, d),
                new ItemStack(flow, 2), "flow template duplicated with a breeze rod");
        Block copper = Owners.block("copper_block");
        Block waxed = Owners.block("waxed_copper_block");
        c.ok(copper != null && waxed != null, "copper blocks for the bolt template");
        if (copper != null && waxed != null) {
            Crafting.expect(c, Crafting.grid(3, 3, d, new ItemStack(bolt), d, d, new ItemStack(copper), d, d, d, d),
                    new ItemStack(bolt, 2), "bolt template duplicated with a copper block");
            Crafting.expect(c, Crafting.grid(3, 3, d, new ItemStack(bolt), d, d, new ItemStack(waxed), d, d, d, d),
                    new ItemStack(bolt, 2), "or a waxed copper block");
        }
        ItemStack none = CraftingManager.findMatchingResult(
                Crafting.grid(3, 3, d, new ItemStack(bolt), d, d, new ItemStack(Blocks.IRON_BLOCK), d, d, d, d), c.world);
        c.ok(none.isEmpty(), "not with an iron block");
    }

    private void patterns(Check c) {
        c.ok(DecorContent.flow != null && "flw".equals(DecorContent.flow.getHashname()), "Flow pattern, hash flw");
        c.ok(DecorContent.guster != null && "gus".equals(DecorContent.guster.getHashname()), "Guster pattern, hash gus");
        c.eq("flow", ObfuscationReflectionHelper.getPrivateValue(BannerPattern.class, DecorContent.flow, "field_191014_N"),
                "Flow draws textures/entity/banner/flow.png");
        int flows = 0;
        for (BannerPattern p : BannerPattern.values()) {
            if (p == DecorContent.flow) {
                flows++;
            }
        }
        c.eq(1, flows, "Flow added once");
    }

    /** Without Village &amp; Pillage: the crafting grid applies the four patterns and keeps the pattern item. */
    private void grid(Check c) {
        c.ok(BannerPattern.BRICKS.getPatternItem().getItem() == DecorContent.fieldMasonedBannerPattern,
                "bricks need Field Masoned in the grid (1.21.2)");
        c.ok(BannerPattern.CURLY_BORDER.getPatternItem().getItem() == DecorContent.bordureIndentedBannerPattern,
                "curly border needs Bordure Indented");
        Object[][] cases = {
                {DecorContent.flowBannerPattern, "flw"}, {DecorContent.gusterBannerPattern, "gus"},
                {DecorContent.fieldMasonedBannerPattern, "bri"}, {DecorContent.bordureIndentedBannerPattern, "cbo"}};
        for (Object[] k : cases) {
            Item pattern = (Item) k[0];
            ItemStack banner = ItemBanner.makeBanner(EnumDyeColor.WHITE, null);
            InventoryCrafting grid = Crafting.grid(3, 1, banner, new ItemStack(Items.DYE, 1, EnumDyeColor.BLUE.getDyeDamage()), new ItemStack(pattern));
            IRecipe recipe = CraftingManager.findMatchingRecipe(grid, c.world);
            if (!c.ok(recipe != null, k[1] + " applies in the grid")) {
                continue;
            }
            ItemStack out = recipe.getCraftingResult(grid);
            NBTTagCompound be = out.getSubCompound("BlockEntityTag");
            NBTTagList list = be == null ? null : be.getTagList("Patterns", 10);
            c.ok(list != null && list.tagCount() == 1 && k[1].equals(list.getCompoundTagAt(0).getString("Pattern"))
                    && list.getCompoundTagAt(0).getInteger("Color") == EnumDyeColor.BLUE.getDyeDamage(), k[1] + " on the banner, blue");
            NonNullList<ItemStack> left = recipe.getRemainingItems(grid);
            c.ok(left.get(2).getItem() == pattern, k[1] + "'s pattern item stays in the grid");
        }
        ItemStack vines = CraftingManager.findMatchingResult(Crafting.grid(3, 1, ItemBanner.makeBanner(EnumDyeColor.WHITE, null),
                new ItemStack(Items.DYE, 1, 4), new ItemStack(Blocks.VINE)), c.world);
        c.ok(vines.isEmpty(), "vines alone no longer make the curly border");
    }

    /** With Village &amp; Pillage: our items are its pattern items, and its loom lists the four as needing them. */
    private void loom(Check c) {
        c.eq("com.exiledradio.villagerbackport.item.ItemBannerPattern", DecorContent.flowBannerPattern.getClass().getName(),
                "Flow pattern is Village & Pillage's pattern item");
        try {
            Class<?> order = Class.forName("com.exiledradio.villagerbackport.block.BannerPatterns");
            Method indexOf = order.getMethod("indexOf", BannerPattern.class);
            Method isFree = order.getMethod("isFree", int.class);
            Field free = order.getField("FREE_COUNT");
            for (BannerPattern p : new BannerPattern[] {DecorContent.flow, DecorContent.guster, BannerPattern.BRICKS, BannerPattern.CURLY_BORDER}) {
                int at = (Integer) indexOf.invoke(null, p);
                c.ok(at > 0 && !(Boolean) isFree.invoke(null, at), p.getHashname() + " needs its item at the loom (index " + at + ")");
            }
            int stripe = (Integer) indexOf.invoke(null, BannerPattern.STRIPE_TOP);
            c.ok((Boolean) isFree.invoke(null, stripe), "stripes stay free");
            c.eq(BannerPattern.values().length - 1 - 4 - 5, free.getInt(null), "free count: all but base, the special five and our four");
            Method item = DecorContent.flowBannerPattern.getClass().getMethod("getPattern");
            c.ok(item.invoke(DecorContent.flowBannerPattern) == DecorContent.flow, "Flow item applies Flow");
        } catch (ReflectiveOperationException e) {
            c.fail("loom: " + e);
        }
    }

    private void paintings(Check c, EntityPlayerMP player, BlockPos area) {
        c.eq(21, Paintings.all().size(), "21 paintings");
        // A 3x3 wall: 1.12.2 has no 3x3 painting, so the largest that fit are all ours.
        BlockPos wall = area.add(2, 1, 10);
        c.fill(wall, wall.add(2, 2, 0), Blocks.STONE.getDefaultState());
        Set<String> seen = new HashSet<String>();
        for (int i = 0; i < 40; i++) {
            Entity e = place(c, player, wall.add(1, 1, 0), EnumFacing.NORTH);
            if (!c.ok(e instanceof EntityModernPainting, "3x3 wall gets one of ours")) {
                break;
            }
            EntityModernPainting p = (EntityModernPainting) e;
            seen.add(p.getVariant().name);
            c.ok(p.getVariant().width == 3 && p.getVariant().height == 3, "a 3x3 one");
            if (i == 0) {
                checkPainting(c, p);
            }
        }
        c.eq(9, seen.size(), "every 3x3 painting chosen at random, " + seen);

        // A 4x4 wall: vanilla's three 4x4 and our two, at random.
        BlockPos big = area.add(8, 1, 10);
        c.fill(big, big.add(3, 3, 0), Blocks.STONE.getDefaultState());
        int ours = 0;
        int vanilla = 0;
        Random random = new Random(9);
        for (int i = 0; i < 200; i++) {
            for (int dx = 0; dx < 4; dx++) {
                for (int dy = 0; dy < 4; dy++) {
                    BlockPos at = big.add(dx, dy, 0).offset(EnumFacing.NORTH);
                    EntityPainting v = new EntityPainting(c.world, at, EnumFacing.NORTH);
                    Entity e = DecorEvents.choose(c.world, at, EnumFacing.NORTH, v, random);
                    if (e instanceof EntityHanging && ((EntityHanging) e).getWidthPixels() == 64 && ((EntityHanging) e).getHeightPixels() == 64) {
                        if (e instanceof EntityModernPainting) {
                            ours++;
                        } else {
                            vanilla++;
                        }
                        dx = 4;
                        break;
                    }
                }
            }
        }
        c.ok(ours > 50 && vanilla > 90, "4x4 wall: ours and vanilla's by count (2 of 5: " + ours + " ours, " + vanilla + " vanilla)");

        // 1x1: seven of vanilla's and Meditative.
        BlockPos one = area.add(16, 1, 10);
        c.fill(one, one, Blocks.STONE.getDefaultState());
        int meditative = 0;
        for (int i = 0; i < 400; i++) {
            EntityPainting v = new EntityPainting(c.world, one.north(), EnumFacing.NORTH);
            Entity e = DecorEvents.choose(c.world, one.north(), EnumFacing.NORTH, v, random);
            if (e instanceof EntityModernPainting && "meditative".equals(((EntityModernPainting) e).getVariant().name)) {
                meditative++;
            }
        }
        c.ok(meditative > 20 && meditative < 90, "1x1: Meditative one time in eight (" + meditative + "/400)");

        // A summoned vanilla painting (no item) is left alone.
        BlockPos at = wall.add(1, 1, 0).north();
        EntityPainting summoned = new EntityPainting(c.world, at, EnumFacing.NORTH);
        c.ok(c.world.spawnEntity(summoned) && !summoned.isDead, "a summoned vanilla painting stays vanilla");
        summoned.setDead();
    }

    /** 1.19.4's painting items with a variant: the creative list, exact placement, pick-block. */
    private void variantItems(Check c, EntityPlayerMP player, BlockPos area) {
        c.ok(PaintingItemTransformer.appliedSubItems && PaintingItemTransformer.appliedPick, "P26 and P27 applied");
        NonNullList<ItemStack> list = NonNullList.create();
        Items.PAINTING.getSubItems(CreativeTabs.DECORATIONS, list);
        c.eq(48, list.size(), "creative: the random painting and 47 variants");
        c.ok(!list.isEmpty() && PaintingItems.of(list.get(0)) == null && !list.get(0).hasTagCompound(), "the random painting first");
        Set<String> names = new HashSet<String>();
        int lastH = 0;
        int lastW = 0;
        boolean ordered = true;
        for (int i = 1; i < list.size(); i++) {
            PaintingItems.Entry e = PaintingItems.of(list.get(i));
            if (!c.ok(e != null, "entry " + i + " carries a variant")) {
                return;
            }
            names.add(e.name);
            ordered &= e.height > lastH || (e.height == lastH && e.width >= lastW);
            lastH = e.height;
            lastW = e.width;
        }
        c.eq(47, names.size(), "47 different variants");
        c.ok(ordered, "ordered by height, then width");
        c.ok(names.contains("kebab") && names.contains("skull_and_roses") && names.contains("donkey_kong")
                && names.contains("baroque") && names.contains("dennis"), "1.12.2's and ours, modern ids");
        c.eq("minecraft:kebab", list.get(1).getSubCompound("EntityTag").getString("variant"), "kept as EntityTag variant, namespaced");
        NonNullList<ItemStack> search = NonNullList.create();
        Items.PAINTING.getSubItems(CreativeTabs.SEARCH, search);
        c.eq(48, search.size(), "the search tab lists them too");
        NonNullList<ItemStack> other = NonNullList.create();
        Items.PAINTING.getSubItems(CreativeTabs.COMBAT, other);
        c.eq(0, other.size(), "not in other tabs");
        NonNullList<ItemStack> frames = NonNullList.create();
        Items.ITEM_FRAME.getSubItems(CreativeTabs.DECORATIONS, frames);
        c.eq(1, frames.size(), "the item frame keeps one entry");

        // A 2x2 wall: Skull and Roses (vanilla) and Baroque (ours) hang exactly; a 4x4 painting does not fit.
        BlockPos wall = area.add(24, 1, 10);
        c.fill(wall, wall.add(1, 1, 0), Blocks.STONE.getDefaultState());
        Entity skull = placeVariant(c, player, PaintingItems.byName("skull_and_roses").stack(), wall);
        c.ok(skull instanceof EntityPainting && ((EntityPainting) skull).art == EntityPainting.EnumArt.SKULL_AND_ROSES,
                "Skull and Roses hangs as itself: " + skull);
        Entity baroque = placeVariant(c, player, PaintingItems.byName("minecraft:baroque").stack(), wall);
        c.ok(baroque instanceof EntityModernPainting && "baroque".equals(((EntityModernPainting) baroque).getVariant().name),
                "Baroque hangs as itself: " + baroque);
        ItemStack big = PaintingItems.byName("pointer").stack();
        c.ok(placeVariant(c, player, big, wall) == null, "a 4x4 painting does not fit a 2x2 wall");
        c.eq(1, player.getHeldItemMainhand().getCount(), "and stays in hand");
        ItemStack held = PaintingItems.byName("kebab").stack();
        held.setCount(2);
        c.ok(placeVariant(c, player, held, wall) != null, "a 1x1 fits");
        c.eq(1, held.getCount(), "placing uses one");

        // Pick-block gives the variant, for vanilla's paintings and ours.
        if (skull != null) {
            ItemStack pick = skull.getPickedResult(null);
            c.ok(ItemStack.areItemStacksEqual(pick, PaintingItems.byName("skull_and_roses").stack()), "pick on Skull and Roses gives it");
        }
        if (baroque != null) {
            ItemStack pick = baroque.getPickedResult(null);
            c.ok(ItemStack.areItemStacksEqual(pick, PaintingItems.byName("baroque").stack()), "pick on Baroque gives it");
        }

        // A namespaced or unknown variant read from a save.
        NBTTagCompound tag = new NBTTagCompound();
        new EntityModernPainting(c.world, wall.north(), EnumFacing.NORTH, Paintings.byName("dennis")).writeToNBT(tag);
        tag.setString("variant", "minecraft:dennis");
        EntityModernPainting read = new EntityModernPainting(c.world);
        read.readFromNBT(tag);
        c.eq("dennis", read.getVariant().name, "minecraft:dennis reads as Dennis");
        tag.setString("variant", "minecraft:no_such");
        read.readFromNBT(tag);
        c.eq("baroque", read.getVariant().name, "an unknown variant still falls back to Baroque");
    }

    /** Uses the stack on each block of the wall until a painting hangs; returns it (removed again) or null. */
    private Entity placeVariant(Check c, EntityPlayerMP player, ItemStack stack, BlockPos wall) {
        player.setHeldItem(EnumHand.MAIN_HAND, stack);
        for (int dx = 0; dx < 2; dx++) {
            for (int dy = 0; dy < 2; dy++) {
                BlockPos clicked = wall.add(dx, dy, 0);
                AxisAlignedBB box = new AxisAlignedBB(clicked.north()).grow(4);
                for (EntityHanging old : c.world.getEntitiesWithinAABB(EntityHanging.class, box)) {
                    c.world.removeEntityDangerously(old);
                }
                PlayerInteractEvent.RightClickBlock event = ForgeHooks.onRightClickBlock(player, EnumHand.MAIN_HAND, clicked,
                        EnumFacing.NORTH, new Vec3d(0.5, 0.5, 0.0));
                if (!event.isCanceled()) {
                    Items.PAINTING.onItemUse(player, c.world, clicked, EnumHand.MAIN_HAND, EnumFacing.NORTH, 0.5F, 0.5F, 0.0F);
                }
                for (EntityHanging h : c.world.getEntitiesWithinAABB(EntityHanging.class, box)) {
                    if (!h.isDead) {
                        c.world.removeEntityDangerously(h);
                        return h;
                    }
                }
            }
        }
        return null;
    }

    private Entity place(Check c, EntityPlayerMP player, BlockPos clicked, EnumFacing face) {
        player.setHeldItem(EnumHand.MAIN_HAND, new ItemStack(Items.PAINTING));
        AxisAlignedBB box = new AxisAlignedBB(clicked.offset(face)).grow(3);
        // Nothing left from the last try for onValidSurface's overlap check.
        for (EntityHanging old : c.world.getEntitiesWithinAABB(EntityHanging.class, box)) {
            c.world.removeEntityDangerously(old);
        }
        ForgeHooks.onRightClickBlock(player, EnumHand.MAIN_HAND, clicked, face, new Vec3d(0.5, 0.5, 0.5));
        Items.PAINTING.onItemUse(player, c.world, clicked, EnumHand.MAIN_HAND, face, 0.5F, 0.5F, 0.0F);
        List<EntityHanging> now = c.world.getEntitiesWithinAABB(EntityHanging.class, box);
        for (EntityHanging h : now) {
            if (!h.isDead) {
                c.world.removeEntityDangerously(h);
                return h;
            }
        }
        return null;
    }

    private void checkPainting(Check c, EntityModernPainting p) {
        c.eq(48, p.getWidthPixels(), "48 pixels wide");
        NBTTagCompound tag = new NBTTagCompound();
        p.writeToNBT(tag);
        c.eq(p.getVariant().name, tag.getString("variant"), "saved as variant");
        EntityModernPainting read = new EntityModernPainting(c.world);
        read.readFromNBT(tag);
        c.ok(read.getVariant() == p.getVariant() && p.getHangingPosition().equals(read.getHangingPosition())
                && read.facingDirection == EnumFacing.NORTH, "read back: variant, position, facing");
        io.netty.buffer.ByteBuf buf = io.netty.buffer.Unpooled.buffer();
        p.writeSpawnData(buf);
        EntityModernPainting client = new EntityModernPainting(c.world);
        client.readSpawnData(buf);
        c.ok(client.getVariant() == p.getVariant() && client.getEntityBoundingBox().equals(p.getEntityBoundingBox()),
                "spawn data gives the same painting and box");
        BlockPos where = p.getHangingPosition();
        Drops.count(c.world, where, Items.PAINTING);
        p.onBroken(null);
        c.eq(1, Drops.count(c.world, where, Items.PAINTING), "breaks into a painting");
        c.ok(ItemStack.areItemStacksEqual(PaintingItems.byName(p.getVariant().name).stack(), p.getPickedResult(null)), "picked as its variant");
    }

    private void tears(Check c, EntityPlayerMP player, BlockPos area) {
        BlockPos at = area.add(20, 6, 20);
        c.eq(1, killGhast(c, at, player, true), "a ghast killed by a fireball a player sent back drops Tears");
        c.eq(0, killGhast(c, at, null, true), "not by a fireball no player touched");
        c.eq(0, killGhast(c, at, player, false), "not by an arrow");
    }

    private int killGhast(Check c, BlockPos at, EntityPlayerMP player, boolean fireball) {
        EntityGhast ghast = new EntityGhast(c.world);
        ghast.setPosition(at.getX() + 0.5, at.getY(), at.getZ() + 0.5);
        c.world.spawnEntity(ghast);
        Item tears = item("music_disc_tears");
        Drops.count(c.world, at, tears);
        DamageSource source;
        if (fireball) {
            EntityGhast shooter = new EntityGhast(c.world);
            EntityLargeFireball ball = new EntityLargeFireball(c.world, shooter, 1, 0, 0);
            if (player != null) {
                // What a player's hit does to a fireball: it becomes the player's.
                ball.shootingEntity = player;
            }
            source = DamageSource.causeFireballDamage(ball, ball.shootingEntity);
        } else {
            EntityArrow arrow = new EntityTippedArrow(c.world, player);
            source = DamageSource.causeArrowDamage(arrow, player);
        }
        ghast.attackEntityFrom(source, 1000.0F);
        return Drops.count(c.world, at, tears);
    }

    private void lavaChicken(Check c, EntityPlayerMP player, BlockPos area) {
        BlockPos at = area.add(24, 1, 24);
        c.eq(1, killZombie(c, at, player, new EntityZombie(c.world), true, true), "a baby zombie riding a chicken drops Lava Chicken");
        c.eq(0, killZombie(c, at, player, new EntityZombie(c.world), false, true), "not a grown one");
        c.eq(0, killZombie(c, at, player, new EntityZombie(c.world), true, false), "not one on foot");
        c.eq(0, killZombie(c, at, null, new EntityZombie(c.world), true, true), "not without a player");
        c.eq(0, killZombie(c, at, player, new EntityHusk(c.world), true, true), "not a husk");
    }

    private int killZombie(Check c, BlockPos at, EntityPlayerMP player, EntityZombie zombie, boolean baby, boolean riding) {
        Item disc = item("music_disc_lava_chicken");
        Drops.count(c.world, at, disc);
        zombie.setChild(baby);
        zombie.setPosition(at.getX() + 0.5, at.getY(), at.getZ() + 0.5);
        c.world.spawnEntity(zombie);
        EntityChicken chicken = null;
        if (riding) {
            chicken = new EntityChicken(c.world);
            chicken.setPosition(at.getX() + 0.5, at.getY(), at.getZ() + 0.5);
            c.world.spawnEntity(chicken);
            zombie.startRiding(chicken, true);
        }
        zombie.attackEntityFrom(player != null ? DamageSource.causePlayerDamage(player) : DamageSource.GENERIC, 1000.0F);
        int n = Drops.count(c.world, at, disc);
        if (chicken != null) {
            chicken.setDead();
        }
        return n;
    }
}
