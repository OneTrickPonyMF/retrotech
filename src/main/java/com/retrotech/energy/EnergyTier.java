package com.retrotech.energy;

/**
 * Niveles de voltaje al estilo IC2. maxPacket es la cantidad máxima de EU por tick
 * que una máquina de ese nivel puede recibir de una sola vez sin explotar.
 */
public enum EnergyTier {
    LV(32),
    MV(128),
    HV(512),
    EV(2048);

    public final int maxPacket;

    EnergyTier(int maxPacket) {
        this.maxPacket = maxPacket;
    }

    /** Devuelve el nivel más bajo capaz de transportar un paquete de este tamaño. */
    public static EnergyTier forPacket(int packet) {
        for (EnergyTier tier : values()) {
            if (packet <= tier.maxPacket) return tier;
        }
        return EV;
    }
}
