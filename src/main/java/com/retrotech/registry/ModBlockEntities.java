package com.retrotech.registry;

import com.retrotech.RetroTech;
import com.retrotech.blockentity.BatBoxBlockEntity;
import com.retrotech.blockentity.ElectricFurnaceBlockEntity;
import com.retrotech.blockentity.GeneratorBlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public final class ModBlockEntities {
    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES =
            DeferredRegister.create(ForgeRegistries.BLOCK_ENTITY_TYPES, RetroTech.MODID);

    @SuppressWarnings("DataFlowIssue")
    public static final RegistryObject<BlockEntityType<GeneratorBlockEntity>> GENERATOR = BLOCK_ENTITIES.register(
            "generator", () -> BlockEntityType.Builder.of(GeneratorBlockEntity::new, ModBlocks.GENERATOR.get()).build(null));

    @SuppressWarnings("DataFlowIssue")
    public static final RegistryObject<BlockEntityType<ElectricFurnaceBlockEntity>> ELECTRIC_FURNACE = BLOCK_ENTITIES.register(
            "electric_furnace", () -> BlockEntityType.Builder.of(ElectricFurnaceBlockEntity::new,
                    ModBlocks.ELECTRIC_FURNACE.get()).build(null));

    @SuppressWarnings("DataFlowIssue")
    public static final RegistryObject<BlockEntityType<BatBoxBlockEntity>> BATBOX = BLOCK_ENTITIES.register(
            "batbox", () -> BlockEntityType.Builder.of(BatBoxBlockEntity::new, ModBlocks.BATBOX.get()).build(null));

    private ModBlockEntities() {
    }
}
