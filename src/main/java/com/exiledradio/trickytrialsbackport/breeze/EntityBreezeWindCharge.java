package com.exiledradio.trickytrialsbackport.breeze;

import com.exiledradio.trickytrialsbackport.common.Content;
import com.exiledradio.trickytrialsbackport.common.WindBurst;

import net.minecraft.entity.EntityLivingBase;
import net.minecraft.world.World;

/**
 * The breeze's wind charge: a large burst, radius 3, plain explosion knockback, the breeze's own burst
 * sound. Breezes are not pushed by it; deflected back at a breeze it still hits (<i>Blowback</i>).
 */
public class EntityBreezeWindCharge extends EntityAbstractWindCharge {

    static final WindBurst.Settings BURST = new WindBurst.Settings(3.0F, 1.0F, true,
            Content.sound("entity.breeze.wind_burst"), true, e -> e instanceof EntityBreeze);

    public EntityBreezeWindCharge(World world) {
        super(world);
    }

    public EntityBreezeWindCharge(World world, EntityLivingBase owner) {
        super(world, owner);
    }

    @Override
    protected WindBurst.Settings burst() {
        return BURST;
    }
}
