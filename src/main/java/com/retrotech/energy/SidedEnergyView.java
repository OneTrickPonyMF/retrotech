package com.retrotech.energy;

import net.minecraftforge.energy.IEnergyStorage;

/** Vista de un almacenamiento que solo deja entrar o solo deja salir energía (según el lado). */
public class SidedEnergyView implements IEnergyStorage {
    private final IEnergyStorage inner;
    private final boolean allowReceive;
    private final boolean allowExtract;

    public SidedEnergyView(IEnergyStorage inner, boolean allowReceive, boolean allowExtract) {
        this.inner = inner;
        this.allowReceive = allowReceive;
        this.allowExtract = allowExtract;
    }

    @Override
    public int receiveEnergy(int maxReceive, boolean simulate) {
        return allowReceive ? inner.receiveEnergy(maxReceive, simulate) : 0;
    }

    @Override
    public int extractEnergy(int maxExtract, boolean simulate) {
        return allowExtract ? inner.extractEnergy(maxExtract, simulate) : 0;
    }

    @Override
    public int getEnergyStored() {
        return inner.getEnergyStored();
    }

    @Override
    public int getMaxEnergyStored() {
        return inner.getMaxEnergyStored();
    }

    @Override
    public boolean canExtract() {
        return allowExtract && inner.canExtract();
    }

    @Override
    public boolean canReceive() {
        return allowReceive && inner.canReceive();
    }
}
