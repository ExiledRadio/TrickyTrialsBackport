package com.exiledradio.trickytrialsbackport.selftest;

import com.exiledradio.trickytrialsbackport.common.Content;
import com.exiledradio.trickytrialsbackport.nautilus.ContainerNautilus;
import com.exiledradio.trickytrialsbackport.nautilus.EntityAbstractNautilus;
import com.exiledradio.trickytrialsbackport.nautilus.EntityNautilus;
import com.exiledradio.trickytrialsbackport.nautilus.EntityZombieNautilus;
import com.exiledradio.trickytrialsbackport.nautilus.ItemNautilusArmor;
import com.exiledradio.trickytrialsbackport.nautilus.NautilusAi;
import com.exiledradio.trickytrialsbackport.nautilus.NautilusArmorTier;
import com.exiledradio.trickytrialsbackport.nautilus.NautilusContent;
import com.exiledradio.trickytrialsbackport.nautilus.NautilusItems;
import com.exiledradio.trickytrialsbackport.nautilus.NautilusTestAccess;

import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityAgeable;
import net.minecraft.entity.EntityList;
import net.minecraft.entity.EntityLiving;
import net.minecraft.entity.EntitySpawnPlacementRegistry;
import net.minecraft.entity.EnumCreatureAttribute;
import net.minecraft.entity.EnumCreatureType;
import net.minecraft.entity.SharedMonsterAttributes;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.entity.monster.EntityZombie;
import net.minecraft.entity.passive.EntityPig;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.init.Biomes;
import net.minecraft.init.Blocks;
import net.minecraft.init.Items;
import net.minecraft.init.MobEffects;
import net.minecraft.item.Item;
import net.minecraft.item.ItemFishFood;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.potion.PotionEffect;
import net.minecraft.util.DamageSource;
import net.minecraft.util.EnumHand;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.GameType;
import net.minecraft.world.biome.Biome;
import net.minecraft.world.storage.loot.LootContext;
import net.minecraft.world.storage.loot.LootPool;
import net.minecraft.world.storage.loot.LootTable;
import net.minecraftforge.event.LootTableLoadEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import net.minecraftforge.fml.common.registry.EntityRegistry;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.function.Function;

/** Stream 23: the nautilus (1.21.11). */
public final class NautilusSelfTest implements SelfTest {

    @Override
    public String name() {
        return "nautilus";
    }

    /** Stand-ins for Update Aquatic's items, for the paths that need it (it is not in the test run). */
    private static final Map<String, Item> UA = new HashMap<String, Item>();

    static {
        UA.put("pufferfish_bucket", Items.LAVA_BUCKET);
        UA.put("cod_bucket", Items.MILK_BUCKET);
        UA.put("salmon_bucket", Items.BUCKET);
        UA.put("tropical_fish_bucket", Items.MAGMA_CREAM);
        UA.put("nautilus_shell", Items.PRISMARINE_SHARD);
        UA.put("trident", Items.STICK);
    }

    private BlockPos water;

    @Override
    public void run(Check c) throws Exception {
        Function<String, Item> real = NautilusItems.uaItem;
        long time = c.world.getWorldTime();
        try {
            BlockPos area = c.area();
            this.water = area.add(8, 1, 8);
            c.fill(area.add(2, 0, 2), area.add(28, 12, 28), Blocks.WATER.getDefaultState());
            registration(c);
            spawning(c, area);
            NautilusItems.uaItem = real;
            withoutUpdateAquatic(c);
            NautilusItems.uaItem = UA::get;
            withUpdateAquatic(c);
            NautilusItems.uaItem = real;
            taming(c);
            equipment(c);
            riding(c);
            breath(c);
            breeding(c);
            behavior(c);
            zombie(c, area);
            loot(c);
        } finally {
            NautilusItems.uaItem = real;
            c.world.setWorldTime(time);
            c.world.calculateInitialSkylight();
        }
    }

    private EntityNautilus nautilus(Check c) {
        return nautilus(c, this.water.add(2, 2, 2));
    }

    private EntityNautilus nautilus(Check c, BlockPos at) {
        EntityNautilus n = new EntityNautilus(c.world);
        n.setLocationAndAngles(at.getX() + 0.5D, at.getY(), at.getZ() + 0.5D, 0.0F, 0.0F);
        n.onInitialSpawn(c.world.getDifficultyForLocation(at), null);
        c.world.spawnEntity(n);
        n.onUpdate();
        return n;
    }

    private EntityPlayerMP player(Check c, String name) {
        EntityPlayerMP p = Players.real(c.world, name);
        p.setGameType(GameType.SURVIVAL);
        BlockPos at = this.water.add(2, 2, 4);
        p.setLocationAndAngles(at.getX() + 0.5D, at.getY(), at.getZ() + 0.5D, 0.0F, 0.0F);
        new io.netty.channel.embedded.EmbeddedChannel(p.connection.getNetworkManager());
        c.world.spawnEntity(p);
        return p;
    }

    private static void remove(Check c, Entity... es) {
        for (Entity e : es) {
            if (e instanceof EntityPlayerMP) {
                e.dismountRidingEntity();
                c.world.removeEntityDangerously(e);
            } else if (e != null) {
                e.setDead();
            }
        }
    }

    private static void tame(EntityAbstractNautilus n, EntityPlayerMP owner) {
        n.setTamedBy(owner);
    }

    private static void hold(EntityPlayerMP p, ItemStack stack) {
        p.setHeldItem(EnumHand.MAIN_HAND, stack);
    }

    private static boolean near(double expected, double actual) {
        return Math.abs(expected - actual) < 1.0E-4D;
    }

    // ------------------------------------------------------------------ registration

