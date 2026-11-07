package com.exiledradio.trickytrialsbackport.selftest;

import com.exiledradio.trickytrialsbackport.common.Content;
import com.exiledradio.trickytrialsbackport.common.Owners;
import com.exiledradio.trickytrialsbackport.common.WindBurst;
import com.exiledradio.trickytrialsbackport.mace.MaceContent;

import net.minecraft.advancements.Advancement;
import net.minecraft.enchantment.Enchantment;
import net.minecraft.enchantment.EnchantmentData;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.SharedMonsterAttributes;
import net.minecraft.entity.ai.attributes.AttributeModifier;
import net.minecraft.entity.monster.EntityZombie;
import net.minecraft.entity.passive.EntityPig;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.init.Enchantments;
import net.minecraft.init.Items;
import net.minecraft.inventory.EntityEquipmentSlot;
import net.minecraft.item.EnumRarity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemEnchantedBook;
import net.minecraft.item.ItemStack;
import net.minecraft.util.CombatRules;
import net.minecraft.util.EnumHand;
import net.minecraft.util.math.BlockPos;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.AnvilUpdateEvent;
import net.minecraftforge.fml.relauncher.ReflectionHelper;

import java.util.Collection;
import java.util.Map;

/** Stream 5: the mace, the heavy core and the mace enchantments. */
public final class MaceSelfTest implements SelfTest {

    @Override
    public String name() {
        return "mace";
    }

    @Override
    public void run(Check c) {
        items(c);
        enchantments(c);
        anvil(c);
        BlockPos area = c.area();
        EntityPlayerMP player = Players.real(c.world, "mace");
        player.setPosition(area.getX() + 4.5, area.getY(), area.getZ() + 4.5);
        c.world.spawnEntity(player);
        try {
            smash(c, player, area);
            breach(c, player, area);
            overkill(c, player, area);
        } finally {
            c.world.removeEntityDangerously(player);
        }
    }

    private void items(Check c) {
        ItemStack mace = new ItemStack(MaceContent.mace);
        c.eq(Content.id("mace"), MaceContent.mace.getRegistryName(), "mace");
        c.eq(500, mace.getMaxDamage(), "500 durability");
        c.eq(1, mace.getMaxStackSize(), "one to a stack");
        c.eq(EnumRarity.EPIC, mace.getRarity(), "epic");
        Collection<AttributeModifier> damage = mace.getAttributeModifiers(EntityEquipmentSlot.MAINHAND)
                .get(SharedMonsterAttributes.ATTACK_DAMAGE.getName());
        Collection<AttributeModifier> speed = mace.getAttributeModifiers(EntityEquipmentSlot.MAINHAND)
                .get(SharedMonsterAttributes.ATTACK_SPEED.getName());
        c.ok(damage.size() == 1 && Math.abs(damage.iterator().next().getAmount() - 5.0) < 1e-9, "6 attack damage (5 over a fist)");
        c.ok(speed.size() == 1 && Math.abs(speed.iterator().next().getAmount() + 3.4) < 1e-9, "0.6 attack speed");
        Item rod = Owners.ours("breeze_rod");
        c.ok(rod != null && MaceContent.mace.getIsRepairable(mace, new ItemStack(rod)), "repaired with a breeze rod");
        c.ok(!MaceContent.mace.getIsRepairable(mace, new ItemStack(Items.IRON_INGOT)), "not with iron");
        Crafting.expect(c, Crafting.grid(1, 2, new ItemStack(MaceContent.heavyCore), new ItemStack(rod)), mace, "heavy core over a breeze rod");

        c.eq(Content.id("heavy_core"), MaceContent.heavyCore.getRegistryName(), "heavy core");
        c.eq(10.0F, MaceContent.heavyCore.getDefaultState().getBlockHardness(c.world, BlockPos.ORIGIN), "heavy core hardness 10");
        c.ok(Math.abs(MaceContent.heavyCore.getExplosionResistance(null) - 1200.0F) < 0.01F, "blast resistance 1200");
        c.ok(MaceContent.heavyCore.getDefaultState().getMaterial().isToolNotRequired(), "mined by hand");
        c.eq(EnumRarity.EPIC, new ItemStack(MaceContent.heavyCore).getRarity(), "heavy core item is epic");
        c.ok(!MaceContent.heavyCore.getDefaultState().isFullCube(), "not a full cube");
    }

