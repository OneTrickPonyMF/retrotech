package com.retrotech.registry;

import com.retrotech.RetroTech;
import com.retrotech.blockentity.BatBoxBlockEntity;
import com.retrotech.blockentity.ElectricFurnaceBlockEntity;
import com.retrotech.blockentity.GeneratorBlockEntity;
import com.retrotech.blockentity.EngineBlockEntity;
import com.retrotech.blockentity.ProcessingMachineBlockEntity;
import com.retrotech.blockentity.QuarryBlockEntity;
import com.retrotech.blockentity.PumpBlockEntity;
import com.retrotech.fluid.TankBlockEntity;
import com.retrotech.pipe.FluidPipeBlockEntity;
import com.retrotech.pipe.PipeBlockEntity;
import com.retrotech.blockentity.SolarPanelBlockEntity;
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

    @SuppressWarnings("DataFlowIssue")
    public static final RegistryObject<BlockEntityType<ProcessingMachineBlockEntity>> MACERATOR = BLOCK_ENTITIES.register(
            "macerator", () -> BlockEntityType.Builder.of(ProcessingMachineBlockEntity::macerator,
                    ModBlocks.MACERATOR.get()).build(null));

    @SuppressWarnings("DataFlowIssue")
    public static final RegistryObject<BlockEntityType<ProcessingMachineBlockEntity>> COMPRESSOR = BLOCK_ENTITIES.register(
            "compressor", () -> BlockEntityType.Builder.of(ProcessingMachineBlockEntity::compressor,
                    ModBlocks.COMPRESSOR.get()).build(null));

    @SuppressWarnings("DataFlowIssue")
    public static final RegistryObject<BlockEntityType<ProcessingMachineBlockEntity>> EXTRACTOR = BLOCK_ENTITIES.register(
            "extractor", () -> BlockEntityType.Builder.of(ProcessingMachineBlockEntity::extractor,
                    ModBlocks.EXTRACTOR.get()).build(null));

    @SuppressWarnings("DataFlowIssue")
    public static final RegistryObject<BlockEntityType<SolarPanelBlockEntity>> SOLAR_PANEL = BLOCK_ENTITIES.register(
            "solar_panel", () -> BlockEntityType.Builder.of(SolarPanelBlockEntity::new,
                    ModBlocks.SOLAR_PANEL.get()).build(null));

    @SuppressWarnings("DataFlowIssue")
    public static final RegistryObject<BlockEntityType<EngineBlockEntity>> REDSTONE_ENGINE = BLOCK_ENTITIES.register(
            "redstone_engine", () -> BlockEntityType.Builder.of(EngineBlockEntity::redstone,
                    ModBlocks.REDSTONE_ENGINE.get()).build(null));

    @SuppressWarnings("DataFlowIssue")
    public static final RegistryObject<BlockEntityType<EngineBlockEntity>> STIRLING_ENGINE = BLOCK_ENTITIES.register(
            "stirling_engine", () -> BlockEntityType.Builder.of(EngineBlockEntity::stirling,
                    ModBlocks.STIRLING_ENGINE.get()).build(null));

    @SuppressWarnings("DataFlowIssue")
    public static final RegistryObject<BlockEntityType<QuarryBlockEntity>> QUARRY = BLOCK_ENTITIES.register(
            "quarry", () -> BlockEntityType.Builder.of(QuarryBlockEntity::new, ModBlocks.QUARRY.get()).build(null));

    @SuppressWarnings("DataFlowIssue")
    public static final RegistryObject<BlockEntityType<PipeBlockEntity>> PIPE = BLOCK_ENTITIES.register(
            "pipe", () -> BlockEntityType.Builder.of(PipeBlockEntity::new, ModBlocks.WOODEN_PIPE.get(),
                    ModBlocks.COBBLESTONE_PIPE.get(), ModBlocks.STONE_PIPE.get(), ModBlocks.GOLD_PIPE.get()).build(null));

    @SuppressWarnings("DataFlowIssue")
    public static final RegistryObject<BlockEntityType<FluidPipeBlockEntity>> FLUID_PIPE = BLOCK_ENTITIES.register(
            "fluid_pipe", () -> BlockEntityType.Builder.of(FluidPipeBlockEntity::new, ModBlocks.WOODEN_FLUID_PIPE.get(),
                    ModBlocks.COBBLESTONE_FLUID_PIPE.get(), ModBlocks.STONE_FLUID_PIPE.get(),
                    ModBlocks.GOLD_FLUID_PIPE.get()).build(null));

    @SuppressWarnings("DataFlowIssue")
    public static final RegistryObject<BlockEntityType<TankBlockEntity>> TANK = BLOCK_ENTITIES.register(
            "tank", () -> BlockEntityType.Builder.of(TankBlockEntity::new, ModBlocks.TANK.get()).build(null));

    @SuppressWarnings("DataFlowIssue")
    public static final RegistryObject<BlockEntityType<PumpBlockEntity>> PUMP = BLOCK_ENTITIES.register(
            "pump", () -> BlockEntityType.Builder.of(PumpBlockEntity::new, ModBlocks.PUMP.get()).build(null));

    private ModBlockEntities() {
    }
}
