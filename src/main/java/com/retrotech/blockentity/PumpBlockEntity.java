package com.retrotech.blockentity;

import com.retrotech.energy.EnergyTier;
import com.retrotech.menu.MachineKind;
import com.retrotech.menu.MachineMenu;
import com.retrotech.registry.ModBlockEntities;
import com.retrotech.registry.ModMenus;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LiquidBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.phys.AABB;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.capability.IFluidHandler;
import net.minecraftforge.fluids.capability.templates.FluidTank;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayDeque;
import java.util.HashSet;
import java.util.Set;

/**
 * Bomba al estilo BuildCraft: baja un tubo hasta encontrar un fluido y saca bloques fuente
 * (de los más altos y lejanos primero, así no deja "agujeros" que se vuelvan a llenar).
 * 40 EU por balde. Manda el fluido a tuberías o tanques pegados (no por abajo).
 */
public class PumpBlockEntity extends MachineBlockEntity {
    public static final int CAPACITY = 4000;
    private static final int EU_PER_BUCKET = 40;
    private static final int MAX_SEARCH = 2048;
    private static final int MAX_RADIUS = 32;

    private final FluidTank tank = new FluidTank(CAPACITY) {
        @Override
        protected void onContentsChanged() {
            setChanged();
        }
    };
    private LazyOptional<IFluidHandler> fluidCap = LazyOptional.of(() -> tank);
    private int cooldown;
    /** Altura hasta donde llega el tubo (para dibujarlo). */
    private int tubeY = Integer.MAX_VALUE;

    public PumpBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.PUMP.get(), pos, state, MachineKind.PUMP, EnergyTier.LV, 2000, 32, 0);
        this.progressMax = CAPACITY;
    }

    public int getTubeY() {
        return tubeY;
    }

    @Override
    public void serverTick() {
        if (level == null) return;
        if (cooldown > 0) {
            cooldown--;
        } else {
            cooldown = 10;
            if (CAPACITY - tank.getFluidAmount() >= 1000 && energy.getEnergyStored() >= EU_PER_BUCKET) {
                pumpOnce();
            }
        }
        pushFluid();
        progress = tank.getFluidAmount();
        progressMax = CAPACITY;
    }

    private void pumpOnce() {
        // Bajar el tubo hasta el primer fluido
        BlockPos p = worldPosition.below();
        while (p.getY() >= level.getMinBuildHeight()) {
            BlockState state = level.getBlockState(p);
            if (state.getBlock() instanceof LiquidBlock) break;
            if (!state.isAir()) {
                setTube(Integer.MAX_VALUE);
                return;
            }
            p = p.below();
        }
        if (p.getY() < level.getMinBuildHeight()) {
            setTube(Integer.MAX_VALUE);
            return;
        }
        setTube(p.getY());

        BlockPos source = findSource(p);
        if (source == null) return;
        FluidState fluidState = level.getBlockState(source).getFluidState();
        FluidStack stack = new FluidStack(fluidState.getType(), 1000);
        if (tank.fill(stack, IFluidHandler.FluidAction.SIMULATE) < 1000) return;
        tank.fill(stack, IFluidHandler.FluidAction.EXECUTE);
        level.setBlock(source, Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL);
        energy.consume(EU_PER_BUCKET);
    }

    /** Busca, entre el fluido conectado, el bloque fuente más alto y más lejano. */
    @Nullable
    private BlockPos findSource(BlockPos start) {
        Fluid fluid = level.getBlockState(start).getFluidState().getType();
        Set<BlockPos> visited = new HashSet<>();
        ArrayDeque<BlockPos> queue = new ArrayDeque<>();
        queue.add(start);
        visited.add(start);
        BlockPos best = null;
        int bestY = Integer.MIN_VALUE;
        double bestDist = -1;

        while (!queue.isEmpty() && visited.size() < MAX_SEARCH) {
            BlockPos p = queue.poll();
            FluidState fs = level.getBlockState(p).getFluidState();
            if (fs.isSource()) {
                double dist = p.distSqr(start);
                if (p.getY() > bestY || (p.getY() == bestY && dist > bestDist)) {
                    best = p;
                    bestY = p.getY();
                    bestDist = dist;
                }
            }
            for (Direction dir : Direction.values()) {
                BlockPos next = p.relative(dir);
                if (visited.contains(next) || !level.isLoaded(next)) continue;
                if (Math.abs(next.getX() - start.getX()) > MAX_RADIUS || Math.abs(next.getZ() - start.getZ()) > MAX_RADIUS) {
                    continue;
                }
                BlockState state = level.getBlockState(next);
                if (state.getBlock() instanceof LiquidBlock && state.getFluidState().getType().isSame(fluid)) {
                    visited.add(next);
                    queue.add(next);
                }
            }
        }
        return best;
    }

    private void pushFluid() {
        if (tank.isEmpty()) return;
        for (Direction dir : Direction.values()) {
            if (dir == Direction.DOWN || tank.isEmpty()) continue;
            BlockEntity neighbor = level.getBlockEntity(worldPosition.relative(dir));
            if (neighbor == null) continue;
            IFluidHandler handler = neighbor.getCapability(ForgeCapabilities.FLUID_HANDLER, dir.getOpposite()).orElse(null);
            if (handler == null) continue;
            FluidStack offer = tank.getFluid().copy();
            offer.setAmount(Math.min(200, offer.getAmount()));
            int filled = handler.fill(offer, IFluidHandler.FluidAction.EXECUTE);
            if (filled > 0) tank.drain(filled, IFluidHandler.FluidAction.EXECUTE);
        }
    }

    private void setTube(int y) {
        if (tubeY != y) {
            tubeY = y;
            setChanged();
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), Block.UPDATE_CLIENTS);
        }
    }

    @Override
    protected boolean canReceiveFrom(@Nullable Direction side) {
        return true;
    }

    @Override
    protected boolean canExtractFrom(@Nullable Direction side) {
        return false;
    }

    @Override
    protected MenuType<MachineMenu> getMenuType() {
        return ModMenus.PUMP.get();
    }

    // ---------- Guardado y sincronización ----------

    @Override
    protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        tag.put("Tank", tank.writeToNBT(new CompoundTag()));
        tag.putInt("TubeY", tubeY);
    }

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
        if (tag.contains("Tank")) tank.readFromNBT(tag.getCompound("Tank"));
        tubeY = tag.contains("TubeY") ? tag.getInt("TubeY") : Integer.MAX_VALUE;
    }

    @Override
    public CompoundTag getUpdateTag() {
        CompoundTag tag = new CompoundTag();
        tag.putInt("TubeY", tubeY);
        return tag;
    }

    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    @Override
    public AABB getRenderBoundingBox() {
        return INFINITE_EXTENT_AABB;
    }

    // ---------- Capabilities ----------

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
