package com.retrotech.menu;

import com.retrotech.blockentity.MachineBlockEntity;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.SlotItemHandler;

/** Menú (contenedor) compartido por todas las máquinas. La distribución depende de MachineKind. */
public class MachineMenu extends AbstractContainerMenu {
    public static final int PLAYER_INV_X = 8;
    public static final int PLAYER_INV_Y = 84;

    private final MachineKind kind;
    private final ContainerData data;
    private final ContainerLevelAccess access;
    private final int machineSlots;

    public MachineMenu(MenuType<?> type, MachineKind kind, int id, Inventory playerInv, IItemHandler handler,
                       ContainerData data, ContainerLevelAccess access) {
        super(type, id);
        this.kind = kind;
        this.data = data;
        this.access = access;
        this.machineSlots = kind.slotCount();
        checkContainerDataCount(data, MachineBlockEntity.DATA_COUNT);

        for (int i = 0; i < machineSlots; i++) {
            addSlot(new SlotItemHandler(handler, i, kind.slotX(i), kind.slotY(i)));
        }
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                addSlot(new Slot(playerInv, col + row * 9 + 9, PLAYER_INV_X + col * 18, PLAYER_INV_Y + row * 18));
            }
        }
        for (int col = 0; col < 9; col++) {
            addSlot(new Slot(playerInv, col, PLAYER_INV_X + col * 18, PLAYER_INV_Y + 58));
        }
        addDataSlots(data);
    }

    /** Constructor del lado del cliente: inventario y datos vacíos que el servidor va llenando. */
    public static MachineMenu client(MenuType<?> type, MachineKind kind, int id, Inventory playerInv) {
        return new MachineMenu(type, kind, id, playerInv, kind.createHandler(() -> {
        }), new SimpleContainerData(MachineBlockEntity.DATA_COUNT), ContainerLevelAccess.NULL);
    }

    public MachineKind getKind() {
        return kind;
    }

    public int getMachineSlots() {
        return machineSlots;
    }

    public int getEnergy() {
        return (data.get(0) & 0xFFFF) | ((data.get(1) & 0xFFFF) << 16);
    }

    public int getCapacity() {
        return (data.get(2) & 0xFFFF) | ((data.get(3) & 0xFFFF) << 16);
    }

    public int getProgress() {
        return data.get(4);
    }

    public int getProgressMax() {
        return data.get(5);
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        Slot slot = slots.get(index);
        if (!slot.hasItem()) return ItemStack.EMPTY;

        ItemStack stack = slot.getItem();
        ItemStack copy = stack.copy();
        int playerStart = machineSlots;
        int playerEnd = machineSlots + 36;

        if (index < machineSlots) {
            if (!moveItemStackTo(stack, playerStart, playerEnd, true)) return ItemStack.EMPTY;
        } else {
            if (machineSlots == 0 || !moveItemStackTo(stack, 0, machineSlots, false)) return ItemStack.EMPTY;
        }

        if (stack.isEmpty()) {
            slot.set(ItemStack.EMPTY);
        } else {
            slot.setChanged();
        }
        if (stack.getCount() == copy.getCount()) return ItemStack.EMPTY;
        slot.onTake(player, stack);
        return copy;
    }

    @Override
    public boolean stillValid(Player player) {
        return access.evaluate((level, pos) -> !level.getBlockState(pos).isAir()
                && player.distanceToSqr(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5) <= 64.0, true);
    }
}
