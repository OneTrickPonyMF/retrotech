package com.retrotech.registry;

import com.retrotech.RetroTech;
import com.retrotech.block.CableBlock;
import com.retrotech.block.HorizontalMachineBlock;
import com.retrotech.block.StorageMachineBlock;
import com.retrotech.blockentity.BatBoxBlockEntity;
import com.retrotech.blockentity.ElectricFurnaceBlockEntity;
import com.retrotech.blockentity.GeneratorBlockEntity;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
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

    // ---- Cables (capacidad EU/t, pérdida por bloque) ----
    public static final RegistryObject<Block> TIN_CABLE = register("tin_cable",
            () -> new CableBlock(cable(), 32, 0.025));
    public static final RegistryObject<Block> COPPER_CABLE = register("copper_cable",
            () -> new CableBlock(cable(), 128, 0.2));
    public static final RegistryObject<Block> GOLD_CABLE = register("gold_cable",
            () -> new CableBlock(cable(), 512, 0.4));

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
