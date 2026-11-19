package com.exiledradio.trickytrialsbackport.selftest;

import com.exiledradio.trickytrialsbackport.asm.ZombieHorseTransformer;
import com.exiledradio.trickytrialsbackport.common.Content;
import com.exiledradio.trickytrialsbackport.common.Owners;
import com.exiledradio.trickytrialsbackport.mounts.EntityParched;
import com.exiledradio.trickytrialsbackport.mounts.HorseArmor;
import com.exiledradio.trickytrialsbackport.mounts.MountsContent;
import com.exiledradio.trickytrialsbackport.mounts.MountsHooks;
import com.exiledradio.trickytrialsbackport.mounts.MountsTestAccess;
import com.exiledradio.trickytrialsbackport.mounts.Riding;

import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityList;
import net.minecraft.entity.EntityLiving;
import net.minecraft.entity.EnumCreatureType;
import net.minecraft.entity.SharedMonsterAttributes;
import net.minecraft.entity.ai.EntityAIAttackRangedBow;
import net.minecraft.entity.ai.EntityAIBase;
import net.minecraft.entity.ai.EntityAITasks;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.entity.monster.AbstractSkeleton;
import net.minecraft.entity.monster.EntitySkeleton;
import net.minecraft.entity.monster.EntityZombie;
import net.minecraft.entity.passive.AbstractHorse;
import net.minecraft.entity.passive.EntityHorse;
import net.minecraft.entity.passive.EntityPig;
import net.minecraft.entity.passive.EntitySkeletonHorse;
import net.minecraft.entity.passive.EntityZombieHorse;
import net.minecraft.entity.passive.HorseArmorType;
import net.minecraft.entity.projectile.EntityTippedArrow;
import net.minecraft.init.Blocks;
import net.minecraft.init.Items;
import net.minecraft.init.MobEffects;
import net.minecraft.inventory.EntityEquipmentSlot;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.potion.PotionEffect;
import net.minecraft.util.DamageSource;
import net.minecraft.util.EnumHand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.EnumDifficulty;
import net.minecraft.world.biome.Biome;
import net.minecraft.world.storage.loot.LootContext;
import net.minecraft.world.storage.loot.LootTable;
import net.minecraft.world.storage.loot.LootTableList;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.common.util.FakePlayer;
import net.minecraftforge.event.entity.living.LivingDropsEvent;
import net.minecraftforge.event.entity.living.LivingEvent;
import net.minecraftforge.event.entity.living.LivingSpawnEvent;
import net.minecraftforge.fml.common.eventhandler.Event;
import net.minecraftforge.fml.relauncher.ReflectionHelper;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/** Stream 24: mounts (1.21.11). */
public final class MountsSelfTest implements SelfTest {

    @Override
    public String name() {
        return "mounts";
    }

    @Override
    public void run(Check c) throws Exception {
        long time = c.world.getWorldTime();
        EnumDifficulty difficulty = c.world.getDifficulty();
        try {
            c.world.getWorldInfo().setDifficulty(EnumDifficulty.NORMAL);
            registration(c);
            parched(c);
            zombieHorse(c);
            taming(c);
            equipment(c);
            sun(c);
            spawning(c);
            riders(c);
            drops(c);
            floating(c);
            reach(c);
        } finally {
            c.world.setWorldTime(time);
            c.world.calculateInitialSkylight();
            c.world.getWorldInfo().setDifficulty(difficulty);
            MountsContent.lookup = Owners::ours;
        }
    }

    // ------------------------------------------------------------------ registration

    private void registration(Check c) {
        c.eq(EntityParched.class, EntityList.getClass(Content.id("parched")), "parched registered");
        c.eq(MountsContent.PARCHED_ID, net.minecraftforge.fml.common.registry.EntityRegistry.instance().lookupModSpawn(EntityParched.class, false)
                .getModEntityId(), "parched entity id 65");
        c.ok(EntityList.ENTITY_EGGS.containsKey(Content.id("parched")), "parched spawn egg");
        Item armor = MountsContent.netheriteHorseArmor;
        c.ok(armor != null && Content.id("netherite_horse_armor").equals(armor.getRegistryName()), "netherite horse armor registered");
        if (armor != null) {
            ItemStack stack = new ItemStack(armor);
            c.eq(1, stack.getMaxStackSize(), "netherite horse armor stacks to 1");
            c.eq(HorseArmor.netheriteType, HorseArmorType.getByItemStack(stack), "netherite horse armor type");
            c.eq(19, HorseArmorType.getByItemStack(stack).getProtection(), "netherite horse armor protection 19");
            c.ok(HorseArmorType.isHorseArmor(stack), "netherite horse armor is horse armor");
        }
        c.ok(ZombieHorseTransformer.applied, "P24a applied to EntityZombieHorse");
        c.ok(c.world.getLootTableManager().getLootTableFromLocation(EntityParched.LOOT) != LootTable.EMPTY_LOOT_TABLE, "parched loot table");
        c.eq(Owners.ours("iron_spear") != null, !MountsContent.ironSpear().isEmpty(), "the rider's spear follows whether stream 22's iron spear exists");
    }

