package com.retrotech.blockentity;

import com.retrotech.energy.EnergyTier;
import com.retrotech.menu.MachineKind;
import com.retrotech.menu.MachineMenu;
import com.retrotech.registry.ModBlockEntities;
import com.retrotech.registry.ModMenus;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

/**
 * Horno eléctrico: usa las recetas del horno normal. 3 EU/t, 130 ticks por item (390 EU por operación).
 */
public class ElectricFurnaceBlockEntity extends MachineBlockEntity {
    public static final int EU_PER_TICK = 3;
    public static final int OPERATION_TIME = 130;

    private ItemStack cachedInput = ItemStack.EMPTY;
    private ItemStack cachedResult = ItemStack.EMPTY;

    public ElectricFurnaceBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.ELECTRIC_FURNACE.get(), pos, state, MachineKind.ELECTRIC_FURNACE, EnergyTier.LV,
                416, 32, 0);
        this.progressMax = OPERATION_TIME;
    }

    @Override
    public void serverTick() {
        progressMax = OPERATION_TIME;
        dischargeFrom(MachineKind.BATTERY);
        ItemStack input = items.getStackInSlot(MachineKind.INPUT);
        ItemStack result = input.isEmpty() ? ItemStack.EMPTY : getResult(input);
        boolean working = false;

        if (!result.isEmpty() && canOutput(result)) {
            if (energy.getEnergyStored() >= EU_PER_TICK) {
                energy.consume(EU_PER_TICK);
                progress++;
                working = true;
                if (progress >= OPERATION_TIME) {
                    progress = 0;
                    items.extractItem(MachineKind.INPUT, 1, false);
                    output(result);
                }
                setChanged();
            }
        } else if (progress != 0) {
            progress = 0;
            setChanged();
        }

        setLit(working);
    }

    private ItemStack getResult(ItemStack input) {
        if (level == null) return ItemStack.EMPTY;
        if (!cachedInput.isEmpty() && ItemStack.isSameItemSameTags(cachedInput, input)) return cachedResult;
        cachedInput = input.copyWithCount(1);
        cachedResult = level.getRecipeManager()
                .getRecipeFor(RecipeType.SMELTING, new SimpleContainer(cachedInput), level)
                .map(recipe -> recipe.getResultItem(level.registryAccess()).copy())
                .orElse(ItemStack.EMPTY);
        return cachedResult;
    }

    private boolean canOutput(ItemStack result) {
        ItemStack out = items.getStackInSlot(MachineKind.OUTPUT);
        if (out.isEmpty()) return true;
        return ItemStack.isSameItemSameTags(out, result) && out.getCount() + result.getCount() <= out.getMaxStackSize();
    }

    private void output(ItemStack result) {
        ItemStack out = items.getStackInSlot(MachineKind.OUTPUT);
        if (out.isEmpty()) {
            items.setStackInSlot(MachineKind.OUTPUT, result.copy());
        } else {
            ItemStack grown = out.copy();
            grown.grow(result.getCount());
            items.setStackInSlot(MachineKind.OUTPUT, grown);
        }
    }

    @Override
    protected boolean canReceiveFrom(@Nullable Direction side) {
        return true;
    }

    @Override
    protected boolean canExtractFrom(@Nullable Direction side) {
        return false;
    }

    @Override
    protected boolean canAutomationExtract(int slot) {
        return slot == MachineKind.OUTPUT || (slot == MachineKind.BATTERY && batteryIs(slot, false));
    }

    @Override
    protected MenuType<MachineMenu> getMenuType() {
        return ModMenus.ELECTRIC_FURNACE.get();
    }
}
