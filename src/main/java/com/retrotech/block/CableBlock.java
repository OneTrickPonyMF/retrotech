package com.retrotech.block;

import com.retrotech.energy.EnergyNet;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.minecraftforge.common.capabilities.ForgeCapabilities;

import java.util.EnumMap;
import java.util.Map;

/**
 * Cable de energía. No tiene BlockEntity: la red se calcula desde las fuentes (ver EnergyNet).
 */
public class CableBlock extends Block {
    public static final Map<Direction, BooleanProperty> PROPERTIES = new EnumMap<>(Direction.class);

    static {
        PROPERTIES.put(Direction.NORTH, BlockStateProperties.NORTH);
        PROPERTIES.put(Direction.SOUTH, BlockStateProperties.SOUTH);
        PROPERTIES.put(Direction.EAST, BlockStateProperties.EAST);
        PROPERTIES.put(Direction.WEST, BlockStateProperties.WEST);
        PROPERTIES.put(Direction.UP, BlockStateProperties.UP);
        PROPERTIES.put(Direction.DOWN, BlockStateProperties.DOWN);
    }

    private static final VoxelShape CORE = Block.box(6, 6, 6, 10, 10, 10);
    private static final Map<Direction, VoxelShape> ARMS = new EnumMap<>(Direction.class);

    static {
        ARMS.put(Direction.NORTH, Block.box(6, 6, 0, 10, 10, 6));
        ARMS.put(Direction.SOUTH, Block.box(6, 6, 10, 10, 10, 16));
        ARMS.put(Direction.WEST, Block.box(0, 6, 6, 6, 10, 10));
        ARMS.put(Direction.EAST, Block.box(10, 6, 6, 16, 10, 10));
        ARMS.put(Direction.UP, Block.box(6, 10, 6, 10, 16, 10));
        ARMS.put(Direction.DOWN, Block.box(6, 0, 6, 10, 6, 10));
    }

    private static final VoxelShape[] SHAPE_CACHE = new VoxelShape[64];

    private final int capacity;
    private final double lossPerBlock;

    public CableBlock(Properties properties, int capacity, double lossPerBlock) {
        super(properties);
        this.capacity = capacity;
        this.lossPerBlock = lossPerBlock;
        BlockState state = this.stateDefinition.any();
        for (BooleanProperty prop : PROPERTIES.values()) {
            state = state.setValue(prop, false);
        }
        registerDefaultState(state);
    }

    public int getCapacity() {
        return capacity;
    }

    public double getLossPerBlock() {
        return lossPerBlock;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(PROPERTIES.values().toArray(new BooleanProperty[0]));
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        BlockState state = defaultBlockState();
        Level level = context.getLevel();
        BlockPos pos = context.getClickedPos();
        for (Direction dir : Direction.values()) {
            state = state.setValue(PROPERTIES.get(dir), canConnectTo(level, pos.relative(dir), dir));
        }
        return state;
    }

    @Override
    public BlockState updateShape(BlockState state, Direction dir, BlockState neighborState, LevelAccessor level,
                                  BlockPos pos, BlockPos neighborPos) {
        return state.setValue(PROPERTIES.get(dir), canConnectTo(level, neighborPos, dir));
    }

    private static boolean canConnectTo(BlockGetter level, BlockPos neighborPos, Direction dirFromCable) {
        BlockState neighbor = level.getBlockState(neighborPos);
        if (neighbor.getBlock() instanceof CableBlock) return true;
        BlockEntity be = level.getBlockEntity(neighborPos);
        return be != null && be.getCapability(ForgeCapabilities.ENERGY, dirFromCable.getOpposite()).isPresent();
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        int index = 0;
        for (Direction dir : Direction.values()) {
            if (state.getValue(PROPERTIES.get(dir))) index |= 1 << dir.ordinal();
        }
        VoxelShape shape = SHAPE_CACHE[index];
        if (shape == null) {
            shape = CORE;
            for (Direction dir : Direction.values()) {
                if ((index & (1 << dir.ordinal())) != 0) shape = Shapes.or(shape, ARMS.get(dir));
            }
            SHAPE_CACHE[index] = shape;
        }
        return shape;
    }

    @Override
    public void onPlace(BlockState state, Level level, BlockPos pos, BlockState oldState, boolean isMoving) {
        super.onPlace(state, level, pos, oldState, isMoving);
        EnergyNet.invalidate();
    }

    @Override
    @SuppressWarnings("deprecation")
    public void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean isMoving) {
        EnergyNet.invalidate();
        super.onRemove(state, level, pos, newState, isMoving);
    }

    @Override
    public void neighborChanged(BlockState state, Level level, BlockPos pos, Block block, BlockPos fromPos,
                                boolean isMoving) {
        super.neighborChanged(state, level, pos, block, fromPos, isMoving);
        EnergyNet.invalidate();
    }
}
