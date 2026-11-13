package com.retrotech.registry;

import com.retrotech.RetroTech;
import com.retrotech.menu.MachineKind;
import com.retrotech.menu.MachineMenu;
import net.minecraft.world.inventory.MenuType;
import net.minecraftforge.common.extensions.IForgeMenuType;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public final class ModMenus {
    public static final DeferredRegister<MenuType<?>> MENUS = DeferredRegister.create(ForgeRegistries.MENU_TYPES, RetroTech.MODID);

    public static final RegistryObject<MenuType<MachineMenu>> GENERATOR = MENUS.register("generator",
            () -> IForgeMenuType.create((id, inv, buf) ->
                    MachineMenu.client(ModMenus.GENERATOR.get(), MachineKind.GENERATOR, id, inv)));

    public static final RegistryObject<MenuType<MachineMenu>> ELECTRIC_FURNACE = MENUS.register("electric_furnace",
            () -> IForgeMenuType.create((id, inv, buf) ->
                    MachineMenu.client(ModMenus.ELECTRIC_FURNACE.get(), MachineKind.ELECTRIC_FURNACE, id, inv)));

    public static final RegistryObject<MenuType<MachineMenu>> BATBOX = MENUS.register("batbox",
            () -> IForgeMenuType.create((id, inv, buf) ->
                    MachineMenu.client(ModMenus.BATBOX.get(), MachineKind.BATBOX, id, inv)));

    public static final RegistryObject<MenuType<MachineMenu>> MACERATOR = MENUS.register("macerator",
            () -> IForgeMenuType.create((id, inv, buf) ->
                    MachineMenu.client(ModMenus.MACERATOR.get(), MachineKind.MACERATOR, id, inv)));

    public static final RegistryObject<MenuType<MachineMenu>> COMPRESSOR = MENUS.register("compressor",
            () -> IForgeMenuType.create((id, inv, buf) ->
                    MachineMenu.client(ModMenus.COMPRESSOR.get(), MachineKind.COMPRESSOR, id, inv)));

    public static final RegistryObject<MenuType<MachineMenu>> EXTRACTOR = MENUS.register("extractor",
            () -> IForgeMenuType.create((id, inv, buf) ->
                    MachineMenu.client(ModMenus.EXTRACTOR.get(), MachineKind.EXTRACTOR, id, inv)));

    public static final RegistryObject<MenuType<MachineMenu>> SOLAR_PANEL = MENUS.register("solar_panel",
            () -> IForgeMenuType.create((id, inv, buf) ->
                    MachineMenu.client(ModMenus.SOLAR_PANEL.get(), MachineKind.SOLAR_PANEL, id, inv)));

    public static final RegistryObject<MenuType<MachineMenu>> REDSTONE_ENGINE = MENUS.register("redstone_engine",
            () -> IForgeMenuType.create((id, inv, buf) ->
                    MachineMenu.client(ModMenus.REDSTONE_ENGINE.get(), MachineKind.REDSTONE_ENGINE, id, inv)));

    public static final RegistryObject<MenuType<MachineMenu>> STIRLING_ENGINE = MENUS.register("stirling_engine",
            () -> IForgeMenuType.create((id, inv, buf) ->
                    MachineMenu.client(ModMenus.STIRLING_ENGINE.get(), MachineKind.STIRLING_ENGINE, id, inv)));

    public static final RegistryObject<MenuType<MachineMenu>> QUARRY = MENUS.register("quarry",
            () -> IForgeMenuType.create((id, inv, buf) ->
                    MachineMenu.client(ModMenus.QUARRY.get(), MachineKind.QUARRY, id, inv)));

    private ModMenus() {
    }
}
