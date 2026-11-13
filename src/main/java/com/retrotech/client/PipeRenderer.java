package com.retrotech.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.retrotech.pipe.PipeBlockEntity;
import com.retrotech.pipe.TravellingItem;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.entity.ItemRenderer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.Direction;
import net.minecraft.world.item.ItemDisplayContext;

/** Dibuja los items que viajan dentro de las tuberías. */
public class PipeRenderer implements BlockEntityRenderer<PipeBlockEntity> {
    private final ItemRenderer itemRenderer;

    public PipeRenderer(BlockEntityRendererProvider.Context context) {
        this.itemRenderer = context.getItemRenderer();
    }

    @Override
    public void render(PipeBlockEntity pipe, float partialTick, PoseStack pose, MultiBufferSource buffers,
                       int light, int overlay) {
        int seed = 0;
        for (TravellingItem item : pipe.getItems()) {
            float p = item.progress + item.speed * partialTick;
            if (item.output == null) p = Math.min(p, 0.5F);
            p = Math.min(p, 1.0F);

            Direction dir;
            float offset;
            if (p < 0.5F) {
                dir = item.input;
                offset = 0.5F - p;
            } else {
                dir = item.output;
                offset = p - 0.5F;
            }

            pose.pushPose();
            if (dir != null) {
                pose.translate(0.5 + dir.getStepX() * offset, 0.5 + dir.getStepY() * offset - 0.12,
                        0.5 + dir.getStepZ() * offset);
            } else {
                pose.translate(0.5, 0.38, 0.5);
            }
            pose.scale(0.55F, 0.55F, 0.55F);
            itemRenderer.renderStatic(item.stack, ItemDisplayContext.GROUND, light, OverlayTexture.NO_OVERLAY,
                    pose, buffers, pipe.getLevel(), seed++);
            pose.popPose();
        }
    }
}
