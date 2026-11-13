package com.retrotech.client;

import com.retrotech.RetroTech;
import com.retrotech.registry.ModBlockEntities;
import com.retrotech.registry.ModMenus;
import net.minecraftforge.client.event.EntityRenderersEvent;
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
            MenuScreens.register(ModMenus.MACERATOR.get(), MachineScreen::new);
            MenuScreens.register(ModMenus.COMPRESSOR.get(), MachineScreen::new);
            MenuScreens.register(ModMenus.EXTRACTOR.get(), MachineScreen::new);
            MenuScreens.register(ModMenus.SOLAR_PANEL.get(), MachineScreen::new);
            MenuScreens.register(ModMenus.REDSTONE_ENGINE.get(), MachineScreen::new);
            MenuScreens.register(ModMenus.STIRLING_ENGINE.get(), MachineScreen::new);
            MenuScreens.register(ModMenus.QUARRY.get(), MachineScreen::new);
            MenuScreens.register(ModMenus.PUMP.get(), MachineScreen::new);
        });
    }

    @SubscribeEvent
    public static void onRegisterRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerBlockEntityRenderer(ModBlockEntities.PIPE.get(), PipeRenderer::new);
        event.registerBlockEntityRenderer(ModBlockEntities.QUARRY.get(), QuarryRenderer::new);
        event.registerBlockEntityRenderer(ModBlockEntities.FLUID_PIPE.get(), FluidPipeRenderer::new);
        event.registerBlockEntityRenderer(ModBlockEntities.TANK.get(), TankRenderer::new);
        event.registerBlockEntityRenderer(ModBlockEntities.PUMP.get(), PumpRenderer::new);
    }
}
