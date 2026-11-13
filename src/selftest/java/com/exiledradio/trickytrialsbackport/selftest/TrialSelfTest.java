package com.exiledradio.trickytrialsbackport.selftest;

import com.exiledradio.trickytrialsbackport.common.Content;
import com.exiledradio.trickytrialsbackport.trial.BlockTrialSpawner;
import com.exiledradio.trickytrialsbackport.trial.BlockVault;
import com.exiledradio.trickytrialsbackport.trial.EntityOminousItemSpawner;
import com.exiledradio.trickytrialsbackport.trial.ModernIds;
import com.exiledradio.trickytrialsbackport.trial.ModernLoot;
import com.exiledradio.trickytrialsbackport.trial.Omens;
import com.exiledradio.trickytrialsbackport.trial.SpawnerConfig;
import com.exiledradio.trickytrialsbackport.trial.TileEntityTrialSpawner;
import com.exiledradio.trickytrialsbackport.trial.TileEntityVault;
import com.exiledradio.trickytrialsbackport.trial.TrialContent;
import com.exiledradio.trickytrialsbackport.trial.TrialSpawnerPhase;
import com.exiledradio.trickytrialsbackport.trial.VaultPhase;

import net.minecraft.advancements.Advancement;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityList;
import net.minecraft.entity.EntityLiving;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.entity.monster.EntityZombie;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.entity.projectile.EntityTippedArrow;
import net.minecraft.init.Items;
import net.minecraft.inventory.EntityEquipmentSlot;
import net.minecraft.item.ItemMonsterPlacer;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.potion.PotionEffect;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.storage.WorldInfo;

import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.UUID;

/** Stream 7: trial spawners, vaults, keys, omens and ominous item spawners. */
public final class TrialSelfTest implements SelfTest {

    private static final String[] POOLS = {"breeze", "melee/husk", "melee/spider", "melee/zombie", "ranged/poison_skeleton", "ranged/skeleton",
            "ranged/stray", "slow_ranged/poison_skeleton", "slow_ranged/skeleton", "slow_ranged/stray", "small_melee/baby_zombie",
            "small_melee/cave_spider", "small_melee/silverfish", "small_melee/slime"};

    @Override
    public String name() {
        return "trials";
    }

    @Override
    public void run(Check c) {
        WorldInfo info = c.world.getWorldInfo();
        long savedTime = info.getWorldTotalTime();
        try {
            registration(c);
            blocks(c);
            configs(c);
            loot(c);
            BlockPos area = c.area();
            spawner(c, area);
            ominous(c, area);
            spawnEgg(c, area);
            vault(c, area);
            itemSpawner(c, area);
            bottle(c);
        } finally {
            info.setWorldTotalTime(savedTime);
        }
    }

    private static void time(Check c, long t) {
        c.world.getWorldInfo().setWorldTotalTime(t);
    }

    private static long now(Check c) {
        return c.world.getTotalWorldTime();
    }

    // ------------------------------------------------------------------------------------------

    private void registration(Check c) {
        c.eq(Content.id("trial_spawner"), TrialContent.trialSpawner.getRegistryName(), "trial spawner");
        c.eq(Content.id("vault"), TrialContent.vault.getRegistryName(), "vault");
        c.ok(TrialContent.trialKey != null && TrialContent.ominousTrialKey != null, "both trial keys");
        c.ok(TrialContent.trialOmen != null && !TrialContent.trialOmen.isBadEffect(), "Trial Omen, neutral");
        c.eq(0x16A6A6, TrialContent.trialOmen.getLiquidColor(), "Trial Omen's color");
        c.ok(EntityList.isRegistered(Content.id("ominous_item_spawner")), "ominous item spawner entity");
        // No Village & Pillage in the dev run: our copies.
        c.ok(TrialContent.copiesOmens(), "no Village & Pillage here");
        c.ok(TrialContent.badOmen != null && Omens.badOmen() == TrialContent.badOmen, "our Bad Omen is the one read");
        c.ok(TrialContent.ominousBottle != null, "our ominous bottle");
        ItemStack bottle = ModernIds.stack("minecraft:ominous_bottle");
        c.ok(bottle != null && bottle.getItem() == TrialContent.ominousBottle, "minecraft:ominous_bottle resolves to ours");
        c.ok(ModernIds.stack("minecraft:trial_key").getItem() == TrialContent.trialKey, "minecraft:trial_key resolves to ours");
        c.eq(new ResourceLocation(Content.MOD_ID, "breeze"), ModernIds.entity("minecraft:breeze"), "minecraft:breeze is ours");
        c.eq(new ResourceLocation("minecraft", "zombie"), ModernIds.entity("minecraft:zombie"), "zombie is vanilla's");
        c.ok(ModernIds.stack("minecraft:enchanted_golden_apple").getMetadata() == 1, "enchanted golden apple");
    }