    // ------------------------------------------------------------------ parched

    private void parched(Check c) {
        BlockPos area = c.area();
        EntityParched p = new EntityParched(c.world);
        p.setPosition(area.getX() + 8.5, area.getY(), area.getZ() + 8.5);
        p.onInitialSpawn(c.world.getDifficultyForLocation(area), null);
        c.world.spawnEntity(p);
        c.ok(p instanceof AbstractSkeleton, "parched is a skeleton");
        c.eq(16.0, p.getEntityAttribute(SharedMonsterAttributes.MAX_HEALTH).getBaseValue(), "parched 16 health");
        c.eq(Items.BOW, p.getHeldItemMainhand().getItem(), "parched spawns with a bow");

        c.world.getWorldInfo().setDifficulty(EnumDifficulty.NORMAL);
        p.setCombatTask();
        c.eq(70, cooldown(p), "parched: 70 ticks between shots below Hard");
        c.world.getWorldInfo().setDifficulty(EnumDifficulty.HARD);
        p.setCombatTask();
        c.eq(50, cooldown(p), "parched: 50 ticks on Hard");
        c.world.getWorldInfo().setDifficulty(EnumDifficulty.NORMAL);

        try {
            Method getArrow = ReflectionHelper.findMethod(AbstractSkeleton.class, "getArrow", "func_190726_a", float.class);
            Object arrow = getArrow.invoke(p, 1.0F);
            c.ok(arrow instanceof EntityTippedArrow, "parched shoots a tipped arrow");
            if (arrow instanceof EntityTippedArrow) {
                NBTTagCompound tag = new NBTTagCompound();
                ((EntityTippedArrow) arrow).writeEntityToNBT(tag);
                NBTTagList effects = tag.getTagList("CustomPotionEffects", 10);
                c.eq(1, effects.tagCount(), "one custom effect on the arrow");
                if (effects.tagCount() == 1) {
                    PotionEffect e = PotionEffect.readCustomPotionEffectFromNBT(effects.getCompoundTagAt(0));
                    c.eq(MobEffects.WEAKNESS, e.getPotion(), "arrow of Weakness");
                    c.eq(600, e.getDuration(), "Weakness for 30 s");
                }
            }
        } catch (ReflectiveOperationException e) {
            c.fail("getArrow: " + e);
        }

        p.addPotionEffect(new PotionEffect(MobEffects.WEAKNESS, 200));
        c.ok(!p.isPotionActive(MobEffects.WEAKNESS), "Weakness never takes hold of a parched");
        p.addPotionEffect(new PotionEffect(MobEffects.SLOWNESS, 200));
        c.ok(p.isPotionActive(MobEffects.SLOWNESS), "other effects do");

        // Daylight: a skeleton beside it burns, the parched does not.
        c.world.setWorldTime(6000);
        c.world.calculateInitialSkylight();
        EntitySkeleton skeleton = new EntitySkeleton(c.world);
        skeleton.setPosition(area.getX() + 12.5, area.getY(), area.getZ() + 12.5);
        c.world.spawnEntity(skeleton);
        p.setItemStackToSlot(EntityEquipmentSlot.HEAD, ItemStack.EMPTY);
        skeleton.setItemStackToSlot(EntityEquipmentSlot.HEAD, ItemStack.EMPTY);
        for (int i = 0; i < 400 && !skeleton.isBurning(); i++) {
            skeleton.onLivingUpdate();
            p.onLivingUpdate();
        }
        c.ok(skeleton.isBurning(), "a skeleton in the sun burns (control)");
        c.ok(!p.isBurning(), "the parched does not burn in daylight");
        c.ok(!MountsTestAccess.inDaylightCheck(p), "the daylight guard is down after its tick");
        p.setFire(5);
        c.ok(p.isBurning(), "other fire still burns a parched");
        p.extinguish();
        skeleton.setDead();
        p.setDead();
    }

    private static int cooldown(AbstractSkeleton s) {
        EntityAIAttackRangedBow<?> bow = ReflectionHelper.getPrivateValue(AbstractSkeleton.class, s, "aiArrowAttack", "field_85037_d");
        Integer v = ReflectionHelper.getPrivateValue(EntityAIAttackRangedBow.class, bow, "attackCooldown", "field_188501_c");
        return v;
    }

    // ------------------------------------------------------------------ the zombie horse

    private EntityZombieHorse horse(Check c, BlockPos at) {
        EntityZombieHorse h = new EntityZombieHorse(c.world);
        h.setPosition(at.getX() + 0.5, at.getY(), at.getZ() + 0.5);
        h.onInitialSpawn(c.world.getDifficultyForLocation(at), null);
        h.setGrowingAge(0);
        c.world.spawnEntity(h);
        return h;
    }

