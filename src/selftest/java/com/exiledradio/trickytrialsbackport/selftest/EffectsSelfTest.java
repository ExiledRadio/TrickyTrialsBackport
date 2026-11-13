package com.exiledradio.trickytrialsbackport.selftest;

import com.exiledradio.trickytrialsbackport.common.Content;
import com.exiledradio.trickytrialsbackport.common.Owners;
import com.exiledradio.trickytrialsbackport.effects.EffectPotion;
import com.exiledradio.trickytrialsbackport.effects.EffectsContent;
import com.exiledradio.trickytrialsbackport.effects.EffectsTestAccess;

import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityAreaEffectCloud;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.MoverType;
import net.minecraft.entity.monster.EntityMagmaCube;
import net.minecraft.entity.monster.EntitySilverfish;
import net.minecraft.entity.monster.EntitySlime;
import net.minecraft.entity.monster.EntityZombie;
import net.minecraft.entity.passive.EntityPig;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.entity.projectile.EntityTippedArrow;
import net.minecraft.init.Blocks;
import net.minecraft.init.Items;
import net.minecraft.init.MobEffects;
import net.minecraft.init.PotionTypes;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.potion.Potion;
import net.minecraft.potion.PotionEffect;
import net.minecraft.potion.PotionHelper;
import net.minecraft.potion.PotionType;
import net.minecraft.potion.PotionUtils;
import net.minecraft.util.DamageSource;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.entity.living.LivingEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import net.minecraftforge.fml.relauncher.ReflectionHelper;
import net.minecraftforge.fml.relauncher.Side;

import java.io.InputStream;
import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.Scanner;
import java.util.Set;

/** Stream 8: Weaving, Oozing, Wind Charged, Infested, their potions, arrows and brewing. */
public final class EffectsSelfTest implements SelfTest {

    @Override
    public String name() {
        return "effects";
    }

    @Override
    public void run(Check c) {
        registration(c);
        assets(c);
        brewing(c);
        forms(c);
        immunity(c);
        weaving(c);
        oozing(c);
        windCharged(c);
        infested(c);
        webs(c);
        particles(c);
    }

    // ------------------------------------------------------------------------------------------

    private void registration(Check c) {
        Object[][] expect = {
                {"wind_charged", EffectsContent.windCharged, EffectsContent.windChargedPotion, 12438015},
                {"weaving", EffectsContent.weaving, EffectsContent.weavingPotion, 7891290},
                {"oozing", EffectsContent.oozing, EffectsContent.oozingPotion, 10092451},
                {"infested", EffectsContent.infested, EffectsContent.infestedPotion, 9214860},
        };
        for (Object[] row : expect) {
            String name = (String) row[0];
            Potion effect = (Potion) row[1];
            PotionType type = (PotionType) row[2];
            c.eq(Content.id(name), effect.getRegistryName(), name + " effect id");
            c.eq(effect, Potion.getPotionFromResourceLocation(Content.id(name).toString()), name + " effect looked up");
            c.eq(effect, Owners.effect("minecraft", name), name + " through Owners");
            c.ok(effect.isBadEffect(), name + " is harmful");
            c.eq(row[3], effect.getLiquidColor(), name + " color");
            c.eq("effect.trickytrialsbackport." + name, effect.getName(), name + " name key");
            c.eq(Content.id(name), type.getRegistryName(), name + " potion id");
            c.eq(type, PotionType.getPotionTypeForName(Content.id(name).toString()), name + " potion looked up");
            c.eq(1, type.getEffects().size(), name + " potion has one effect");
            c.eq(effect, type.getEffects().get(0).getPotion(), name + " potion's effect");
            c.eq(3600, type.getEffects().get(0).getDuration(), name + " potion 3:00");
            c.eq(0, type.getEffects().get(0).getAmplifier(), name + " potion level I");
            c.eq("potion.effect.trickytrialsbackport." + name, type.getNamePrefixed("potion.effect."), name + " potion name key");
        }
    }

