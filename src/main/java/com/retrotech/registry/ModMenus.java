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

    private ModMenus() {
    }
}