    private void registration(Check c) {
        c.eq(EntityNautilus.class, EntityList.getClass(Content.id("nautilus")), "nautilus registered");
        c.eq(EntityZombieNautilus.class, EntityList.getClass(Content.id("zombie_nautilus")), "zombie nautilus registered");
        c.eq(60, EntityRegistry.instance().lookupModSpawn(EntityNautilus.class, false).getModEntityId(), "nautilus entity id 60");
        c.eq(61, EntityRegistry.instance().lookupModSpawn(EntityZombieNautilus.class, false).getModEntityId(), "zombie nautilus entity id 61");
        c.ok(EntityList.ENTITY_EGGS.containsKey(Content.id("nautilus")), "nautilus spawn egg");
        c.ok(EntityList.ENTITY_EGGS.containsKey(Content.id("zombie_nautilus")), "zombie nautilus spawn egg");
        c.eq(5, NautilusContent.ARMOR.size(), "five nautilus armors");
        for (NautilusArmorTier tier : NautilusArmorTier.values()) {
            ItemNautilusArmor item = NautilusContent.ARMOR.get(tier);
            c.ok(item != null && Content.id(tier.itemName).equals(item.getRegistryName()), tier.itemName + " registered");
            if (item != null) {
                c.eq(1, new ItemStack(item).getMaxStackSize(), tier.itemName + " stacks to 1");
                c.ok(!item.isDamageable(), tier.itemName + " has no durability");
            }
        }
        c.eq(4, NautilusArmorTier.COPPER.protection, "copper 4");
        c.eq(5, NautilusArmorTier.IRON.protection, "iron 5");
        c.eq(7, NautilusArmorTier.GOLDEN.protection, "gold 7");
        c.eq(11, NautilusArmorTier.DIAMOND.protection, "diamond 11");
        c.eq(19, NautilusArmorTier.NETHERITE.protection, "netherite 19");
        c.ok(NautilusContent.breath != null && Content.id("breath_of_the_nautilus").equals(NautilusContent.breath.getRegistryName()),
                "Breath of the Nautilus registered");
        if (NautilusContent.breath != null) {
            c.ok(!NautilusContent.breath.isBadEffect(), "Breath of the Nautilus is not harmful");
            c.eq(0x00FFEE, NautilusContent.breath.getLiquidColor(), "Breath of the Nautilus color");
        }
        for (ResourceLocation id : new ResourceLocation[] {Content.id("entity.nautilus.dash"), Content.id("entity.zombie_nautilus.dash"),
                Content.id("entity.nautilus.riding"), Content.id("item.armor.equip_nautilus"), Content.id("item.nautilus_saddle_equip")}) {
            c.ok(net.minecraft.util.SoundEvent.REGISTRY.containsKey(id), "sound " + id);
        }
        c.ok(c.world.getLootTableManager().getLootTableFromLocation(EntityZombieNautilus.LOOT) != LootTable.EMPTY_LOOT_TABLE, "zombie loot table");
        EntityNautilus n = nautilus(c);
        c.ok(near(0.875D, n.width) && near(0.95D, n.height), "0.875 x 0.95");
        c.ok(near(0.2751D, n.getEyeHeight()), "eyes at 0.2751");
        c.ok(near(15.0D, n.getMaxHealth()), "15 health");
        c.ok(near(1.0D, n.getEntityAttribute(SharedMonsterAttributes.MOVEMENT_SPEED).getBaseValue()), "speed 1");
        c.ok(near(3.0D, n.getEntityAttribute(SharedMonsterAttributes.ATTACK_DAMAGE).getBaseValue()), "attack 3");
        c.ok(near(0.3D, n.getEntityAttribute(SharedMonsterAttributes.KNOCKBACK_RESISTANCE).getBaseValue()), "knockback resistance 0.3");
        c.ok(n.canBreatheUnderwater(), "breathes under water");
        c.ok(!n.shouldDismountInWater(n), "riders stay on under water");
        c.ok(!n.isPotionApplicable(new PotionEffect(MobEffects.POISON, 100)), "poison does not take");
        c.ok(n.isPotionApplicable(new PotionEffect(MobEffects.SLOWNESS, 100)), "slowness does");
        c.ok(n.getNavigator() instanceof net.minecraft.pathfinding.PathNavigateSwimmer, "water-bound navigation");
        c.ok(n.getMoveHelper() instanceof NautilusAi.SwimMoveHelper, "smooth swimming move control");
        c.ok(n.getLookHelper() instanceof NautilusAi.SwimLookHelper, "smooth swimming look control");
        EntityNautilus baby = nautilus(c);
        baby.setGrowingAge(-24000);
        c.ok(baby.isChild() && near(0.4375D, baby.width), "a baby is half size");
        remove(c, n, baby);
    }

    // ------------------------------------------------------------------ spawning

    private void spawning(Check c, BlockPos area) {
        int sea = c.world.getSeaLevel();
        BlockPos col = area.add(30, 0, 30);
        BlockPos at = new BlockPos(col.getX(), sea - 10, col.getZ());
        c.fill(at.down(30), at.up(30), Blocks.WATER.getDefaultState());
        c.ok(EntityNautilus.checkSpawnRules(c.world, at), "10 below sea level in water: may spawn");
        c.ok(EntityNautilus.checkSpawnRules(c.world, at.up(5)), "5 below: may spawn");
        c.ok(!EntityNautilus.checkSpawnRules(c.world, at.up(6)), "4 below: may not");
        c.ok(EntityNautilus.checkSpawnRules(c.world, at.down(15)), "25 below: may spawn");
        c.ok(!EntityNautilus.checkSpawnRules(c.world, at.down(16)), "26 below: may not");
        c.world.setBlockState(at.up(), Blocks.AIR.getDefaultState(), 2);
        c.ok(!EntityNautilus.checkSpawnRules(c.world, at), "no water above: may not");
        c.world.setBlockState(at.up(), Blocks.WATER.getDefaultState(), 2);
        c.world.setBlockState(at.down(), Blocks.STONE.getDefaultState(), 2);
        c.ok(!EntityNautilus.checkSpawnRules(c.world, at), "no water below: may not");
        c.fill(at.down(30), at.up(30), Blocks.STONE.getDefaultState());
        c.eq(EntityLiving.SpawnPlacementType.IN_WATER, EntitySpawnPlacementRegistry.getPlacementForEntity(EntityNautilus.class), "placed in water");
        EntityNautilus n = nautilus(c);
        c.ok(n.isCreatureType(EnumCreatureType.WATER_CREATURE, false), "a water creature");
        c.ok(!n.isCreatureType(EnumCreatureType.CREATURE, false), "not a land creature");
        n.enablePersistence();
        c.ok(!n.isCreatureType(EnumCreatureType.WATER_CREATURE, true), "a persistent one does not count to the cap");
        remove(c, n);
        EntityZombieNautilus z = new EntityZombieNautilus(c.world);
        c.ok(z.isCreatureType(EnumCreatureType.MONSTER, false), "the zombie nautilus is a monster");
        c.eq(5, spawnWeight(Biomes.OCEAN), "ocean weight 5");
        c.eq(5, spawnWeight(Biomes.DEEP_OCEAN), "deep ocean weight 5");
        c.eq(2, spawnWeight(Biomes.FROZEN_OCEAN), "frozen ocean weight 2");
        c.eq(0, spawnWeight(Biomes.PLAINS), "no nautiluses on plains");
        c.eq(0, spawnWeight(Biomes.RIVER), "nor in rivers");
        for (Biome.SpawnListEntry e : Biomes.OCEAN.getSpawnableList(EnumCreatureType.WATER_CREATURE)) {
            if (e.entityClass == EntityNautilus.class) {
                c.ok(e.minGroupCount == 1 && e.maxGroupCount == 1, "alone");
            }
        }
    }

    private static int spawnWeight(Biome biome) {
        for (Biome.SpawnListEntry e : biome.getSpawnableList(EnumCreatureType.WATER_CREATURE)) {
            if (e.entityClass == EntityNautilus.class) {
                return e.itemWeight;
            }
        }
        return 0;
    }

    // ------------------------------------------------------------------ Update Aquatic's items

    private void withoutUpdateAquatic(Check c) {
        c.ok(NautilusItems.isTamingItem(new ItemStack(Items.FISH, 1, ItemFishFood.FishType.PUFFERFISH.getMetadata())), "a pufferfish tames");
        c.ok(!NautilusItems.isTamingItem(new ItemStack(Items.FISH)), "a cod does not");
        c.ok(NautilusItems.isFood(new ItemStack(Items.FISH)) && NautilusItems.isFood(new ItemStack(Items.COOKED_FISH, 1, 1)), "fish are food");
        c.ok(!NautilusItems.isFood(new ItemStack(Items.LAVA_BUCKET)), "no bucket is food without Update Aquatic");
        c.ok(NautilusContent.shell() == null, "no shell without Update Aquatic");
        EntityZombie zombie = new EntityZombie(c.world);
        zombie.setLocationAndAngles(this.water.getX(), this.water.getY() + 2, this.water.getZ(), 0, 0);
        zombie.setHeldItem(EnumHand.MAIN_HAND, new ItemStack(Items.STICK));
        c.world.spawnEntity(zombie);
        boolean any = false;
        for (int i = 0; i < 40; i++) {
            any |= NautilusContent.maybeMount(zombie) != null;
        }
        c.ok(!any && !zombie.isRiding(), "without Update Aquatic's trident no rider gets a mount");
        remove(c, zombie);
    }

