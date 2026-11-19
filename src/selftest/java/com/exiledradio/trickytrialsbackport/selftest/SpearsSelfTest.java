package com.exiledradio.trickytrialsbackport.selftest;

import com.exiledradio.trickytrialsbackport.ModConfig;
import com.exiledradio.trickytrialsbackport.asm.SpearTransformer;
import com.exiledradio.trickytrialsbackport.common.Content;
import com.exiledradio.trickytrialsbackport.common.Owners;
import com.exiledradio.trickytrialsbackport.spear.EntityAISpearUse;
import com.exiledradio.trickytrialsbackport.spear.EntityFireproofItem;
import com.exiledradio.trickytrialsbackport.spear.ItemSpear;
import com.exiledradio.trickytrialsbackport.spear.SpearAttacks;
import com.exiledradio.trickytrialsbackport.spear.SpearContent;
import com.exiledradio.trickytrialsbackport.spear.SpearEvents;
import com.exiledradio.trickytrialsbackport.spear.SpearJabMessage;
import com.exiledradio.trickytrialsbackport.spear.SpearTier;

import net.minecraft.advancements.Advancement;
import net.minecraft.enchantment.Enchantment;
import net.minecraft.enchantment.EnchantmentData;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.SharedMonsterAttributes;
import net.minecraft.entity.ai.EntityAITasks;
import net.minecraft.entity.ai.attributes.AttributeModifier;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.entity.monster.EntityPigZombie;
import net.minecraft.entity.monster.EntityZombie;
import net.minecraft.entity.passive.EntityPig;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.init.Blocks;
import net.minecraft.init.Enchantments;
import net.minecraft.init.Items;
import net.minecraft.inventory.EntityEquipmentSlot;
import net.minecraft.item.Item;
import net.minecraft.item.ItemEnchantedBook;
import net.minecraft.item.ItemStack;
import net.minecraft.util.DamageSource;
import net.minecraft.util.EnumHand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.TextComponentTranslation;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.EnumDifficulty;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.AnvilUpdateEvent;
import net.minecraftforge.fml.common.registry.EntityRegistry;
import net.minecraftforge.fml.relauncher.ReflectionHelper;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

/** Stream 22: spears, the jab, the charge, Lunge, mobs with spears. */
public final class SpearsSelfTest implements SelfTest {

    @Override
    public String name() {
        return "spears";
    }

    @Override
    public void run(Check c) {
        c.ok(ModConfig.spears.enabled, "spears enabled");
        if (!ModConfig.spears.enabled) {
            return;
        }
        items(c);
        tiers(c);
        recipes(c);
        lunge(c);
        anvil(c);
        BlockPos area = c.area();
        EntityPlayerMP player = Players.real(c.world, "spears");
        player.setPositionAndRotation(area.getX() + 4.5, area.getY(), area.getZ() + 4.5, 0.0F, 0.0F);
        player.rotationYawHead = 0.0F;
        c.world.spawnEntity(player);
        List<Entity> made = new ArrayList<Entity>();
        try {
            jab(c, player, area, made);
            charge(c, player, area, made);
            hitRate(c, player, made);
            lungeEffect(c, player);
            kabob(c, player, area, made);
            mobCharge(c, area, made);
        } finally {
            for (Entity e : made) {
                e.setDead();
            }
            c.world.removeEntityDangerously(player);
        }
        mobs(c, area);
    }

    // ------------------------------------------------------------------------------------------

    private static ItemSpear spear(SpearTier t) {
        return SpearContent.spear(t);
    }

    private static double modifier(ItemStack stack, String attribute) {
        Collection<AttributeModifier> m = stack.getAttributeModifiers(EntityEquipmentSlot.MAINHAND).get(attribute);
        return m.size() == 1 ? m.iterator().next().getAmount() : Double.NaN;
    }

