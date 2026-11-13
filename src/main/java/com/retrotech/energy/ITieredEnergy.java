package com.retrotech.energy;

/** Implementado por las máquinas del mod que tienen un nivel de voltaje (y pueden explotar). */
public interface ITieredEnergy {
    EnergyTier getEnergyTier();
}
