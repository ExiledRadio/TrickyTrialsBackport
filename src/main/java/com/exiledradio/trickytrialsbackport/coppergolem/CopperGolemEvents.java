package com.exiledradio.trickytrialsbackport.coppergolem;

import com.exiledradio.backportcore.copper.Oxidation;
import com.exiledradio.trickytrialsbackport.ModConfig;
import com.exiledradio.trickytrialsbackport.TrickyTrialsBackport;
import com.exiledradio.trickytrialsbackport.common.Owners;

import com.google.common.base.Predicate;
import net.minecraft.advancements.CriteriaTriggers;
import net.minecraft.block.Block;
import net.minecraft.block.BlockDispenser;
import net.minecraft.block.BlockPumpkin;
import net.minecraft.block.state.BlockWorldState;
import net.minecraft.block.state.IBlockState;
import net.minecraft.block.state.pattern.BlockPattern;
import net.minecraft.block.state.pattern.FactoryBlockPattern;
import net.minecraft.dispenser.IBehaviorDispenseItem;
import net.minecraft.dispenser.IBlockSource;
import net.minecraft.entity.ai.EntityAIBase;
import net.minecraft.entity.ai.EntityAILookAtVillager;
import net.minecraft.entity.ai.EntityAITasks;
import net.minecraft.entity.monster.EntityIronGolem;
import net.minecraft.entity.passive.EntityVillager;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.init.Blocks;
import net.minecraft.item.Item;
import net.minecraft.item.ItemAxe;
import net.minecraft.item.ItemStack;
import net.minecraft.util.EnumActionResult;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.SoundEvent;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraftforge.event.entity.EntityJoinWorldEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.event.world.BlockEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.EventPriority;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.oredict.OreDictionary;
import net.minecraftforge.fml.common.registry.ForgeRegistries;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * What the world does with the copper golem's blocks:
 * <ul>
 *   <li><b>Building</b> (1.21.9's {@code CarvedPumpkinBlock}): a carved pumpkin or jack o'lantern placed last onto a
 *       copper block, in any of the pattern's orientations, makes a copper golem where the pumpkin was, at the copper
 *       block's stage (unwaxed), and the copper block becomes a copper chest of that stage facing the pumpkin's way.
 *       Snow and iron golems are tried first, as in 1.21. The pattern is checked after any block change at a pumpkin
 *       (Forge's neighbor notification, which 1.12.2 sends right after the pumpkin's own golem check), so pumpkins
 *       placed by players, endermen, pistons and dispensers all count. Dispensers place a pumpkin onto copper.</li>
 *   <li><b>Right click</b> on chests and statues: honeycomb waxes, an axe takes the wax off or scrapes a stage; on an
 *       unaffected unwaxed statue the axe brings the golem back.</li>
 *   <li><b>Iron golems</b> (1.21.9's {@code OfferFlowerGoal}): their poppy offer can go to a copper golem too, and if
 *       the offer runs its full 400 ticks close enough, the poppy goes onto its antenna.</li>
 * </ul>
 */
@Mod.EventBusSubscriber(modid = TrickyTrialsBackport.MOD_ID)
public final class CopperGolemEvents {

    private static final Predicate<IBlockState> IS_PUMPKIN = state -> state != null
            && (state.getBlock() == carvedPumpkin() || state.getBlock() == Blocks.LIT_PUMPKIN);
    private static final Predicate<IBlockState> IS_COPPER = state -> state != null && CopperGolemContent.copperBlockStage(state.getBlock()) >= 0;

    private static BlockPattern copperGolemBase;
    private static BlockPattern copperGolemFull;

    private CopperGolemEvents() {
    }

    /** From the mod's init: dispensers place pumpkins onto copper blocks too (1.21's {@code canSpawnGolem}). */
    public static void init() {
        if (!CopperGolemContent.enabled()) {
            return;
        }
        Item pumpkin = Item.getItemFromBlock(carvedPumpkin());
        IBehaviorDispenseItem old = BlockDispenser.DISPENSE_BEHAVIOR_REGISTRY.getObject(pumpkin);
        BlockDispenser.DISPENSE_BEHAVIOR_REGISTRY.putObject(pumpkin, new PumpkinOntoCopper(old));
    }

    /**
     * The carved pumpkin, which builds golems (1.21.9's {@code CarvedPumpkinBlock}). 1.12.2's pumpkin has a face and
     * is the carved pumpkin; Update Aquatic Backport owns 1.13's split (Rule 1), where that block becomes the plain
     * pumpkin and {@code updateaquaticbackport:carved_pumpkin} is the carved one, so with it installed only its carved
     * pumpkin (and the jack o'lantern) builds a copper golem.
     */
    public static Block carvedPumpkin() {
        if (Owners.present(Owners.UPDATE_AQUATIC)) {
            Block carved = ForgeRegistries.BLOCKS.getValue(new ResourceLocation(Owners.UPDATE_AQUATIC, "carved_pumpkin"));
            if (carved != null && carved != Blocks.AIR) {
                return carved;
            }
        }
        return Blocks.PUMPKIN;
    }

    // ------------------------------------------------------------------------------------------
    // Shared item tests
    // ------------------------------------------------------------------------------------------

    /** Wax: an item in {@code copper.waxItems}, or any item with the ore name honeycomb (the copper streams' rule). */
    public static boolean isWax(ItemStack stack) {
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

    public static boolean isAxe(ItemStack stack) {
        return !stack.isEmpty() && (stack.getItem() instanceof ItemAxe || stack.getItem().getToolClasses(stack).contains("axe"));
    }

    // ------------------------------------------------------------------------------------------
    // Building
    // ------------------------------------------------------------------------------------------

    static BlockPattern base() {
        if (copperGolemBase == null) {
            copperGolemBase = FactoryBlockPattern.start().aisle(" ", "#").where('#', BlockWorldState.hasState(IS_COPPER)).build();
        }
        return copperGolemBase;
    }

    static BlockPattern full() {
        if (copperGolemFull == null) {
            copperGolemFull = FactoryBlockPattern.start().aisle("^", "#")
                    .where('^', BlockWorldState.hasState(IS_PUMPKIN))
                    .where('#', BlockWorldState.hasState(IS_COPPER)).build();
        }
        return copperGolemFull;
    }

    @SubscribeEvent
    public static void onNeighborNotify(BlockEvent.NeighborNotifyEvent event) {
        World world = event.getWorld();
        if (world.isRemote || !CopperGolemContent.enabled() || !ModConfig.copperGolem.building || !IS_PUMPKIN.apply(event.getState())) {
            return;
        }
        trySpawnCopperGolem(world, event.getPos());
    }

    /** @return the golem made, or null when the pumpkin is not on copper */
    @Nullable
    public static EntityCopperGolem trySpawnCopperGolem(World world, BlockPos pos) {
        BlockPattern.PatternHelper match = full().match(world, pos);
        if (match == null) {
            return null;
        }
        BlockWorldState head = match.translateOffset(0, 0, 0);
        BlockWorldState body = match.translateOffset(0, 1, 0);
        IBlockState pumpkin = head.getBlockState();
        IBlockState copper = body.getBlockState();
        int stage = CopperGolemContent.copperBlockStage(copper.getBlock());
        EnumFacing facing = pumpkin.getValue(BlockPumpkin.FACING);

        // 1.21's spawnGolemInWorld: clear the pattern, spawn at the pumpkin, tell nearby players, update neighbors.
        for (BlockWorldState s : new BlockWorldState[] {head, body}) {
            world.setBlockState(s.getPos(), Blocks.AIR.getDefaultState(), 2);
            world.playEvent(2001, s.getPos(), Block.getStateId(s.getBlockState()));
        }
        EntityCopperGolem golem = new EntityCopperGolem(world);
        BlockPos at = head.getPos();
        golem.setLocationAndAngles(at.getX() + 0.5D, at.getY() + 0.05D, at.getZ() + 0.5D, 0.0F, 0.0F);
        world.spawnEntity(golem);
        for (EntityPlayerMP player : world.getEntitiesWithinAABB(EntityPlayerMP.class, golem.getEntityBoundingBox().grow(5.0D))) {
            CriteriaTriggers.SUMMONED_ENTITY.trigger(player, golem);
        }
        world.notifyNeighborsRespectDebug(head.getPos(), Blocks.AIR, false);
        world.notifyNeighborsRespectDebug(body.getPos(), Blocks.AIR, false);

        // 1.21's replaceCopperBlockWithChest: the chest of the copper's stage, joining a single copper chest beside it.
        BlockCopperChest chest = CopperGolemContent.CHESTS[0][stage];
        BlockPos chestPos = body.getPos();
        for (EnumFacing f : EnumFacing.Plane.HORIZONTAL) {
            BlockPos p = chestPos.offset(f);
            Block b = world.getBlockState(p).getBlock();
            if (b instanceof BlockCopperChest && BlockCopperChest.partner(world, p) == null) {
                chest = BlockCopperChest.leastOxidized(chest, (BlockCopperChest) b);
                BlockCopperChest.setChestBlock(world, p, chest, 2);
                break;
            }
        }
        world.setBlockState(chestPos, chest.getDefaultState().withProperty(net.minecraft.block.BlockChest.FACING, facing), 2);
        golem.spawn(stage);
        return golem;
    }

    /** 1.21's carved pumpkin dispensing: placed in front when it would build a golem on copper, else as before. */
    static final class PumpkinOntoCopper implements IBehaviorDispenseItem {
        private final IBehaviorDispenseItem old;

        PumpkinOntoCopper(IBehaviorDispenseItem old) {
            this.old = old;
        }

        @Override
        public ItemStack dispense(IBlockSource source, ItemStack stack) {
            World world = source.getWorld();
            EnumFacing facing = source.getBlockState().getValue(BlockDispenser.FACING);
            BlockPos pos = source.getBlockPos().offset(facing);
            if (!ModConfig.copperGolem.building || !world.isAirBlock(pos) || base().match(world, pos) == null) {
                return this.old.dispense(source, stack);
            }
            world.setBlockState(pos, carvedPumpkin().getDefaultState(), 3);
            stack.shrink(1);
            // BehaviorDefaultDispenseItem's click and smoke.
            world.playEvent(1000, source.getBlockPos(), 0);
            world.playEvent(2000, source.getBlockPos(), facing.getXOffset() + 1 + (facing.getZOffset() + 1) * 3);
            return stack;
        }
    }

    // ------------------------------------------------------------------------------------------
    // Right click on copper chests and statues
    // ------------------------------------------------------------------------------------------

    /** Before the copper streams' handlers, which would change one chest half without its tile entity rule. */
    @SubscribeEvent(priority = EventPriority.HIGH)
    public static void rightClick(PlayerInteractEvent.RightClickBlock event) {
        if (!CopperGolemContent.enabled()) {
            return;
        }
        World world = event.getWorld();
        BlockPos pos = event.getPos();
        IBlockState state = world.getBlockState(pos);
        Block block = state.getBlock();
        if (!(block instanceof BlockCopperChest) && !(block instanceof BlockCopperGolemStatue)) {
            return;
        }
        ItemStack held = event.getItemStack();
        EntityPlayer player = event.getEntityPlayer();
        boolean wax = isWax(held);
        boolean axe = !wax && isAxe(held);
        if (!wax && !axe) {
            return;
        }
        Block to;
        SoundEvent sound;
        if (wax) {
            to = Oxidation.waxedOf(block);
            sound = CopperGolemContent.WAX_ON;
        } else {
            to = Oxidation.unwaxed(block);
            if (to != null) {
                sound = CopperGolemContent.AXE_WAX_OFF;
            } else {
                to = Oxidation.previous(block);
                sound = CopperGolemContent.AXE_SCRAPE;
            }
        }
        boolean revive = axe && block instanceof BlockCopperGolemStatue && to == null && Oxidation.stage(block) == 0;
        if (to == null && !revive) {
            return;
        }
        event.setCanceled(true);
        event.setCancellationResult(EnumActionResult.SUCCESS);
        if (world.isRemote) {
            return;
        }
        if (revive) {
            reviveStatue(world, pos, state, held, player);
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

    /** 1.21.9: an axe on an unaffected, unwaxed statue turns it back into a copper golem with its name. */
    static void reviveStatue(World world, BlockPos pos, IBlockState state, ItemStack axe, EntityPlayer player) {
        net.minecraft.tileentity.TileEntity te = world.getTileEntity(pos);
        if (!(te instanceof TileEntityCopperGolemStatue)) {
            return;
        }
        EntityCopperGolem golem = ((TileEntityCopperGolemStatue) te).removeStatue(state);
        axe.damageItem(1, player);
        world.spawnEntity(golem);
        world.setBlockToAir(pos);
    }

    // ------------------------------------------------------------------------------------------
    // Iron golems
    // ------------------------------------------------------------------------------------------

    @SubscribeEvent
    public static void onJoin(EntityJoinWorldEvent event) {
        if (event.getWorld().isRemote || !(event.getEntity() instanceof EntityIronGolem) || !CopperGolemContent.enabled()
                || !ModConfig.copperGolem.ironGolemFlowers) {
            return;
        }
        EntityIronGolem golem = (EntityIronGolem) event.getEntity();
        List<EntityAITasks.EntityAITaskEntry> old = new ArrayList<EntityAITasks.EntityAITaskEntry>();
        for (EntityAITasks.EntityAITaskEntry e : golem.tasks.taskEntries) {
            if (e.action instanceof EntityAILookAtVillager) {
                old.add(e);
            }
        }
        for (EntityAITasks.EntityAITaskEntry e : old) {
            golem.tasks.removeTask(e.action);
            golem.tasks.addTask(e.priority, new OfferFlower(golem));
        }
    }

    /**
     * 1.21.9's {@code OfferFlowerGoal}: by day, 1 in 8000 ticks, the iron golem holds out a poppy for 400 ticks to the
     * nearest villager or copper golem within 6 blocks; a copper golem with nothing on its antenna that is still within
     * reach when the offer ends gets the poppy.
     */
    public static final class OfferFlower extends EntityAIBase {
        public static final int OFFER_TICKS = 400;
        private final EntityIronGolem golem;
        @Nullable
        private net.minecraft.entity.EntityLivingBase entity;
        private int tick;

        public OfferFlower(EntityIronGolem golem) {
            this.golem = golem;
            setMutexBits(3);
        }

        private AxisAlignedBB box() {
            return this.golem.getEntityBoundingBox().grow(6.0D, 2.0D, 6.0D);
        }

        @Override
        public boolean shouldExecute() {
            if (!this.golem.world.isDaytime() || this.golem.getRNG().nextInt(8000) != 0) {
                return false;
            }
            this.entity = nearest();
            return this.entity != null;
        }

        @Nullable
        net.minecraft.entity.EntityLivingBase nearest() {
            net.minecraft.entity.EntityLivingBase best = null;
            double bestDist = Double.MAX_VALUE;
            for (net.minecraft.entity.EntityLivingBase e : this.golem.world.getEntitiesWithinAABB(net.minecraft.entity.EntityLivingBase.class,
                    box(), e -> e instanceof EntityVillager || e instanceof EntityCopperGolem)) {
                double d = e.getDistanceSq(this.golem);
                if (e != this.golem && d <= 36.0D && d < bestDist) {
                    best = e;
                    bestDist = d;
                }
            }
            return best;
        }

        @Override
        public boolean shouldContinueExecuting() {
            return this.tick > 0;
        }

        @Override
        public void startExecuting() {
            this.tick = OFFER_TICKS;
            this.golem.setHoldingRose(true);
        }

        @Override
        public void resetTask() {
            this.golem.setHoldingRose(false);
            if (this.tick == 0 && this.entity instanceof EntityCopperGolem && ((EntityCopperGolem) this.entity).getAntenna().isEmpty()
                    && box().intersects(this.entity.getEntityBoundingBox())) {
                ((EntityCopperGolem) this.entity).setAntenna(new ItemStack(Blocks.RED_FLOWER, 1, 0));
            }
            this.entity = null;
        }

        @Override
        public void updateTask() {
            if (this.entity != null) {
                this.golem.getLookHelper().setLookPositionWithEntity(this.entity, 30.0F, 30.0F);
            }
            this.tick--;
        }

        /** For the self-test: the target and the countdown. */
        public void force(net.minecraft.entity.EntityLivingBase target, int ticks) {
            this.entity = target;
            this.tick = ticks;
        }
    }
}
