package com.exiledradio.trickytrialsbackport.selftest;

import com.exiledradio.backportcore.ownership.ItemOwnership;
import com.exiledradio.trickytrialsbackport.common.Content;
import com.exiledradio.trickytrialsbackport.trial.ItemOminousBottle;
import com.exiledradio.trickytrialsbackport.trial.Omens;
import com.exiledradio.trickytrialsbackport.trial.TrialContent;

import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityList;
import net.minecraft.entity.EntityLiving;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.inventory.EntityEquipmentSlot;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.potion.Potion;
import net.minecraft.potion.PotionEffect;
import net.minecraft.util.DamageSource;
import net.minecraft.util.NonNullList;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.RegistryEvent;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.fml.common.Loader;
import net.minecraftforge.fml.common.registry.ForgeRegistries;

import java.util.Collections;
import java.util.List;

/**
 * {@code -Pselftest=ominous}: the ominous bottle, ours since 1.0.2 (1.21 added it), with or without Village &amp;
 * Pillage. Drinking gives Bad Omen at the bottle's level (Village &amp; Pillage's when it is here). With Village &amp;
 * Pillage (its dev jar in {@code eclipse/mods}): its pillager captains drop our bottle and give no Bad Omen, and its
 * older worlds' {@code villagerbackport:ominous_bottle} becomes ours.
 */
public final class OminousBottleSelfTest implements SelfTest {

    private static final String VP = "villagerbackport";

    @Override
    public String name() {
        return "ominous";
    }

    @Override
    public void run(Check c) throws Exception {
        boolean vp = Loader.isModLoaded(VP);
        Item bottle = TrialContent.ominousBottle;
        c.ok(bottle != null && Content.id("ominous_bottle").equals(bottle.getRegistryName()), "our ominous bottle is registered"
                + (vp ? " with Village & Pillage" : " alone"));
        c.ok(bottle.getCreativeTab() != null, "it has a creative tab");
        NonNullList<ItemStack> sub = NonNullList.create();
        bottle.getSubItems(CreativeTabs.SEARCH, sub);
        c.eq(ItemOminousBottle.LEVELS, sub.size(), "levels I-V in the creative inventory");
        Potion bad = Omens.badOmen();
        c.ok(bad != null && new ResourceLocation(vp ? VP : Content.MOD_ID, "bad_omen").equals(bad.getRegistryName()),
                "Bad Omen is " + (vp ? "Village & Pillage's" : "our copy"));
        boolean core145 = "trickytrialsbackport".equals(ItemOwnership.itemOwner("ominous_bottle"));
        if (core145) {
            // Backport Core 1.4.5 and later.
            c.ok(ItemOwnership.itemProvided("ominous_bottle"), "Core: the bottle's owner, us, provides it");
            c.ok(ItemOwnership.item("ominous_bottle", VP) == bottle, "Core's lookup finds our bottle");
        }

        drink(c, bottle, bad);
        if (vp) {
            captains(c, bottle);
        }
        if (vp || core145) {
            // Village & Pillage renames its old bottles; Core 1.4.5 does too, so a world keeps them without it.
            remap(c, bottle);
        }
    }

    private void drink(Check c, Item bottle, Potion bad) {
        EntityPlayerMP player = Players.real(c.world, "omenDrinker");
        ItemStack two = new ItemStack(bottle, 2, 2);
        bottle.onItemUseFinish(two, c.world, player);
        PotionEffect effect = player.getActivePotionEffect(bad);
        c.ok(effect != null && effect.getAmplifier() == 2 && effect.getDuration() == 120000, "bottle III: Bad Omen III for 100 minutes");
        c.eq(1, two.getCount(), "one bottle drunk");
        bottle.onItemUseFinish(new ItemStack(bottle, 1, 0), c.world, player);
        effect = player.getActivePotionEffect(bad);
        c.ok(effect != null && effect.getAmplifier() == 2, "a lower bottle leaves Bad Omen III");
        bottle.onItemUseFinish(new ItemStack(bottle, 1, 4), c.world, player);
        effect = player.getActivePotionEffect(bad);
        c.ok(effect != null && effect.getAmplifier() == 4, "a higher bottle raises it to V");
        c.eq(4, ItemOminousBottle.level(new ItemStack(bottle, 1, 9)), "levels past V read as V");
        player.clearActivePotions();
    }