    private void blocks(Check c) {
        BlockTrialSpawner spawner = (BlockTrialSpawner) TrialContent.trialSpawner;
        for (TrialSpawnerPhase phase : TrialSpawnerPhase.values()) {
            for (boolean om : new boolean[] {false, true}) {
                IBlockState s = spawner.getDefaultState().withProperty(BlockTrialSpawner.STATE, phase).withProperty(BlockTrialSpawner.OMINOUS, om);
                c.eq(s, spawner.getStateFromMeta(spawner.getMetaFromState(s)), "spawner " + phase + " " + om + " meta round trip");
            }
            c.eq(phase.light, spawner.getDefaultState().withProperty(BlockTrialSpawner.STATE, phase).getLightValue(), "spawner light " + phase);
        }
        c.eq(4, TrialSpawnerPhase.WAITING_FOR_PLAYERS.light, "waiting: light 4");
        c.eq(8, TrialSpawnerPhase.ACTIVE.light, "active: light 8");
        c.eq(50.0F, spawner.getDefaultState().getBlockHardness(c.world, BlockPos.ORIGIN), "spawner hardness 50");
        c.ok(Math.abs(spawner.getExplosionResistance(null) - 50.0F) < 0.01F, "spawner blast resistance 50");
        BlockVault vault = (BlockVault) TrialContent.vault;
        for (EnumFacing f : EnumFacing.HORIZONTALS) {
            for (VaultPhase phase : VaultPhase.values()) {
                IBlockState s = vault.getDefaultState().withProperty(BlockVault.FACING, f).withProperty(BlockVault.STATE, phase);
                c.eq(s, vault.getStateFromMeta(vault.getMetaFromState(s)), "vault " + f + " " + phase + " meta round trip");
            }
        }
        c.eq(6, vault.getDefaultState().getLightValue(), "inactive vault: light 6");
        c.eq(12, vault.getDefaultState().withProperty(BlockVault.STATE, VaultPhase.ACTIVE).getLightValue(), "active vault: light 12");
        c.ok(Math.abs(vault.getExplosionResistance(null) - 50.0F) < 0.01F, "vault blast resistance 50");
        c.eq(Items.AIR, spawner.getItemDropped(spawner.getDefaultState(), new Random(), 0), "spawner drops nothing");
        c.eq(Items.AIR, vault.getItemDropped(vault.getDefaultState(), new Random(), 0), "vault drops nothing");
    }

    private void configs(Check c) {
        for (String pool : POOLS) {
            for (String kind : new String[] {"normal", "ominous"}) {
                c.ok(SpawnerConfig.named("minecraft:trial_chamber/" + pool + "/" + kind) != null, "config " + pool + "/" + kind);
            }
        }
        c.ok(SpawnerConfig.named("minecraft:trial_chamber/nothing/normal") == null, "an unknown config is missing");
    }

