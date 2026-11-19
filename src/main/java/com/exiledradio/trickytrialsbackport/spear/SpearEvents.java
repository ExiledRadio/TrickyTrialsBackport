package com.exiledradio.trickytrialsbackport.spear;

import com.exiledradio.trickytrialsbackport.ModConfig;
import com.exiledradio.trickytrialsbackport.TrickyTrialsBackport;

import net.minecraft.enchantment.Enchantment;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.entity.EntityLiving;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.monster.EntityPigZombie;
import net.minecraft.entity.monster.EntityZombie;
import net.minecraft.init.Items;
import net.minecraft.inventory.EntityEquipmentSlot;
import net.minecraft.item.ItemStack;
import net.minecraft.world.DifficultyInstance;
import net.minecraftforge.event.AnvilUpdateEvent;
import net.minecraftforge.event.entity.EntityJoinWorldEvent;
import net.minecraftforge.event.entity.living.LivingEntityUseItemEvent;
import net.minecraftforge.event.entity.living.LivingEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.EventPriority;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * The spear's server-side rules outside the item.
 *
 * <ul>
 *   <li><b>Speed</b>: every living entity's move is sampled at the start of its update ({@link SpearAttacks#sample}).</li>
 *   <li><b>A new charge</b> forgets the last one's targets.</li>
 *   <li><b>Mobs with spears</b> (patch P20, {@link #spawnWeapon}): 1.21.11's zombie rolls an iron sword, an iron spear
 *       or an iron shovel (1 in 6, 1 in 6, 4 in 6) where 1.12.2 rolls a sword or a shovel (1 in 3, 2 in 3), so half
 *       of 1.12.2's swords become spears; a zombie pigman holds a golden spear 1 time in 20. Husks and zombie
 *       villagers roll as zombies. Zombies and zombie pigmen holding a spear charge with it ({@link EntityAISpearUse}).</li>
 *   <li><b>Anvil</b>: 1.12.2's anvil asks each enchantment's type, and the melee enchantments fit swords only, so a
 *       spear with a book or a second spear is worked out here by vanilla's rules.</li>
 * </ul>
 */
@Mod.EventBusSubscriber(modid = TrickyTrialsBackport.MOD_ID)
public final class SpearEvents {

    private SpearEvents() {
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onLivingUpdate(LivingEvent.LivingUpdateEvent event) {
        EntityLivingBase e = event.getEntityLiving();
        if (!e.world.isRemote && SpearContent.enabled()) {
            SpearAttacks.sample(e);
        }
    }

    /** A spear never mines (1.21.11's {@code Minecraft.continueAttack} skips spears), not even in creative. */
    @SubscribeEvent
    public static void onLeftClickBlock(PlayerInteractEvent.LeftClickBlock event) {
        if (SpearContent.enabled() && event.getItemStack().getItem() instanceof ItemSpear) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public static void onUseStart(LivingEntityUseItemEvent.Start event) {
        if (event.getItem().getItem() instanceof ItemSpear) {
            SpearAttacks.startCharge(event.getEntityLiving());
        }
    }

    @SubscribeEvent
    public static void onJoin(EntityJoinWorldEvent event) {
        if (event.getWorld().isRemote || !SpearContent.enabled() || !ModConfig.spears.mobCharges) {
            return;
        }
        if (event.getEntity() instanceof EntityZombie) {
            EntityZombie zombie = (EntityZombie) event.getEntity();
            // Above 1.12.2's melee (2), as 1.21.11's spear goal sits above its zombie attack goal.
            zombie.tasks.addTask(1, new EntityAISpearUse(zombie, 1.0, 1.0, 10.0F, 2.0F));
        }
    }

    /**
     * Patch P20: the end of {@code EntityZombie.setEquipmentBasedOnDifficulty} and
     * {@code EntityPigZombie.setEquipmentBasedOnDifficulty}.
     */
    public static void spawnWeapon(EntityLiving mob, DifficultyInstance difficulty) {
        if (!SpearContent.enabled() || !ModConfig.spears.mobSpears) {
            return;
        }
        ItemStack held = mob.getHeldItemMainhand();
        if (mob instanceof EntityPigZombie) {
            if (held.getItem() == Items.GOLDEN_SWORD && mob.getRNG().nextInt(20) == 0) {
                mob.setItemStackToSlot(EntityEquipmentSlot.MAINHAND, new ItemStack(SpearContent.spear(SpearTier.GOLD)));
            }
        } else if (held.getItem() == Items.IRON_SWORD && mob.getRNG().nextBoolean()) {
            mob.setItemStackToSlot(EntityEquipmentSlot.MAINHAND, new ItemStack(SpearContent.spear(SpearTier.IRON)));
        }
    }

    // ------------------------------------------------------------------------------------------
    // Anvil
    // ------------------------------------------------------------------------------------------

    @SubscribeEvent
    public static void onAnvil(AnvilUpdateEvent event) {
        ItemStack left = event.getLeft();
        ItemStack right = event.getRight();
        if (!(left.getItem() instanceof ItemSpear) || right.isEmpty()
                || !(right.getItem() == Items.ENCHANTED_BOOK || right.getItem() == left.getItem())) {
            return;
        }
        boolean book = right.getItem() == Items.ENCHANTED_BOOK;
        Map<Enchantment, Integer> result = new LinkedHashMap<Enchantment, Integer>(EnchantmentHelper.getEnchantments(left));
        int cost = 0;
        boolean any = false;
        for (Map.Entry<Enchantment, Integer> e : EnchantmentHelper.getEnchantments(right).entrySet()) {
            Enchantment ench = e.getKey();
            if (!SpearContent.takes(ench)) {
                continue;
            }
            boolean fits = true;
            for (Enchantment had : result.keySet()) {
                if (had != ench && !had.isCompatibleWith(ench)) {
                    fits = false;
                }
            }
            if (!fits) {
                cost++;
                continue;
            }
            int had = result.containsKey(ench) ? result.get(ench) : 0;
            int level = e.getValue();
            level = had == level ? level + 1 : Math.max(level, had);
            level = Math.min(level, ench.getMaxLevel());
            result.put(ench, level);
            any = true;
            int multiplier = rarityCost(ench.getRarity());
            if (book) {
                multiplier = Math.max(1, multiplier / 2);
            }
            cost += multiplier * level;
        }
        if (!any) {
            return;
        }
        ItemStack out = left.copy();
        int repair = 0;
        if (!book && left.isItemDamaged()) {
            // Two spears combine durability, as two tools do: theirs plus 12% of the full bar.
            int sum = (left.getMaxDamage() - left.getItemDamage()) + (right.getMaxDamage() - right.getItemDamage())
                    + left.getMaxDamage() * 12 / 100;
            out.setItemDamage(Math.max(0, left.getMaxDamage() - sum));
            repair = 2;
        }
        EnchantmentHelper.setEnchantments(result, out);
        int prior = left.getRepairCost() + right.getRepairCost();
        out.setRepairCost(Math.max(left.getRepairCost(), right.getRepairCost()) * 2 + 1);
        if (event.getName() != null && !event.getName().isEmpty() && !event.getName().equals(left.getDisplayName())) {
            out.setStackDisplayName(event.getName());
            cost++;
        }
        event.setOutput(out);
        event.setCost(prior + cost + repair);
        event.setMaterialCost(1);
    }

    /** 1.12.2's anvil multipliers: common 1, uncommon 2, rare 4, very rare 8. */
    static int rarityCost(Enchantment.Rarity rarity) {
        switch (rarity) {
            case COMMON:
                return 1;
            case UNCOMMON:
                return 2;
            case RARE:
                return 4;
            default:
                return 8;
        }
    }
}
