package com.exiledradio.trickytrialsbackport.selftest;

import com.exiledradio.trickytrialsbackport.ModConfig;
import com.exiledradio.trickytrialsbackport.animals.AnimalData;
import com.exiledradio.trickytrialsbackport.animals.AnimalsContent;
import com.exiledradio.trickytrialsbackport.animals.AnimalsHooks;
import com.exiledradio.trickytrialsbackport.animals.EntityColoredEgg;
import com.exiledradio.trickytrialsbackport.animals.FarmAnimals;
import com.exiledradio.trickytrialsbackport.animals.FarmVariant;
import com.exiledradio.trickytrialsbackport.animals.WolfVoice;
import com.exiledradio.trickytrialsbackport.asm.AnimalsTransformer;
import com.exiledradio.trickytrialsbackport.common.Content;

import net.minecraft.block.BlockDispenser;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityList;
import net.minecraft.entity.EntityLiving;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.entity.passive.EntityAnimal;
import net.minecraft.entity.passive.EntityChicken;
import net.minecraft.entity.passive.EntityCow;
import net.minecraft.entity.passive.EntityMooshroom;
import net.minecraft.entity.passive.EntityPig;
import net.minecraft.entity.passive.EntitySheep;
import net.minecraft.entity.passive.EntityWolf;
import net.minecraft.init.Biomes;
import net.minecraft.init.Blocks;
import net.minecraft.init.Items;
import net.minecraft.init.SoundEvents;
import net.minecraft.item.EnumDyeColor;
import net.minecraft.item.ItemMonsterPlacer;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.JsonToNBT;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.SoundEvent;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.biome.Biome;
import net.minecraft.world.chunk.Chunk;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.entity.living.BabyEntitySpawnEvent;
import net.minecraftforge.fml.common.registry.ForgeRegistries;

import java.lang.reflect.Method;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;

/** Stream 16: farm animal variants. */
public final class AnimalsSelfTest implements SelfTest {

    @Override
    public String name() {
        return "animals";
    }

    @Override
    public void run(Check c) throws Exception {
        registration(c);
        biomes(c);
        spawning(c);
        nbt(c);
        breeding(c);
        eggs(c);
        sheep(c);
        wolves(c);
    }

    // ------------------------------------------------------------------------------------------

    /** Sets the biome of the 3x3 chunks around a position, the way generation stores it. */
    private static void biome(Check c, BlockPos at, Biome biome) {
        byte id = (byte) Biome.getIdForBiome(biome);
        for (int dx = -1; dx <= 1; dx++) {
            for (int dz = -1; dz <= 1; dz++) {
                Chunk chunk = c.world.getChunk(at.add(dx * 16, 0, dz * 16));
                java.util.Arrays.fill(chunk.getBiomeArray(), id);
            }
        }
    }

    private static <E extends EntityLiving> E spawned(Check c, E e, BlockPos at) {
        e.setLocationAndAngles(at.getX() + 0.5D, at.getY(), at.getZ() + 0.5D, 0.0F, 0.0F);
        e.onInitialSpawn(c.world.getDifficultyForLocation(at), null);
        return e;
    }

    private static NBTTagCompound saved(Entity e) {
        NBTTagCompound tag = new NBTTagCompound();
        tag.setString("id", String.valueOf(EntityList.getKey(e)));
        e.writeToNBT(tag);
        return tag;
    }

    /** The living ones among entities of a class around a position (dead ones stay listed until the next tick). */
    private static <T extends Entity> List<T> alive(Check c, Class<T> cls, BlockPos at) {
        List<T> out = new java.util.ArrayList<T>();
        for (T e : c.world.getEntitiesWithinAABB(cls, new AxisAlignedBB(at).grow(3))) {
            if (!e.isDead) {
                out.add(e);
            }
        }
        return out;
    }