    private static boolean hasGoal(EntityLiving mob, java.util.function.Predicate<EntityAIBase> want) {
        for (EntityAITasks.EntityAITaskEntry e : mob.tasks.taskEntries) {
            if (want.test(e.action)) {
                return true;
            }
        }
        return false;
    }

    private void zombieHorse(Check c) throws Exception {
        BlockPos area = c.area();
        EntityZombieHorse h = horse(c, area.add(8, 0, 8));
        c.eq(25.0, h.getEntityAttribute(SharedMonsterAttributes.MAX_HEALTH).getBaseValue(), "zombie horse 25 health");
        c.eq(25.0F, h.getHealth(), "spawns at full health");
        double speed = h.getEntityAttribute(SharedMonsterAttributes.MOVEMENT_SPEED).getBaseValue();
        c.ok(speed >= 9.0 / 42.16 && speed <= 12.0 / 42.16, "speed rolled as 1.21.11's: " + speed);
        double jump = h.getHorseJumpStrength();
        c.ok(jump >= 0.5 && jump <= 0.7, "jump strength rolled as 1.21.11's: " + jump);
        c.ok(hasGoal(h, MountsTestAccess::isTempt), "red mushrooms tempt it");
        c.ok(hasGoal(h, MountsTestAccess::isRunAroundUnlessMobRides), "its crazy run skips mob riders");
        c.ok(h.wearsArmor(), "wears armor (P24a)");
        c.ok(h.isArmor(new ItemStack(Items.IRON_HORSE_ARMOR)), "iron horse armor fits");
        c.ok(!h.isArmor(new ItemStack(Items.SADDLE)), "a saddle is not armor");
        Method canDespawn = ReflectionHelper.findMethod(EntityLiving.class, "canDespawn", "func_70692_ba");
        c.ok((Boolean) canDespawn.invoke(h), "an untamed zombie horse despawns");
        h.setHorseTamed(true);
        c.ok(!(Boolean) canDespawn.invoke(h), "a tamed one does not");
        h.setDead();

        // Saved and loaded: attributes are kept, not rolled again.
        EntityZombieHorse a = horse(c, area.add(12, 0, 12));
        double s1 = a.getEntityAttribute(SharedMonsterAttributes.MOVEMENT_SPEED).getBaseValue();
        NBTTagCompound tag = new NBTTagCompound();
        a.writeToNBTOptional(tag);
        a.setDead();
        Entity b = EntityList.createEntityFromNBT(tag, c.world);
        c.world.spawnEntity(b);
        c.ok(b instanceof EntityZombieHorse, "reloads as a zombie horse");
        if (b instanceof EntityZombieHorse) {
            c.eq(s1, ((EntityZombieHorse) b).getEntityAttribute(SharedMonsterAttributes.MOVEMENT_SPEED).getBaseValue(), "speed kept on reload");
            c.eq(25.0, ((EntityZombieHorse) b).getEntityAttribute(SharedMonsterAttributes.MAX_HEALTH).getBaseValue(), "health kept on reload");
            b.setDead();
        }

        // A skeleton horse is left alone.
        EntitySkeletonHorse sk = new EntitySkeletonHorse(c.world);
        sk.setPosition(area.getX() + 20.5, area.getY(), area.getZ() + 20.5);
        c.world.spawnEntity(sk);
        c.eq(15.0, sk.getEntityAttribute(SharedMonsterAttributes.MAX_HEALTH).getBaseValue(), "skeleton horse keeps 15 health");
        c.ok(!sk.wearsArmor(), "skeleton horse still wears no armor");
        sk.setDead();
    }