    private void withUpdateAquatic(Check c) {
        c.ok(NautilusItems.isTamingItem(new ItemStack(Items.LAVA_BUCKET)), "the pufferfish bucket tames");
        c.ok(NautilusItems.isFood(new ItemStack(Items.MILK_BUCKET)) && NautilusItems.isBucketFood(new ItemStack(Items.MILK_BUCKET)), "the cod bucket is food");
        c.eq(Items.PRISMARINE_SHARD, NautilusContent.shell(), "the shell is Update Aquatic's");

        EntityPlayerMP p = player(c, "naut_ua");
        EntityNautilus n = nautilus(c);
        tame(n, p);
        n.setHealth(5.0F);
        hold(p, new ItemStack(Items.MILK_BUCKET));
        c.ok(n.processInteract(p, EnumHand.MAIN_HAND), "a fish bucket heals");
        c.eq(Items.WATER_BUCKET, p.getHeldItemMainhand().getItem(), "and leaves a water bucket");
        c.ok(near(6.0D, n.getHealth()), "a bucket heals 1");
        EntityNautilus wild = nautilus(c);
        int tries = 0;
        while (!wild.isTamed() && tries < 60) {
            hold(p, new ItemStack(Items.LAVA_BUCKET));
            wild.processInteract(p, EnumHand.MAIN_HAND);
            tries++;
            c.ok(p.getHeldItemMainhand().getItem() == Items.WATER_BUCKET, "the pufferfish bucket is emptied (" + tries + ")");
        }
        c.ok(wild.isTamed() && p.getUniqueID().equals(wild.getOwnerId()), "tamed with buckets of pufferfish");

        // Drowned riders: a natural adult holding a trident outside rivers, half the time.
        int mounted = 0;
        for (int i = 0; i < 200; i++) {
            EntityZombie drowned = new EntityZombie(c.world);
            drowned.setLocationAndAngles(this.water.getX() + 4, this.water.getY() + 2, this.water.getZ() + 4, 30.0F, 0);
            drowned.setHeldItem(EnumHand.MAIN_HAND, new ItemStack(Items.STICK));
            c.world.spawnEntity(drowned);
            EntityZombieNautilus mount = NautilusContent.maybeMount(drowned);
            if (mount != null) {
                mounted++;
                if (mounted == 1) {
                    mount.updatePassenger(drowned);
                    c.ok(drowned.getRidingEntity() == mount, "the drowned rides its zombie nautilus");
                    c.ok(near(drowned.posY, mount.posY + mount.getMountedYOffset() + drowned.getYOffset()), "seated on it");
                    c.ok(near(30.0D, mount.rotationYaw), "facing the drowned's way");
                    c.ok(mount.isMobControlled(), "mob controlled");
                    c.ok(mount.getControllingPassenger() == null, "a mob does not steer like a player");
                    c.ok(!mount.canBeLeashedTo(p), "a mob-ridden zombie nautilus cannot be leashed");
                }
                remove(c, mount);
            }
            remove(c, drowned);
        }
        if (net.minecraftforge.common.BiomeDictionary.hasType(c.world.getBiome(this.water), net.minecraftforge.common.BiomeDictionary.Type.RIVER)) {
            c.eq(0, mounted, "none in a river");
        } else {
            c.ok(mounted > 60 && mounted < 140, "about half get a mount: " + mounted + " of 200");
        }
        EntityZombie baby = new EntityZombie(c.world);
        baby.setChild(true);
        baby.setHeldItem(EnumHand.MAIN_HAND, new ItemStack(Items.STICK));
        baby.setLocationAndAngles(this.water.getX(), this.water.getY() + 2, this.water.getZ(), 0, 0);
        c.world.spawnEntity(baby);
        boolean any = false;
        for (int i = 0; i < 40; i++) {
            any |= NautilusContent.maybeMount(baby) != null;
        }
        c.ok(!any, "a baby drowned never gets one");
        remove(c, baby, n, wild, p);
    }

    // ------------------------------------------------------------------ taming

    private void taming(Check c) {
        EntityPlayerMP p = player(c, "naut_tame");
        EntityNautilus n = nautilus(c);
        hold(p, new ItemStack(Items.FISH, 5));
        c.ok(!n.processInteract(p, EnumHand.MAIN_HAND), "a wild adult takes no cod");
        c.eq(5, p.getHeldItemMainhand().getCount(), "the cod is kept");
        c.ok(!n.isTamed(), "and it is not tamed");
        c.ok(n.isNoDespawnRequired(), "any use makes it persistent");
        hold(p, new ItemStack(Items.SADDLE));
        n.processInteract(p, EnumHand.MAIN_HAND);
        c.ok(!n.isSaddled() && p.getHeldItemMainhand().getItem() == Items.SADDLE, "a wild one takes no saddle");
        n.processInteract(p, EnumHand.MAIN_HAND);
        c.ok(!p.isRiding(), "a wild one cannot be ridden");
        int tries = 0;
        int eaten = 0;
        while (!n.isTamed() && tries < 60) {
            ItemStack puffer = new ItemStack(Items.FISH, 2, ItemFishFood.FishType.PUFFERFISH.getMetadata());
            hold(p, puffer);
            n.processInteract(p, EnumHand.MAIN_HAND);
            eaten += 2 - p.getHeldItemMainhand().getCount();
            tries++;
        }
        c.ok(n.isTamed(), "tamed with pufferfish after " + tries);
        c.eq(tries, eaten, "one pufferfish each try");
        c.eq(p.getUniqueID(), n.getOwnerId(), "owned by the player");
        c.ok(tries < 60, "one in three: tamed within 60 tries");
        // Creative players keep their pufferfish.
        EntityNautilus other = nautilus(c);
        p.setGameType(GameType.CREATIVE);
        hold(p, new ItemStack(Items.FISH, 1, ItemFishFood.FishType.PUFFERFISH.getMetadata()));
        other.processInteract(p, EnumHand.MAIN_HAND);
        c.eq(1, p.getHeldItemMainhand().getCount(), "creative keeps the pufferfish");
        p.setGameType(GameType.SURVIVAL);
        // The odds over many: about a third.
        int tamed = 0;
        for (int i = 0; i < 300; i++) {
            EntityNautilus w = new EntityNautilus(c.world);
            w.setPosition(this.water.getX(), this.water.getY() + 2, this.water.getZ());
            NautilusTestAccess.tryToTame(w, p);
            if (w.isTamed()) {
                tamed++;
            }
        }
        c.ok(tamed > 70 && tamed < 130, "about one in three tames: " + tamed + " of 300");
        // A hurt tamed one heals 2 x the fish's food: a cod heals 4.
        n.setHealth(5.0F);
        hold(p, new ItemStack(Items.FISH, 3));
        c.ok(n.processInteract(p, EnumHand.MAIN_HAND), "a hurt tamed nautilus eats a cod");
        c.ok(near(9.0D, n.getHealth()), "and heals 4: " + n.getHealth());
        c.eq(2, p.getHeldItemMainhand().getCount(), "the cod is eaten");
        remove(c, n, other, p);
    }