    private void registration(Check c) {
        c.ok(AnimalsTransformer.APPLIED.contains("P16"), "patch P16 applied (EntityLiving.onInitialSpawn)");
        c.ok(AnimalsTransformer.APPLIED.contains("P17"), "patch P17 applied (EntitySheep.onInitialSpawn)");
        c.ok(AnimalsTransformer.APPLIED.contains("P18"), "patch P18 applied (EntityAnimal NBT)");
        c.ok(AnimalsTransformer.APPLIED.contains("P19"), "patch P19 applied (EntityWolf sounds)");
        c.ok(AnimalsContent.blueEgg != null && AnimalsContent.brownEgg != null, "both eggs registered");
        c.eq(Content.id("blue_egg"), AnimalsContent.blueEgg.getRegistryName(), "blue egg name");
        c.eq(Content.id("brown_egg"), AnimalsContent.brownEgg.getRegistryName(), "brown egg name");
        c.eq(16, AnimalsContent.blueEgg.getItemStackLimit(new ItemStack(AnimalsContent.blueEgg)), "blue egg stacks to 16");
        c.eq(AnimalsContent.blueEgg, AnimalsContent.egg(FarmVariant.COLD), "cold lays blue");
        c.eq(AnimalsContent.brownEgg, AnimalsContent.egg(FarmVariant.WARM), "warm lays brown");
        c.eq(null, AnimalsContent.egg(FarmVariant.TEMPERATE), "temperate lays 1.12.2's egg");
        c.eq(EntityColoredEgg.class, EntityList.getClass(Content.id("egg")), "thrown egg entity registered");
        c.ok(BlockDispenser.DISPENSE_BEHAVIOR_REGISTRY.getObject(AnimalsContent.blueEgg) != BlockDispenser.DISPENSE_BEHAVIOR_REGISTRY
                .getObject(Items.STICK), "dispenser throws blue eggs");
        c.ok(AnimalData.of(new EntityCow(c.world)) != null, "cows carry the data");
        c.ok(AnimalData.of(new EntityPig(c.world)) != null, "pigs carry the data");
        c.ok(AnimalData.of(new EntityChicken(c.world)) != null, "chickens carry the data");
        c.ok(AnimalData.of(new EntityWolf(c.world)) != null, "wolves carry the data");
        c.ok(AnimalData.of(new EntityMooshroom(c.world)) == null, "mooshrooms do not");
        c.ok(AnimalData.of(new EntitySheep(c.world)) == null, "sheep do not");
        c.eq(FarmVariant.TEMPERATE, FarmAnimals.variant(new EntityCow(c.world)), "a new cow is temperate");
        c.eq(WolfVoice.CLASSIC, FarmAnimals.voice(new EntityWolf(c.world)), "a new wolf is classic");
        int sounds = 0;
        for (WolfVoice v : WolfVoice.values()) {
            for (String kind : new String[]{"ambient", "death", "growl", "hurt", "pant", "whine"}) {
                if (v != WolfVoice.CLASSIC && ForgeRegistries.SOUND_EVENTS.containsKey(Content.id(v.event(kind)))) {
                    sounds++;
                }
            }
        }
        c.eq(36, sounds, "the six new voices' 36 sound events registered");
        c.ok(ModConfig.animals.cowVariants && ModConfig.animals.pigVariants && ModConfig.animals.chickenVariants
                && ModConfig.animals.coloredEggs && ModConfig.animals.sheepColors && ModConfig.animals.wolfSoundVariants,
                "every setting on by default");
    }

