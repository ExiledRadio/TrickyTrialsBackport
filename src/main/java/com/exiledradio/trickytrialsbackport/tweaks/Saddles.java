package com.exiledradio.trickytrialsbackport.tweaks;

import com.exiledradio.trickytrialsbackport.ModConfig;
import com.exiledradio.trickytrialsbackport.TrickyTrialsBackport;
import com.exiledradio.trickytrialsbackport.common.Content;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLiving;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.passive.AbstractChestHorse;
import net.minecraft.entity.passive.AbstractHorse;
import net.minecraft.entity.passive.EntityHorse;
import net.minecraft.entity.passive.EntityLlama;
import net.minecraft.entity.passive.EntityPig;
import net.minecraft.entity.passive.EntitySkeletonHorse;
import net.minecraft.entity.passive.EntityZombieHorse;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Enchantments;
import net.minecraft.init.Items;
import net.minecraft.item.ItemStack;
import net.minecraft.item.crafting.IRecipe;
import net.minecraft.util.EnumActionResult;
import net.minecraft.util.EnumHand;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.SoundEvent;
import net.minecraft.world.storage.loot.LootEntry;
import net.minecraft.world.storage.loot.LootEntryItem;
import net.minecraft.world.storage.loot.LootPool;
import net.minecraft.world.storage.loot.RandomValueRange;
import net.minecraft.world.storage.loot.conditions.LootCondition;
import net.minecraft.world.storage.loot.functions.LootFunction;
import net.minecraft.world.storage.loot.functions.LootingEnchantBonus;
import net.minecraft.world.storage.loot.functions.SetCount;
import net.minecraftforge.event.LootTableLoadEvent;
import net.minecraftforge.event.RegistryEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.items.CapabilityItemHandler;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.oredict.ShapedOreRecipe;
import net.minecraftforge.registries.IForgeRegistryModifiable;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

/**
 * 1.21.6's saddle and horse changes, the 1.21.5 saddle Thorns, and the witch's redstone of 1.21:
 * <ul>
 *   <li>the saddle recipe (3 leather around an iron ingot) and the lead's (5 string, no slimeball);</li>
 *   <li>1-5 leather where dungeon, desert and jungle temple and stronghold corridor chests had a saddle;</li>
 *   <li>shears take off a mob's horse armor or carpet, else its saddle, when nobody rides it and the player is not
 *       sneaking (leads first: see {@link Leads});</li>
 *   <li>horses, donkeys and mules eat carrots exactly as apples;</li>
 *   <li>Thorns on a horse's saddle hurts its attackers;</li>
 *   <li>witches always drop 4-8 redstone in a pool of its own (Looting adds 0-1), no longer one of their 1-3 rolls.</li>
 * </ul>
 */
@Mod.EventBusSubscriber(modid = TrickyTrialsBackport.MOD_ID)
public final class Saddles {

    /** The chests whose saddle 1.21.6 turned into 1-5 leather. */
    static final Set<ResourceLocation> SADDLE_CHESTS = new HashSet<ResourceLocation>(Arrays.asList(
            new ResourceLocation("chests/simple_dungeon"), new ResourceLocation("chests/desert_pyramid"),
            new ResourceLocation("chests/jungle_temple"), new ResourceLocation("chests/stronghold_corridor")));

    static final ResourceLocation WITCH = new ResourceLocation("entities/witch");

    private static final Method EATING = Tweaks.method(AbstractHorse.class, "eatingHorse", "func_110266_cB");

    private Saddles() {
    }

    @SubscribeEvent
    public static void onRecipes(RegistryEvent.Register<IRecipe> event) {
        if (!ModConfig.tweaks.enabled) {
            return;
        }
        if (ModConfig.tweaks.saddleRecipe) {
            event.getRegistry().register(new ShapedOreRecipe(null, new ItemStack(Items.SADDLE),
                    " X ", "X#X", 'X', Items.LEATHER, '#', Items.IRON_INGOT).setRegistryName(Content.id("saddle")));
        }
        if (ModConfig.tweaks.leadRecipe) {
            if (event.getRegistry() instanceof IForgeRegistryModifiable) {
                ((IForgeRegistryModifiable<IRecipe>) event.getRegistry()).remove(new ResourceLocation("lead"));
            }
            event.getRegistry().register(new ShapedOreRecipe(null, new ItemStack(Items.LEAD, 2),
                    "~~ ", "~~ ", "  ~", '~', Items.STRING).setRegistryName(Content.id("lead")));
        }
    }

