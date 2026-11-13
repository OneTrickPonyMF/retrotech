package com.retrotech.pipe;

/** Tipos de tubería de items, como en BuildCraft. */
public enum PipeType {
    /** Extrae items de inventarios cuando recibe energía de un motor. No se conecta con otras de madera. */
    WOODEN,
    /** Tubería básica. No se conecta con las de piedra. */
    COBBLESTONE,
    /** Tubería básica. No se conecta con las de adoquín. */
    STONE,
    /** Acelera los items que pasan por ella. */
    GOLD;

    public boolean canConnectTo(PipeType other) {
        if (this == WOODEN && other == WOODEN) return false;
        if ((this == COBBLESTONE && other == STONE) || (this == STONE && other == COBBLESTONE)) return false;
        return true;
    }
}