    private void taming(Check c) {
        BlockPos area = c.area();
        EntityZombieHorse h = horse(c, area.add(8, 0, 8));
        FakePlayer player = c.player("mounts", area.add(10, 0, 8));

        // Food: a red mushroom heals 3 and adds 3 temper, and is used up.
        h.setHealth(10.0F);
        player.setHeldItem(EnumHand.MAIN_HAND, new ItemStack(Blocks.RED_MUSHROOM, 2));
        int temper = h.getTemper();
        c.ok(h.processInteract(player, EnumHand.MAIN_HAND), "feeding a red mushroom is taken");
        c.eq(13.0F, h.getHealth(), "a red mushroom heals 3");
        c.eq(temper + 3, h.getTemper(), "and adds 3 temper");
        c.eq(1, player.getHeldItemMainhand().getCount(), "the mushroom is used up");
        h.setHealth(h.getMaxHealth());
        h.setTemper(h.getMaxTemper());
        c.ok(!h.processInteract(player, EnumHand.MAIN_HAND), "a full, fully tempered horse refuses the mushroom");
        c.eq(1, player.getHeldItemMainhand().getCount(), "the mushroom is kept");
        h.setTemper(0);

        // Untamed: another item makes it mad, no saddle goes on.
        player.setHeldItem(EnumHand.MAIN_HAND, new ItemStack(Items.SADDLE));
        c.ok(h.processInteract(player, EnumHand.MAIN_HAND), "an item on an untamed horse is taken");
        c.ok(!h.isHorseSaddled(), "untamed: no saddle");
        c.eq(1, player.getHeldItemMainhand().getCount(), "the saddle is kept");

        // Empty hand: the player gets on, untamed.
        player.setHeldItem(EnumHand.MAIN_HAND, ItemStack.EMPTY);
        c.ok(h.processInteract(player, EnumHand.MAIN_HAND), "an empty hand mounts");
        c.eq(h, player.getRidingEntity(), "the player rides the untamed zombie horse");
        EntityAIBase crazy = null;
        for (EntityAITasks.EntityAITaskEntry e : h.tasks.taskEntries) {
            if (MountsTestAccess.isRunAroundUnlessMobRides(e.action)) {
                crazy = e.action;
            }
        }
        boolean starts = false;
        for (int i = 0; i < 50 && !starts && crazy != null; i++) {
            starts = crazy.shouldExecute();
        }
        c.ok(starts, "with a player on, it runs around to throw them");
        // Taming by riding: at full temper the next throw tames instead.
        h.setTemper(h.getMaxTemper());
        for (int i = 0; i < 2000 && !h.isTame() && crazy != null; i++) {
            crazy.updateTask();
        }
        c.ok(h.isTame(), "ridden at full temper, it is tamed");
        c.eq(player.getUniqueID(), h.getOwnerUniqueId(), "tamed by the rider");
        player.dismountRidingEntity();

        // A mob rider is never thrown.
        EntityZombieHorse m = horse(c, area.add(16, 0, 16));
        EntityZombie z = new EntityZombie(c.world);
        z.setPosition(m.posX, m.posY, m.posZ);
        c.world.spawnEntity(z);
        z.startRiding(m, true);
        boolean mobStarts = false;
        for (EntityAITasks.EntityAITaskEntry e : m.tasks.taskEntries) {
            if (MountsTestAccess.isRunAroundUnlessMobRides(e.action)) {
                for (int i = 0; i < 50; i++) {
                    mobStarts |= e.action.shouldExecute();
                }
            }
        }
        c.ok(!mobStarts, "a zombie rider is never thrown");
        z.setDead();
        m.setDead();
        h.setDead();
    }