    private void assets(Check c) {
        String lang = resource("/assets/trickytrialsbackport/lang/en_us.lang");
        c.ok(lang != null, "en_us.lang on the classpath");
        String[][] names = {
                {"effect.trickytrialsbackport.weaving", "Weaving"},
                {"effect.trickytrialsbackport.wind_charged", "Wind Charged"},
                {"potion.effect.trickytrialsbackport.infested", "Potion of Infestation"},
                {"splash_potion.effect.trickytrialsbackport.oozing", "Splash Potion of Oozing"},
                {"lingering_potion.effect.trickytrialsbackport.wind_charged", "Lingering Potion of Wind Charging"},
                {"tipped_arrow.effect.trickytrialsbackport.weaving", "Arrow of Weaving"},
        };
        for (String[] kv : names) {
            c.ok(lang != null && lang.contains("\n" + kv[0] + "=" + kv[1] + "\n"), "lang " + kv[0] + "=" + kv[1]);
        }
        for (String kind : new String[]{"potion", "splash_potion", "lingering_potion", "tipped_arrow"}) {
            for (EffectPotion p : list()) {
                String key = kind + ".effect.trickytrialsbackport." + p.shortName() + "=";
                c.ok(lang != null && lang.contains("\n" + key), "lang has " + key);
            }
        }
        for (EffectPotion p : list()) {
            c.ok(getClass().getResource("/assets/trickytrialsbackport/textures/mob_effect/" + p.shortName() + ".png") != null,
                    p.shortName() + " icon");
        }
        c.ok(getClass().getResource("/assets/trickytrialsbackport/textures/particle/infested.png") != null, "infested particle texture");
        for (int i = 0; i < 7; i++) {
            c.ok(getClass().getResource("/assets/trickytrialsbackport/textures/particle/small_gust_" + i + ".png") != null,
                    "small gust frame " + i);
        }
        String sounds = resource("/assets/trickytrialsbackport/sounds.json");
        c.ok(sounds != null && sounds.contains("\"block.cobweb.place\"") && sounds.contains("\"entity.breeze.wind_burst\""),
                "cobweb place and breeze burst sounds");
        c.ok(getClass().getResource("/assets/trickytrialsbackport/sounds/block/cobweb/break1.ogg") != null, "cobweb sound file");
    }

