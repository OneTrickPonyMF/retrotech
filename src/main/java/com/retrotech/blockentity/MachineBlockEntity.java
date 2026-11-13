package com.retrotech.blockentity;

import com.retrotech.block.HorizontalMachineBlock;
import com.retrotech.energy.EnergyNet;
import com.retrotech.energy.EnergyTier;
import com.retrotech.energy.ITieredEnergy;
import com.retrotech.energy.MachineEnergyStorage;
import com.retrotech.energy.SidedEnergyView;
import com.retrotech.menu.MachineKind;
import com.retrotech.menu.MachineMenu;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.Containers;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.energy.IEnergyStorage;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.ItemStackHandler;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;

/**
 * Base de todas las máquinas: inventario, energía, guardado, capabilities y datos para la interfaz.
 */
public abstract class MachineBlockEntity extends BlockEntity implements MenuProvider, ITieredEnergy {
    /** Cantidad de valores que se sincronizan con la interfaz (ver MachineMenu). */
    public static final int DATA_COUNT = 6;

    protected final MachineKind kind;
    protected final ItemStackHandler items;
    protected final MachineEnergyStorage energy;
    private final EnergyTier tier;
    private final EnergyNet.Cache netCache = new EnergyNet.Cache();

    protected int progress;
    protected int progressMax;

    private LazyOptional<IItemHandler> itemCap;
    @SuppressWarnings("unchecked")
    private LazyOptional<IEnergyStorage>[] energyCaps = new LazyOptional[7];

    /**
     * Los valores se mandan como 16 bits, así que la energía (que puede pasar de 32767)
     * se parte en dos mitades: 0/1 energía, 2/3 capacidad, 4 progreso, 5 progreso máximo.
     */
    private final ContainerData data = new ContainerData() {
        @Override
        public int get(int index) {
            return switch (index) {
                case 0 -> energy.getEnergyStored() & 0xFFFF;
                case 1 -> (energy.getEnergyStored() >>> 16) & 0xFFFF;
                case 2 -> energy.getMaxEnergyStored() & 0xFFFF;
                case 3 -> (energy.getMaxEnergyStored() >>> 16) & 0xFFFF;
                case 4 -> progress;
                case 5 -> progressMax;
                default -> 0;
            };
        }

        @Override
        public void set(int index, int value) {
            // Solo lectura en el servidor.
        }

        @Override
        public int getCount() {
            return DATA_COUNT;
        }
    };

    protected MachineBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state, MachineKind kind,
                                 EnergyTier tier, int capacity, int maxReceive, int maxExtract) {
        super(type, pos, state);
        this.kind = kind;
        this.tier = tier;
        this.items = kind.createHandler(this::setChanged);
        this.energy = new MachineEnergyStorage(capacity, maxReceive, maxExtract, this::setChanged);
        this.itemCap = LazyOptional.of(() -> new AutomationItemHandler(this));
    }

    /** Se llama una vez por tick, solo en el servidor. */
    public abstract void serverTick();

    /** ¿Entra energía por este lado? (side == null: acceso genérico, por ejemplo de otros mods) */
    protected abstract boolean canReceiveFrom(@Nullable Direction side);

    /** ¿Sale energía por este lado? */
    protected abstract boolean canExtractFrom(@Nullable Direction side);

    /** Tolvas y tuberías: ¿pueden meter items en este slot? */
    protected boolean canAutomationInsert(int slot, ItemStack stack) {
        return kind.isItemValid(slot, stack);
    }

    /** Tolvas y tuberías: ¿pueden sacar items de este slot? */
    protected boolean canAutomationExtract(int slot) {
        return false;
    }

    @Override
    public EnergyTier getEnergyTier() {
        return tier;
    }

    public ItemStackHandler getItems() {
        return items;
    }

    /** Manda energía por la red a través de los lados indicados. */
    protected void pushEnergy(Collection<Direction> sides) {
        if (level == null || energy.getEnergyStored() <= 0) return;
        int sent = EnergyNet.distribute(level, worldPosition, sides, netCache, energy.getEnergyStored(),
                energy.getMaxExtract(), tier);
        energy.consume(sent);
    }

    /** Cambia el estado encendido/apagado (textura del frente) solo si hace falta. */
    protected void setLit(boolean lit) {
        if (level == null) return;
        BlockState state = getBlockState();
        if (state.hasProperty(HorizontalMachineBlock.LIT) && state.getValue(HorizontalMachineBlock.LIT) != lit) {
            level.setBlock(worldPosition, state.setValue(HorizontalMachineBlock.LIT, lit), Block.UPDATE_CLIENTS);
        }
    }

    public void dropContents() {
        if (level == null) return;
        for (int i = 0; i < items.getSlots(); i++) {
            ItemStack stack = items.getStackInSlot(i);
            if (!stack.isEmpty()) {
                Containers.dropItemStack(level, worldPosition.getX(), worldPosition.getY(), worldPosition.getZ(), stack);
            }
        }
    }

    protected abstract MenuType<MachineMenu> getMenuType();

    @Override
    public Component getDisplayName() {
        return getBlockState().getBlock().getName();
    }

    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int id, Inventory inventory, Player player) {
        return new MachineMenu(getMenuType(), kind, id, inventory, items, data,
                ContainerLevelAccess.create(level, worldPosition));
    }

    // ---- Capabilities ----

    @NotNull
    @Override
    public <T> LazyOptional<T> getCapability(@NotNull Capability<T> cap, @Nullable Direction side) {
        if (cap == ForgeCapabilities.ITEM_HANDLER) {
            return itemCap.cast();
        }
        if (cap == ForgeCapabilities.ENERGY) {
            int index = side == null ? 6 : side.ordinal();
            if (energyCaps[index] == null) {
                boolean receive = canReceiveFrom(side);
                boolean extract = canExtractFrom(side);
                energyCaps[index] = (receive || extract)
                        ? LazyOptional.of(() -> new SidedEnergyView(energy, receive, extract))
                        : LazyOptional.empty();
            }
            return energyCaps[index].cast();
        }
        return super.getCapability(cap, side);
    }

    @Override
    public void invalidateCaps() {
        super.invalidateCaps();
        itemCap.invalidate();
        for (LazyOptional<IEnergyStorage> cap : energyCaps) {
            if (cap != null) cap.invalidate();
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    public void reviveCaps() {
        super.reviveCaps();
        itemCap = LazyOptional.of(() -> new AutomationItemHandler(this));
        energyCaps = new LazyOptional[7];
    }

    // ---- Guardado ----

    @Override
    protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        tag.put("Items", items.serializeNBT());
        tag.putInt("Energy", energy.getEnergyStored());
        tag.putInt("Progress", progress);
        tag.putInt("ProgressMax", progressMax);
    }

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
        items.deserializeNBT(tag.getCompound("Items"));
        energy.setEnergy(tag.getInt("Energy"));
        progress = tag.getInt("Progress");
        progressMax = tag.getInt("ProgressMax");
    }
}
