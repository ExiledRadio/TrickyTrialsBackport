package com.exiledradio.trickytrialsbackport.selftest;

import com.exiledradio.trickytrialsbackport.bogged.BoggedContent;
import com.exiledradio.trickytrialsbackport.bogged.EntityBogged;
import com.exiledradio.trickytrialsbackport.common.Content;

import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityList;
import net.minecraft.entity.EnumCreatureType;
import net.minecraft.entity.SharedMonsterAttributes;
import net.minecraft.entity.ai.EntityAIAttackRangedBow;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.entity.monster.AbstractSkeleton;
import net.minecraft.entity.monster.EntitySkeleton;
import net.minecraft.entity.passive.EntityPig;
import net.minecraft.entity.projectile.EntityTippedArrow;
import net.minecraft.init.Biomes;
import net.minecraft.init.Blocks;
import net.minecraft.init.Items;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.util.DamageSource;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.EnumDifficulty;
import net.minecraft.world.biome.Biome;
import net.minecraftforge.common.util.FakePlayer;
import net.minecraftforge.fml.relauncher.ReflectionHelper;

import java.util.List;

/** Stream 4: the bogged. */
public final class BoggedSelfTest implements SelfTest {

    @Override
    public String name() {
        return "bogged";
    }

    @Override
    public void run(Check c) {
        BlockPos area = c.area();
        EntityBogged bogged = spawn(c, area.add(16, 0, 16));
        c.eq(Content.id("bogged"), EntityList.getKey(bogged), "bogged entity name");
        c.ok(EntityList.ENTITY_EGGS.containsKey(Content.id("bogged")), "bogged has a spawn egg");
        c.ok(bogged instanceof AbstractSkeleton, "a skeleton");
        c.eq(16.0, bogged.getEntityAttribute(SharedMonsterAttributes.MAX_HEALTH).getBaseValue(), "16 health");
        c.eq(Items.BOW, bogged.getHeldItemMainhand().getItem(), "spawns with a bow");

        cooldowns(c, bogged);
        arrows(c, bogged, area);
        shearing(c, area);
        loot(c, area);
        spawning(c);
    }

    private static EntityBogged spawn(Check c, BlockPos at) {
        EntityBogged b = new EntityBogged(c.world);
        b.setPosition(at.getX() + 0.5, at.getY(), at.getZ() + 0.5);
        b.onInitialSpawn(c.world.getDifficultyForLocation(at), null);
        c.world.spawnEntity(b);
        return b;
    }

    private static int cooldown(EntityBogged b) {
        EntityAIAttackRangedBow<?> bow = ReflectionHelper.getPrivateValue(AbstractSkeleton.class, b, "aiArrowAttack");
        Integer v = ReflectionHelper.getPrivateValue(EntityAIAttackRangedBow.class, bow, "attackCooldown");
        return v;
    }

    private void cooldowns(Check c, EntityBogged b) {
        EnumDifficulty was = c.world.getDifficulty();
        c.world.getWorldInfo().setDifficulty(EnumDifficulty.NORMAL);
        b.setCombatTask();
        c.eq(70, cooldown(b), "70 ticks between shots below Hard (4.5 s with the draw)");
        c.world.getWorldInfo().setDifficulty(EnumDifficulty.HARD);
        b.setCombatTask();
        c.eq(50, cooldown(b), "50 ticks on Hard");
        c.world.getWorldInfo().setDifficulty(was);
        EntitySkeleton skeleton = new EntitySkeleton(c.world);
        skeleton.setItemStackToSlot(net.minecraft.inventory.EntityEquipmentSlot.MAINHAND, new ItemStack(Items.BOW));
        skeleton.setCombatTask();
        Integer sk = ReflectionHelper.getPrivateValue(EntityAIAttackRangedBow.class,
                ReflectionHelper.<EntityAIAttackRangedBow<?>, AbstractSkeleton>getPrivateValue(AbstractSkeleton.class, skeleton, "aiArrowAttack"),
                "attackCooldown");
        c.ok(sk == 20 || sk == 40, "a skeleton keeps its own: " + sk);
    }

    private void arrows(Check c, EntityBogged b, BlockPos area) {
        EntityPig pig = new EntityPig(c.world);
        pig.setPosition(area.getX() + 26.5, area.getY(), area.getZ() + 16.5);
        c.world.spawnEntity(pig);
        b.attackEntityWithRangedAttack(pig, 1.0F);
        List<EntityTippedArrow> arrows = c.world.getEntitiesWithinAABB(EntityTippedArrow.class, b.getEntityBoundingBox().grow(3));
        c.eq(1, arrows.size(), "one arrow shot");
        if (!arrows.isEmpty()) {
            NBTTagCompound tag = new NBTTagCompound();
            arrows.get(0).writeToNBT(tag);
            NBTTagList effects = tag.getTagList("CustomPotionEffects", 10);
            c.ok(effects.tagCount() == 1 && effects.getCompoundTagAt(0).getByte("Id") == 19
                    && effects.getCompoundTagAt(0).getInteger("Duration") == 100, "an arrow of Poison for 5 s: " + effects);
            arrows.get(0).setDead();
        }
        pig.setDead();
    }

