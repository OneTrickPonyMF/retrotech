package com.retrotech.block;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RotatedPillarBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.level.block.state.properties.EnumProperty;

/**
 * Tronco de caucho. Los árboles naturales tienen manchas de resina (RESIN = WET) en un costado.
 * Al sacar la resina con el grifo la mancha queda seca (DRY) y se vuelve a llenar con el tiempo.
 * Los troncos colocados por el jugador no tienen mancha (NONE).
 */
public class RubberLogBlock extends RotatedPillarBlock {
    public static final EnumProperty<Resin> RESIN = EnumProperty.create("resin", Resin.class);
    public static final DirectionProperty FACING = BlockStateProperties.HORIZONTAL_FACING;

    public RubberLogBlock(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any()
                .setValue(AXIS, Direction.Axis.Y)
                .setValue(RESIN, Resin.NONE)
                .setValue(FACING, Direction.NORTH));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        super.createBlockStateDefinition(builder);
        builder.add(RESIN, FACING);
    }

    @Override
    public boolean isRandomlyTicking(BlockState state) {
        return state.getValue(RESIN) == Resin.DRY;
    }

    @Override
    @SuppressWarnings("deprecation")
    public void randomTick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        if (state.getValue(RESIN) == Resin.DRY && random.nextInt(6) == 0) {
            level.setBlock(pos, state.setValue(RESIN, Resin.WET), Block.UPDATE_ALL);
        }
    }

    public enum Resin implements StringRepresentable {
        NONE("none"),
        DRY("dry"),
        WET("wet");

        private final String name;

        Resin(String name) {
            this.name = name;
        }

        @Override
        public String getSerializedName() {
            return name;
        }
    }
}
