package com.exiledradio.trickytrialsbackport.trial;

import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.network.NetworkManager;
import net.minecraft.network.play.server.SPacketUpdateTileEntity;
import net.minecraft.stats.StatList;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.ITickable;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.world.World;
import net.minecraft.world.WorldServer;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * The vault: 1.21's {@code VaultBlockEntity} with its server, shared and client data, and the
 * {@code VaultState} machine, with the same numbers and NBT names.
 *
 * <ul>
 *   <li>Inactive (light 6), it lights up (12) when a player who hasn't opened it comes within 4 blocks, and
 *       goes out when none is left within 4.5; while lit it shows an item from its loot table, a new one
 *       every second.</li>
 *   <li>Its key (a trial key; an ominous trial key for the ominous vault) unlocks it once per player, ever:
 *       it rolls its loot table with the player's luck and ejects the items one per second. The last 128
 *       players are remembered.</li>
 * </ul>
 */
public class TileEntityVault extends TileEntity implements ITickable {

    private static final int MAX_REWARD_PLAYERS = 128;
    private static final int UNLOCKING_DELAY_TICKS = 14;
    private static final int INSERT_FAIL_SOUND_BUFFER_TICKS = 15;

    private VaultSettings config = VaultSettings.normal();
    private boolean ominous;

    // Server data.
    private final Set<UUID> rewardedPlayers = new LinkedHashSet<UUID>();
    private long stateUpdatingResumesAt;
    private final List<ItemStack> itemsToEject = new ArrayList<ItemStack>();
    private long lastInsertFailTimestamp;
    private int totalEjectionsNeeded;
    private boolean serverDirty;

    // Shared data (sent to clients).
    private ItemStack displayItem = ItemStack.EMPTY;
    private Set<UUID> connectedPlayers = new LinkedHashSet<UUID>();
    private double connectedParticlesRange = 4.5;
    private boolean sharedDirty;

    // Client data.
    public float currentSpin;
    public float previousSpin;

    @Nullable
    private List<? extends EntityPlayer> onlyPlayers;

    public VaultSettings getConfig() {
        return this.config;
    }

    /** Sets what the vault gives and takes ({@link VaultSettings#normal}, {@link VaultSettings#ominous}). */
    public void setConfig(VaultSettings config) {
        this.config = config;
        markDirty();
    }

    /** For tests: only these count as players nearby. */
    public void onlySelectPlayers(@Nullable List<? extends EntityPlayer> players) {
        this.onlyPlayers = players;
    }

    public ItemStack getDisplayItem() {
        return this.displayItem;
    }

    public Set<UUID> getConnectedPlayers() {
        return this.connectedPlayers;
    }

    public double connectedParticlesRange() {
        return this.connectedParticlesRange;
    }

    public Set<UUID> getRewardedPlayers() {
        return this.rewardedPlayers;
    }

    public List<ItemStack> getItemsToEject() {
        return this.itemsToEject;
    }

    VaultPhase getPhase() {
        IBlockState state = this.world.getBlockState(this.pos);
        return state.getBlock() instanceof BlockVault ? state.getValue(BlockVault.STATE) : VaultPhase.INACTIVE;
    }

    /**
     * Whether this is an ominous vault. The block's metadata holds its facing and phase; the
     * {@code ominous} state (fixed for a vault's life) is kept here and shown by the block's actual state.
     */
    public boolean ominousFlag() {
        return this.ominous;
    }

    public void setOminous(boolean ominous) {
        if (this.ominous != ominous) {
            this.ominous = ominous;
            markDirty();
            if (this.world != null) {
                IBlockState state = this.world.getBlockState(this.pos);
                this.world.notifyBlockUpdate(this.pos, state, state, 3);
            }
        }
    }

    /** As trial chambers' ominous vault: the ominous state, key and loot table. */
    public void makeOminous() {
        setConfig(VaultSettings.ominous());
        setOminous(true);
    }

    // ------------------------------------------------------------------------------------------
    // Ticking
    // ------------------------------------------------------------------------------------------