    @SubscribeEvent
    public static void onLootLoad(LootTableLoadEvent event) {
        if (!ModConfig.tweaks.enabled) {
            return;
        }
        if (ModConfig.tweaks.saddleLoot && SADDLE_CHESTS.contains(event.getName())) {
            for (String name : new String[] {"main", "pool0", "pool1", "pool2"}) {
                LootPool pool = event.getTable().getPool(name);
                if (pool == null) {
                    continue;
                }
                LootEntry saddle = pool.getEntry("minecraft:saddle");
                if (saddle != null) {
                    pool.removeEntry("minecraft:saddle");
                    pool.addEntry(new LootEntryItem(Items.LEATHER, saddle.getEffectiveWeight(0.0F), 0,
                            new LootFunction[] {new SetCount(new LootCondition[0], new RandomValueRange(1.0F, 5.0F))},
                            new LootCondition[0], "minecraft:leather"));
                }
            }
        }
        if (ModConfig.tweaks.witchRedstone && WITCH.equals(event.getName())) {
            LootPool main = event.getTable().getPool("main");
            if (main != null && main.getEntry("minecraft:redstone") != null) {
                main.removeEntry("minecraft:redstone");
            }
            event.getTable().addPool(new LootPool(new LootEntry[] {
                    new LootEntryItem(Items.REDSTONE, 1, 0, new LootFunction[] {
                            new SetCount(new LootCondition[0], new RandomValueRange(4.0F, 8.0F)),
                            new LootingEnchantBonus(new LootCondition[0], new RandomValueRange(0.0F, 1.0F), 0)},
                            new LootCondition[0], "minecraft:redstone")},
                    new LootCondition[0], new RandomValueRange(1.0F), new RandomValueRange(0.0F),
                    TrickyTrialsBackport.MOD_ID + ":redstone"));
        }
    }

    // ------------------------------------------------------------------------------------------
    // Shears and carrots
    // ------------------------------------------------------------------------------------------

    @SubscribeEvent
    public static void onInteract(PlayerInteractEvent.EntityInteract event) {
        if (!ModConfig.tweaks.enabled || event.isCanceled()) {
            return;
        }
        EntityPlayer player = event.getEntityPlayer();
        ItemStack held = event.getItemStack();
        Entity target = event.getTarget();
        if (held.getItem() == Items.SHEARS && ModConfig.tweaks.shearsRemoveEquipment && target instanceof EntityLiving) {
            if (Leads.snip((EntityLiving) target, player, held, event.getHand())) {
                event.setCanceled(true);
                event.setCancellationResult(EnumActionResult.SUCCESS);
                return;
            }
            if (!target.isBeingRidden() && !player.isSneaking() && shearEquipment((EntityLiving) target, player, held, event.getHand())) {
                event.setCanceled(true);
                event.setCancellationResult(EnumActionResult.SUCCESS);
            }
            return;
        }
        if (held.getItem() == Items.CARROT && ModConfig.tweaks.horsesEatCarrots && eatsCarrots(target)) {
            AbstractHorse horse = (AbstractHorse) target;
            if (!horse.isChild() && (horse.isTame() && player.isSneaking() || horse.isBeingRidden())) {
                return;
            }
            if (horse.world.isRemote) {
                event.setCanceled(true);
                event.setCancellationResult(EnumActionResult.SUCCESS);
            } else if (eatCarrot(horse)) {
                if (!player.capabilities.isCreativeMode) {
                    held.shrink(1);
                }
                event.setCanceled(true);
                event.setCancellationResult(EnumActionResult.SUCCESS);
            }
        }
    }

    static boolean eatsCarrots(Entity e) {
        if (e instanceof EntityHorse || e instanceof AbstractChestHorse && !(e instanceof EntityLlama)) {
            return true;
        }
        return (e instanceof EntitySkeletonHorse || e instanceof EntityZombieHorse) && ((AbstractHorse) e).isTame();
    }

