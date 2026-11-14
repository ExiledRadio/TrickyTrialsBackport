package com.exiledradio.trickytrialsbackport.chambers;

import com.exiledradio.backportcore.structure.ModernPalette;
import net.minecraft.block.Block;
import net.minecraft.block.BlockButton;
import net.minecraft.block.BlockLever;
import net.minecraft.block.state.IBlockState;
import net.minecraft.init.Blocks;
import net.minecraft.util.EnumFacing;

import javax.annotation.Nullable;
import java.util.Map;

/**
 * Buttons and levers of today's structure files: {@code face} (floor, wall, ceiling) and {@code facing} to 1.12.2's one
 * {@code facing}. Backport Core reads Minecraft's Flattening table backwards, which lists a floor or ceiling button only
 * as {@code facing=north}; a floor button facing east (the trial chambers' wall dispensers) scored the same against the
 * wall button facing east and came out as that, hanging on air. Here every combination maps: a floor button is
 * 1.12.2's {@code up}, a ceiling one {@code down}, a wall one its facing; a lever on the floor or ceiling is
 * {@code up_x}/{@code up_z} or {@code down_x}/{@code down_z} by the axis it faces along.
 */
final class FaceAttached {

    private FaceAttached() {
    }

    static void register() {
        ModernPalette.map("minecraft:oak_button", props -> button(Blocks.WOODEN_BUTTON, props));
        ModernPalette.map("minecraft:stone_button", props -> button(Blocks.STONE_BUTTON, props));
        ModernPalette.map("minecraft:lever", FaceAttached::lever);
    }

    @Nullable
    static IBlockState button(Block block, Map<String, String> props) {
        EnumFacing facing = attachedFacing(props);
        if (facing == null) {
            return null;
        }
        return block.getDefaultState().withProperty(BlockButton.FACING, facing)
                .withProperty(BlockButton.POWERED, "true".equals(props.get("powered")));
    }

    @Nullable
    static IBlockState lever(Map<String, String> props) {
        EnumFacing facing = horizontal(props.get("facing"));
        if (facing == null) {
            return null;
        }
        String face = props.getOrDefault("face", "wall");
        boolean x = facing.getAxis() == EnumFacing.Axis.X;
        BlockLever.EnumOrientation o;
        if ("floor".equals(face)) {
            o = x ? BlockLever.EnumOrientation.UP_X : BlockLever.EnumOrientation.UP_Z;
        } else if ("ceiling".equals(face)) {
            o = x ? BlockLever.EnumOrientation.DOWN_X : BlockLever.EnumOrientation.DOWN_Z;
        } else {
            o = BlockLever.EnumOrientation.forFacings(facing, facing);
        }
        return Blocks.LEVER.getDefaultState().withProperty(BlockLever.FACING, o)
                .withProperty(BlockLever.POWERED, "true".equals(props.get("powered")));
    }

    /** @return the facing 1.12.2's button keeps: up on the floor, down on the ceiling, the wall's own facing */
    @Nullable
    static EnumFacing attachedFacing(Map<String, String> props) {
        String face = props.getOrDefault("face", "wall");
        if ("floor".equals(face)) {
            return EnumFacing.UP;
        }
        if ("ceiling".equals(face)) {
            return EnumFacing.DOWN;
        }
        return horizontal(props.get("facing"));
    }

    @Nullable
    static EnumFacing horizontal(@Nullable String name) {
        EnumFacing f = name == null ? EnumFacing.NORTH : EnumFacing.byName(name);
        return f == null || f.getAxis() == EnumFacing.Axis.Y ? null : f;
    }
}