    private void equipment(Check c) {
        BlockPos area = c.area();
        EntityZombieHorse h = horse(c, area.add(8, 0, 8));
        FakePlayer player = c.player("mounts", area.add(10, 0, 8));
        h.setTamedBy(player);

        player.setHeldItem(EnumHand.MAIN_HAND, new ItemStack(Items.SADDLE));
        c.ok(h.processInteract(player, EnumHand.MAIN_HAND), "a saddle on a tamed zombie horse is taken");
        c.ok(h.isHorseSaddled(), "saddled");
        c.ok(player.getHeldItemMainhand().isEmpty(), "the saddle is used up");

        player.setHeldItem(EnumHand.MAIN_HAND, new ItemStack(Items.IRON_HORSE_ARMOR));
        c.ok(h.processInteract(player, EnumHand.MAIN_HAND), "horse armor on a tamed zombie horse is taken");
        c.eq(Items.IRON_HORSE_ARMOR, MountsTestAccess.chest(h).getStackInSlot(1).getItem(), "the armor is in its armor slot");
        c.ok(player.getHeldItemMainhand().isEmpty(), "the armor is used up");
        tick(h);
        c.eq(Items.IRON_HORSE_ARMOR, h.getItemStackFromSlot(EntityEquipmentSlot.CHEST).getItem(), "the armor is shown as its equipment");
        c.eq(5.0, h.getEntityAttribute(SharedMonsterAttributes.ARMOR).getAttributeValue(), "iron horse armor gives 5 armor");

        if (MountsContent.netheriteHorseArmor != null) {
            MountsTestAccess.chest(h).setInventorySlotContents(1, new ItemStack(MountsContent.netheriteHorseArmor));
            tick(h);
            c.eq(19.0, h.getEntityAttribute(SharedMonsterAttributes.ARMOR).getAttributeValue(), "netherite: 19 armor");
            c.eq(3.0, h.getEntityAttribute(SharedMonsterAttributes.ARMOR_TOUGHNESS).getAttributeValue(), "netherite: 3 toughness");
            c.eq(0.1, h.getEntityAttribute(SharedMonsterAttributes.KNOCKBACK_RESISTANCE).getAttributeValue(), "netherite: 0.1 knockback resistance");

            EntityHorse horse = new EntityHorse(c.world);
            horse.setPosition(area.getX() + 20.5, area.getY(), area.getZ() + 20.5);
            c.world.spawnEntity(horse);
            horse.setHorseTamed(true);
            horse.replaceItemInInventory(401, new ItemStack(MountsContent.netheriteHorseArmor));
            tick(horse);
            c.eq(19.0, horse.getEntityAttribute(SharedMonsterAttributes.ARMOR).getAttributeValue(), "a horse in netherite: 19 armor");
            c.eq(3.0, horse.getEntityAttribute(SharedMonsterAttributes.ARMOR_TOUGHNESS).getAttributeValue(), "a horse in netherite: 3 toughness");
            horse.replaceItemInInventory(401, new ItemStack(Items.DIAMOND_HORSE_ARMOR));
            tick(horse);
            c.eq(11.0, horse.getEntityAttribute(SharedMonsterAttributes.ARMOR).getAttributeValue(), "back to diamond: 11 armor");
            c.eq(0.0, horse.getEntityAttribute(SharedMonsterAttributes.ARMOR_TOUGHNESS).getAttributeValue(), "and no toughness");
            horse.setDead();
        }
        MountsTestAccess.chest(h).setInventorySlotContents(1, ItemStack.EMPTY);
        tick(h);
        c.ok(h.getItemStackFromSlot(EntityEquipmentSlot.CHEST).isEmpty(), "armor taken out: none shown");
        c.eq(0.0, h.getEntityAttribute(SharedMonsterAttributes.ARMOR).getAttributeValue(), "and no armor");
        float[] chances = ReflectionHelper.getPrivateValue(EntityLiving.class, h, "inventoryArmorDropChances", "field_184655_bs");
        c.eq(0.0F, chances[EntityEquipmentSlot.CHEST.getIndex()], "the shown armor is never dropped twice");

        // Sneaking opens the inventory rather than mounting (a real player: a fake one has no connection for the GUI).
        net.minecraft.entity.player.EntityPlayerMP real = Players.real(c.world, "mountsgui");
        real.setPosition(h.posX + 1.0, h.posY, h.posZ);
        h.setTamedBy(real);
        real.setSneaking(true);
        c.ok(h.processInteract(real, EnumHand.MAIN_HAND), "sneaking with a tamed zombie horse is taken");
        c.ok(real.getRidingEntity() == null, "sneaking does not mount a tamed zombie horse");
        c.ok(real.openContainer instanceof net.minecraft.inventory.ContainerHorseInventory, "sneaking opens its inventory");
        if (real.openContainer instanceof net.minecraft.inventory.ContainerHorseInventory) {
            net.minecraft.inventory.Slot armorSlot = real.openContainer.getSlot(1);
            c.ok(armorSlot.isItemValid(new ItemStack(Items.DIAMOND_HORSE_ARMOR)), "which takes horse armor");
            c.ok(!armorSlot.isItemValid(new ItemStack(Items.APPLE)), "and nothing else");
        }
        real.closeScreen();
        h.processInteract(player, EnumHand.MAIN_HAND);
        c.eq(h, player.getRidingEntity(), "a tamed, saddled zombie horse is ridden");
        player.dismountRidingEntity();
        h.setDead();
    }

    private static void tick(EntityLiving mob) {
        MinecraftForge.EVENT_BUS.post(new LivingEvent.LivingUpdateEvent(mob));
    }

    private void sun(Check c) {
        BlockPos area = c.area();
        c.world.setWorldTime(6000);
        c.world.calculateInitialSkylight();
        EntityZombieHorse h = horse(c, area.add(8, 0, 8));
        for (int i = 0; i < 2000 && !h.isBurning(); i++) {
            MountsTestAccess.burn(h);
        }
        c.ok(h.isBurning(), "a zombie horse burns in the sun");
        h.extinguish();
        h.setHorseTamed(true);
        MountsTestAccess.chest(h).setInventorySlotContents(1, new ItemStack(Items.GOLDEN_HORSE_ARMOR));
        for (int i = 0; i < 2000; i++) {
            MountsTestAccess.burn(h);
        }
        c.ok(!h.isBurning(), "horse armor keeps the sun off");
        h.setDead();
        c.world.setWorldTime(18000);
        c.world.calculateInitialSkylight();
        EntityZombieHorse n = horse(c, area.add(16, 0, 16));
        for (int i = 0; i < 2000; i++) {
            MountsTestAccess.burn(n);
        }
        c.ok(!n.isBurning(), "no burning at night");
        n.setDead();
    }

    // ------------------------------------------------------------------ spawning

    private static boolean listed(Biome biome, Class<?> type, int weight, int min, int max) {
        for (Biome.SpawnListEntry e : biome.getSpawnableList(EnumCreatureType.MONSTER)) {
            if (e.entityClass == type && e.itemWeight == weight && e.minGroupCount == min && e.maxGroupCount == max) {
                return true;
            }
        }
        return false;
    }