    // ------------------------------------------------------------------ equipment

    private void equipment(Check c) {
        EntityPlayerMP p = player(c, "naut_equip");
        EntityNautilus n = nautilus(c);
        tame(n, p);
        hold(p, new ItemStack(Items.SADDLE, 2));
        c.ok(n.processInteract(p, EnumHand.MAIN_HAND), "a saddle goes on by use");
        c.ok(n.isSaddled(), "saddled");
        c.eq(1, p.getHeldItemMainhand().getCount(), "one saddle used");
        n.processInteract(p, EnumHand.MAIN_HAND);
        c.eq(1, p.getHeldItemMainhand().getCount(), "no second saddle");
        c.eq(n, p.getRidingEntity(), "a saddled one is ridden instead (as in 1.21.11)");
        p.dismountRidingEntity();
        ItemNautilusArmor iron = NautilusContent.ARMOR.get(NautilusArmorTier.IRON);
        hold(p, new ItemStack(iron));
        c.ok(n.processInteract(p, EnumHand.MAIN_HAND), "armor goes on by use");
        c.eq(iron, n.getBodyArmor().getItem(), "wears iron armor");
        c.ok(p.getHeldItemMainhand().isEmpty(), "the armor is used");
        c.ok(near(5.0D, n.getTotalArmorValue()), "iron gives 5 armor: " + n.getTotalArmorValue());
        n.setBodyArmor(new ItemStack(NautilusContent.ARMOR.get(NautilusArmorTier.NETHERITE)));
        c.ok(near(19.0D, n.getTotalArmorValue()), "netherite gives 19 armor");
        c.ok(near(3.0D, n.getEntityAttribute(SharedMonsterAttributes.ARMOR_TOUGHNESS).getAttributeValue()), "and 3 toughness");
        c.ok(near(0.4D, n.getEntityAttribute(SharedMonsterAttributes.KNOCKBACK_RESISTANCE).getAttributeValue()), "and 0.1 knockback resistance");
        n.setBodyArmor(new ItemStack(NautilusContent.ARMOR.get(NautilusArmorTier.DIAMOND)));
        c.ok(near(11.0D, n.getTotalArmorValue()) && near(2.0D, n.getEntityAttribute(SharedMonsterAttributes.ARMOR_TOUGHNESS).getAttributeValue()),
                "diamond: 11 armor, 2 toughness");
        c.ok(near(0.3D, n.getEntityAttribute(SharedMonsterAttributes.KNOCKBACK_RESISTANCE).getAttributeValue()), "the netherite extra is gone");

        // Saved and loaded.
        NBTTagCompound tag = new NBTTagCompound();
        n.writeToNBT(tag);
        EntityNautilus copy = new EntityNautilus(c.world);
        copy.readFromNBT(tag);
        c.ok(copy.isSaddled() && copy.getBodyArmor().getItem() == NautilusContent.ARMOR.get(NautilusArmorTier.DIAMOND), "saved and loaded");
        c.ok(near(11.0D, copy.getTotalArmorValue()), "loaded armor counts");
        c.ok(copy.isTamed() && p.getUniqueID().equals(copy.getOwnerId()), "tamed and owned after loading");

        // The inventory: sneaking opens it.
        p.setSneaking(true);
        hold(p, ItemStack.EMPTY);
        n.processInteract(p, EnumHand.MAIN_HAND);
        c.ok(p.openContainer instanceof ContainerNautilus, "sneaking opens the inventory");
        c.ok(!p.isRiding(), "and does not mount");
        if (p.openContainer instanceof ContainerNautilus) {
            ContainerNautilus menu = (ContainerNautilus) p.openContainer;
            c.ok(menu.getSlot(0).xPos == 8 && menu.getSlot(0).yPos == 18 && menu.getSlot(1).xPos == 8 && menu.getSlot(1).yPos == 36, "1.21.11's slot places");
            c.ok(menu.getSlot(0).isItemValid(new ItemStack(Items.SADDLE)) || menu.getSlot(0).getHasStack(), "the saddle slot takes a saddle");
            c.ok(!menu.getSlot(0).isItemValid(new ItemStack(Items.APPLE)), "and not an apple");
            c.ok(menu.getSlot(1).isItemValid(new ItemStack(iron)), "the armor slot takes nautilus armor");
            c.ok(!menu.getSlot(1).isItemValid(new ItemStack(Items.DIAMOND_HORSE_ARMOR)), "and not horse armor");
            c.eq(38, menu.inventorySlots.size(), "2 slots and the player's 36");
            p.closeScreen();
        }
        p.setSneaking(false);
        EntityNautilus wild = nautilus(c);
        ContainerNautilus wildMenu = new ContainerNautilus(p.inventory, wild);
        c.ok(!wildMenu.getSlot(1).isItemValid(new ItemStack(iron)), "a wild one's armor slot takes nothing");

        // Shears: the armor, then the saddle; not while ridden.
        hold(p, new ItemStack(Items.SHEARS));
        c.ok(n.processInteract(p, EnumHand.MAIN_HAND), "shears take something off");
        c.ok(n.getBodyArmor().isEmpty() && n.isSaddled(), "the armor first");
        c.eq(1, p.getHeldItemMainhand().getItemDamage(), "shears damaged 1");
        n.processInteract(p, EnumHand.MAIN_HAND);
        c.ok(!n.isSaddled(), "then the saddle");
        List<EntityItem> drops = c.world.getEntitiesWithinAABB(EntityItem.class, n.getEntityBoundingBox().grow(3.0D));
        boolean armorDropped = false;
        boolean saddleDropped = false;
        for (EntityItem e : drops) {
            armorDropped |= e.getItem().getItem() instanceof ItemNautilusArmor;
            saddleDropped |= e.getItem().getItem() == Items.SADDLE;
            e.setDead();
        }
        c.ok(armorDropped && saddleDropped, "both dropped");
        n.setSaddle(new ItemStack(Items.SADDLE));
        EntityPlayerMP rider = player(c, "naut_equip2");
        rider.startRiding(n, true);
        c.ok(!n.processInteract(p, EnumHand.MAIN_HAND) || n.isSaddled(), "shears do nothing while ridden");
        c.ok(n.isSaddled(), "the saddle stays on a ridden nautilus");
        rider.dismountRidingEntity();

        // A dispenser puts armor on.
        c.ok(n.dispenseEquip(new ItemStack(iron)), "a dispenser equips armor");
        c.ok(!n.dispenseEquip(new ItemStack(iron)), "but not over armor");
        c.ok(!wild.dispenseEquip(new ItemStack(iron)), "nor onto a wild one");

        // Dropped on death.
        n.attackEntityFrom(DamageSource.OUT_OF_WORLD, 100.0F);
        drops = c.world.getEntitiesWithinAABB(EntityItem.class, n.getEntityBoundingBox().grow(3.0D));
        armorDropped = false;
        saddleDropped = false;
        for (EntityItem e : drops) {
            armorDropped |= e.getItem().getItem() == iron;
            saddleDropped |= e.getItem().getItem() == Items.SADDLE;
            e.setDead();
        }
        c.ok(armorDropped && saddleDropped, "armor and saddle dropped on death");
        remove(c, n, wild, rider, p);
    }

