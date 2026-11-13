package com.retrotech.block;

import com.retrotech.RetroTech;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.grower.AbstractTreeGrower;
import net.minecraft.world.level.levelgen.feature.ConfiguredFeature;

/** Hace crecer el brote de caucho usando el árbol definido en worldgen/configured_feature/rubber_tree.json. */
public class RubberTreeGrower extends AbstractTreeGrower {
    public static final ResourceKey<ConfiguredFeature<?, ?>> RUBBER_TREE =
            ResourceKey.create(Registries.CONFIGURED_FEATURE, new ResourceLocation(RetroTech.MODID, "rubber_tree"));

    @Override
    protected ResourceKey<ConfiguredFeature<?, ?>> getConfiguredFeature(RandomSource random, boolean hasFlowers) {
        return RUBBER_TREE;
    }
}