    private void biomes(Check c) {
        Biome[] warm = {Biomes.DESERT, Biomes.DESERT_HILLS, Biomes.MUTATED_DESERT, Biomes.JUNGLE, Biomes.JUNGLE_EDGE,
                Biomes.MUTATED_JUNGLE, Biomes.SAVANNA, Biomes.SAVANNA_PLATEAU, Biomes.MUTATED_SAVANNA, Biomes.MESA,
                Biomes.MESA_ROCK, Biomes.MESA_CLEAR_ROCK, Biomes.MUTATED_MESA, Biomes.HELL};
        Biome[] cold = {Biomes.ICE_PLAINS, Biomes.ICE_MOUNTAINS, Biomes.MUTATED_ICE_FLATS, Biomes.FROZEN_OCEAN,
                Biomes.FROZEN_RIVER, Biomes.COLD_BEACH, Biomes.COLD_TAIGA, Biomes.COLD_TAIGA_HILLS, Biomes.TAIGA,
                Biomes.TAIGA_HILLS, Biomes.MUTATED_TAIGA, Biomes.REDWOOD_TAIGA, Biomes.MUTATED_REDWOOD_TAIGA,
                Biomes.EXTREME_HILLS, Biomes.EXTREME_HILLS_EDGE, Biomes.EXTREME_HILLS_WITH_TREES,
                Biomes.MUTATED_EXTREME_HILLS, Biomes.SKY};
        Biome[] temperate = {Biomes.PLAINS, Biomes.FOREST, Biomes.BIRCH_FOREST, Biomes.ROOFED_FOREST, Biomes.SWAMPLAND,
                Biomes.RIVER, Biomes.OCEAN, Biomes.DEEP_OCEAN, Biomes.BEACH, Biomes.STONE_BEACH, Biomes.MUSHROOM_ISLAND,
                Biomes.MUTATED_PLAINS, Biomes.MUTATED_FOREST, Biomes.VOID};
        for (Biome b : warm) {
            c.eq(FarmVariant.WARM, FarmVariant.forBiome(b), b.getRegistryName() + " is warm");
        }
        for (Biome b : cold) {
            c.eq(FarmVariant.COLD, FarmVariant.forBiome(b), b.getRegistryName() + " is cold");
        }
        for (Biome b : temperate) {
            c.eq(FarmVariant.TEMPERATE, FarmVariant.forBiome(b), b.getRegistryName() + " is temperate");
        }
        c.eq(FarmVariant.TEMPERATE, FarmVariant.forBiome(null), "no biome is temperate");
        c.eq(FarmVariant.COLD, FarmVariant.byId("minecraft:cold"), "id minecraft:cold");
        c.eq(FarmVariant.WARM, FarmVariant.byId("warm"), "id warm, namespace implied");
        c.eq(null, FarmVariant.byId("minecraft:purple"), "unknown id");
    }

    private void spawning(Check c) {
        BlockPos area = c.area();
        BlockPos at = area.add(16, 0, 16);
        Object[][] cases = {{Biomes.DESERT, FarmVariant.WARM}, {Biomes.ICE_PLAINS, FarmVariant.COLD},
                {Biomes.PLAINS, FarmVariant.TEMPERATE}, {Biomes.SAVANNA, FarmVariant.WARM}, {Biomes.TAIGA, FarmVariant.COLD}};
        for (Object[] k : cases) {
            Biome b = (Biome) k[0];
            FarmVariant want = (FarmVariant) k[1];
            biome(c, at, b);
            c.eq(b, c.world.getBiome(at), "test biome set");
            c.eq(want, FarmAnimals.variant(spawned(c, new EntityCow(c.world), at)), "cow spawned in " + b.getRegistryName());
            c.eq(want, FarmAnimals.variant(spawned(c, new EntityPig(c.world), at)), "pig spawned in " + b.getRegistryName());
            c.eq(want, FarmAnimals.variant(spawned(c, new EntityChicken(c.world), at)), "chicken spawned in " + b.getRegistryName());
        }
        biome(c, at, Biomes.DESERT);
        c.ok(AnimalData.of(spawned(c, new EntityMooshroom(c.world), at)) == null, "a mooshroom has no variant");

        // A spawn egg: 1.12.2's spawnCreature calls onInitialSpawn, so the egg picks by biome, as 1.21.5's does.
        Entity egged = ItemMonsterPlacer.spawnCreature(c.world, new net.minecraft.util.ResourceLocation("minecraft", "cow"),
                at.getX() + 0.5D, at.getY(), at.getZ() + 0.5D);
        c.eq(FarmVariant.WARM, FarmAnimals.variant(egged), "spawn egg cow in a desert is warm");
        // The egg's EntityTag (spawn egg NBT) overrides it, as 1.21.5's entity_data does.
        ItemStack tagged = new ItemStack(Items.SPAWN_EGG);
        ItemMonsterPlacer.applyEntityIdToItemStack(tagged, new net.minecraft.util.ResourceLocation("minecraft", "cow"));
        tagged.getTagCompound().getCompoundTag("EntityTag").setString("variant", "minecraft:cold");
        ItemMonsterPlacer.applyItemEntityDataToEntity(c.world, null, tagged, egged);
        c.eq(FarmVariant.COLD, FarmAnimals.variant(egged), "spawn egg EntityTag variant");
        if (egged != null) {
            egged.setDead();
        }

        // /summon: without NBT it picks by biome; with NBT it takes the NBT's variant.
        biome(c, at, Biomes.ICE_PLAINS);
        String pos = at.getX() + " " + at.getY() + " " + at.getZ();
        c.world.getMinecraftServer().getCommandManager().executeCommand(c.world.getMinecraftServer(), "summon minecraft:pig " + pos);
        c.world.getMinecraftServer().getCommandManager().executeCommand(c.world.getMinecraftServer(),
                "summon minecraft:chicken " + pos + " {variant:\"minecraft:warm\"}");
        List<EntityPig> pigs = alive(c, EntityPig.class, at);
        List<EntityChicken> chickens = alive(c, EntityChicken.class, at);
        c.ok(pigs.size() == 1 && FarmAnimals.variant(pigs.get(0)) == FarmVariant.COLD, "/summon pig in snowy plains is cold");
        c.ok(chickens.size() == 1 && FarmAnimals.variant(chickens.get(0)) == FarmVariant.WARM, "/summon chicken {variant:warm}");
        for (Entity e : pigs) {
            e.setDead();
        }
        for (Entity e : chickens) {
            e.setDead();
        }
        biome(c, at, Biomes.PLAINS);
    }