    // ------------------------------------------------------------------ riding

    private void riding(Check c) {
        EntityPlayerMP p = player(c, "naut_ride");
        EntityNautilus n = nautilus(c);
        hold(p, ItemStack.EMPTY);
        tame(n, p);
        n.processInteract(p, EnumHand.MAIN_HAND);
        c.eq(n, p.getRidingEntity(), "a tamed nautilus is ridden by use");
        c.ok(n.getControllingPassenger() == null, "no saddle: no steering");
        c.ok(!n.canJump(), "no saddle: no dash");
        EntityPlayerMP second = player(c, "naut_ride2");
        n.processInteract(second, EnumHand.MAIN_HAND);
        c.ok(second.getRidingEntity() == null, "one rider only");
        p.dismountRidingEntity();
        n.setSaddle(new ItemStack(Items.SADDLE));
        hold(p, new ItemStack(Items.FISH));
        n.processInteract(p, EnumHand.MAIN_HAND);
        c.ok(!p.isRiding(), "holding food does not mount (it feeds or breeds)");
        hold(p, ItemStack.EMPTY);
        n.processInteract(p, EnumHand.MAIN_HAND);
        c.eq(p, n.getControllingPassenger(), "a saddled nautilus is steered by its rider");
        c.ok(n.canJump(), "a saddled nautilus can dash");
        n.updatePassenger(p);
        c.ok(near(n.posY + 1.1375D - 0.6D, p.posY), "the rider sits at 1.21.11's height: " + (p.posY - n.posY));

        // Ridden input: forward along the look, back at half and mirrored.
        Vec3d in = EntityAbstractNautilus.riddenInput(0.0F, 1.0F, 0.0F);
        c.ok(near(0, in.x) && near(0, in.y) && near(1, in.z), "forward, level");
        in = EntityAbstractNautilus.riddenInput(0.0F, 1.0F, -90.0F);
        c.ok(near(1, in.y) && near(0, in.z), "forward looking up rises");
        in = EntityAbstractNautilus.riddenInput(0.0F, -1.0F, 30.0F);
        c.ok(near(-0.5D * Math.cos(Math.toRadians(30)), in.z) && near(0.5D * Math.sin(Math.toRadians(30)), in.y), "back is half, mirrored");
        in = EntityAbstractNautilus.riddenInput(1.0F, 0.0F, 0.0F);
        c.ok(near(1, in.x) && near(0, in.z), "strafe as pressed");

        // Ridden swimming: 0.0325 under water, kept 0.9 each tick, no gravity.
        c.ok(n.isInWater(), "in the water");
        c.ok(near(0.0325D, n.riddenSpeed()), "ridden speed 0.0325 in water");
        p.moveForward = 1.0F;
        p.moveStrafing = 0.0F;
        p.rotationYaw = 0.0F;
        p.rotationPitch = 0.0F;
        n.rotationYaw = 0.0F;
        n.motionX = n.motionY = n.motionZ = 0.0D;
        n.travelRidden(p);
        c.ok(near(0.0325D * 0.9D, n.motionZ), "one tick: 0.0325 then 0.9 kept: " + n.motionZ);
        c.ok(near(0.0D, n.motionY), "no gravity in water");
        for (int i = 0; i < 120; i++) {
            n.posX = this.water.getX() + 4.5D;
            n.posZ = this.water.getZ() + 4.5D;
            n.setPosition(n.posX, n.posY, n.posZ);
            n.travelRidden(p);
        }
        c.ok(Math.abs(n.motionZ - 0.2925D) < 0.002D, "top speed about 0.29 a tick (5.9 blocks a second): " + n.motionZ);

        // Turning: half way to the rider's yaw each tick, half their pitch.
        p.rotationYaw = 90.0F;
        p.rotationPitch = 40.0F;
        n.rotationYaw = 0.0F;
        NautilusTestAccess.tickRidden(n, p);
        c.ok(near(45.0D, n.rotationYaw) && near(20.0D, n.rotationPitch), "turns half way and takes half the pitch");
        c.ok(near(45.0D, n.renderYawOffset) && near(45.0D, n.rotationYawHead), "body and head follow");

        // The dash.
        p.rotationYaw = 0.0F;
        p.rotationPitch = 0.0F;
        n.rotationYaw = 0.0F;
        n.motionX = n.motionY = n.motionZ = 0.0D;
        n.setJumpPower(100);
        c.ok(near(1.0D, NautilusTestAccess.pendingDash(n)), "a full charge");
        n.setJumpPower(45);
        c.ok(near(0.6D, NautilusTestAccess.pendingDash(n)), "half a charge: 0.4 + 0.4 x 45 / 90");
        n.setJumpPower(100);
        n.travelRidden(p);
        c.ok(near((1.2D + 0.0325D) * 0.9D, n.motionZ), "the dash: the look x 1.2 in water: " + n.motionZ);
        c.eq(40, n.getDashCooldown(), "40 ticks of cooldown");
        c.ok(n.isDashing(), "dashing");
        n.setJumpPower(100);
        c.ok(near(0.0D, NautilusTestAccess.pendingDash(n)), "no dash during the cooldown");
        for (int i = 0; i < 7; i++) {
            n.onUpdate();
        }
        c.ok(!n.isDashing(), "the dash flag drops after 5 ticks");
        for (int i = 0; i < 40; i++) {
            n.onUpdate();
        }
        c.eq(0, n.getDashCooldown(), "the cooldown runs out");
        EntityNautilus server = nautilus(c);
        server.setSaddle(new ItemStack(Items.SADDLE));
        tame(server, p);
        server.handleStartJump(80);
        c.ok(server.isDashing() && server.getDashCooldown() == 40, "the server hears the dash: dashing, cooldown 40");

        // On land: 0.02 and 1.12.2's own travel; the dash is 0.5.
        EntityNautilus dry = nautilus(c, this.water.add(0, 13, 0));
        c.ok(!dry.isInWater(), "out of water");
        c.ok(near(0.02D, dry.riddenSpeed()), "ridden speed 0.02 on land");

        // The server does not steer for a real player.
        n.motionX = 0.5D;
        n.travel(0.0F, 0.0F, 1.0F);
        c.ok(near(0.0D, n.motionX), "the server zeroes its motion under a player");

        // The inventory key while riding (packet 115's handler).
        p.closeScreen();
        NautilusTestAccess.openRidden(p);
        c.ok(p.openContainer instanceof ContainerNautilus, "the inventory key opens the ridden nautilus's inventory");
        p.closeScreen();
        remove(c, n, server, dry, second, p);
    }

    // ------------------------------------------------------------------ Breath of the Nautilus

