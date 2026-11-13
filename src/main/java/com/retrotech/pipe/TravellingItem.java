package com.retrotech.pipe;

import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

/**
 * Un item viajando dentro de una tubería.
 * progress: 0 = recién entrado por el lado "input", 0.5 = en el centro, 1 = saliendo por "output".
 */
public class TravellingItem {
    public ItemStack stack;
    /** Lado por el que entró. */
    public Direction input;
    /** Lado por el que va a salir (se elige al llegar al centro). */
    @Nullable
    public Direction output;
    public float progress;
    public float speed;
    public int bounces;

    public TravellingItem(ItemStack stack, Direction input, float speed) {
        this.stack = stack;
        this.input = input;
        this.speed = speed;
    }

    public CompoundTag save() {
        CompoundTag tag = new CompoundTag();
        tag.put("Item", stack.save(new CompoundTag()));
        tag.putByte("In", (byte) input.get3DDataValue());
        tag.putByte("Out", (byte) (output == null ? -1 : output.get3DDataValue()));
        tag.putFloat("Progress", progress);
        tag.putFloat("Speed", speed);
        tag.putByte("Bounces", (byte) bounces);
        return tag;
    }

    public static TravellingItem load(CompoundTag tag) {
        TravellingItem item = new TravellingItem(ItemStack.of(tag.getCompound("Item")),
                Direction.from3DDataValue(tag.getByte("In")), tag.getFloat("Speed"));
        byte out = tag.getByte("Out");
        item.output = out < 0 ? null : Direction.from3DDataValue(out);
        item.progress = tag.getFloat("Progress");
        item.bounces = tag.getByte("Bounces");
        return item;
    }
}