    private void nbt(Check c) {
        for (EntityAnimal a : new EntityAnimal[]{new EntityCow(c.world), new EntityPig(c.world), new EntityChicken(c.world)}) {
            String kind = a.getClass().getSimpleName();
            for (FarmVariant v : FarmVariant.values()) {
                FarmAnimals.setVariant(a, v);
                NBTTagCompound tag = saved(a);
                c.eq(v.id.toString(), tag.getString("variant"), kind + " saves variant " + v.name);
                Entity back = EntityList.createEntityFromNBT(tag, c.world);
                c.eq(v, FarmAnimals.variant(back), kind + " loads variant " + v.name);
            }
            NBTTagCompound tag = saved(a);
            tag.setString("variant", "minecraft:not_a_variant");
            Entity back = EntityList.createEntityFromNBT(tag, c.world);
            c.eq(FarmVariant.TEMPERATE, FarmAnimals.variant(back), kind + ": an unknown variant stays temperate");
            tag.removeTag("variant");
            c.eq(FarmVariant.TEMPERATE, FarmAnimals.variant(EntityList.createEntityFromNBT(tag, c.world)),
                    kind + " from before this mod is temperate");
        }
        try {
            NBTTagCompound summon = JsonToNBT.getTagFromJson("{id:\"minecraft:cow\",variant:\"minecraft:warm\"}");
            c.eq(FarmVariant.WARM, FarmAnimals.variant(EntityList.createEntityFromNBT(summon, c.world)), "cow from {variant:warm}");
        } catch (net.minecraft.nbt.NBTException e) {
            c.fail("summon NBT parse: " + e);
        }
        NBTTagCompound mooshroom = saved(new EntityMooshroom(c.world));
        c.ok(!mooshroom.hasKey("variant"), "a mooshroom saves no variant");
        NBTTagCompound sheep = saved(new EntitySheep(c.world));
        c.ok(!sheep.hasKey("variant"), "a sheep saves no variant");
    }

    /** 1.21.5: a spawn egg used on an animal of its kind makes a baby with the animal's variant. */
    private void eggBaby(Check c, net.minecraft.entity.EntityAgeable parent, FarmVariant v, String what) {
        net.minecraft.util.math.BlockPos at = c.world.getSpawnPoint().add(40, 0, 40);
        at = c.world.getTopSolidOrLiquidBlock(at);
        FarmAnimals.setVariant(parent, v);
        parent.setLocationAndAngles(at.getX() + 0.5, at.getY(), at.getZ() + 0.5, 0, 0);
        c.world.spawnEntity(parent);
        net.minecraftforge.common.util.FakePlayer player = net.minecraftforge.common.util.FakePlayerFactory.getMinecraft(
                (net.minecraft.world.WorldServer) c.world);
        player.capabilities.isCreativeMode = true;
        net.minecraft.item.ItemStack egg = new net.minecraft.item.ItemStack(net.minecraft.init.Items.SPAWN_EGG);
        net.minecraft.item.ItemMonsterPlacer.applyEntityIdToItemStack(egg, net.minecraft.entity.EntityList.getKey(parent));
        player.setHeldItem(net.minecraft.util.EnumHand.MAIN_HAND, egg);
        player.interactOn(parent, net.minecraft.util.EnumHand.MAIN_HAND);
        java.util.List<net.minecraft.entity.EntityAgeable> babies = c.world.getEntitiesWithinAABB(parent.getClass(),
                parent.getEntityBoundingBox().grow(2), e -> e != parent && e.isChild());
        c.ok(babies.size() == 1, what + ": the egg made one baby (" + babies.size() + ")");
        if (babies.size() == 1) {
            c.eq(v, FarmAnimals.variant(babies.get(0)), what + ": the egg's baby has its parent's variant");
            babies.get(0).setDead();
        }
        parent.setDead();
    }

