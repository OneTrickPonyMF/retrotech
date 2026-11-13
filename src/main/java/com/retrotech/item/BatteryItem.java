package com.retrotech.item;

import net.minecraft.ChatFormatting;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.common.capabilities.ICapabilityProvider;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.energy.IEnergyStorage;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * Batería recargable. La energía se guarda en el NBT del item ("Energy").
 * Se carga en la BatBox y alimenta máquinas desde su slot de batería.
 */
public class BatteryItem extends Item {
    private static final String TAG = "Energy";

    private final int capacity;
    private final int transfer;

    public BatteryItem(Properties properties, int capacity, int transfer) {
        super(properties.stacksTo(1));
        this.capacity = capacity;
        this.transfer = transfer;
    }

    public int getCapacity() {
        return capacity;
    }

    public static int getEnergy(ItemStack stack) {
        CompoundTag tag = stack.getTag();
        return tag == null ? 0 : tag.getInt(TAG);
    }

    public static void setEnergy(ItemStack stack, int energy) {
        stack.getOrCreateTag().putInt(TAG, energy);
    }

    /** Una batería llena (para la pestaña creativa). */
    public ItemStack charged() {
        ItemStack stack = new ItemStack(this);
        setEnergy(stack, capacity);
        return stack;
    }

    @Override
    public boolean isBarVisible(ItemStack stack) {
        return true;
    }

    @Override
    public int getBarWidth(ItemStack stack) {
        return Math.round(13.0F * Mth.clamp(getEnergy(stack), 0, capacity) / capacity);
    }

    @Override
    public int getBarColor(ItemStack stack) {
        return 0x4FC3F7;
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("tooltip.retrotech.energy", getEnergy(stack), capacity)
                .withStyle(ChatFormatting.GRAY));
    }

    /** Evita que el item "rebote" en la mano cada vez que cambia la carga. */
    @Override
    public boolean shouldCauseReequipAnimation(ItemStack oldStack, ItemStack newStack, boolean slotChanged) {
        return slotChanged || !ItemStack.isSameItem(oldStack, newStack);
    }

    @Override
    public @Nullable ICapabilityProvider initCapabilities(ItemStack stack, @Nullable CompoundTag nbt) {
        return new Provider(stack, capacity, transfer);
    }

    private static class Provider implements ICapabilityProvider {
        private final LazyOptional<IEnergyStorage> energy;

        Provider(ItemStack stack, int capacity, int transfer) {
            this.energy = LazyOptional.of(() -> new Storage(stack, capacity, transfer));
        }

        @NotNull
        @Override
        public <T> LazyOptional<T> getCapability(@NotNull Capability<T> cap, @Nullable Direction side) {
            return ForgeCapabilities.ENERGY.orEmpty(cap, energy);
        }
    }

    private record Storage(ItemStack stack, int capacity, int transfer) implements IEnergyStorage {
        @Override
        public int receiveEnergy(int maxReceive, boolean simulate) {
            int energy = getEnergy(stack);
            int received = Math.min(capacity - energy, Math.min(transfer, maxReceive));
            if (received > 0 && !simulate) setEnergy(stack, energy + received);
            return Math.max(received, 0);
        }

        @Override
        public int extractEnergy(int maxExtract, boolean simulate) {
            int energy = getEnergy(stack);
            int extracted = Math.min(energy, Math.min(transfer, maxExtract));
            if (extracted > 0 && !simulate) setEnergy(stack, energy - extracted);
            return Math.max(extracted, 0);
        }

        @Override
        public int getEnergyStored() {
            return getEnergy(stack);
        }

        @Override
        public int getMaxEnergyStored() {
            return capacity;
        }

        @Override
        public boolean canExtract() {
            return true;
        }

        @Override
        public boolean canReceive() {
            return true;
        }
    }
}
