package com.exiledradio.trickytrialsbackport.breeze;

import com.exiledradio.trickytrialsbackport.common.Content;
import com.exiledradio.trickytrialsbackport.common.WindBurst;

import net.minecraft.entity.EntityLivingBase;
import net.minecraft.world.World;

/**
 * The player's wind charge (thrown from the item or a dispenser): a small burst, radius 1.2, with 22%
 * more knockback than a plain explosion (1.21's 1.22 multiplier). Breezes are not pushed by it.
 */
public class EntityWindCharge extends EntityAbstractWindCharge {

    static final WindBurst.Settings BURST = new WindBurst.Settings(1.2F, 1.22F, false,
            Content.sound("entity.wind_charge.wind_burst"), true, e -> e instanceof EntityBreeze);

    public EntityWindCharge(World world) {
        super(world);
    }

    public EntityWindCharge(World world, EntityLivingBase owner) {
        super(world, owner);
    }

    public EntityWindCharge(World world, double x, double y, double z) {
        super(world, x, y, z);
    }

    @Override
    protected WindBurst.Settings burst() {
        return BURST;
    }
}
