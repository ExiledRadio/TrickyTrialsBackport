package com.exiledradio.trickytrialsbackport.coppergear;

import com.exiledradio.backportcore.copper.Oxidation;
import com.exiledradio.trickytrialsbackport.ModConfig;
import com.exiledradio.trickytrialsbackport.TrickyTrialsBackport;
import com.exiledradio.trickytrialsbackport.common.Content;

import net.minecraft.block.Block;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.Entity;
import net.minecraft.entity.effect.EntityLightningBolt;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemAxe;
import net.minecraft.item.ItemStack;
import net.minecraft.util.EnumActionResult;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.SoundEvent;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraftforge.event.entity.EntityJoinWorldEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.ObfuscationReflectionHelper;
import net.minecraftforge.fml.common.eventhandler.EventPriority;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.oredict.OreDictionary;

import java.util.Arrays;
import java.util.Random;

/**
 * Waxing, scraping and lightning for this stream's bars, chains and lanterns, as for every copper block.
 *
 * <p>The copper stream's {@code CopperEvents} does the same for every copper block under this mod's id, so while it is
 * on either handler serves a click (the first one cancels the event, so it never happens twice) and its lightning
 * already reaches these blocks; this stream's lightning runs only while the copper stream is off.
 */
@Mod.EventBusSubscriber(modid = TrickyTrialsBackport.MOD_ID)
public final class CopperGearEvents {

    private CopperGearEvents() {
    }

    static boolean ours(Block b) {
        return CopperGearContent.BLOCKS.containsValue(b);
    }

    /** Wax: an item in {@code copper.waxItems}, or any item with the ore name honeycomb (the copper stream's rule). */
    static boolean isWax(ItemStack stack) {
        if (stack.isEmpty()) {
            return false;
        }
        ResourceLocation id = stack.getItem().getRegistryName();
        if (id != null && Arrays.asList(ModConfig.copper.waxItems).contains(id.toString())) {
            return true;
        }
        for (int ore : OreDictionary.getOreIDs(stack)) {
            if ("honeycomb".equals(OreDictionary.getOreName(ore))) {
                return true;
            }
        }
        return false;
    }

    static boolean isAxe(ItemStack stack) {
        return !stack.isEmpty() && (stack.getItem() instanceof ItemAxe || stack.getItem().getToolClasses(stack).contains("axe"));
    }

    @SubscribeEvent
    public static void rightClick(PlayerInteractEvent.RightClickBlock event) {
        if (!CopperGearContent.enabled()) {
            return;
        }
        World world = event.getWorld();
        BlockPos pos = event.getPos();
        IBlockState state = world.getBlockState(pos);
        Block block = state.getBlock();
        if (!ours(block)) {
            return;
        }
        ItemStack held = event.getItemStack();
        EntityPlayer player = event.getEntityPlayer();
        Block to;
        SoundEvent sound;
        boolean wax = isWax(held);
        if (wax) {
            to = Oxidation.waxedOf(block);
            sound = Content.sound("item.honeycomb.wax_on");
        } else if (isAxe(held)) {
            to = Oxidation.unwaxed(block);
            if (to != null) {
                sound = Content.sound("item.axe.wax_off");
            } else {
                to = Oxidation.previous(block);
                sound = Content.sound("item.axe.scrape");
            }
        } else {
            return;
        }
        if (to == null) {
            return;
        }
        event.setCanceled(true);
        event.setCancellationResult(EnumActionResult.SUCCESS);
        if (world.isRemote) {
            return;
        }
        Oxidation.change(world, pos, state, to);
        world.playSound(null, pos, sound, SoundCategory.BLOCKS, 1.0F, 1.0F);
        if (!player.capabilities.isCreativeMode) {
            if (wax) {
                held.shrink(1);
            } else {
                held.damageItem(1, player);
            }
        }
    }

    // ---------------------------------------------------------------------------------------------
    // Lightning, while the copper stream is off

    @SubscribeEvent(priority = EventPriority.LOW)
    public static void lightning(EntityJoinWorldEvent event) {
        Entity e = event.getEntity();
        World world = event.getWorld();
        if (world.isRemote || event.isCanceled() || !(e instanceof EntityLightningBolt) || !CopperGearContent.enabled()
                || ModConfig.copper.enabled || effectOnly((EntityLightningBolt) e)) {
            return;
        }
        cleanCopper(world, new BlockPos(e.posX, e.posY - 1.0E-6, e.posZ), world.rand);
    }

    private static boolean effectOnly(EntityLightningBolt bolt) {
        try {
            Boolean b = ObfuscationReflectionHelper.getPrivateValue(EntityLightningBolt.class, bolt, "field_184529_d");
            return b != null && b;
        } catch (RuntimeException ex) {
            return false;
        }
    }

    /**
     * 1.17's {@code LightningBolt.clearCopperOnLightningStrike}: the struck copper turns unaffected, then 3-5 random
     * walks of 1-8 steps through touching copper scrape one stage off each block they reach (ours only).
     */
    public static void cleanCopper(World world, BlockPos pos, Random rand) {
        IBlockState state = world.getBlockState(pos);
        if (Oxidation.stage(state.getBlock()) < 0 || !ours(state.getBlock())) {
            return;
        }
        Block first = Oxidation.first(state.getBlock());
        if (first != state.getBlock()) {
            Oxidation.change(world, pos, state, first);
        }
        int walks = rand.nextInt(3) + 3;
        for (int w = 0; w < walks; w++) {
            int steps = rand.nextInt(8) + 1;
            BlockPos cur = pos;
            for (int s = 0; s < steps && cur != null; s++) {
                cur = step(world, cur, rand);
            }
        }
    }

    private static BlockPos step(World world, BlockPos from, Random rand) {
        for (int i = 0; i < 10; i++) {
            BlockPos p = from.add(rand.nextInt(3) - 1, rand.nextInt(3) - 1, rand.nextInt(3) - 1);
            IBlockState s = world.getBlockState(p);
            if (Oxidation.stage(s.getBlock()) >= 0) {
                Block prev = Oxidation.previous(s.getBlock());
                if (prev != null && ours(s.getBlock())) {
                    Oxidation.change(world, p, s, prev);
                }
                return p;
            }
        }
        return null;
    }
}