    private void loot(Check c) {
        Random r = new Random(7);
        List<ItemStack> key = ModernLoot.roll(c.world, SpawnerConfig.KEY, r, 0.0F);
        c.ok(key.size() == 1 && key.get(0).getItem() == TrialContent.trialKey, "spawner key table: a trial key");
        List<ItemStack> okey = ModernLoot.roll(c.world, "minecraft:spawners/ominous/trial_chamber/key", r, 0.0F);
        c.ok(okey.size() == 1 && okey.get(0).getItem() == TrialContent.ominousTrialKey, "ominous key table: an ominous trial key");
        boolean allConsumables = true;
        for (int i = 0; i < 50; i++) {
            List<ItemStack> items = ModernLoot.roll(c.world, SpawnerConfig.CONSUMABLES, r, 0.0F);
            allConsumables &= items.size() == 1 && (items.get(0).getItem() == Items.COOKED_CHICKEN || items.get(0).getItem() == Items.BREAD
                    || items.get(0).getItem() == Items.BAKED_POTATO || items.get(0).getItem() == Items.POTIONITEM);
        }
        c.ok(allConsumables, "consumables: one food or potion each time");
        int rewards = 0;
        boolean bottleLevels = true;
        for (int i = 0; i < 100; i++) {
            List<ItemStack> items = ModernLoot.roll(c.world, "minecraft:chests/trial_chambers/reward", r, 0.0F);
            rewards += items.isEmpty() ? 0 : 1;
            for (ItemStack s : items) {
                if (s.getItem() == TrialContent.ominousBottle) {
                    bottleLevels &= s.getMetadata() <= 1;
                }
            }
        }
        c.eq(100, rewards, "a vault reward always has items");
        c.ok(bottleLevels, "the vault's ominous bottles are I or II");
        int arrows = 0;
        int potions = 0;
        for (int i = 0; i < 60; i++) {
            for (ItemStack s : ModernLoot.roll(c.world, SpawnerConfig.ITEMS_TO_DROP_WHEN_OMINOUS, r, 0.0F)) {
                arrows += s.getItem() == Items.ARROW || s.getItem() == Items.TIPPED_ARROW ? 1 : 0;
                potions += s.getItem() == Items.LINGERING_POTION ? 1 : 0;
            }
        }
        c.ok(arrows > 0 && potions > 0, "ominous drops: arrows and lingering potions (" + arrows + ", " + potions + ")");
        boolean armored = false;
        boolean armed = false;
        for (int i = 0; i < 20; i++) {
            for (ItemStack s : ModernLoot.roll(c.world, "minecraft:equipment/trial_chamber_melee", r, 0.0F)) {
                armed |= s.getItem() == Items.IRON_SWORD || s.getItem() == Items.DIAMOND_SWORD;
                armored |= EntityLiving.getSlotForItemStack(s).getSlotType() == EntityEquipmentSlot.Type.ARMOR && s.isItemEnchanted();
            }
        }
        c.ok(armed && armored, "melee equipment: a sword and enchanted armor");
    }

    // ------------------------------------------------------------------------------------------

    private static TileEntityTrialSpawner placeSpawner(Check c, BlockPos pos) {
        c.world.setBlockState(pos, TrialContent.trialSpawner.getDefaultState(), 3);
        TileEntityTrialSpawner te = (TileEntityTrialSpawner) c.world.getTileEntity(pos);
        te.overridePeacefulAndMobSpawnRule();
        return te;
    }

    /** The next time a spawner looks for players: (packed position + time) a multiple of 20. */
    private static long nextScan(BlockPos pos, long from) {
        long t = from;
        while ((pos.toLong() + t) % 20L != 0L) {
            t++;
        }
        return t;
    }

    private static void killMobs(Check c, TileEntityTrialSpawner te) {
        for (UUID id : te.currentMobs()) {
            Entity e = c.world.getEntityFromUuid(id);
            if (e != null) {
                c.world.removeEntityDangerously(e);
            }
        }
    }

    private static int items(Check c, BlockPos near, double radius) {
        int n = 0;
        for (EntityItem e : c.world.getEntitiesWithinAABB(EntityItem.class, new AxisAlignedBB(near).grow(radius))) {
            if (!e.isDead) {
                n += e.getItem().getCount();
                e.setDead();
            }
        }
        return n;
    }

