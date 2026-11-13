package com.retrotech.registry;

import com.retrotech.RetroTech;
import com.retrotech.item.BatteryItem;
import com.retrotech.item.TreetapItem;
import net.minecraft.world.item.Item;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public final class ModItems {
    public static final DeferredRegister<Item> ITEMS = DeferredRegister.create(ForgeRegistries.ITEMS, RetroTech.MODID);

    public static final RegistryObject<Item> RAW_TIN = ITEMS.register("raw_tin", () -> new Item(new Item.Properties()));
    public static final RegistryObject<Item> TIN_INGOT = ITEMS.register("tin_ingot", () -> new Item(new Item.Properties()));

    // ---- Polvos (macerador) ----
    public static final RegistryObject<Item> TIN_DUST = simple("tin_dust");
    public static final RegistryObject<Item> COPPER_DUST = simple("copper_dust");
    public static final RegistryObject<Item> IRON_DUST = simple("iron_dust");
    public static final RegistryObject<Item> GOLD_DUST = simple("gold_dust");
    public static final RegistryObject<Item> COAL_DUST = simple("coal_dust");

    // ---- Caucho ----
    public static final RegistryObject<Item> STICKY_RESIN = simple("sticky_resin");
    public static final RegistryObject<Item> RUBBER = simple("rubber");
    public static final RegistryObject<Item> TREETAP = ITEMS.register("treetap", () -> new TreetapItem(new Item.Properties()));

    // ---- Energía ----
    public static final RegistryObject<BatteryItem> RE_BATTERY = ITEMS.register("re_battery",
            () -> new BatteryItem(new Item.Properties(), 10000, 32));

    private static RegistryObject<Item> simple(String name) {
        return ITEMS.register(name, () -> new Item(new Item.Properties()));
    }

    private ModItems() {
    }
}
