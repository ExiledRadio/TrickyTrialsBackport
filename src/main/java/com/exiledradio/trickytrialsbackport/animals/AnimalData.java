package com.exiledradio.trickytrialsbackport.animals;

import com.exiledradio.trickytrialsbackport.common.Content;

import net.minecraft.entity.Entity;
import net.minecraft.nbt.NBTBase;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.CapabilityInject;
import net.minecraftforge.common.capabilities.CapabilityManager;
import net.minecraftforge.common.capabilities.ICapabilityProvider;

import javax.annotation.Nullable;

/**
 * What 1.21.5 added to a cow, pig, chicken or wolf: its farm variant and, on a wolf, its voice. Kept in a capability
 * that is not saved by Forge: the entity's own NBT carries vanilla's {@code variant} and {@code sound_variant}
 * (patch P16, {@link AnimalsHooks#read}), so {@code /summon} and spawn egg NBT read as in 1.21.5.
 */
public final class AnimalData {

    static final ResourceLocation KEY = Content.id("animal");

    @CapabilityInject(AnimalData.class)
    public static Capability<AnimalData> CAP = null;

    FarmVariant variant = FarmVariant.TEMPERATE;
    WolfVoice voice = WolfVoice.CLASSIC;

    static void register() {
        CapabilityManager.INSTANCE.register(AnimalData.class, new Capability.IStorage<AnimalData>() {
            @Nullable
            @Override
            public NBTBase writeNBT(Capability<AnimalData> capability, AnimalData instance, EnumFacing side) {
                return null;
            }

            @Override
            public void readNBT(Capability<AnimalData> capability, AnimalData instance, EnumFacing side, NBTBase nbt) {
            }
        }, AnimalData::new);
    }

    /** @return the entity's data, or null for an entity that has none (or before the capability exists) */
    @Nullable
    public static AnimalData of(@Nullable Entity entity) {
        return entity == null || CAP == null ? null : entity.getCapability(CAP, null);
    }

    static final class Provider implements ICapabilityProvider {
        private final AnimalData data = new AnimalData();

        @Override
        public boolean hasCapability(Capability<?> capability, @Nullable EnumFacing facing) {
            return capability == CAP;
        }

        @Nullable
        @Override
        public <T> T getCapability(Capability<T> capability, @Nullable EnumFacing facing) {
            return capability == CAP ? CAP.cast(this.data) : null;
        }
    }
}
