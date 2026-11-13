package com.retrotech.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.retrotech.blockentity.PumpBlockEntity;
import com.retrotech.registry.ModBlocks;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.block.BlockRenderDispatcher;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

/** Dibuja el tubo de la bomba bajando hasta el fluido. */
public class PumpRenderer implements BlockEntityRenderer<PumpBlockEntity> {
    public PumpRenderer(BlockEntityRendererProvider.Context context) {
    }

    @Override
    public void render(PumpBlockEntity pump, float partialTick, PoseStack pose, MultiBufferSource buffers,
                       int light, int overlay) {
        Level level = pump.getLevel();
        int tubeY = pump.getTubeY();
        if (level == null || tubeY == Integer.MAX_VALUE) return;

        BlockRenderDispatcher blocks = Minecraft.getInstance().getBlockRenderer();
        BlockState tube = ModBlocks.PUMP_TUBE.get().defaultBlockState();
        BlockPos origin = pump.getBlockPos();
        for (int y = tubeY; y < origin.getY(); y++) {
            BlockPos at = new BlockPos(origin.getX(), y, origin.getZ());
            pose.pushPose();
            pose.translate(0.3125, y - origin.getY() + (y == tubeY ? 0.6 : 0), 0.3125);
            pose.scale(0.375F, y == tubeY ? 0.4F : 1F, 0.375F);
            blocks.renderSingleBlock(tube, pose, buffers, LevelRenderer.getLightColor(level, at),
                    OverlayTexture.NO_OVERLAY);
            pose.popPose();
        }
    }

    @Override
    public boolean shouldRenderOffScreen(PumpBlockEntity pump) {
        return true;
    }
}
