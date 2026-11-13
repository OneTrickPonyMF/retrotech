package com.retrotech.energy;

import net.minecraftforge.energy.EnergyStorage;

/**
 * Almacenamiento de energía de una máquina. 1 EU = 1 FE.
 * Avisa a la máquina cuando cambia para que se guarde.
 */
public class MachineEnergyStorage extends EnergyStorage {
    private final Runnable onChanged;

    public MachineEnergyStorage(int capacity, int maxReceive, int maxExtract, Runnable onChanged) {
        super(capacity, maxReceive, maxExtract);
        this.onChanged = onChanged;
    }

    @Override
    public int receiveEnergy(int maxReceive, boolean simulate) {
        int received = super.receiveEnergy(maxReceive, simulate);
        if (received > 0 && !simulate) onChanged.run();
        return received;
    }

    @Override
    public int extractEnergy(int maxExtract, boolean simulate) {
        int extracted = super.extractEnergy(maxExtract, simulate);
        if (extracted > 0 && !simulate) onChanged.run();
        return extracted;
    }

    /** Suma energía ignorando el límite de entrada (lo usan los generadores). */
    public int generate(int amount) {
        int added = Math.min(capacity - energy, Math.max(0, amount));
        if (added > 0) {
            energy += added;
            onChanged.run();
        }
        return added;
    }

    /** Resta energía ignorando el límite de salida (consumo interno o envío por la red). */
    public int consume(int amount) {
        int removed = Math.min(energy, Math.max(0, amount));
        if (removed > 0) {
            energy -= removed;
            onChanged.run();
        }
        return removed;
    }

    public void setEnergy(int value) {
        this.energy = Math.max(0, Math.min(capacity, value));
    }

    public int getMaxReceive() {
        return maxReceive;
    }

    public int getMaxExtract() {
        return maxExtract;
    }

    public boolean isFull() {
        return energy >= capacity;
    }
}
