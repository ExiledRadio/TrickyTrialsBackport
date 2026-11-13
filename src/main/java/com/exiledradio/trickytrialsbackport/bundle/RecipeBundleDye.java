package com.exiledradio.trickytrialsbackport.bundle;

import net.minecraft.inventory.InventoryCrafting;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.crafting.Ingredient;
import net.minecraft.util.ResourceLocation;
import net.minecraft.world.World;
import net.minecraftforge.oredict.OreIngredient;
import net.minecraftforge.oredict.ShapelessOreRecipe;

/**
 * Any bundle and one dye make the bundle of that color with the same contents: 1.21.11's
 * {@code crafting_transmute} recipes (group {@code bundle_dye}). A bundle already of that color makes
 * nothing, as vanilla's transmute recipe refuses an unchanged result.
 *
 * <p>The dye is any {@code dye<Color>} ore-dictionary item, vanilla's dye first among them.
 */
public final class RecipeBundleDye extends ShapelessOreRecipe {

    private final Item result;

    public RecipeBundleDye(ResourceLocation group, Item result, Item[] bundles, String dyeOre) {
        super(group, new ItemStack(result), Ingredient.fromItems(bundles), new OreIngredient(dyeOre));
        this.result = result;
    }

    @Override
    public boolean matches(InventoryCrafting inv, World world) {
        if (!super.matches(inv, world)) {
            return false;
        }
        ItemStack bundle = this.bundle(inv);
        return !bundle.isEmpty() && bundle.getItem() != this.result;
    }

    @Override
    public ItemStack getCraftingResult(InventoryCrafting inv) {
        ItemStack bundle = this.bundle(inv);
        ItemStack out = new ItemStack(this.result);
        if (!bundle.isEmpty() && bundle.hasTagCompound()) {
            out.setTagCompound(bundle.getTagCompound().copy());
        }
        return out;
    }

    private ItemStack bundle(InventoryCrafting inv) {
        for (int i = 0; i < inv.getSizeInventory(); i++) {
            ItemStack stack = inv.getStackInSlot(i);
            if (BundleContents.isBundle(stack)) {
                return stack;
            }
        }
        return ItemStack.EMPTY;
    }
}
