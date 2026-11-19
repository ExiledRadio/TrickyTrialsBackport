package com.exiledradio.trickytrialsbackport.animals;

import com.exiledradio.trickytrialsbackport.ModConfig;
import com.exiledradio.trickytrialsbackport.network.NetworkHandler;

import net.minecraft.entity.Entity;
import net.minecraft.entity.passive.EntityChicken;
import net.minecraft.entity.passive.EntityCow;
import net.minecraft.entity.passive.EntityMooshroom;
import net.minecraft.entity.passive.EntityPig;
import net.minecraft.entity.passive.EntitySheep;
import net.minecraft.entity.passive.EntityWolf;
import net.minecraft.item.EnumDyeColor;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.WorldServer;
import net.minecraftforge.common.util.Constants;

import javax.annotation.Nullable;
import java.util.Random;

/** The variants' rules: which animals have one, picking by biome, saving, breeding, syncing, sheep colors. */
public final class FarmAnimals {

    public enum Kind { COW, PIG, CHICKEN }

    private FarmAnimals() {
    }

    /** @return true for the entities that carry {@link AnimalData}: cows (not mooshrooms), pigs, chickens, wolves */
    static boolean carries(Entity e) {
        return (e instanceof EntityCow && !(e instanceof EntityMooshroom)) || e instanceof EntityPig
                || e instanceof EntityChicken || e instanceof EntityWolf;
    }

    /** @return the kind of farm animal, whatever its switch says, or null */
    @Nullable
    public static Kind kindOf(Entity e) {
        if (e instanceof EntityMooshroom) {
            return null;
        }
        if (e instanceof EntityCow) {
            return Kind.COW;
        }
        if (e instanceof EntityPig) {
            return Kind.PIG;
        }
        if (e instanceof EntityChicken) {
            return Kind.CHICKEN;
        }
        return null;
    }

    /** @return whether this kind's variants are switched on */
    public static boolean on(Kind kind) {
        if (!ModConfig.animals.enabled) {
            return false;
        }
        switch (kind) {
            case COW:
                return ModConfig.animals.cowVariants;
            case PIG:
                return ModConfig.animals.pigVariants;
            default:
                return ModConfig.animals.chickenVariants;
        }
    }

    public static FarmVariant variant(Entity e) {
        AnimalData d = AnimalData.of(e);
        return d == null ? FarmVariant.TEMPERATE : d.variant;
    }

    /** Sets an animal's variant; on a server, every player tracking it is told. */
    public static void setVariant(Entity e, FarmVariant v) {
        AnimalData d = AnimalData.of(e);
        if (d == null || v == null) {
            return;
        }
        d.variant = v;
        if (e.world instanceof WorldServer) {
            ((WorldServer) e.world).getEntityTracker().sendToTracking(e,
                    NetworkHandler.channel.getPacketFrom(new AnimalVariantMessage(e.getEntityId(), v)));
        }
    }

    public static WolfVoice voice(Entity e) {
        AnimalData d = AnimalData.of(e);
        return d == null ? WolfVoice.CLASSIC : d.voice;
    }

    public static void setVoice(Entity e, WolfVoice v) {
        AnimalData d = AnimalData.of(e);
        if (d != null && v != null) {
            d.voice = v;
        }
    }

    /** 1.21.5's {@code finalizeSpawn}: the variant of the biome at the animal's block, and a wolf's random voice. */
    static void initialSpawn(Entity e) {
        if (e.world == null || e.world.isRemote || !ModConfig.animals.enabled) {
            return;
        }
        Kind kind = kindOf(e);
        if (kind != null && on(kind)) {
            setVariant(e, FarmVariant.forBiome(e.world.getBiome(new BlockPos(e))));
        }
        if (e instanceof EntityWolf && ModConfig.animals.wolfSoundVariants) {
            setVoice(e, WolfVoice.random(e.world.rand));
        }
    }

    /** Vanilla's {@code VariantUtils.readVariant} and the wolf's {@code sound_variant}: an unknown id changes nothing. */
    static void read(Entity e, NBTTagCompound tag) {
        AnimalData d = AnimalData.of(e);
        if (d == null) {
            return;
        }
        if (kindOf(e) != null && tag.hasKey("variant", Constants.NBT.TAG_STRING)) {
            FarmVariant v = FarmVariant.byId(tag.getString("variant"));
            if (v != null) {
                setVariant(e, v);
            }
        }
        if (e instanceof EntityWolf && tag.hasKey("sound_variant", Constants.NBT.TAG_STRING)) {
            WolfVoice v = WolfVoice.byId(tag.getString("sound_variant"));
            if (v != null) {
                d.voice = v;
            }
        }
    }

    static void write(Entity e, NBTTagCompound tag) {
        AnimalData d = AnimalData.of(e);
        if (d == null) {
            return;
        }
        if (kindOf(e) != null) {
            tag.setString("variant", d.variant.id.toString());
        }
        if (e instanceof EntityWolf) {
            tag.setString("sound_variant", d.voice.id.toString());
        }
    }

    /** 1.21.5's {@code getBreedOffspring}: the baby takes either parent's variant, evenly; a puppy gets a random voice. */
    static void bred(Entity a, Entity b, Entity child, Random rand) {
        Kind kind = kindOf(child);
        if (kind != null && on(kind) && kindOf(a) == kind && kindOf(b) == kind) {
            setVariant(child, rand.nextBoolean() ? variant(a) : variant(b));
        }
        if (child instanceof EntityWolf && ModConfig.animals.wolfSoundVariants) {
            setVoice(child, WolfVoice.random(rand));
        }
    }

    /** 1.21.5's {@code SheepColorSpawnRules}, for warm and cold biomes; temperate is 1.12.2's own (the same weights). */
    static void sheepSpawn(EntitySheep sheep) {
        if (sheep.world == null || sheep.world.isRemote || !ModConfig.animals.enabled || !ModConfig.animals.sheepColors) {
            return;
        }
        FarmVariant climate = FarmVariant.forBiome(sheep.world.getBiome(new BlockPos(sheep)));
        if (climate != FarmVariant.TEMPERATE) {
            sheep.setFleeceColor(sheepColor(climate, sheep.world.rand));
        }
    }

    /** Weights out of 100: three singles of 5, one of 3, and 82 for the common color (1 in 500 of those pink). */
    public static EnumDyeColor sheepColor(FarmVariant climate, Random rand) {
        EnumDyeColor[] singles;
        EnumDyeColor common;
        switch (climate) {
            case WARM:
                singles = new EnumDyeColor[]{EnumDyeColor.GRAY, EnumDyeColor.SILVER, EnumDyeColor.WHITE, EnumDyeColor.BLACK};
                common = EnumDyeColor.BROWN;
                break;
            case COLD:
                singles = new EnumDyeColor[]{EnumDyeColor.SILVER, EnumDyeColor.GRAY, EnumDyeColor.WHITE, EnumDyeColor.BROWN};
                common = EnumDyeColor.BLACK;
                break;
            default:
                singles = new EnumDyeColor[]{EnumDyeColor.BLACK, EnumDyeColor.GRAY, EnumDyeColor.SILVER, EnumDyeColor.BROWN};
                common = EnumDyeColor.WHITE;
                break;
        }
        int roll = rand.nextInt(100);
        if (roll < 5) {
            return singles[0];
        }
        if (roll < 10) {
            return singles[1];
        }
        if (roll < 15) {
            return singles[2];
        }
        if (roll < 18) {
            return singles[3];
        }
        return rand.nextInt(500) == 0 ? EnumDyeColor.PINK : common;
    }
}