    private void enchantments(Check c) {
        c.eq(5, MaceContent.density.getMaxLevel(), "Density V");
        c.eq(4, MaceContent.breach.getMaxLevel(), "Breach IV");
        c.eq(3, MaceContent.windBurst.getMaxLevel(), "Wind Burst III");
        c.ok(!MaceContent.density.isTreasureEnchantment() && !MaceContent.breach.isTreasureEnchantment()
                && MaceContent.windBurst.isTreasureEnchantment(), "Wind Burst alone is treasure");
        c.eq(Enchantment.Rarity.UNCOMMON, MaceContent.density.getRarity(), "Density uncommon");
        c.eq(Enchantment.Rarity.RARE, MaceContent.breach.getRarity(), "Breach rare");
        c.eq(5, MaceContent.density.getMinEnchantability(1), "Density I from 5");
        c.eq(37, MaceContent.density.getMinEnchantability(5), "Density V from 37");
        c.eq(42, MaceContent.breach.getMinEnchantability(4), "Breach IV from 42");
        c.ok(!MaceContent.density.isCompatibleWith(MaceContent.breach), "Density and Breach exclude each other");
        c.ok(!MaceContent.density.isCompatibleWith(Enchantments.SMITE) && !MaceContent.breach.isCompatibleWith(Enchantments.SHARPNESS),
                "and the damage enchantments");
        c.ok(MaceContent.density.isCompatibleWith(MaceContent.windBurst) && MaceContent.breach.isCompatibleWith(Enchantments.UNBREAKING),
                "Wind Burst and Unbreaking go with either");

        ItemStack mace = new ItemStack(MaceContent.mace);
        ItemStack sword = new ItemStack(Items.DIAMOND_SWORD);
        c.ok(MaceContent.density.canApplyAtEnchantingTable(mace) && !MaceContent.density.canApplyAtEnchantingTable(sword), "Density on maces only");
        c.ok(Enchantments.SMITE.canApplyAtEnchantingTable(mace) && Enchantments.FIRE_ASPECT.canApplyAtEnchantingTable(mace)
                && Enchantments.BANE_OF_ARTHROPODS.canApplyAtEnchantingTable(mace), "Smite, Bane and Fire Aspect at the table");
        c.ok(!Enchantments.SHARPNESS.canApplyAtEnchantingTable(mace) && !Enchantments.KNOCKBACK.canApplyAtEnchantingTable(mace)
                && !Enchantments.LOOTING.canApplyAtEnchantingTable(mace), "no Sharpness, Knockback or Looting");
        c.ok(Enchantments.UNBREAKING.canApplyAtEnchantingTable(mace), "Unbreaking");
        boolean sawDensity = false;
        boolean sawWindBurst = false;
        for (int i = 0; i < 200; i++) {
            for (EnchantmentData d : EnchantmentHelper.buildEnchantmentList(c.world.rand, mace.copy(), 30, false)) {
                sawDensity |= d.enchantment == MaceContent.density;
                sawWindBurst |= d.enchantment == MaceContent.windBurst;
            }
        }
        c.ok(sawDensity && !sawWindBurst, "the table offers Density, never Wind Burst");
    }

