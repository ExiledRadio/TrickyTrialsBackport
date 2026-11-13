package com.exiledradio.trickytrialsbackport.lookfeel;

import net.minecraft.init.Blocks;
import net.minecraft.init.Items;
import net.minecraft.item.EnumRarity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemRecord;
import net.minecraft.item.ItemStack;

import javax.annotation.Nullable;
import java.util.IdentityHashMap;
import java.util.Map;

/**
 * 1.21.2's item rarities for the vanilla items 1.12.2 has (1.21.11's {@code Items}, the items whose rarity is not
 * Common), and 1.21's rule for enchanted items: Common and Uncommon become Rare, Rare becomes Epic.
 *
 * <p>1.12.2 rated nearly everything Common: golden apples Rare and Epic, music discs Rare, enchanted books
 * Uncommon, anything enchanted Rare. The client shows these in tooltips ({@code lookfeel.client.ItemRarityTooltips}).
 */
public final class Rarities {

    private static final Map<Item, EnumRarity> BASE = new IdentityHashMap<Item, EnumRarity>();

    private Rarities() {
    }

    private static synchronized Map<Item, EnumRarity> base() {
        if (BASE.isEmpty()) {
            epic(Item.getItemFromBlock(Blocks.DRAGON_EGG), Item.getItemFromBlock(Blocks.COMMAND_BLOCK),
                    Item.getItemFromBlock(Blocks.REPEATING_COMMAND_BLOCK), Item.getItemFromBlock(Blocks.CHAIN_COMMAND_BLOCK),
                    Item.getItemFromBlock(Blocks.BARRIER), Item.getItemFromBlock(Blocks.STRUCTURE_VOID),
                    Item.getItemFromBlock(Blocks.STRUCTURE_BLOCK), Items.ELYTRA, Items.COMMAND_BLOCK_MINECART,
                    Items.KNOWLEDGE_BOOK);
            BASE.put(Item.getItemFromBlock(Blocks.BEACON), EnumRarity.RARE);
            BASE.put(Items.NETHER_STAR, EnumRarity.RARE);
            BASE.put(Items.ENCHANTED_BOOK, EnumRarity.RARE);
            for (Item item : new Item[]{Items.CHAINMAIL_HELMET, Items.CHAINMAIL_CHESTPLATE, Items.CHAINMAIL_LEGGINGS,
                    Items.CHAINMAIL_BOOTS, Items.EXPERIENCE_BOTTLE, Items.DRAGON_BREATH, Items.TOTEM_OF_UNDYING}) {
                BASE.put(item, EnumRarity.UNCOMMON);
            }
        }
        return BASE;
    }

    private static void epic(Item... items) {
        for (Item item : items) {
            BASE.put(item, EnumRarity.EPIC);
        }
    }

    /**
     * @return 1.21.11's rarity of a vanilla 1.12.2 item stack, or null for an item this table does not rate (another
     * mod's, which keeps its own)
     */
    @Nullable
    public static EnumRarity of(ItemStack stack) {
        if (stack.isEmpty() || stack.getItem().getRegistryName() == null
                || !"minecraft".equals(stack.getItem().getRegistryName().getNamespace())) {
            return null;
        }
        Item item = stack.getItem();
        EnumRarity base;
        if (item == Items.GOLDEN_APPLE) {
            // The golden apple is Common, the enchanted golden apple Rare.
            base = stack.getMetadata() == 0 ? EnumRarity.COMMON : EnumRarity.RARE;
        } else if (item == Items.SKULL) {
            // 0 skeleton, 1 wither skeleton, 2 zombie, 3 player, 4 creeper, 5 dragon.
            int meta = stack.getMetadata();
            base = meta == 1 ? EnumRarity.RARE : meta == 5 ? EnumRarity.EPIC : EnumRarity.UNCOMMON;
        } else if (item instanceof ItemRecord) {
            // 1.12.2's twelve discs are all Uncommon in 1.21.2.
            base = EnumRarity.UNCOMMON;
        } else {
            EnumRarity rated = base().get(item);
            base = rated != null ? rated : EnumRarity.COMMON;
        }
        if (!stack.isItemEnchanted()) {
            return base;
        }
        switch (base) {
            case COMMON:
            case UNCOMMON:
                return EnumRarity.RARE;
            default:
                return EnumRarity.EPIC;
        }
    }
}
