package com.exiledradio.trickytrialsbackport.decor;

import net.minecraft.item.EnumRarity;
import net.minecraft.item.ItemRecord;
import net.minecraft.item.ItemStack;
import net.minecraft.util.SoundEvent;

/**
 * A 1.21 music disc on 1.12.2's record item, so the jukebox, "Now Playing", the tooltip line
 * ({@code item.record.<name>.desc}) and the record stat are vanilla's.
 *
 * <p>The disc carries its 1.21 comparator signal under the name the family's jukebox patch reads
 * ({@code comparatorSignal()}; {@code docs/CORE_REQUESTS.md} Decor-1). Until that patch is shared, 1.12.2's jukebox
 * counts the signal from the item id, as it does for every modded disc.
 */
public final class ItemDecorDisc extends ItemRecord {

    private final SoundEvent music;
    private final int signal;
    private final EnumRarity rarity;

    ItemDecorDisc(String name, SoundEvent sound, int signal, EnumRarity rarity) {
        super(name, sound);
        this.music = sound;
        this.signal = signal;
        this.rarity = rarity;
    }

    /** @return the comparator signal from a jukebox playing this disc (1.21's {@code comparator_output}) */
    public int comparatorSignal() {
        return this.signal;
    }

    /** @return the disc's sound event (vanilla's getter is client-only) */
    public SoundEvent music() {
        return this.music;
    }

    @Override
    public EnumRarity getRarity(ItemStack stack) {
        return this.rarity;
    }
}
