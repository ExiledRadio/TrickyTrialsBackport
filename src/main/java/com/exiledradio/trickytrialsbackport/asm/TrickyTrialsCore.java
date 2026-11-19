package com.exiledradio.trickytrialsbackport.asm;

import net.minecraftforge.fml.relauncher.IFMLLoadingPlugin;

import javax.annotation.Nullable;
import java.util.Map;

/**
 * This mod's own small coremod: the bytecode patches 1.21's behavior needs where 1.12.2 has no hook. One transformer
 * per patch, each documented in {@code docs/PATCHES.md}. No MixinBooter.
 *
 * <p>Named in the jar's manifest ({@code FMLCorePlugin}, with {@code FMLCorePluginContainsFMLMod}) and, in the dev
 * environment, by {@code -Dfml.coreMods.load}. A transformer only rewrites a call or adds a call to a hook in the
 * mod's own packages; the hook keeps 1.12.2's behavior while its stream is off.
 */
@IFMLLoadingPlugin.Name("Tricky Trials Backport Core")
@IFMLLoadingPlugin.MCVersion("1.12.2")
@IFMLLoadingPlugin.TransformerExclusions("com.exiledradio.trickytrialsbackport.asm.")
@IFMLLoadingPlugin.SortingIndex(1001)
public final class TrickyTrialsCore implements IFMLLoadingPlugin {

    @Override
    public String[] getASMTransformerClass() {
        return new String[] {
                CactusFlowerTransformer.class.getName(),
                DeadBushSoilTransformer.class.getName(),
                DesertSoundsTransformer.class.getName(),
                StrafeSpeedTransformer.class.getName(),
                MechanicsTransformer.class.getName(),
                AnimalsTransformer.class.getName(),
                MobArmorTransformer.class.getName(),
                SpearTransformer.class.getName(),
        };
    }

    @Nullable
    @Override
    public String getModContainerClass() {
        return null;
    }

    @Nullable
    @Override
    public String getSetupClass() {
        return null;
    }

    @Override
    public void injectData(Map<String, Object> data) {
    }

    @Nullable
    @Override
    public String getAccessTransformerClass() {
        return null;
    }
}