    @Override
    public void update() {
        if (this.world == null || !(this.world.getBlockState(this.pos).getBlock() instanceof BlockVault)) {
            return;
        }
        if (this.world.isRemote) {
            this.previousSpin = this.currentSpin;
            this.currentSpin = MathHelper.wrapDegrees(this.currentSpin + 10.0F);
            com.exiledradio.trickytrialsbackport.trial.client.TrialFx.tickVault(this, this.ominous);
            return;
        }
        WorldServer world = (WorldServer) this.world;
        IBlockState state = world.getBlockState(this.pos);
        VaultPhase phase = state.getValue(BlockVault.STATE);
        if (world.getTotalWorldTime() % 20L == 0L && phase == VaultPhase.ACTIVE) {
            cycleDisplayItemFromLootTable(world, phase);
        }
        IBlockState after = state;
        if (world.getTotalWorldTime() >= this.stateUpdatingResumesAt) {
            after = state.withProperty(BlockVault.STATE, tickAndGetNext(world, phase));
            if (after != state) {
                setVaultState(world, state, after);
            }
        }
        if (this.serverDirty || this.sharedDirty) {
            markDirty();
            if (this.sharedDirty) {
                IBlockState now = world.getBlockState(this.pos);
                world.notifyBlockUpdate(this.pos, now, now, 2);
            }
            this.serverDirty = false;
            this.sharedDirty = false;
        }
    }

    private VaultPhase tickAndGetNext(WorldServer world, VaultPhase phase) {
        switch (phase) {
            case INACTIVE:
                return updateStateForConnectedPlayers(world, this.config.activationRange);
            case ACTIVE:
                return updateStateForConnectedPlayers(world, this.config.deactivationRange);
            case UNLOCKING:
                pauseStateUpdatingUntil(world.getTotalWorldTime() + 20L);
                return VaultPhase.EJECTING;
            case EJECTING:
            default:
                if (this.itemsToEject.isEmpty()) {
                    this.totalEjectionsNeeded = 0;
                    this.serverDirty = true;
                    return updateStateForConnectedPlayers(world, this.config.deactivationRange);
                }
                float progress = ejectionProgress();
                ejectResultItem(world, popNextItemToEject(), progress);
                setDisplayItem(nextItemToEject());
                pauseStateUpdatingUntil(world.getTotalWorldTime() + 20L);
                return VaultPhase.EJECTING;
        }
    }

    private VaultPhase updateStateForConnectedPlayers(WorldServer world, double range) {
        updateConnectedPlayersWithinRange(world, range);
        pauseStateUpdatingUntil(world.getTotalWorldTime() + 20L);
        return this.connectedPlayers.isEmpty() ? VaultPhase.INACTIVE : VaultPhase.ACTIVE;
    }

    private void setVaultState(WorldServer world, IBlockState from, IBlockState to) {
        VaultPhase old = from.getValue(BlockVault.STATE);
        VaultPhase now = to.getValue(BlockVault.STATE);
        world.setBlockState(this.pos, to, 3);
        boolean ominous = this.ominous;
        // onExit
        if (old == VaultPhase.EJECTING) {
            play(world, "block.vault.close_shutter");
        }
        // onEnter
        switch (now) {
            case INACTIVE:
                setDisplayItem(ItemStack.EMPTY);
                TrialMessage.send(world, this.pos, TrialMessage.VAULT_DEACTIVATE, ominous ? 1 : 0);
                break;
            case ACTIVE:
                if (this.displayItem.isEmpty()) {
                    cycleDisplayItemFromLootTable(world, now);
                }
                TrialMessage.send(world, this.pos, TrialMessage.VAULT_ACTIVATE, ominous ? 1 : 0);
                break;
            case UNLOCKING:
                play(world, "block.vault.insert_item");
                break;
            case EJECTING:
            default:
                play(world, "block.vault.open_shutter");
                break;
        }
    }

    private void play(World world, String event) {
        world.playSound(null, this.pos, TrialContent.sound(event), SoundCategory.BLOCKS, 1.0F, 1.0F);
    }

    private void cycleDisplayItemFromLootTable(WorldServer world, VaultPhase phase) {
        if (!canEjectReward(phase)) {
            setDisplayItem(ItemStack.EMPTY);
            return;
        }
        String table = this.config.overrideLootTableToDisplay != null ? this.config.overrideLootTableToDisplay : this.config.lootTable;
        List<ItemStack> items = ModernLoot.roll(world, table, world.rand, 0.0F);
        setDisplayItem(items.isEmpty() ? ItemStack.EMPTY : items.get(world.rand.nextInt(items.size())));
    }

    private boolean canEjectReward(VaultPhase phase) {
        return !this.config.keyItem.isEmpty() && phase != VaultPhase.INACTIVE;
    }

    private void ejectResultItem(WorldServer world, ItemStack stack, float progress) {
        TileEntityTrialSpawner.spawnItemUp(world, stack, this.pos);
        TrialMessage.send(world, this.pos, TrialMessage.VAULT_EJECT_ITEM, 0);
        world.playSound(null, this.pos, TrialContent.sound("block.vault.eject_item"), SoundCategory.BLOCKS, 1.0F, 0.8F + 0.4F * progress);
    }

