package com.retrotech.pipe;

import com.retrotech.energy.MachineEnergyStorage;
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
import net.minecraftforge.energy.IEnergyStorage;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.capability.IFluidHandler;
import net.minecraftforge.fluids.capability.templates.FluidTank;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Tubería de fluidos. Cada tubería guarda un poco de fluido (250 mB) y en cada tick:
 * - lo mete en tanques y máquinas conectados (salvo la de madera),
 * - lo reparte con las tuberías vecinas que tienen menos (y siempre hacia abajo).
 * La de madera saca fluido de lo que tenga pegado cuando recibe energía de un motor.
 */
public class FluidPipeBlockEntity extends BlockEntity {
    public static final int CAPACITY = 250;
    /** mB de fluido extraídos por cada EU (tubería de madera). */
    private static final int MB_PER_EU = 10;

    private boolean needsSync;
    private final FluidTank tank = new FluidTank(CAPACITY) {
        @Override
        protected void onContentsChanged() {
            setChanged();
            needsSync = true;
        }
    };
    private final MachineEnergyStorage energy = new MachineEnergyStorage(400, 32, 0, this::setChanged);
    private LazyOptional<IFluidHandler> fluidCap = LazyOptional.of(Inlet::new);
    private LazyOptional<IEnergyStorage> energyCap = LazyOptional.of(() -> energy);

    public FluidPipeBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.FLUID_PIPE.get(), pos, state);
    }

    public PipeType getPipeType() {
        return getBlockState().getBlock() instanceof PipeBlock pipe ? pipe.getType() : PipeType.STONE;
    }

    public FluidStack getFluid() {
        return tank.getFluid();
    }

    private boolean isConnected(Direction dir) {
        return PipeBlock.isConnected(getBlockState(), dir);
    }

    public void serverTick() {
        if (level == null) return;
        PipeType type = getPipeType();
        if (type == PipeType.WOODEN) extract();
        if (!tank.isEmpty()) distribute(type);
        if (needsSync && level.getGameTime() % 4 == 0) {
            needsSync = false;
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), Block.UPDATE_CLIENTS);
        }
    }

    private void extract() {
        int room = CAPACITY - tank.getFluidAmount();
        int max = Math.min(Math.min(100, room), energy.getEnergyStored() * MB_PER_EU);
        if (max <= 0) return;
        for (Direction dir : Direction.values()) {
            if (!isConnected(dir)) continue;
            BlockEntity neighbor = level.getBlockEntity(worldPosition.relative(dir));
            if (neighbor == null || neighbor instanceof FluidPipeBlockEntity) continue;
            IFluidHandler handler = neighbor.getCapability(ForgeCapabilities.FLUID_HANDLER, dir.getOpposite()).orElse(null);
            if (handler == null) continue;
            FluidStack simulated = handler.drain(max, IFluidHandler.FluidAction.SIMULATE);
            if (simulated.isEmpty()) continue;
            int accepted = tank.fill(simulated, IFluidHandler.FluidAction.SIMULATE);
            if (accepted <= 0) continue;
            FluidStack drained = handler.drain(accepted, IFluidHandler.FluidAction.EXECUTE);
            tank.fill(drained, IFluidHandler.FluidAction.EXECUTE);
            energy.consume((drained.getAmount() + MB_PER_EU - 1) / MB_PER_EU);
            return;
        }
    }

    private void distribute(PipeType type) {
        int rate = type == PipeType.GOLD ? 200 : 50;

        // 1) Tanques y máquinas (la de madera no devuelve fluido a lo que tiene pegado)
        if (type != PipeType.WOODEN) {
            for (Direction dir : Direction.values()) {
                if (tank.isEmpty()) return;
                if (!isConnected(dir)) continue;
                BlockEntity neighbor = level.getBlockEntity(worldPosition.relative(dir));
                if (neighbor == null || neighbor instanceof FluidPipeBlockEntity) continue;
                IFluidHandler handler = neighbor.getCapability(ForgeCapabilities.FLUID_HANDLER, dir.getOpposite())
                        .orElse(null);
                if (handler == null) continue;
                FluidStack offer = tank.getFluid().copy();
                offer.setAmount(Math.min(rate, offer.getAmount()));
                int filled = handler.fill(offer, IFluidHandler.FluidAction.EXECUTE);
                if (filled > 0) tank.drain(filled, IFluidHandler.FluidAction.EXECUTE);
            }
        }

        // 2) Tuberías vecinas: se nivela (y hacia abajo cae todo lo que entre)
        for (Direction dir : Direction.values()) {
            if (tank.isEmpty()) return;
            if (!isConnected(dir)) continue;
            if (!(level.getBlockEntity(worldPosition.relative(dir)) instanceof FluidPipeBlockEntity other)) continue;
            if (other.getPipeType() == PipeType.WOODEN) continue;
            FluidStack theirs = other.tank.getFluid();
            if (!theirs.isEmpty() && !theirs.isFluidEqual(tank.getFluid())) continue;

            int mine = tank.getFluidAmount();
            int move = dir == Direction.DOWN ? Math.min(rate, mine)
                    : Math.min(rate, (mine - theirs.getAmount()) / 2);
            if (move <= 0) continue;
            FluidStack offer = tank.getFluid().copy();
            offer.setAmount(move);
            int filled = other.tank.fill(offer, IFluidHandler.FluidAction.EXECUTE);
            if (filled > 0) tank.drain(filled, IFluidHandler.FluidAction.EXECUTE);
        }
    }

    // ---------- Guardado y sincronización ----------

    @Override
    protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        tag.put("Tank", tank.writeToNBT(new CompoundTag()));
        tag.putInt("Energy", energy.getEnergyStored());
    }

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
        tank.readFromNBT(tag.getCompound("Tank"));
        energy.setEnergy(tag.getInt("Energy"));
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

    // ---------- Capabilities ----------

    @NotNull
    @Override
    public <T> LazyOptional<T> getCapability(@NotNull Capability<T> cap, @Nullable Direction side) {
        if (cap == ForgeCapabilities.FLUID_HANDLER) return fluidCap.cast();
        if (cap == ForgeCapabilities.ENERGY && getPipeType() == PipeType.WOODEN) return energyCap.cast();
        return super.getCapability(cap, side);
    }

    @Override
    public void invalidateCaps() {
        super.invalidateCaps();
        fluidCap.invalidate();
        energyCap.invalidate();
    }

    @Override
    public void reviveCaps() {
        super.reviveCaps();
        fluidCap = LazyOptional.of(Inlet::new);
        energyCap = LazyOptional.of(() -> energy);
    }

    /** Lo que ven bombas y otros mods: se puede llenar la tubería, pero no vaciarla desde afuera. */
    private class Inlet implements IFluidHandler {
        @Override
        public int getTanks() {
            return 1;
        }

        @NotNull
        @Override
        public FluidStack getFluidInTank(int t) {
            return tank.getFluid();
        }

        @Override
        public int getTankCapacity(int t) {
            return CAPACITY;
        }

        @Override
        public boolean isFluidValid(int t, @NotNull FluidStack stack) {
            return true;
        }

        @Override
        public int fill(FluidStack resource, FluidAction action) {
            return tank.fill(resource, action);
        }

        @NotNull
        @Override
        public FluidStack drain(FluidStack resource, FluidAction action) {
            return FluidStack.EMPTY;
        }

        @NotNull
        @Override
        public FluidStack drain(int maxDrain, FluidAction action) {
            return FluidStack.EMPTY;
        }
    }
}
