package com.exiledradio.trickytrialsbackport.animals.client;

import net.minecraft.client.model.ModelChicken;

/**
 * 1.21.5's {@code ColdChickenModel}: 1.12.2's chicken (the same parts and texture layout as 1.21.5's
 * {@code ChickenModel}, 64x32) with its two extra boxes, a fluffed crest on the head and a tail on the body.
 */
public final class ModelColdChicken extends ModelChicken {

    public ModelColdChicken() {
        this.head.setTextureOffset(44, 0).addBox(-3.0F, -7.0F, -2.015F, 6, 3, 4, 0.0F);
        this.body.setTextureOffset(38, 9).addBox(0.0F, 3.0F, -1.0F, 0, 3, 5, 0.0F);
    }
}
