package com.retrotech.menu;

import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraftforge.common.ForgeHooks;
import net.minecraftforge.items.ItemStackHandler;

/**
 * Describe cada tipo de máquina: cuántos slots tiene, dónde se dibujan en la interfaz
 * y qué items acepta. Lo usan tanto el servidor como el cliente, así coinciden.
 */
public enum MachineKind {
    GENERATOR(new int[][]{{80, 53}}),
    ELECTRIC_FURNACE(new int[][]{{56, 35}, {116, 35}}),
    BATBOX(new int[0][]);

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

    public boolean isItemValid(int slot, ItemStack stack) {
        return switch (this) {
            case GENERATOR -> ForgeHooks.getBurnTime(stack, RecipeType.SMELTING) > 0;
            case ELECTRIC_FURNACE -> slot == 0;
            case BATBOX -> false;
        };
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
        };
    }
}
