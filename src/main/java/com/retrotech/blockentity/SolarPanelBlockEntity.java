package com.retrotech.blockentity;

import com.retrotech.energy.EnergyTier;
import com.retrotech.menu.MachineKind;
import com.retrotech.menu.MachineMenu;
import com.retrotech.registry.ModBlockEntities;
import com.retrotech.registry.ModMenus;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * Panel solar: 1 EU/t de día, con cielo despejado y sin lluvia. Emite por los costados y por abajo.
 */
public class SolarPanelBlockEntity extends MachineBlockEntity {
    private static final List<Direction> OUTPUT_SIDES =
            List.of(Direction.DOWN, Direction.NORTH, Direction.SOUTH, Direction.EAST, Direction.WEST);

    private boolean sunny;

    public SolarPanelBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.SOLAR_PANEL.get(), pos, state, MachineKind.SOLAR_PANEL, EnergyTier.LV, 100, 0, 32);
        this.progressMax = 1;
    }

    @Override
    public void serverTick() {
        if (level == null) return;
        // Revisar el cielo una vez por segundo alcanza (y es más liviano).
        if ((level.getGameTime() + worldPosition.asLong()) % 20 == 0) {
            sunny = level.dimensionType().hasSkyLight()
                    && level.isDay()
                    && !level.isRaining()
                    && level.canSeeSky(worldPosition.above());
            progress = sunny ? 1 : 0;
        }
        if (sunny) energy.generate(1);
        pushEnergy(OUTPUT_SIDES);
    }

    @Override
    protected boolean canReceiveFrom(@Nullable Direction side) {
        return false;
    }

    @Override
    protected boolean canExtractFrom(@Nullable Direction side) {
        return side != Direction.UP;
    }

    @Override
    protected MenuType<MachineMenu> getMenuType() {
        return ModMenus.SOLAR_PANEL.get();
    }
}