    private void spawning(Check c) {
        for (Biome b : MountsTestAccess.deserts()) {
            c.ok(listed(b, EntityParched.class, 50, 4, 4), "parched 50 x 4 in " + b.getRegistryName());
            c.ok(listed(b, EntitySkeleton.class, 50, 4, 4), "skeleton down to 50 in " + b.getRegistryName());
        }
        for (Biome b : MountsTestAccess.plains()) {
            c.ok(listed(b, EntityZombieHorse.class, 5, 1, 1), "zombie horse 5 x 1 in " + b.getRegistryName());
        }
        c.ok(!listed(net.minecraft.init.Biomes.FOREST, EntityZombieHorse.class, 5, 1, 1), "no zombie horses in forests");

        // Monster rules, in a closed stone box (dark) and beside a torch.
        BlockPos area = c.area();
        BlockPos dark = area.add(8, 0, 8);
        c.fill(dark.add(-2, 0, -2), dark.add(2, 4, 2), Blocks.STONE.getDefaultState());
        c.fill(dark.add(-1, 0, -1), dark.add(1, 2, 1), Blocks.AIR.getDefaultState());
        relight(c, dark);
        EntityZombieHorse h = new EntityZombieHorse(c.world);
        h.setPosition(dark.getX() + 0.5, dark.getY(), dark.getZ() + 0.5);
        boolean allDark = true;
        for (int i = 0; i < 20; i++) {
            allDark &= MountsTestAccess.monsterRules(h);
        }
        c.ok(allDark, "monster rules pass in the dark (sky " + c.world.getLightFor(net.minecraft.world.EnumSkyBlock.SKY, dark)
                + ", block " + c.world.getLightFor(net.minecraft.world.EnumSkyBlock.BLOCK, dark) + ")");
        LivingSpawnEvent.CheckSpawn check = new LivingSpawnEvent.CheckSpawn(h, c.world, (float) h.posX, (float) h.posY, (float) h.posZ, false);
        MinecraftForge.EVENT_BUS.post(check);
        c.eq(Event.Result.ALLOW, check.getResult(), "a natural zombie horse may spawn in the dark");
        c.world.getWorldInfo().setDifficulty(EnumDifficulty.PEACEFUL);
        check = new LivingSpawnEvent.CheckSpawn(h, c.world, (float) h.posX, (float) h.posY, (float) h.posZ, false);
        MinecraftForge.EVENT_BUS.post(check);
        c.eq(Event.Result.DENY, check.getResult(), "not on Peaceful");
        c.world.getWorldInfo().setDifficulty(EnumDifficulty.NORMAL);
        c.world.setBlockState(dark.add(2, 0, 0), Blocks.GLOWSTONE.getDefaultState());
        relight(c, dark);
        boolean anyLit = false;
        for (int i = 0; i < 20; i++) {
            anyLit |= MountsTestAccess.monsterRules(h);
        }
        c.ok(!anyLit, "nor beside glowstone (block light " + c.world.getLightFor(net.minecraft.world.EnumSkyBlock.BLOCK, dark) + ")");
        check = new LivingSpawnEvent.CheckSpawn(h, c.world, (float) h.posX, (float) h.posY, (float) h.posZ, true);
        MinecraftForge.EVENT_BUS.post(check);
        c.eq(Event.Result.DEFAULT, check.getResult(), "spawners are left to 1.12.2");

        // Parched need the sky: inside the box no, in the open yes (the event's say; light is 1.12.2's).
        EntityParched p = new EntityParched(c.world);
        p.setPosition(dark.getX() + 0.5, dark.getY(), dark.getZ() + 0.5);
        check = new LivingSpawnEvent.CheckSpawn(p, c.world, (float) p.posX, (float) p.posY, (float) p.posZ, false);
        MinecraftForge.EVENT_BUS.post(check);
        c.eq(Event.Result.DENY, check.getResult(), "no parched without sky");
        p.setPosition(area.getX() + 20.5, area.getY(), area.getZ() + 20.5);
        check = new LivingSpawnEvent.CheckSpawn(p, c.world, (float) p.posX, (float) p.posY, (float) p.posZ, false);
        MinecraftForge.EVENT_BUS.post(check);
        c.eq(Event.Result.DEFAULT, check.getResult(), "under the sky the usual rules decide");
    }

    private static void relight(Check c, BlockPos at) {
        // World.checkLight works only with every chunk 17 blocks round loaded.
        for (int dx = -2; dx <= 2; dx++) {
            for (int dz = -2; dz <= 2; dz++) {
                c.world.getChunkProvider().provideChunk((at.getX() >> 4) + dx, (at.getZ() >> 4) + dz);
            }
        }
        for (BlockPos p : BlockPos.getAllInBox(at.add(-3, -1, -3), at.add(3, 5, 3))) {
            c.world.checkLight(p);
        }
    }

    // ------------------------------------------------------------------ riders