    private void spawner(Check c, BlockPos area) {
        BlockPos pos = area.add(8, 0, 8);
        TileEntityTrialSpawner te = placeSpawner(c, pos);
        c.ok(te.configure("minecraft:trial_chamber/melee/zombie/normal", "minecraft:trial_chamber/melee/zombie/ominous"), "configured by name");
        EntityPlayerMP player = Players.real(c.world, "trialist");
        player.setPosition(pos.getX() + 4.5, pos.getY(), pos.getZ() + 0.5);
        te.onlySelectPlayers(Collections.singletonList(player));
        try {
            long t = 1000000L;
            time(c, t);
            te.update();
            c.eq(TrialSpawnerPhase.WAITING_FOR_PLAYERS, te.getPhase(), "with a mob to show, it waits for players");
            Entity shown = te.getOrCreateDisplayEntity(TrialSpawnerPhase.WAITING_FOR_PLAYERS);
            c.ok(shown instanceof EntityZombie, "the zombie spins inside");
            t = nextScan(pos, t + 1);
            time(c, t);
            te.update();
            c.eq(TrialSpawnerPhase.ACTIVE, te.getPhase(), "a player in sight starts the trial");
            c.ok(te.detectedPlayers().contains(player.getUniqueID()), "the player takes part");
            c.eq(t + 40, te.nextMobSpawnsAt(), "first mob two seconds after detection");
            Set<UUID> spawned = new HashSet<UUID>();
            int maxAlive = 0;
            for (int i = 0; i < 2000 && te.getPhase() == TrialSpawnerPhase.ACTIVE; i++) {
                time(c, ++t);
                te.update();
                spawned.addAll(te.currentMobs());
                maxAlive = Math.max(maxAlive, te.currentMobs().size());
                if (i % 100 == 99) {
                    killMobs(c, te);
                }
            }
            c.eq(6, spawned.size(), "one player: six zombies in all (total_mobs 6)");
            c.eq(3, maxAlive, "at most three at once (simultaneous_mobs 3)");
            c.eq(TrialSpawnerPhase.WAITING_FOR_REWARD_EJECTION, te.getPhase(), "all dead: waiting to eject");
            long cooldownEnd = te.cooldownEndsAt();
            c.eq(t + 36000, cooldownEnd, "cooldown 30 minutes from the last kill");
            items(c, pos, 3.0);
            for (int i = 0; i < 200 && te.getPhase() != TrialSpawnerPhase.COOLDOWN; i++) {
                time(c, ++t);
                te.update();
            }
            c.eq(TrialSpawnerPhase.COOLDOWN, te.getPhase(), "rewards ejected, cooling down");
            c.ok(items(c, pos, 3.0) > 0, "one roll ejected for the one player");
            time(c, cooldownEnd);
            te.update();
            c.eq(TrialSpawnerPhase.WAITING_FOR_PLAYERS, te.getPhase(), "after the cooldown it waits again");

            // Two players: more mobs.
            EntityPlayerMP second = Players.real(c.world, "trialist2");
            second.setPosition(pos.getX() - 3.5, pos.getY(), pos.getZ() + 0.5);
            te.onlySelectPlayers(java.util.Arrays.asList(player, second));
            t = nextScan(pos, now(c) + 1);
            time(c, t);
            te.update();
            c.eq(2, te.detectedPlayers().size(), "both players take part");
            spawned.clear();
            for (int i = 0; i < 3000 && te.getPhase() == TrialSpawnerPhase.ACTIVE; i++) {
                time(c, ++t);
                te.update();
                spawned.addAll(te.currentMobs());
                if (i % 100 == 99) {
                    killMobs(c, te);
                }
            }
            c.eq(8, spawned.size(), "two players: eight zombies (+2 per extra player)");

            // NBT.
            NBTTagCompound tag = te.writeToNBT(new NBTTagCompound());
            c.eq("minecraft:trial_chamber/melee/zombie/normal", tag.getString("normal_config"), "config saved by name");
            TileEntityTrialSpawner copy = new TileEntityTrialSpawner();
            copy.readFromNBT(tag);
            c.eq(te.cooldownEndsAt(), copy.cooldownEndsAt(), "NBT: cooldown");
            c.eq(te.detectedPlayers(), copy.detectedPlayers(), "NBT: players");
            c.eq(36000, copy.getTargetCooldownLength(), "NBT: cooldown length");
            c.eq(14, copy.getRequiredPlayerRange(), "NBT: player range");
            killMobs(c, te);
        } finally {
            c.world.setBlockToAir(pos);
        }
    }

