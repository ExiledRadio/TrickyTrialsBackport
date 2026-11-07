package com.exiledradio.trickytrialsbackport.mace;

import com.exiledradio.trickytrialsbackport.TrickyTrialsBackport;
import com.exiledradio.trickytrialsbackport.common.Content;
import com.exiledradio.trickytrialsbackport.common.ValueTrigger;

import net.minecraft.enchantment.Enchantment;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.SharedMonsterAttributes;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.init.Enchantments;
import net.minecraft.init.Items;
import net.minecraft.item.ItemStack;
import net.minecraft.util.CombatRules;
import net.minecraftforge.event.AnvilUpdateEvent;
import net.minecraftforge.event.entity.living.LivingDamageEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.EventPriority;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.WeakHashMap;

/**
 * The mace's damage rules and its anvil.
 *
 * <ul>
 *   <li><b>Smash and Density</b> ({@link #onHurt}): a melee hit with a mace by an attacker who has fallen
 *       more than 1.5 blocks gains {@link ItemMace#smashBonus} plus 0.5 per block per Density level, added
 *       after vanilla's critical hit, as 1.21 adds it.</li>
 *   <li><b>Breach</b>: armor protects 15% less per level. The hit is raised so that vanilla's own armor
 *       step leaves what 1.21's weaker armor would; everything after it (Resistance, Protection,
 *       absorption) works on that as usual.</li>
 *   <li><b>Over-Overkill</b> ({@link #onDamage}): a smash dealing 100 or more fires
 *       {@code trickytrialsbackport:mace_smash}.</li>
 *   <li><b>Anvil</b> ({@link #onAnvil}): 1.12.2's anvil asks each enchantment whether it fits, and Smite,
 *       Bane of Arthropods and Fire Aspect say no to anything not a sword. For a mace with an enchanted book
 *       or another mace, this works the result out by vanilla's rules instead.</li>
 * </ul>
 */
@Mod.EventBusSubscriber(modid = TrickyTrialsBackport.MOD_ID)
public final class MaceEvents {

    /** {@code trickytrialsbackport:mace_smash}: a smash attack's damage, for <i>Over-Overkill</i>. */
    public static final ValueTrigger MACE_SMASH = new ValueTrigger(Content.id("mace_smash"), "min_damage");

    /** Targets hit by a smash this tick, so the damage event knows it was one. */
    private static final Map<EntityLivingBase, EntityPlayerMP> SMASHED = new WeakHashMap<EntityLivingBase, EntityPlayerMP>();

    private MaceEvents() {
    }

    /** The enchantments 1.21 puts on a mace beyond those 1.12.2's types already allow. */
    static boolean acceptsOnMace(Enchantment e) {
        return e instanceof EnchantmentMace || e == Enchantments.SMITE || e == Enchantments.BANE_OF_ARTHROPODS || e == Enchantments.FIRE_ASPECT;
    }

    @SubscribeEvent(priority = EventPriority.HIGH)
    public static void onHurt(LivingHurtEvent event) {
        Entity direct = event.getSource().getImmediateSource();
        if (!(direct instanceof EntityLivingBase) || direct != event.getSource().getTrueSource() || direct.world.isRemote) {
            return;
        }
        EntityLivingBase attacker = (EntityLivingBase) direct;
        ItemStack mace = attacker.getHeldItemMainhand();
        if (!(mace.getItem() instanceof ItemMace) || event.getSource().isProjectile() || event.getSource().isExplosion()) {
            return;
        }
        float amount = event.getAmount();
        if (ItemMace.canSmash(attacker)) {
            float fall = attacker.fallDistance;
            int density = MaceContent.density == null ? 0 : EnchantmentHelper.getEnchantmentLevel(MaceContent.density, mace);
            amount += ItemMace.smashBonus(fall) + 0.5F * density * fall;
            if (attacker instanceof EntityPlayerMP) {
                synchronized (SMASHED) {
                    SMASHED.put(event.getEntityLiving(), (EntityPlayerMP) attacker);
                }
            }
        }
        int breach = MaceContent.breach == null ? 0 : EnchantmentHelper.getEnchantmentLevel(MaceContent.breach, mace);
        if (breach > 0 && !event.getSource().isUnblockable()) {
            amount = breached(event.getEntityLiving(), amount, breach);
        }
        event.setAmount(amount);
    }

    /**
     * @return the hit that, after vanilla's armor step, leaves what armor working at (1 - 0.15 x level) of its
     * strength would have left of {@code amount}
     */
    static float breached(EntityLivingBase target, float amount, int level) {
        float armor = target.getTotalArmorValue();
        float toughness = (float) target.getEntityAttribute(SharedMonsterAttributes.ARMOR_TOUGHNESS).getAttributeValue();
        if (armor <= 0.0F || amount <= 0.0F) {
            return amount;
        }
        float effectiveness = Math.max(0.0F, 1.0F - 0.15F * level);
        float afterArmor = CombatRules.getDamageAfterAbsorb(amount, armor, toughness);
        float want = amount - (amount - afterArmor) * effectiveness;
        // Vanilla's step rises with the hit, so the hit that gives `want` is found by halving.
        float lo = amount;
        float hi = amount * 8.0F;
        for (int i = 0; i < 40; i++) {
            float mid = (lo + hi) / 2.0F;
            if (CombatRules.getDamageAfterAbsorb(mid, armor, toughness) < want) {
                lo = mid;
            } else {
                hi = mid;
            }
        }
        return hi;
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onDamage(LivingDamageEvent event) {
        EntityPlayerMP player;
        synchronized (SMASHED) {
            player = SMASHED.remove(event.getEntityLiving());
        }
        if (player != null && !event.isCanceled()) {
            MACE_SMASH.trigger(player, event.getAmount());
        }
    }

    // ------------------------------------------------------------------------------------------
    // Anvil
    // ------------------------------------------------------------------------------------------

    @SubscribeEvent
    public static void onAnvil(AnvilUpdateEvent event) {
        ItemStack left = event.getLeft();
        ItemStack right = event.getRight();
        if (!(left.getItem() instanceof ItemMace) || right.isEmpty()
                || !(right.getItem() == Items.ENCHANTED_BOOK || right.getItem() instanceof ItemMace)) {
            return;
        }
        boolean book = right.getItem() == Items.ENCHANTED_BOOK;
        Map<Enchantment, Integer> result = new LinkedHashMap<Enchantment, Integer>(EnchantmentHelper.getEnchantments(left));
        Map<Enchantment, Integer> adding = EnchantmentHelper.getEnchantments(right);
        int cost = 0;
        boolean any = false;
        for (Map.Entry<Enchantment, Integer> e : adding.entrySet()) {
            Enchantment ench = e.getKey();
            if (!ItemMace.takes(ench)) {
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
            // Two maces also combine durability, as two tools do: theirs plus 12% of the full bar.
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