    private void items(Check c) {
        String[] names = {"wooden_spear", "stone_spear", "copper_spear", "iron_spear", "golden_spear", "diamond_spear", "netherite_spear"};
        int[] uses = {59, 131, 190, 250, 32, 1561, 2031};
        double[] damage = {1, 2, 2, 3, 1, 4, 5};
        double[] speed = {1.0 / 0.65, 1.0 / 0.75, 1.0 / 0.85, 1.0 / 0.95, 1.0 / 0.95, 1.0 / 1.05, 1.0 / 1.15};
        int[] ench = {15, 5, 13, 14, 22, 10, 15};
        SpearTier[] tiers = SpearTier.values();
        for (int i = 0; i < tiers.length; i++) {
            ItemSpear s = spear(tiers[i]);
            ItemStack stack = new ItemStack(s);
            c.eq(Content.id(names[i]), s.getRegistryName(), names[i] + " registered");
            c.eq(uses[i], stack.getMaxDamage(), names[i] + " durability");
            c.eq(1, stack.getMaxStackSize(), names[i] + " one to a stack");
            c.eq(ench[i], s.getItemEnchantability(), names[i] + " enchantability");
            c.ok(Math.abs(1.0 + modifier(stack, SharedMonsterAttributes.ATTACK_DAMAGE.getName()) - damage[i]) < 1e-6,
                    names[i] + " attack damage " + damage[i]);
            c.ok(Math.abs(4.0 + modifier(stack, SharedMonsterAttributes.ATTACK_SPEED.getName()) - speed[i]) < 1e-4,
                    names[i] + " attack speed " + speed[i]);
            c.eq(72000, s.getMaxItemUseDuration(stack), names[i] + " charges as long as it is held");
            c.ok(!s.canHarvestBlock(Blocks.DIRT.getDefaultState()), names[i] + " mines nothing");
        }
        c.ok(spear(SpearTier.IRON).getIsRepairable(new ItemStack(spear(SpearTier.IRON)), new ItemStack(Items.IRON_INGOT)), "iron repairs the iron spear");
        c.ok(!spear(SpearTier.IRON).getIsRepairable(new ItemStack(spear(SpearTier.IRON)), new ItemStack(Items.DIAMOND)), "diamond does not");
        c.ok(spear(SpearTier.WOOD).getIsRepairable(new ItemStack(spear(SpearTier.WOOD)), new ItemStack(Blocks.PLANKS, 1, 3)), "any planks repair wood");
        c.ok(spear(SpearTier.STONE).getIsRepairable(new ItemStack(spear(SpearTier.STONE)), new ItemStack(Blocks.COBBLESTONE)), "cobblestone repairs stone");
        Item copper = Owners.item(Owners.CAVES_AND_CLIFFS, "copper_ingot");
        if (copper != null) {
            c.ok(spear(SpearTier.COPPER).getIsRepairable(new ItemStack(spear(SpearTier.COPPER)), new ItemStack(copper)), "the copper ingot repairs copper");
        }

        ItemStack netherite = new ItemStack(spear(SpearTier.NETHERITE));
        c.ok(spear(SpearTier.NETHERITE).hasCustomEntity(netherite), "a dropped netherite spear is its own entity");
        EntityItem plain = new EntityItem(c.world, 0, 100, 0, netherite);
        Entity drop = spear(SpearTier.NETHERITE).createEntity(c.world, plain, netherite);
        c.ok(drop instanceof EntityFireproofItem && drop.isImmuneToFire(), "that does not burn");
        c.ok(drop != null && !drop.attackEntityFrom(DamageSource.LAVA, 4.0F), "lava does not hurt it");
        c.ok(!spear(SpearTier.DIAMOND).hasCustomEntity(new ItemStack(spear(SpearTier.DIAMOND))), "a diamond spear burns");
        c.ok(EntityRegistry.getEntry(EntityFireproofItem.class) != null, "the fireproof item is registered (saved)");

        DamageSource src = SpearAttacks.source(new EntityZombie(c.world));
        c.eq("spear", src.getDamageType(), "spear damage type");
        EntityPig pig = new EntityPig(c.world);
        c.eq("death.attack.spear", ((TextComponentTranslation) src.getDeathMessage(pig)).getKey(), "was speared by");
    }

    private void tiers(Check c) {
        c.eq(15, SpearTier.WOOD.delayTicks, "wood: 0.75 s before a charge counts");
        c.eq(12, SpearTier.IRON.delayTicks, "iron: 0.6 s");
        c.eq(8, SpearTier.NETHERITE.delayTicks, "netherite: 0.4 s");
        c.eq(100, SpearTier.WOOD.dismount.maxDurationTicks, "wood dismounts for 5 s");
        c.eq(50, SpearTier.IRON.dismount.maxDurationTicks, "iron for 2.5 s");
        c.eq(135, SpearTier.IRON.knockback.maxDurationTicks, "iron knocks back for 6.75 s");
        c.eq(225, SpearTier.IRON.damage.maxDurationTicks, "iron damages for 11.25 s");
        c.eq(175, SpearTier.NETHERITE.damage.maxDurationTicks, "netherite for 8.75 s");
        c.eq(237, SpearTier.IRON.damageUseDuration(), "a mob holds an iron charge 237 ticks");
        c.eq(6.0F, SpearAttacks.chargeDamage(SpearTier.IRON, 1.0, 5.6), "iron at sprint speed: 1 + floor(5.6 x 0.95) = 6");
        // 0.7F is just under 0.7, so vanilla floors 6.9999999 to 6.
        c.eq(7.0F, SpearAttacks.chargeDamage(SpearTier.WOOD, 1.0, 10.0), "wood at 10 b/s: 1 + floor(10 x 0.7F) = 7, as vanilla's float rounds");
        c.eq(17.0F, SpearAttacks.chargeDamage(SpearTier.NETHERITE, 1.0, 14.0), "netherite at 14 b/s: 1 + floor(16.8) = 17");
        c.eq(14.0F, SpearAttacks.chargeDamage(SpearTier.DIAMOND, 3.0, 10.5), "a zombie's base 3 with diamond at 10.5 b/s: 3 + 11");
        SpearTier.Condition d = SpearTier.IRON.dismount;
        c.ok(d.test(0, 8.0, 8.0, 1.0) && !d.test(0, 7.9, 7.9, 1.0) && !d.test(51, 9.0, 9.0, 1.0), "iron dismounts from 8 b/s, for 50 ticks");
        c.ok(d.test(0, 1.61, 0.0, SpearTier.MOB_SPEED_FACTOR) && !d.test(0, 1.59, 0.0, SpearTier.MOB_SPEED_FACTOR), "a mob needs a fifth of that");
        SpearTier.Condition h = SpearTier.IRON.damage;
        c.ok(h.test(225, 0.0, 4.6, 1.0) && !h.test(225, 9.0, 4.5, 1.0), "damage needs 4.6 b/s faster than the target");
    }

