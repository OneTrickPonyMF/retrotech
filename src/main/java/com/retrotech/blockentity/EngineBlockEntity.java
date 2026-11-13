package com.retrotech.blockentity;

import com.retrotech.block.EngineBlock;
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
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.common.ForgeHooks;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.function.Supplier;

/**
 * Motores al estilo BuildCraft. Solo funcionan con señal de redstone y entregan energía por el frente.
 * - Motor de redstone: 1 EU/t, sin combustible (alcanza para una tubería de madera).
 * - Motor Stirling: quema combustible sólido, 5 EU/t.
 */
public class EngineBlockEntity extends MachineBlockEntity {
    private final int euPerTick;
    private final Supplier<MenuType<MachineMenu>> menuType;

    public EngineBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state, MachineKind kind, int capacity,
                             int euPerTick, Supplier<MenuType<MachineMenu>> menuType) {
        super(type, pos, state, kind, EnergyTier.LV, capacity, 0, 32);
        this.euPerTick = euPerTick;
        this.menuType = menuType;
    }

    public static EngineBlockEntity redstone(BlockPos pos, BlockState state) {
        return new EngineBlockEntity(ModBlockEntities.REDSTONE_ENGINE.get(), pos, state, MachineKind.REDSTONE_ENGINE,
                200, 1, ModMenus.REDSTONE_ENGINE);
    }

    public static EngineBlockEntity stirling(BlockPos pos, BlockState state) {
        return new EngineBlockEntity(ModBlockEntities.STIRLING_ENGINE.get(), pos, state, MachineKind.STIRLING_ENGINE,
                2000, 5, ModMenus.STIRLING_ENGINE);
    }

    private Direction front() {
        return getBlockState().getValue(EngineBlock.FACING);
    }

    @Override
    public void serverTick() {
        if (level == null) return;
        boolean powered = level.hasNeighborSignal(worldPosition);
        boolean running = false;

        if (kind.usesFuel()) {
            if (powered && progress <= 0 && !energy.isFull()) {
                ItemStack fuel = items.getStackInSlot(0);
                int burn = ForgeHooks.getBurnTime(fuel, RecipeType.SMELTING) / 2;
                if (burn > 0) {
                    progress = burn;
                    progressMax = burn;
                    if (fuel.hasCraftingRemainingItem() && fuel.getCount() == 1) {
                        items.setStackInSlot(0, fuel.getCraftingRemainingItem());
                    } else {
                        items.extractItem(0, 1, false);
                    }
                }
            }
            if (powered && progress > 0 && !energy.isFull()) {
                progress--;
                energy.generate(euPerTick);
                running = true;
            }
        } else {
            progress = powered ? 1 : 0;
            progressMax = 1;
            if (powered && !energy.isFull()) {
                energy.generate(euPerTick);
                running = true;
            }
        }

        setLit(running || (powered && energy.getEnergyStored() > 0));
        pushEnergy(List.of(front()));
    }

    @Override
    protected boolean canReceiveFrom(@Nullable Direction side) {
        return false;
    }

    @Override
    protected boolean canExtractFrom(@Nullable Direction side) {
        return side == null || side == front();
    }

    @Override
    protected boolean canAutomationExtract(int slot) {
        return kind.usesFuel() && ForgeHooks.getBurnTime(items.getStackInSlot(slot), RecipeType.SMELTING) <= 0;
    }

    @Override
    protected MenuType<MachineMenu> getMenuType() {
        return menuType.get();
    }
}