    private void brewing(Check c) {
        Item rod = Owners.ours("breeze_rod");
        c.ok(rod != null, "breeze rod present (breeze stream on)");
        Object[][] mixes = {
                {new ItemStack(rod), EffectsContent.windChargedPotion},
                {new ItemStack(Blocks.SLIME_BLOCK), EffectsContent.oozingPotion},
                {new ItemStack(Blocks.STONE), EffectsContent.infestedPotion},
                {new ItemStack(Blocks.WEB), EffectsContent.weavingPotion},
        };
        for (Object[] mix : mixes) {
            ItemStack ingredient = (ItemStack) mix[0];
            PotionType out = (PotionType) mix[1];
            String what = ingredient.getItem().getRegistryName().toString();
            c.ok(PotionHelper.isReagent(ingredient), what + " is a brewing ingredient");
            for (Item bottle : new Item[]{Items.POTIONITEM, Items.SPLASH_POTION, Items.LINGERING_POTION}) {
                ItemStack awkward = PotionUtils.addPotionToItemStack(new ItemStack(bottle), PotionTypes.AWKWARD);
                ItemStack result = PotionHelper.doReaction(ingredient, awkward);
                c.eq(out, PotionUtils.getPotionFromItem(result), what + " + awkward " + bottle.getRegistryName().getPath());
                c.eq(bottle, result.getItem(), what + " keeps the bottle " + bottle.getRegistryName().getPath());
            }
            ItemStack water = PotionUtils.addPotionToItemStack(new ItemStack(Items.POTIONITEM), PotionTypes.WATER);
            c.eq(PotionTypes.MUNDANE, PotionUtils.getPotionFromItem(PotionHelper.doReaction(ingredient, water)), what + " + water = mundane");
            ItemStack thick = PotionUtils.addPotionToItemStack(new ItemStack(Items.POTIONITEM), PotionTypes.THICK);
            c.eq(PotionTypes.THICK, PotionUtils.getPotionFromItem(PotionHelper.doReaction(ingredient, thick)), what + " does nothing to thick");
            // No long or strong forms in 1.21.
            ItemStack ours = PotionUtils.addPotionToItemStack(new ItemStack(Items.POTIONITEM), out);
            c.eq(out, PotionUtils.getPotionFromItem(PotionHelper.doReaction(new ItemStack(Items.REDSTONE), ours)), what + " potion: no redstone form");
            c.eq(out, PotionUtils.getPotionFromItem(PotionHelper.doReaction(new ItemStack(Items.GLOWSTONE_DUST), ours)), what + " potion: no glowstone form");
            ItemStack splash = PotionHelper.doReaction(new ItemStack(Items.GUNPOWDER), ours);
            c.ok(splash.getItem() == Items.SPLASH_POTION && PotionUtils.getPotionFromItem(splash) == out, what + " potion + gunpowder = splash");
            ItemStack lingering = PotionHelper.doReaction(new ItemStack(Items.DRAGON_BREATH), splash);
            c.ok(lingering.getItem() == Items.LINGERING_POTION && PotionUtils.getPotionFromItem(lingering) == out, what + " splash + breath = lingering");
        }
        ItemStack awkward = PotionUtils.addPotionToItemStack(new ItemStack(Items.POTIONITEM), PotionTypes.AWKWARD);
        c.eq(PotionTypes.AWKWARD, PotionUtils.getPotionFromItem(PotionHelper.doReaction(new ItemStack(Blocks.STONE, 1, 1), awkward)),
                "granite is not stone");
        c.eq(PotionTypes.AWKWARD, PotionUtils.getPotionFromItem(PotionHelper.doReaction(new ItemStack(Blocks.COBBLESTONE), awkward)),
                "cobblestone is not stone");
        c.ok(!PotionHelper.isReagent(new ItemStack(Blocks.STONE, 1, 3)), "diorite is no ingredient");
    }

    /** Tipped arrows give an eighth (0:22), lingering clouds a quarter (0:45), as 1.21. */
    private void forms(Check c) {
        BlockPos area = c.area();
        EntityZombie target = zombie(c, area.getX() + 8.5, area.getY(), area.getZ() + 8.5);
        try {
            ItemStack arrowStack = PotionUtils.addPotionToItemStack(new ItemStack(Items.TIPPED_ARROW), EffectsContent.weavingPotion);
            EntityTippedArrow arrow = new EntityTippedArrow(c.world, target.posX, target.posY + 3, target.posZ);
            arrow.setPotionEffect(arrowStack);
            Method hit = ReflectionHelper.findMethod(EntityTippedArrow.class, "arrowHit", "func_184548_a", EntityLivingBase.class);
            hit.invoke(arrow, target);
            PotionEffect fromArrow = target.getActivePotionEffect(EffectsContent.weaving);
            c.ok(fromArrow != null && fromArrow.getDuration() == 450, "arrow of weaving: 450 ticks, got " + (fromArrow == null ? null : fromArrow.getDuration()));
            target.clearActivePotions();

            EntityAreaEffectCloud cloud = new EntityAreaEffectCloud(c.world, target.posX, target.posY, target.posZ);
            cloud.setRadius(3.0F);
            cloud.setRadiusOnUse(-0.5F);
            cloud.setWaitTime(10);
            cloud.setRadiusPerTick(-cloud.getRadius() / cloud.getDuration());
            cloud.setPotion(EffectsContent.oozingPotion);
            c.world.spawnEntity(cloud);
            // 1.12.2 counts ticksExisted in the world's entity loop, not in onUpdate.
            for (int i = 1; i <= 20 && target.getActivePotionEffect(EffectsContent.oozing) == null; i++) {
                cloud.ticksExisted = i;
                cloud.onUpdate();
            }
            PotionEffect fromCloud = target.getActivePotionEffect(EffectsContent.oozing);
            c.ok(fromCloud != null && fromCloud.getDuration() == 900, "lingering oozing: 900 ticks, got " + (fromCloud == null ? null : fromCloud.getDuration()));
            cloud.setDead();
        } catch (ReflectiveOperationException e) {
            c.fail("arrowHit: " + e);
        } finally {
            target.setDead();
        }
    }

