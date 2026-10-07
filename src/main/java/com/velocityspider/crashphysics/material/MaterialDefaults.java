package com.velocityspider.crashphysics.material;

import com.velocityspider.crashphysics.physics.MaterialProfile;
import net.minecraft.core.BlockPos;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.level.EmptyBlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.common.Tags;
import org.jetbrains.annotations.Nullable;

/**
 * Guesses the material of blocks that no datapack describes, from their tags, sound type and hardness.
 * <p>
 * Values are for a block treated as a 1 m³ chunk of material as it is typically built. Natural rock is solid rock,
 * while planks, bricks and glass behave like the walls and hulls players build out of them.
 */
final class MaterialDefaults {

    private MaterialDefaults() {
    }

    static CrashMaterial infer(final BlockState state) {
        final Block block = state.getBlock();
        final float hardness = block.defaultDestroyTime();

        if (hardness < 0.0f) {
            return CrashMaterial.UNBREAKABLE;
        }

        final SoundType sound = state.getSoundType();

        // Soft things
        if (state.is(BlockTags.LEAVES) || sound == SoundType.AZALEA_LEAVES || sound == SoundType.CHERRY_LEAVES) {
            return of(80, 2.0e4, 5.0e3, FractureMode.SOFT);
        }
        if (state.is(BlockTags.WOOL) || state.is(BlockTags.WOOL_CARPETS) || sound == SoundType.WOOL) {
            return of(150, 4.0e4, 3.0e4, FractureMode.SOFT);
        }
        if (block == Blocks.SLIME_BLOCK || block == Blocks.HONEY_BLOCK || block == Blocks.HAY_BLOCK || block == Blocks.SPONGE || block == Blocks.WET_SPONGE) {
            return of(300, 1.0e5, 5.0e4, FractureMode.SOFT);
        }

        // Snow and ice
        if (block == Blocks.SNOW) {
            return new CrashMaterial(profile(300, 3.0e4, 2.0e3), FractureMode.GRANULAR, null, null, Blocks.AIR.defaultBlockState(), 0.0f, false);
        }
        if (state.is(BlockTags.SNOW) || sound == SoundType.SNOW || sound == SoundType.POWDER_SNOW) {
            return new CrashMaterial(profile(400, 6.0e4, 5.0e3), FractureMode.GRANULAR, Blocks.SNOW_BLOCK.defaultBlockState(), null, null, 0.0f, false);
        }
        if (state.is(BlockTags.ICE)) {
            final double strength = block == Blocks.BLUE_ICE ? 6.0e6 : block == Blocks.PACKED_ICE ? 4.0e6 : 2.0e6;
            return of(900, strength, strength * 0.1, FractureMode.BRITTLE);
        }

        // Glass
        if (state.is(Tags.Blocks.GLASS_BLOCKS) || state.is(Tags.Blocks.GLASS_PANES) || sound == SoundType.GLASS) {
            return of(2500, 4.0e5, 1.0e5, FractureMode.GLASS);
        }

        // Loose ground: these are what craters and trenches are dug into
        if (block == Blocks.CLAY) {
            return granular(1900, 8.0e5, 6.0e4, Blocks.CLAY.defaultBlockState(), null, null);
        }
        if (block == Blocks.MUD || block == Blocks.MUDDY_MANGROVE_ROOTS || sound == SoundType.MUD) {
            return granular(1700, 8.0e4, 1.0e4, Blocks.MUD.defaultBlockState(), null, null);
        }
        if (block == Blocks.GRASS_BLOCK || block == Blocks.PODZOL || block == Blocks.MYCELIUM) {
            // Roots hold turf together a little; torn turf leaves bare coarse dirt behind
            return granular(1400, 3.5e5, 3.0e4, Blocks.DIRT.defaultBlockState(), Blocks.COARSE_DIRT.defaultBlockState(), Blocks.COARSE_DIRT.defaultBlockState());
        }
        if (block == Blocks.MOSS_BLOCK) {
            return granular(500, 1.0e5, 2.0e4, Blocks.DIRT.defaultBlockState(), Blocks.DIRT.defaultBlockState(), Blocks.DIRT.defaultBlockState());
        }
        if (state.is(BlockTags.DIRT) || block == Blocks.FARMLAND || block == Blocks.DIRT_PATH) {
            return granular(1400, 3.0e5, 2.0e4, Blocks.DIRT.defaultBlockState(), Blocks.COARSE_DIRT.defaultBlockState(), null);
        }
        if (state.is(BlockTags.CONCRETE_POWDER)) {
            return granular(1500, 2.0e5, 1.0e4, state.getBlock().defaultBlockState(), null, null);
        }
        if (state.is(BlockTags.SAND) || state.is(Tags.Blocks.SANDS) || sound == SoundType.SAND) {
            return granular(1600, 1.5e5, 5.0e3, state.getBlock().defaultBlockState(), null, null);
        }
        if (state.is(Tags.Blocks.GRAVELS) || sound == SoundType.GRAVEL) {
            return granular(1800, 3.0e5, 1.0e4, Blocks.GRAVEL.defaultBlockState(), null, null);
        }
        if (sound == SoundType.SOUL_SAND || sound == SoundType.SOUL_SOIL) {
            return granular(1500, 2.0e5, 1.0e4, state.getBlock().defaultBlockState(), null, null);
        }

        // Wood
        if (state.is(BlockTags.LOGS) || state.is(Tags.Blocks.STRIPPED_LOGS) || state.is(Tags.Blocks.STRIPPED_WOODS)) {
            return of(700, 4.0e6, 1.0e6, FractureMode.FIBROUS);
        }
        if (state.is(BlockTags.PLANKS) || isWoodSound(sound)) {
            return of(600, 1.5e6, 5.0e5, FractureMode.FIBROUS);
        }

        // Metals
        if (state.is(Tags.Blocks.OBSIDIANS)) {
            return of(2600, 1.5e8, 1.5e7, FractureMode.BRITTLE);
        }
        if (sound == SoundType.NETHERITE_BLOCK || sound == SoundType.ANCIENT_DEBRIS) {
            return of(7800, 3.0e8, 6.0e7, FractureMode.DUCTILE);
        }
        if (state.is(BlockTags.ANVIL) || (state.is(Tags.Blocks.STORAGE_BLOCKS) && isMetalSound(sound)) || block == Blocks.HEAVY_CORE) {
            return of(7800, 1.0e8, 2.0e7, FractureMode.DUCTILE);
        }
        if (isMetalSound(sound)) {
            // Machines, rails, bars and other metal fittings: mostly hollow, still tough
            return of(3000, 2.0e7 * hardnessFactor(hardness, 3.0), 4.0e6, FractureMode.DUCTILE);
        }

        // Rock
        if (state.is(Tags.Blocks.END_STONES)) {
            return rock(2300, 3.0e7, hardness, state.getBlock().defaultBlockState());
        }
        if (state.is(Tags.Blocks.NETHERRACKS) || sound == SoundType.NETHERRACK) {
            return rock(1600, 3.0e6, 0.4f, Blocks.NETHERRACK.defaultBlockState());
        }
        if (state.is(Tags.Blocks.ORES)) {
            // Ore is dropped as items rather than thrown as rubble
            return rock(2900, 5.0e7, hardness, null);
        }
        if (state.is(BlockTags.BASE_STONE_OVERWORLD) || state.is(BlockTags.BASE_STONE_NETHER)) {
            final BlockState rubble;
            if (block == Blocks.STONE) {
                rubble = Blocks.COBBLESTONE.defaultBlockState();
            } else if (block == Blocks.DEEPSLATE) {
                rubble = Blocks.COBBLED_DEEPSLATE.defaultBlockState();
            } else {
                rubble = block.defaultBlockState();
            }
            return rock(2700, 5.0e7, hardness, rubble);
        }
        if (sound == SoundType.CALCITE || sound == SoundType.DRIPSTONE_BLOCK || sound == SoundType.AMETHYST) {
            return rock(2700, 2.0e7, hardness, null);
        }

        // Built masonry
        if (state.is(BlockTags.TERRACOTTA) || state.is(Tags.Blocks.GLAZED_TERRACOTTAS)) {
            return of(2000, 1.2e7, 1.2e6, FractureMode.BRITTLE);
        }
        if (state.is(Tags.Blocks.CONCRETES)) {
            return of(2400, 2.5e7, 2.5e6, FractureMode.BRITTLE);
        }
        if (sound == SoundType.MUD_BRICKS || sound == SoundType.PACKED_MUD) {
            return of(1800, 4.0e6, 4.0e5, FractureMode.BRITTLE);
        }
        if (isStoneSound(sound)) {
            return of(2300, 1.2e7 * hardnessFactor(hardness, 1.5), 1.5e6, FractureMode.BRITTLE);
        }

        // Plants and other things without a collision box
        if (state.getCollisionShape(EmptyBlockGetter.INSTANCE, BlockPos.ZERO).isEmpty() || isPlantSound(sound)) {
            return of(100, 1.0e4, 2.0e3, FractureMode.PLANT);
        }

        // Everything else: scale a generic material with how hard the block is to mine
        final double strength = 2.0e6 * Math.pow(Math.max(hardness, 0.05f), 0.8);
        return of(1500, strength, strength * 0.1, FractureMode.BRITTLE);
    }