    private void recipes(Check c) {
        ItemStack e = ItemStack.EMPTY;
        ItemStack stick = new ItemStack(Items.STICK);
        Object[][] mats = {{SpearTier.WOOD, new ItemStack(Blocks.PLANKS, 1, 2)}, {SpearTier.STONE, new ItemStack(Blocks.COBBLESTONE)},
                {SpearTier.IRON, new ItemStack(Items.IRON_INGOT)}, {SpearTier.GOLD, new ItemStack(Items.GOLD_INGOT)},
                {SpearTier.DIAMOND, new ItemStack(Items.DIAMOND)}};
        for (Object[] m : mats) {
            SpearTier t = (SpearTier) m[0];
            Crafting.expect(c, Crafting.grid(3, 3, e, e, (ItemStack) m[1], e, stick, e, stick, e, e), new ItemStack(spear(t)),
                    t.itemName + " from two sticks and its material");
        }
        Item copper = Owners.item(Owners.CAVES_AND_CLIFFS, "copper_ingot");
        if (copper != null) {
            Crafting.expect(c, Crafting.grid(3, 3, e, e, new ItemStack(copper), e, stick, e, stick, e, e), new ItemStack(spear(SpearTier.COPPER)),
                    "copper spear");
        }
        // Mirrored, as 1.21.11's shaped recipes allow.
        Crafting.expect(c, Crafting.grid(3, 3, new ItemStack(Items.IRON_INGOT), e, e, e, stick, e, e, e, stick), new ItemStack(spear(SpearTier.IRON)),
                "the mirrored shape");
        ItemStack none = net.minecraft.item.crafting.CraftingManager.findMatchingResult(
                Crafting.grid(3, 3, e, e, new ItemStack(Items.IRON_INGOT), e, stick, e, e, e, e), c.world);
        c.ok(none.isEmpty(), "one stick is not enough");
    }

    private void lunge(Check c) {
        Enchantment l = SpearContent.lunge;
        c.eq(Content.id("lunge"), l.getRegistryName(), "Lunge registered");
        c.eq(3, l.getMaxLevel(), "Lunge III");
        c.eq(Enchantment.Rarity.UNCOMMON, l.getRarity(), "uncommon (weight 5)");
        c.ok(!l.isTreasureEnchantment(), "not treasure");
        c.ok(l.getMinEnchantability(1) == 5 && l.getMinEnchantability(2) == 13 && l.getMinEnchantability(3) == 21, "costs from 5, 13, 21");
        c.ok(l.getMaxEnchantability(1) == 25 && l.getMaxEnchantability(3) == 41, "to 25 ... 41");
        ItemStack iron = new ItemStack(spear(SpearTier.IRON));
        c.ok(l.canApplyAtEnchantingTable(iron) && !l.canApplyAtEnchantingTable(new ItemStack(Items.DIAMOND_SWORD))
                && !l.canApplyAtEnchantingTable(new ItemStack(Items.BOW)), "Lunge on spears only");
        c.ok(l.isCompatibleWith(Enchantments.SHARPNESS) && l.isCompatibleWith(Enchantments.KNOCKBACK) && l.isCompatibleWith(Enchantments.UNBREAKING),
                "Lunge goes with anything");
        Enchantment[] yes = {Enchantments.SHARPNESS, Enchantments.SMITE, Enchantments.BANE_OF_ARTHROPODS, Enchantments.KNOCKBACK,
                Enchantments.FIRE_ASPECT, Enchantments.LOOTING, Enchantments.UNBREAKING};
        for (Enchantment y : yes) {
            c.ok(y.canApplyAtEnchantingTable(iron), y.getName() + " at the table on a spear");
        }
        c.ok(!Enchantments.SWEEPING.canApplyAtEnchantingTable(iron) && !Enchantments.EFFICIENCY.canApplyAtEnchantingTable(iron)
                && !Enchantments.POWER.canApplyAtEnchantingTable(iron), "not Sweeping Edge, Efficiency or Power");
        boolean onSpear = false;
        boolean onBook = false;
        boolean sweeping = false;
        for (int i = 0; i < 300; i++) {
            for (EnchantmentData d : EnchantmentHelper.buildEnchantmentList(c.world.rand, iron.copy(), 30, false)) {
                onSpear |= d.enchantment == l;
                sweeping |= d.enchantment == Enchantments.SWEEPING;
            }
            for (EnchantmentData d : EnchantmentHelper.buildEnchantmentList(c.world.rand, new ItemStack(Items.BOOK), 30, false)) {
                onBook |= d.enchantment == l;
            }
        }
        c.ok(onSpear, "the table offers Lunge on a spear");
        c.ok(onBook, "and on a book");
        c.ok(!sweeping, "never Sweeping Edge");
    }

