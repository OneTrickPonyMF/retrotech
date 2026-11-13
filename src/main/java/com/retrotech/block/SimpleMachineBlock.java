package com.retrotech.block;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

import java.util.function.BiFunction;

/** Máquina sin orientación (por ejemplo, el panel solar). */
public class SimpleMachineBlock extends MachineBlock {
    public SimpleMachineBlock(Properties properties, BiFunction<BlockPos, BlockState, BlockEntity> factory) {
        super(properties, factory);
    }
}
