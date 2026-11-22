package com.exiledradio.trickytrialsbackport.selftest;

import com.exiledradio.trickytrialsbackport.trial.TrialContent;

import net.minecraft.entity.item.EntityItem;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.init.Blocks;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.tileentity.TileEntityChest;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraftforge.fml.common.registry.ForgeRegistries;

import java.util.List;

/**
 * Village &amp; Pillage 2.7.3's ominous bottles carried into a game with Tricky Trials, through a saved world. Two runs,
 * the world kept between them:
 * <ol>
 *   <li>{@code -Pselftest=ominoussave} with Village &amp; Pillage 2.7.3 in {@code eclipse/mods}: its bottles go into a
 *   saved player's inventory (IV x7), a chest (III x5) and onto the ground (II x2) at the spawn, and the world saves.</li>
 *   <li>{@code -Pselftest=ominousload} with Village &amp; Pillage 2.7.4: every one is our bottle, same level and count.</li>
 * </ol>
 * The world's registry snapshot holds {@code villagerbackport:ominous_bottle}; Forge asks MissingMappings for it as the
 * world loads, and the stacks in the chunk and the player file read through the rename.
 */
public final class OminousMigrationSelfTest implements SelfTest {

    private static final ResourceLocation OLD = new ResourceLocation("villagerbackport", "ominous_bottle");

    private final boolean save;

    OminousMigrationSelfTest(boolean save) {
        this.save = save;
    }

    @Override
    public String name() {
        return save ? "ominoussave" : "ominousload";
    }

    @Override
    public void run(Check c) {
        BlockPos chest = c.world.getSpawnPoint().up(100);
        EntityPlayerMP player = Players.real(c.world, "bottleCarrier");
        if (save) {
            Item old = ForgeRegistries.ITEMS.getValue(OLD);
            if (!c.ok(old != null && OLD.equals(old.getRegistryName()), "Village & Pillage 2.7.3's bottle is registered")) {
                return;
            }
            player.setPosition(chest.getX() + 0.5, chest.getY() + 1, chest.getZ() + 0.5);
            player.inventory.setInventorySlotContents(0, new ItemStack(old, 7, 4));
            c.world.getSaveHandler().getPlayerNBTManager().writePlayerData(player);
            c.world.setBlockState(chest, Blocks.CHEST.getDefaultState());
            TileEntity te = c.world.getTileEntity(chest);
            c.ok(te instanceof TileEntityChest, "a chest");
            ((TileEntityChest) te).setInventorySlotContents(0, new ItemStack(old, 5, 3));
            EntityItem ground = new EntityItem(c.world, chest.getX() + 2.5, chest.getY() + 1, chest.getZ() + 0.5, new ItemStack(old, 2, 1));
            ground.setNoGravity(true);
            ground.setNoDespawn();
            ground.setInfinitePickupDelay();
            c.ok(c.world.spawnEntity(ground), "a dropped stack");
            c.world.getChunk(chest).markDirty();
            return;
        }

        Item ours = TrialContent.ominousBottle;
        Item old = ForgeRegistries.ITEMS.getValue(OLD);
        c.ok(old == ours, "villagerbackport:ominous_bottle now reads as our bottle ("
                + (old == null ? "null" : old.getRegistryName()) + ")");
        c.world.getSaveHandler().getPlayerNBTManager().readPlayerData(player);
        check(c, player.inventory.getStackInSlot(0), ours, 7, 4, "the saved player's inventory");
        TileEntity te = c.world.getTileEntity(chest);
        c.ok(te instanceof TileEntityChest, "the chest is still there");
        if (te instanceof TileEntityChest) {
            check(c, ((TileEntityChest) te).getStackInSlot(0), ours, 5, 3, "the chest");
        }
        List<EntityItem> items = c.world.getEntitiesWithinAABB(EntityItem.class, new AxisAlignedBB(chest).grow(6.0));
        c.eq(1, items.size(), "one dropped stack");
        if (!items.isEmpty()) {
            check(c, items.get(0).getItem(), ours, 2, 1, "the dropped stack");
        }
    }

    private static void check(Check c, ItemStack stack, Item ours, int count, int level, String where) {
        c.ok(stack.getItem() == ours && stack.getCount() == count && stack.getMetadata() == level,
                where + ": " + count + " of our bottle " + (level + 1) + " (got " + stack.getCount() + " "
                        + stack.getItem().getRegistryName() + ":" + stack.getMetadata() + ")");
    }
}