    // ------------------------------------------------------------------------------------------
    // Keys
    // ------------------------------------------------------------------------------------------

    /** 1.21's {@code tryInsertKey}, from a right click with an item while active. */
    public void tryInsertKey(WorldServer world, EntityPlayer player, ItemStack held) {
        VaultPhase phase = getPhase();
        if (!canEjectReward(phase)) {
            return;
        }
        if (!isValidToInsert(held)) {
            playInsertFailSound(world, "block.vault.insert_item_fail");
        } else if (this.rewardedPlayers.contains(player.getUniqueID())) {
            playInsertFailSound(world, "block.vault.reject_rewarded_player");
        } else {
            List<ItemStack> items = ModernLoot.roll(world, this.config.lootTable, world.rand, player.getLuck());
            if (!items.isEmpty()) {
                player.addStat(StatList.getObjectUseStats(held.getItem()));
                if (!player.capabilities.isCreativeMode) {
                    held.shrink(this.config.keyItem.getCount());
                }
                unlock(world, items);
                addToRewardedPlayers(player);
                updateConnectedPlayersWithinRange(world, this.config.deactivationRange);
            }
        }
    }

    private boolean isValidToInsert(ItemStack held) {
        ItemStack key = this.config.keyItem;
        return !held.isEmpty() && held.getItem() == key.getItem() && held.getMetadata() == key.getMetadata()
                && ItemStack.areItemStackTagsEqual(held, key) && held.getCount() >= key.getCount();
    }

    private void playInsertFailSound(WorldServer world, String event) {
        if (world.getTotalWorldTime() >= this.lastInsertFailTimestamp + INSERT_FAIL_SOUND_BUFFER_TICKS) {
            play(world, event);
            this.lastInsertFailTimestamp = world.getTotalWorldTime();
        }
    }

    private void unlock(WorldServer world, List<ItemStack> items) {
        this.itemsToEject.clear();
        this.itemsToEject.addAll(items);
        this.totalEjectionsNeeded = this.itemsToEject.size();
        this.serverDirty = true;
        setDisplayItem(nextItemToEject());
        pauseStateUpdatingUntil(world.getTotalWorldTime() + UNLOCKING_DELAY_TICKS);
        IBlockState state = world.getBlockState(this.pos);
        setVaultState(world, state, state.withProperty(BlockVault.STATE, VaultPhase.UNLOCKING));
    }

    private void addToRewardedPlayers(EntityPlayer player) {
        this.rewardedPlayers.add(player.getUniqueID());
        if (this.rewardedPlayers.size() > MAX_REWARD_PLAYERS) {
            Iterator<UUID> it = this.rewardedPlayers.iterator();
            it.next();
            it.remove();
        }
        this.serverDirty = true;
    }

    private void pauseStateUpdatingUntil(long time) {
        this.stateUpdatingResumesAt = time;
        this.serverDirty = true;
    }

    private ItemStack nextItemToEject() {
        return this.itemsToEject.isEmpty() ? ItemStack.EMPTY : this.itemsToEject.get(this.itemsToEject.size() - 1);
    }

    private ItemStack popNextItemToEject() {
        if (this.itemsToEject.isEmpty()) {
            return ItemStack.EMPTY;
        }
        this.serverDirty = true;
        return this.itemsToEject.remove(this.itemsToEject.size() - 1);
    }

    private float ejectionProgress() {
        if (this.totalEjectionsNeeded == 1) {
            return 1.0F;
        }
        // 1 - inverseLerp(items left, 1, total)
        return 1.0F - (this.itemsToEject.size() - 1.0F) / (this.totalEjectionsNeeded - 1.0F);
    }

    private void setDisplayItem(ItemStack stack) {
        if (!ItemStack.areItemStacksEqual(this.displayItem, stack)) {
            this.displayItem = stack.copy();
            this.sharedDirty = true;
        }
    }

    /** Players nearby who haven't opened the vault (1.21's {@code INCLUDING_CREATIVE_PLAYERS}, no sight needed). */
    private void updateConnectedPlayersWithinRange(WorldServer world, double range) {
        Set<UUID> found = new LinkedHashSet<UUID>();
        List<? extends EntityPlayer> players = this.onlyPlayers != null ? this.onlyPlayers : world.playerEntities;
        for (EntityPlayer p : players) {
            if (!p.isSpectator() && p.getPosition().distanceSq(this.pos) < range * range && !this.rewardedPlayers.contains(p.getUniqueID())) {
                found.add(p.getUniqueID());
            }
        }
        if (!this.connectedPlayers.equals(found)) {
            this.connectedPlayers = found;
            this.sharedDirty = true;
        }
    }

    // ------------------------------------------------------------------------------------------
    // NBT
    // ------------------------------------------------------------------------------------------