    private EntityZombie naturalRider(Check c, BlockPos at) {
        EntityZombieHorse h = new EntityZombieHorse(c.world);
        h.setPosition(at.getX() + 0.5, at.getY(), at.getZ() + 0.5);
        LivingSpawnEvent.SpecialSpawn special = new LivingSpawnEvent.SpecialSpawn(h, c.world, (float) h.posX, (float) h.posY, (float) h.posZ, null);
        if (!MinecraftForge.EVENT_BUS.post(special)) {
            h.onInitialSpawn(c.world.getDifficultyForLocation(at), null);
        }
        c.ok(h.getEntityData().getBoolean(MountsTestAccess.RIDER), "a natural spawn owes a rider");
        c.world.spawnEntity(h);
        tick(h);
        c.ok(!h.getEntityData().getBoolean(MountsTestAccess.RIDER), "paid on its first tick");
        Entity rider = h.getControllingPassenger();
        c.ok(rider instanceof EntityZombie && rider.getClass() == EntityZombie.class, "ridden by a zombie");
        return rider instanceof EntityZombie ? (EntityZombie) rider : null;
    }

    private void riders(Check c) {
        BlockPos area = c.area();
        // Without a spear (stream 22 absent, or its stand-in removed): the zombie keeps its own gear.
        MountsContent.lookup = name -> null;
        EntityZombie plain = naturalRider(c, area.add(6, 0, 6));
        if (plain != null) {
            c.ok(plain.getHeldItemMainhand().isEmpty() || plain.getHeldItemMainhand().getItem() != Items.STICK, "no spear: no spear");
            c.ok(plain.isRiding(), "rides without a spear");
            plain.getRidingEntity().setDead();
            plain.setDead();
        }
        // With a spear (a stand-in for stream 22's iron spear, looked up by name).
        MountsContent.lookup = name -> "iron_spear".equals(name) ? Items.STICK : null;
        EntityZombie armed = naturalRider(c, area.add(14, 0, 14));
        if (armed != null) {
            c.eq(Items.STICK, armed.getHeldItemMainhand().getItem(), "the rider holds the spear");
            armed.getRidingEntity().setDead();
            armed.setDead();
        }
        MountsContent.lookup = Owners::ours;
        // The real lookup, whichever way stream 22 stands in this build.
        EntityZombie real = naturalRider(c, area.add(22, 0, 22));
        if (real != null) {
            Item spear = Owners.ours("iron_spear");
            if (spear != null) {
                c.eq(spear, real.getHeldItemMainhand().getItem(), "with stream 22: an iron spear");
            } else {
                c.ok(real.isRiding(), "without stream 22: rides unarmed by spear");
            }
            real.getRidingEntity().setDead();
            real.setDead();
        }
        // Not natural (a spawn egg, a spawner): no rider.
        EntityZombieHorse egg = horse(c, area.add(28, 0, 28));
        tick(egg);
        c.ok(!egg.isBeingRidden(), "a spawn-egg zombie horse comes alone");
        egg.setDead();
        // The rider's chicken jockey roll never leaves a chicken behind or ridden.
        int chickens = 0;
        for (int i = 0; i < 40; i++) {
            EntityZombieHorse h = horse(c, area.add(4 + (i % 5) * 5, 0, 4 + (i / 5) * 3));
            EntityZombie z = MountsTestAccess.spawnRider(h);
            c.ok(z.getRidingEntity() == h, "rider " + i + " rides the horse");
            z.setDead();
            h.setDead();
        }
        for (Entity e : c.world.loadedEntityList) {
            if (e instanceof net.minecraft.entity.passive.EntityChicken && !e.isDead && e.getDistanceSq(area) < 64 * 64) {
                chickens++;
            }
        }
        c.eq(0, chickens, "no leftover jockey chickens");
    }

    // ------------------------------------------------------------------ drops

    private void drops(Check c) {
        LootTable table = c.world.getLootTableManager().getLootTableFromLocation(LootTableList.ENTITIES_ZOMBIE_HORSE);
        Random r = new Random(1);
        int min = 99;
        int max = 0;
        for (int i = 0; i < 200; i++) {
            int n = 0;
            for (ItemStack s : table.generateLootForPools(r, new LootContext.Builder(c.world).build())) {
                c.ok(s.getItem() == Items.ROTTEN_FLESH, "zombie horse drops rotten flesh only");
                n += s.getCount();
            }
            min = Math.min(min, n);
            max = Math.max(max, n);
        }
        c.eq(2, min, "at least 2 rotten flesh");
        c.eq(3, max, "at most 3 without Looting");

        BlockPos area = c.area();
        EntityZombieHorse h = horse(c, area.add(8, 0, 8));
        EntityZombie z = new EntityZombie(c.world);
        z.setPosition(h.posX, h.posY, h.posZ);
        c.world.spawnEntity(z);
        z.startRiding(h, true);
        int mushrooms = 0;
        int most = 0;
        for (int i = 0; i < 200; i++) {
            ArrayList<EntityItem> list = new ArrayList<EntityItem>();
            MinecraftForge.EVENT_BUS.post(new LivingDropsEvent(z, DamageSource.GENERIC, list, 0, false));
            for (EntityItem e : list) {
                if (e.getItem().getItem() == Item.getItemFromBlock(Blocks.RED_MUSHROOM)) {
                    mushrooms++;
                    most = Math.max(most, e.getItem().getCount());
                }
            }
        }
        c.ok(mushrooms > 50 && mushrooms < 150, "a riding zombie drops a red mushroom about half the time: " + mushrooms);
        c.eq(1, most, "one at a time without Looting");
        z.dismountRidingEntity();
        ArrayList<EntityItem> list = new ArrayList<EntityItem>();
        for (int i = 0; i < 50; i++) {
            MinecraftForge.EVENT_BUS.post(new LivingDropsEvent(z, DamageSource.GENERIC, list, 0, false));
        }
        c.eq(0, list.size(), "a zombie on foot drops none");
        z.setDead();
        h.setDead();
    }