    private void breeding(Check c) {
        eggBaby(c, new EntityCow(c.world), FarmVariant.WARM, "warm cow");
        eggBaby(c, new EntityPig(c.world), FarmVariant.COLD, "cold pig");
        eggBaby(c, new EntityChicken(c.world), FarmVariant.WARM, "warm chicken");
        Random rand = new Random(16);
        EntityCow a = new EntityCow(c.world);
        EntityCow b = new EntityCow(c.world);
        FarmAnimals.setVariant(a, FarmVariant.WARM);
        FarmAnimals.setVariant(b, FarmVariant.COLD);
        Map<FarmVariant, Integer> seen = new EnumMap<FarmVariant, Integer>(FarmVariant.class);
        for (int i = 0; i < 400; i++) {
            EntityCow child = new EntityCow(c.world);
            MinecraftForge.EVENT_BUS.post(new BabyEntitySpawnEvent(a, b, child));
            FarmVariant v = FarmAnimals.variant(child);
            Integer n = seen.get(v);
            seen.put(v, n == null ? 1 : n + 1);
        }
        c.ok(!seen.containsKey(FarmVariant.TEMPERATE), "a calf of warm and cold parents is never temperate: " + seen);
        c.ok(seen.containsKey(FarmVariant.WARM) && seen.get(FarmVariant.WARM) > 150, "calves take the warm parent's: " + seen);
        c.ok(seen.containsKey(FarmVariant.COLD) && seen.get(FarmVariant.COLD) > 150, "calves take the cold parent's: " + seen);

        EntityChicken hen = new EntityChicken(c.world);
        EntityChicken rooster = new EntityChicken(c.world);
        FarmAnimals.setVariant(hen, FarmVariant.COLD);
        FarmAnimals.setVariant(rooster, FarmVariant.COLD);
        EntityChicken chick = new EntityChicken(c.world);
        MinecraftForge.EVENT_BUS.post(new BabyEntitySpawnEvent(hen, rooster, chick));
        c.eq(FarmVariant.COLD, FarmAnimals.variant(chick), "two cold chickens' chick is cold");

        EntityPig sow = new EntityPig(c.world);
        EntityPig boar = new EntityPig(c.world);
        FarmAnimals.setVariant(sow, FarmVariant.WARM);
        FarmAnimals.setVariant(boar, FarmVariant.WARM);
        EntityPig piglet = new EntityPig(c.world);
        MinecraftForge.EVENT_BUS.post(new BabyEntitySpawnEvent(sow, boar, piglet));
        c.eq(FarmVariant.WARM, FarmAnimals.variant(piglet), "two warm pigs' piglet is warm");

        EntityWolf w1 = new EntityWolf(c.world);
        EntityWolf w2 = new EntityWolf(c.world);
        Set<WolfVoice> voices = EnumSet.noneOf(WolfVoice.class);
        for (int i = 0; i < 200; i++) {
            EntityWolf pup = new EntityWolf(c.world);
            MinecraftForge.EVENT_BUS.post(new BabyEntitySpawnEvent(w1, w2, pup));
            voices.add(FarmAnimals.voice(pup));
        }
        c.eq(7, voices.size(), "puppies get any of the seven voices");
    }

