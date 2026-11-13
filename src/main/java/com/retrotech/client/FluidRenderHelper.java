package com.retrotech.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraftforge.client.extensions.common.IClientFluidTypeExtensions;
import net.minecraftforge.fluids.FluidStack;
import org.joml.Matrix3f;
import org.joml.Matrix4f;

/** Dibuja un prisma de fluido (usado por el tanque y las tuberías de fluidos). Coordenadas en bloques (0..1). */
public final class FluidRenderHelper {
    private FluidRenderHelper() {
    }

    public static void renderCuboid(PoseStack pose, MultiBufferSource buffers, FluidStack fluid,
                                    float x0, float y0, float z0, float x1, float y1, float z1, int light) {
        if (fluid.isEmpty() || y1 <= y0) return;
        IClientFluidTypeExtensions ext = IClientFluidTypeExtensions.of(fluid.getFluid());
        ResourceLocation still = ext.getStillTexture(fluid);
        if (still == null) return;
        TextureAtlasSprite sprite = Minecraft.getInstance().getTextureAtlas(InventoryMenu.BLOCK_ATLAS).apply(still);

        int color = ext.getTintColor(fluid);
        float a = ((color >> 24) & 0xFF) / 255F;
        if (a <= 0) a = 1;
        float r = ((color >> 16) & 0xFF) / 255F;
        float g = ((color >> 8) & 0xFF) / 255F;
        float b = (color & 0xFF) / 255F;

        VertexConsumer vc = buffers.getBuffer(RenderType.translucent());
        Matrix4f m = pose.last().pose();
        Matrix3f n = pose.last().normal();

        // Arriba y abajo
        face(vc, m, n, sprite, r, g, b, a, light, 0, 1, 0,
                x0, y1, z0, x0, y1, z1, x1, y1, z1, x1, y1, z0, x0, z0, x1, z1);
        face(vc, m, n, sprite, r, g, b, a, light, 0, -1, 0,
                x0, y0, z0, x1, y0, z0, x1, y0, z1, x0, y0, z1, x0, z0, x1, z1);
        // Norte y sur
        face(vc, m, n, sprite, r, g, b, a, light, 0, 0, -1,
                x0, y0, z0, x0, y1, z0, x1, y1, z0, x1, y0, z0, x0, y0, x1, y1);
        face(vc, m, n, sprite, r, g, b, a, light, 0, 0, 1,
                x0, y0, z1, x1, y0, z1, x1, y1, z1, x0, y1, z1, x0, y0, x1, y1);
        // Oeste y este
        face(vc, m, n, sprite, r, g, b, a, light, -1, 0, 0,
                x0, y0, z0, x0, y0, z1, x0, y1, z1, x0, y1, z0, z0, y0, z1, y1);
        face(vc, m, n, sprite, r, g, b, a, light, 1, 0, 0,
                x1, y0, z0, x1, y1, z0, x1, y1, z1, x1, y0, z1, z0, y0, z1, y1);
    }

    /** Un cuadrado dibujado de los dos lados, así no importa el orden de los vértices. */
    private static void face(VertexConsumer vc, Matrix4f m, Matrix3f n, TextureAtlasSprite sprite,
                             float r, float g, float b, float a, int light, float nx, float ny, float nz,
                             float ax, float ay, float az, float bx, float by, float bz,
                             float cx, float cy, float cz, float dx, float dy, float dz,
                             float u0, float v0, float u1, float v1) {
        float su0 = sprite.getU(u0 * 16), sv0 = sprite.getV(v0 * 16);
        float su1 = sprite.getU(u1 * 16), sv1 = sprite.getV(v1 * 16);
        vertex(vc, m, n, ax, ay, az, su0, sv0, r, g, b, a, light, nx, ny, nz);
        vertex(vc, m, n, bx, by, bz, su0, sv1, r, g, b, a, light, nx, ny, nz);
        vertex(vc, m, n, cx, cy, cz, su1, sv1, r, g, b, a, light, nx, ny, nz);
        vertex(vc, m, n, dx, dy, dz, su1, sv0, r, g, b, a, light, nx, ny, nz);

        vertex(vc, m, n, dx, dy, dz, su1, sv0, r, g, b, a, light, -nx, -ny, -nz);
        vertex(vc, m, n, cx, cy, cz, su1, sv1, r, g, b, a, light, -nx, -ny, -nz);
        vertex(vc, m, n, bx, by, bz, su0, sv1, r, g, b, a, light, -nx, -ny, -nz);
        vertex(vc, m, n, ax, ay, az, su0, sv0, r, g, b, a, light, -nx, -ny, -nz);
    }

    private static void vertex(VertexConsumer vc, Matrix4f m, Matrix3f n, float x, float y, float z, float u, float v,
                               float r, float g, float b, float a, int light, float nx, float ny, float nz) {
        vc.vertex(m, x, y, z).color(r, g, b, a).uv(u, v).uv2(light).normal(n, nx, ny, nz).endVertex();
    }
}
