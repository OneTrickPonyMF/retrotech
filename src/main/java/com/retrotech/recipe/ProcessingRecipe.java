package com.retrotech.recipe;

import com.google.gson.JsonObject;
import net.minecraft.core.RegistryAccess;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.GsonHelper;
import net.minecraft.world.Container;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.item.crafting.ShapedRecipe;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

import java.util.function.Supplier;

/**
 * Receta de una entrada y una salida, usada por el macerador, el compresor y el extractor.
 *
 * Formato JSON:
 * <pre>
 * {
 *   "type": "retrotech:macerating",
 *   "ingredient": { "tag": "forge:ores/tin" },
 *   "count": 1,                                  (opcional, cantidad de entrada)
 *   "result": { "item": "retrotech:tin_dust", "count": 2 }
 * }
 * </pre>
 */
public class ProcessingRecipe implements Recipe<Container> {
    private final ResourceLocation id;
    private final Ingredient ingredient;
    private final int inputCount;
    private final ItemStack result;
    private final Serializer serializer;

    public ProcessingRecipe(ResourceLocation id, Ingredient ingredient, int inputCount, ItemStack result,
                            Serializer serializer) {
        this.id = id;
        this.ingredient = ingredient;
        this.inputCount = inputCount;
        this.result = result;
        this.serializer = serializer;
    }

    public Ingredient getIngredient() {
        return ingredient;
    }

    public int getInputCount() {
        return inputCount;
    }

    public ItemStack getResult() {
        return result;
    }

    @Override
    public boolean matches(Container container, Level level) {
        ItemStack input = container.getItem(0);
        return ingredient.test(input) && input.getCount() >= inputCount;
    }

    @Override
    public ItemStack assemble(Container container, RegistryAccess access) {
        return result.copy();
    }

    @Override
    public boolean canCraftInDimensions(int width, int height) {
        return true;
    }

    @Override
    public ItemStack getResultItem(RegistryAccess access) {
        return result;
    }

    @Override
    public ResourceLocation getId() {
        return id;
    }

    @Override
    public RecipeSerializer<?> getSerializer() {
        return serializer;
    }

    @Override
    public RecipeType<?> getType() {
        return serializer.type.get();
    }

    /** No aparece en el libro de recetas (evita avisos en el log). */
    @Override
    public boolean isSpecial() {
        return true;
    }

    public static class Serializer implements RecipeSerializer<ProcessingRecipe> {
        private final Supplier<RecipeType<ProcessingRecipe>> type;

        public Serializer(Supplier<RecipeType<ProcessingRecipe>> type) {
            this.type = type;
        }

        @Override
        public ProcessingRecipe fromJson(ResourceLocation id, JsonObject json) {
            Ingredient ingredient = Ingredient.fromJson(GsonHelper.getNonNull(json, "ingredient"));
            int count = GsonHelper.getAsInt(json, "count", 1);
            ItemStack result = ShapedRecipe.itemStackFromJson(GsonHelper.getAsJsonObject(json, "result"));
            return new ProcessingRecipe(id, ingredient, count, result, this);
        }

        @Override
        public @Nullable ProcessingRecipe fromNetwork(ResourceLocation id, FriendlyByteBuf buf) {
            Ingredient ingredient = Ingredient.fromNetwork(buf);
            int count = buf.readVarInt();
            ItemStack result = buf.readItem();
            return new ProcessingRecipe(id, ingredient, count, result, this);
        }

        @Override
        public void toNetwork(FriendlyByteBuf buf, ProcessingRecipe recipe) {
            recipe.ingredient.toNetwork(buf);
            buf.writeVarInt(recipe.inputCount);
            buf.writeItem(recipe.result);
        }
    }
}