    private static double hardnessFactor(final float hardness, final double reference) {
        return Math.max(0.5, Math.min(4.0, Math.pow(Math.max(hardness, 0.1) / reference, 0.7)));
    }

    private static MaterialProfile profile(final double density, final double strength, final double jointStrength) {
        return new MaterialProfile(density, strength, jointStrength, false);
    }

    private static CrashMaterial of(final double density, final double strength, final double jointStrength, final FractureMode fracture) {
        return new CrashMaterial(profile(density, strength, jointStrength), fracture, null, null, null, 0.0f, false);
    }

    private static CrashMaterial granular(final double density, final double strength, final double jointStrength, @Nullable final BlockState ejecta,
                                          @Nullable final BlockState scar, @Nullable final BlockState skid) {
        return new CrashMaterial(profile(density, strength, jointStrength), FractureMode.GRANULAR, ejecta, scar, skid, 0.0f, false);
    }

    private static CrashMaterial rock(final double density, final double referenceStrength, final float hardness, @Nullable final BlockState rubble) {
        final double strength = referenceStrength * hardnessFactor(hardness, 1.5);
        return new CrashMaterial(profile(density, strength, strength * 0.1), FractureMode.BRITTLE, rubble, null, null, 0.0f, false);
    }

    private static boolean isWoodSound(final SoundType sound) {
        return sound == SoundType.WOOD || sound == SoundType.BAMBOO_WOOD || sound == SoundType.CHERRY_WOOD || sound == SoundType.NETHER_WOOD
                || sound == SoundType.LADDER || sound == SoundType.SCAFFOLDING || sound == SoundType.BAMBOO || sound == SoundType.CHISELED_BOOKSHELF
                || sound == SoundType.HANGING_SIGN || sound == SoundType.NETHER_WOOD_HANGING_SIGN || sound == SoundType.BAMBOO_WOOD_HANGING_SIGN
                || sound == SoundType.CHERRY_WOOD_HANGING_SIGN || sound == SoundType.STEM;
    }