    private void anvil(Check c) {
        ItemStack iron = new ItemStack(spear(SpearTier.IRON));
        AnvilUpdateEvent sharp = new AnvilUpdateEvent(iron, ItemEnchantedBook.getEnchantedItemStack(new EnchantmentData(Enchantments.SHARPNESS, 5)), "", 0);
        MinecraftForge.EVENT_BUS.post(sharp);
        c.ok(Integer.valueOf(5).equals(EnchantmentHelper.getEnchantments(sharp.getOutput()).get(Enchantments.SHARPNESS)), "an anvil puts Sharpness V on a spear");
        c.eq(5, sharp.getCost(), "for 5 levels (common, 1 a level)");
        AnvilUpdateEvent lunge = new AnvilUpdateEvent(iron, ItemEnchantedBook.getEnchantedItemStack(new EnchantmentData(SpearContent.lunge, 3)), "", 0);
        MinecraftForge.EVENT_BUS.post(lunge);
        c.ok(Integer.valueOf(3).equals(EnchantmentHelper.getEnchantments(lunge.getOutput()).get(SpearContent.lunge)), "and Lunge III");
        c.eq(3, lunge.getCost(), "for 3 levels (uncommon, halved from a book)");
        AnvilUpdateEvent sweep = new AnvilUpdateEvent(iron, ItemEnchantedBook.getEnchantedItemStack(new EnchantmentData(Enchantments.SWEEPING, 1)), "", 0);
        MinecraftForge.EVENT_BUS.post(sweep);
        c.ok(sweep.getOutput().isEmpty(), "but not Sweeping Edge");
    }

    // ------------------------------------------------------------------------------------------

    private static void ready(EntityPlayerMP player, ItemStack held) {
        player.resetActiveHand();
        player.setHeldItem(EnumHand.MAIN_HAND, held);
        player.getAttributeMap().removeAttributeModifiers(held.getAttributeModifiers(EntityEquipmentSlot.MAINHAND));
        player.getAttributeMap().applyAttributeModifiers(held.getAttributeModifiers(EntityEquipmentSlot.MAINHAND));
        ReflectionHelper.setPrivateValue(EntityLivingBase.class, player, 100, "ticksSinceLastSwing", "field_184617_aD");
        player.rotationYaw = 0.0F;
        player.rotationYawHead = 0.0F;
        player.rotationPitch = 0.0F;
        player.motionX = player.motionY = player.motionZ = 0.0;
        player.getFoodStats().setFoodLevel(20);
        SpearAttacks.setMotion(player, 0, 0, 0);
    }

    /** A zombie that stands still, 1000 health, no armor, no knockback resistance: dz blocks ahead of the player (+z). */
    private static EntityZombie dummy(Check c, EntityPlayerMP player, double dz, List<Entity> made) {
        EntityZombie z = new EntityZombie(c.world);
        z.setPosition(player.posX, player.posY, player.posZ + dz);
        z.getEntityAttribute(SharedMonsterAttributes.MAX_HEALTH).setBaseValue(1000);
        z.setHealth(1000);
        z.getEntityAttribute(SharedMonsterAttributes.ARMOR).setBaseValue(0.0);
        z.setNoAI(true);
        z.setItemStackToSlot(EntityEquipmentSlot.MAINHAND, ItemStack.EMPTY);
        c.world.spawnEntity(z);
        SpearAttacks.setMotion(z, 0, 0, 0);
        made.add(z);
        return z;
    }

    private static float lost(EntityZombie z, float before) {
        return before - z.getHealth();
    }