    private void immunity(Check c) {
        EntitySilverfish fish = new EntitySilverfish(c.world);
        c.ok(!fish.isPotionApplicable(new PotionEffect(EffectsContent.infested, 100)), "silverfish cannot be Infested");
        c.ok(fish.isPotionApplicable(new PotionEffect(EffectsContent.oozing, 100)), "silverfish can be Oozing");
        EntitySlime slime = new EntitySlime(c.world);
        c.ok(!slime.isPotionApplicable(new PotionEffect(EffectsContent.oozing, 100)), "slime cannot be Oozing");
        c.ok(slime.isPotionApplicable(new PotionEffect(EffectsContent.infested, 100)), "slime can be Infested");
        EntityMagmaCube cube = new EntityMagmaCube(c.world);
        c.ok(cube.isPotionApplicable(new PotionEffect(EffectsContent.oozing, 100)), "magma cube can be Oozing");
        EntityZombie zombie = new EntityZombie(c.world);
        for (EffectPotion p : list()) {
            c.ok(zombie.isPotionApplicable(new PotionEffect(p, 100)), "zombie takes " + p.shortName());
        }
        slime.addPotionEffect(new PotionEffect(EffectsContent.oozing, 100));
        c.ok(!slime.isPotionActive(EffectsContent.oozing), "Oozing does not stick to a slime");
    }

    // ------------------------------------------------------------------------------------------

    private void weaving(Check c) {
        // Unit: the cube rule, with a seeded random, against an independent count.
        BlockPos area = c.area();
        BlockPos center = area.add(8, 0, 8);
        Set<BlockPos> webs = EffectsTestAccess.weave(c.world, new Random(7), center);
        c.ok(webs.size() == 2 || webs.size() == 3, "2-3 cobwebs, got " + webs.size());
        boolean allGood = true;
        for (BlockPos p : webs) {
            allGood &= Math.abs(p.getX() - center.getX()) <= 1 && Math.abs(p.getY() - center.getY()) <= 1
                    && Math.abs(p.getZ() - center.getZ()) <= 1 && c.world.getBlockState(p).getBlock() == Blocks.WEB
                    && c.world.getBlockState(p.down()).getBlock() == Blocks.STONE;
        }
        c.ok(allGood, "each cobweb within one block, placed, on the stone floor " + webs);
        c.eq(new Random(7).nextInt(2) + 2 >= webs.size(), true, "no more than the drawn count");

        // Only spots over a sturdy top count: over slabs' bottom halves nothing.
        BlockPos high = area.add(20, 10, 20);
        c.fill(high.add(-1, -2, -1), high.add(1, -2, 1), Blocks.STONE_SLAB.getDefaultState());
        c.eq(0, EffectsTestAccess.weave(c.world, new Random(3), high).size(), "no cobwebs over bottom slabs");

        // On death: a zombie with Weaving, mobGriefing on and off.
        BlockPos at = c.area().add(8, 0, 8);
        boolean griefing = c.world.getGameRules().getBoolean("mobGriefing");
        try {
            c.world.getGameRules().setOrCreateGameRule("mobGriefing", "false");
            EntityZombie quiet = zombie(c, at.getX() + 0.5, at.getY(), at.getZ() + 0.5);
            quiet.addPotionEffect(new PotionEffect(EffectsContent.weaving, 3600));
            die(quiet);
            c.eq(0, countWebs(c, at), "no mob cobwebs without mobGriefing");
            c.world.getGameRules().setOrCreateGameRule("mobGriefing", "true");
            EntityZombie weaver = zombie(c, at.getX() + 0.5, at.getY(), at.getZ() + 0.5);
            weaver.addPotionEffect(new PotionEffect(EffectsContent.weaving, 3600));
            weaver.setHealth(0.0F);
            weaver.deathTime = 18;
            weaver.onUpdate();
            c.eq(0, countWebs(c, at), "nothing before the 20th death tick");
            weaver.onUpdate();
            int n = countWebs(c, at);
            c.ok(n == 2 || n == 3, "2-3 cobwebs when the dead zombie is removed, got " + n);
            c.ok(weaver.isDead, "zombie removed that tick");
            c.ok(!weaver.isPotionActive(EffectsContent.weaving), "effect gone after triggering");
        } finally {
            c.world.getGameRules().setOrCreateGameRule("mobGriefing", String.valueOf(griefing));
        }

        // Players weave even without mobGriefing.
        BlockPos pat = c.area().add(8, 0, 8);
        EntityPlayerMP player = Players.real(c.world, "weaver");
        player.setPosition(pat.getX() + 0.5, pat.getY(), pat.getZ() + 0.5);
        c.world.spawnEntity(player);
        try {
            c.world.getGameRules().setOrCreateGameRule("mobGriefing", "false");
            player.addPotionEffect(new PotionEffect(EffectsContent.weaving, 3600));
            player.setHealth(0.0F);
            player.deathTime = 19;
            MinecraftForge.EVENT_BUS.post(new LivingEvent.LivingUpdateEvent(player));
            int n = countWebs(c, pat);
            c.ok(n == 2 || n == 3, "a dying player weaves without mobGriefing, got " + n);
        } finally {
            c.world.getGameRules().setOrCreateGameRule("mobGriefing", String.valueOf(griefing));
            c.world.removeEntityDangerously(player);
        }
    }

