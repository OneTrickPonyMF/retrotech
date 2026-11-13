package com.retrotech.block;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

import java.util.EnumMap;
import java.util.Map;
import java.util.function.BiFunction;

/**
 * Motor al estilo BuildCraft. Se coloca mirando hacia el bloque sobre el que lo apoyaste
 * (por ejemplo, una tubería de madera o la quarry) y le entrega energía por ese lado.
 * Solo funciona con señal de redstone.
 */
public class EngineBlock extends MachineBlock {
    public static final DirectionProperty FACING = BlockStateProperties.FACING;
    public static final BooleanProperty LIT = BlockStateProperties.LIT;

    private static final Map<Direction, VoxelShape> SHAPES = new EnumMap<>(Direction.class);

    static {
        // Base completa + cámara + placa superior, rotado según hacia dónde mira el motor.
        SHAPES.put(Direction.UP, Shapes.or(Block.box(0, 0, 0, 16, 4, 16), Block.box(4, 4, 4, 12, 10, 12),
                Block.box(0, 10, 0, 16, 14, 16), Block.box(5, 14, 5, 11, 16, 11)));
        SHAPES.put(Direction.DOWN, Shapes.or(Block.box(0, 12, 0, 16, 16, 16), Block.box(4, 6, 4, 12, 12, 12),
                Block.box(0, 2, 0, 16, 6, 16), Block.box(5, 0, 5, 11, 2, 11)));
        SHAPES.put(Direction.NORTH, Shapes.or(Block.box(0, 0, 12, 16, 16, 16), Block.box(4, 4, 6, 12, 12, 12),
                Block.box(0, 0, 2, 16, 16, 6), Block.box(5, 5, 0, 11, 11, 2)));
        SHAPES.put(Direction.SOUTH, Shapes.or(Block.box(0, 0, 0, 16, 16, 4), Block.box(4, 4, 4, 12, 12, 10),
                Block.box(0, 0, 10, 16, 16, 14), Block.box(5, 5, 14, 11, 11, 16)));
        SHAPES.put(Direction.WEST, Shapes.or(Block.box(12, 0, 0, 16, 16, 16), Block.box(6, 4, 4, 12, 12, 12),
                Block.box(2, 0, 0, 6, 16, 16), Block.box(0, 5, 5, 2, 11, 11)));
        SHAPES.put(Direction.EAST, Shapes.or(Block.box(0, 0, 0, 4, 16, 16), Block.box(4, 4, 4, 10, 12, 12),
                Block.box(10, 0, 0, 14, 16, 16), Block.box(14, 5, 5, 16, 11, 11)));
    }

    public EngineBlock(Properties properties, BiFunction<BlockPos, BlockState, BlockEntity> factory) {
        super(properties, factory);
        registerDefaultState(stateDefinition.any().setValue(FACING, Direction.UP).setValue(LIT, false));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING, LIT);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return defaultBlockState().setValue(FACING, context.getClickedFace().getOpposite());
    }

    @Override
    @SuppressWarnings("deprecation")
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPES.get(state.getValue(FACING));
    }

    @Override
    @SuppressWarnings("deprecation")
    public BlockState rotate(BlockState state, Rotation rotation) {
        return state.setValue(FACING, rotation.rotate(state.getValue(FACING)));
    }

    @Override
    @SuppressWarnings("deprecation")
    public BlockState mirror(BlockState state, Mirror mirror) {
        return state.rotate(mirror.getRotation(state.getValue(FACING)));
    }
}