    private void anvil(Check c) {
        ItemStack mace = new ItemStack(MaceContent.mace);
        ItemStack smite = ItemEnchantedBook.getEnchantedItemStack(new EnchantmentData(Enchantments.SMITE, 5));
        AnvilUpdateEvent event = new AnvilUpdateEvent(mace, smite, "", 0);
        MinecraftForge.EVENT_BUS.post(event);
        Map<Enchantment, Integer> out = EnchantmentHelper.getEnchantments(event.getOutput());
        c.ok(Integer.valueOf(5).equals(out.get(Enchantments.SMITE)), "an anvil puts Smite V on a mace: " + out);
        c.eq(5, event.getCost(), "costing 5 levels: Smite is uncommon, halved from a book");
        ItemStack sharp = ItemEnchantedBook.getEnchantedItemStack(new EnchantmentData(Enchantments.SHARPNESS, 1));
        AnvilUpdateEvent none = new AnvilUpdateEvent(mace, sharp, "", 0);
        MinecraftForge.EVENT_BUS.post(none);
        c.ok(none.getOutput().isEmpty(), "but not Sharpness");
        ItemStack dense = mace.copy();
        dense.addEnchantment(MaceContent.density, 2);
        ItemStack dense2 = ItemEnchantedBook.getEnchantedItemStack(new EnchantmentData(MaceContent.density, 2));
        AnvilUpdateEvent up = new AnvilUpdateEvent(dense, dense2, "", 0);
        MinecraftForge.EVENT_BUS.post(up);
        c.ok(Integer.valueOf(3).equals(EnchantmentHelper.getEnchantments(up.getOutput()).get(MaceContent.density)), "Density II and II make III");
    }

    /** Ready to swing at full strength: the attack timer as if it had been waiting. */
    private static void ready(EntityPlayerMP player, ItemStack held) {
        player.setHeldItem(EnumHand.MAIN_HAND, held);
        player.getAttributeMap().removeAttributeModifiers(held.getAttributeModifiers(EntityEquipmentSlot.MAINHAND));
        player.getAttributeMap().applyAttributeModifiers(held.getAttributeModifiers(EntityEquipmentSlot.MAINHAND));
        ReflectionHelper.setPrivateValue(EntityLivingBase.class, player, 100, "ticksSinceLastSwing");
        player.onGround = true;
        player.motionX = player.motionY = player.motionZ = 0;
    }

    private static EntityZombie dummy(Check c, BlockPos at, double health) {
        EntityZombie z = new EntityZombie(c.world);
        z.setPosition(at.getX() + 0.5, at.getY(), at.getZ() + 0.5);
        z.getEntityAttribute(SharedMonsterAttributes.MAX_HEALTH).setBaseValue(health);
        z.setHealth((float) health);
        z.getEntityAttribute(SharedMonsterAttributes.KNOCKBACK_RESISTANCE).setBaseValue(1.0);
        // A zombie's own 2 armor would blur the numbers.
        z.getEntityAttribute(SharedMonsterAttributes.ARMOR).setBaseValue(0.0);
        z.setNoAI(true);
        c.world.spawnEntity(z);
        return z;
    }

    private static float hit(EntityPlayerMP player, EntityZombie target, float fall) {
        float before = target.getHealth();
        target.hurtResistantTime = 0;
        player.fallDistance = fall;
        player.attackTargetEntityWithCurrentItem(target);
        return before - target.getHealth();
    }

    private void smash(Check c, EntityPlayerMP player, BlockPos area) {
        EntityZombie target = dummy(c, area.add(5, 0, 4), 1000);
        ItemStack mace = new ItemStack(MaceContent.mace);
        ready(player, mace);
        c.ok(Math.abs(hit(player, target, 0.0F) - 6.0F) < 0.01F, "a plain hit does 6");
        ready(player, mace);
        c.ok(Math.abs(hit(player, target, 1.4F) - 6.0F) < 0.01F, "no smash under 1.5 blocks");

        EntityPig bystander = new EntityPig(c.world);
        bystander.setPosition(target.posX + 2, target.posY, target.posZ);
        c.world.spawnEntity(bystander);
        bystander.motionX = bystander.motionY = bystander.motionZ = 0;
        ready(player, mace);
        float dealt = hit(player, target, 4.0F);
        c.ok(Math.abs(dealt - 20.0F) < 0.01F, "a 4-block smash: 6 + 12 + 2 = 20, got " + dealt);
        c.eq(0.0F, player.fallDistance, "the smash ends the fall");
        c.ok(Math.abs(player.motionY - 0.01) < 1e-6, "and stops the wielder: " + player.motionY);
        c.ok(WindBurst.launched(player), "the fall after it counts only below this height");
        c.ok(bystander.motionY > 0.6 && bystander.motionX > 0.1, "a bystander 2 blocks away is knocked back: "
                + bystander.motionX + "," + bystander.motionY);
        c.ok(mace.getItemDamage() >= 3, "each hit wears the mace: " + mace.getItemDamage());

        ready(player, mace);
        c.ok(Math.abs(hit(player, target, 10.0F) - (6.0F + 24.0F)) < 0.01F, "10 blocks: 6 + 12 + 10 + 2 = 30");

        ItemStack dense = new ItemStack(MaceContent.mace);
        dense.addEnchantment(MaceContent.density, 3);
        ready(player, dense);
        c.ok(Math.abs(hit(player, target, 4.0F) - 26.0F) < 0.01F, "Density III adds 0.5 x 3 x 4 = 6");

        ItemStack burst = new ItemStack(MaceContent.mace);
        burst.addEnchantment(MaceContent.windBurst, 1);
        ready(player, burst);
        hit(player, target, 4.0F);
        c.ok(player.motionY > 0.5, "Wind Burst launches the wielder: " + player.motionY);
        ready(player, burst);
        hit(player, target, 0.5F);
        c.ok(player.motionY < 0.1, "no Wind Burst without a smash");
        bystander.setDead();
        target.setDead();
    }