    private void eggs(Check c) throws Exception {
        BlockPos area = c.area();
        BlockPos at = area.add(8, 0, 8);
        Object[][] laid = {{FarmVariant.COLD, AnimalsContent.blueEgg}, {FarmVariant.WARM, AnimalsContent.brownEgg},
                {FarmVariant.TEMPERATE, Items.EGG}};
        for (Object[] k : laid) {
            FarmVariant v = (FarmVariant) k[0];
            EntityChicken hen = new EntityChicken(c.world);
            hen.setLocationAndAngles(at.getX() + 0.5D, at.getY(), at.getZ() + 0.5D, 0.0F, 0.0F);
            FarmAnimals.setVariant(hen, v);
            c.world.spawnEntity(hen);
            hen.timeUntilNextEgg = 1;
            hen.onUpdate();
            List<EntityItem> items = alive(c, EntityItem.class, at);
            c.ok(items.size() == 1 && items.get(0).getItem().getItem() == k[1] && items.get(0).getItem().getCount() == 1,
                    "a " + v.name + " chicken lays " + k[1] + ", got " + items);
            c.ok(hen.timeUntilNextEgg >= 5999, "the timer restarts after laying");
            for (EntityItem e : items) {
                e.setDead();
            }
            hen.setDead();
        }
        // A cold chick lays nothing.
        EntityChicken chickling = new EntityChicken(c.world);
        chickling.setLocationAndAngles(at.getX() + 0.5D, at.getY(), at.getZ() + 0.5D, 0.0F, 0.0F);
        chickling.setGrowingAge(-24000);
        FarmAnimals.setVariant(chickling, FarmVariant.COLD);
        c.world.spawnEntity(chickling);
        chickling.timeUntilNextEgg = 1;
        chickling.onUpdate();
        c.eq(0, alive(c, EntityItem.class, at).size(), "a chick lays nothing");
        chickling.setDead();

        // Hatching: 1 in 8 throws hatches, 1 in 32 of those four chicks; each chick of the egg's variant, a baby.
        Method hatch = EntityColoredEgg.class.getDeclaredMethod("hatch");
        hatch.setAccessible(true);
        at = at.add(12, 0, 12);
        int throwsHatched = 0;
        int chicks = 0;
        boolean allBlue = true;
        for (int i = 0; i < 800; i++) {
            EntityColoredEgg egg = new EntityColoredEgg(c.world, at.getX() + 0.5D, at.getY() + 1, at.getZ() + 0.5D, FarmVariant.COLD);
            int n = (Integer) hatch.invoke(egg);
            if (n > 0) {
                throwsHatched++;
                chicks += n;
            }
        }
        List<EntityChicken> hatched = alive(c, EntityChicken.class, at);
        for (EntityChicken ch : hatched) {
            allBlue &= FarmAnimals.variant(ch) == FarmVariant.COLD && ch.getGrowingAge() == -24000;
            ch.setDead();
        }
        c.ok(throwsHatched > 60 && throwsHatched < 145, "about 1 in 8 blue eggs hatch: " + throwsHatched + " of 800");
        c.eq(chicks, hatched.size(), "every hatched chick is in the world");
        c.ok(allBlue && !hatched.isEmpty(), "chicks from blue eggs are cold babies");
        EntityColoredEgg brown = new EntityColoredEgg(c.world, 0, 0, 0, FarmVariant.WARM);
        c.eq(AnimalsContent.brownEgg, brown.item(), "a thrown brown egg shows the brown egg");
        NBTTagCompound tag = saved(brown);
        EntityColoredEgg back = new EntityColoredEgg(c.world);
        back.readFromNBT(tag);
        c.eq(FarmVariant.WARM, back.getVariant(), "a thrown egg saves its variant");

        // Cake and pumpkin pie take them (1.21.5's #eggs).
        ItemStack milk = new ItemStack(Items.MILK_BUCKET);
        ItemStack sugar = new ItemStack(Items.SUGAR);
        ItemStack wheat = new ItemStack(Items.WHEAT);
        for (ItemStack egg : new ItemStack[]{new ItemStack(AnimalsContent.blueEgg), new ItemStack(AnimalsContent.brownEgg),
                new ItemStack(Items.EGG)}) {
            Crafting.expect(c, Crafting.grid(3, 3, milk, milk, milk, sugar, egg, sugar, wheat, wheat, wheat),
                    new ItemStack(Items.CAKE), "cake with " + egg.getItem().getRegistryName());
            Crafting.expect(c, Crafting.grid(3, 1, new ItemStack(Blocks.PUMPKIN), sugar, egg),
                    new ItemStack(Items.PUMPKIN_PIE), "pumpkin pie with " + egg.getItem().getRegistryName());
        }
    }