    private void breath(Check c) {
        EntityPlayerMP p = player(c, "naut_breath");
        EntityNautilus n = nautilus(c);
        tame(n, p);
        p.startRiding(n, true);
        n.onUpdate();
        PotionEffect e = NautilusContent.breath == null ? null : p.getActivePotionEffect(NautilusContent.breath);
        c.ok(e != null, "the rider gets Breath of the Nautilus");
        if (e != null) {
            c.ok(e.getDuration() <= 60 && e.getDuration() > 50, "for 60 ticks: " + e.getDuration());
            c.ok(e.getIsAmbient(), "ambient");
            c.eq(0, e.getAmplifier(), "level I");
        }
        p.removePotionEffect(NautilusContent.breath);
        long t = c.world.getTotalWorldTime();
        if (t % 40 == 0) {
            c.world.getWorldInfo().setWorldTotalTime(t + 1);
        }
        p.addPotionEffect(new PotionEffect(NautilusContent.breath, 30, 0));
        n.onUpdate();
        c.eq(30, p.getActivePotionEffect(NautilusContent.breath).getDuration(), "not refreshed between beats");
        c.world.getWorldInfo().setWorldTotalTime(c.world.getTotalWorldTime() + (40 - c.world.getTotalWorldTime() % 40));
        n.onUpdate();
        c.eq(60, p.getActivePotionEffect(NautilusContent.breath).getDuration(), "refreshed every 40 ticks");

        // Air under water is held.
        c.ok(p.isInsideOfMaterial(net.minecraft.block.material.Material.WATER), "the rider's head is under water");
        c.ok(NautilusContent.holdsAir(p), "the effect holds the air");
        p.setAir(150);
        NautilusContent.onPlayerTick(new TickEvent.PlayerTickEvent(TickEvent.Phase.START, p));
        p.setAir(149);
        NautilusContent.onPlayerTick(new TickEvent.PlayerTickEvent(TickEvent.Phase.END, p));
        c.eq(150, p.getAir(), "no air lost under water");
        p.addPotionEffect(new PotionEffect(MobEffects.WATER_BREATHING, 100));
        c.ok(!NautilusContent.holdsAir(p), "water breathing takes over");
        p.removePotionEffect(MobEffects.WATER_BREATHING);
        p.dismountRidingEntity();
        p.removePotionEffect(NautilusContent.breath);
        c.ok(!NautilusContent.holdsAir(p), "without the effect the air is not held");
        // A mob with the effect: its air comes back each tick.
        EntityPig pig = new EntityPig(c.world);
        pig.setPosition(this.water.getX() + 6, this.water.getY() + 2, this.water.getZ() + 6);
        c.world.spawnEntity(pig);
        pig.addPotionEffect(new PotionEffect(NautilusContent.breath, 200));
        pig.setAir(200);
        for (int i = 0; i < 20; i++) {
            pig.onUpdate();
        }
        c.ok(pig.getAir() >= 199, "a pig with the effect keeps its air: " + pig.getAir());
        remove(c, n, pig, p);
    }

    // ------------------------------------------------------------------ breeding

    private void breeding(Check c) {
        EntityPlayerMP p = player(c, "naut_breed");
        EntityNautilus a = nautilus(c);
        EntityNautilus b = nautilus(c, this.water.add(3, 2, 2));
        tame(a, p);
        tame(b, p);
        hold(p, new ItemStack(Items.COOKED_FISH, 4, 1));
        a.processInteract(p, EnumHand.MAIN_HAND);
        b.processInteract(p, EnumHand.MAIN_HAND);
        c.ok(a.isInLove() && b.isInLove(), "fish make tamed adults fall in love");
        c.eq(2, p.getHeldItemMainhand().getCount(), "one fish each");
        c.ok(a.canMateWith(b), "they can mate");
        EntityAgeable baby = a.createChild(b);
        c.ok(baby instanceof EntityNautilus, "the baby is a nautilus");
        if (baby instanceof EntityNautilus) {
            c.ok(((EntityNautilus) baby).isTamed() && p.getUniqueID().equals(((EntityNautilus) baby).getOwnerId()), "tamed to the parents' owner");
        }
        EntityNautilus wild = nautilus(c);
        c.ok(wild.createChild(a) instanceof EntityNautilus && !((EntityNautilus) wild.createChild(a)).isTamed(), "a wild parent's baby is wild");
        c.ok(!a.canMateWith(new EntityZombieNautilus(c.world)), "not with a zombie nautilus");
        EntityNautilus young = nautilus(c);
        young.setGrowingAge(-24000);
        hold(p, new ItemStack(Items.FISH, 2));
        young.processInteract(p, EnumHand.MAIN_HAND);
        c.ok(young.getGrowingAge() > -24000, "a fish speeds up a baby: " + young.getGrowingAge());
        c.ok(young.isFood(new ItemStack(Items.FISH)) && !young.isFood(new ItemStack(Items.WHEAT)), "a baby eats fish");
        c.ok(!young.canUseEquipment(), "a baby takes no saddle");
        hold(p, new ItemStack(Items.SADDLE));
        young.processInteract(p, EnumHand.MAIN_HAND);
        c.ok(!young.isSaddled(), "a baby is never saddled");
        remove(c, a, b, wild, young, p);
    }

    // ------------------------------------------------------------------ behavior

