package com.exiledradio.trickytrialsbackport.copper;

import com.exiledradio.trickytrialsbackport.TrickyTrialsBackport;
import com.exiledradio.trickytrialsbackport.common.Content;

import net.minecraft.block.Block;
import net.minecraft.block.properties.IProperty;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.Entity;
import net.minecraft.entity.effect.EntityLightningBolt;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemAxe;
import net.minecraft.item.ItemStack;
import net.minecraft.util.EnumActionResult;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.SoundEvent;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraftforge.event.entity.EntityJoinWorldEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.EventPriority;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.ObfuscationReflectionHelper;

import java.util.Random;

/**
 * What players and lightning do to this mod's copper, as Caves &amp; Cliffs does to its own (it handles its
 * blocks; this handles only blocks registered under this mod's id, so nothing happens twice).
 *
 * <ul>
 *   <li><b>Right click</b> (current Java): honeycomb waxes copper (one used); an axe takes the wax off first,
 *       otherwise scrapes off one stage of oxidation (one durability). A door's two halves change together.
 *       The arm swings and nothing else happens.</li>
 *   <li><b>Lightning</b>: a real bolt striking our copper (or a lightning rod standing on it) turns that block
 *       back to unaffected, then walks 3-5 random paths of 1-8 steps through touching copper, scraping one
 *       stage off each of our blocks it reaches.</li>
 * </ul>
 */
@Mod.EventBusSubscriber(modid = TrickyTrialsBackport.MOD_ID)
public final class CopperEvents {

    private CopperEvents() {
    }

    static boolean ours(Block b) {
        return b.getRegistryName() != null && TrickyTrialsBackport.MOD_ID.equals(b.getRegistryName().getNamespace());
    }

    static boolean isAxe(ItemStack stack) {
        return !stack.isEmpty() && (stack.getItem() instanceof ItemAxe || stack.getItem().getToolClasses(stack).contains("axe"));
    }

    @SubscribeEvent
    public static void rightClick(PlayerInteractEvent.RightClickBlock event) {
        if (!CopperContent.enabled()) {
            return;
        }
        World world = event.getWorld();
        BlockPos pos = event.getPos();
        IBlockState state = world.getBlockState(pos);
        Block block = state.getBlock();
        if (!Weathering.isCopper(block) || !ours(block)) {
            return;
        }
        ItemStack held = event.getItemStack();
        EntityPlayer player = event.getEntityPlayer();
        Block to;
        SoundEvent sound;
        boolean wax = Weathering.isWax(held);
        if (wax) {
            to = Weathering.waxedOf(block);
            sound = Content.sound("item.honeycomb.wax_on");
        } else if (isAxe(held)) {
            to = Weathering.unwaxed(block);
            if (to != null) {
                sound = Content.sound("item.axe.wax_off");
            } else {
                to = Weathering.previous(block);
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
        Weathering.change(world, pos, state, to);
        world.playSound(null, pos, sound, SoundCategory.BLOCKS, 1.0F, 1.0F);
        if (!player.capabilities.isCreativeMode) {
            if (wax) {
                held.shrink(1);
            } else {
                held.damageItem(1, player);
            }
        }
    }

    private static boolean effectOnly(EntityLightningBolt bolt) {
        try {
            Boolean b = ObfuscationReflectionHelper.getPrivateValue(EntityLightningBolt.class, bolt, "field_184529_d");
            return b != null && b;
        } catch (RuntimeException e) {
            return false;
        }
    }

    @SubscribeEvent(priority = EventPriority.LOW)
    public static void lightning(EntityJoinWorldEvent event) {
        Entity e = event.getEntity();
        World world = event.getWorld();
        if (world.isRemote || event.isCanceled() || !(e instanceof EntityLightningBolt) || !CopperContent.enabled()
                || effectOnly((EntityLightningBolt) e)) {
            return;
        }
        BlockPos at = new BlockPos(e.posX, e.posY - 1.0E-6, e.posZ);
        IBlockState hit = world.getBlockState(at);
        BlockPos clean = at;
        if (isRod(hit)) {
            for (IProperty<?> p : hit.getPropertyKeys()) {
                if ("facing".equals(p.getName()) && hit.getValue(p) instanceof EnumFacing) {
                    clean = at.offset(((EnumFacing) hit.getValue(p)).getOpposite());
                }
            }
        }
        cleanCopper(world, clean, world.rand);
    }

    /** Any mod's lightning rod (Caves &amp; Cliffs' is {@code lightning_rod}). */
    static boolean isRod(IBlockState state) {
        return state.getBlock().getRegistryName() != null
                && "lightning_rod".equals(state.getBlock().getRegistryName().getPath());
    }

    /** Turns a struck copper block of ours back to unaffected and scrapes a stage off a random walk around it. */
    public static void cleanCopper(World world, BlockPos pos, Random rand) {
        IBlockState state = world.getBlockState(pos);
        if (Weathering.stage(state.getBlock()) < 0 || !ours(state.getBlock())) {
            return;
        }
        Block first = Weathering.first(state.getBlock());
        if (first != state.getBlock()) {
            Weathering.change(world, pos, state, first);
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

    /** One step: up to 10 tries at a random touching block; the first copper one (ours) loses a stage. */
    private static BlockPos step(World world, BlockPos from, Random rand) {
        for (int i = 0; i < 10; i++) {
            BlockPos p = from.add(rand.nextInt(3) - 1, rand.nextInt(3) - 1, rand.nextInt(3) - 1);
            IBlockState s = world.getBlockState(p);
            if (Weathering.stage(s.getBlock()) >= 0) {
                Block prev = Weathering.previous(s.getBlock());
                if (prev != null && ours(s.getBlock())) {
                    Weathering.change(world, p, s, prev);
                }
                return p;
            }
        }
        return null;
    }
}