    private void shearing(Check c, BlockPos area) {
        EntityBogged b = spawn(c, area.add(4, 0, 4));
        ItemStack shears = new ItemStack(Items.SHEARS);
        c.ok(b.isShearable(shears, c.world, b.getPosition()), "shearable at first");
        List<ItemStack> drops = b.onSheared(shears, c.world, b.getPosition(), 0);
        int mushrooms = 0;
        for (ItemStack s : drops) {
            Item i = s.getItem();
            if (i == Item.getItemFromBlock(Blocks.BROWN_MUSHROOM) || i == Item.getItemFromBlock(Blocks.RED_MUSHROOM)) {
                mushrooms += s.getCount();
            }
        }
        c.eq(2, mushrooms, "two mushrooms");
        c.ok(b.isSheared() && !b.isShearable(shears, c.world, b.getPosition()), "only once");
        NBTTagCompound tag = new NBTTagCompound();
        b.writeToNBTOptional(tag);
        Entity back = EntityList.createEntityFromNBT(tag, c.world);
        c.ok(back instanceof EntityBogged && ((EntityBogged) back).isSheared(), "stays sheared after saving");
    }

    private void loot(Check c, BlockPos area) {
        FakePlayer player = c.player("bogged", area.up());
        int[] arrowsMinMax = {99, 0};
        int[] bonesMinMax = {99, 0};
        int tipped = 0;
        int tippedMax = 0;
        for (int i = 0; i < 40; i++) {
            EntityBogged b = spawn(c, area.add(20, 0, 4));
            b.setItemStackToSlot(net.minecraft.inventory.EntityEquipmentSlot.MAINHAND, ItemStack.EMPTY);
            b.attackEntityFrom(DamageSource.causePlayerDamage(player), 1000.0F);
            int arrows = 0;
            int bones = 0;
            int t = 0;
            for (EntityItem e : c.world.getEntitiesWithinAABB(EntityItem.class, new AxisAlignedBB(b.getPosition()).grow(2))) {
                if (e.isDead) {
                    continue;
                }
                Item item = e.getItem().getItem();
                if (item == Items.ARROW) {
                    arrows += e.getItem().getCount();
                } else if (item == Items.BONE) {
                    bones += e.getItem().getCount();
                } else if (item == Items.TIPPED_ARROW) {
                    t += e.getItem().getCount();
                    NBTTagCompound tag = e.getItem().getTagCompound();
                    c.ok(tag != null && "minecraft:poison".equals(tag.getString("Potion")), "the tipped arrow is Poison");
                }
                e.setDead();
            }
            arrowsMinMax[0] = Math.min(arrowsMinMax[0], arrows);
            arrowsMinMax[1] = Math.max(arrowsMinMax[1], arrows);
            bonesMinMax[0] = Math.min(bonesMinMax[0], bones);
            bonesMinMax[1] = Math.max(bonesMinMax[1], bones);
            tipped += t;
            tippedMax = Math.max(tippedMax, t);
        }
        c.ok(arrowsMinMax[0] == 0 && arrowsMinMax[1] == 2, "0-2 arrows: " + arrowsMinMax[0] + "-" + arrowsMinMax[1]);
        c.ok(bonesMinMax[0] == 0 && bonesMinMax[1] == 2, "0-2 bones: " + bonesMinMax[0] + "-" + bonesMinMax[1]);
        c.ok(tipped > 0 && tippedMax == 1, "player kills sometimes drop one Arrow of Poison: " + tipped + " in 40");

        EntityBogged unowned = spawn(c, area.add(24, 0, 4));
        unowned.attackEntityFrom(DamageSource.GENERIC, 1000.0F);
        c.eq(0, Drops.count(c.world, unowned.getPosition(), Items.TIPPED_ARROW), "no tipped arrow without a player");
    }

    private void spawning(Check c) {
        for (Biome biome : new Biome[] {Biomes.SWAMPLAND, Biomes.MUTATED_SWAMPLAND}) {
            boolean bogged = false;
            int skeletons = -1;
            for (Biome.SpawnListEntry e : biome.getSpawnableList(EnumCreatureType.MONSTER)) {
                if (e.entityClass == EntityBogged.class) {
                    bogged = e.itemWeight == 30 && e.minGroupCount == 4 && e.maxGroupCount == 4;
                }
                if (e.entityClass == EntitySkeleton.class) {
                    skeletons = e.itemWeight;
                }
            }
            c.ok(bogged, biome.getRegistryName() + " spawns bogged at 30, groups of 4");
            c.eq(70, skeletons, biome.getRegistryName() + " skeletons down to 70");
        }
        for (Biome.SpawnListEntry e : Biomes.PLAINS.getSpawnableList(EnumCreatureType.MONSTER)) {
            c.ok(e.entityClass != EntityBogged.class, "no bogged in plains");
            if (e.entityClass == EntitySkeleton.class) {
                c.eq(100, e.itemWeight, "plains skeletons unchanged");
            }
        }
        c.eq(BoggedContent.swamps().size(), 2, "two swamps without the mangrove swamp's mod");
    }
}
