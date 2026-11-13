package com.retrotech.client;

import com.retrotech.RetroTech;
import com.retrotech.registry.ModMenus;
import net.minecraft.client.gui.screens.MenuScreens;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;

@Mod.EventBusSubscriber(modid = RetroTech.MODID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public final class ClientSetup {
    private ClientSetup() {
    }

    @SubscribeEvent
    public static void onClientSetup(FMLClientSetupEvent event) {
        event.enqueueWork(() -> {
            MenuScreens.register(ModMenus.GENERATOR.get(), MachineScreen::new);
            MenuScreens.register(ModMenus.ELECTRIC_FURNACE.get(), MachineScreen::new);
            MenuScreens.register(ModMenus.BATBOX.get(), MachineScreen::new);
        });
    }
}