    private static boolean isMetalSound(final SoundType sound) {
        return sound == SoundType.METAL || sound == SoundType.COPPER || sound == SoundType.COPPER_BULB || sound == SoundType.COPPER_GRATE
                || sound == SoundType.CHAIN || sound == SoundType.LANTERN || sound == SoundType.ANVIL || sound == SoundType.HEAVY_CORE
                || sound == SoundType.LODESTONE || sound == SoundType.VAULT || sound == SoundType.TRIAL_SPAWNER;
    }

    private static boolean isStoneSound(final SoundType sound) {
        return sound == SoundType.STONE || sound == SoundType.DEEPSLATE || sound == SoundType.DEEPSLATE_BRICKS || sound == SoundType.DEEPSLATE_TILES
                || sound == SoundType.POLISHED_DEEPSLATE || sound == SoundType.NETHER_BRICKS || sound == SoundType.BASALT || sound == SoundType.TUFF
                || sound == SoundType.TUFF_BRICKS || sound == SoundType.POLISHED_TUFF || sound == SoundType.GILDED_BLACKSTONE || sound == SoundType.NETHER_ORE
                || sound == SoundType.BONE_BLOCK || sound == SoundType.CORAL_BLOCK || sound == SoundType.DECORATED_POT || sound == SoundType.DECORATED_POT_CRACKED;
    }

    private static boolean isPlantSound(final SoundType sound) {
        return sound == SoundType.GRASS || sound == SoundType.CROP || sound == SoundType.HARD_CROP || sound == SoundType.VINE || sound == SoundType.SWEET_BERRY_BUSH
                || sound == SoundType.CAVE_VINES || sound == SoundType.MOSS_CARPET || sound == SoundType.PINK_PETALS || sound == SoundType.FUNGUS
                || sound == SoundType.ROOTS || sound == SoundType.NETHER_SPROUTS || sound == SoundType.WEEPING_VINES || sound == SoundType.TWISTING_VINES
                || sound == SoundType.LILY_PAD || sound == SoundType.SPORE_BLOSSOM || sound == SoundType.AZALEA || sound == SoundType.FLOWERING_AZALEA
                || sound == SoundType.BIG_DRIPLEAF || sound == SoundType.SMALL_DRIPLEAF || sound == SoundType.HANGING_ROOTS || sound == SoundType.GLOW_LICHEN
                || sound == SoundType.WET_GRASS || sound == SoundType.CHERRY_SAPLING || sound == SoundType.BAMBOO_SAPLING || sound == SoundType.NETHER_WART;
    }
}
