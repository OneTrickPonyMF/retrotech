package com.retrotech.pipe;

import com.retrotech.block.CableBlock;
import com.retrotech.energy.EnergyNet;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.minecraftforge.common.capabilities.ForgeCapabilities;

import java.util.EnumMap;
import java.util.Map;

/** Tubería de items o de fluidos al estilo BuildCraft (8 píxeles de grosor). */
public class PipeBlock extends BaseEntityBlock {
    private static final VoxelShape CORE = Block.box(4, 4, 4, 12, 12, 12);
    private static final Map<Direction, VoxelShape> ARMS = new EnumMap<>(Direction.class);
    private static final VoxelShape[] SHAPE_CACHE = new VoxelShape[64];

    static {
        ARMS.put(Direction.NORTH, Block.box(4, 4, 0, 12, 12, 4));
        ARMS.put(Direction.SOUTH, Block.box(4, 4, 12, 12, 12, 16));
        ARMS.put(Direction.WEST, Block.box(0, 4, 4, 4, 12, 12));
        ARMS.put(Direction.EAST, Block.box(12, 4, 4, 16, 12, 12));
        ARMS.put(Direction.UP, Block.box(4, 12, 4, 12, 16, 12));
        ARMS.put(Direction.DOWN, Block.box(4, 0, 4, 12, 4, 12));
    }

    private final PipeType type;
    private final boolean fluid;

    public PipeBlock(Properties properties, PipeType type, boolean fluid) {
        super(properties);
        this.type = type;
        this.fluid = fluid;
        BlockState state = stateDefinition.any();
        for (BooleanProperty prop : CableBlock.PROPERTIES.values()) {
            state = state.setValue(prop, false);
        }
        registerDefaultState(state);
    }

    public PipeType getType() {
        return type;
    }

    public boolean isFluidPipe() {
        return fluid;
    }

    public static boolean isConnected(BlockState state, Direction dir) {
        return state.getBlock() instanceof PipeBlock && state.getValue(CableBlock.PROPERTIES.get(dir));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(CableBlock.PROPERTIES.values().toArray(new BooleanProperty[0]));
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        BlockState state = defaultBlockState();
        for (Direction dir : Direction.values()) {
            state = state.setValue(CableBlock.PROPERTIES.get(dir),
                    canConnectTo(context.getLevel(), context.getClickedPos().relative(dir), dir));
        }
        return state;
    }

    @Override
    @SuppressWarnings("deprecation")
    public BlockState updateShape(BlockState state, Direction dir, BlockState neighborState, LevelAccessor level,
                                  BlockPos pos, BlockPos neighborPos) {
        return state.setValue(CableBlock.PROPERTIES.get(dir), canConnectTo(level, neighborPos, dir));
    }

    private boolean canConnectTo(BlockGetter level, BlockPos neighborPos, Direction dir) {
        BlockState neighbor = level.getBlockState(neighborPos);
        if (neighbor.getBlock() instanceof PipeBlock other) return other.fluid == fluid && type.canConnectTo(other.type);
        BlockEntity be = level.getBlockEntity(neighborPos);
        if (be == null) return false;
        if (be.getCapability(fluid ? ForgeCapabilities.FLUID_HANDLER : ForgeCapabilities.ITEM_HANDLER,
                dir.getOpposite()).isPresent()) return true;
        // La de madera también se conecta a motores (para recibir energía)
        return type == PipeType.WOODEN && be.getCapability(ForgeCapabilities.ENERGY, dir.getOpposite())
                .map(e -> e.canExtract()).orElse(false);
    }

    @Override
    @SuppressWarnings("deprecation")
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        int index = 0;
        for (Direction dir : Direction.values()) {
            if (state.getValue(CableBlock.PROPERTIES.get(dir))) index |= 1 << dir.ordinal();
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
    @SuppressWarnings("deprecation")
    public RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return fluid ? new FluidPipeBlockEntity(pos, state) : new PipeBlockEntity(pos, state);
    }

    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return (lvl, pos, st, be) -> {
            if (be instanceof PipeBlockEntity pipe) pipe.tick();
            else if (be instanceof FluidPipeBlockEntity pipe && !lvl.isClientSide) pipe.serverTick();
        };
    }

    @Override
    @SuppressWarnings("deprecation")
    public void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean isMoving) {
        if (!state.is(newState.getBlock())) {
            if (level.getBlockEntity(pos) instanceof PipeBlockEntity pipe) pipe.dropAll();
            EnergyNet.invalidate();
        }
        super.onRemove(state, level, pos, newState, isMoving);
    }

    @Override
    @SuppressWarnings("deprecation")
    public void onPlace(BlockState state, Level level, BlockPos pos, BlockState oldState, boolean isMoving) {
        super.onPlace(state, level, pos, oldState, isMoving);
        if (!oldState.is(state.getBlock())) EnergyNet.invalidate();
    }
}
