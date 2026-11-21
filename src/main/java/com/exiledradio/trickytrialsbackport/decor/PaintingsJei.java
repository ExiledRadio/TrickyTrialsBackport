package com.exiledradio.trickytrialsbackport.decor;

import mezz.jei.api.IModPlugin;
import mezz.jei.api.ISubtypeRegistry;
import mezz.jei.api.JEIPlugin;
import net.minecraft.init.Items;

/**
 * JEI tells items apart by item and metadata only, so the variant paintings ({@link PaintingItems}) would show as one
 * entry. The variant is their subtype; a random painting has none. Only JEI loads this class.
 */
@JEIPlugin
public final class PaintingsJei implements IModPlugin {

    @Override
    public void registerItemSubtypes(ISubtypeRegistry registry) {
        if (!DecorContent.enabled()) {
            return;
        }
        registry.registerSubtypeInterpreter(Items.PAINTING, stack -> {
            PaintingItems.Entry entry = PaintingItems.of(stack);
            return entry == null ? ISubtypeRegistry.ISubtypeInterpreter.NONE : entry.name;
        });
    }
}
