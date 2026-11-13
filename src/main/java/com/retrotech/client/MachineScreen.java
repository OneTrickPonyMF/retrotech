package com.retrotech.client;

import com.retrotech.menu.MachineKind;
import com.retrotech.menu.MachineMenu;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.Slot;

/**
 * Interfaz de las máquinas, dibujada con rectángulos (sin texturas de GUI por ahora).
 * Muestra la barra de energía, la llama del generador o la flecha de progreso.
 */
public class MachineScreen extends AbstractContainerScreen<MachineMenu> {
    private static final int BG = 0xFFC6C6C6;
    private static final int LIGHT = 0xFFFFFFFF;
    private static final int DARK = 0xFF555555;
    private static final int SLOT = 0xFF8B8B8B;
    private static final int SLOT_DARK = 0xFF373737;
    private static final int BAR_BG = 0xFF2B2B2B;
    private static final int ENERGY = 0xFFE0C020;
    private static final int FLAME = 0xFFFF8A1E;
    private static final int ARROW = 0xFF6FCF4A;

    private static final int BAR_X = 152, BAR_Y = 17, BAR_W = 14, BAR_H = 52;

    public MachineScreen(MachineMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        this.imageWidth = 176;
        this.imageHeight = 166;
        this.inventoryLabelY = this.imageHeight - 94;
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        renderBackground(g);
        super.render(g, mouseX, mouseY, partialTick);
        renderTooltip(g, mouseX, mouseY);

        int x = leftPos + BAR_X, y = topPos + BAR_Y;
        if (mouseX >= x && mouseX < x + BAR_W && mouseY >= y && mouseY < y + BAR_H) {
            g.renderTooltip(font, Component.translatable("gui.retrotech.energy", menu.getEnergy(), menu.getCapacity()),
                    mouseX, mouseY);
        }
    }

    @Override
    protected void renderBg(GuiGraphics g, float partialTick, int mouseX, int mouseY) {
        int x = leftPos, y = topPos;

        // Panel con borde biselado, al estilo vanilla
        g.fill(x, y, x + imageWidth, y + imageHeight, BG);
        g.fill(x, y, x + imageWidth - 1, y + 2, LIGHT);
        g.fill(x, y, x + 2, y + imageHeight - 1, LIGHT);
        g.fill(x + 1, y + imageHeight - 2, x + imageWidth, y + imageHeight, DARK);
        g.fill(x + imageWidth - 2, y + 1, x + imageWidth, y + imageHeight, DARK);

        for (Slot slot : menu.slots) {
            drawSlot(g, x + slot.x, y + slot.y);
        }

        // Barra de energía
        int bx = x + BAR_X, by = y + BAR_Y;
        g.fill(bx - 1, by - 1, bx + BAR_W + 1, by + BAR_H + 1, SLOT_DARK);
        g.fill(bx, by, bx + BAR_W, by + BAR_H, BAR_BG);
        int capacity = menu.getCapacity();
        if (capacity > 0) {
            int filled = (int) ((long) menu.getEnergy() * BAR_H / capacity);
            g.fill(bx, by + BAR_H - filled, bx + BAR_W, by + BAR_H, ENERGY);
        }

        MachineKind kind = menu.getKind();
        int progress = menu.getProgress();
        int max = menu.getProgressMax();

        if (kind.usesFuel()) {
            // Llama encima del slot de combustible
            int fx = x + 81, fy = y + 36, size = 14;
            g.fill(fx, fy, fx + size, fy + size, SLOT);
            if (progress > 0 && max > 0) {
                int h = Math.max(1, progress * size / max);
                g.fill(fx, fy + size - h, fx + size, fy + size, FLAME);
            }
        } else if (kind.isProcessing()) {
            // Flecha de progreso entre entrada y salida
            int ax = x + 79, ay = y + 35, w = 24, h = 16;
            g.fill(ax, ay + 5, ax + w - 6, ay + h - 5, SLOT);
            for (int i = 0; i < 6; i++) {
                g.fill(ax + w - 6 + i, ay + 2 + i, ax + w - 5 + i, ay + h - 2 - i, SLOT);
            }
            if (progress > 0 && max > 0) {
                int len = progress * w / max;
                g.enableScissor(ax, ay, ax + len, ay + h);
                g.fill(ax, ay + 5, ax + w - 6, ay + h - 5, ARROW);
                for (int i = 0; i < 6; i++) {
                    g.fill(ax + w - 6 + i, ay + 2 + i, ax + w - 5 + i, ay + h - 2 - i, ARROW);
                }
                g.disableScissor();
            }
        } else if (kind == MachineKind.QUARRY) {
            Component status = switch (max) {
                case 0 -> Component.translatable("gui.retrotech.quarry.frame");
                case 1 -> Component.translatable("gui.retrotech.quarry.mining", progress);
                default -> Component.translatable("gui.retrotech.quarry.done");
            };
            g.drawString(font, status, x + 10, y + 34, 0xFF404040, false);
        } else if (kind == MachineKind.PUMP) {
            g.drawString(font, Component.translatable("gui.retrotech.pump", progress, max), x + 10, y + 34,
                    0xFF404040, false);
        } else if (kind == MachineKind.REDSTONE_ENGINE) {
            Component status = Component.translatable(progress > 0 ? "gui.retrotech.engine.on" : "gui.retrotech.engine.off");
            g.drawString(font, status, x + 10, y + 34, 0xFF404040, false);
        } else if (kind == MachineKind.SOLAR_PANEL) {
            // Sol: amarillo cuando genera, gris de noche o con lluvia
            int sx = x + 72, sy = y + 30, size = 24;
            int color = progress > 0 ? 0xFFFFD84A : 0xFF6B6B6B;
            g.fill(sx + 4, sy, sx + size - 4, sy + size, color);
            g.fill(sx, sy + 4, sx + size, sy + size - 4, color);
            g.fill(sx + 2, sy + 2, sx + size - 2, sy + size - 2, color);
        }

        // Marca de rayo al lado de los slots de batería
        for (int i = 0; i < menu.getMachineSlots(); i++) {
            if (kind.isBatterySlot(i)) {
                Slot slot = menu.slots.get(i);
                drawBolt(g, x + slot.x - 12, y + slot.y + 3);
            }
        }
    }

    private static void drawBolt(GuiGraphics g, int bx, int by) {
        int c = 0xFFE0C020;
        g.fill(bx + 3, by, bx + 6, by + 1, c);
        g.fill(bx + 2, by + 1, bx + 5, by + 3, c);
        g.fill(bx + 1, by + 3, bx + 7, by + 5, c);
        g.fill(bx + 3, by + 5, bx + 6, by + 7, c);
        g.fill(bx + 2, by + 7, bx + 5, by + 9, c);
        g.fill(bx + 2, by + 9, bx + 3, by + 10, c);
    }

    private static void drawSlot(GuiGraphics g, int sx, int sy) {
        g.fill(sx - 1, sy - 1, sx + 17, sy + 17, SLOT_DARK);
        g.fill(sx, sy, sx + 17, sy + 17, LIGHT);
        g.fill(sx, sy, sx + 16, sy + 16, SLOT);
    }
}
