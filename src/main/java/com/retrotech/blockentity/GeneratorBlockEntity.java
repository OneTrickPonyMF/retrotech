package com.retrotech.blockentity;

import com.retrotech.energy.EnergyTier;
import com.retrotech.menu.MachineKind;
import com.retrotech.menu.MachineMenu;
import com.retrotech.registry.ModBlockEntities;
import com.retrotech.registry.ModMenus;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.common.ForgeHooks;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * Generador: quema combustible y produce 10 EU/t (un carbón = 4000 EU, como en IC2).
 * Emite energía por los 6 lados.
 */
public class GeneratorBlockEntity extends MachineBlockEntity {
    public static final int EU_PER_TICK = 10;
    private static final List<Direction> ALL_SIDES = List.of(Direction.values());

    public GeneratorBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.GENERATOR.get(), pos, state, MachineKind.GENERATOR, EnergyTier.LV, 4000, 0, 32);
    }

    @Override
    public void serverTick() {
        if (progress <= 0 && !energy.isFull()) {
            ItemStack fuel = items.getStackInSlot(0);
            int burn = ForgeHooks.getBurnTime(fuel, RecipeType.SMELTING) / 4;
            if (burn > 0) {
                progress = burn;
                progressMax = burn;
                if (fuel.hasCraftingRemainingItem() && fuel.getCount() == 1) {
                    items.setStackInSlot(0, fuel.getCraftingRemainingItem()); // balde de lava -> balde vacío
                } else {
                    items.extractItem(0, 1, false);
                }
                setChanged();
            }
        }

        if (progress > 0 && !energy.isFull()) {
            progress--;
            energy.generate(EU_PER_TICK);
        }

        setLit(progress > 0);
        pushEnergy(ALL_SIDES);
    }

    @Override
    protected boolean canReceiveFrom(@Nullable Direction side) {
        return false;
    }

    @Override
    protected boolean canExtractFrom(@Nullable Direction side) {
        return true;
    }

    @Override
    protected boolean canAutomationExtract(int slot) {
        // Deja sacar lo que ya no sirve como combustible (por ejemplo, baldes vacíos).
        return ForgeHooks.getBurnTime(items.getStackInSlot(slot), RecipeType.SMELTING) <= 0;
    }

    @Override
    protected MenuType<MachineMenu> getMenuType() {
        return ModMenus.GENERATOR.get();
    }
}
