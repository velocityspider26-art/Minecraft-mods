package com.vantage.client.gen;

import net.minecraft.core.HolderGetter;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.registries.VanillaRegistries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.Biomes;
import net.minecraft.world.level.block.Blocks;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BiomeLookTest {
    private static final HolderLookup.Provider LOOKUP = VanillaRegistries.createLookup();

    private static BiomeLook look(ResourceKey<Biome> key) {
        HolderGetter<Biome> biomes = LOOKUP.lookupOrThrow(Registries.BIOME);
        BiomeLook l = BiomeLook.resolve(biomes.getOrThrow(key), Blocks.STONE.defaultBlockState());
        System.out.printf("[look] %s: leaves=%s canopy=%.2f height=%d%n", key.location(), l.leaves(), l.canopy(), l.treeHeight());
        return l;
    }

    @Test
    void treesComeFromBiomeFeatures() {
        BiomeLook forest = look(Biomes.FOREST);
        assertTrue(forest.canopy() > 0.6f);
        assertTrue(forest.leaves() != null && (forest.leaves().is(Blocks.OAK_LEAVES) || forest.leaves().is(Blocks.BIRCH_LEAVES)));

        BiomeLook taiga = look(Biomes.TAIGA);
        assertEquals(Blocks.SPRUCE_LEAVES, taiga.leaves().getBlock());
        assertTrue(taiga.canopy() > 0.6f);

        BiomeLook jungle = look(Biomes.JUNGLE);
        assertTrue(jungle.canopy() > 0.9f);
        assertEquals(Blocks.JUNGLE_LEAVES, jungle.leaves().getBlock());

        BiomeLook dark = look(Biomes.DARK_FOREST);
        assertEquals(Blocks.DARK_OAK_LEAVES, dark.leaves().getBlock());

        BiomeLook plains = look(Biomes.PLAINS);
        assertNull(plains.leaves(), "plains have too few trees to show from afar");

        BiomeLook savanna = look(Biomes.SAVANNA);
        assertTrue(savanna.canopy() < 0.5f);

        assertNull(look(Biomes.DESERT).leaves());
        assertNull(look(Biomes.OCEAN).leaves());
        look(Biomes.CHERRY_GROVE);
        look(Biomes.MANGROVE_SWAMP);
        look(Biomes.BIRCH_FOREST);
    }
}