    // ------------------------------------------------------------------ floating and reach

    private void floating(Check c) {
        BlockPos area = c.area();
        BlockPos pool = area.add(8, 0, 8);
        c.fill(pool.add(-2, 0, -2), pool.add(2, 2, 2), Blocks.WATER.getDefaultState());
        c.ok(MountsTestAccess.waterAboveFeet(at(new EntityHorse(c.world), pool)) > Riding.FLUID_JUMP_THRESHOLD, "three deep: over the threshold");

        EntityHorse horse = at(new EntityHorse(c.world), pool);
        c.world.spawnEntity(horse);
        FakePlayer player = c.player("mounts", pool);
        player.startRiding(horse, true);
        ReflectionHelper.setPrivateValue(Entity.class, horse, true, "inWater", "field_70171_ac");
        horse.motionY = 0.0D;
        tick(horse);
        c.eq(0.04, Math.round(horse.motionY * 1000.0) / 1000.0, "a ridden horse in deep water rises 0.04");
        player.dismountRidingEntity();
        horse.motionY = 0.0D;
        tick(horse);
        c.eq(0.0, horse.motionY, "unridden it does not");

        EntitySkeletonHorse sk = at(new EntitySkeletonHorse(c.world), pool.add(1, 0, 1));
        c.world.spawnEntity(sk);
        player.startRiding(sk, true);
        ReflectionHelper.setPrivateValue(Entity.class, sk, true, "inWater", "field_70171_ac");
        sk.motionY = 0.0D;
        tick(sk);
        c.eq(0.0, sk.motionY, "a skeleton horse is not in #can_float_while_ridden");
        player.dismountRidingEntity();

        c.fill(pool.add(-2, 1, -2), pool.add(2, 2, 2), Blocks.AIR.getDefaultState());
        EntityHorse shallow = at(new EntityHorse(c.world), pool);
        c.ok(MountsTestAccess.waterAboveFeet(shallow) < Riding.FLUID_JUMP_THRESHOLD + 0.5, "one deep: " + MountsTestAccess.waterAboveFeet(shallow));
        shallow.setPosition(shallow.posX, pool.getY() + 0.6, shallow.posZ);
        c.ok(MountsTestAccess.waterAboveFeet(shallow) < Riding.FLUID_JUMP_THRESHOLD, "standing high in a shallow pool: under it");
        horse.setDead();
        sk.setDead();
    }

    private static <E extends Entity> E at(E e, BlockPos p) {
        e.setPosition(p.getX() + 0.5, p.getY(), p.getZ() + 0.5);
        return e;
    }

    private void reach(Check c) {
        BlockPos area = c.area();
        EntityZombie z = new EntityZombie(c.world);
        z.setPosition(area.getX() + 8.5, area.getY(), area.getZ() + 8.5);
        c.world.spawnEntity(z);
        c.ok(hasGoal(z, g -> g instanceof Riding.MountedZombieAttack), "zombies get the mounted-reach attack");
        EntityZombieHorse h = horse(c, area.add(8, 0, 8));
        z.startRiding(h, true);
        z.setPosition(h.posX, h.posY + h.getMountedYOffset() + z.getYOffset(), h.posZ);
        EntityPig near = new EntityPig(c.world);
        near.setPosition(h.posX + 1.9, area.getY(), h.posZ);
        EntityPig far = new EntityPig(c.world);
        far.setPosition(h.posX + 3.0, area.getY(), h.posZ);
        Riding.MountedZombieAttack attack = null;
        for (EntityAITasks.EntityAITaskEntry e : z.tasks.taskEntries) {
            if (e.action instanceof Riding.MountedZombieAttack) {
                attack = (Riding.MountedZombieAttack) e.action;
            }
        }
        if (attack != null) {
            c.ok(attack.reachForTest(near) > 1000.0, "a pig 1.9 blocks from the horse's middle is in reach");
            c.ok(attack.reachForTest(far) < 0.0, "one 3 blocks off is not");
            z.dismountRidingEntity();
            c.eq((double) (z.width * 2.0F * z.width * 2.0F + near.width), attack.reachForTest(near), "on foot, 1.12.2's reach");
        }
        z.setDead();
        h.setDead();
    }
}
