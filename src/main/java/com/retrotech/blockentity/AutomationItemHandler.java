package com.retrotech.blockentity;

import net.minecraft.world.item.ItemStack;
import net.minecraftforge.items.IItemHandler;
import org.jetbrains.annotations.NotNull;

/**
 * Lo que ven las tolvas y tuberías: solo pueden meter en las entradas y sacar de las salidas.
 */
public class AutomationItemHandler implements IItemHandler {
    private final MachineBlockEntity machine;

    public AutomationItemHandler(MachineBlockEntity machine) {
        this.machine = machine;
    }

    @Override
    public int getSlots() {
        return machine.items.getSlots();
    }

    @NotNull
    @Override
    public ItemStack getStackInSlot(int slot) {
        return machine.items.getStackInSlot(slot);
    }

    @NotNull
    @Override
    public ItemStack insertItem(int slot, @NotNull ItemStack stack, boolean simulate) {
        if (!machine.canAutomationInsert(slot, stack)) return stack;
        return machine.items.insertItem(slot, stack, simulate);
    }

    @NotNull
    @Override
    public ItemStack extractItem(int slot, int amount, boolean simulate) {
        if (!machine.canAutomationExtract(slot)) return ItemStack.EMPTY;
        return machine.items.extractItem(slot, amount, simulate);
    }

    @Override
    public int getSlotLimit(int slot) {
        return machine.items.getSlotLimit(slot);
    }

    @Override
    public boolean isItemValid(int slot, @NotNull ItemStack stack) {
        return machine.canAutomationInsert(slot, stack);
    }
}