    /** Village &amp; Pillage's captains, from outside its code: a pillager wearing its ominous banner. */
    private void captains(Check c, Item bottle) throws Exception {
        ItemStack banner = (ItemStack) Class.forName("com.exiledradio.villagerbackport.raid.OminousBanner").getMethod("create").invoke(null);
        BlockPos at = c.world.getSpawnPoint().up(120);
        EntityPlayerMP player = Players.real(c.world, "captainKiller");
        player.setPosition(at.getX() + 0.5, at.getY(), at.getZ() + 0.5);
        player.clearActivePotions();
        int before = bottles(c, at).size();
        Entity mob = EntityList.createEntityByIDFromName(new ResourceLocation(VP, "pillager"), c.world);
        c.ok(mob instanceof EntityLiving, "Village & Pillage's pillager");
        if (!(mob instanceof EntityLiving)) {
            return;
        }
        EntityLiving pillager = (EntityLiving) mob;
        pillager.setPosition(at.getX() + 0.5, at.getY(), at.getZ() + 0.5);
        pillager.setItemStackToSlot(EntityEquipmentSlot.HEAD, banner);
        MinecraftForge.EVENT_BUS.post(new LivingDeathEvent(pillager, DamageSource.causePlayerDamage(player)));
        List<EntityItem> drops = bottles(c, at);
        c.eq(before + 1, drops.size(), "a pillager captain drops one bottle");
        ItemStack drop = drops.isEmpty() ? ItemStack.EMPTY : drops.get(drops.size() - 1).getItem();
        c.ok(drop.getItem() == bottle && drop.getMetadata() < ItemOminousBottle.LEVELS, "ours, level I-V (" + drop.getMetadata() + ")");
        c.ok(player.getActivePotionEffect(Omens.badOmen()) == null, "and gives no Bad Omen (1.21)");
        for (EntityItem item : drops) {
            item.setDead();
        }
        Item theirs = ForgeRegistries.ITEMS.getValue(new ResourceLocation(VP, "ominous_bottle"));
        c.ok(theirs == null || !new ResourceLocation(VP, "ominous_bottle").equals(theirs.getRegistryName()),
                "no villagerbackport:ominous_bottle with us here");
    }

    /** An older world's Village &amp; Pillage bottle, missing as it loads. */
    private void remap(Check c, Item bottle) {
        RegistryEvent.MissingMappings.Mapping<Item> mapping = new RegistryEvent.MissingMappings.Mapping<Item>(
                ForgeRegistries.ITEMS, ForgeRegistries.ITEMS, new ResourceLocation(VP, "ominous_bottle"), 4000);
        MinecraftForge.EVENT_BUS.post(new RegistryEvent.MissingMappings<Item>(new ResourceLocation("minecraft", "items"),
                ForgeRegistries.ITEMS, Collections.singletonList(mapping)));
        c.ok(mapping.getAction() == RegistryEvent.MissingMappings.Action.REMAP && mapping.getTarget() == bottle,
                "villagerbackport:ominous_bottle from an older world becomes ours"
                        + (Loader.isModLoaded(VP) ? "" : " (Core, Village & Pillage removed)"));
    }

    private static List<EntityItem> bottles(Check c, BlockPos at) {
        return c.world.getEntitiesWithinAABB(EntityItem.class, new AxisAlignedBB(at).grow(4.0),
                e -> e != null && e.getItem().getItem() == TrialContent.ominousBottle);
    }
}
