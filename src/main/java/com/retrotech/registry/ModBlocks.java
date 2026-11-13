package com.retrotech.registry;

import com.retrotech.RetroTech;
import com.retrotech.block.CableBlock;
import com.retrotech.block.EngineBlock;
import com.retrotech.block.HorizontalMachineBlock;
import com.retrotech.block.RubberLogBlock;
import com.retrotech.block.RubberTreeGrower;
import com.retrotech.block.SimpleMachineBlock;
import com.retrotech.block.StorageMachineBlock;
import com.retrotech.blockentity.BatBoxBlockEntity;
import com.retrotech.blockentity.ElectricFurnaceBlockEntity;
import com.retrotech.blockentity.GeneratorBlockEntity;
import com.retrotech.blockentity.EngineBlockEntity;
import com.retrotech.blockentity.ProcessingMachineBlockEntity;
import com.retrotech.blockentity.PumpBlockEntity;
import com.retrotech.fluid.TankBlock;
import com.retrotech.blockentity.QuarryBlockEntity;
import com.retrotech.pipe.PipeBlock;
import com.retrotech.pipe.PipeType;
import com.retrotech.blockentity.SolarPanelBlockEntity;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LeavesBlock;
import net.minecraft.world.level.block.SaplingBlock;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.material.MapColor;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

import java.util.function.Supplier;

public final class ModBlocks {
    public static final DeferredRegister<Block> BLOCKS = DeferredRegister.create(ForgeRegistries.BLOCKS, RetroTech.MODID);

    // ---- Recursos ----
    public static final RegistryObject<Block> TIN_ORE = register("tin_ore", () -> new Block(
            BlockBehaviour.Properties.of().mapColor(MapColor.STONE).strength(3.0F, 3.0F)
                    .requiresCorrectToolForDrops()));
    public static final RegistryObject<Block> DEEPSLATE_TIN_ORE = register("deepslate_tin_ore", () -> new Block(
            BlockBehaviour.Properties.of().mapColor(MapColor.DEEPSLATE).strength(4.5F, 3.0F)
                    .requiresCorrectToolForDrops().sound(SoundType.DEEPSLATE)));
    public static final RegistryObject<Block> TIN_BLOCK = register("tin_block", () -> new Block(
            BlockBehaviour.Properties.of().mapColor(MapColor.METAL).strength(5.0F, 6.0F)
                    .requiresCorrectToolForDrops().sound(SoundType.METAL)));

    // ---- Componentes ----
    public static final RegistryObject<Block> MACHINE_CASING = register("machine_casing", () -> new Block(machine()));

    // ---- Cables (capacidad EU/t, pérdida por bloque, grosor en píxeles) ----
    public static final RegistryObject<Block> TIN_CABLE = register("tin_cable",
            () -> new CableBlock(cable(), 32, 0.025, 4));
    public static final RegistryObject<Block> COPPER_CABLE = register("copper_cable",
            () -> new CableBlock(cable(), 128, 0.2, 6));
    public static final RegistryObject<Block> GOLD_CABLE = register("gold_cable",
            () -> new CableBlock(cable(), 512, 0.4, 6));

    // ---- Máquinas ----
    public static final RegistryObject<Block> GENERATOR = register("generator",
            () -> new HorizontalMachineBlock(machine().lightLevel(s -> s.getValue(BlockStateProperties.LIT) ? 13 : 0),
                    GeneratorBlockEntity::new));
    public static final RegistryObject<Block> ELECTRIC_FURNACE = register("electric_furnace",
            () -> new HorizontalMachineBlock(machine().lightLevel(s -> s.getValue(BlockStateProperties.LIT) ? 13 : 0),
                    ElectricFurnaceBlockEntity::new));
    public static final RegistryObject<Block> BATBOX = register("batbox",
            () -> new StorageMachineBlock(BlockBehaviour.Properties.of().mapColor(MapColor.WOOD).strength(1.5F)
                    .sound(SoundType.WOOD), BatBoxBlockEntity::new));

    public static final RegistryObject<Block> MACERATOR = register("macerator",
            () -> new HorizontalMachineBlock(litMachine(), ProcessingMachineBlockEntity::macerator));
    public static final RegistryObject<Block> COMPRESSOR = register("compressor",
            () -> new HorizontalMachineBlock(litMachine(), ProcessingMachineBlockEntity::compressor));
    public static final RegistryObject<Block> EXTRACTOR = register("extractor",
            () -> new HorizontalMachineBlock(litMachine(), ProcessingMachineBlockEntity::extractor));
    public static final RegistryObject<Block> SOLAR_PANEL = register("solar_panel",
            () -> new SimpleMachineBlock(machine(), SolarPanelBlockEntity::new));