    private int countWebs(Check c, BlockPos at) {
        int n = 0;
        for (BlockPos p : BlockPos.getAllInBox(at.add(-1, -1, -1), at.add(1, 1, 1))) {
            if (c.world.getBlockState(p).getBlock() == Blocks.WEB) {
                n++;
            }
        }
        return n;
    }

    private void oozing(Check c) {
        c.eq(2, EffectsTestAccess.slimesToSpawn(24, 0, 2), "cramming 24, no slimes near: 2");
        c.eq(1, EffectsTestAccess.slimesToSpawn(24, 23, 2), "23 near: 1");
        c.eq(0, EffectsTestAccess.slimesToSpawn(24, 30, 2), "30 near: 0");
        c.eq(2, EffectsTestAccess.slimesToSpawn(0, 99, 2), "no cramming limit: 2");

        BlockPos at = c.area().add(8, 0, 8);
        EntityZombie zombie = zombie(c, at.getX() + 0.5, at.getY(), at.getZ() + 0.5);
        zombie.addPotionEffect(new PotionEffect(EffectsContent.oozing, 3600));
        die(zombie);
        List<EntitySlime> slimes = slimes(c, at);
        c.eq(2, slimes.size(), "two slimes from an Oozing zombie");
        boolean sized = true;
        for (EntitySlime s : slimes) {
            sized &= s.getSlimeSize() == 2 && Math.abs(s.posY - (at.getY() + 0.5)) < 0.01
                    && Math.abs(s.getHealth() - 4.0F) < 0.01F;
        }
        c.ok(sized, "size 2, 4 health, half a block up");
        clear(slimes);

        // Cramming: with maxEntityCramming 3 and two slimes within 2 blocks, one more.
        int cramming = c.world.getGameRules().getInt("maxEntityCramming");
        BlockPos at2 = c.area().add(8, 0, 8);
        List<Entity> extra = new ArrayList<Entity>();
        try {
            c.world.getGameRules().setOrCreateGameRule("maxEntityCramming", "3");
            for (int i = 0; i < 2; i++) {
                EntitySlime s = new EntitySlime(c.world);
                s.setPosition(at2.getX() + 1.5, at2.getY(), at2.getZ() + 0.5 + i);
                c.world.spawnEntity(s);
                extra.add(s);
            }
            EntityMagmaCube cube = new EntityMagmaCube(c.world);
            cube.setPosition(at2.getX() - 0.5, at2.getY(), at2.getZ() + 0.5);
            c.world.spawnEntity(cube);
            extra.add(cube);
            EntityZombie z2 = zombie(c, at2.getX() + 0.5, at2.getY(), at2.getZ() + 0.5);
            z2.addPotionEffect(new PotionEffect(EffectsContent.oozing, 3600));
            die(z2);
            c.eq(3, slimes(c, at2).size(), "one more slime under a cramming limit of 3 (magma cubes don't count)");
            clear(slimes(c, at2));
        } finally {
            c.world.getGameRules().setOrCreateGameRule("maxEntityCramming", String.valueOf(cramming));
            for (Entity e : extra) {
                e.setDead();
            }
        }
    }