    private void jab(Check c, EntityPlayerMP player, BlockPos area, List<Entity> made) {
        ItemStack iron = new ItemStack(spear(SpearTier.IRON));
        EntityZombie near = dummy(c, player, 1.5, made);
        EntityZombie mid = dummy(c, player, 3.0, made);
        EntityZombie far = dummy(c, player, 5.0, made);
        ready(player, iron);
        List<Entity> inLine = SpearAttacks.hitsAlong(player);
        c.ok(inLine.size() == 1 && inLine.get(0) == mid, "reach 2 to 4.5: only the zombie 3 blocks off is in line, got " + inLine);
        SpearJabMessage.apply(player);
        c.ok(Math.abs(lost(mid, 1000) - 3.0F) < 0.01F, "a jab with an iron spear: 3, got " + lost(mid, 1000));
        c.ok(lost(near, 1000) == 0 && lost(far, 1000) == 0, "1.5 and 5 blocks off are out of reach");
        c.ok(mid.motionZ > 0.1, "knocked back away from the player: " + mid.motionZ);
        c.ok(iron.getItemDamage() == 1, "the hit wears the spear by one: " + iron.getItemDamage());

        float before = mid.getHealth();
        mid.hurtResistantTime = 0;
        SpearJabMessage.apply(player);
        c.eq(before, mid.getHealth(), "no jab until the attack timer is full again");
        mid.setDead();
        near.setDead();

        player.setGameType(net.minecraft.world.GameType.CREATIVE);
        ready(player, iron);
        far.hurtResistantTime = 0;
        SpearJabMessage.apply(player);
        c.ok(lost(far, 1000) > 2.9F, "creative reaches 6.5: the zombie 5 blocks off is hit");
        player.setGameType(net.minecraft.world.GameType.SURVIVAL);
        far.setDead();

        EntityZombie a = dummy(c, player, 2.6, made);
        EntityZombie b = dummy(c, player, 3.6, made);
        ItemStack diamond = new ItemStack(spear(SpearTier.DIAMOND));
        diamond.addEnchantment(Enchantments.SHARPNESS, 5);
        ready(player, diamond);
        SpearJabMessage.apply(player);
        c.ok(Math.abs(lost(a, 1000) - 7.0F) < 0.01F && Math.abs(lost(b, 1000) - 7.0F) < 0.01F,
                "one jab hits both zombies in line, a diamond spear with Sharpness V for 4 + 3 each: " + lost(a, 1000) + ", " + lost(b, 1000));
        a.setDead();
        b.setDead();

        c.world.setBlockState(new BlockPos(player.posX, player.posY + 1, player.posZ + 1), Blocks.STONE.getDefaultState());
        EntityZombie walled = dummy(c, player, 3.0, made);
        ready(player, iron);
        SpearJabMessage.apply(player);
        c.eq(1000.0F, walled.getHealth(), "a block within the minimum reach stops the jab");
        c.world.setBlockToAir(new BlockPos(player.posX, player.posY + 1, player.posZ + 1));
        walled.setDead();
    }

    private static int remainingAfter(int ticks) {
        return 72000 - ticks;
    }

    private void charge(Check c, EntityPlayerMP player, BlockPos area, List<Entity> made) {
        ItemStack iron = new ItemStack(spear(SpearTier.IRON));
        ready(player, iron);
        player.setActiveHand(EnumHand.MAIN_HAND);
        c.ok(player.isHandActive(), "the charge is held");
        EntityZombie z = dummy(c, player, 3.0, made);
        SpearAttacks.setMotion(player, 0, 0, 0.28);
        c.ok(!SpearAttacks.chargeTick(player, iron, remainingAfter(5)), "nothing in the first 0.6 s");
        c.ok(SpearAttacks.chargeTick(player, iron, remainingAfter(13)), "then the charge hits");
        c.ok(Math.abs(lost(z, 1000) - 6.0F) < 0.01F, "at sprint speed (5.6 b/s): 1 + floor(5.6 x 0.95) = 6, got " + lost(z, 1000));
        c.ok(z.motionZ > 0.1, "and knocks back");
        c.ok(!SpearAttacks.chargeTick(player, iron, remainingAfter(14)), "the same target waits 10 ticks");
        float h = z.getHealth();

        // A new charge, faster.
        ready(player, iron);
        player.setActiveHand(EnumHand.MAIN_HAND);
        z.hurtResistantTime = 0;
        z.motionZ = 0;
        SpearAttacks.setMotion(player, 0, 0, 0.5);
        SpearAttacks.chargeTick(player, iron, remainingAfter(20));
        c.ok(Math.abs(h - z.getHealth() - 10.0F) < 0.01F, "at 10 b/s: 1 + floor(9.5) = 10, got " + (h - z.getHealth()));

        // The target running away from the wielder closes slower.
        ready(player, iron);
        player.setActiveHand(EnumHand.MAIN_HAND);
        z.hurtResistantTime = 0;
        h = z.getHealth();
        SpearAttacks.setMotion(player, 0, 0, 0.5);
        SpearAttacks.setMotion(z, 0, 0, 0.2);
        SpearAttacks.chargeTick(player, iron, remainingAfter(20));
        c.ok(Math.abs(h - z.getHealth() - 6.0F) < 0.01F, "6 b/s closing: 1 + floor(5.7) = 6, got " + (h - z.getHealth()));
        SpearAttacks.setMotion(z, 0, 0, 0);

        // Too slow: nothing at all.
        ready(player, iron);
        player.setActiveHand(EnumHand.MAIN_HAND);
        z.hurtResistantTime = 0;
        z.motionZ = 0;
        h = z.getHealth();
        SpearAttacks.setMotion(player, 0, 0, 0.1);
        c.ok(!SpearAttacks.chargeTick(player, iron, remainingAfter(20)), "walking (2 b/s) does nothing");
        c.eq(h, z.getHealth(), "no damage walking");

        // Past the damage window: knockback still, no damage.
        ready(player, iron);
        player.setActiveHand(EnumHand.MAIN_HAND);
        z.hurtResistantTime = 0;
        h = z.getHealth();
        SpearAttacks.setMotion(player, 0, 0, 0.5);
        SpearAttacks.chargeTick(player, iron, remainingAfter(12 + 140));
        c.ok(Math.abs(h - z.getHealth() - 10.0F) < 0.01F, "after 7 s (past knockback's 6.75 s) it still damages, until 11.25 s");
        z.hurtResistantTime = 0;
        h = z.getHealth();
        ready(player, iron);
        player.setActiveHand(EnumHand.MAIN_HAND);
        SpearAttacks.setMotion(player, 0, 0, 0.5);
        SpearAttacks.chargeTick(player, iron, remainingAfter(12 + 230));
        c.eq(h, z.getHealth(), "after 11.5 s the charge does no damage");
        z.setDead();

        // Dismounting.
        EntityPig pig = new EntityPig(c.world);
        pig.setPosition(player.posX, player.posY, player.posZ + 3.0);
        c.world.spawnEntity(pig);
        made.add(pig);
        EntityZombie rider = dummy(c, player, 3.0, made);
        rider.startRiding(pig, true);
        SpearAttacks.setMotion(pig, 0, 0, 0);
        c.ok(rider.isRiding(), "a zombie rides a pig");
        ready(player, iron);
        player.setActiveHand(EnumHand.MAIN_HAND);
        SpearAttacks.setMotion(player, 0, 0, 0.45);
        SpearAttacks.chargeTick(player, iron, remainingAfter(14));
        c.ok(!rider.isRiding(), "a charge at 9 b/s knocks it off");
        player.resetActiveHand();
    }

