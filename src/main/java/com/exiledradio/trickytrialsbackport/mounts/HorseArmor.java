package com.exiledradio.trickytrialsbackport.mounts;

import com.exiledradio.trickytrialsbackport.TrickyTrialsBackport;

import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.SharedMonsterAttributes;
import net.minecraft.entity.ai.attributes.AttributeModifier;
import net.minecraft.entity.ai.attributes.IAttribute;
import net.minecraft.entity.ai.attributes.IAttributeInstance;
import net.minecraft.entity.passive.EntityHorse;
import net.minecraft.entity.passive.EntityZombieHorse;
import net.minecraft.entity.passive.HorseArmorType;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraftforge.common.util.EnumHelper;
import net.minecraftforge.event.entity.living.LivingEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

import java.util.UUID;

/**
 * Netherite horse armor (1.21.11) and the attributes horse armor gives beyond 1.12.2's armor points.
 *
 * <p>1.21.11's {@code ArmorMaterials.NETHERITE} for the body: 19 armor, 3 toughness, 0.1 knockback resistance (the
 * tooltip's "+1"). 1.12.2's horse armor is a Forge {@link HorseArmorType} whose protection the horse applies itself; the
 * toughness and knockback resistance are added here, to horses and (stream 24) zombie horses wearing it. Made from
 * diamond horse armor and a netherite ingot through Backport Core's {@code SmithingUpgrades}, so only when a netherite
 * mod is installed.
 */
@Mod.EventBusSubscriber(modid = TrickyTrialsBackport.MOD_ID)
public final class HorseArmor {

    public static final int NETHERITE_ARMOR = 19;
    public static final double NETHERITE_TOUGHNESS = 3.0D;
    public static final double NETHERITE_KNOCKBACK = 0.1D;

    static final UUID ARMOR = UUID.fromString("5f1e0b9c-6d3a-4f2e-9a71-2b4c8d0e7a11");
    static final UUID TOUGHNESS = UUID.fromString("5f1e0b9c-6d3a-4f2e-9a71-2b4c8d0e7a12");
    static final UUID KNOCKBACK = UUID.fromString("5f1e0b9c-6d3a-4f2e-9a71-2b4c8d0e7a13");

    public static HorseArmorType netheriteType;

    private HorseArmor() {
    }

    static Item create() {
        netheriteType = EnumHelper.addHorseArmor("TRICKYTRIALSBACKPORT_NETHERITE",
                TrickyTrialsBackport.MOD_ID + ":textures/entity/horse/armor/horse_armor_netherite.png", NETHERITE_ARMOR);
        return new ItemNetheriteHorseArmor();
    }

    /** Netherite horse armor: protection 19, one to a stack. */
    public static final class ItemNetheriteHorseArmor extends Item {
        ItemNetheriteHorseArmor() {
            setMaxStackSize(1);
            setCreativeTab(CreativeTabs.MISC);
        }

        @Override
        public HorseArmorType getHorseArmorType(ItemStack stack) {
            return netheriteType;
        }
    }

    /** A horse wearing netherite gets its toughness and knockback resistance; anything else has them taken off. */
    @SubscribeEvent
    public static void onUpdate(LivingEvent.LivingUpdateEvent event) {
        if (event.getEntityLiving() instanceof EntityHorse && !event.getEntityLiving().world.isRemote && netheriteType != null) {
            EntityHorse horse = (EntityHorse) event.getEntityLiving();
            extras(horse, horse.getHorseArmorType() == netheriteType);
        }
    }

    /** The zombie horse's armor, as {@link EntityHorse#setHorseArmorStack} gives a horse's, with netherite's extras. */
    static void apply(EntityZombieHorse horse, ItemStack armor) {
        HorseArmorType type = HorseArmorType.getByItemStack(armor);
        set(horse, SharedMonsterAttributes.ARMOR, ARMOR, "Horse armor bonus", type.getProtection());
        extras(horse, netheriteType != null && type == netheriteType);
    }

    static void extras(EntityLivingBase horse, boolean netherite) {
        set(horse, SharedMonsterAttributes.ARMOR_TOUGHNESS, TOUGHNESS, "Horse armor toughness", netherite ? NETHERITE_TOUGHNESS : 0.0D);
        set(horse, SharedMonsterAttributes.KNOCKBACK_RESISTANCE, KNOCKBACK, "Horse armor knockback resistance",
                netherite ? NETHERITE_KNOCKBACK : 0.0D);
    }

    private static void set(EntityLivingBase mob, IAttribute attribute, UUID id, String name, double amount) {
        IAttributeInstance instance = mob.getEntityAttribute(attribute);
        if (instance == null) {
            return;
        }
        AttributeModifier old = instance.getModifier(id);
        if (old != null && old.getAmount() == amount) {
            return;
        }
        if (old != null) {
            instance.removeModifier(id);
        }
        if (amount != 0.0D) {
            instance.applyModifier(new AttributeModifier(id, name, amount, 0).setSaved(false));
        }
    }
}