    /** 1.21.6's {@code AbstractHorse.handleEating} for a carrot: an apple's 3 health, 60 ticks of growth, 3 temper. */
    public static boolean eatCarrot(AbstractHorse horse) {
        boolean ate = false;
        if (horse.getHealth() < horse.getMaxHealth()) {
            horse.heal(3.0F);
            ate = true;
        }
        if (horse.isChild()) {
            horse.addGrowth(60);
            ate = true;
        }
        if ((ate || !horse.isTame()) && horse.getTemper() < horse.getMaxTemper()) {
            horse.increaseTemper(3);
            ate = true;
        }
        if (ate) {
            Tweaks.call(EATING, horse);
        }
        return ate;
    }

    /**
     * 1.21.6's {@code Entity.attemptToShearEquipment}: the first shearable piece in slot order (body armor or carpet
     * before the saddle) drops where a rider would sit.
     */
    public static boolean shearEquipment(EntityLiving mob, EntityPlayer player, ItemStack shears, EnumHand hand) {
        ItemStack taken = ItemStack.EMPTY;
        SoundEvent sound = null;
        if (mob instanceof AbstractHorse && mob.world.isRemote) {
            // The client has no copy of the horse's inventory, only what it shows.
            AbstractHorse horse = (AbstractHorse) mob;
            boolean body = mob instanceof EntityLlama ? ((EntityLlama) mob).getColor() != null
                    : mob instanceof EntityHorse && ((EntityHorse) mob).getHorseArmorType() != net.minecraft.entity.passive.HorseArmorType.NONE;
            return body || !(mob instanceof EntityLlama) && horse.isHorseSaddled();
        }
        if (mob instanceof AbstractHorse) {
            IItemHandler inv = mob.getCapability(CapabilityItemHandler.ITEM_HANDLER_CAPABILITY, null);
            if (inv == null) {
                return false;
            }
            if (inv.getSlots() > 1 && !inv.getStackInSlot(1).isEmpty()
                    && (mob instanceof EntityLlama || mob instanceof EntityHorse)) {
                sound = Content.sound(mob instanceof EntityLlama ? "item.llama_carpet.unequip" : "item.horse_armor.unequip");
                taken = inv.extractItem(1, 64, false);
            } else if (!(mob instanceof EntityLlama) && inv.getSlots() > 0 && inv.getStackInSlot(0).getItem() == Items.SADDLE) {
                sound = Content.sound("item.saddle.unequip");
                taken = inv.extractItem(0, 64, false);
            }
        } else if (mob instanceof EntityPig && ((EntityPig) mob).getSaddled()) {
            sound = Content.sound("item.saddle.unequip");
            taken = new ItemStack(Items.SADDLE);
            if (!mob.world.isRemote) {
                ((EntityPig) mob).setSaddled(false);
            }
        }
        if (taken.isEmpty()) {
            return false;
        }
        if (!mob.world.isRemote) {
            shears.damageItem(1, player);
            mob.world.playSound(null, mob.posX, mob.posY, mob.posZ, sound, mob.getSoundCategory(), 1.0F, 1.0F);
            mob.entityDropItem(taken.copy(), (float) mob.getMountedYOffset());
        }
        return true;
    }

    /** Thorns on a horse's saddle (1.21.5): the saddle slot is equipment, so its Thorns answers attackers. */
    @SubscribeEvent
    public static void onHurt(LivingHurtEvent event) {
        EntityLivingBase mob = event.getEntityLiving();
        if (!(mob instanceof AbstractHorse) || mob.world.isRemote || !Tweaks.on(ModConfig.tweaks.saddleThorns)) {
            return;
        }
        Entity attacker = event.getSource().getTrueSource();
        if (!(attacker instanceof EntityLivingBase) || event.getSource().getDamageType().equals("thorns")) {
            return;
        }
        IItemHandler inv = mob.getCapability(CapabilityItemHandler.ITEM_HANDLER_CAPABILITY, null);
        ItemStack saddle = inv == null || inv.getSlots() == 0 ? ItemStack.EMPTY : inv.getStackInSlot(0);
        int level = EnchantmentHelper.getEnchantmentLevel(Enchantments.THORNS, saddle);
        if (level > 0) {
            Enchantments.THORNS.onUserHurt(mob, attacker, level);
        }
    }
}