    // ---- Árbol de caucho ----
    public static final RegistryObject<Block> RUBBER_LOG = register("rubber_log",
            () -> new RubberLogBlock(BlockBehaviour.Properties.copy(Blocks.JUNGLE_LOG).randomTicks()));
    public static final RegistryObject<Block> RUBBER_LEAVES = register("rubber_leaves",
            () -> new LeavesBlock(BlockBehaviour.Properties.copy(Blocks.OAK_LEAVES)));
    public static final RegistryObject<Block> RUBBER_SAPLING = register("rubber_sapling",
            () -> new SaplingBlock(new RubberTreeGrower(), BlockBehaviour.Properties.copy(Blocks.OAK_SAPLING)));

    // ---- BuildCraft: motores, quarry y tuberías ----
    public static final RegistryObject<Block> REDSTONE_ENGINE = register("redstone_engine",
            () -> new EngineBlock(BlockBehaviour.Properties.of().mapColor(MapColor.WOOD).strength(1.5F)
                    .sound(SoundType.WOOD).noOcclusion(), EngineBlockEntity::redstone));
    public static final RegistryObject<Block> STIRLING_ENGINE = register("stirling_engine",
            () -> new EngineBlock(BlockBehaviour.Properties.of().mapColor(MapColor.STONE).strength(2.5F)
                    .requiresCorrectToolForDrops().sound(SoundType.STONE).noOcclusion(), EngineBlockEntity::stirling));
    public static final RegistryObject<Block> QUARRY = register("quarry",
            () -> new HorizontalMachineBlock(litMachine(), QuarryBlockEntity::new));

    public static final RegistryObject<Block> WOODEN_PIPE = register("wooden_pipe", () -> pipe(PipeType.WOODEN, false));
    public static final RegistryObject<Block> COBBLESTONE_PIPE = register("cobblestone_pipe", () -> pipe(PipeType.COBBLESTONE, false));
    public static final RegistryObject<Block> STONE_PIPE = register("stone_pipe", () -> pipe(PipeType.STONE, false));
    public static final RegistryObject<Block> GOLD_PIPE = register("gold_pipe", () -> pipe(PipeType.GOLD, false));

    // ---- Fluidos ----
    public static final RegistryObject<Block> WOODEN_FLUID_PIPE = register("wooden_fluid_pipe", () -> pipe(PipeType.WOODEN, true));
    public static final RegistryObject<Block> COBBLESTONE_FLUID_PIPE = register("cobblestone_fluid_pipe", () -> pipe(PipeType.COBBLESTONE, true));
    public static final RegistryObject<Block> STONE_FLUID_PIPE = register("stone_fluid_pipe", () -> pipe(PipeType.STONE, true));
    public static final RegistryObject<Block> GOLD_FLUID_PIPE = register("gold_fluid_pipe", () -> pipe(PipeType.GOLD, true));
    public static final RegistryObject<Block> TANK = register("tank",
            () -> new TankBlock(BlockBehaviour.Properties.of().mapColor(MapColor.NONE).strength(0.5F)
                    .sound(SoundType.GLASS).noOcclusion()));
    public static final RegistryObject<Block> PUMP = register("pump",
            () -> new SimpleMachineBlock(machine(), PumpBlockEntity::new));
    public static final RegistryObject<Block> PUMP_TUBE = BLOCKS.register("pump_tube",
            () -> new Block(BlockBehaviour.Properties.of().mapColor(MapColor.METAL).strength(-1.0F, 3600000.0F)
                    .noLootTable().noOcclusion()));

    /** Bloques que coloca la quarry (sin item). */
    public static final RegistryObject<Block> QUARRY_FRAME = BLOCKS.register("quarry_frame",
            () -> new Block(BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_YELLOW).strength(0.5F)
                    .sound(SoundType.METAL).noOcclusion()));
    public static final RegistryObject<Block> QUARRY_DRILL = BLOCKS.register("quarry_drill",
            () -> new Block(BlockBehaviour.Properties.of().mapColor(MapColor.METAL).strength(-1.0F, 3600000.0F)
                    .noLootTable().noOcclusion()));

    private static PipeBlock pipe(PipeType type, boolean fluid) {
        return new PipeBlock(BlockBehaviour.Properties.of().mapColor(MapColor.NONE).strength(0.25F)
                .sound(SoundType.GLASS).noOcclusion(), type, fluid);
    }

    private static BlockBehaviour.Properties litMachine() {
        return machine().lightLevel(s -> s.getValue(BlockStateProperties.LIT) ? 13 : 0);
    }

    private static BlockBehaviour.Properties machine() {
        return BlockBehaviour.Properties.of().mapColor(MapColor.METAL).strength(3.5F, 6.0F)
                .requiresCorrectToolForDrops().sound(SoundType.METAL);
    }

    private static BlockBehaviour.Properties cable() {
        return BlockBehaviour.Properties.of().mapColor(MapColor.WOOL).strength(0.3F).sound(SoundType.WOOL)
                .noOcclusion();
    }

    /** Registra el bloque y su item. */
    private static <T extends Block> RegistryObject<T> register(String name, Supplier<T> supplier) {
        RegistryObject<T> block = BLOCKS.register(name, supplier);
        ModItems.ITEMS.register(name, () -> new BlockItem(block.get(), new Item.Properties()));
        return block;
    }

    private ModBlocks() {
    }
}