    private void ominous(Check c, BlockPos area) {
        BlockPos pos = area.add(8, 0, 24);
        TileEntityTrialSpawner te = placeSpawner(c, pos);
        te.configure("minecraft:trial_chamber/melee/zombie/normal", "minecraft:trial_chamber/melee/zombie/ominous");
        EntityPlayerMP player = Players.real(c.world, "omened");
        player.setPosition(pos.getX() + 4.5, pos.getY(), pos.getZ() + 0.5);
        player.addPotionEffect(new PotionEffect(Omens.badOmen(), 120000, 2));
        te.onlySelectPlayers(Collections.singletonList(player));
        try {
            long t = 2000000L;
            time(c, t);
            te.update();
            t = nextScan(pos, t + 1);
            time(c, t);
            te.update();
            c.ok(c.world.getBlockState(pos).getValue(BlockTrialSpawner.OMINOUS), "Bad Omen makes the spawner ominous");
            c.ok(te.isOminous(), "ominous config in use");
            c.ok(!player.isPotionActive(Omens.badOmen()), "Bad Omen used up");
            PotionEffect omen = player.getActivePotionEffect(Omens.trialOmen());
            c.ok(omen != null && omen.getDuration() == 3 * 18000 && omen.getAmplifier() == 0, "Bad Omen III: 45 minutes of Trial Omen I");
            c.eq(TrialSpawnerPhase.ACTIVE, te.getPhase(), "the ominous trial starts");
            boolean armored = false;
            int itemSpawners = 0;
            for (int i = 0; i < 400; i++) {
                time(c, ++t);
                te.update();
                for (UUID id : te.currentMobs()) {
                    Entity e = c.world.getEntityFromUuid(id);
                    armored |= e instanceof EntityLiving && !((EntityLiving) e).getItemStackFromSlot(EntityEquipmentSlot.MAINHAND).isEmpty();
                }
            }
            for (EntityOminousItemSpawner e : c.world.getEntitiesWithinAABB(EntityOminousItemSpawner.class, new AxisAlignedBB(pos).grow(20))) {
                itemSpawners++;
                c.world.removeEntityDangerously(e);
            }
            c.ok(armored, "ominous zombies carry equipment");
            c.ok(itemSpawners >= 1 && itemSpawners <= 3, "ominous item spawners every 8 seconds (" + itemSpawners + " in 20 s)");
            killMobs(c, te);
        } finally {
            c.world.setBlockToAir(pos);
        }
    }

    private void spawnEgg(Check c, BlockPos area) {
        BlockPos pos = area.add(24, 0, 8);
        TileEntityTrialSpawner te = placeSpawner(c, pos);
        EntityPlayerMP player = Players.real(c.world, "egger");
        ItemStack egg = new ItemStack(Items.SPAWN_EGG);
        ItemMonsterPlacer.applyEntityIdToItemStack(egg, new ResourceLocation("minecraft", "skeleton"));
        player.setHeldItem(EnumHand.MAIN_HAND, egg);
        try {
            te.update();
            c.eq(TrialSpawnerPhase.INACTIVE, te.getPhase(), "a placed spawner has nothing to spawn");
            IBlockState state = c.world.getBlockState(pos);
            c.ok(state.getBlock().onBlockActivated(c.world, pos, state, player, EnumHand.MAIN_HAND, EnumFacing.UP, 0.5F, 1.0F, 0.5F),
                    "a spawn egg is used on it");
            c.ok(player.getHeldItem(EnumHand.MAIN_HAND).isEmpty(), "the egg is used up");
            te.update();
            c.eq(TrialSpawnerPhase.WAITING_FOR_PLAYERS, te.getPhase(), "now it has a skeleton to spawn");
            c.ok(te.getOrCreateDisplayEntity(TrialSpawnerPhase.WAITING_FOR_PLAYERS) instanceof net.minecraft.entity.monster.EntitySkeleton,
                    "a skeleton spins inside");
        } finally {
            c.world.setBlockToAir(pos);
        }
    }

    // ------------------------------------------------------------------------------------------

    private static boolean use(Check c, BlockPos pos, EntityPlayerMP player, ItemStack held) {
        player.setHeldItem(EnumHand.MAIN_HAND, held);
        IBlockState state = c.world.getBlockState(pos);
        return state.getBlock().onBlockActivated(c.world, pos, state, player, EnumHand.MAIN_HAND, EnumFacing.NORTH, 0.5F, 0.5F, 0.0F);
    }

