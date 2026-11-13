package com.retrotech;

import com.retrotech.registry.ModBlockEntities;
import com.retrotech.registry.ModBlocks;
import com.retrotech.registry.ModCreativeTabs;
import com.retrotech.registry.ModItems;
import com.retrotech.registry.ModMenus;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;

@Mod(RetroTech.MODID)
public class RetroTech {
    public static final String MODID = "retrotech";

    public RetroTech() {
        IEventBus modBus = FMLJavaModLoadingContext.get().getModEventBus();
        ModBlocks.BLOCKS.register(modBus); // también crea los items de los bloques
        ModItems.ITEMS.register(modBus);
        ModBlockEntities.BLOCK_ENTITIES.register(modBus);
        ModMenus.MENUS.register(modBus);
        ModCreativeTabs.TABS.register(modBus);
    }
}