    private void behavior(Check c) {
        EntityPlayerMP p = player(c, "naut_ai");
        EntityNautilus n = nautilus(c);
        hold(p, new ItemStack(Items.FISH));
        c.eq(p, NautilusTestAccess.temptingPlayer(n), "fish in hand tempts");
        hold(p, new ItemStack(Items.STICK));
        c.ok(NautilusTestAccess.temptingPlayer(n) == null, "a stick does not");
        p.setHeldItem(EnumHand.OFF_HAND, new ItemStack(Items.COOKED_FISH));
        c.eq(p, NautilusTestAccess.temptingPlayer(n), "the off hand tempts too");
        p.setHeldItem(EnumHand.OFF_HAND, ItemStack.EMPTY);
        hold(p, new ItemStack(Items.FISH));
        p.setPosition(n.posX + 11.0D, n.posY, n.posZ);
        c.ok(NautilusTestAccess.temptingPlayer(n) == null, "not from 11 blocks");
        p.setPosition(n.posX + 2.0D, n.posY, n.posZ);

        // Hurt by a player in the water: angry and panicking.
        c.ok(n.attackEntityFrom(DamageSource.causePlayerDamage(p), 1.0F), "hurt");
        c.ok(NautilusTestAccess.panicking(n), "panics");
        c.eq(p.getUniqueID(), NautilusTestAccess.angryAt(n), "angry at the player");
        c.eq(400L, NautilusTestAccess.angryUntil(n) - c.world.getTotalWorldTime(), "for 400 ticks");
        c.ok(n.isAggravated() && !n.canBeLeashedTo(p), "an angry nautilus cannot be leashed");
        p.handleWaterMovement();
        c.ok(p.isInWater(), "the player is in the water");
        c.eq(p, NautilusTestAccess.findTarget(n), "it goes for the player in the water");
        p.setGameType(GameType.CREATIVE);
        c.ok(NautilusTestAccess.findTarget(n) == null || NautilusTestAccess.findTarget(n) != p, "not a creative player");
        p.setGameType(GameType.SURVIVAL);
        tame(n, p);
        c.ok(NautilusTestAccess.findTarget(n) == null, "a tamed nautilus does not attack");

        // The charge.
        EntityNautilus wild = nautilus(c, this.water.add(12, 2, 4));
        EntityPig pig = new EntityPig(c.world);
        pig.setPosition(wild.posX, wild.posY, wild.posZ + 3.0D);
        c.world.spawnEntity(pig);
        hold(p, ItemStack.EMPTY);
        wild.onUpdate();
        wild.setAttackTarget(pig);
        NautilusAi.Charge charge = new NautilusAi.Charge(wild, NautilusAi.SPEED_WHEN_ATTACKING);
        c.ok(charge.shouldExecute(), "with a target and no cooldown it charges");
        charge.startExecuting();
        charge.updateTask();
        c.ok(near(0.6D, wild.motionZ) && near(0.0D, wild.motionX), "straight at it at 0.6: " + wild.motionZ);
        float before = pig.getHealth();
        pig.setPosition(wild.posX, wild.posY, wild.posZ);
        charge.updateTask();
        c.ok(near(before - 3.0F, pig.getHealth()), "touching, 3 damage: " + (before - pig.getHealth()));
        c.ok(!charge.shouldContinueExecuting(), "one hit ends the charge");
        charge.resetTask();
        c.eq(80, NautilusTestAccess.chargeCooldown(wild), "80 ticks to the next");
        c.ok(wild.getAttackTarget() == null, "and the target is forgotten");
        wild.setAttackTarget(pig);
        c.ok(!charge.shouldExecute(), "no charge during the cooldown");
        NautilusTestAccess.setChargeCooldown(wild, 0);
        pig.setPosition(wild.posX, wild.posY, wild.posZ + 12.0D);
        charge.startExecuting();
        c.ok(!charge.shouldContinueExecuting(), "a target 12 away is too far");
        charge.resetTask();

        // Hunting: no random target until the 2-3 minute cooldown runs out.
        EntityNautilus hunter = nautilus(c, this.water.add(4, 2, 12));
        c.ok(NautilusTestAccess.attackTargetCooldown(hunter) >= 2390 && NautilusTestAccess.attackTargetCooldown(hunter) <= 3600,
                "2400-3600 ticks before it hunts: " + NautilusTestAccess.attackTargetCooldown(hunter));
        c.ok(NautilusTestAccess.findTarget(hunter) == null, "nothing to hunt (no pufferfish)");
        NautilusTestAccess.setAttackTargetCooldown(hunter, 0);
        NautilusTestAccess.findTarget(hunter);
        c.ok(NautilusTestAccess.attackTargetCooldown(hunter) >= 2400, "looking rolls a new cooldown");

        // Panic swims to spots within 5 x 4.
        c.ok(NautilusTestAccess.randomSwim(wild, 5, 4), "finds water to flee to");

        // Out of water it dries out.
        EntityNautilus dry = nautilus(c, this.water.add(0, 13, 0));
        NautilusTestAccess.handleAir(dry, 300);
        c.eq(299, dry.getAir(), "out of water it loses air");
        float hp = dry.getHealth();
        NautilusTestAccess.handleAir(dry, -19);
        c.ok(dry.getAir() == 0 && near(hp - 2.0F, dry.getHealth()), "at -20: 2 damage and air back to 0");
        NautilusTestAccess.handleAir(wild, 10);
        c.eq(300, wild.getAir(), "in water the air is full");
        // Home: a tamed one keeps near.
        tame(wild, p);
        NautilusTestAccess.checkRestriction(wild);
        c.ok(wild.hasHome() && (int) wild.getMaximumHomeDistance() == 32, "an unsaddled tamed adult keeps within 32");
        wild.setSaddle(new ItemStack(Items.SADDLE));
        NautilusTestAccess.checkRestriction(wild);
        c.ok((int) wild.getMaximumHomeDistance() == 16, "a saddled one within 16");
        remove(c, n, wild, pig, hunter, dry, p);
    }

    // ------------------------------------------------------------------ zombie nautilus

    private void zombie(Check c, BlockPos area) {
        EntityZombieNautilus z = new EntityZombieNautilus(c.world);
        z.setLocationAndAngles(this.water.getX() + 2.5D, this.water.getY() + 2, this.water.getZ() + 2.5D, 0, 0);
        z.onInitialSpawn(c.world.getDifficultyForLocation(this.water), null);
        c.world.spawnEntity(z);
        c.ok(near(1.1D, z.getEntityAttribute(SharedMonsterAttributes.MOVEMENT_SPEED).getBaseValue()), "speed 1.1");
        c.ok(near(15.0D, z.getMaxHealth()), "15 health");
        c.eq(EnumCreatureAttribute.UNDEAD, z.getCreatureAttribute(), "undead");
        z.setGrowingAge(-24000);
        c.ok(!z.isChild(), "never a baby");
        c.ok(z.createChild(z) == null, "never breeds");
        c.eq(EntityZombieNautilus.TEMPERATE, EntityZombieNautilus.variantFor("ocean"), "temperate in other oceans");
        c.eq(EntityZombieNautilus.WARM, EntityZombieNautilus.variantFor("warm_ocean"), "coral in warm oceans");
        z.setVariant(EntityZombieNautilus.WARM);
        NBTTagCompound tag = new NBTTagCompound();
        z.writeToNBT(tag);
        c.eq("minecraft:warm", tag.getString("variant"), "variant saved as 1.21.11 names it");
        EntityZombieNautilus copy = new EntityZombieNautilus(c.world);
        copy.readFromNBT(tag);
        c.eq(EntityZombieNautilus.WARM, copy.getVariant(), "variant loaded");
        // It can be tamed and ridden as a nautilus.
        EntityPlayerMP p = player(c, "naut_zombie");
        int tries = 0;
        while (!z.isTamed() && tries < 60) {
            hold(p, new ItemStack(Items.FISH, 1, ItemFishFood.FishType.PUFFERFISH.getMetadata()));
            z.processInteract(p, EnumHand.MAIN_HAND);
            tries++;
        }
        c.ok(z.isTamed(), "a zombie nautilus is tamed with pufferfish too");
        hold(p, new ItemStack(Items.SADDLE));
        z.processInteract(p, EnumHand.MAIN_HAND);
        c.ok(z.isSaddled(), "and saddled");
        // Daylight: it burns dry under the sky, not with armor.
        EntityZombieNautilus sun = new EntityZombieNautilus(c.world);
        BlockPos sky = area.add(16, 20, 16);
        sun.setLocationAndAngles(sky.getX() + 0.5D, sky.getY(), sky.getZ() + 0.5D, 0, 0);
        c.world.spawnEntity(sun);
        float rain = c.world.getRainStrength(1.0F);
        boolean raining = c.world.getWorldInfo().isRaining();
        c.world.getWorldInfo().setRaining(false);
        setRain(c, 0.0F);
        c.world.setWorldTime(6000);
        c.world.calculateInitialSkylight();
        boolean burned = false;
        for (int i = 0; i < 3000 && !burned; i++) {
            NautilusTestAccess.burnUndead(sun);
            burned = sun.isBurning();
        }
        c.ok(burned, "it burns in daylight");
        sun.extinguish();
        sun.setBodyArmor(new ItemStack(NautilusContent.ARMOR.get(NautilusArmorTier.COPPER)));
        tame(sun, p);
        sun.setBodyArmor(new ItemStack(NautilusContent.ARMOR.get(NautilusArmorTier.COPPER)));
        burned = false;
        for (int i = 0; i < 3000 && !burned; i++) {
            NautilusTestAccess.burnUndead(sun);
            burned = sun.isBurning();
        }
        c.ok(!burned, "not with armor");
        c.world.setWorldTime(18000);
        c.world.calculateInitialSkylight();
        EntityZombieNautilus night = new EntityZombieNautilus(c.world);
        night.setLocationAndAngles(sky.getX() + 2.5D, sky.getY(), sky.getZ() + 0.5D, 0, 0);
        c.world.spawnEntity(night);
        burned = false;
        for (int i = 0; i < 3000 && !burned; i++) {
            NautilusTestAccess.burnUndead(night);
            burned = night.isBurning();
        }
        c.ok(!burned, "nor at night");
        c.world.getWorldInfo().setRaining(raining);
        setRain(c, rain);
        // A rider's target steers it.
        EntityZombie rider = new EntityZombie(c.world);
        rider.setPosition(this.water.getX() + 5, this.water.getY() + 2, this.water.getZ() + 5);
        EntityZombieNautilus mount = NautilusContent.mount(rider);
        c.ok(rider.getRidingEntity() == mount && mount.isMobControlled(), "a zombie rides a fresh zombie nautilus");
        remove(c, z, sun, night, rider, mount, p);
    }