    private List<EntitySlime> slimes(Check c, BlockPos at) {
        List<EntitySlime> out = new ArrayList<EntitySlime>();
        for (EntitySlime s : c.world.getEntitiesWithinAABB(EntitySlime.class, new AxisAlignedBB(at).grow(4))) {
            if (!s.isDead && s.getClass() == EntitySlime.class) {
                out.add(s);
            }
        }
        return out;
    }

    private void windCharged(Check c) {
        BlockPos at = c.area().add(8, 0, 8);
        EntityZombie zombie = zombie(c, at.getX() + 0.5, at.getY(), at.getZ() + 0.5);
        EntityPig pig = new EntityPig(c.world);
        pig.setPosition(at.getX() + 2.5, at.getY(), at.getZ() + 0.5);
        c.world.spawnEntity(pig);
        try {
            zombie.addPotionEffect(new PotionEffect(EffectsContent.windCharged, 3600));
            zombie.motionX = 0;
            zombie.motionY = 0;
            zombie.motionZ = 0;
            pig.motionX = 0;
            pig.motionY = 0;
            pig.motionZ = 0;
            die(zombie);
            c.ok(pig.motionX > 0.1, "a pig two blocks east is blown east, motionX " + pig.motionX);
            c.eq(0.0, zombie.motionX, "the dying zombie is not pushed");

            // A plain death pushes nothing.
            EntityZombie plain = zombie(c, at.getX() + 0.5, at.getY(), at.getZ() + 0.5);
            pig.motionX = 0;
            die(plain);
            c.eq(0.0, pig.motionX, "no burst without the effect");
        } finally {
            pig.setDead();
        }
    }