    private static VaultPhase vaultPhase(Check c, BlockPos pos) {
        return c.world.getBlockState(pos).getValue(BlockVault.STATE);
    }

    private static void run(Check c, TileEntityVault te, int ticks) {
        for (int i = 0; i < ticks; i++) {
            time(c, now(c) + 1);
            te.update();
        }
    }

    private void vault(Check c, BlockPos area) {
        BlockPos pos = area.add(24, 0, 24);
        c.world.setBlockState(pos, TrialContent.vault.getDefaultState().withProperty(BlockVault.FACING, EnumFacing.NORTH), 3);
        TileEntityVault te = (TileEntityVault) c.world.getTileEntity(pos);
        EntityPlayerMP player = Players.real(c.world, "vaulter");
        player.setPosition(pos.getX() + 0.5, pos.getY(), pos.getZ() - 2.5);
        c.world.spawnEntity(player);
        te.onlySelectPlayers(Collections.singletonList(player));
        Advancement lockAndKey = c.world.getMinecraftServer().getAdvancementManager().getAdvancement(Content.id("adventure/under_lock_and_key"));
        Advancement revaulting = c.world.getMinecraftServer().getAdvancementManager().getAdvancement(Content.id("adventure/revaulting"));
        c.ok(lockAndKey != null && revaulting != null, "Under Lock and Key and Revaulting loaded");
        try {
            time(c, 3000000L);
            run(c, te, 2);
            c.eq(VaultPhase.ACTIVE, vaultPhase(c, pos), "a player within 4 blocks lights it");
            run(c, te, 20);
            c.ok(!te.getDisplayItem().isEmpty(), "it shows an item from its loot");
            ItemStack wrong = new ItemStack(Items.DIAMOND);
            c.ok(use(c, pos, player, wrong), "any item is tried as the key");
            c.eq(1, wrong.getCount(), "a wrong item is not taken");
            c.eq(VaultPhase.ACTIVE, vaultPhase(c, pos), "and does not unlock it");
            ItemStack ominousKey = new ItemStack(TrialContent.ominousTrialKey);
            use(c, pos, player, ominousKey);
            c.eq(1, ominousKey.getCount(), "an ominous key does not open a normal vault");
            ItemStack key = new ItemStack(TrialContent.trialKey, 2);
            use(c, pos, player, key);
            c.eq(1, key.getCount(), "the trial key is taken");
            c.eq(VaultPhase.UNLOCKING, vaultPhase(c, pos), "unlocking");
            c.ok(te.getRewardedPlayers().contains(player.getUniqueID()), "the player is remembered");
            c.ok(player.getAdvancements().getProgress(lockAndKey).isDone(), "Under Lock and Key earned");
            int expected = te.getItemsToEject().size();
            c.ok(expected > 0, "a reward rolled (" + expected + " items)");
            items(c, pos, 3.0);
            int ejected = 0;
            for (int i = 0; i < 40 * expected + 100 && vaultPhase(c, pos) != VaultPhase.INACTIVE; i++) {
                run(c, te, 1);
                ejected += items(c, pos, 3.0) > 0 ? 1 : 0;
                if (vaultPhase(c, pos) == VaultPhase.EJECTING && te.getItemsToEject().isEmpty()) {
                    run(c, te, 25);
                    break;
                }
            }
            c.eq(expected, ejected, "one item ejected per second");
            c.eq(VaultPhase.INACTIVE, vaultPhase(c, pos), "opened by its only player: it goes out");
            use(c, pos, player, key);
            c.eq(1, key.getCount(), "one key per player per vault");

            // NBT.
            NBTTagCompound tag = te.writeToNBT(new NBTTagCompound());
            TileEntityVault copy = new TileEntityVault();
            copy.readFromNBT(tag);
            c.eq(te.getRewardedPlayers(), copy.getRewardedPlayers(), "NBT: rewarded players");
            c.eq(loot(te), loot(copy), "NBT: loot table");

            // The ominous vault, as a structure places it.
            BlockPos opos = pos.add(-4, 0, 0);
            c.world.setBlockState(opos, TrialContent.vault.getDefaultState().withProperty(BlockVault.OMINOUS, true), 3);
            TileEntityVault ote = (TileEntityVault) c.world.getTileEntity(opos);
            c.ok(ote.ominousFlag(), "placing ominous=true makes an ominous vault");
            c.ok(c.world.getBlockState(opos).getActualState(c.world, opos).getValue(BlockVault.OMINOUS), "and it looks ominous");
            ote.onlySelectPlayers(Collections.singletonList(player));
            player.setPosition(opos.getX() + 0.5, opos.getY(), opos.getZ() - 2.5);
            run(c, ote, 2);
            c.eq(VaultPhase.ACTIVE, vaultPhase(c, opos), "the ominous vault lights for a new player");
            ItemStack normalKey = new ItemStack(TrialContent.trialKey);
            use(c, opos, player, normalKey);
            c.eq(1, normalKey.getCount(), "a trial key does not open the ominous vault");
            use(c, opos, player, ominousKey);
            c.eq(0, ominousKey.getCount(), "the ominous key opens it");
            c.ok(player.getAdvancements().getProgress(revaulting).isDone(), "Revaulting earned");
            NBTTagCompound otag = ote.writeToNBT(new NBTTagCompound());
            TileEntityVault ocopy = new TileEntityVault();
            ocopy.readFromNBT(otag);
            c.ok(ocopy.ominousFlag(), "NBT: ominous");
            c.world.setBlockToAir(opos);
        } finally {
            c.world.removeEntityDangerously(player);
            c.world.setBlockToAir(pos);
            items(c, pos, 6.0);
        }
    }