    /**
     * A 40-tick sprinting charge into a standing zombie, a second zombie stepping into line 5 ticks later: each target
     * is hit every 10 ticks on its own clock, the hit sound comes at most once every 11 ticks, and the charge survives
     * the wear its hits put on the spear (the client gets a new stack object each time).
     */
    private void hitRate(Check c, EntityPlayerMP player, List<Entity> made) {
        ItemStack iron = new ItemStack(spear(SpearTier.IRON));
        ready(player, iron);
        player.setActiveHand(EnumHand.MAIN_HAND);

        ItemStack worn = iron.copy();
        worn.setItemDamage(iron.getItemDamage() + 1);
        player.setHeldItem(EnumHand.MAIN_HAND, worn);
        try {
            java.lang.reflect.Method update = ReflectionHelper.findMethod(EntityLivingBase.class, "updateActiveHand", "func_184608_ct");
            update.invoke(player);
        } catch (ReflectiveOperationException e) {
            c.fail("updateActiveHand: " + e);
        }
        c.ok(player.isHandActive() && player.getActiveItemStack() == worn, "a worn copy of the spear keeps the charge going");
        c.ok(!net.minecraftforge.common.ForgeHooks.canContinueUsing(worn, new ItemStack(spear(SpearTier.GOLD))), "another spear ends it");
        player.resetActiveHand();

        ready(player, iron);
        player.setActiveHand(EnumHand.MAIN_HAND);
        EntityZombie a = dummy(c, player, 3.0, made);
        EntityZombie b = null;
        net.minecraft.world.storage.WorldInfo info = c.world.getWorldInfo();
        long base = info.getWorldTotalTime() + 20;
        int hitsA = 0;
        int hitsB = 0;
        int sounds = 0;
        long lastA = -1;
        long lastB = -1;
        long lastSound = SpearAttacks.lastFeedback(player);
        StringBuilder log = new StringBuilder();
        for (int t = 0; t < 40; t++) {
            info.setWorldTotalTime(base + t);
            if (t == 5) {
                b = dummy(c, player, 3.6, made);
            }
            SpearAttacks.setMotion(player, 0, 0, 0.28);
            SpearAttacks.chargeTick(player, player.getHeldItemMainhand(), remainingAfter(SpearTier.IRON.delayTicks + t));
            if (SpearAttacks.lastStab(player, a) != lastA) {
                lastA = SpearAttacks.lastStab(player, a);
                hitsA++;
                log.append(" a@").append(t);
            }
            if (b != null && SpearAttacks.lastStab(player, b) != lastB) {
                lastB = SpearAttacks.lastStab(player, b);
                hitsB++;
                log.append(" b@").append(t);
            }
            if (SpearAttacks.lastFeedback(player) != lastSound) {
                lastSound = SpearAttacks.lastFeedback(player);
                sounds++;
                log.append(" sound@").append(t);
            }
        }
        info.setWorldTotalTime(base + 40);
        c.eq(4, hitsA, "40 ticks at sprint speed hit the zombie 4 times (every 10 ticks):" + log);
        c.eq(4, hitsB, "the second zombie keeps its own 10-tick clock (5, 15, 25, 35)");
        c.eq(3, sounds, "the hit sound plays 3 times (0, 15, 30: once every 11 ticks at most)");
        c.ok(player.isHandActive(), "the charge is still held after 40 ticks");
        player.resetActiveHand();
        a.setDead();
        if (b != null) {
            b.setDead();
        }
    }

