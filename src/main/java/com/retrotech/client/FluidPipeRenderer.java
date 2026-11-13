package com.retrotech.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.retrotech.pipe.FluidPipeBlockEntity;
import com.retrotech.pipe.PipeBlock;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.fluids.FluidStack;

/** Dibuja el fluido dentro de las tuberías de fluidos (centro + brazos conectados). */
public class FluidPipeRenderer implements BlockEntityRenderer<FluidPipeBlockEntity> {
    private static final float LO = 4.6F / 16F, HI = 11.4F / 16F;

    public FluidPipeRenderer(BlockEntityRendererProvider.Context context) {
    }

    @Override
    public void render(FluidPipeBlockEntity pipe, float partialTick, PoseStack pose, MultiBufferSource buffers,
                       int light, int overlay) {
        FluidStack fluid = pipe.getFluid();
        if (fluid.isEmpty()) return;
        float fill = Math.min(1F, fluid.getAmount() / (float) FluidPipeBlockEntity.CAPACITY);
        float top = LO + (HI - LO) * Math.max(0.15F, fill);
        BlockState state = pipe.getBlockState();

        FluidRenderHelper.renderCuboid(pose, buffers, fluid, LO, LO, LO, HI, top, HI, light);
        for (Direction dir : Direction.values()) {
            if (!PipeBlock.isConnected(state, dir)) continue;
            switch (dir) {
                case NORTH -> FluidRenderHelper.renderCuboid(pose, buffers, fluid, LO, LO, 0, HI, top, LO, light);
                case SOUTH -> FluidRenderHelper.renderCuboid(pose, buffers, fluid, LO, LO, HI, HI, top, 1, light);
                case WEST -> FluidRenderHelper.renderCuboid(pose, buffers, fluid, 0, LO, LO, LO, top, HI, light);
                case EAST -> FluidRenderHelper.renderCuboid(pose, buffers, fluid, HI, LO, LO, 1, top, HI, light);
                case UP -> FluidRenderHelper.renderCuboid(pose, buffers, fluid, 0.4F, HI, 0.4F, 0.6F, 1, 0.6F, light);
                case DOWN -> FluidRenderHelper.renderCuboid(pose, buffers, fluid, 0.4F, 0, 0.4F, 0.6F, LO, 0.6F, light);
            }
        }
    }
}