    /** The vault's loot table, as its NBT shows it. */
    private static String loot(TileEntityVault te) {
        return te.writeToNBT(new NBTTagCompound()).getCompoundTag("config").getString("loot_table");
    }

    private void itemSpawner(Check c, BlockPos area) {
        BlockPos pos = area.add(16, 4, 16);
        EntityOminousItemSpawner e = EntityOminousItemSpawner.create(c.world, new ItemStack(Items.ARROW));
        e.setPosition(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5);
        c.ok(e.spawnItemAfterTicks() >= 60 && e.spawnItemAfterTicks() <= 120, "drops after 3 to 6 seconds");
        NBTTagCompound tag = new NBTTagCompound();
        e.writeToNBT(tag);
        EntityOminousItemSpawner copy = new EntityOminousItemSpawner(c.world);
        copy.readFromNBT(tag);
        c.eq(e.spawnItemAfterTicks(), copy.spawnItemAfterTicks(), "NBT: delay");
        c.eq(Items.ARROW, copy.getItem().getItem(), "NBT: item");
        c.world.spawnEntity(e);
        for (int i = 0; i < 130 && !e.isDead; i++) {
            e.ticksExisted++;
            e.onUpdate();
        }
        c.ok(e.isDead, "gone once its item is out");
        List<EntityTippedArrow> arrows = c.world.getEntitiesWithinAABB(EntityTippedArrow.class, new AxisAlignedBB(pos).grow(3));
        c.ok(!arrows.isEmpty() && arrows.get(0).motionY < 0, "an arrow fired straight down");
        for (EntityTippedArrow a : arrows) {
            c.world.removeEntityDangerously(a);
        }
        EntityOminousItemSpawner food = EntityOminousItemSpawner.create(c.world, new ItemStack(Items.BREAD));
        food.setPosition(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5);
        c.world.spawnEntity(food);
        for (int i = 0; i < 130 && !food.isDead; i++) {
            food.ticksExisted++;
            food.onUpdate();
        }
        c.ok(items(c, pos, 3.0) == 1, "other items just drop");
    }

    private void bottle(Check c) {
        EntityPlayerMP player = Players.real(c.world, "drinker");
        ItemStack bottle = new ItemStack(TrialContent.ominousBottle, 1, 3);
        TrialContent.ominousBottle.onItemUseFinish(bottle, c.world, player);
        PotionEffect bad = player.getActivePotionEffect(Omens.badOmen());
        c.ok(bad != null && bad.getAmplifier() == 3 && bad.getDuration() == 120000, "ominous bottle IV: Bad Omen IV for 100 minutes");
        c.eq(0, bottle.getCount(), "the bottle is drunk");
    }
}