    private void sheep(Check c) {
        Random rand = new Random(5);
        Object[][] climates = {{FarmVariant.WARM, EnumDyeColor.BROWN}, {FarmVariant.COLD, EnumDyeColor.BLACK},
                {FarmVariant.TEMPERATE, EnumDyeColor.WHITE}};
        for (Object[] k : climates) {
            FarmVariant v = (FarmVariant) k[0];
            Map<EnumDyeColor, Integer> n = new EnumMap<EnumDyeColor, Integer>(EnumDyeColor.class);
            for (int i = 0; i < 20000; i++) {
                EnumDyeColor col = FarmAnimals.sheepColor(v, rand);
                Integer m = n.get(col);
                n.put(col, m == null ? 1 : m + 1);
            }
            int common = n.get(k[1]);
            c.ok(common > 16000 && common < 16800, v.name + " sheep mostly " + k[1] + " (82%): " + n);
            c.eq(6, n.size(), v.name + " sheep come in six colors: " + n.keySet());
        }
        BlockPos area = c.area();
        BlockPos at = area.add(16, 0, 16);
        Object[][] biomes = {{Biomes.DESERT, EnumDyeColor.BROWN}, {Biomes.ICE_PLAINS, EnumDyeColor.BLACK},
                {Biomes.PLAINS, EnumDyeColor.WHITE}};
        for (Object[] k : biomes) {
            biome(c, at, (Biome) k[0]);
            int common = 0;
            for (int i = 0; i < 200; i++) {
                if (spawned(c, new EntitySheep(c.world), at).getFleeceColor() == k[1]) {
                    common++;
                }
            }
            c.ok(common > 140, "sheep spawned in " + ((Biome) k[0]).getRegistryName() + " mostly " + k[1] + ": " + common + "/200");
        }
        biome(c, at, Biomes.PLAINS);
    }

    private void wolves(Check c) throws Exception {
        BlockPos area = c.area();
        BlockPos at = area.add(16, 0, 16);
        Set<WolfVoice> voices = EnumSet.noneOf(WolfVoice.class);
        for (int i = 0; i < 200; i++) {
            voices.add(FarmAnimals.voice(spawned(c, new EntityWolf(c.world), at)));
        }
        c.eq(7, voices.size(), "spawned wolves get any of the seven voices");

        EntityWolf wolf = new EntityWolf(c.world);
        FarmAnimals.setVoice(wolf, WolfVoice.PUGLIN);
        NBTTagCompound tag = saved(wolf);
        c.eq("minecraft:puglin", tag.getString("sound_variant"), "wolf saves sound_variant");
        c.eq(WolfVoice.PUGLIN, FarmAnimals.voice(EntityList.createEntityFromNBT(tag, c.world)), "wolf loads sound_variant");
        tag.removeTag("sound_variant");
        c.eq(WolfVoice.CLASSIC, FarmAnimals.voice(EntityList.createEntityFromNBT(tag, c.world)), "an old wolf is classic");

        SoundEvent puglinHurt = ForgeRegistries.SOUND_EVENTS.getValue(Content.id("entity.wolf_puglin.hurt"));
        c.eq(puglinHurt, AnimalsHooks.wolfSound(SoundEvents.ENTITY_WOLF_HURT, wolf), "puglin's hurt sound");
        c.eq(SoundEvents.ENTITY_WOLF_SHAKE, AnimalsHooks.wolfSound(SoundEvents.ENTITY_WOLF_SHAKE, wolf), "shaking is every wolf's");
        // Through the patched method itself.
        Method death = EntityLivingBaseMethod(EntityWolf.class, "getDeathSound", "func_184615_bR");
        c.eq(ForgeRegistries.SOUND_EVENTS.getValue(Content.id("entity.wolf_puglin.death")), death.invoke(wolf),
                "a puglin wolf's getDeathSound (patch P19)");
        Method ambient = EntityLivingBaseMethod(EntityWolf.class, "getAmbientSound", "func_184639_G");
        SoundEvent heard = (SoundEvent) ambient.invoke(wolf);
        c.ok(heard != null && heard.getRegistryName() != null && heard.getRegistryName().getPath().startsWith("entity.wolf_puglin."),
                "a puglin wolf's getAmbientSound (patch P19): " + (heard == null ? null : heard.getRegistryName()));
        FarmAnimals.setVoice(wolf, WolfVoice.CLASSIC);
        c.eq(SoundEvents.ENTITY_WOLF_DEATH, death.invoke(wolf), "a classic wolf sounds as 1.12.2's");
    }

    private static Method EntityLivingBaseMethod(Class<?> cls, String mcp, String srg) throws NoSuchMethodException {
        Method m;
        try {
            m = cls.getDeclaredMethod(mcp);
        } catch (NoSuchMethodException e) {
            m = cls.getDeclaredMethod(srg);
        }
        m.setAccessible(true);
        return m;
    }
}
