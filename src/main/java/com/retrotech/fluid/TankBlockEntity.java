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

/** Tanque de 16 baldes. Si hay otro tanque abajo, el fluido cae hacia ese. */
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
    private LazyOptional<IFluidHandler> fluidCap = LazyOptional.of(() -> tank);

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
        fluidCap = LazyOptional.of(() -> tank);
    }
}