    private void infested(Check c) {
        BlockPos at = c.area().add(8, 0, 8);
        // A random whose floats are always 0.05 (under the 10% chance) and whose ints are always 0 (one fish).
        Random low = new Random() {
            @Override
            public float nextFloat() {
                return 0.05F;
            }

            @Override
            public int nextInt(int bound) {
                return 0;
            }
        };
        Random high = new Random() {
            @Override
            public float nextFloat() {
                return 0.5F;
            }
        };
        EntityZombie zombie = zombie(c, at.getX() + 0.5, at.getY(), at.getZ() + 0.5);
        zombie.setHealth(zombie.getMaxHealth());
        zombie.rotationYaw = 0.0F;
        zombie.rotationPitch = 0.0F;
        zombie.rotationYawHead = 0.0F;
        zombie.addPotionEffect(new PotionEffect(EffectsContent.infested, 3600));
        try {
            ReflectionHelper.setPrivateValue(Entity.class, zombie, low, "rand", "field_70146_Z");
            zombie.attackEntityFrom(DamageSource.GENERIC, 1.0F);
            List<EntitySilverfish> fish = fish(c, at);
            c.eq(1, fish.size(), "a hit at a 5% roll releases one silverfish");
            if (fish.size() == 1) {
                EntitySilverfish f = fish.get(0);
                c.ok(Math.abs(f.posY - (zombie.posY + zombie.height / 2.0)) < 0.01, "from the middle of the body");
                // look (0, 0, 1) x 0.3, turned by 0.05 x pi - pi/2 about Y
                double angle = 0.05 * Math.PI - Math.PI / 2;
                double ex = 0.3 * Math.sin(angle);
                double ez = 0.3 * Math.cos(angle);
                c.ok(Math.abs(f.motionX - ex) < 1e-3 && Math.abs(f.motionZ - ez) < 1e-3,
                        "thrown along the look turned by the roll: expected " + ex + "," + ez + " got " + f.motionX + "," + f.motionZ);
            }
            clear(fish);
            zombie.hurtResistantTime = 0;
            ReflectionHelper.setPrivateValue(Entity.class, zombie, high, "rand", "field_70146_Z");
            zombie.attackEntityFrom(DamageSource.GENERIC, 1.0F);
            c.eq(0, fish(c, at).size(), "a hit at a 50% roll releases none");

            // Statistics with a seeded random: 10% of hits, 1-2 each.
            ReflectionHelper.setPrivateValue(Entity.class, zombie, new Random(42), "rand", "field_70146_Z");
            int hits = 0;
            int total = 0;
            boolean oneOrTwo = true;
            for (int i = 0; i < 2000; i++) {
                List<EntitySilverfish> out = EffectsTestAccess.infest(zombie);
                if (!out.isEmpty()) {
                    hits++;
                    total += out.size();
                    oneOrTwo &= out.size() <= 2;
                }
                clear(out);
            }
            c.ok(hits > 140 && hits < 260, "about 10% of 2000 hits release silverfish, got " + hits);
            c.ok(oneOrTwo && total > hits * 1.3 && total < hits * 1.7, "1-2 each, " + total + " from " + hits);

            // No damage, no silverfish (a hit fully blocked).
            ReflectionHelper.setPrivateValue(Entity.class, zombie, low, "rand", "field_70146_Z");
            zombie.hurtResistantTime = 0;
            net.minecraftforge.event.entity.living.LivingHurtEvent zero =
                    new net.minecraftforge.event.entity.living.LivingHurtEvent(zombie, DamageSource.GENERIC, 0.0F);
            MinecraftForge.EVENT_BUS.post(zero);
            c.eq(0, fish(c, at).size(), "a hit doing no damage releases none");

            EntitySilverfish immune = new EntitySilverfish(c.world);
            c.ok(!immune.isPotionApplicable(new PotionEffect(EffectsContent.infested, 10)), "silverfish don't spawn silverfish");
        } finally {
            zombie.setDead();
            clear(fish(c, at));
        }
    }

    private List<EntitySilverfish> fish(Check c, BlockPos at) {
        List<EntitySilverfish> out = new ArrayList<EntitySilverfish>();
        for (EntitySilverfish f : c.world.getEntitiesWithinAABB(EntitySilverfish.class, new AxisAlignedBB(at).grow(6))) {
            if (!f.isDead) {
                out.add(f);
            }
        }
        return out;
    }

    /** Weaving in a cobweb: half speed across, a quarter down, where 1.12.2 gives a quarter and a twentieth. */
    private void webs(Check c) {
        BlockPos at = c.area().add(8, 0, 8);
        c.fill(at, at.add(0, 3, 0), Blocks.WEB.getDefaultState());
        c.eq(0.2, webMove(c, at, true, 0.4, 0.0), "Weaving: 0.4 across moves 0.2");
        c.eq(0.1, webMove(c, at, false, 0.4, 0.0), "without: 0.1");
        c.eq(-0.1, webMove(c, at, true, 0.0, -0.4), "Weaving: 0.4 down falls 0.1");
        c.eq(-0.02, webMove(c, at, false, 0.0, -0.4), "without: 0.02");
    }

    private double webMove(Check c, BlockPos at, boolean weaving, double dx, double dy) {
        EntityZombie z = zombie(c, at.getX() + 0.3, at.getY() + 1.0, at.getZ() + 0.5);
        z.setNoAI(true);
        try {
            if (weaving) {
                z.addPotionEffect(new PotionEffect(EffectsContent.weaving, 3600));
            }
            EffectsTestAccess.setInWeb(z, true);
            MinecraftForge.EVENT_BUS.post(new LivingEvent.LivingUpdateEvent(z));
            double startX = z.posX;
            double startY = z.posY;
            z.prevPosX = z.posX;
            z.prevPosY = z.posY;
            z.prevPosZ = z.posZ;
            z.move(MoverType.SELF, dx, dy, 0.0);
            MinecraftForge.EVENT_BUS.post(new LivingEvent.LivingUpdateEvent(z));
            double moved = dx != 0.0 ? z.posX - startX : z.posY - startY;
            return Math.round(moved * 1e6) / 1e6;
        } finally {
            z.setDead();
        }
    }