    @Override
    public void readFromNBT(NBTTagCompound tag) {
        super.readFromNBT(tag);
        if (tag.hasKey("server_data", 10)) {
            NBTTagCompound s = tag.getCompoundTag("server_data");
            Json.readUuids(s, "rewarded_players", this.rewardedPlayers);
            this.stateUpdatingResumesAt = s.getLong("state_updating_resumes_at");
            this.itemsToEject.clear();
            NBTTagList items = s.getTagList("items_to_eject", 10);
            for (int i = 0; i < items.tagCount(); i++) {
                ItemStack stack = VaultSettings.stack(items.getCompoundTagAt(i));
                if (!stack.isEmpty()) {
                    this.itemsToEject.add(stack);
                }
            }
            this.totalEjectionsNeeded = s.getInteger("total_ejections_needed");
        }
        // 1.21's own block entity data (a structure file's) has no "ominous": the block state says it, and
        // createTileEntity already set it from that state, so it is kept.
        if (tag.hasKey("ominous")) {
            this.ominous = tag.getBoolean("ominous");
        }
        this.config = tag.hasKey("config", 10) ? VaultSettings.read(tag.getCompoundTag("config")) : VaultSettings.normal();
        // Trial chambers generated before that fix saved their ominous vaults as normal ones with the ominous key and
        // table; such a vault is the ominous vault.
        if (!this.ominous && this.config.isOminousReward()) {
            this.ominous = true;
        }
        if (tag.hasKey("shared_data", 10)) {
            readShared(tag.getCompoundTag("shared_data"));
        }
    }

    private void readShared(NBTTagCompound s) {
        this.displayItem = s.hasKey("display_item", 10) ? VaultSettings.stack(s.getCompoundTag("display_item")) : ItemStack.EMPTY;
        Set<UUID> connected = new LinkedHashSet<UUID>();
        Json.readUuids(s, "connected_players", connected);
        this.connectedPlayers = connected;
        this.connectedParticlesRange = s.hasKey("connected_particles_range") ? s.getDouble("connected_particles_range") : 4.5;
    }

    private NBTTagCompound writeShared() {
        NBTTagCompound s = new NBTTagCompound();
        if (!this.displayItem.isEmpty()) {
            s.setTag("display_item", this.displayItem.writeToNBT(new NBTTagCompound()));
        }
        s.setTag("connected_players", Json.uuids(this.connectedPlayers));
        s.setDouble("connected_particles_range", this.connectedParticlesRange);
        return s;
    }

    @Override
    public NBTTagCompound writeToNBT(NBTTagCompound tag) {
        super.writeToNBT(tag);
        tag.setBoolean("ominous", this.ominous);
        tag.setTag("config", this.config.write());
        tag.setTag("shared_data", writeShared());
        NBTTagCompound s = new NBTTagCompound();
        s.setTag("rewarded_players", Json.uuids(this.rewardedPlayers));
        s.setLong("state_updating_resumes_at", this.stateUpdatingResumesAt);
        NBTTagList items = new NBTTagList();
        for (ItemStack stack : this.itemsToEject) {
            items.appendTag(stack.writeToNBT(new NBTTagCompound()));
        }
        s.setTag("items_to_eject", items);
        s.setInteger("total_ejections_needed", this.totalEjectionsNeeded);
        tag.setTag("server_data", s);
        return tag;
    }

    @Override
    public NBTTagCompound getUpdateTag() {
        NBTTagCompound tag = super.writeToNBT(new NBTTagCompound());
        tag.setBoolean("ominous", this.ominous);
        tag.setTag("shared_data", writeShared());
        return tag;
    }

    @Override
    public SPacketUpdateTileEntity getUpdatePacket() {
        return new SPacketUpdateTileEntity(this.pos, 0, getUpdateTag());
    }

    @Override
    public void handleUpdateTag(NBTTagCompound tag) {
        super.readFromNBT(tag);
        readClient(tag);
    }

    @Override
    public void onDataPacket(NetworkManager net, SPacketUpdateTileEntity packet) {
        readClient(packet.getNbtCompound());
    }

    private void readClient(NBTTagCompound tag) {
        boolean was = this.ominous;
        this.ominous = tag.getBoolean("ominous");
        readShared(tag.getCompoundTag("shared_data"));
        if (was != this.ominous && this.world != null) {
            this.world.markBlockRangeForRenderUpdate(this.pos, this.pos);
        }
    }

    @Override
    public boolean shouldRefresh(World world, BlockPos pos, IBlockState oldState, IBlockState newState) {
        return oldState.getBlock() != newState.getBlock();
    }
}
