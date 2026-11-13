package com.retrotech.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.retrotech.fluid.TankBlockEntity;
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
        FluidRenderHelper.renderCuboid(pose, buffers, fluid, min, 0.01F, min, max, 0.01F + fill * 0.98F, max, light);
    }
}
