package com.exiledradio.trickytrialsbackport.animals;

import com.exiledradio.trickytrialsbackport.common.Content;

import net.minecraft.init.SoundEvents;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.SoundEvent;

import javax.annotation.Nullable;
import java.util.Random;

/**
 * 1.21.5's seven {@code wolf_sound_variant}s. Classic is 1.12.2's own wolf sounds; the other six are 1.21.5's
 * {@code entity.wolf_<name>.*} events, ours under this mod's namespace.
 */
public enum WolfVoice {
    CLASSIC("classic", ""),
    PUGLIN("puglin", "_puglin"),
    SAD("sad", "_sad"),
    ANGRY("angry", "_angry"),
    GRUMPY("grumpy", "_grumpy"),
    BIG("big", "_big"),
    CUTE("cute", "_cute");

    /** The six kinds of sound a voice has, in 1.21.5's order. */
    static final String[] KINDS = {"ambient", "death", "growl", "hurt", "pant", "whine"};

    public final ResourceLocation id;
    private final String suffix;
    /** ambient, death, growl, hurt, pant, whine; null until {@link #registerSounds}. */
    private SoundEvent[] sounds;

    WolfVoice(String name, String suffix) {
        this.id = new ResourceLocation("minecraft", name);
        this.suffix = suffix;
    }

    private static final WolfVoice[] VALUES = values();

    public static WolfVoice byOrdinal(int i) {
        return i >= 0 && i < VALUES.length ? VALUES[i] : CLASSIC;
    }

    @Nullable
    public static WolfVoice byId(String id) {
        ResourceLocation key = new ResourceLocation(id);
        for (WolfVoice v : VALUES) {
            if (v.id.equals(key)) {
                return v;
            }
        }
        return null;
    }

    /** 1.21.5's {@code pickRandomSoundVariant}: any of the seven, evenly. */
    public static WolfVoice random(Random rand) {
        return VALUES[rand.nextInt(VALUES.length)];
    }

    /** @return the sound event name of one of this voice's kinds, such as {@code entity.wolf_puglin.growl} */
    public String event(String kind) {
        return "entity.wolf" + this.suffix + "." + kind;
    }

    /** Makes our 36 events (classic uses 1.12.2's). From the stream's pre-init. */
    static void registerSounds() {
        for (WolfVoice v : VALUES) {
            if (v == CLASSIC) {
                v.sounds = new SoundEvent[]{SoundEvents.ENTITY_WOLF_AMBIENT, SoundEvents.ENTITY_WOLF_DEATH,
                        SoundEvents.ENTITY_WOLF_GROWL, SoundEvents.ENTITY_WOLF_HURT, SoundEvents.ENTITY_WOLF_PANT,
                        SoundEvents.ENTITY_WOLF_WHINE};
            } else {
                v.sounds = new SoundEvent[KINDS.length];
                for (int k = 0; k < KINDS.length; k++) {
                    v.sounds[k] = Content.sound(v.event(KINDS[k]));
                }
            }
        }
    }

    /**
     * @return this voice's sound for one of 1.12.2's wolf sounds (ambient, death, growl, hurt, pant, whine), or the
     * sound itself when it is none of those (shake, step) or the sounds are not made
     */
    public SoundEvent swap(SoundEvent classic) {
        if (this == CLASSIC || this.sounds == null || classic == null) {
            return classic;
        }
        SoundEvent[] base = CLASSIC.sounds;
        for (int k = 0; k < base.length; k++) {
            if (base[k] == classic) {
                return this.sounds[k];
            }
        }
        return classic;
    }
}
