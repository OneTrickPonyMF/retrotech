package com.retrotech.pipe;

import com.retrotech.energy.MachineEnergyStorage;
import com.retrotech.registry.ModBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.Containers;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.energy.IEnergyStorage;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.ItemHandlerHelper;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Lógica de una tubería de items. Los items avanzan del lado de entrada al centro, ahí eligen
 * una salida al azar entre las posibles y siguen hasta la próxima tubería o inventario.
 * El servidor decide todo; el cliente solo recibe la lista y anima el movimiento.
 */
public class PipeBlockEntity extends BlockEntity {
    public static final float BASE_SPEED = 0.06F;
    public static final float GOLD_SPEED = 0.25F;
    private static final int MAX_ITEMS = 64;
    private static final int MAX_BOUNCES = 16;
    /** Energía que gasta la tubería de madera por cada item extraído. */
    private static final int EU_PER_ITEM = 20;

    private final List<TravellingItem> items = new ArrayList<>();
    private final MachineEnergyStorage energy = new MachineEnergyStorage(1280, 64, 0, this::setChanged);
    private LazyOptional<IEnergyStorage> energyCap = LazyOptional.of(() -> energy);
    @SuppressWarnings("unchecked")
    private LazyOptional<IItemHandler>[] itemCaps = new LazyOptional[6];
    private int extractCooldown;