    private void breach(Check c, EntityPlayerMP player, BlockPos area) {
        EntityZombie armored = dummy(c, area.add(5, 0, 8), 1000);
        armored.setItemStackToSlot(EntityEquipmentSlot.HEAD, new ItemStack(Items.DIAMOND_HELMET));
        armored.setItemStackToSlot(EntityEquipmentSlot.CHEST, new ItemStack(Items.DIAMOND_CHESTPLATE));
        armored.setItemStackToSlot(EntityEquipmentSlot.LEGS, new ItemStack(Items.DIAMOND_LEGGINGS));
        armored.setItemStackToSlot(EntityEquipmentSlot.FEET, new ItemStack(Items.DIAMOND_BOOTS));
        armored.onUpdate();
        float armor = armored.getTotalArmorValue();
        float toughness = (float) armored.getEntityAttribute(SharedMonsterAttributes.ARMOR_TOUGHNESS).getAttributeValue();
        c.eq(20.0F, armor, "full diamond: 20 armor");
        ready(player, new ItemStack(MaceContent.mace));
        float plain = hit(player, armored, 0.0F);
        c.ok(Math.abs(plain - CombatRules.getDamageAfterAbsorb(6.0F, armor, toughness)) < 0.01F, "plain hit through armor: " + plain);
        ItemStack breach = new ItemStack(MaceContent.mace);
        breach.addEnchantment(MaceContent.breach, 4);
        ready(player, breach);
        float breached = hit(player, armored, 0.0F);
        float want = 6.0F - (6.0F - CombatRules.getDamageAfterAbsorb(6.0F, armor, toughness)) * 0.4F;
        c.ok(Math.abs(breached - want) < 0.02F, "Breach IV leaves armor 40% of its strength: " + breached + " want " + want);
        armored.setDead();
    }

    private void overkill(Check c, EntityPlayerMP player, BlockPos area) {
        Advancement adv = c.world.getMinecraftServer().getAdvancementManager().getAdvancement(Content.id("adventure/overoverkill"));
        c.ok(adv != null, "Over-Overkill loaded");
        EntityZombie target = dummy(c, area.add(9, 0, 4), 1000);
        ItemStack mace = new ItemStack(MaceContent.mace);
        ready(player, mace);
        hit(player, target, 40.0F);
        c.ok(!player.getAdvancements().getProgress(adv).isDone(), "60 damage is not enough");
        ItemStack dense = new ItemStack(MaceContent.mace);
        dense.addEnchantment(MaceContent.density, 5);
        ready(player, dense);
        float dealt = hit(player, target, 40.0F);
        c.ok(dealt >= 100.0F, "Density V from 40 blocks: " + dealt);
        c.ok(player.getAdvancements().getProgress(adv).isDone(), "Over-Overkill earned");
        target.setDead();
    }
}
