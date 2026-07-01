package com.velocityspider.deathstar.block;

import java.util.ArrayList;
import java.util.List;

import com.velocityspider.deathstar.DeathStarMod;

import net.minecraft.world.item.BlockItem;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

/**
 * The custom blocks the Death Star is built from. These are what give it its own look instead of
 * re-using vanilla iron/concrete: panelled hull, structural frame, scorched wreckage, Imperial
 * interior surfaces, and the glowing reactor / superlaser internals.
 *
 * <p>All of them are plain full cubes, so registration is deliberately compact. The glowing ones
 * emit light so the reactor and superlaser read as powered even inside the assembled sub-level.
 */
public final class ModBlocks {

    public static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(DeathStarMod.MODID);

    /** Block items to drop into the creative tab, in registration order. */
    public static final List<DeferredItem<BlockItem>> BLOCK_ITEMS = new ArrayList<>();

    // --- Hull --------------------------------------------------------------------------------
    public static final DeferredBlock<Block> HULL_PLATING = metal("hull_plating", MapColor.METAL);
    public static final DeferredBlock<Block> HULL_PLATING_DARK = metal("hull_plating_dark", MapColor.COLOR_GRAY);
    public static final DeferredBlock<Block> HULL_GREEBLE = metal("hull_greeble", MapColor.COLOR_GRAY);
    public static final DeferredBlock<Block> SCORCHED_HULL = metal("scorched_hull", MapColor.COLOR_BLACK);

    // --- Structure & interior ----------------------------------------------------------------
    public static final DeferredBlock<Block> REINFORCED_FRAME = metal("reinforced_frame", MapColor.COLOR_BLACK);
    public static final DeferredBlock<Block> INTERIOR_WALL = metal("interior_wall", MapColor.COLOR_LIGHT_GRAY);
    public static final DeferredBlock<Block> INTERIOR_FLOOR = metal("interior_floor", MapColor.COLOR_GRAY);
    public static final DeferredBlock<Block> CONTROL_PANEL = glowing("control_panel", MapColor.COLOR_BLACK, 5);

    // --- Reactor & superlaser (glowing) ------------------------------------------------------
    public static final DeferredBlock<Block> REACTOR_CORE = glowing("reactor_core", MapColor.COLOR_ORANGE, 15);
    public static final DeferredBlock<Block> REACTOR_CASING = glowing("reactor_casing", MapColor.COLOR_BLACK, 4);
    public static final DeferredBlock<Block> SUPERLASER_LENS = glowing("superlaser_lens", MapColor.COLOR_GREEN, 13);
    public static final DeferredBlock<Block> POWER_CONDUIT = glowing("power_conduit", MapColor.COLOR_LIGHT_BLUE, 9);

    private ModBlocks() {}

    /** Touching a field forces class-load so every block above is registered. */
    public static void init() {}

    private static BlockBehaviour.Properties base(MapColor color) {
        return BlockBehaviour.Properties.of()
                .mapColor(color)
                .sound(SoundType.NETHERITE_BLOCK)
                .strength(50.0f, 1200.0f) // tough, like a battle station
                .requiresCorrectToolForDrops();
    }

    private static DeferredBlock<Block> metal(String name, MapColor color) {
        return register(name, base(color));
    }

    private static DeferredBlock<Block> glowing(String name, MapColor color, int light) {
        return register(name, base(color).lightLevel(state -> light));
    }

    private static DeferredBlock<Block> register(String name, BlockBehaviour.Properties props) {
        DeferredBlock<Block> block = BLOCKS.registerSimpleBlock(name, props);
        BLOCK_ITEMS.add(DeathStarMod.ITEMS.registerSimpleBlockItem(name, block));
        return block;
    }
}
