package com.retrotech.blockentity;

import com.retrotech.block.StorageMachineBlock;
import com.retrotech.energy.EnergyTier;
import com.retrotech.menu.MachineKind;
import com.retrotech.menu.MachineMenu;
import com.retrotech.registry.ModBlockEntities;
import com.retrotech.registry.ModMenus;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * BatBox: guarda 40.000 EU. Recibe por cualquier lado menos el frente, y emite por el frente (32 EU/t).
 */
public class BatBoxBlockEntity extends MachineBlockEntity {
    public BatBoxBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.BATBOX.get(), pos, state, MachineKind.BATBOX, EnergyTier.LV, 40000, 32, 32);
    }

    private Direction front() {
        return getBlockState().getValue(StorageMachineBlock.FACING);
    }

    @Override
    public void serverTick() {
        chargeInto(0);      // slot de arriba: carga baterías
        dischargeFrom(1);   // slot de abajo: vacía baterías en la BatBox
        pushEnergy(List.of(front()));
    }

    @Override
    protected boolean canAutomationExtract(int slot) {
        return slot == 0 ? batteryIs(0, true) : batteryIs(1, false);
    }

    @Override
    protected boolean canReceiveFrom(@Nullable Direction side) {
        return side == null || side != front();
    }

    @Override
    protected boolean canExtractFrom(@Nullable Direction side) {
        return side == null || side == front();
    }

    @Override
    protected MenuType<MachineMenu> getMenuType() {
        return ModMenus.BATBOX.get();
    }
}
