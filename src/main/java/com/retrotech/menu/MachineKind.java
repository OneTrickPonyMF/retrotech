package com.retrotech.menu;

import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraftforge.common.ForgeHooks;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.items.ItemStackHandler;

/**
 * Describe cada tipo de máquina: cuántos slots tiene, dónde se dibujan en la interfaz
 * y qué items acepta. Lo usan tanto el servidor como el cliente, así coinciden.
 *
 * Máquinas de procesamiento: slot 0 = entrada, 1 = salida, 2 = batería (descarga).
 * BatBox: slot 0 = cargar batería, 1 = descargar batería.
 */
public enum MachineKind {
    GENERATOR(new int[][]{{80, 53}}),
    ELECTRIC_FURNACE(new int[][]{{56, 17}, {116, 35}, {56, 53}}),
    MACERATOR(new int[][]{{56, 17}, {116, 35}, {56, 53}}),
    COMPRESSOR(new int[][]{{56, 17}, {116, 35}, {56, 53}}),
    EXTRACTOR(new int[][]{{56, 17}, {116, 35}, {56, 53}}),
    BATBOX(new int[][]{{56, 17}, {56, 53}}),
    SOLAR_PANEL(new int[0][]),
    REDSTONE_ENGINE(new int[0][]),
    STIRLING_ENGINE(new int[][]{{80, 53}}),
    QUARRY(new int[0][]);

    public static final int INPUT = 0;
    public static final int OUTPUT = 1;
    public static final int BATTERY = 2;

    private final int[][] slotPositions;

    MachineKind(int[][] slotPositions) {
        this.slotPositions = slotPositions;
    }

    public int slotCount() {
        return slotPositions.length;
    }

    public int slotX(int slot) {
        return slotPositions[slot][0];
    }

    public int slotY(int slot) {
        return slotPositions[slot][1];
    }

    /** Máquinas con entrada, salida y flecha de progreso. */
    public boolean isProcessing() {
        return this == ELECTRIC_FURNACE || this == MACERATOR || this == COMPRESSOR || this == EXTRACTOR;
    }

    /** Máquinas que queman combustible sólido en su único slot. */
    public boolean usesFuel() {
        return this == GENERATOR || this == STIRLING_ENGINE;
    }

    /** Slots donde va una batería. */
    public boolean isBatterySlot(int slot) {
        return (isProcessing() && slot == BATTERY) || this == BATBOX;
    }

    public static boolean holdsEnergy(ItemStack stack) {
        return !stack.isEmpty() && stack.getCapability(ForgeCapabilities.ENERGY).isPresent();
    }

    public boolean isItemValid(int slot, ItemStack stack) {
        if (isBatterySlot(slot)) return holdsEnergy(stack);
        if (isProcessing()) return slot == INPUT;
        if (usesFuel()) return ForgeHooks.getBurnTime(stack, RecipeType.SMELTING) > 0;
        return false;
    }

    public ItemStackHandler createHandler(Runnable onChanged) {
        return new ItemStackHandler(slotCount()) {
            @Override
            protected void onContentsChanged(int slot) {
                onChanged.run();
            }

            @Override
            public boolean isItemValid(int slot, ItemStack stack) {
                return MachineKind.this.isItemValid(slot, stack);
            }

            @Override
            public int getSlotLimit(int slot) {
                return MachineKind.this.isBatterySlot(slot) ? 1 : super.getSlotLimit(slot);
            }
        };
    }
}
