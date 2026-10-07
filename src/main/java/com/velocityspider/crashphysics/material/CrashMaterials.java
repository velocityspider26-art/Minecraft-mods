package com.velocityspider.crashphysics.material;

import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

import java.util.List;

/**
 * Looks up the {@link CrashMaterial} of a block state.
 * <p>
 * Lookups happen inside Sable's physics step for every contact, so results are cached in a flat array indexed by
 * block state ID. The cache is replaced wholesale when datapacks or tags reload, so readers never need a lock.
 */
public final class CrashMaterials {

    private static volatile List<MaterialOverride> overrides = List.of();
    private static volatile CrashMaterial[] cache = new CrashMaterial[0];

    private CrashMaterials() {
    }

    public static CrashMaterial get(final BlockState state) {
        final int id = Block.BLOCK_STATE_REGISTRY.getId(state);
        CrashMaterial[] table = cache;

        if (id < 0) {
            return resolve(state);
        }

        if (id >= table.length) {
            table = growCache(id);
        }

        CrashMaterial material = table[id];
        if (material == null) {
            material = resolve(state);
            table[id] = material;
        }
        return material;
    }

    private static synchronized CrashMaterial[] growCache(final int id) {
        CrashMaterial[] table = cache;
        if (id >= table.length) {
            final CrashMaterial[] grown = new CrashMaterial[Math.max(id + 1, Block.BLOCK_STATE_REGISTRY.size())];
            System.arraycopy(table, 0, grown, 0, table.length);
            cache = grown;
            table = grown;
        }
        return table;
    }

    private static CrashMaterial resolve(final BlockState state) {
        CrashMaterial material = MaterialDefaults.infer(state);
        for (final MaterialOverride override : overrides) {
            if (override.matches(state)) {
                material = override.applyTo(state, material);
            }
        }
        return material;
    }

    /**
     * Installs new datapack definitions, sorted by ascending priority.
     */
    static void setOverrides(final List<MaterialOverride> newOverrides) {
        overrides = List.copyOf(newOverrides);
        invalidate();
    }

    /**
     * Forgets every cached material, e.g. after tags changed.
     */
    public static synchronized void invalidate() {
        cache = new CrashMaterial[Block.BLOCK_STATE_REGISTRY.size()];
    }
}
