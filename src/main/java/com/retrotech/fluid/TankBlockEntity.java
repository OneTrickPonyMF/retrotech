package com.retrotech.fluid;

import com.retrotech.registry.ModBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.capability.IFluidHandler;
import net.minecraftforge.fluids.capability.templates.FluidTank;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

/**
 * Tanque de 16 baldes. Los tanques apilados funcionan como uno solo:
 * se llenan desde el de abajo hacia arriba y se vacían desde el de arriba hacia abajo.
 */
public class TankBlockEntity extends BlockEntity {
    public static final int CAPACITY = 16000;

    private boolean needsSync;
    private final FluidTank tank = new FluidTank(CAPACITY) {
        @Override
        protected void onContentsChanged() {
            setChanged();
            needsSync = true;
        }
    };
    private LazyOptional<IFluidHandler> fluidCap = LazyOptional.of(ColumnHandler::new);

    public TankBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.TANK.get(), pos, state);
    }

    public FluidTank getTank() {
        return tank;
    }

    public void serverTick() {
        if (level == null) return;
        // Gravedad: pasa fluido al tanque de abajo
        if (!tank.isEmpty() && level.getBlockEntity(worldPosition.below()) instanceof TankBlockEntity below) {
            FluidStack offer = tank.getFluid().copy();
            offer.setAmount(Math.min(1000, offer.getAmount()));
            int filled = below.tank.fill(offer, IFluidHandler.FluidAction.EXECUTE);
            if (filled > 0) tank.drain(filled, IFluidHandler.FluidAction.EXECUTE);
        }
        if (needsSync && level.getGameTime() % 4 == 0) {
            needsSync = false;
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), Block.UPDATE_CLIENTS);
        }
    }

    @Override
    protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        tag.put("Tank", tank.writeToNBT(new CompoundTag()));
    }

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
        tank.readFromNBT(tag.getCompound("Tank"));
    }

    @Override
    public CompoundTag getUpdateTag() {
        CompoundTag tag = new CompoundTag();
        tag.put("Tank", tank.writeToNBT(new CompoundTag()));
        return tag;
    }

    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    @NotNull
    @Override
    public <T> LazyOptional<T> getCapability(@NotNull Capability<T> cap, @Nullable Direction side) {
        if (cap == ForgeCapabilities.FLUID_HANDLER) return fluidCap.cast();
        return super.getCapability(cap, side);
    }

    @Override
    public void invalidateCaps() {
        super.invalidateCaps();
        fluidCap.invalidate();
    }

    @Override
    public void reviveCaps() {
        super.reviveCaps();
        fluidCap = LazyOptional.of(ColumnHandler::new);
    }

    /** Todos los tanques de la columna, de abajo hacia arriba. */
    private List<TankBlockEntity> column() {
        List<TankBlockEntity> list = new ArrayList<>();
        if (level == null) {
            list.add(this);
            return list;
        }
        BlockPos p = worldPosition;
        for (int i = 0; i < 64 && level.getBlockEntity(p.below()) instanceof TankBlockEntity; i++) p = p.below();
        for (int i = 0; i < 64 && level.getBlockEntity(p) instanceof TankBlockEntity t; i++) {
            list.add(t);
            p = p.above();
        }
        if (list.isEmpty()) list.add(this);
        return list;
    }

    /** Lo que ven los baldes, tuberías y bombas: la columna entera como un solo tanque. */
    private class ColumnHandler implements IFluidHandler {
        @Override
        public int getTanks() {
            return 1;
        }

        @NotNull
        @Override
        public FluidStack getFluidInTank(int t) {
            FluidStack total = FluidStack.EMPTY;
            for (TankBlockEntity tank : column()) {
                FluidStack f = tank.tank.getFluid();
                if (f.isEmpty()) continue;
                if (total.isEmpty()) {
                    total = f.copy();
                } else if (total.isFluidEqual(f)) {
                    total.grow(f.getAmount());
                }
            }
            return total;
        }

        @Override
        public int getTankCapacity(int t) {
            return column().size() * CAPACITY;
        }

        @Override
        public boolean isFluidValid(int t, @NotNull FluidStack stack) {
            return true;
        }

        @Override
        public int fill(FluidStack resource, FluidAction action) {
            if (resource.isEmpty()) return 0;
            int remaining = resource.getAmount();
            for (TankBlockEntity tank : column()) {
                FluidStack inside = tank.tank.getFluid();
                if (!inside.isEmpty() && !inside.isFluidEqual(resource)) break; // otro fluido: no sigue subiendo
                FluidStack part = resource.copy();
                part.setAmount(remaining);
                remaining -= tank.tank.fill(part, action);
                if (remaining <= 0) break;
            }
            return resource.getAmount() - remaining;
        }

        @NotNull
        @Override
        public FluidStack drain(FluidStack resource, FluidAction action) {
            FluidStack current = getFluidInTank(0);
            if (resource.isEmpty() || current.isEmpty() || !current.isFluidEqual(resource)) return FluidStack.EMPTY;
            return drain(resource.getAmount(), action);
        }

        @NotNull
        @Override
        public FluidStack drain(int maxDrain, FluidAction action) {
            List<TankBlockEntity> tanks = column();
            FluidStack result = FluidStack.EMPTY;
            int remaining = maxDrain;
            for (int i = tanks.size() - 1; i >= 0 && remaining > 0; i--) {
                FluidTank tank = tanks.get(i).tank;
                if (tank.isEmpty()) continue;
                if (!result.isEmpty() && !result.isFluidEqual(tank.getFluid())) break;
                FluidStack drained = tank.drain(remaining, action);
                if (drained.isEmpty()) continue;
                if (result.isEmpty()) {
                    result = drained.copy();
                } else {
                    result.grow(drained.getAmount());
                }
                remaining -= drained.getAmount();
            }
            return result;
        }
    }
}