    private void particles(Check c) {
        BlockPos at = c.area().add(8, 0, 8);
        EntityZombie z = zombie(c, at.getX() + 0.5, at.getY(), at.getZ() + 0.5);
        try {
            z.addPotionEffect(new PotionEffect(MobEffects.SPEED, 3600));
            z.addPotionEffect(new PotionEffect(EffectsContent.weaving, 3600));
            z.addPotionEffect(new PotionEffect(EffectsContent.infested, 3600, 0, true, true));
            c.eq(2 | 8 | 1 << 8, EffectsTestAccess.particleState(z), "weaving and infested visible, one other, not all ambient");
            int speedOnly = PotionUtils.getPotionColorFromEffectList(java.util.Collections.singletonList(new PotionEffect(MobEffects.SPEED, 3600)));
            c.eq(speedOnly, EffectsTestAccess.swirlColor(z), "the swirl is Speed's color alone");
            MinecraftForge.EVENT_BUS.post(new LivingEvent.LivingUpdateEvent(z));
            z.onUpdate();
            MinecraftForge.EVENT_BUS.post(new TickEvent.WorldTickEvent(Side.SERVER, TickEvent.Phase.END, c.world));
            c.eq(speedOnly, EffectsTestAccess.dataColor(z), "entity's swirl color set to Speed's");
            c.eq(2 | 8 | 1 << 8, EffectsTestAccess.sentState(z), "particle state sent");
            z.removePotionEffect(MobEffects.SPEED);
            MinecraftForge.EVENT_BUS.post(new TickEvent.WorldTickEvent(Side.SERVER, TickEvent.Phase.END, c.world));
            c.eq(0, EffectsTestAccess.dataColor(z), "no swirl with only our effects");
            z.removePotionEffect(EffectsContent.weaving);
            z.removePotionEffect(EffectsContent.infested);
            MinecraftForge.EVENT_BUS.post(new TickEvent.WorldTickEvent(Side.SERVER, TickEvent.Phase.END, c.world));
            c.eq(0, EffectsTestAccess.sentState(z), "cleared when the effects end");
            z.addPotionEffect(new PotionEffect(EffectsContent.oozing, 3600, 0, false, false));
            c.eq(0, EffectsTestAccess.particleState(z), "hidden particles stay hidden");
        } finally {
            z.setDead();
        }
    }

    // ------------------------------------------------------------------------------------------

    private static List<EffectPotion> list() {
        List<EffectPotion> out = new ArrayList<EffectPotion>();
        out.add(EffectsContent.windCharged);
        out.add(EffectsContent.weaving);
        out.add(EffectsContent.oozing);
        out.add(EffectsContent.infested);
        return out;
    }

    private static EntityZombie zombie(Check c, double x, double y, double z) {
        EntityZombie zombie = new EntityZombie(c.world);
        zombie.setPosition(x, y, z);
        c.world.spawnEntity(zombie);
        return zombie;
    }

    /** Kills an entity the way vanilla does, then ticks it to its removal (the 20th death tick). */
    private static void die(EntityLivingBase entity) {
        entity.setHealth(0.0F);
        entity.deathTime = 18;
        entity.onUpdate();
        entity.onUpdate();
    }

    private static void clear(List<? extends Entity> entities) {
        for (Entity e : entities) {
            e.setDead();
        }
    }

    private static String resource(String path) {
        InputStream in = EffectsSelfTest.class.getResourceAsStream(path);
        if (in == null) {
            return null;
        }
        Scanner s = new Scanner(in, StandardCharsets.UTF_8.name()).useDelimiter("\\A");
        return "\n" + (s.hasNext() ? s.next() : "") + "\n";
    }
}
