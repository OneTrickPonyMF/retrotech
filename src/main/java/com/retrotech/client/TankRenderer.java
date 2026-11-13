package com.retrotech.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.retrotech.fluid.TankBlock;
import com.retrotech.fluid.TankBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraftforge.fluids.FluidStack;

/** Dibuja el fluido dentro del tanque según cuánto tiene. */
public class TankRenderer implements BlockEntityRenderer<TankBlockEntity> {
    public TankRenderer(BlockEntityRendererProvider.Context context) {
    }

    @Override
    public void render(TankBlockEntity tank, float partialTick, PoseStack pose, MultiBufferSource buffers,
                       int light, int overlay) {
        FluidStack fluid = tank.getTank().getFluid();
        if (fluid.isEmpty()) return;
        float fill = Math.min(1F, fluid.getAmount() / (float) TankBlockEntity.CAPACITY);
        float min = 2.2F / 16F, max = 13.8F / 16F;
        BlockState state = tank.getBlockState();
        boolean below = state.hasProperty(TankBlock.DOWN) && state.getValue(TankBlock.DOWN);
        boolean above = state.hasProperty(TankBlock.UP) && state.getValue(TankBlock.UP);
        float bottom = below ? 0F : 0.01F;
        float top = fill >= 0.999F && above ? 1F : bottom + fill * (1F - bottom - 0.01F);
        FluidRenderHelper.renderCuboid(pose, buffers, fluid, min, bottom, min, max, top, max, light);
    }
}