    public PipeBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.PIPE.get(), pos, state);
    }

    public PipeType getPipeType() {
        return getBlockState().getBlock() instanceof PipeBlock pipe ? pipe.getType() : PipeType.STONE;
    }

    public List<TravellingItem> getItems() {
        return Collections.unmodifiableList(items);
    }

    // ---------- Movimiento ----------

    public void tick() {
        if (level == null) return;
        boolean client = level.isClientSide;
        if (!client && getPipeType() == PipeType.WOODEN) tryExtract();
        if (items.isEmpty()) return;

        boolean changed = false;
        List<TravellingItem> leaving = new ArrayList<>();
        List<TravellingItem> dropped = new ArrayList<>();

        for (TravellingItem item : items) {
            if (item.output == null && item.progress + item.speed >= 0.5F) {
                if (client) {
                    item.progress = 0.5F; // espera a que el servidor diga hacia dónde va
                    continue;
                }
                Direction out = chooseOutput(item);
                if (out == null) {
                    dropped.add(item);
                    continue;
                }
                item.output = out;
                item.speed = getPipeType() == PipeType.GOLD ? GOLD_SPEED : Math.max(BASE_SPEED, item.speed - 0.02F);
                changed = true;
            }
            item.progress += item.speed;
            if (item.progress >= 1.0F) leaving.add(item);
        }

        if (client) {
            items.removeAll(leaving); // el servidor ya los mandó a la tubería siguiente
            return;
        }

        for (TravellingItem item : dropped) {
            items.remove(item);
            dropItem(item.stack);
            changed = true;
        }
        for (TravellingItem item : leaving) {
            items.remove(item);
            passOn(item);
            changed = true;
        }
        if (changed) sync();
    }

    /** Elige una salida: otra tubería o un inventario que acepte el item. Si no hay, vuelve por donde vino. */
    @Nullable
    private Direction chooseOutput(TravellingItem item) {
        List<Direction> options = new ArrayList<>();
        for (Direction dir : Direction.values()) {
            if (dir == item.input || !isConnected(dir)) continue;
            BlockEntity neighbor = level.getBlockEntity(worldPosition.relative(dir));
            if (neighbor instanceof PipeBlockEntity) {
                options.add(dir);
            } else if (neighbor != null) {
                IItemHandler handler = neighbor.getCapability(ForgeCapabilities.ITEM_HANDLER, dir.getOpposite()).orElse(null);
                if (handler != null && ItemHandlerHelper.insertItemStacked(handler, item.stack, true).getCount()
                        < item.stack.getCount()) {
                    options.add(dir);
                }
            }
        }
        if (options.isEmpty()) {
            return isConnected(item.input) && item.bounces < MAX_BOUNCES ? item.input : null;
        }
        return options.get(level.random.nextInt(options.size()));
    }

    private void passOn(TravellingItem item) {
        Direction out = item.output;
        BlockEntity neighbor = out == null ? null : level.getBlockEntity(worldPosition.relative(out));
        if (neighbor instanceof PipeBlockEntity pipe && isConnected(out)) {
            if (pipe.receive(item.stack, out.getOpposite(), item.speed)) return;
        } else if (neighbor != null && out != null) {
            IItemHandler handler = neighbor.getCapability(ForgeCapabilities.ITEM_HANDLER, out.getOpposite()).orElse(null);
            if (handler != null) {
                item.stack = ItemHandlerHelper.insertItemStacked(handler, item.stack, false);
                if (item.stack.isEmpty()) return;
            }
        }
        // No entró: rebota hacia el centro y vuelve a elegir salida
        if (out == null || ++item.bounces > MAX_BOUNCES) {
            dropItem(item.stack);
            return;
        }
        item.input = out;
        item.output = null;
        item.progress = 0;
        items.add(item);
    }

    /** Recibe un item desde una tubería vecina o un inventario. Devuelve false si está llena. */
    public boolean receive(ItemStack stack, Direction from, float speed) {
        if (stack.isEmpty() || items.size() >= MAX_ITEMS) return false;
        items.add(new TravellingItem(stack.copy(), from, Math.max(speed, BASE_SPEED)));
        sync();
        return true;
    }

    /** Tubería de madera: con energía, saca items de los inventarios conectados. */
    private void tryExtract() {
        if (--extractCooldown > 0) return;
        extractCooldown = 10;
        int amount = Math.min(64, energy.getEnergyStored() / EU_PER_ITEM);
        if (amount <= 0) return;

        for (Direction dir : Direction.values()) {
            if (!isConnected(dir)) continue;
            BlockEntity neighbor = level.getBlockEntity(worldPosition.relative(dir));
            if (neighbor == null || neighbor instanceof PipeBlockEntity) continue;
            IItemHandler handler = neighbor.getCapability(ForgeCapabilities.ITEM_HANDLER, dir.getOpposite()).orElse(null);
            if (handler == null) continue;
            for (int slot = 0; slot < handler.getSlots(); slot++) {
                ItemStack simulated = handler.extractItem(slot, amount, true);
                if (simulated.isEmpty()) continue;
                ItemStack extracted = handler.extractItem(slot, simulated.getCount(), false);
                if (extracted.isEmpty()) continue;
                if (!receive(extracted, dir, BASE_SPEED)) {
                    ItemHandlerHelper.insertItemStacked(handler, extracted, false);
                    return;
                }
                energy.consume(extracted.getCount() * EU_PER_ITEM);
                return;
            }
        }
    }

    private boolean isConnected(Direction dir) {
        return PipeBlock.isConnected(getBlockState(), dir);
    }

    private void dropItem(ItemStack stack) {
        if (level != null && !stack.isEmpty()) {
            Containers.dropItemStack(level, worldPosition.getX() + 0.5, worldPosition.getY() + 0.5,
                    worldPosition.getZ() + 0.5, stack);
        }
    }

    public void dropAll() {
        for (TravellingItem item : items) dropItem(item.stack);
        items.clear();
    }

    // ---------- Sincronización y guardado ----------

    private void sync() {
        setChanged();
        if (level != null && !level.isClientSide) {
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), Block.UPDATE_CLIENTS);
        }
    }

    private void writeItems(CompoundTag tag) {
        ListTag list = new ListTag();
        for (TravellingItem item : items) list.add(item.save());
        tag.put("Travelling", list);
    }

    private void readItems(CompoundTag tag) {
        items.clear();
        ListTag list = tag.getList("Travelling", Tag.TAG_COMPOUND);
        for (int i = 0; i < list.size(); i++) items.add(TravellingItem.load(list.getCompound(i)));
    }

    @Override
    protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        writeItems(tag);
        tag.putInt("Energy", energy.getEnergyStored());
    }

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
        readItems(tag);
        energy.setEnergy(tag.getInt("Energy"));
    }

    @Override
    public CompoundTag getUpdateTag() {
        CompoundTag tag = new CompoundTag();
        writeItems(tag);
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
        if (cap == ForgeCapabilities.ENERGY && getPipeType() == PipeType.WOODEN) {
            return energyCap.cast();
        }
        if (cap == ForgeCapabilities.ITEM_HANDLER && side != null) {
            int i = side.ordinal();
            if (itemCaps[i] == null) itemCaps[i] = LazyOptional.of(() -> new Inlet(side));
            return itemCaps[i].cast();
        }
        return super.getCapability(cap, side);
    }

    @Override
    public void invalidateCaps() {
        super.invalidateCaps();
        energyCap.invalidate();
        for (LazyOptional<IItemHandler> cap : itemCaps) {
            if (cap != null) cap.invalidate();
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    public void reviveCaps() {
        super.reviveCaps();
        energyCap = LazyOptional.of(() -> energy);
        itemCaps = new LazyOptional[6];
    }

    /** Entrada de items desde máquinas, tolvas o la quarry por un lado de la tubería. */
    private class Inlet implements IItemHandler {
        private final Direction side;

        Inlet(Direction side) {
            this.side = side;
        }

        @Override
        public int getSlots() {
            return 1;
        }

        @NotNull
        @Override
        public ItemStack getStackInSlot(int slot) {
            return ItemStack.EMPTY;
        }

        @NotNull
        @Override
        public ItemStack insertItem(int slot, @NotNull ItemStack stack, boolean simulate) {
            if (stack.isEmpty() || items.size() >= MAX_ITEMS) return stack;
            if (!simulate) receive(stack, side, BASE_SPEED);
            return ItemStack.EMPTY;
        }

        @NotNull
        @Override
        public ItemStack extractItem(int slot, int amount, boolean simulate) {
            return ItemStack.EMPTY;
        }

        @Override
        public int getSlotLimit(int slot) {
            return 64;
        }

        @Override
        public boolean isItemValid(int slot, @NotNull ItemStack stack) {
            return true;
        }
    }
}
