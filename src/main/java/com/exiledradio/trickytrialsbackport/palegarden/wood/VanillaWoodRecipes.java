package com.exiledradio.trickytrialsbackport.palegarden.wood;

import com.exiledradio.trickytrialsbackport.TrickyTrialsBackport;
import com.exiledradio.trickytrialsbackport.common.Owners;

import net.minecraft.init.Blocks;
import net.minecraft.item.ItemStack;
import net.minecraft.item.crafting.IRecipe;
import net.minecraft.item.crafting.Ingredient;
import net.minecraft.item.crafting.ShapedRecipes;
import net.minecraft.util.NonNullList;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.registries.IForgeRegistry;

import java.util.ArrayList;
import java.util.List;

/**
 * Makes vanilla's trapdoor, button, pressure plate and sign recipes oak-only, as 1.14 did.
 *
 * <p>1.12.2 has one trapdoor, button, plate and sign for every wood, and Forge widens their recipes to any
 * {@code plankWood}, so without this cherry and bamboo planks would make oak ones before their own recipes
 * are reached. Village & Pillage narrows the same four when it is installed (it adds the other woods'), so
 * this runs only without it. Edited in place, never removed: each recipe keeps its name and recipe-book
 * unlock; one another mod has replaced is left alone and named in the log.
 */
public final class VanillaWoodRecipes {

    private static boolean done;

    private VanillaWoodRecipes() {
    }

    public static void narrowToOak(IForgeRegistry<IRecipe> registry) {
        if (done || Owners.present(Owners.VILLAGE_AND_PILLAGE)) {
            return;
        }
        done = true;
        ItemStack oak = new ItemStack(Blocks.PLANKS, 1, 0);
        Ingredient oakOnly = Ingredient.fromStacks(oak);
        List<String> changed = new ArrayList<String>();
        for (String name : new String[] {"trapdoor", "wooden_button", "wooden_pressure_plate", "sign"}) {
            IRecipe recipe = registry.getValue(new ResourceLocation("minecraft", name));
            if (!(recipe instanceof ShapedRecipes)) {
                TrickyTrialsBackport.LOGGER.warn("minecraft:{} is not vanilla's recipe; left as it is.", name);
                continue;
            }
            NonNullList<Ingredient> slots = ((ShapedRecipes) recipe).recipeItems;
            boolean any = false;
            for (int i = 0; i < slots.size(); i++) {
                if (slots.get(i) != Ingredient.EMPTY && slots.get(i).apply(oak)) {
                    slots.set(i, oakOnly);
                    any = true;
                }
            }
            if (any) {
                changed.add(name);
            }
        }
        TrickyTrialsBackport.LOGGER.info("Narrowed vanilla recipes to oak planks as 1.14 did: {}", changed);
    }
}
