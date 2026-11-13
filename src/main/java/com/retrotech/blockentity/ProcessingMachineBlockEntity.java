package com.retrotech.blockentity;

import com.retrotech.energy.EnergyTier;
import com.retrotech.menu.MachineKind;
import com.retrotech.menu.MachineMenu;
import com.retrotech.recipe.ProcessingRecipe;
import com.retrotech.registry.ModBlockEntities;
import com.retrotech.registry.ModMenus;
import com.retrotech.registry.ModRecipes;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

import java.util.function.Supplier;

/**
 * Macerador, compresor y extractor: toman una entrada y dan una salida según su tipo de receta.
 * 2 EU/t, 300 ticks por operación (600 EU), como en IC2.
 */
public class ProcessingMachineBlockEntity extends MachineBlockEntity {
    public static final int EU_PER_TICK = 2;
    public static final int OPERATION_TIME = 300;

    private final Supplier<RecipeType<ProcessingRecipe>> recipeType;
    private final Supplier<MenuType<MachineMenu>> menuType;

    private ItemStack cachedInput = ItemStack.EMPTY;
    @Nullable
    private ProcessingRecipe cachedRecipe;

    public ProcessingMachineBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state, MachineKind kind,
                                        Supplier<RecipeType<ProcessingRecipe>> recipeType,
                                        Supplier<MenuType<MachineMenu>> menuType) {
        super(type, pos, state, kind, EnergyTier.LV, 800, 32, 0);
        this.recipeType = recipeType;
        this.menuType = menuType;
        this.progressMax = OPERATION_TIME;
    }

    public static ProcessingMachineBlockEntity macerator(BlockPos pos, BlockState state) {
        return new ProcessingMachineBlockEntity(ModBlockEntities.MACERATOR.get(), pos, state, MachineKind.MACERATOR,
                ModRecipes.MACERATING, ModMenus.MACERATOR);
    }

    public static ProcessingMachineBlockEntity compressor(BlockPos pos, BlockState state) {
        return new ProcessingMachineBlockEntity(ModBlockEntities.COMPRESSOR.get(), pos, state, MachineKind.COMPRESSOR,
                ModRecipes.COMPRESSING, ModMenus.COMPRESSOR);
    }

    public static ProcessingMachineBlockEntity extractor(BlockPos pos, BlockState state) {
        return new ProcessingMachineBlockEntity(ModBlockEntities.EXTRACTOR.get(), pos, state, MachineKind.EXTRACTOR,
                ModRecipes.EXTRACTING, ModMenus.EXTRACTOR);
    }

    @Override
    public void serverTick() {
        progressMax = OPERATION_TIME;
        dischargeFrom(MachineKind.BATTERY);

        ItemStack input = items.getStackInSlot(MachineKind.INPUT);
        ProcessingRecipe recipe = input.isEmpty() ? null : findRecipe(input);
        boolean working = false;

        if (recipe != null && input.getCount() >= recipe.getInputCount() && canOutput(recipe.getResult())) {
            if (energy.getEnergyStored() >= EU_PER_TICK) {
                energy.consume(EU_PER_TICK);
                progress++;
                working = true;
                if (progress >= OPERATION_TIME) {
                    progress = 0;
                    items.extractItem(MachineKind.INPUT, recipe.getInputCount(), false);
                    output(recipe.getResult());
                }
                setChanged();
            }
        } else if (progress != 0) {
            progress = 0;
            setChanged();
        }

        setLit(working);
    }

    @Nullable
    private ProcessingRecipe findRecipe(ItemStack input) {
        if (level == null) return null;
        if (!cachedInput.isEmpty() && ItemStack.isSameItemSameTags(cachedInput, input)) return cachedRecipe;
        cachedInput = input.copyWithCount(1);
        // Se busca con la pila completa para que las recetas con "count" > 1 coincidan.
        cachedRecipe = level.getRecipeManager()
                .getAllRecipesFor(recipeType.get()).stream()
                .filter(r -> r.getIngredient().test(input))
                .findFirst()
                .orElse(null);
        return cachedRecipe;
    }

    private boolean canOutput(ItemStack result) {
        ItemStack out = items.getStackInSlot(MachineKind.OUTPUT);
        if (out.isEmpty()) return true;
        return ItemStack.isSameItemSameTags(out, result) && out.getCount() + result.getCount() <= out.getMaxStackSize();
    }

    private void output(ItemStack result) {
        ItemStack out = items.getStackInSlot(MachineKind.OUTPUT);
        if (out.isEmpty()) {
            items.setStackInSlot(MachineKind.OUTPUT, result.copy());
        } else {
            ItemStack grown = out.copy();
            grown.grow(result.getCount());
            items.setStackInSlot(MachineKind.OUTPUT, grown);
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
    protected boolean canAutomationExtract(int slot) {
        return slot == MachineKind.OUTPUT || (slot == MachineKind.BATTERY && batteryIs(slot, false));
    }

    @Override
    protected MenuType<MachineMenu> getMenuType() {
        return menuType.get();
    }
}