    private void lungeEffect(Check c, EntityPlayerMP player) {
        ItemStack lunging = new ItemStack(spear(SpearTier.IRON));
        lunging.addEnchantment(SpearContent.lunge, 2);
        ready(player, lunging);
        float food = exhaustion(player);
        int dmg = lunging.getItemDamage();
        SpearJabMessage.apply(player);
        c.ok(Math.abs(player.motionZ - 0.916) < 0.02, "Lunge II pushes 0.916 along the look: " + player.motionZ);
        c.ok(Math.abs(player.motionY) < 1e-6, "level only");
        c.eq(dmg + 1, lunging.getItemDamage(), "Lunge wears the spear by one more");
        c.ok(exhaustion(player) - food >= 8.0F, "and tires the player by 4 a level: " + (exhaustion(player) - food));

        ready(player, lunging);
        player.getFoodStats().setFoodLevel(6);
        SpearJabMessage.apply(player);
        c.ok(Math.abs(player.motionZ) < 1e-6, "no Lunge with 6 food or less");

        ready(player, lunging);
        player.rotationPitch = -60.0F;
        SpearJabMessage.apply(player);
        c.ok(Math.abs(player.motionZ - 0.458) < 0.02, "looking up 60 degrees, half as far: " + player.motionZ);
        ready(player, new ItemStack(spear(SpearTier.IRON)));
        SpearJabMessage.apply(player);
        c.ok(Math.abs(player.motionZ) < 1e-6, "no push without Lunge");
    }

    private static float exhaustion(EntityPlayerMP player) {
        Float f = ReflectionHelper.getPrivateValue(net.minecraft.util.FoodStats.class, player.getFoodStats(), "foodExhaustionLevel", "field_75126_c");
        return f == null ? 0.0F : f;
    }

    private void kabob(Check c, EntityPlayerMP player, BlockPos area, List<Entity> made) {
        Advancement adv = c.world.getMinecraftServer().getAdvancementManager().getAdvancement(Content.id("adventure/spear_many_mobs"));
        c.ok(adv != null, "Mob Kabob loaded");
        ItemStack netherite = new ItemStack(spear(SpearTier.NETHERITE));
        ready(player, netherite);
        player.setActiveHand(EnumHand.MAIN_HAND);
        List<EntityZombie> line = new ArrayList<EntityZombie>();
        for (int i = 0; i < 4; i++) {
            line.add(dummy(c, player, 2.4 + 0.65 * i, made));
        }
        SpearAttacks.setMotion(player, 0, 0, 0.5);
        SpearAttacks.chargeTick(player, netherite, remainingAfter(10));
        c.eq(4, SpearAttacks.stabbedLiving(player), "one charge through four zombies");
        c.ok(adv != null && !player.getAdvancements().getProgress(adv).isDone(), "four are not a kabob");
        line.add(dummy(c, player, 2.4 + 0.65 * 4, made));
        SpearAttacks.chargeTick(player, netherite, remainingAfter(11));
        c.eq(5, SpearAttacks.stabbedLiving(player), "a fifth, reached by the move ahead");
        c.ok(adv != null && player.getAdvancements().getProgress(adv).isDone(), "Mob Kabob");
        player.resetActiveHand();
        for (EntityZombie z : line) {
            z.setDead();
        }
    }

