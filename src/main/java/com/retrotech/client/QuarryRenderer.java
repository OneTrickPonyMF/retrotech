package com.retrotech.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.retrotech.blockentity.QuarryBlockEntity;
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
import net.minecraft.world.phys.AABB;

/** Dibuja el pórtico y el taladro de la quarry mientras excava. */
public class QuarryRenderer implements BlockEntityRenderer<QuarryBlockEntity> {
    private static final float BEAM = 0.25F;

    public QuarryRenderer(BlockEntityRendererProvider.Context context) {
    }

    @Override
    public void render(QuarryBlockEntity quarry, float partialTick, PoseStack pose, MultiBufferSource buffers,
                       int light, int overlay) {
        BlockPos target = quarry.getTarget();
        Level level = quarry.getLevel();
        if (quarry.getPhase() != QuarryBlockEntity.PHASE_MINE || target == null || level == null) return;

        BlockRenderDispatcher blocks = Minecraft.getInstance().getBlockRenderer();
        BlockState beam = ModBlocks.QUARRY_DRILL.get().defaultBlockState();
        BlockPos origin = quarry.getBlockPos();
        AABB area = quarry.getInnerArea();

        int topY = origin.getY();
        double off = (1 - BEAM) / 2;

        // Viga a lo largo de X, a la altura del marco, sobre la fila del taladro
        for (int x = (int) area.minX; x <= (int) area.maxX; x++) {
            piece(blocks, beam, pose, buffers, level, origin, new BlockPos(x, topY, target.getZ()),
                    0, off, off, 1, BEAM, BEAM);
        }
        // Viga a lo largo de Z, sobre la columna del taladro
        for (int z = (int) area.minZ; z <= (int) area.maxZ; z++) {
            piece(blocks, beam, pose, buffers, level, origin, new BlockPos(target.getX(), topY, z),
                    off, off + 0.01, 0, BEAM, BEAM, 1);
        }
        // Eje vertical desde el marco hasta el bloque que se está rompiendo
        for (int y = target.getY() + 1; y <= topY; y++) {
            piece(blocks, beam, pose, buffers, level, origin, new BlockPos(target.getX(), y, target.getZ()),
                    off, 0, off, BEAM, 1, BEAM);
        }
        // Cabeza del taladro
        piece(blocks, beam, pose, buffers, level, origin, target, 0.3, 0.55, 0.3, 0.4F, 0.45F, 0.4F);
    }

    private static void piece(BlockRenderDispatcher blocks, BlockState state, PoseStack pose,
                              MultiBufferSource buffers, Level level, BlockPos origin, BlockPos at,
                              double ox, double oy, double oz, float sx, float sy, float sz) {
        pose.pushPose();
        pose.translate(at.getX() - origin.getX() + ox, at.getY() - origin.getY() + oy, at.getZ() - origin.getZ() + oz);
        pose.scale(sx, sy, sz);
        blocks.renderSingleBlock(state, pose, buffers, LevelRenderer.getLightColor(level, at),
                OverlayTexture.NO_OVERLAY);
        pose.popPose();
    }

    @Override
    public boolean shouldRenderOffScreen(QuarryBlockEntity quarry) {
        return true;
    }

    @Override
    public int getViewDistance() {
        return 96;
    }
}