    // ------------------------------------------------------------------ loot

    private void loot(Check c) {
        // Zombie nautilus: 0-3 rotten flesh for a player kill.
        EntityPlayerMP p = player(c, "naut_loot");
        LootTable zombie = c.world.getLootTableManager().getLootTableFromLocation(EntityZombieNautilus.LOOT);
        EntityZombieNautilus z = new EntityZombieNautilus(c.world);
        int min = 99;
        int max = -1;
        boolean other = false;
        Random r = new Random(5);
        for (int i = 0; i < 200; i++) {
            LootContext ctx = new LootContext.Builder(c.world).withLootedEntity(z).withPlayer(p).withDamageSource(DamageSource.causePlayerDamage(p)).build();
            int n = 0;
            for (ItemStack s : zombie.generateLootForPools(r, ctx)) {
                if (s.getItem() == Items.ROTTEN_FLESH) {
                    n += s.getCount();
                } else {
                    other = true;
                }
            }
            min = Math.min(min, n);
            max = Math.max(max, n);
        }
        c.ok(!other && min == 0 && max == 3, "0-3 rotten flesh and nothing else: " + min + "-" + max);
        LootContext noPlayer = new LootContext.Builder(c.world).withLootedEntity(z).build();
        c.ok(zombie.generateLootForPools(r, noPlayer).isEmpty(), "nothing without a player kill");

        // Nautilus: the shell pool only with Update Aquatic's shell.
        LootTable bare = new LootTable(new LootPool[0]);
        NautilusContent.onLootLoad(new LootTableLoadEvent(EntityNautilus.LOOT, bare, c.world.getLootTableManager()));
        c.ok(bare.getPool("nautilus_shell") == null, "no shell pool without Update Aquatic");
        NautilusItems.uaItem = UA::get;
        LootTable withShell = new LootTable(new LootPool[0]);
        NautilusContent.onLootLoad(new LootTableLoadEvent(EntityNautilus.LOOT, withShell, c.world.getLootTableManager()));
        c.ok(withShell.getPool("nautilus_shell") != null, "a shell pool with it");
        EntityNautilus n = new EntityNautilus(c.world);
        int shells = 0;
        for (int i = 0; i < 4000; i++) {
            LootContext ctx = new LootContext.Builder(c.world).withLootedEntity(n).withPlayer(p).withDamageSource(DamageSource.causePlayerDamage(p)).build();
            for (ItemStack s : withShell.generateLootForPools(r, ctx)) {
                if (s.getItem() == Items.PRISMARINE_SHARD) {
                    shells++;
                }
            }
        }
        c.ok(shells > 140 && shells < 270, "about 5% drop a shell: " + shells + " of 4000");
        c.ok(withShell.generateLootForPools(r, new LootContext.Builder(c.world).withLootedEntity(n).build()).isEmpty(), "no shell without a player kill");

        // Update Aquatic's six ocean chests get the armor pool; others do not.
        int added = 0;
        for (String chest : NautilusContent.ARMOR_CHESTS) {
            LootTable t = new LootTable(new LootPool[0]);
            NautilusContent.onLootLoad(new LootTableLoadEvent(new ResourceLocation("updateaquaticbackport", "chests/" + chest), t,
                    c.world.getLootTableManager()));
            if (t.getPool("nautilus_armor") != null) {
                added++;
            }
        }
        c.eq(6, added, "six of Update Aquatic's chests");
        LootTable dungeon = new LootTable(new LootPool[0]);
        NautilusContent.onLootLoad(new LootTableLoadEvent(new ResourceLocation("minecraft", "chests/simple_dungeon"), dungeon,
                c.world.getLootTableManager()));
        c.ok(dungeon.getPool("nautilus_armor") == null, "not in a dungeon");
        LootTable ship = new LootTable(new LootPool[0]);
        NautilusContent.onLootLoad(new LootTableLoadEvent(new ResourceLocation("updateaquaticbackport", "chests/shipwreck_supply"), ship,
                c.world.getLootTableManager()));
        Map<Item, Integer> counts = new HashMap<Item, Integer>();
        int rolls = 18500;
        for (int i = 0; i < rolls; i++) {
            for (ItemStack s : ship.generateLootForPools(r, new LootContext.Builder(c.world).build())) {
                Integer k = counts.get(s.getItem());
                counts.put(s.getItem(), k == null ? 1 : k + 1);
            }
        }
        int copper = count(counts, NautilusArmorTier.COPPER);
        int iron = count(counts, NautilusArmorTier.IRON);
        int gold = count(counts, NautilusArmorTier.GOLDEN);
        int diamond = count(counts, NautilusArmorTier.DIAMOND);
        c.ok(Math.abs(copper - 2000) < 300 && Math.abs(iron - 1000) < 200 && Math.abs(gold - 500) < 130 && Math.abs(diamond - 200) < 80,
                "weights 20/10/5/2 of 185: " + copper + "/" + iron + "/" + gold + "/" + diamond);
        c.eq(0, count(counts, NautilusArmorTier.NETHERITE), "never netherite");
        NautilusItems.uaItem = NautilusItems::updateAquatic;
        remove(c, p);
    }

    /** World.setRainStrength is client-only in 1.12.2. */
    private static void setRain(Check c, float strength) {
        net.minecraftforge.fml.relauncher.ReflectionHelper.setPrivateValue(net.minecraft.world.World.class, c.world, strength,
                "rainingStrength", "field_73004_o");
        net.minecraftforge.fml.relauncher.ReflectionHelper.setPrivateValue(net.minecraft.world.World.class, c.world, strength,
                "prevRainingStrength", "field_73003_n");
    }

    private static int count(Map<Item, Integer> counts, NautilusArmorTier tier) {
        Integer n = counts.get(NautilusContent.ARMOR.get(tier));
        return n == null ? 0 : n;
    }
}