    private void mobCharge(Check c, BlockPos area, List<Entity> made) {
        EntityZombie z = new EntityZombie(c.world);
        z.setPosition(area.getX() + 20.5, area.getY(), area.getZ() + 4.5);
        z.rotationYaw = 0.0F;
        z.rotationYawHead = 0.0F;
        z.rotationPitch = 0.0F;
        ItemStack iron = new ItemStack(spear(SpearTier.IRON));
        z.setItemStackToSlot(EntityEquipmentSlot.MAINHAND, iron);
        c.world.spawnEntity(z);
        made.add(z);
        boolean hasTask = false;
        for (EntityAITasks.EntityAITaskEntry t : z.tasks.taskEntries) {
            hasTask |= t.action instanceof EntityAISpearUse;
        }
        c.eq(ModConfig.spears.mobCharges, hasTask, "zombies get the spear charge");
        z.setNoAI(true);
        EntityZombie target = new EntityZombie(c.world);
        target.setPosition(z.posX, z.posY, z.posZ + 1.7);
        target.getEntityAttribute(SharedMonsterAttributes.MAX_HEALTH).setBaseValue(100);
        target.setHealth(100);
        target.getEntityAttribute(SharedMonsterAttributes.ARMOR).setBaseValue(0.0);
        target.setNoAI(true);
        c.world.spawnEntity(target);
        made.add(target);
        EntityZombie beyond = new EntityZombie(c.world);
        beyond.setPosition(z.posX, z.posY, z.posZ + 3.0);
        beyond.setNoAI(true);
        c.world.spawnEntity(beyond);
        made.add(beyond);
        z.setActiveHand(EnumHand.MAIN_HAND);
        SpearAttacks.setMotion(z, 0, 0, 0.1);
        SpearAttacks.setMotion(target, 0, 0, 0);
        List<Entity> inLine = SpearAttacks.hitsAlong(z);
        c.ok(inLine.size() == 1 && inLine.get(0) == target, "a mob's reach is half a player's (1 to 2.25): " + inLine);
        SpearAttacks.chargeTick(z, iron, remainingAfter(13));
        c.ok(Math.abs(100 - target.getHealth() - 4.0F) < 0.01F, "a zombie charging at 2 b/s: its base 3 + floor(1.9) = 4, got " + (100 - target.getHealth()));
    }

    private void mobs(Check c, BlockPos area) {
        c.ok(SpearTransformer.zombie && SpearTransformer.pigZombie, "P20 applied to EntityZombie and EntityPigZombie");
        if (!ModConfig.spears.mobSpears) {
            return;
        }
        DifficultyInstance hard = new DifficultyInstance(EnumDifficulty.HARD, 10000000L, 10000000L, 1.0F);
        EntityZombie z = new EntityZombie(c.world);
        int spears = 0;
        for (int i = 0; i < 4000; i++) {
            z.setItemStackToSlot(EntityEquipmentSlot.MAINHAND, new ItemStack(Items.IRON_SWORD));
            SpearEvents.spawnWeapon(z, hard);
            spears += z.getHeldItemMainhand().getItem() == spear(SpearTier.IRON) ? 1 : 0;
        }
        c.ok(Math.abs(spears / 4000.0 - 0.5) < 0.04, "half of 1.12.2's iron swords become spears: " + spears / 4000.0);
        z.setItemStackToSlot(EntityEquipmentSlot.MAINHAND, new ItemStack(Items.IRON_SHOVEL));
        SpearEvents.spawnWeapon(z, hard);
        c.eq(Items.IRON_SHOVEL, z.getHeldItemMainhand().getItem(), "shovels stay");
        EntityPigZombie pigman = new EntityPigZombie(c.world);
        int golden = 0;
        for (int i = 0; i < 6000; i++) {
            pigman.setItemStackToSlot(EntityEquipmentSlot.MAINHAND, new ItemStack(Items.GOLDEN_SWORD));
            SpearEvents.spawnWeapon(pigman, hard);
            golden += pigman.getHeldItemMainhand().getItem() == spear(SpearTier.GOLD) ? 1 : 0;
        }
        c.ok(Math.abs(golden / 6000.0 - 0.05) < 0.015, "1 zombie pigman in 20 holds a golden spear: " + golden / 6000.0);

        // Through the patched spawn: Hard, 6000 zombies (5% armed, a sixth of those with spears).
        EnumDifficulty was = c.world.getDifficulty();
        c.world.getWorldInfo().setDifficulty(EnumDifficulty.HARD);
        int sword = 0;
        int spear = 0;
        int shovel = 0;
        try {
            for (int i = 0; i < 6000; i++) {
                EntityZombie fresh = new EntityZombie(c.world);
                fresh.setPosition(area.getX() + 8, area.getY(), area.getZ() + 8);
                fresh.onInitialSpawn(hard, null);
                Item held = fresh.getHeldItemMainhand().getItem();
                sword += held == Items.IRON_SWORD ? 1 : 0;
                spear += held == spear(SpearTier.IRON) ? 1 : 0;
                shovel += held == Items.IRON_SHOVEL ? 1 : 0;
            }
        } finally {
            c.world.getWorldInfo().setDifficulty(was);
        }
        for (Entity e : c.world.getEntitiesWithinAABB(net.minecraft.entity.passive.EntityChicken.class,
                new net.minecraft.util.math.AxisAlignedBB(area).grow(32))) {
            e.setDead();
        }
        int armed = sword + spear + shovel;
        c.ok(armed > 200 && spear > armed / 12 && spear < armed / 3 && sword > armed / 12 && shovel > armed / 2,
                "spawned zombies: " + sword + " swords, " + spear + " spears, " + shovel + " shovels (1 : 1 : 4)");
    }
}
