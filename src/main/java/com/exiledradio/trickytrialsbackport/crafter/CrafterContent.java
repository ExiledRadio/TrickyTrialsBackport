package com.exiledradio.trickytrialsbackport.crafter;

import com.exiledradio.trickytrialsbackport.ModConfig;
import com.exiledradio.trickytrialsbackport.TrickyTrialsBackport;
import com.exiledradio.trickytrialsbackport.common.Content;
import com.exiledradio.trickytrialsbackport.common.Guis;
import com.exiledradio.trickytrialsbackport.common.ItemTrigger;

import net.minecraft.block.Block;
import net.minecraft.init.Blocks;
import net.minecraft.init.Items;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.crafting.IRecipe;
import net.minecraft.tileentity.TileEntity;
import net.minecraftforge.event.RegistryEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.registry.GameRegistry;
import net.minecraftforge.oredict.ShapedOreRecipe;

/**
 * Stream 6: the crafter ({@code crafter}) and <i>Crafters Crafting Crafters</i>. Gui id 6 (its row).
 */
@Mod.EventBusSubscriber(modid = TrickyTrialsBackport.MOD_ID)
public final class CrafterContent {

    public static final int GUI = 6;

    /** {@code trickytrialsbackport:crafter_crafted}: what a crafter made, for players within 17 blocks. */
    public static final ItemTrigger CRAFTED = new ItemTrigger(Content.id("crafter_crafted"));

    public static Block crafter;

    private CrafterContent() {
    }

    public static boolean enabled() {
        return ModConfig.crafter.enabled;
    }

    @SubscribeEvent
    public static void registerBlocks(RegistryEvent.Register<Block> event) {
        if (!enabled()) {
            return;
        }
        crafter = Content.block(event.getRegistry(), "crafter", new BlockCrafter(), "pickaxe");
        GameRegistry.registerTileEntity(TileEntityCrafter.class, Content.id("crafter"));
        Guis.container(GUI, (player, world, pos) -> {
            TileEntity te = world.getTileEntity(pos);
            return te instanceof TileEntityCrafter ? new ContainerCrafter(player.inventory, (TileEntityCrafter) te) : null;
        });
    }

    @SubscribeEvent
    public static void registerItems(RegistryEvent.Register<Item> event) {
        if (!enabled()) {
            return;
        }
        Content.sound("block.crafter.craft");
        Content.sound("block.crafter.fail");
        Content.registerBlockItem(event.getRegistry(), crafter);
        CRAFTED.register();
    }

    @SubscribeEvent
    public static void registerRecipes(RegistryEvent.Register<IRecipe> event) {
        if (!enabled()) {
            return;
        }
        ShapedOreRecipe r = new ShapedOreRecipe(null, new ItemStack(crafter), "###", "#C#", "RDR",
                '#', "ingotIron", 'C', Blocks.CRAFTING_TABLE, 'R', Items.REDSTONE, 'D', Blocks.DROPPER);
        r.setRegistryName(Content.id("crafter"));
        event.getRegistry().register(r);
    }
}
