package com.exiledradio.trickytrialsbackport.lookfeel.client;

import com.exiledradio.trickytrialsbackport.lookfeel.LookAndFeel;

import net.minecraft.block.material.Material;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.RenderGlobal;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.world.World;
import net.minecraftforge.client.event.DrawBlockHighlightEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

/**
 * 1.21.2's High Contrast Block Outlines ({@code LevelRenderer.renderBlockOutline}): a black outline seven pixels
 * wide under the targeted block's outline, which turns from 40% black to opaque {@code 0x57FFE1}.
 *
 * <p>Drawn as 1.12.2's {@code RenderGlobal.drawSelectionBox} draws the outline (the same box, grown by 0.002),
 * in place of it while the option is on.
 */
@SideOnly(Side.CLIENT)
public final class HighContrastOutline {

    /** For a look in game and the log: outlines drawn. */
    public static volatile int drawn;

    @SubscribeEvent
    public void highlight(DrawBlockHighlightEvent event) {
        RayTraceResult target = event.getTarget();
        if (event.getSubID() != 0 || target == null || target.typeOfHit != RayTraceResult.Type.BLOCK
                || !LookAndFeel.HIGH_CONTRAST_OUTLINE.get()) {
            return;
        }
        EntityPlayer player = event.getPlayer();
        World world = player.world;
        BlockPos pos = target.getBlockPos();
        IBlockState state = world.getBlockState(pos);
        event.setCanceled(true);
        if (state.getMaterial() == Material.AIR || !world.getWorldBorder().contains(pos)) {
            return;
        }
        float partial = event.getPartialTicks();
        double x = player.lastTickPosX + (player.posX - player.lastTickPosX) * partial;
        double y = player.lastTickPosY + (player.posY - player.lastTickPosY) * partial;
        double z = player.lastTickPosZ + (player.posZ - player.lastTickPosZ) * partial;
        AxisAlignedBB box = state.getSelectedBoundingBox(world, pos).grow(0.002D).offset(-x, -y, -z);
        GlStateManager.enableBlend();
        GlStateManager.tryBlendFuncSeparate(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA,
                GlStateManager.SourceFactor.ONE, GlStateManager.DestFactor.ZERO);
        GlStateManager.disableTexture2D();
        GlStateManager.depthMask(false);
        GlStateManager.glLineWidth(7.0F);
        RenderGlobal.drawSelectionBoundingBox(box, 0.0F, 0.0F, 0.0F, 1.0F);
        GlStateManager.glLineWidth(2.0F);
        RenderGlobal.drawSelectionBoundingBox(box, 0x57 / 255.0F, 0xFF / 255.0F, 0xE1 / 255.0F, 1.0F);
        GlStateManager.depthMask(true);
        GlStateManager.enableTexture2D();
        GlStateManager.disableBlend();
        drawn++;
    }
}
