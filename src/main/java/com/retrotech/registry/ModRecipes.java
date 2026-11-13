package com.retrotech.registry;

import com.retrotech.RetroTech;
import com.retrotech.recipe.ProcessingRecipe;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public final class ModRecipes {
    public static final DeferredRegister<RecipeType<?>> TYPES =
            DeferredRegister.create(ForgeRegistries.RECIPE_TYPES, RetroTech.MODID);
    public static final DeferredRegister<RecipeSerializer<?>> SERIALIZERS =
            DeferredRegister.create(ForgeRegistries.RECIPE_SERIALIZERS, RetroTech.MODID);

    public static final RegistryObject<RecipeType<ProcessingRecipe>> MACERATING = type("macerating");
    public static final RegistryObject<RecipeType<ProcessingRecipe>> COMPRESSING = type("compressing");
    public static final RegistryObject<RecipeType<ProcessingRecipe>> EXTRACTING = type("extracting");

    public static final RegistryObject<RecipeSerializer<ProcessingRecipe>> MACERATING_SERIALIZER =
            SERIALIZERS.register("macerating", () -> new ProcessingRecipe.Serializer(MACERATING));
    public static final RegistryObject<RecipeSerializer<ProcessingRecipe>> COMPRESSING_SERIALIZER =
            SERIALIZERS.register("compressing", () -> new ProcessingRecipe.Serializer(COMPRESSING));
    public static final RegistryObject<RecipeSerializer<ProcessingRecipe>> EXTRACTING_SERIALIZER =
            SERIALIZERS.register("extracting", () -> new ProcessingRecipe.Serializer(EXTRACTING));

    private static RegistryObject<RecipeType<ProcessingRecipe>> type(String name) {
        return TYPES.register(name, () -> RecipeType.simple(new ResourceLocation(RetroTech.MODID, name)));
    }

    private ModRecipes() {
    }
}
